package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;
import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mqtt.annotation.Topic;
import io.micronaut.mqtt.annotation.v5.MqttPublisher;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

@MqttPublisher
@Requires(property = MqttDeviceSettings.PREFIX + ".id")
public interface MqttSonnenBatteryPublisher {
	@Topic("house/sonnen/configuration/${" + MqttDeviceSettings.PREFIX + ".id}")
	@ExecuteOn(TaskExecutors.IO)
	public CompletableFuture<Void> publishSonnenConfiguration(SonnenConfiguration data);

	@Topic("house/sonnen/status/${" + MqttDeviceSettings.PREFIX + ".id}")
	@ExecuteOn(TaskExecutors.IO)
	public CompletableFuture<Void> publishSonnenStatus(SonnenStatus data);
}
