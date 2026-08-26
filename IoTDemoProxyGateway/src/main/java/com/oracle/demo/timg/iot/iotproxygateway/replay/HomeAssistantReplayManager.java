package com.oracle.demo.timg.iot.iotproxygateway.replay;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayConfigDataUploader;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayStatsData;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayStatsDataUploader;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayStatsTrackingData;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantEntityRetrieveStatus;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.HomeAssistantEntityUploadHandler;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder.RecordedData;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder.RecorderHomeAssistantEntityRetrieveStatusInfo;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder.RecorderHomeAssistantEntitySetInfo;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
@Context
// are we going to retrieve data from home assistant ?
@Requires(property = PropertyNames.OPERATING_MODE_INPUT, value = "REPLAY")
public class HomeAssistantReplayManager implements Runnable {
	public final static ZoneId UTCTZ = ZoneId.of("UTC");
	public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX");

	private final ObjectMapper mapper;
	private final Collection<HomeAssistantMonitoredEntitySet> monitoredEntitySets;
	private final Duration replayStartOffset;
	private final Duration highSpeedDuration;
	private final Duration highSpeedDelay;
	private final Duration realTimeDuration;

	private BufferedReader reader;
	private final HomeAssistantEntityUploadHandler homeAssistantEntityUploadHandler;
	private final GatewayConfigDataUploader gatewayConfigDataUploader;
	private final GatewayStatsDataUploader gatewayStatsDataUploader;
	private final Map<String, HomeAssistantMonitoredEntitySet> entitySetNameToHomeAssistantMonitoredEntitySet;
	private final GatewayStatsTrackingData gatewayStatsTrackingData;

	private RecordedData lastEntryRead;

	private final String inputFile;

	private final long sucessfullUploadWindow;
	private final long failedUploadWindow;

	private int highSpeedEntriesCountRemaining;
	// default to high speed mode
	private boolean inHighSpeedMode = true;

	private ZonedDateTime realTimeEntryStartTimestamp;
	private ZonedDateTime realTimeEntryFinishTimestamp;
	private Duration highSpeedOffsetAfterDurationAdded;

	private ScheduledExecutorService executor;

	@Inject
	public HomeAssistantReplayManager(ObjectMapper mapper,
			Collection<HomeAssistantMonitoredEntitySet> monitoredEntitySets,
			HomeAssistantEntityUploadHandler homeAssistantEntityUploadHandler,
			GatewayConfigDataUploader gatewayConfigDataUploader, GatewayStatsDataUploader gatewayStatsDataUploader,
			GatewayStatsTrackingData gatewayStatsTrackingData,
			@Property(name = PropertyNames.HA_REPLAY_INPUT_FILE) String inputFile,
			@Property(name = PropertyNames.HA_REPLAY_START_OFFSET, defaultValue = "0s") Duration replayStartOffset,
			@Property(name = PropertyNames.HA_REPLAY_HIGH_SPEED_DURATION, defaultValue = "0s") Duration highSpeedDuration,
			@Property(name = PropertyNames.HA_REPLAY_HIGH_SPEED_DELAY, defaultValue = "1ms") Duration highSpeedDelay,
			@Property(name = PropertyNames.HA_REPLAY_REALTIME_DURATION, defaultValue = "0s") Duration realTimeDuration,
			@Property(name = PropertyNames.GATEWAY_STATS_SUCESSFULL_UPLOAD_WINDOW, defaultValue = "PT10m") Duration sucessfullUploadWindow,
			@Property(name = PropertyNames.GATEWAY_STATS_FAILED_UPLOAD_WINDOW, defaultValue = "PT10m") Duration failedUploadWindow)
			throws IOException {
		// save our settings
		this.homeAssistantEntityUploadHandler = homeAssistantEntityUploadHandler;
		this.gatewayConfigDataUploader = gatewayConfigDataUploader;
		this.gatewayStatsDataUploader = gatewayStatsDataUploader;
		this.gatewayStatsTrackingData = gatewayStatsTrackingData;
		this.inputFile = inputFile;
		this.mapper = mapper;
		this.monitoredEntitySets = monitoredEntitySets;
		this.replayStartOffset = replayStartOffset;
		this.highSpeedDuration = highSpeedDuration;
		this.highSpeedDelay = highSpeedDelay;
		this.realTimeDuration = realTimeDuration;
		this.sucessfullUploadWindow = sucessfullUploadWindow.getSeconds();
		this.failedUploadWindow = failedUploadWindow.getSeconds();
		// do whatever setup is needed
		this.entitySetNameToHomeAssistantMonitoredEntitySet = monitoredEntitySets.stream()
				.collect(Collectors.toMap(entitySet -> entitySet.getName(), entitySet -> entitySet));
		// set the gw stats and config uploads to paused - we only want them to work in
		// real time mode, that will be set further down
		gatewayStatsDataUploader.setPauseUploads(true);
		gatewayConfigDataUploader.setPauseUploads(true);
		openReader(false);
	}

