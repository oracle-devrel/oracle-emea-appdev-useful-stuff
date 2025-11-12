package com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Serdeable
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonnenStatus {
	@JsonProperty("BackupBuffer")
	private int reservedBatteryCapacity;
	@JsonProperty("BatteryCharging")
	private boolean batteryCharging;
	@JsonProperty("BatteryDischarging")
	private boolean batteryDischarging;
	@JsonProperty("Consumption_Avg")
	private int consumptionAvgLastMinute;
	@JsonProperty("Consumption_W")
	private int consumptionPointInTime;
	@JsonProperty("GridFeedIn_W")
	private int gridConsumption;
	@JsonProperty("OperatingMode")
	private int operatingMode;
	@JsonProperty("Production_W")
	private int solarProduction;

}
