package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import lombok.Data;

@Data
@Serdeable
public class KeyValue {
	private String key;
	private AnyValue value;
}