	/**
	 * @param inputFile
	 * @throws IOException
	 */
	private void openReader(boolean closeFileFirst) throws IOException {
		// if asked to try to close the reader
		if (closeFileFirst) {
			try {
				reader.close();
			} catch (IOException e) {
				log.severe("Replayer can't close the input file " + inputFile);
				throw e;
			}
		}
		// now try to open it
		try {
			reader = new BufferedReader(new FileReader(this.inputFile));
		} catch (IOException e) {
			log.severe("Replayer can't open the input file " + inputFile);
			throw e;
		}
	}

	public String getConfig() {
		String outputs = "HomeAssistantEntityUploadHandler using " + homeAssistantEntityUploadHandler.getName();
		return outputs + "inputFile=" + inputFile + ", replayStartOffset=" + replayStartOffset + ", highSpeedDuration="
				+ highSpeedDuration + ", highSpeedDelay=" + highSpeedDelay + ", realTimeDuration=" + realTimeDuration;
	}

	public String getMonitoredEntitySetDetails() {
		return "There are " + monitoredEntitySets.size() + " monitoredEntitySets which are \n"
				+ monitoredEntitySets.stream()
						.map(entitySet -> entitySet.getName() + " has " + entitySet.getMonitoredentities().size()
								+ " monitored entities which are " + entitySet.getMonitoredentities().stream()
										.map(entity -> entity.getName()).collect(Collectors.joining(", ")))
						.collect(Collectors.joining("n"));
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Replayer loaded with the following settings " + getConfig());
		log.info(getMonitoredEntitySetDetails());
		log.info("Locating start point - offset from start is " + replayStartOffset);
		try {
			// get the first entry, as we need that to work out the actual starting point
			lastEntryRead = readRecordedDataLine();
			if (lastEntryRead == null) {
				log.info("There are no entries in the file, cannot proceed, halting replay");
				return;
			}
			ZonedDateTime firstEntryZDT = lastEntryRead.getRecordTimestamp();
			ZonedDateTime startAtTime = firstEntryZDT.plus(replayStartOffset);
			log.info("Looking for the first entry after " + startAtTime.format(formatter) + " this is "
					+ replayStartOffset + " after the first entry timestamp of " + firstEntryZDT.format(formatter));
			int startCount = scanDatafileForwardsUntilPastTimestamp(startAtTime, false);
			if (lastEntryRead == null) {
				log.info(
						"Hit end of file before locating an entry time stamped after " + startAtTime.format(formatter));
				return;
			}
			log.info("The start point is " + startCount + " entries in, it has a time stamp of "
					+ lastEntryRead.getRecordTimestamp().format(formatter));
			ZonedDateTime highSpeedEndPoint = startAtTime.plus(highSpeedDuration);
			log.info("Locating the number of entries to send at high speed from start point that's at time stamp "
					+ highSpeedEndPoint.format(formatter));
			highSpeedEntriesCountRemaining = scanDatafileForwardsUntilPastTimestamp(highSpeedEndPoint, true);
			if (lastEntryRead == null) {
				log.info("Hit end of file before locating an entry time stamped after "
						+ highSpeedEndPoint.format(formatter));
				return;
			}
			Duration totalHighSpeedDelayImpact = highSpeedDelay.multipliedBy(highSpeedEntriesCountRemaining);
			log.info("There are " + highSpeedEntriesCountRemaining + " high speed entries, with a duration of "
					+ highSpeedDelay + " between each that means a high speed impact of " + totalHighSpeedDelayImpact);
			realTimeEntryStartTimestamp = lastEntryRead.getRecordTimestamp();
			realTimeEntryFinishTimestamp = realTimeEntryStartTimestamp.plus(realTimeDuration);
			// now we have the timestamps for the end of the high speed section we can reset
			// to be ready for the actual replay
			lastEntryRead = null;
			openReader(true);
			log.info("reset input data, fast forwarding to the first entry after " + startAtTime.format(formatter));
			scanDatafileForwardsUntilPastTimestamp(startAtTime, false);
			if (lastEntryRead == null) {
				log.severe("Programming problem, was previously able to find an entry after "
						+ startAtTime.format(formatter) + "but can't now");
				return;
			}
			// work out the offset for the begining of high speed to now
			Duration highSpeedOffsetBeforeDurationAdded = Duration.between(highSpeedEndPoint, ZonedDateTime.now(UTCTZ));
			log.info("highSpeedOffsetBeforeDurationAdded is " + highSpeedOffsetBeforeDurationAdded);
			highSpeedOffsetAfterDurationAdded = highSpeedOffsetBeforeDurationAdded.plus(totalHighSpeedDelayImpact);
			log.info("Total staring off set allowing for high speed is " + highSpeedOffsetAfterDurationAdded);
			log.info("Setting up executor");
			// OK, we're set to go, lastly setup the executors
			executor = Executors.newSingleThreadScheduledExecutor();
			log.info("Starting to process events");
			executor.execute(this);

		} catch (Exception e) {
			log.severe("Problem locating start time data, cannot continue - " + e.getLocalizedMessage());
		}
	}

