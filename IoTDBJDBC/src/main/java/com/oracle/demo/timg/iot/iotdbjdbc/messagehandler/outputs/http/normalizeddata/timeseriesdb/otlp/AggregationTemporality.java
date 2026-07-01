package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum AggregationTemporality {
	AGGREGATION_TEMPORALITY_UNSPECIFIED,
	AGGREGATION_TEMPORALITY_DELTA,
	AGGREGATION_TEMPORALITY_CUMULATIVE
}
