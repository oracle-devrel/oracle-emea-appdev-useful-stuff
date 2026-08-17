package com.oracle.demo.timg.iot.iotproxygateway.mqtt;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStats;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.ToString;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = PropertyNames.MQTT_CLIENT_UPLOAD_ENABLED, value = "true", defaultValue = "false")

public class MqttHomeAssistantEntityHandler implements HomeAssistantEntityHandler {

	@Inject
	private GatewayStats gatewayStats;
	private final String topicBase;
	@ToString.Exclude
	private final MqttHomeAssistantEntityPublisher mqttHomeAssistantEntityPublisher;
	@ToString.Exclude
	private final ObjectMapper mapper;

	@Inject
	public MqttHomeAssistantEntityHandler(
			Optional<MqttHomeAssistantEntityPublisher> mqttHomeAssistantEntityPublisherOptional, ObjectMapper mapper,
			@Property(name = PropertyNames.GATEWAY_BASE_ENDPOINT, defaultValue = "house/homeassistant") String endpointBase,
			@Property(name = PropertyNames.GATEWAY_ENTITIES_ENDPOINT, defaultValue = "entities") String endpointEntities) {
		log.info("Constructing MqttUploadHandler");
		if (mqttHomeAssistantEntityPublisherOptional.isEmpty()) {
			log.info("Uploads to MQTT are turned off " + PropertyNames.MQTT_CLIENT_UPLOAD_ENABLED);
			mqttHomeAssistantEntityPublisher = null;
		} else {
			this.mqttHomeAssistantEntityPublisher = mqttHomeAssistantEntityPublisherOptional.get();
		}
		this.mapper = mapper;
		this.topicBase = endpointBase + "/" + endpointEntities;
	}

	@PostConstruct
	void postConstruct() {
		log.info("mqtt entity uploader configued with topic prefix " + topicBase);
	}

	@Override
	public void upload(Map<String, Object> ioTCoreEvent, HomeAssistantMonitoredEntitySet entity) {
		IoTEntityData ioTEntityData = IoTEntityData.builder().devicekey(entity.getDevicekey()).payload(ioTCoreEvent)
				.build();
		String mappedToJson;
		try {
			mappedToJson = mapper.writeValueAsString(ioTEntityData);
		} catch (IOException e) {
			log.severe("Error converting to Json, this should not have happened, " + e.getLocalizedMessage());
			return;
		}
		String topic = topicBase + "/" + entity.getEndpoint();
		log.info("Sending to topic " + topic + ", mapped event as json is " + mappedToJson);
		// need to get the right topic details to match against the expected IoT topic

		// try and sent it
		if (entity.getDoupload()) {
			try {
				if (mqttHomeAssistantEntityPublisher != null) {
					mqttHomeAssistantEntityPublisher.publishHomeAssistantData(topic, mappedToJson);
					gatewayStats.trackSucessfullUploadCall();
				} else {
					log.info("No mqtt uploader set for entity " + entity.getName() + " for data " + mappedToJson);
				}
			} catch (Exception e) {
				log.severe("Exception uploading event (" + mappedToJson + ") to Iot, " + e.getLocalizedMessage());
				gatewayStats.trackFailedUploadCall();
			}
		} else {
			log.info("Uploads disabled for entity " + entity.getName() + " for data " + mappedToJson);
		}
	}

}
