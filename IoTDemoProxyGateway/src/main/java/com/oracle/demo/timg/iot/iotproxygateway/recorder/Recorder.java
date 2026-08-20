package com.oracle.demo.timg.iot.iotproxygateway.recorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantEntityRetrieveStatus;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntity;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "RECORDER")

public class Recorder {
	private final static String DTG_FORMAT = "uuuu-MM-dd'T'HH-mm-ss.SSSSSS-";
	private final ObjectMapper mapper;
	private final String outputLocation;
	private final Instant stopRecordingAt;
	private final boolean exitAfterRecordingStop;
	private BufferedWriter writer;
	private boolean active = false;

	@Inject
	public Recorder(ObjectMapper mapper,
			@Property(name = PropertyNames.RECORD_OUTPUT_FILE, defaultValue = PropertyNames.RECORD_OUTPUT_FILE_DEFAULT) String outputFileName,
			@Property(name = PropertyNames.RECORD_OUTPUT_DIRECTORY, defaultValue = PropertyNames.RECORD_OUTPUT_DIRECTORY_DEFAULT) String outputDirectory,
			@Property(name = PropertyNames.RECORD_OUTPUT_PREFIX_WITH_DTG, defaultValue = "true") boolean prefixwithdtg,
			@Property(name = PropertyNames.RECORD_DURATION, defaultValue = "60m") Duration recordDuration,
			@Property(name = PropertyNames.RECORD_EXIT_AFTER_RECORDING_STOP, defaultValue = "true") boolean exitAfterRecordingStop)
			throws IOException {
		this.mapper = mapper;
		String prefix = prefixwithdtg ? LocalDateTime.now().format(DateTimeFormatter.ofPattern(DTG_FORMAT)) : "";
		this.outputLocation = outputDirectory + File.separator + prefix + outputFileName;
		try {
			this.writer = new BufferedWriter(new FileWriter(this.outputLocation));
			log.info("Recording to " + this.outputLocation);
		} catch (IOException e) {
			log.info("Can't open recorder output file " + outputFileName + " because " + e.getLocalizedMessage());
			throw e;
		}
		log.info("Will stop recording in " + recordDuration);
		this.stopRecordingAt = Instant.now().plus(recordDuration);
		this.exitAfterRecordingStop = exitAfterRecordingStop;
		active = true;
	}

	private void recordData(@NonNull RecordedDataType type, @NonNull String jsonData) {
		if (!active) {
			log.info("Data recording is inactive, but would have recorded type " + type + " with data " + jsonData);
			return;
		} else {
			// check the timestamps
			if (Instant.now().isAfter(stopRecordingAt)) {
				log.info("Current time is after stop recording time, stopping");
				// stop the recording and close the output
				stopRecording();
				// do we finish processing at this point ?
				if (exitAfterRecordingStop) {
					log.info("Been asked to stop the process after stoppign recording, shutting down the JVM");
					System.exit(0);
				}
			}
			log.info("About to record type " + type + " with data " + jsonData);
			RecordedData recordedData = RecordedData.builder().recordedDataType(type).data(jsonData).build();
			String recordedDataAsString;
			try {
				recordedDataAsString = mapper.writeValueAsString(recordedData);
			} catch (IOException e) {
				log.warning(
						"Unable to map recorded data due to " + e.getLocalizedMessage() + ", data is " + recordedData);
				return;
			}
			log.info("recording data " + recordedDataAsString);
			try {
				// synchronized to make sure it's MT safe
				synchronized (writer) {
					writer.write(recordedDataAsString);
					writer.newLine();
				}
			} catch (IOException e) {
				log.warning("Exception recording data to file due to " + e.getLocalizedMessage() + ", data is "
						+ recordedDataAsString);
			}
		}
	}

	public void recordIoTEntityData(@NonNull IoTEntityData ioTEntityData) {
		log.info("Recording IoTEntityData " + ioTEntityData);
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(ioTEntityData);
		} catch (IOException e) {
			log.warning("Problem serialising IoTEntityData to json data, " + e.getLocalizedMessage() + ", entity is "
					+ ioTEntityData);
			return;
		}
		recordData(RecordedDataType.ENTITY, jsonData);
	}

	public void stopRecording() {
		log.info("Stopping recording to " + outputLocation);
		this.active = false;
		try {
			if (writer != null) {
				this.writer.flush();
				this.writer.close();
				this.writer = null;
			}
		} catch (IOException e) {
			log.warning("Unable to close recording output file " + outputLocation
					+ " but no more data will be written, " + e.getLocalizedMessage());
			return;
		}
		log.info("Completed and closed recording to " + outputLocation);
	}

	public void recordSucessfullHARetrieveCall(@NonNull HomeAssistantMonitoredEntitySet entitySet,
			@NonNull HomeAssistantMonitoredEntity entity) {
		log.info("Recording sucessfull HA entity call for " + entity + " in set " + entitySet.getName());
		EntityInfo entityInfo = EntityInfo.builder().retrieveStatus(HomeAssistantEntityRetrieveStatus.RETRIEVED)
				.entitySetName(entitySet.getName()).entityid(entity.getEntityid()).build();
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(entityInfo);
		} catch (IOException e) {
			log.warning("Problem serialising Sucessfull HA call data to json data, " + e.getLocalizedMessage()
					+ ", entity info is " + entityInfo);
			return;
		}
		recordData(RecordedDataType.HA_RETRIEVE, jsonData);
	}

	public void recordFailedHARetrieveCall(@NonNull HomeAssistantEntityRetrieveStatus retrieveStatus,
			@NonNull HomeAssistantMonitoredEntitySet entitySet, @NonNull HomeAssistantMonitoredEntity entity) {
		log.info("Recording failed HA entity call due to " + retrieveStatus + " for " + entity + " in set "
				+ entitySet.getName());
		EntityInfo entityInfo = EntityInfo.builder().retrieveStatus(retrieveStatus).entitySetName(entitySet.getName())
				.entityid(entity.getEntityid()).build();
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(entityInfo);
		} catch (IOException e) {
			log.warning("Problem serialising failed HA call data to json data, " + e.getLocalizedMessage()
					+ ", entity info is " + entityInfo);
			return;
		}
		recordData(RecordedDataType.HA_RETRIEVE, jsonData);
	}

	public void recordGatewayStatsUploadEvent(IoTGatewayStatsData data) {
		log.info("Recording IoTGatewayStatsData " + data);
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(data);
		} catch (IOException e) {
			log.warning("Problem serialising IoTGatewayStatsData to json data, " + e.getLocalizedMessage()
					+ ", stats are " + data);
			return;
		}
		recordData(RecordedDataType.GATEWAY_STATS_RESET_AND_SEND, jsonData);
	}

	public void recordGatewayConfigUploadEvent(IoTGatewayConfigData data) {
		log.info("Recording IoTGatewayConfigData " + data);
		String jsonData;
		try {
			jsonData = mapper.writeValueAsString(data);
		} catch (IOException e) {
			log.warning("Problem serialising IoTGatewayConfigData to json data, " + e.getLocalizedMessage()
					+ ", config is " + data);
			return;
		}
		recordData(RecordedDataType.GATEWAY_CONFIG_SEND, jsonData);
	}

	@PreDestroy
	public void preDestroy() {
		log.info("In recorder preDestroy");
		if ((active) || (writer != null)) {
			stopRecording();
		}
	}
}
