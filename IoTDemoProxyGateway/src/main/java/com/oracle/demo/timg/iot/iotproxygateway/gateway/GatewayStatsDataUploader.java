package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import java.util.Optional;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;
import com.oracle.demo.timg.iot.iotproxygateway.mqtt.MqttGatewayEventPublisher;

import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Requires(property = PropertyNames.GATEWAY_STATS_PUBLISH_ENABLED, value = "true", defaultValue = "true")
@Singleton
@Log
public class GatewayStatsDataUploader {
	@Inject
	private GatewayStats gatewayStats;
	private MqttGatewayEventPublisher gatewayEventPublisher;

	@Inject
	public GatewayStatsDataUploader(Optional<MqttGatewayEventPublisher> gatewayEventPublisherOpt) {
		if (gatewayEventPublisherOpt.isEmpty()) {
			log.warning("gatewayEventPublisher not found, gateway stats data will not be uploaded");
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
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_STATS_PUBLISH_RATE + ":120s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_STATS_INITIAL_DELAY + ":30s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processStats() {
		// in theory this should not happen unless someone calls the method directly,
		// but let's do some defensive programming
		if (gatewayEventPublisher == null) {
			return;
		}
		GatewayStatsData gatewayStatsData = gatewayStats.getGatewayStatsData();
		IoTGatewayStatsData ioTGatewayStatsData = IoTGatewayStatsData.builder().payload(gatewayStatsData).build();
		log.info("Publishing gateway stats " + ioTGatewayStatsData);
		try {
			gatewayEventPublisher.publishGatewayStats(ioTGatewayStatsData);
			gatewayStats.trackSucessfullUploadCall();
		} catch (Exception e) {
			log.warning("Exception uploading gateway stats " + ioTGatewayStatsData + ", " + e.getLocalizedMessage());
			gatewayStats.trackFailedUploadCall();
		}
	}

}
