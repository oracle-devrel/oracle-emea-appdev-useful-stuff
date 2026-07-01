package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

import java.util.UUID;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.OAuthTokenRetrievalException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.TimeSeriesDBOAuthTokenRequestFilter;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.TimeSeriesDBOAuthTokenRetriever;

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

//@ClientFilter(patterns = { "${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH + ":/tel/v1/metrics}",
//		"${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH + ":/tel/v1/metrics}/**" })
@ClientFilter(patterns = "/tel/v1/metrics", serviceId = OtlpProperties.METRICS_CLIENT_ID)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = "micronaut.http.services.timeseriesmetrics.url")
@Requires(property = "micronaut.http.services.timeseriesoauth.url")
@Log
public class OtlpMetricsRequestFilter {
	// private final TimeSeriesDBOAuthTokenRetriever tokenRetriever;
	// need to do this using a provider as otherwise we hit a massive recursion that
	// goes this then the retriever that itself uses a http client and so
	// ends up in the http client framework that then tries to load this again on
	// setting this up that ultimately results in stack overflow
	// using the bean provider delays the retriever setup until this is constructed,
	// and hopefully also the retriever and it's client
	private final BeanProvider<TimeSeriesDBOAuthTokenRetriever> tokenRetriever;

	@Inject
	public OtlpMetricsRequestFilter(BeanProvider<TimeSeriesDBOAuthTokenRetriever> tokenRetriever) {
		this.tokenRetriever = tokenRetriever;
	}

//	@Inject
//	public OtlpMetricsRequestFilter(TimeSeriesDBOAuthTokenRetriever tokenRetriever) {
//		log.info("OtlpMetricsRequestFilter constructor");
//		this.tokenRetriever = tokenRetriever;
//	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) {
		log.fine("Running metrics OTLP filtering");
		try {
			String token = tokenRetriever.get().getToken();
			String tokenType = tokenRetriever.get().getTokenType();
			request.getHeaders().add(AUTHORIZATION, (tokenType == null ? "Bearer" : tokenType) + " " + token);
			request.getHeaders().add(TimeSeriesDBOAuthTokenRequestFilter.HEADER_REQUEST_ID,
					UUID.randomUUID().toString());
			log.finer("request uri " + request.getUri().toASCIIString());
			log.finer("request path " + request.getPath());
			log.finer("request params = " + request.getParameters().asMap().toString());
			log.finer("request headers = " + request.getHeaders().asMap().toString());
			log.finer("Request body " + request.getBody(String.class).orElse("No body set"));

		} catch (OAuthTokenRetrievalException e) {
			throw new IllegalStateException("Unable to retrieve an OAuth token for OTLP metrics upload", e);
		}
		log.fine("Added OAuth authorization headers and uuid for OTLP metrics upload");
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for OtlpMetricsRequestFilter");
	}
}
