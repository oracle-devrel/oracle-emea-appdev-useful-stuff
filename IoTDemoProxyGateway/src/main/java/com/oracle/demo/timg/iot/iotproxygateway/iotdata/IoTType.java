package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantState;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum IoTType {
	LUMINANCE("-1"), POWER_WATTS("-999999"), POWER_KILO_WATTS("-999999");

	@Getter
	private String unavailableDefault;

	public IoTCoreEvent createEventFrom(HomeAssistantState hastate) {
		switch (this) {
		case LUMINANCE: {
			return IoTLuminanceEvent.builder().lux(hastate.getStateAsDouble(unavailableDefault))
					.timestamp(hastate.getLast_updated()).build();
		}
		case POWER_WATTS: {
			return IoTPowerWattsEvent.builder().watts(hastate.getStateAsDouble(unavailableDefault))
					.timestamp(hastate.getLast_updated()).build();
		}
		case POWER_KILO_WATTS: {
			return IoTPowerKiloWattsEvent.builder().kiloWatts(hastate.getStateAsDouble(unavailableDefault))
					.timestamp(hastate.getLast_updated()).build();
		}
		default: {
			throw new UnsupportedOperationException("Support for IoT type " + this + " is not yet implemented");
		}
		}
	}
}
