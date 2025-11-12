package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import java.util.concurrent.CompletableFuture;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mqtt.annotation.Topic;
import io.micronaut.mqtt.annotation.v5.MqttPublisher;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

@MqttPublisher
@Requires(property = MqttDeviceSettings.PREFIX + ".id")
public interface MqttCommandResponsePublisher {
	@Topic("house/sonnen/commandresponse/${" + MqttDeviceSettings.PREFIX + ".id}")
	@ExecuteOn(TaskExecutors.IO)
	public CompletableFuture<Void> publishCommandResponse(byte[] responsebytes);
}
