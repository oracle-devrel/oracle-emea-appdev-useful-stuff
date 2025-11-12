package com.oracle.demo.timg.iot.iotsonnenuploader.sonnencontroller;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import lombok.Data;

@ConfigurationProperties(SonnenBatteryHttpClientSettings.PREFIX)
@Requires(property = SonnenBatteryHttpClientSettings.PREFIX + ".authToken")
@Data
public class SonnenBatteryHttpClientSettings {
	public static final String PREFIX = "sonnenbattery";
	private String authToken;
}