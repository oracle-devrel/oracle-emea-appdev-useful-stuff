package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@Serdeable
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IoTPowerKiloWattsEvent extends IoTCoreEvent {
	private double kiloWatts;
}
