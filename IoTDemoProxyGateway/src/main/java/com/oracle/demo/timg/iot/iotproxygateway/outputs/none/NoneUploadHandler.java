package com.oracle.demo.timg.iot.iotproxygateway.outputs.none;

import java.util.Map;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.HomeAssistantEntityUploadHandler;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "NONE")

public class NoneUploadHandler implements HomeAssistantEntityUploadHandler {

	@Override
	public void upload(Map<String, Object> ioTCoreEvent,
			HomeAssistantMonitoredEntitySet homeAssistantMonitoredEntitySet) {
		log.info("None Publishing from home assistant entity set " + homeAssistantMonitoredEntitySet.getName()
				+ " with key " + homeAssistantMonitoredEntitySet.getDevicekey() + " with data " + ioTCoreEvent);
	}

	@Override
	public String getName() {
		return "NoneUploadHandler";
	}

}
