package com.oracle.demo.timg.iot.iotproxygateway.recorder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum RecordedDataType {
	ENTITY(true), HA_RETRIEVE(false), GATEWAY_STATS_RESET_AND_SEND(true), GATEWAY_CONFIG_SEND(true);

	@Getter
	private final boolean isUploadedToIoT;

}
