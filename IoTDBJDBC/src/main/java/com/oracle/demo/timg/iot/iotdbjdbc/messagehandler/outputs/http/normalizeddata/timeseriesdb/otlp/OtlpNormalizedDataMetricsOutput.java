package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.util.Map;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = OtlpProperties.ENABLED, value = "true", defaultValue = "false")
@Requires(property = OtlpProperties.ORDER)
@Log
public class OtlpNormalizedDataMetricsOutput implements NormalizedDataMessageHandler {
	private final OtlpMetricsClient metricsClient;
	private final OtlpNormalizedDataMetricMapper mapper;
	private final int order;
	private final boolean passthrough;
	private final String serviceName;
	private final String serviceNamespace;
	private final String serviceInstanceId;
	private final String scopeName;
	private final String scopeVersion;
	private final String queryX;
	private final String queryY;

	@Inject
	public OtlpNormalizedDataMetricsOutput(OtlpMetricsClient metricsClient,
			@Property(name = OtlpProperties.ORDER) int order,
			@Property(name = OtlpProperties.PASSTHROUGH, defaultValue = "false") boolean passthrough,
			@Property(name = OtlpProperties.SERVICE_NAME, defaultValue = "IoTDBJDBC") String serviceName,
			@Property(name = OtlpProperties.SERVICE_NAMESPACE, defaultValue = "") String serviceNamespace,
			@Property(name = OtlpProperties.SERVICE_INSTANCE_ID, defaultValue = "") String serviceInstanceId,
			@Property(name = OtlpProperties.SCOPE_NAME, defaultValue = "com.oracle.demo.timg.iot.iotdbjdbc") String scopeName,
			@Property(name = OtlpProperties.SCOPE_VERSION, defaultValue = "") String scopeVersion,
			@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_URI_QUERY_PARAMS_X) String queryX,
			@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_URI_QUERY_PARAMS_Y) String queryY,
			@Property(name = OtlpProperties.METRIC_NAME_PREFIX, defaultValue = OtlpNormalizedDataMetricMapper.DEFAULT_METRIC_PREFIX) String metricNamePrefix,
			@Property(name = OtlpProperties.METRIC_UNIT, defaultValue = OtlpNormalizedDataMetricMapper.DEFAULT_UNIT) String metricUnit,
			@Property(name = OtlpProperties.METRIC_DESCRIPTION, defaultValue = "IoT normalized data value") String metricDescription) {
		this.metricsClient = metricsClient;
		this.order = order;
		this.passthrough = passthrough;
		this.serviceName = serviceName;
		this.serviceNamespace = serviceNamespace;
		this.serviceInstanceId = serviceInstanceId;
		this.scopeName = scopeName;
		this.scopeVersion = scopeVersion;
		this.queryX = queryX;
		this.queryY = queryY;
		this.mapper = new OtlpNormalizedDataMetricMapper(metricNamePrefix, metricUnit, metricDescription);
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) {
		try {
			Map<String, Object> payload = buildMetricsPayload(input);
			HttpResponse<String> response = metricsClient.uploadMetrics(queryX, queryY, payload);
			boolean uploaded = response.getStatus().getCode() >= 200 && response.getStatus().getCode() < 300;
			if (uploaded && !passthrough) {
				return new NormalizedData[0];
			}
		} catch (RuntimeException e) {
			log.warning("Unable to upload normalized data as OTLP metrics JSON: " + e.getLocalizedMessage());
		}
		return new NormalizedData[] { input };
	}

	public Map<String, Object> buildMetricsPayload(NormalizedData input) {
		OtlpMetricsJsonBuilder builder = OtlpMetricsJsonBuilder.create().service(serviceName).scope(scopeName,
				scopeVersion);
		builder.resourceAttribute("service.namespace", blankToNull(serviceNamespace));
		builder.resourceAttribute("service.instance.id", blankToNull(serviceInstanceId));
		mapper.addGauge(builder, input);
		return builder.build();
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "OTLP metrics JSON output";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + ", passthrough " + passthrough;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}
}
