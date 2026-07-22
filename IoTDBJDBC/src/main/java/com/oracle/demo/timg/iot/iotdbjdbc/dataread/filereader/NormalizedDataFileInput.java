/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.demo.timg.iot.iotdbjdbc.dataread.filereader;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTDBClient;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandlerService;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.MissingInstanceException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.filewriter.NormalizedDataFileVersion;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Min;
import lombok.ToString;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_ENABLED, value = "true", defaultValue = "false")
@Requires(property = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_ORDER)
@ToString
public class NormalizedDataFileInput implements IoTDBClient, Runnable {
	// get the UTC TZ once to speed things later
	private final static ZoneId UTC_TZ = ZoneId.of("UTC");
	private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter
			.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX");
	@ToString.Include
	private boolean stopped = false;
	@ToString.Include
	private final int order;
	@ToString.Include
	private final String sourceFilename;
	@ToString.Include
	private Duration replayStartOffset;
	@ToString.Include
	private Duration replayDuration;
	@ToString.Include
	private ZonedDateTime replayEnd;
	@ToString.Include
	private final FileDataInputMode mode;
	@ToString.Include
	private final Duration highSpeedReplayDelay;
	@ToString.Include
	private ZonedDateTime startOffsetZDT;
	@ToString.Include
	private ZonedDateTime stopAfterZDT;
	@ToString.Include
	private Duration highSpeedOffset;
	@ToString.Exclude
	private final ObjectMapper mapper;
	@ToString.Exclude
	private BufferedReader inputReader;
	@ToString.Exclude
	private Thread currentThread;
	@ToString.Exclude
	private ScheduledExecutorService executor;
	@ToString.Include
	private NormalizedDataFileVersion nextDataToSend;

	private final NormalizedDataMessageHandlerService normalizedDataMessageHandlerService;

	private final DeviceModelInstancesCache deviceModelInstancesCache;
	private ZonedDateTime nextDataToSendTimeStamp;

