/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotsonnenuploader.sonnendatareplayer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;
import com.oracle.demo.timg.iot.iotsonnenuploader.mqtt.MqttSonnenBatteryPublisher;
import com.oracle.demo.timg.iot.iotsonnenuploader.sonnendatacapture.DataCaptureConfig;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "datareplay.enabled", value = "true", defaultValue = "false")
@Requires(property = "datareplay.statusreplay.enabled", value = "true", defaultValue = "false")
public class StatusDataReplay implements Runnable {
	@Getter
	@Setter
	private boolean dontWrite = true;
	@Property(name = "datacapture.statussave.filename", defaultValue = "./saveddata/status.json")
	private String statusFileName;
	@Inject
	private MqttSonnenBatteryPublisher mqttSonnenBatteryPublisher;
	@Inject
	private ObjectMapper mapper;

	private BufferedReader reader;
	private boolean shutdownInProgress = false;

	// 0 means send the next as soon as possible, anything else is to divide the
	// wall clock time for this program (not the simulated data, while still
	// maintaining the offset timestamps in the actual replay data)
	@Property(name = "datareplay.replayrate", defaultValue = "1")
	private int replayrate;
	@Property(name = "datareplay.uploadtimestarts.now", defaultValue = "true")
	private boolean uploadTimeStartsNow;
	@Property(name = "datareplay.uploadtimestarts.offsettofinishnow", defaultValue = "true")
	private boolean offsetToFinishNow;
	@Property(name = "datareplay.uploadtimestarts.relative", defaultValue = "false")
	private boolean uploadTimeStartsRelative;
	@Property(name = "datareplay.uploadtimestart.timestamp", defaultValue = "2025-01-01T00:00:00.000000Z[Europe/London]")
	private ZonedDateTime specifiedUploadTimeStartZTD;
	@Property(name = "datareplay.uploadtimestart.relativeoffset", defaultValue = "-100s")
	private Duration uploadTimeStartRelativeOffset;
	// when to start the replay within the data set
	// should we skip forward ?
	@Property(name = "datareplay.startafterinputtimestamp.enabled", defaultValue = "false")
	private boolean startAfterInputTimestamp;
	// is the start relative to the data start (e.g. 1 hour in) or absolute against
	// the input timestream
	@Property(name = "datareplay.startafterinputtimestamp.relative", defaultValue = "false")
	private boolean startAfterInputTimestampRelative;
	// if it's the absolute within the input data the timestamp to start
	@Property(name = "datareplay.startafterinputtimestamp.timestamp", defaultValue = "2025-01-01T00:00:00.000000Z[Europe/London]")
	private ZonedDateTime startAfterInputTimestampTimestamp;
	// if we start on a relative time when is it
	@Property(name = "datareplay.startafterinputtimestamp.duration", defaultValue = "10m")
	private Duration startAfterInputTimestampDuration;

	// these control when to stop the replay
	// do we stop after a specified time / duration
	@Property(name = "datareplay.stopafterinputtimestamp.enabled", defaultValue = "false")
	private boolean stopAfterInputTimestamp;
	// is the stop relative to the data start (e.g. 2 hours in) or absolute against
	// the input timestream ?
	@Property(name = "datareplay.stopafterinputtimestamp.relative", defaultValue = "false")
	private boolean stopAfterInputTimestampRelative;
	@Property(name = "datareplay.stopafterinputtimestamp.relativetostart", defaultValue = "false")
	// if the stop time is relative then if this is true then the duration is
	// relative to the point when data is actually uploaded
	// e.g. a start of 2 hours in (relative or absolute) and a relative end of 3
	// hours will send data for 1 hour (2 and 3 hours in) if this is
	// false, but if it's true it will send data for 3 hours (2 to 5 hours)
	private boolean stopAfterInputTimestampRelativeToStart;
	// if we stop on an absolute time when is it
	@Property(name = "datareplay.stopafterinputtimestamp.timestamp", defaultValue = "2025-01-02T00:00:00.000000Z[Europe/London]")
	private ZonedDateTime stopAfterInputTimestampTimestamp;
	// if we stop on a relative time when is it
	@Property(name = "datareplay.stopafterinputtimestamp.duration", defaultValue = "10m")
	private Duration stopAfterInputTimestampDuration;

	@Property(name = "datareplay.startafterinputtimestamp.uploadtimesettostart", defaultValue = "true")
	private boolean uploadTimeSetToStart;
	@Property(name = "datareplay.statusreplay.actuallyupload", defaultValue = "true")
	private boolean actuallyUpload;

