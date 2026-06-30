package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;

@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = "micronaut.http.services." + OtlpProperties.METRICS_CLIENT_ID + ".url")
@Client(id = OtlpProperties.METRICS_CLIENT_ID)
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface OtlpMetricsClient {
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Post("${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH + "}")
	HttpResponse<String> uploadMetrics(@QueryValue("x") String queryParamX, @QueryValue("y") String queryParamY,
			@Body String metricsDataString);
}
