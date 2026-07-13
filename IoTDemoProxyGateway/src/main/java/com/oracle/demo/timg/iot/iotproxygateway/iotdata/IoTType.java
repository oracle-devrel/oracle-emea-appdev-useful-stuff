package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantMonitoredEntity;
import com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities.HomeAssistantState;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.java.Log;

@AllArgsConstructor
@Log
public enum IoTType {
	BOOLEAN("false", "boolean"), ENERGY_KILO_WATT_HOURS("-999999", "kilowatthours"),
	ENERGY_WATT_HOURS("-999999", "watthours"), LUMINANCE("-1", "lux"), MATTER_DOOR("Closed", "door", "Open"),
	MATTER_WINDOW("Closed", "window", "Open"), PERCENT("0", "percent"), POWER_WATTS("-999999", "watts"),
	POWER_KILO_WATTS("-999999", "kilowatts"), SWITCH("off", "switch", "On");

	@Getter
	@NonNull
	private String unavailableDefault;

	@Getter
	@NonNull
	private String defaultFieldName;

	@Getter
	private String valueToReturnTrue;

	private IoTType(String unavailableDefault, String defaultFieldName) {
		this.unavailableDefault = unavailableDefault;
		this.defaultFieldName = defaultFieldName;
	}

	public String getFieldName(HomeAssistantMonitoredEntity entity) {
		if (entity.getFieldname() == null) {
			return defaultFieldName;
		} else {
			return entity.getFieldname();
		}
	}

	public Object createObjectFrom(HomeAssistantState hastate) {
		switch (this) {
		case SWITCH:
		case MATTER_DOOR:
		case MATTER_WINDOW: {
			return hastate.getStateStringMatchesValue(unavailableDefault, valueToReturnTrue);
		}
		case BOOLEAN: {
			return hastate.getStateAsBoolean(unavailableDefault);
		}
		case ENERGY_KILO_WATT_HOURS:
		case ENERGY_WATT_HOURS:
		case LUMINANCE:
		case PERCENT:
		case POWER_WATTS:
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