	private ZonedDateTime uploadTimeStartZTD;
	private ZonedDateTime simulationTimeRunStartZTD; // the current time
	private ZonedDateTime simulationTimeLastSentZTD;// the current time
	private long uploadTimeStartInMillis;
	// we need to sell the reader to use the loaded line when we get the first
	// actual data we want to send
	private boolean reuseInputData = true;
	private SonnenStatus nextToSend;
	private int lineRead = 0;
	private DataCaptureConfig dataCaptureConfig;

	@Inject
	private TaskScheduler executor;

	private ScheduledFuture<?> nextRunFuture;

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for status replay, input file=" + statusFileName);
		try {
			reader = new BufferedReader(new FileReader(statusFileName));
		} catch (IOException e) {
			log.warning("Unable to open reader from file " + statusFileName + " due to " + e.getLocalizedMessage()
					+ "\nNo data is available to replay.");
			return;
		}
		// try and get the first config info
		String DCCinput;
		try {
			DCCinput = reader.readLine();
		} catch (IOException e) {
			log.warning("Unable to read first line (data capture config) due to " + e.getLocalizedMessage());
			return;
		}
		try {
			dataCaptureConfig = mapper.readValue(DCCinput, DataCaptureConfig.class);
		} catch (IOException e) {
			log.warning("Unable to parse data capture config (" + DCCinput + ") due to " + e.getLocalizedMessage());
			return;
		}
		lineRead++;
		log.info("Data capture = " + dataCaptureConfig);

