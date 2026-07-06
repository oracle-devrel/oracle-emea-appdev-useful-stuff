package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantState;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum IoTType {
	LUMINANCE("luminance"), POWER_WATTS("power_watts"), POWER_KILO_WATTS("power_watts");

	@Getter
	private String iotUploadPath;

	public IoTCoreEvent createEventFrom(HomeAssistantState hastate) {
		return switch (this) {
		case LUMINANCE ->
			IoTLuminanceEvent.builder().lux(hastate.getStateAsDouble()).timestamp(hastate.getLast_updated()).build();
		case POWER_WATTS ->
			IoTPowerWattsEvent.builder().watts(hastate.getStateAsDouble()).timestamp(hastate.getLast_updated()).build();
		case POWER_KILO_WATTS -> IoTPowerKiloWattsEvent.builder().kiloWatts(hastate.getStateAsDouble())
				.timestamp(hastate.getLast_updated()).build();
		default -> throw new UnsupportedOperationException("Support for IoT type " + this + " is not yet implemented");
		};
	}
}
