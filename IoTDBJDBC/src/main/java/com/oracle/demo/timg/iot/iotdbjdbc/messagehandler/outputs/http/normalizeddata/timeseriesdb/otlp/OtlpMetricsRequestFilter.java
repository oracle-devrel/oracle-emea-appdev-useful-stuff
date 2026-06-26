package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

import java.util.UUID;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.OAuthTokenRetrievalException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.TimeSeriesDBOAuthTokenRequestFilter;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.TimeSeriesDBOAuthTokenRetriever;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@ClientFilter(patterns = { "${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH + ":/tel/v1/metrics}",
		"${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH + ":/tel/v1/metrics}/**" })
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_USERNAME)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_PASSWORD)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_X)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_Y)
@Log
public class OtlpMetricsRequestFilter {
	private final TimeSeriesDBOAuthTokenRetriever tokenRetriever;

	@Inject
	public OtlpMetricsRequestFilter(TimeSeriesDBOAuthTokenRetriever tokenRetriever) {
		this.tokenRetriever = tokenRetriever;
	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) {
		try {
			String token = tokenRetriever.getToken();
			String tokenType = tokenRetriever.getTokenType();
			request.getHeaders().add(AUTHORIZATION, (tokenType == null ? "Bearer" : tokenType) + " " + token);
			request.getHeaders().add(TimeSeriesDBOAuthTokenRequestFilter.HEADER_REQUEST_ID,
					UUID.randomUUID().toString());
		} catch (OAuthTokenRetrievalException e) {
			throw new IllegalStateException("Unable to retrieve an OAuth token for OTLP metrics upload", e);
		}
		log.info("Added OAuth authorization headers for OTLP metrics upload");
	}
}
