package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OtlpMetricsJsonBuilder {
	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private final Map<String, Object> resourceAttributes = new LinkedHashMap<>();
	private final Map<String, Object> scopeAttributes = new LinkedHashMap<>();
	private final List<Map<String, Object>> metrics = new ArrayList<>();
	private String scopeName = "com.oracle.demo.timg.iot.iotdbjdbc";
	private String scopeVersion;

	public static OtlpMetricsJsonBuilder create() {
		return new OtlpMetricsJsonBuilder();
	}

	public OtlpMetricsJsonBuilder resourceAttribute(String key, Object value) {
		putIfPresent(resourceAttributes, key, value);
		return this;
	}

	public OtlpMetricsJsonBuilder service(String serviceName) {
		return resourceAttribute("service.name", serviceName);
	}

	public OtlpMetricsJsonBuilder scope(String scopeName, String scopeVersion) {
		if (hasText(scopeName)) {
			this.scopeName = scopeName;
		}
		if (hasText(scopeVersion)) {
			this.scopeVersion = scopeVersion;
		}
		return this;
	}

	public OtlpMetricsJsonBuilder scopeAttribute(String key, Object value) {
		putIfPresent(scopeAttributes, key, value);
		return this;
	}

	public OtlpMetricsJsonBuilder addGauge(String name, String description, String unit, Number value,
			Instant observedAt, Map<String, ?> attributes) {
		Map<String, Object> metric = baseMetric(name, description, unit);
		metric.put("gauge", Map.of("dataPoints", List.of(dataPoint(value, observedAt, null, attributes))));
		metrics.add(metric);
		return this;
	}

	public OtlpMetricsJsonBuilder addSum(String name, String description, String unit, Number value, Instant observedAt,
			Instant startTime, Map<String, ?> attributes, OtlpAggregationTemporality temporality, boolean monotonic) {
		Map<String, Object> sum = new LinkedHashMap<>();
		sum.put("dataPoints", List.of(dataPoint(value, observedAt, startTime, attributes)));
		sum.put("aggregationTemporality",
				(temporality == null ? OtlpAggregationTemporality.UNSPECIFIED : temporality).getJsonValue());
		sum.put("isMonotonic", monotonic);

		Map<String, Object> metric = baseMetric(name, description, unit);
		metric.put("sum", sum);
		metrics.add(metric);
		return this;
	}

	public boolean isEmpty() {
		return metrics.isEmpty();
	}

	public Map<String, Object> build() {
		Map<String, Object> resource = new LinkedHashMap<>();
		List<Map<String, Object>> resourceAttributeList = OtlpJsonValues.attributes(resourceAttributes);
		if (!resourceAttributeList.isEmpty()) {
			resource.put("attributes", resourceAttributeList);
		}

		Map<String, Object> scope = new LinkedHashMap<>();
		scope.put("name", scopeName);
		if (hasText(scopeVersion)) {
			scope.put("version", scopeVersion);
		}
		List<Map<String, Object>> scopeAttributeList = OtlpJsonValues.attributes(scopeAttributes);
		if (!scopeAttributeList.isEmpty()) {
			scope.put("attributes", scopeAttributeList);
		}

		Map<String, Object> scopeMetrics = new LinkedHashMap<>();
		scopeMetrics.put("scope", scope);
		scopeMetrics.put("metrics", metrics);

		Map<String, Object> resourceMetrics = new LinkedHashMap<>();
		resourceMetrics.put("resource", resource);
		resourceMetrics.put("scopeMetrics", List.of(scopeMetrics));

		return Map.of("resourceMetrics", List.of(resourceMetrics));
	}

	private Map<String, Object> baseMetric(String name, String description, String unit) {
		if (!hasText(name)) {
			throw new IllegalArgumentException("OTLP metric name must not be blank");
		}
		Map<String, Object> metric = new LinkedHashMap<>();
		metric.put("name", name);
		if (hasText(description)) {
			metric.put("description", description);
		}
		if (hasText(unit)) {
			metric.put("unit", unit);
		}
		return metric;
	}

	private Map<String, Object> dataPoint(Number value, Instant observedAt, Instant startTime,
			Map<String, ?> attributes) {
		Map<String, Object> dataPoint = new LinkedHashMap<>();
		List<Map<String, Object>> attributeList = OtlpJsonValues.attributes(attributes);
		if (!attributeList.isEmpty()) {
			dataPoint.put("attributes", attributeList);
		}
		if (startTime != null) {
			dataPoint.put("startTimeUnixNano", unixNanoString(startTime));
		}
		dataPoint.put("timeUnixNano", unixNanoString(observedAt == null ? Instant.now() : observedAt));
		OtlpJsonValues.putNumberValue(dataPoint, value);
		return dataPoint;
	}

	private static String unixNanoString(Instant instant) {
		long epochSeconds = instant.getEpochSecond();
		long nanos = instant.getNano();
		return Long.toUnsignedString(Math.addExact(Math.multiplyExact(epochSeconds, NANOS_PER_SECOND), nanos));
	}

	private static void putIfPresent(Map<String, Object> target, String key, Object value) {
		if (hasText(key) && value != null) {
			target.put(key, value);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
