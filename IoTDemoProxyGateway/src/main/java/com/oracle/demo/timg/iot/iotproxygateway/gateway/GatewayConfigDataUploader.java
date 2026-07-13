package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import java.util.Optional;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.mqtt.MqttGatewayEventPublisher;

import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Requires(property = PropertyNames.GATEWAY_CONFIG_PUBLISH_ENABLED, value = "true", defaultValue = "true")
@Singleton
@Log
public class GatewayConfigDataUploader {
	@Inject
	private GatewayStats gatewayStats;
	private final MqttGatewayEventPublisher gatewayEventPublisher;

	@Inject
	public GatewayConfigDataUploader(Optional<MqttGatewayEventPublisher> gatewayEventPublisherOpt) {
		if (gatewayEventPublisherOpt.isEmpty()) {
			log.warning("gatewayEventPublisher not found, gateway config data will not be uploaded");
			gatewayEventPublisher = null;
			return;
		} else {
			gatewayEventPublisher = gatewayEventPublisherOpt.get();
		}
	}

	@PostConstruct
	void postConstruct() {
		log.info("GatewayConfigDataUploader starting operation");
	}

	/*
	 * have this use the micronaut scheduler, it's easier
	 */
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_CONFIG + ":1200s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_CONFIG_INITIAL_DELAY + ":5s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processConfiguration() {
		// in theory this should not happen unless someone calls the method directly,
		// but let's do some defensive programming
		if (gatewayEventPublisher == null) {
			return;
		}
		GatewayConfigData gatewayConfigData = gatewayStats.getGatewayConfigData();
		IoTGatewayConfigData ioTGatewayConfigData = IoTGatewayConfigData.builder().payload(gatewayConfigData).build();
		log.info("Publishing gateway config " + ioTGatewayConfigData);
		try {
			gatewayEventPublisher.publishGatewayConfig(ioTGatewayConfigData);
			gatewayStats.trackSucessfullUploadCall();
		} catch (Exception e) {
			log.warning("Exception uploading gateway config " + ioTGatewayConfigData + ", " + e.getLocalizedMessage());
			gatewayStats.trackFailedUploadCall();
		}

	}
}
