package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Serdeable
@Builder
public class IoTEntityData {
	private String devicekey;
	private IoTCoreEvent payload;
}
