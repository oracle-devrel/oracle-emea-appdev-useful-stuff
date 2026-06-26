package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class Metric {
	private String name;
	private String description;
	private String unit;
	private Gauge gauge;
	private Sum sum;
	private Histogram histogram;
	private ExponentialHistogram exponentialHistogram;
	private Summary summary;
	private List<KeyValue> metadata = new ArrayList<>();
}
