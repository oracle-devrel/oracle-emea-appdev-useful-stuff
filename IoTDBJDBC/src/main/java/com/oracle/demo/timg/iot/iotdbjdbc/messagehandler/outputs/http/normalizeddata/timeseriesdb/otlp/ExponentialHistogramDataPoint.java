package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class ExponentialHistogramDataPoint {
	private List<KeyValue> attributes = new ArrayList<>();
	private Long startTimeUnixNano;
	private Long timeUnixNano;
	private Long count;
	private Double sum;
	private Integer scale;
	private Long zeroCount;
	private Buckets positive;
	private Buckets negative;
	private Integer flags;
	private List<Exemplar> exemplars = new ArrayList<>();
	private Double min;
	private Double max;
	private Double zeroThreshold;
}
