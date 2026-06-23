package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

public final class OtlpProperties {
	public static final String PREFIX = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS + ".otlp";
	public static final String ENABLED = PREFIX + ".enabled";
	public static final String ORDER = PREFIX + ".order";
	public static final String PASSTHROUGH = PREFIX + ".passthrough";
	public static final String SERVICE_NAME = PREFIX + ".service.name";
	public static final String SERVICE_NAMESPACE = PREFIX + ".service.namespace";
	public static final String SERVICE_INSTANCE_ID = PREFIX + ".service.instance.id";
	public static final String SCOPE_NAME = PREFIX + ".scope.name";
	public static final String SCOPE_VERSION = PREFIX + ".scope.version";
	public static final String METRIC_NAME_PREFIX = PREFIX + ".metric.name.prefix";
	public static final String METRIC_UNIT = PREFIX + ".metric.unit";
	public static final String METRIC_DESCRIPTION = PREFIX + ".metric.description";
	public static final String METRICS_CLIENT_ID = "timeseriesmetrics";

	private OtlpProperties() {
	}
}
