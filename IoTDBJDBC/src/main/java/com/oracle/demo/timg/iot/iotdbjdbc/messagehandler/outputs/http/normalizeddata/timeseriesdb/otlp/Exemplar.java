package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class Exemplar {
	private List<KeyValue> filteredAttributes = new ArrayList<>();
	private Long timeUnixNano;
	private Double asDouble;
	private Long asInt;
	private byte[] spanId;
	private byte[] traceId;
}