	@Override
	public void run() {
		// process the current saved item
		if (lastEntryRead == null) {
			log.info("lastEntryRead is null, shutting down");
			System.exit(0);
		}
		// adjust the timestamp to reflect the delta for the moment if wer're doing high
		// speed stuff
		// and reduce the counter
		if (highSpeedEntriesCountRemaining > 0) {
			highSpeedEntriesCountRemaining--;
			Duration origHSOffset = highSpeedOffsetAfterDurationAdded;
			highSpeedOffsetAfterDurationAdded = highSpeedOffsetAfterDurationAdded.minus(highSpeedDelay);
			log.info(() -> "After reduction are " + highSpeedEntriesCountRemaining
					+ " high speed entries remaining, the origional origHSOffset " + origHSOffset
					+ " but after remoding the individual hs duration of " + highSpeedDelay
					+ " the remaining highSpeedOffsetAfterDurationAdded is " + highSpeedOffsetAfterDurationAdded + "");
		}

		// determine the logic to use, this will de-serialize the data, upload it and /
		// or trigger a stats / config upload / update the ha retrieval stats
		switch (lastEntryRead.getRecordedDataType()) {
		case ENTITY:
			uploadHomeAssistantEntity(lastEntryRead, highSpeedOffsetAfterDurationAdded);
			break;
		case GATEWAY_CONFIG_SEND:
			sendGatewayConfig(lastEntryRead, highSpeedOffsetAfterDurationAdded);
			break;
		case GATEWAY_STATS_RESET_AND_SEND:
			sentGatewayStatsAndReset(lastEntryRead, highSpeedOffsetAfterDurationAdded);
			break;
		case HA_RETRIEVE:
			updateGatewayHAStats(lastEntryRead, highSpeedOffsetAfterDurationAdded);
			break;
		default:
			log.warning("Unknown data type " + lastEntryRead.getRecordedDataType() + " in last entry read "
					+ lastEntryRead);
			break;
		}
		// get the next entry
		RecordedData nextEntryRead = null;
		try {
			nextEntryRead = readRecordedDataLine();
		} catch (RecordedDataFormatException e) {
			log.severe("Unable to read the next recorded data item, " + e.getLocalizedMessage());
			log.severe("Cannot proceed, shutting down");
			System.exit(1);
		}
		// if it's null time to stop
		if (nextEntryRead == null) {
			log.info("Read entry is null, probabaly reached the end of the input file. Shutting down ");
			System.exit(0);
		}
		// if it's timestamp is after the enf or the replay then time to stop
		if (nextEntryRead.getRecordTimestamp().isAfter(realTimeEntryFinishTimestamp)) {
			log.info("Reached the end of the playback " + realTimeEntryFinishTimestamp.format(formatter)
					+ " Shutting down ");
			System.exit(0);
		}
		// have we reached the end of the high speed mode ?
		if (inHighSpeedMode && (nextEntryRead.getRecordTimestamp().isAfter(realTimeEntryStartTimestamp))) {
			// stop the high speed operation, when we calculate the next timestamp is will
			// be based on the "normal" offsets.
			inHighSpeedMode = false;
			// switch the stats updates to be based on the "regular" timestamps
			gatewayStatsDataUploader.setPauseUploads(false);
			gatewayConfigDataUploader.setPauseUploads(false);
		}
		Duration sleepToNextSend;
		// are we in high speed mode (still have high speed entries to handle ?)
		if (inHighSpeedMode) {
			// we reduced the high speed count and reduced the duration offset to allow for
			// the number of high speed delays remaining earlier
			sleepToNextSend = highSpeedDelay;
		} else {
			// in real time mode, work out how long to wait based on the next and current
			// item timestamps
			sleepToNextSend = Duration.between(lastEntryRead.getRecordTimestamp(), nextEntryRead.getRecordTimestamp());
		} // check for a negative sleep, should not happen, but let's be defensive
		if (sleepToNextSend.isNegative()) {
			log.warning("just processed entry (" + lastEntryRead + ") is after next entry (" + nextEntryRead
					+ ") for mow making it zero");
			sleepToNextSend = Duration.ZERO;
		}
		// move the loaded recorded data to be saved ready for the next run through
		lastEntryRead = nextEntryRead;
		final Duration finalSleepToNextSend = sleepToNextSend;
		final RecordedData finalNextEntryRead = nextEntryRead;
		// schedule the next run
		log.info(() -> "Scheduling next upload in " + finalSleepToNextSend + " which will send " + finalNextEntryRead);
		executor.schedule(this, sleepToNextSend.toNanos(), TimeUnit.NANOSECONDS);

	}

