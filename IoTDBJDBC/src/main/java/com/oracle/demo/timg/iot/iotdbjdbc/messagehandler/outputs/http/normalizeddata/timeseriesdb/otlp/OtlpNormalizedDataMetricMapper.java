package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

public class OtlpNormalizedDataMetricMapper {
	public static final String DEFAULT_METRIC_PREFIX = "iot.normalized";
	public static final String DEFAULT_UNIT = "1";

	private final String metricNamePrefix;
	private final String unit;
	private final String description;

	public OtlpNormalizedDataMetricMapper() {
		this(DEFAULT_METRIC_PREFIX, DEFAULT_UNIT, "IoT normalized data value");
	}

	public OtlpNormalizedDataMetricMapper(String metricNamePrefix, String unit, String description) {
		this.metricNamePrefix = hasText(metricNamePrefix) ? metricNamePrefix : DEFAULT_METRIC_PREFIX;
		this.unit = hasText(unit) ? unit : DEFAULT_UNIT;
		this.description = description;
	}

	public OtlpMetricsJsonBuilder addGauge(OtlpMetricsJsonBuilder builder, NormalizedData input) {
		return builder.addGauge(metricName(input), description, unit, metricValue(input), observedAt(input),
				attributes(input));
	}

	public Map<String, Object> attributes(NormalizedData input) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		putIfPresent(attributes, "iot.digital_twin.instance_id", input.getDigitalTwinInstanceId());
		putIfPresent(attributes, "iot.content.path", input.getContentPath());
		putIfPresent(attributes, "iot.content.type", input.getContentType());
		if (input.getContentJsonType() != null) {
			attributes.put("iot.content.json_type", input.getContentJsonType().toString());
		}
		return attributes;
	}

	public String metricName(NormalizedData input) {
		String contentPath = input.getContentPath();
		if (!hasText(contentPath)) {
			return metricNamePrefix + ".value";
		}
		String sanitizedPath = contentPath.strip().replace('\\', '/').replaceAll("^/+", "").replace('/', '.')
				.replaceAll("[^A-Za-z0-9_.-]+", "_").replaceAll("\\.+", ".");
		if (!hasText(sanitizedPath)) {
			sanitizedPath = "value";
		}
		return metricNamePrefix + "." + sanitizedPath;
	}

	public BigDecimal metricValue(NormalizedData input) {
		String rawValue = input.getContent();
		if (!hasText(rawValue) && input.getContentJsonValue() != null) {
			rawValue = input.getContentJsonValue().toString();
		}
		if (!hasText(rawValue)) {
			throw new IllegalArgumentException("NormalizedData content is empty and cannot be converted to a metric");
		}
		String normalized = rawValue.strip();
		if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
			normalized = normalized.substring(1, normalized.length() - 1);
		}
		return new BigDecimal(normalized);
	}

	public Instant observedAt(NormalizedData input) {
		String timeObserved = input.getTimeObserved();
		if (!hasText(timeObserved)) {
			return Instant.now();
		}
		String normalized = timeObserved.strip();
		try {
			return Instant.parse(normalized);
		} catch (RuntimeException e) {
			// Try the next common shape.
		}
		String isoLike = normalized.indexOf('T') >= 0 ? normalized : normalized.replace(' ', 'T');
		try {
			return OffsetDateTime.parse(isoLike).toInstant();
		} catch (RuntimeException e) {
			// Try a local timestamp and assume UTC.
		}
		return LocalDateTime.parse(isoLike).toInstant(ZoneOffset.UTC);
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
