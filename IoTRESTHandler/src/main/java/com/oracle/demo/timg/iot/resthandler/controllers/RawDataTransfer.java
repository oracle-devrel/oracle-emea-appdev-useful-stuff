package com.oracle.demo.timg.iot.resthandler.controllers;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class RawDataTransfer {
	private String digitalTwinInstanceId;
	private String endpoint;
	private byte content[];
	private String contentType;
	private String timeReceived;
}
