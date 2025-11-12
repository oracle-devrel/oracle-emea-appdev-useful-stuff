package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import lombok.Data;

@ConfigurationProperties(MqttDeviceSettings.PREFIX)
@Requires(property = MqttDeviceSettings.PREFIX + ".id")
@Data
public class MqttDeviceSettings {
	public static final String PREFIX = "mqtt.device";
	private String id;
}