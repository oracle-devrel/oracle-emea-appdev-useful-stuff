package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum DataPointFlags {
	DATA_POINT_FLAGS_DO_NOT_USE(0),
	DATA_POINT_FLAGS_NO_RECORDED_VALUE_MASK(1);

	private final int value;

	DataPointFlags(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
