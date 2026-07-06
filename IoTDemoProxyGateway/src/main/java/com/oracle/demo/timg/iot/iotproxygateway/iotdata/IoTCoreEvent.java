package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import java.time.ZonedDateTime;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@Serdeable
@SuperBuilder
public class IoTCoreEvent {
	private ZonedDateTime timestamp;
}
