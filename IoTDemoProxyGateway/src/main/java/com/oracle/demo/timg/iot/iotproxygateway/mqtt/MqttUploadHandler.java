package com.oracle.demo.timg.iot.iotproxygateway.mqtt;

import java.io.IOException;

import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTCoreEvent;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.ToString;
import lombok.extern.java.Log;

@Singleton
@Log
public class MqttUploadHandler {
	@ToString.Exclude
	private final MqttEventPublisher mqttEventPublisher;
	@ToString.Exclude
	private final ObjectMapper mapper;

	@Inject
	public MqttUploadHandler(MqttEventPublisher mqttEventPublisher, ObjectMapper mapper) {
		log.info("in MqttUploadHandler");
		this.mqttEventPublisher = mqttEventPublisher;
		this.mapper = mapper;
	}

	public void upload(IoTCoreEvent ioTCoreEvent) {
		String mappedToJson;
		try {
			mappedToJson = mapper.writeValueAsString(ioTCoreEvent);
		} catch (IOException e) {
			log.severe("Error converting to Json, this should not have happened, " + e.getLocalizedMessage());
			return;
		}
		log.info("Mapped event as json is " + mappedToJson);
		// need to get the right topic details to match against the expected IoT topic
	}
}
