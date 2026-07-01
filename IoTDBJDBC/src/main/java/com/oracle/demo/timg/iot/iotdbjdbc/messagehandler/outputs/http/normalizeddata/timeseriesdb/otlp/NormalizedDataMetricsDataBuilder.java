/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import oracle.sql.json.OracleJsonArray;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;

public class NormalizedDataMetricsDataBuilder {
	public static final String DEFAULT_SERVICE_NAME = "IoTDBJDBC";
	public static final String DEFAULT_SCOPE_NAME = "com.oracle.demo.timg.iot.iotdbjdbc";
	public static final String DEFAULT_SCOPE_VERSION = "1.0.0";
	public static final String DEFAULT_METRIC_PREFIX = "iot.normalized";
	public static final String DEFAULT_METRIC_UNIT = "1";
	public static final String DEFAULT_METRIC_DESCRIPTION = "IoT normalized data value";

	private String serviceName = DEFAULT_SERVICE_NAME;
	private String scopeName = DEFAULT_SCOPE_NAME;
	private String scopeVersion = DEFAULT_SCOPE_VERSION;
	private String metricPrefix = DEFAULT_METRIC_PREFIX;
	private String metricUnit = DEFAULT_METRIC_UNIT;
	private String metricDescription = DEFAULT_METRIC_DESCRIPTION;
	private final List<KeyValue> resourceAttributes = new ArrayList<>();
	private final List<KeyValue> scopeAttributes = new ArrayList<>();
	private final MetricsData metricsData = new MetricsData();