		// are we skipping forward ?
		if (startAfterInputTimestamp) {
			log.info("startAfterInputTimestamp is true");
			// we are, let's work out until when
			if (startAfterInputTimestampRelative) {
				// if it's relative then skip forward that much
				startAfterInputTimestampTimestamp = dataCaptureConfig.getStarttime()
						.plus(startAfterInputTimestampDuration);
				log.info("Start after input timestamp is relative of " + startAfterInputTimestampDuration);
			} else {
				// it's absolute, use the one that was set
				log.info("Start after input timestamp is absolute");
			}
			log.info("Adjusted start time is "
					+ startAfterInputTimestampTimestamp.format(DateTimeFormatter.ISO_DATE_TIME));
		} else {
			// no start time set, but still set the start as we use when we see if there are
			// inputs to skip
			log.info("No start time set, using data capture start");
			startAfterInputTimestampTimestamp = dataCaptureConfig.getStarttime();
		}
		// are we stopping before the end ?
		if (stopAfterInputTimestamp) {
			log.info("Stop after input timestamp is true");
			if (stopAfterInputTimestampRelative) {
				log.info("Stop time is relative from sequence delayed " + stopAfterInputTimestampDuration);
				if (stopAfterInputTimestampRelativeToStart) {
					log.info("Stop time is relative to calculated start time");
					stopAfterInputTimestampTimestamp = startAfterInputTimestampTimestamp
							.plus(stopAfterInputTimestampDuration);
				} else {
					log.info("Stop time is relative to data stream start time");
					stopAfterInputTimestampTimestamp = dataCaptureConfig.getStarttime()
							.plus(stopAfterInputTimestampDuration);
				}
			} else {
				log.info("Stop time is absolute, using specified value");
			}
			log.info("Targeted stop time based on input timestamps is "
					+ stopAfterInputTimestampTimestamp.format(DateTimeFormatter.ISO_DATE_TIME));
		} else {
			// we can;t work out the end as it's the last record and we don't want to skip
			// through all the data
			log.info("No stop time set");
		}
		// do some checking to make sure this is valid timestamps
		// the start time (if specified) shouldn't be before the data capture (this test
		// is not strictly needed but helps spot problems)
		if (startAfterInputTimestamp) {
			if (startAfterInputTimestampTimestamp.isBefore(dataCaptureConfig.getStarttime())) {
				log.warning("Data input error, specified start time is before data input start time,not proceeding");
				return;
			}
		}
		if (stopAfterInputTimestamp) {
			// the start will have been set, even if just to the data capture start, so we
			// know we have something to check against
			if (stopAfterInputTimestampTimestamp.isBefore(startAfterInputTimestampTimestamp)) {
				log.warning("Data input error, specified stop time is before specified start time,not proceeding");
				return;
			}
			if (stopAfterInputTimestampTimestamp.isBefore(dataCaptureConfig.getStarttime())) {
				log.warning("Data input error, specified stop time is before the input data start time,not proceeding");
				return;
			}
		}
		// when sending the data do we want to set the timestamp based on the specified
		// time or the current time, of back sate it to the run will finish now based
		// on the capture duration ?
		// if offseting to the current time it takes priority over the other start time
		// settings
		if (offsetToFinishNow) {
			// if we have an end time in the input stream use that, otherwise use the start
			// time within the stream plus the duration
			ZonedDateTime projectedEnd = stopAfterInputTimestamp ? stopAfterInputTimestampTimestamp
					: dataCaptureConfig.getStarttime().plus(dataCaptureConfig.getDuration());
			// we know we have a start time within the stream, (though it may be the data
			// stream start timestamp) so we can work out how long the stream will be
			Duration streamUploadWindow = Duration.between(projectedEnd, startAfterInputTimestampTimestamp).abs();
			// work out how long the upload window is based on the start / end time(s)
			// start time will have been set above
			uploadTimeStartZTD = ZonedDateTime.now().minus(streamUploadWindow);
			log.info(
					"Offseting start time from now into the past by the capture duration (allowing for stop / start settings) "
							+ streamUploadWindow + " to " + uploadTimeStartZTD.format(DateTimeFormatter.ISO_DATE_TIME)
							+ " and finish now");
		} else {
			if (uploadTimeStartsNow) {
				uploadTimeStartZTD = ZonedDateTime.now();
				log.info("Upload start time is now " + uploadTimeStartZTD.format(DateTimeFormatter.ISO_DATE_TIME));
			} else {
				uploadTimeStartZTD = specifiedUploadTimeStartZTD;
				log.info("Upload start time is specified as "
						+ uploadTimeStartZTD.format(DateTimeFormatter.ISO_DATE_TIME));
			}
			if (uploadTimeStartsRelative) {
				log.info("Upload start time has a relative offset of " + uploadTimeStartRelativeOffset);
				uploadTimeStartZTD = uploadTimeStartZTD.plus(uploadTimeStartRelativeOffset);
				log.info("Upload start time after offset is "
						+ uploadTimeStartZTD.format(DateTimeFormatter.ISO_DATE_TIME));
			}
		}
		simulationTimeRunStartZTD = ZonedDateTime.now();
		simulationTimeLastSentZTD = simulationTimeRunStartZTD;
		uploadTimeStartInMillis = uploadTimeStartZTD.toInstant().toEpochMilli();
		log.info("Time to be used for upload start is " + uploadTimeStartZTD.format(DateTimeFormatter.ISO_DATE_TIME));
		// skip forward to until we have reached the entry with the start time
		do {
			String nextUploadLine;
			try {
				nextUploadLine = reader.readLine();
			} catch (IOException e) {
				log.warning(
						"Unable to read sonnen status from line " + lineRead + " due to " + e.getLocalizedMessage());
				return;
			}
			log.info("Skipping forward Just read line " + lineRead + " which is" + nextUploadLine);
			try {
				nextToSend = mapper.readValue(nextUploadLine, SonnenStatus.class);
			} catch (IOException e) {
				log.warning("Unable to parse data capture config from line " + lineRead + " (" + nextUploadLine
						+ ") due to " + e.getLocalizedMessage());
				return;
			}
			lineRead++;
		} while (nextToSend.getTimestamp().isBefore(startAfterInputTimestampTimestamp));