	private void updateGatewayHAStats(RecordedData recordedData, Duration timestampsDelta) {
		// deserialize it
		RecorderHomeAssistantEntityRetrieveStatusInfo recorderHomeAssistantEntityRetrieveStatusInfo;
		try {
			recorderHomeAssistantEntityRetrieveStatusInfo = mapper.readValue(recordedData.getData(),
					RecorderHomeAssistantEntityRetrieveStatusInfo.class);
		} catch (IOException e) {
			log.warning("Unable to deserialize " + recordedData.getData()
					+ " to RecorderHomeAssistantEntitySetInfo due to " + e.getLocalizedMessage());
			return;
		}
		// if in real time mode then apply the current time, don't alter it,
		// if however we're in high speed replay then adjust to the timestamp it woudl
		// have been based
		// on the offset
		// there is no payload time that we need to care about
		Instant haInteractionInstant = inHighSpeedMode ? Instant.now().minus(timestampsDelta) : Instant.now();
		if (recorderHomeAssistantEntityRetrieveStatusInfo
				.getRetrieveStatus() == HomeAssistantEntityRetrieveStatus.RETRIEVED) {
			log.info(() -> "Recording sucessfull HA retrieve for " + recorderHomeAssistantEntityRetrieveStatusInfo);
			gatewayStatsTrackingData.trackSucessfullHARetrieveCall(null, null, haInteractionInstant);
		} else {
			log.info(() -> "Recording failed HA retrieve for " + recorderHomeAssistantEntityRetrieveStatusInfo);
			gatewayStatsTrackingData.trackFailedHARetrieveCall(null, null, null, haInteractionInstant);
		}
	}