	@Inject
	public NormalizedDataFileInput(ObjectMapper mapper,
			NormalizedDataMessageHandlerService normalizedDataMessageHandlerService,
			DeviceModelInstancesCache deviceModelInstancesCache,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_ORDER) @Min(value = 0) int order,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_SOURCE_FILE) String sourceFilename,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_REPLAY_START_OFFSET, defaultValue = "0s") Duration replayStartOffset,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_REPLAY_DURATION) Duration replayDuration,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_MODE, defaultValue = "REAL_TIME") FileDataInputMode mode,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_REPLAY_HIGH_SPEED_PLAYBACK_DELAY, defaultValue = "100ms") Duration highSpeedReplayDelay,
			@Property(name = FileReaderProperties.NORMALIZED_DATA_FILE_INPUT_REPLAY_END) Optional<ZonedDateTime> replayEnd) {
		this.mapper = mapper;
		this.normalizedDataMessageHandlerService = normalizedDataMessageHandlerService;
		this.deviceModelInstancesCache = deviceModelInstancesCache;
		this.order = order;
		this.sourceFilename = sourceFilename;
		this.replayStartOffset = replayStartOffset;
		this.replayDuration = replayDuration;
		this.mode = mode;
		this.highSpeedReplayDelay = highSpeedReplayDelay;
		// if we have a specified end time for the replay use the current time, if now
		// use what's been specified
		this.replayEnd = replayEnd.orElse(ZonedDateTime.now(UTC_TZ));
	}

	@Override
	public void configureDBClient(String filteringRule) throws DateTimeParseException, EOFException, IOException {
		// try to locate the very first timestamp, we will need to use that to work out
		// at what point in the file if any we need to stop.
		// try to open the file, we need to scan it to find the line previous to the one
		inputReader = new BufferedReader(new FileReader(sourceFilename));
		nextDataToSend = readNormalizedDataFileVersionFromInput(inputReader);
		ZonedDateTime startZDT = getTimeObservedFromNormalizedDataFileVersion(nextDataToSend);
		if (startZDT == null) {
			// we can't locate the initial data, throw an exception, we can't deal with
			// this, it's up to the caller to then remove us from any further processing
			throw new EOFException("No data in input file, cannot determine time stamps or start point");
		}
		// this is the start point based on the timestamps in the data file
		startOffsetZDT = startZDT.plus(replayDuration);
		// now add the replay time to the start offset time, this is also based on the
		// data file timestamps
		this.stopAfterZDT = startOffsetZDT.plus(replayDuration);
		// if we are in REAL_TIME replay mode we will be sending based on the current
		// time and then waiting for the next to send (based on the difference between
		// the one we just sent and the next one we're about to send) so for that we
		// don't need to do any further timestamp thinking, we're just going to scan
		// forwards later on
		//
		// if however we are in HIGH_SPEED mode then we need to adjust" the timestamp
		// of the loaded data value, based on the delta of first timestamp that will be
		// sent, and the timestamp found at the replay end point (which of course could
		// be in the past as well as now) relative to the end timestamp we want to
		// finish with.
		highSpeedOffset = Duration.between(stopAfterZDT, replayEnd);
		// we're going to reset the reader as we're looking to load
		// now we need to move forwards until we get to the start point, if we get null
		// then we've fallen off the end of the input stream, so need to error
		ZonedDateTime readZDT = startZDT;
		while ((readZDT != null) && (readZDT.isBefore(startOffsetZDT))) {
			nextDataToSend = readNormalizedDataFileVersionFromInput(inputReader);
			readZDT = getTimeObservedFromNormalizedDataFileVersion(nextDataToSend);
		}
		if (nextDataToSend == null) {
			throw new EOFException("Hit the end of file while moving forward to the specified start point");
		}
		// to avoid doing multiple time conversions later stash the current timestamp
		nextDataToSendTimeStamp = readZDT;
		// OK, we're set to go, lastly setup the executors
		executor = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * @param inputReader
	 * @return
	 * @throws IOException
	 * @throws EOFException
	 * @throws DateTimeParseException
	 */
	protected ZonedDateTime readNormalizedDataFileVersionTimestampFromInput(BufferedReader inputReader)
			throws IOException, DateTimeParseException {
		NormalizedDataFileVersion normalizedDataFileVersion = readNormalizedDataFileVersionFromInput(inputReader);
		if (normalizedDataFileVersion == null) {
			return null;
		}
		ZonedDateTime observedZDT = getTimeObservedFromNormalizedDataFileVersion(normalizedDataFileVersion);
		return observedZDT;
	}

	/**
	 * @param normalizedDataFileVersion
	 * @return
	 * @throws DateTimeParseException
	 */
	protected ZonedDateTime getTimeObservedFromNormalizedDataFileVersion(
			NormalizedDataFileVersion normalizedDataFileVersion) throws DateTimeParseException {
		if (normalizedDataFileVersion == null) {
			return null;
		}
		try {
			return ZonedDateTime.parse(normalizedDataFileVersion.getTimeObserved(), dateTimeFormatter);
		} catch (DateTimeParseException e) {
			throw new DateTimeParseException("Can't parse time Obeserved", e.getParsedString(), e.getErrorIndex());
		}
	}

	/**
	 * @param inputReader
	 * @return
	 * @throws IOException
	 */
	protected NormalizedDataFileVersion readNormalizedDataFileVersionFromInput(BufferedReader inputReader)
			throws IOException {
		String NormalizedDataFileVersionString = inputReader.readLine();
		if (NormalizedDataFileVersionString == null) {
			return null;
		}
		log.info("Read input " + NormalizedDataFileVersionString);
		// try and convert it to a normalized data line (the version we write to files)
		NormalizedDataFileVersion normalizedDataFileVersion = mapper.readValue(NormalizedDataFileVersionString,
				NormalizedDataFileVersion.class);
		return normalizedDataFileVersion;
	}

	@Override
	public void startDBProcessing() throws Exception {
		// if running in HIGH_SPEED mode then work out the offset between now
		// start this in a separate loop to run as soon as possible
		executor.execute(() -> this.run());
	}

	@Override
	public void stopDBProcessing() throws Exception {
		log.info("Stopping reading");
		this.stopped = true;
		// interrupt the thread if it's not null
		if (currentThread != null) {
			log.info("Interrupting thread");
			currentThread.interrupt();
		}
	}

	@Override
	public void unconfigureDBClient() throws Exception {
		inputReader.close();
		// stop the executors from accepting new tasks
		executor.shutdown();
		// close the reader

	}

	@Override
	public void run() {
		// save the thread we're running in so we can interrupt it later
		currentThread = Thread.currentThread();
		if (nextDataToSend == null) {
			log.info("nextDataToSend is null, stopping processing");
		}

		log.info("Running a send cycle on " + nextDataToSend);
		// the timestamp was extracted when the nextDataToSend was setup, but just for
		// defensive reasons
		if (nextDataToSendTimeStamp == null) {
			log.warning("nextDataToSendTimeStamp is null, cant continue with processing");
			return;
		}
		// get the normalized data using the instance device name to map to the instance
		// OCID
		NormalizedData normalizedData = getNormalizedDataFromNormalizedDataFileVersion(nextDataToSend);
		if (normalizedData == null) {
			log.info(
					"Programming error, this should not have happened, conversion of NormalizedDataFromNormalized to NormalizedData returned null, stopping processing");
			return;
		}
		// depending on the mode we need to replace the timestamp with the current time
		// or work out an offset for it
		ZonedDateTime timeToSet = switch (this.mode) {
		case REAL_TIME -> ZonedDateTime.now(UTC_TZ);
		case HIGH_SPEED -> nextDataToSendTimeStamp.plus(highSpeedOffset);
		};
		normalizedData.setTimeObserved(timeToSet.format(dateTimeFormatter));
		// OK, got it all, let's send it
		normalizedDataMessageHandlerService.handle(normalizedData);
		// need to re-schedule for the next instance ;
		NormalizedDataFileVersion followingNormalizedDataFileVersion;
		try {
			followingNormalizedDataFileVersion = readNormalizedDataFileVersionFromInput(inputReader);
		} catch (IOException e) {
			log.info("retrieving followingNormalizedDataFileVersion threw an IOException (" + e.getLocalizedMessage()
					+ ") will not reschedule");
			return;
		}
		if (followingNormalizedDataFileVersion == null) {
			log.info("retrieved followingNormalizedDataFileVersion is null, cannot reschedule");
			return;
		}
		// get the time stamp of the next file
		ZonedDateTime followingNormalizedDataFileVersionTimeObserved = getTimeObservedFromNormalizedDataFileVersion(
				followingNormalizedDataFileVersion);
		if (followingNormalizedDataFileVersionTimeObserved == null) {
			log.info("extracted timeObserved from followingNormalizedDataFileVersion is null, cannot reschedule");
			return;
		}
		// work out how long until we run again
		Duration delayDuration = switch (mode) {
		case HIGH_SPEED -> highSpeedReplayDelay;
		case REAL_TIME -> Duration.between(nextDataToSendTimeStamp, followingNormalizedDataFileVersionTimeObserved);
		};
		// move the saved data along
		nextDataToSend = followingNormalizedDataFileVersion;
		nextDataToSendTimeStamp = followingNormalizedDataFileVersionTimeObserved;
		// reschedule us to run later on
		executor.schedule(this, delayDuration.toNanos(), TimeUnit.NANOSECONDS);
	}

	/**
	 * @param instanceName
	 * @param normalizedData
	 * @return
	 * @throws MissingInstanceException
	 * @throws SQLException
	 */
	protected NormalizedData getNormalizedDataFromNormalizedDataFileVersion(
			NormalizedDataFileVersion normalizedDataFileVersion) {
		String instanceDisplayName = normalizedDataFileVersion.getDigitalTwinInstanceDisplayName();
		if (instanceDisplayName == null) {
			log.info("No instance name found for " + normalizedDataFileVersion
					+ " won't be able to get the instanceID so can't send it");
			return null;
		}
		// try and get the instance id, we need this
		String instanceId;
		try {
			instanceId = deviceModelInstancesCache.getInstanceIdByInstanceDisplayName(instanceDisplayName, true);
		} catch (MissingInstanceException e) {
			log.info("Can't get the instanceId for instance named " + instanceDisplayName);
			return null;
		} catch (SQLException e) {
			log.warning("SQLException getting the instanceId, " + e.getLocalizedMessage());
			return null;
		}
		return normalizedDataFileVersion.buildTo(instanceId);
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getConfig() {
		return this.toString();
	}
}
