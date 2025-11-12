package com.oracle.demo.timg.iot.iotsonnenuploader.uploader;

import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;
import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;
import com.oracle.demo.timg.iot.iotsonnenuploader.mqtt.MqttSonnenBatteryPublisher;
import com.oracle.demo.timg.iot.iotsonnenuploader.sonnencontroller.SonnenBatteryClient;

import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
public class Uploader {
	@Inject
	private SonnenBatteryClient client;
	@Inject
	private MqttSonnenBatteryPublisher mqttSonnenBatteryPublisher;

	@Scheduled(fixedRate = "120s", initialDelay = "5s")
	@ExecuteOn(TaskExecutors.IO)
	public SonnenConfiguration processConfiguration() {
		SonnenConfiguration conf = client.fetchConfiguration();
		log.info("Retrieved configuration from battery : " + conf);
		CompletableFuture<Void> publishResp = mqttSonnenBatteryPublisher.publishSonnenConfiguration(conf);
		publishResp.thenRun(() -> log.info("Published configuration as object"));
		return conf;
	}

	@Scheduled(fixedRate = "10s", initialDelay = "10s")
	@ExecuteOn(TaskExecutors.IO)
	public SonnenStatus processStatus() {
		SonnenStatus status = client.fetchStatus();
		log.info("Retrieved status from battery : " + status);
		CompletableFuture<Void> publishResp = mqttSonnenBatteryPublisher.publishSonnenStatus(status);
		publishResp.thenRun(() -> log.info("Published status as object"));
		return status;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received");
	}
}
