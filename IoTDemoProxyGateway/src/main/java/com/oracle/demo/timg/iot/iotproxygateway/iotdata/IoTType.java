package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntity;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantState;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public enum IoTType {
	LUMINANCE("-1", "lux"), POWER_WATTS("-999999", "watts"), POWER_KILO_WATTS("-999999", "kilowatts");

	@Getter
	@NonNull
	private String unavailableDefault;

	@Getter
	@NonNull
	private String defaultFieldName;

	public String getFieldName(HomeAssistantMonitoredEntity entity) {
		if (entity.getFieldname() == null) {
			return defaultFieldName;
		} else {
			return entity.getFieldname();
		}
	}

	public Object createObjectFrom(HomeAssistantState hastate) {
		switch (this) {
		case LUMINANCE: {
			return hastate.getStateAsDouble(unavailableDefault);
		}
		case POWER_WATTS: {
			return hastate.getStateAsDouble(unavailableDefault);
		}
		case POWER_KILO_WATTS: {
			return hastate.getStateAsDouble(unavailableDefault);
		}
		default: {
			throw new UnsupportedOperationException(
					"Support for IoT type " + this + " is not yet implemented, cannot get the value for it");
		}
		}
	}
}