	private void sentGatewayStatsAndReset(RecordedData recordedData, Duration timestampsDelta) {
		// if we are not in high speed mode then the switch to real time will have
		// started the timers there, so we don't need to set the data any more.
		// we will still update thw HA success and fail though
		if (!inHighSpeedMode) {
			return;
		}
		IoTGatewayStatsData ioTGatewayStatsData;
		try {
			ioTGatewayStatsData = mapper.readValue(recordedData.getData(), IoTGatewayStatsData.class);
		} catch (IOException e) {
			log.warning("Unable to deserialize " + recordedData.getData() + " to IoTGatewayStatsData due to "
					+ e.getLocalizedMessage());
			return;
		}
		GatewayStatsData gatewayStatsData = ioTGatewayStatsData.getPayload();
		// need to retrieve the upload stats as the saved ones will not be relevant
		// (and will prob be zero as the upload was written to a file, not the IoT
		// service
		gatewayStatsData.setUploadsuccess(
				gatewayStatsTrackingData.getSucessfullUploadCalls().averageCalls(sucessfullUploadWindow));
		gatewayStatsData
				.setUploadfail(gatewayStatsTrackingData.getFailedUploadCalls().averageCalls(failedUploadWindow));
		// work out the time stamp to put in as the data field when uploading
		ZonedDateTime newUploadZDT = Instant.now().minus(timestampsDelta).atZone(UTCTZ);
		gatewayStatsData.setTimestamp(newUploadZDT);

		gatewayStatsDataUploader.publishGatewayStats(gatewayStatsData);
		// reset all the stats to allow for the upload to be reset as well
		gatewayStatsTrackingData.resetHAStats();
		log.info("Published and then reset gateway stats " + gatewayStatsData);
	}

	private void sendGatewayConfig(RecordedData recordedData, Duration timestampsDelta) {
		// if we are not in high speed mode then the switch to real time will have
		// started the timers there, so we don't need to set the data any more.
		if (!inHighSpeedMode) {
			return;
		}
		IoTGatewayConfigData ioTGatewayConfigData;
		try {
			ioTGatewayConfigData = mapper.readValue(recordedData.getData(), IoTGatewayConfigData.class);
		} catch (IOException e) {
			log.warning("Unable to deserialize " + recordedData.getData() + " to IoTGatewayConfigData due to "
					+ e.getLocalizedMessage());
			return;
		}
		GatewayConfigData gatewayConfigData = ioTGatewayConfigData.getPayload();

		// work out the time stamp to put in as the data field when uploading
		ZonedDateTime newUploadZDT = Instant.now().minus(timestampsDelta).atZone(UTCTZ);
		gatewayConfigData.setTimestamp(newUploadZDT);

		gatewayConfigDataUploader.publishGatewayConfig(gatewayConfigData);
		log.info("Published the gateway config " + gatewayConfigData);
	}

