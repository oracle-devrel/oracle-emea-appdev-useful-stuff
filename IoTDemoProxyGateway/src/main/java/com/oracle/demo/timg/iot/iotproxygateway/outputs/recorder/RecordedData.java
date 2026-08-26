package com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Serdeable
@Data
@Builder
public class RecordedData {
	// get the UTC TZ once to speed things later
	@JsonIgnore
	public final static ZoneId UTCTZ = ZoneId.of("UTC");
	@JsonIgnore
	public final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX");
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime recordTimestamp = ZonedDateTime.now(UTCTZ);
	private RecordedDataType recordedDataType;
	private String data;
}
