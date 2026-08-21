package com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder;

import java.util.Map;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.HomeAssistantEntityUploadHandler;

import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "RECORDER")
public class RecorderUploadHandler implements HomeAssistantEntityUploadHandler {
	private final Recorder recorder;

	@Inject
	public RecorderUploadHandler(Recorder recorder) {
		log.info("RecorderUploadHandler In constructor");
		this.recorder = recorder;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("RecorderUploadHandler In post construct");

	}

	@Override
	public void upload(Map<String, Object> ioTCoreEvent, HomeAssistantMonitoredEntitySet entity) {
		IoTEntityData ioTEntityData = IoTEntityData.builder().devicekey(entity.getDevicekey()).payload(ioTCoreEvent)
				.build();
		recorder.recordIoTEntityData(ioTEntityData);
	}

	@Override
	public String getName() {
		return "RecorderUploadHandler";
	}

}
