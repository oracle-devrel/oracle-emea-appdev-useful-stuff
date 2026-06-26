package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

public final class OtlpProperties {
	public static final String PREFIX = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS + ".otlp";
	public static final String ENABLED = PREFIX + ".enabled";
	public static final String METRICS_CLIENT_ID = "timeseriesmetrics";

	private OtlpProperties() {
	}
}
