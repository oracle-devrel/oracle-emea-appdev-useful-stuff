package com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Serdeable
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SonnenConfiguration {
	@JsonProperty("EM_USOC")
	private int reservedBatteryCapacity;
	@JsonProperty("EM_OperatingMode")
	private int operatingMode;
	@JsonProperty("EM_ToU_Schedule")
	private String touSchedule;
	@JsonProperty("DE_Software")
	private String softwareVersion;

}
