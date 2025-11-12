package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;
import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mqtt.annotation.MqttSubscriber;
import io.micronaut.mqtt.annotation.Topic;
import lombok.extern.java.Log;

@Log
@MqttSubscriber
@Requires(property = MqttDeviceSettings.PREFIX + ".id")
public class MqttUploadMonitor {
	@Topic("house/sonnen/configuration/${" + MqttDeviceSettings.PREFIX + ".id}")
	public void receiveConfig(SonnenConfiguration config) {
		log.info("Monitor recieved config " + config);
	}

	@Topic("house/sonnen/status/${" + MqttDeviceSettings.PREFIX + ".id}")
	public void receiveStatus(SonnenStatus status) {
		log.info("Monitor recieved status " + status);
	}
}
