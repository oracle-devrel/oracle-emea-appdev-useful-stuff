package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Serdeable
@Builder
public class IoTEntityData {
	@ToString.Exclude
	@JsonIgnore
	public final static String TIMESTAMP_FIELD_NAME = "timestamp";
	private String devicekey;
	private Map<String, Object> payload;
}
