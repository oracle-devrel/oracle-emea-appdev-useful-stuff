package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class SummaryDataPoint {
	private List<KeyValue> attributes = new ArrayList<>();
	private Long startTimeUnixNano;
	private Long timeUnixNano;
	private Long count;
	private Double sum;
	private List<ValueAtQuantile> quantileValues = new ArrayList<>();
	private Integer flags;
}
