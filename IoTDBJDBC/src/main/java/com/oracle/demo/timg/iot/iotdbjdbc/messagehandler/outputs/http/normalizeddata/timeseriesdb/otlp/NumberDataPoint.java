package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class NumberDataPoint {
	private List<KeyValue> attributes = new ArrayList<>();
	private Long startTimeUnixNano;
	private Long timeUnixNano;
	private Double asDouble;
	private Long asInt;
	private List<Exemplar> exemplars = new ArrayList<>();
	private Integer flags;
}
