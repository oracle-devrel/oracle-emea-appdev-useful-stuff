package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.time.Instant;
import java.util.Map;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;

public final class OtlpMetricsConstructionAndSendExample {
	private OtlpMetricsConstructionAndSendExample() {
	}

	public static void main(String[] args) {
		try (ApplicationContext context = ApplicationContext.run(exampleConfiguration())) {
			OtlpMetricsClient metricsClient = context.getBean(OtlpMetricsClient.class);

			NormalizedData normalizedData = NormalizedData.builder().digitalTwinInstanceId("pump-001")
					.contentPath("/telemetry/temperature").contentType("number").content("21.7")
					.timeObserved(Instant.now().toString()).build();

			Map<String, Object> payload = constructOtlpMetricsPayload(normalizedData);
			HttpResponse<String> response = metricsClient.uploadMetrics("example-x", "example-y", payload);

			System.out.println("OTLP metrics upload returned HTTP " + response.getStatus().getCode());
		}
	}

	public static Map<String, Object> constructOtlpMetricsPayload(NormalizedData normalizedData) {
		OtlpNormalizedDataMetricMapper mapper = new OtlpNormalizedDataMetricMapper();
		OtlpMetricsJsonBuilder builder = OtlpMetricsJsonBuilder.create().service("IoTDBJDBC")
				.resourceAttribute("service.namespace", "iot-demo")
				.resourceAttribute("service.instance.id", "local-example")
				.scope("com.oracle.demo.timg.iot.iotdbjdbc.examples", "1.0.0");

		mapper.addGauge(builder, normalizedData);
		return builder.build();
	}

	private static Map<String, Object> exampleConfiguration() {
		return Map.ofEntries(Map.entry(TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, "true"),
				Map.entry(OtlpProperties.ENABLED, "true"),
				Map.entry(TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS_X, "example-x"),
				Map.entry(TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS_Y, "example-y"),
				Map.entry("micronaut.http.services.timeseriesmetrics.url", "https://example.invalid"),
				Map.entry(TimeSeriesDBProperties.TIME_SERIES_PROPERTY_METRICS_PATH, "/tel/v1/metrics"));
	}
}
