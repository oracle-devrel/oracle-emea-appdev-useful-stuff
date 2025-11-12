package com.oracle.demo.timg.iot.iotsonnenuploader.commanddata;

import java.time.ZonedDateTime;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Serdeable
@Data
@Builder
public class CommandResponse {
	private ZonedDateTime cmdReceived;
	private ZonedDateTime cmdActioned;
	private int cmdLength;
	private boolean cmdStatus;
	private String cmdResponse;
}
