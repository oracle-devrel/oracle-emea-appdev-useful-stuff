/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Serdeable
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonnenStatus {

	// This is a Jackson de-serialisation "cheat" is effectively allows us to have
	// two property names in the inbound JSON for reservedBatteryCapacity,
	// BackupBuffer is the one used by the sonnen and will be used when
	// de-serializing the JSON from the sonnen, but reservedBatteryCapacity will be
	// used when serializing and is also used in the default de-serializing process
	@JsonProperty(value = "BackupBuffer")
	public void setReservedBatteryCapacityPercentageFromSonnen(int reservedBatteryCapacityPercentage) {
		this.reservedBatteryCapacityPercentage = reservedBatteryCapacityPercentage;
	}

	// the default serialization / de-serialization will use reservedBatteryCapacity
	// for the JSON property name
	private int reservedBatteryCapacityPercentage;

	@JsonProperty(value = "RSOC")
	public void setCurrentBatteryCapacityFromSonnen(int currentBatteryCapacityPercentage) {
		this.currentBatteryCapacityPercentage = currentBatteryCapacityPercentage;
	}

	private int currentBatteryCapacityPercentage;

	@JsonProperty(value = "RemainingCapacity_W")
	public void setRemainingBatteryCapacityWattHoursFromSonnen(int remainingBatteryCapacityWattHours) {
		this.remainingBatteryCapacityWattHours = remainingBatteryCapacityWattHours;
	}

	private int remainingBatteryCapacityWattHours;

	// do this renaming so the generated JSON will follow a consistent
	// capitalization pattern
	@JsonProperty("BatteryCharging")
	public void setBatteryChargingFromSonnen(boolean batteryCharging) {
		this.batteryCharging = batteryCharging;
	}

	private boolean batteryCharging;

	@JsonProperty("BatteryDischarging")
	public void setBatteryDischargingFromSonnen(boolean batteryDischarging) {
		this.batteryDischarging = batteryDischarging;
	}

	private boolean batteryDischarging;

	@JsonProperty("Consumption_Avg")
	public void setConsumptionAvgLastMinuteFromSonnen(int consumptionAvgWattsLastMinute) {
		this.consumptionAvgWattsLastMinute = consumptionAvgWattsLastMinute;
	}

	private int consumptionAvgWattsLastMinute;

	@JsonProperty("Consumption_W")
	public void setConsumptionWattsPointInTimeFromSonnen(int consumptionWattsPointInTime) {
		this.consumptionWattsPointInTime = consumptionWattsPointInTime;
	}

	private int consumptionWattsPointInTime;

	@JsonProperty("GridFeedIn_W")
	// note that this is measuring feed in as a positive number, so if we want grid
	// consumption we need to invert if
	public void setGridFeedInWattsPointInTimeFromSonnen(int gridFeedInWattsPointInTime) {
		this.gridConsumptionWattsPointInTime = gridFeedInWattsPointInTime * -1;
	}

	private int gridConsumptionWattsPointInTime;

	@JsonProperty("OperatingMode")
	public void setOperatingMod(int operatingMode) {
		this.operatingMode = operatingMode;
	}

	private int operatingMode;

	@JsonProperty("Production_W")
	public void setProductionWattsPointInTimeFromSonnen(int consumptionWattsPointInTime) {
		this.consumptionWattsPointInTime = consumptionWattsPointInTime;
	}

	private int solarProductionWattsPointInTime;

}
