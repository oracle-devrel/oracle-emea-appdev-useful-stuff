package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb;

public class TimeSeriesDBProperties {
	public static final String TIME_SERIES_PROPERTY_PREFIX = "messagehandler.output.normalizeddata.timeseries";
	public static final String TIME_SERIES_PROPERTY_USERNAME = TIME_SERIES_PROPERTY_PREFIX + ".username";
	public static final String TIME_SERIES_PROPERTY_PASSWORD = TIME_SERIES_PROPERTY_PREFIX + ".password";
	public static final String TIME_SERIES_PROPERTY_ENABLED = TIME_SERIES_PROPERTY_PREFIX + ".enabled";
	public static final String TIME_SERIES_PROPERTY_ORDER = TIME_SERIES_PROPERTY_PREFIX + ".order";
	public static final String TIME_SERIES_PROPERTY_URI_QUERY_PARAMS = TIME_SERIES_PROPERTY_PREFIX + ".uri.query";
	public static final String TIME_SERIES_PROPERTY_URI_QUERY_PARAMS_X = TIME_SERIES_PROPERTY_URI_QUERY_PARAMS + ".x";
	public static final String TIME_SERIES_PROPERTY_URI_QUERY_PARAMS_Y = TIME_SERIES_PROPERTY_URI_QUERY_PARAMS + ".y";
	// things for the oauth token
	public static final String TIME_SERIES_PROPERTY_OAUTH = TIME_SERIES_PROPERTY_PREFIX + ".oauth";
	public static final String TIME_SERIES_PROPERTY_OAUTH_PATH = TIME_SERIES_PROPERTY_OAUTH + ".path";
	public static final String TIME_SERIES_PROPERTY_OAUTH_RENEWAL_PREEMPT = TIME_SERIES_PROPERTY_OAUTH
			+ ".renewalpreempt";
	public static final String TIME_SERIES_PROPERTY_OAUTH_TENANCY_OCID = TIME_SERIES_PROPERTY_OAUTH + ".tenancyocid";
	public static final String TIME_SERIES_PROPERTY_OAUTH_DATABASE_NAME = TIME_SERIES_PROPERTY_OAUTH + ".databasename";
	// these are the metrics endpoints
	public static final String TIME_SERIES_PROPERTY_METRICS = TIME_SERIES_PROPERTY_PREFIX + ".metrics";
	public static final String TIME_SERIES_PROPERTY_METRICS_PATH = TIME_SERIES_PROPERTY_METRICS + ".path";
}
