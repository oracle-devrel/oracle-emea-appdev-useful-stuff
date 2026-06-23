package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

public enum OtlpAggregationTemporality {
	UNSPECIFIED("AGGREGATION_TEMPORALITY_UNSPECIFIED"),
	DELTA("AGGREGATION_TEMPORALITY_DELTA"),
	CUMULATIVE("AGGREGATION_TEMPORALITY_CUMULATIVE");

	private final String jsonValue;

	OtlpAggregationTemporality(String jsonValue) {
		this.jsonValue = jsonValue;
	}

	public String getJsonValue() {
		return jsonValue;
	}
}
