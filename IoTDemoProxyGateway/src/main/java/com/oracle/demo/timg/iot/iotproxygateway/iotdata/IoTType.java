/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
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
	AIR_QUALITY_CO2_PPM("-1", "co2ppm"), AIR_QUALITY_NOX_INDEX("-1", "noxindex"),
	AIR_QUALITY_VOC_INDEX("-1", "vocindex"), AIR_QUALITY_PM_2_5("-1", "pm2_5_ugm3"), AIR_PRESSURE("-1", "millibar"),
	BINARY_ON_OFF("off", "boolean", "on"), BOOLEAN("false", "boolean"), COST("0", "cost"),
	ENERGY_KILO_WATT_HOURS("-999999", "kilowatthours"), ENERGY_WATT_HOURS("-999999", "watthours"),
	LUMINANCE("-1", "lux"), MATTER_DOOR("Closed", "door", "Open"), MATTER_OCCUPANCY("off", "occupied", "on"),
	MATTER_WINDOW("Closed", "window", "Open"), NUMBER("-999999", "number"), PERCENT("0", "percent"),
	POWER_WATTS("-999999", "watts"), POWER_KILO_WATTS("-999999", "kilowatts"),
	RELATIVE_HUMIDITY("-1", "relativehumidity"), SWITCH("off", "switch", "On"), STRING("Unavailable", "stringvalue"),
	TEMPERATURE("-274", "temperature");

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
		case STRING:
			return hastate.getState();
		case BINARY_ON_OFF:
		case MATTER_DOOR:
		case MATTER_WINDOW:
		case MATTER_OCCUPANCY:
		case SWITCH: {
			return hastate.getStateStringMatchesValue(unavailableDefault, valueToReturnTrue);
		}
		case BOOLEAN: {
			return hastate.getStateAsBoolean(unavailableDefault);
		}
		case AIR_PRESSURE:
		case AIR_QUALITY_CO2_PPM:
		case AIR_QUALITY_NOX_INDEX:
		case AIR_QUALITY_VOC_INDEX:
		case AIR_QUALITY_PM_2_5:
		case COST:
		case ENERGY_KILO_WATT_HOURS:
		case ENERGY_WATT_HOURS:
		case LUMINANCE:
		case NUMBER:
		case PERCENT:
		case POWER_WATTS:
		case POWER_KILO_WATTS:
		case RELATIVE_HUMIDITY:
		case TEMPERATURE: {
			return hastate.getStateAsDouble(unavailableDefault);
		}
		default: {
			throw new UnsupportedOperationException(
					"Support for IoT type " + this + " is not yet implemented, cannot get the value for it");
		}
		}
	}
}
