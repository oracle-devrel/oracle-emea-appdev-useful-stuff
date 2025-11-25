package com.oracle.demo.timg.iot.iotsonnenuploader.sonnendatacapture;

import java.time.Duration;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCaptureConfig {
	private LocalDateTime starttime;
	private Duration duration;
}