	public NormalizedDataMetricsDataBuilder serviceName(String serviceName) {
		if (hasText(serviceName)) {
			this.serviceName = serviceName;
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder scope(String scopeName, String scopeVersion) {
		if (hasText(scopeName)) {
			this.scopeName = scopeName;
		}
		if (hasText(scopeVersion)) {
			this.scopeVersion = scopeVersion;
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder metric(String metricPrefix, String metricUnit, String metricDescription) {
		if (hasText(metricPrefix)) {
			this.metricPrefix = metricPrefix;
		}
		if (hasText(metricUnit)) {
			this.metricUnit = metricUnit;
		}
		if (hasText(metricDescription)) {
			this.metricDescription = metricDescription;
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder resourceAttribute(String key, String value) {
		if (hasText(key)) {
			resourceAttributes.add(attribute(key, value));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder resourceAttribute(String key, AnyValue value) {
		if (hasText(key) && value != null) {
			resourceAttributes.add(attribute(key, value));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder resourceAttributes(List<KeyValue> attributes) {
		if (attributes != null) {
			resourceAttributes.addAll(normalizeAttributeKeys(attributes));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder scopeAttribute(String key, String value) {
		if (hasText(key)) {
			scopeAttributes.add(attribute(key, value));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder scopeAttribute(String key, AnyValue value) {
		if (hasText(key) && value != null) {
			scopeAttributes.add(attribute(key, value));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder scopeAttributes(List<KeyValue> attributes) {
		if (attributes != null) {
			scopeAttributes.addAll(normalizeAttributeKeys(attributes));
		}
		return this;
	}

	public NormalizedDataMetricsDataBuilder gaugeMetric(NormalizedData normalizedData) {
		ResourceMetrics resourceMetrics = resourceMetrics(normalizedData);
		ScopeMetrics scopeMetrics = scopeMetrics();
		addMetrics(scopeMetrics, normalizedData);
		resourceMetrics.getScopeMetrics().add(scopeMetrics);
		metricsData.getResourceMetrics().add(resourceMetrics);
		return this;
	}

	public MetricsData build() {
		for (ResourceMetrics resourceMetrics : metricsData.getResourceMetrics()) {
			if (resourceMetrics.getResource() != null) {
				addMissingAttributes(resourceMetrics.getResource().getAttributes(), resourceAttributes);
			}
			for (ScopeMetrics scopeMetrics : resourceMetrics.getScopeMetrics()) {
				if (scopeMetrics.getScope() != null) {
					addMissingAttributes(scopeMetrics.getScope().getAttributes(), scopeAttributes);
				}
			}
		}
		return metricsData;
	}

	public Metric metric(NormalizedData normalizedData) {
		return metric(normalizedData, normalizedData.getContentPath(), normalizedData.getContent(),
				normalizedData.getContentJsonValue(), normalizedData.getContentJsonType());
	}

	private void addMetrics(ScopeMetrics scopeMetrics, NormalizedData normalizedData) {
		OracleJsonValue jsonValue = normalizedData.getContentJsonValue();
		OracleJsonType jsonType = normalizedData.getContentJsonType();
		if (jsonValue != null && jsonType == OracleJsonType.OBJECT) {
			addObjectMetrics(scopeMetrics, normalizedData, normalizedData.getContentPath(), jsonValue.asJsonObject());
			return;
		}
		if (jsonValue != null && jsonType == OracleJsonType.ARRAY) {
			addArrayMetrics(scopeMetrics, normalizedData, normalizedData.getContentPath(), jsonValue.asJsonArray());
			return;
		}
		scopeMetrics.getMetrics().add(metric(normalizedData));
	}

	private void addObjectMetrics(ScopeMetrics scopeMetrics, NormalizedData normalizedData, String contentPath,
			OracleJsonObject object) {
		for (Map.Entry<String, OracleJsonValue> entry : object.entrySet()) {
			addJsonValueMetric(scopeMetrics, normalizedData, appendPath(contentPath, entry.getKey()), entry.getValue());
		}
	}

	private void addArrayMetrics(ScopeMetrics scopeMetrics, NormalizedData normalizedData, String contentPath,
			OracleJsonArray array) {
		for (int i = 0; i < array.size(); i++) {
			addJsonValueMetric(scopeMetrics, normalizedData, appendPath(contentPath, String.valueOf(i)), array.get(i));
		}
	}

	private void addJsonValueMetric(ScopeMetrics scopeMetrics, NormalizedData normalizedData, String contentPath,
			OracleJsonValue value) {
		if (value == null) {
			scopeMetrics.getMetrics().add(metric(normalizedData, contentPath, null, null, OracleJsonType.NULL));
			return;
		}

		OracleJsonType jsonType = value.getOracleJsonType();
		if (jsonType == OracleJsonType.OBJECT) {
			addObjectMetrics(scopeMetrics, normalizedData, contentPath, value.asJsonObject());
			return;
		}
		if (jsonType == OracleJsonType.ARRAY) {
			addArrayMetrics(scopeMetrics, normalizedData, contentPath, value.asJsonArray());
			return;
		}
		scopeMetrics.getMetrics().add(metric(normalizedData, contentPath, value.toString(), value, jsonType));
	}

	private Metric metric(NormalizedData normalizedData, String contentPath, String content, OracleJsonValue jsonValue,
			OracleJsonType jsonType) {
		Gauge gauge = new Gauge();
		gauge.getDataPoints().add(dataPoint(normalizedData, contentPath, content, jsonValue, jsonType));

		Metric metric = new Metric();
		metric.setName(metricName(contentPath));
		metric.setDescription(metricDescription);
		metric.setUnit(metricUnit);
		metric.setGauge(gauge);
		return metric;
	}

	public NumberDataPoint dataPoint(NormalizedData normalizedData) {
		return dataPoint(normalizedData, normalizedData.getContentPath(), normalizedData.getContent(),
				normalizedData.getContentJsonValue(), normalizedData.getContentJsonType());
	}

	private NumberDataPoint dataPoint(NormalizedData normalizedData, String contentPath, String content,
			OracleJsonValue jsonValue, OracleJsonType jsonType) {
		NumberDataPoint dataPoint = new NumberDataPoint();
		dataPoint.setTimeUnixNano(OtlpTimeUtils.unixNanoAsString(normalizedData.getTimeObserved()));
		dataPoint.setAsDouble(metricValue(content, jsonValue, jsonType).doubleValue());
		dataPoint.getAttributes().add(attribute("iot.content.path", contentPath));
		dataPoint.getAttributes()
				.add(attribute("iot.content.type", normalizedData.getContentType()));
		return dataPoint;
	}

	public String metricName(NormalizedData normalizedData) {
		return metricName(normalizedData.getContentPath());
	}

	public String metricName(String contentPath) {
		if (!hasText(contentPath)) {
			return toDotSeparatedSnakeCase(metricPrefix) + ".value";
		}
		String sanitizedPath = toDotSeparatedSnakeCase(contentPath.strip().replace('\\', '/').replaceAll("^/+", "")
				.replace('/', '.').replaceAll("[^A-Za-z0-9_.-]+", "_").replaceAll("\\.+", "."));
		if (!hasText(sanitizedPath)) {
			sanitizedPath = "value";
		}
		return toDotSeparatedSnakeCase(metricPrefix) + "." + sanitizedPath;
	}

	public static String toDotSeparatedSnakeCase(String input) {
		if (!hasText(input)) {
			return input;
		}
		String[] parts = input.strip().replace('\\', '/').replace('/', '.').split("\\.");
		for (int i = 0; i < parts.length; i++) {
			parts[i] = toSnakeCase(parts[i]);
		}
		return String.join(".", parts).replaceAll("\\.+", ".");
	}

	public BigDecimal metricValue(NormalizedData normalizedData) {
		return metricValue(normalizedData.getContent(), normalizedData.getContentJsonValue(),
				normalizedData.getContentJsonType());
	}

	private BigDecimal metricValue(String content, OracleJsonValue jsonValue, OracleJsonType jsonType) {
		if (jsonValue != null) {
			if (jsonType == OracleJsonType.TRUE) {
				return BigDecimal.ONE;
			}
			if (jsonType == OracleJsonType.FALSE || jsonType == OracleJsonType.NULL) {
				return BigDecimal.ZERO;
			}
			if (isNumeric(jsonType)) {
				return jsonValue.asJsonNumber().bigDecimalValue();
			}
		}

		if (!hasText(content)) {
			return BigDecimal.ONE;
		}
		String value = content.strip();
		if ("true".equalsIgnoreCase(value)) {
			return BigDecimal.ONE;
		}
		if ("false".equalsIgnoreCase(value)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return BigDecimal.ONE;
		}
	}

	private static String toSnakeCase(String value) {
		if (!hasText(value)) {
			return value;
		}
		return value.strip()
				.replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
				.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
				.replaceAll("[^A-Za-z0-9]+", "_")
				.replaceAll("_+", "_")
				.replaceAll("^_|_$", "")
				.toLowerCase(java.util.Locale.ROOT);
	}

	private static String appendPath(String contentPath, String fieldName) {
		if (!hasText(contentPath)) {
			return fieldName;
		}
		if (contentPath.endsWith("/")) {
			return contentPath + fieldName;
		}
		return contentPath + "/" + fieldName;
	}

	private static boolean isNumeric(OracleJsonType jsonType) {
		return jsonType == OracleJsonType.DECIMAL || jsonType == OracleJsonType.DOUBLE
				|| jsonType == OracleJsonType.FLOAT;
	}

	private ResourceMetrics resourceMetrics(NormalizedData normalizedData) {
		Resource resource = new Resource();
		resource.getAttributes().add(attribute("service.name", serviceName));
		resource.getAttributes().add(attribute("iot.digital_twin.instance_id", normalizedData.getDigitalTwinInstanceId()));

		ResourceMetrics resourceMetrics = new ResourceMetrics();
		resourceMetrics.setResource(resource);
		return resourceMetrics;
	}

	private ScopeMetrics scopeMetrics() {
		InstrumentationScope scope = new InstrumentationScope();
		scope.setName(toDotSeparatedSnakeCase(scopeName));
		scope.setVersion(scopeVersion);

		ScopeMetrics scopeMetrics = new ScopeMetrics();
		scopeMetrics.setScope(scope);
		return scopeMetrics;
	}

	private static void addMissingAttributes(List<KeyValue> target, List<KeyValue> attributes) {
		for (KeyValue attribute : attributes) {
			if (!target.contains(attribute)) {
				target.add(attribute);
			}
		}
	}

	private static List<KeyValue> normalizeAttributeKeys(List<KeyValue> attributes) {
		List<KeyValue> normalizedAttributes = new ArrayList<>();
		for (KeyValue attribute : attributes) {
			if (attribute != null) {
				normalizedAttributes.add(attribute(attribute.getKey(), attribute.getValue()));
			}
		}
		return normalizedAttributes;
	}

	private static KeyValue attribute(String key, String value) {
		return OtlpAttributeUtils.attribute(toDotSeparatedSnakeCase(key), value);
	}

	private static KeyValue attribute(String key, AnyValue value) {
		return OtlpAttributeUtils.attribute(toDotSeparatedSnakeCase(key), value);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
