package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;
import com.oracle.demo.timg.iot.iotproxygateway.mqtt.MqttGatewayEventPublisher;

import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
public class GatewayDataUploader {
	@Inject
	private GatewayStats gatewayStats;
	@Inject
	private MqttGatewayEventPublisher gatewayEventPublisher;

	@PostConstruct
	void postConstruct() {
		log.info("GatewayDataUploader starting operation");
	}

	/*
	 * have this use the micronaut scheduler, it's easier
	 */
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_CONFIG + ":1200s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_CONFIG_INITIAL_DELAY + ":5s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processConfiguration() {
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

	/*
	 * have this use the micronaut scheduler, it's easier
	 */
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_STATS_PUBLISH_RATE + ":120s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_STATS_INITIAL_DELAY + ":30s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processStats() {
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
