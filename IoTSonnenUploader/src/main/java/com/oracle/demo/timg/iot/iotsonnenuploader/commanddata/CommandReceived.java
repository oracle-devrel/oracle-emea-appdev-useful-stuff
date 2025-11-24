package com.oracle.demo.timg.iot.iotsonnenuploader.commanddata;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Serdeable
@Data
public class CommandReceived {
	private SupportedCommands commandIdentifier;
	private String data;
	/*
	 * example {"commandIdentifier": "SET_PLACEHOLDER", "data":
	 * "New placeholder text" }
	 */
}