	private void uploadHomeAssistantEntity(RecordedData recordedData, Duration timestampsDelta) {
		// deserialize it
		RecorderHomeAssistantEntitySetInfo entitySetInfo;
		try {
			entitySetInfo = mapper.readValue(recordedData.getData(), RecorderHomeAssistantEntitySetInfo.class);
		} catch (IOException e) {
			log.warning("Unable to deserialize " + recordedData.getData()
					+ " to RecorderHomeAssistantEntitySetInfo due to " + e.getLocalizedMessage());
			return;
		}
		// try to get the entity set that relates to the record, we are looking by name
		// (that's the name given in the config file) that way we can handle generated
		// deviceKeys or changed deviceKeys
		HomeAssistantMonitoredEntitySet entitySet = entitySetNameToHomeAssistantMonitoredEntitySet
				.get(entitySetInfo.getEntitySetName());
		if (entitySet == null) {
			log.warning("Cannot find HomeAssistantMonitoredEntitySet named " + entitySetInfo.getEntitySetName()
					+ ", unable to process saved entity set upload of " + entitySetInfo);
			return;
		}
		Map<String, Object> payload = entitySetInfo.getIoTEntityData().getPayload();
		String payloadZDTString;
		try {
			payloadZDTString = (String) payload.get(IoTEntityData.TIMESTAMP_FIELD_NAME);
			if (payloadZDTString == null) {
				log.warning("Payload does not have a timestamp, cannot process it, " + entitySetInfo);
				return;
			}
		} catch (ClassCastException e) {
			log.severe("For some reason the stored payload in entitySetInfo is not a String, it's "
					+ payload.get(IoTEntityData.TIMESTAMP_FIELD_NAME).getClass() + " here's the payload map "
					+ payload);
			return;
		}
		// try to convert the string to the ZonedDateTime, use the formatter that was
		// used to save it to a string
		ZonedDateTime payloadZDT;
		try {
			payloadZDT = ZonedDateTime.parse(payloadZDTString, HomeAssistantMonitoredEntitySet.formatter);
		} catch (DateTimeParseException e) {
			log.warning("The payload field named " + IoTEntityData.TIMESTAMP_FIELD_NAME + " contains a string of "
					+ payloadZDTString + ", but it does not format using format string "
					+ HomeAssistantMonitoredEntitySet.PAYLOAD_TIMESTAMP_FORMAT_STRING
					+ " cannot upload this Home Assistant entity");
			return;
		}
		Duration recordedVsOriginatorDuration = Duration.between(payloadZDT, recordedData.getRecordTimestamp());
		ZonedDateTime currentZDT = ZonedDateTime.now(UTCTZ);
		ZonedDateTime recalculatedPayloadZDT;
		if (inHighSpeedMode) {
			// if we are in high speed mode then we need to alter the payload timestamp by
			// the delta
			ZonedDateTime recordedZDT = recordedData.getRecordTimestamp().plus(timestampsDelta);
			recalculatedPayloadZDT = recordedZDT.minus(recordedVsOriginatorDuration);
			log.info(() -> "In hs mode, origional recording was " + recordedData.getRecordTimestamp().format(formatter)
					+ " with the payload ZDT of " + payloadZDT.format(formatter) + " Given the current ZDT of "
					+ currentZDT.format(formatter) + " and it's offset of " + timestampsDelta
					+ " that means that the 'recorded timestamp should be " + recordedZDT.format(formatter)
					+ " and given the recorded vs payload delta" + recordedVsOriginatorDuration
					+ " that means the new payload ZDT will be " + recalculatedPayloadZDT.format(formatter));
		} else {
			// if in real time mode we need to calculate the difference between the recorded
			// data ts and the payload ts, then apply that to the current time to get the
			// payload ts
			recalculatedPayloadZDT = currentZDT.minus(recordedVsOriginatorDuration);
			log.info(() -> "In rt mode, origional recording was " + recordedData.getRecordTimestamp().format(formatter)
					+ " with the payload ZDT of " + payloadZDT.format(formatter) + " which is "
					+ recordedVsOriginatorDuration + " apart. Given the current ZDT of " + currentZDT.format(formatter)
					+ " that means the new payload ZDT will be " + recalculatedPayloadZDT.format(formatter));
		}
		payload.put(IoTEntityData.TIMESTAMP_FIELD_NAME,
				recalculatedPayloadZDT.format(HomeAssistantMonitoredEntitySet.formatter));
		log.info("Sending to entity set named " + entitySet.getName() + " payload " + payload);
		homeAssistantEntityUploadHandler.upload(payload, entitySet);
	}

	public RecordedData readRecordedDataLine() throws RecordedDataFormatException {
		String line;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			log.warning("IOException while reading recorded data, " + e.getLocalizedMessage());
			return null;
		}
		if (line == null) {
			log.info("Reached the end of the file");
			return null;
		}
		try {
			return mapper.readValue(line, RecordedData.class);
		} catch (IOException e) {
			log.warning("IOException while deserializing recorded data, " + e.getLocalizedMessage());
			throw new RecordedDataFormatException(
					"IOException while deserializing recorded data, " + e.getLocalizedMessage(), e);
		}
	}

	/*
	 * look forwards until either we hit end of file OR
	 */
	public int scanDatafileForwardsUntilPastTimestamp(ZonedDateTime stopAtTs, boolean onlyCountUploadableEntries)
			throws RecordedDataFormatException {
		// if there is a read entry and it's after the specified time then no need to
		// read anything else
		if ((lastEntryRead != null) && (lastEntryRead.getRecordTimestamp().isAfter(stopAtTs))) {
			return 0;
		}

		int i = 0;
		while (true) {
			// try and read an entry, if it's null we hit EOF
			lastEntryRead = readRecordedDataLine();
			if (lastEntryRead == null) {
				return i;
			}
			// we got something, increment the counter and test to see if it's after the
			// specified time
			if (onlyCountUploadableEntries) {
				if (lastEntryRead.getRecordedDataType().isUploadedToIoT()) {
					i++;
				}
			} else {
				i++;
			}
			if (lastEntryRead.getRecordTimestamp().isAfter(stopAtTs)) {
				return i;
			}
		}
	}
}