		log.info("First data to send is " + nextToSend);
		// start the sending process
		getAndScheduleNextUpload();
	}

	@Override
	public void run() {
		// if we're shutting down then give in
		if (shutdownInProgress) {
			return;
		}
		nextRunFuture = null;
		// we need to offset the timestamps in the upload
		// work out how long it's been between the start of the run and the saved record
		// based on the sated run data
		Duration runTimeSinceStartForData = Duration
				.between(dataCaptureConfig.getStarttime(), nextToSend.getTimestamp()).abs();

		// now calculate the time based on that for the adjusted time for this record
		ZonedDateTime adjustedZTD = uploadTimeStartZTD.plus(runTimeSinceStartForData);
		long adjustedMillis = uploadTimeStartInMillis + runTimeSinceStartForData.toMillis();
		log.info("Origional data " + nextToSend + "\nthis is " + runTimeSinceStartForData + " from the start time of "
				+ simulationTimeRunStartZTD.format(DateTimeFormatter.ISO_DATE_TIME)
				+ "so the clock will be adjusted to " + adjustedZTD.format(DateTimeFormatter.ISO_DATE_TIME)
				+ "and the millis since epoch to " + adjustedMillis);
		nextToSend.setTime(adjustedMillis);
		nextToSend.setTimestamp(adjustedZTD);
		log.info("Timer updated data " + nextToSend);
		// do the send
		sendToMqtt(nextToSend);
		// move the last sent (real timeclock for this application) forwards
		simulationTimeLastSentZTD = ZonedDateTime.now();
		// reschedule the next one
		getAndScheduleNextUpload();
	}

	/**
	 * @param nextToSend
	 * 
	 */
	private void sendToMqtt(SonnenStatus statusToSend) {
		if (actuallyUpload) {
			try {
				CompletableFuture<Void> publishResp = mqttSonnenBatteryPublisher.publishSonnenStatus(statusToSend);
				publishResp.thenRun(() -> log.info("Published status to mqtt"));
			} catch (Exception e) {
				log.warning("Problem publishing status to mqtt, " + e.getLocalizedMessage());
			}
		} else {
			log.info("actuallyUpload is false but would have uploaded status to mqtt");
		}
	}

	public void getAndScheduleNextUpload() {
		// We may have loaded data when doing a skip forward (if it was needed), so need
		// to check if we're reusing the input data we already have it loaded and
		// deserialized, so just flag that next time we do need to load it
		if (reuseInputData) {
			reuseInputData = false;
		} else {
			String nextUploadLine;
			try {
				nextUploadLine = reader.readLine();
			} catch (IOException e) {
				log.warning(
						"Unable to read sonnen status from line " + lineRead + " due to " + e.getLocalizedMessage());
				return;
			}
			log.info("Just read the line " + lineRead + " which is " + nextUploadLine);
			try {
				nextToSend = mapper.readValue(nextUploadLine, SonnenStatus.class);
			} catch (IOException e) {
				log.warning("Unable to parse data capture config from line " + lineRead + " (" + nextUploadLine
						+ ") due to " + e.getLocalizedMessage());
				return;
			}
			lineRead++;
		}
		// if we're stopping after the specified timestamp (as in the simulation data)
		// then check the timestamp
		if (stopAfterInputTimestamp) {
			if (nextToSend.getTimestamp().isAfter(stopAfterInputTimestampTimestamp)) {
				log.info("Most recent read input " + nextToSend
						+ "\n has timestamp after internal simulation sop after timestamp of "
						+ stopAfterInputTimestampTimestamp.format(DateTimeFormatter.ISO_DATE_TIME)
						+ "\nStopping status upload simulation");
				// returning here means nothing will be scheduled so this sequence is over.
				return;
			}
		}
		// work out how far into the replay we are, Note that this just
		// calculates relative offsets in the data stream
		// for the actual "absolute" we will need to add the upload timestamp sequence
		// start time with this
		Duration timeIntoTheReplay = Duration.between(nextToSend.getTimestamp(), dataCaptureConfig.getStarttime())
				.abs();
		log.info("Time from the start of the replay is " + timeIntoTheReplay);
		ZonedDateTime timeToNextSend = simulationTimeRunStartZTD.plus(timeIntoTheReplay);
		log.info("Last sent was " + simulationTimeLastSentZTD.format(DateTimeFormatter.ISO_DATE_TIME) + " next is "
				+ timeToNextSend.format(DateTimeFormatter.ISO_DATE_TIME));
		// here setup the schedule for the next call, allow for a variable "time rate"
		// to be applied from running in real time to running
		Duration intervalToNextSend;
		if (replayrate == 0) {
			intervalToNextSend = Duration.ofNanos(0);
		} else {
			intervalToNextSend = Duration.between(simulationTimeLastSentZTD, timeToNextSend).abs()
					.dividedBy(replayrate);
		}
		log.info("Delay in replay data until next send is " + intervalToNextSend + " (the replay rate is " + replayrate
				+ ")");
		// schedule the next call if we are still running
		if (!shutdownInProgress) {
			nextRunFuture = executor.schedule(intervalToNextSend, this);
		}
	}

	@EventListener
	public void onShutdown(ShutdownEvent event) {
		shutdownInProgress = true;
		if (nextRunFuture != null) {
			nextRunFuture.cancel(false);
		}
		if (reader != null) {
			try {
				reader.close();
				reader = null;
			} catch (IOException e) {
				log.warning("Unable to close reader");
			}
		}
	}
}
