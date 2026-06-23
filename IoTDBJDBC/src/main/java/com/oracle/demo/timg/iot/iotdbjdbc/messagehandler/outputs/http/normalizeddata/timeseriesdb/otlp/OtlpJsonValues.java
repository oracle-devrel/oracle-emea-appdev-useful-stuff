package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OtlpJsonValues {
	private OtlpJsonValues() {
	}

	public static List<Map<String, Object>> attributes(Map<String, ?> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return List.of();
		}
		return attributes.entrySet().stream().filter(entry -> entry.getKey() != null && entry.getValue() != null)
				.map(entry -> attribute(entry.getKey(), entry.getValue())).toList();
	}

	public static Map<String, Object> attribute(String key, Object value) {
		Map<String, Object> attribute = new LinkedHashMap<>();
		attribute.put("key", key);
		attribute.put("value", anyValue(value));
		return attribute;
	}

	public static Map<String, Object> anyValue(Object value) {
		Map<String, Object> anyValue = new LinkedHashMap<>();
		if (value instanceof Boolean boolValue) {
			anyValue.put("boolValue", boolValue);
		} else if (value instanceof Number numberValue) {
			if (isIntegral(numberValue)) {
				anyValue.put("intValue", integralString(numberValue));
			} else {
				double doubleValue = numberValue.doubleValue();
				if (!Double.isFinite(doubleValue)) {
					throw new IllegalArgumentException("OTLP JSON does not support non-finite numeric values");
				}
				anyValue.put("doubleValue", doubleValue);
			}
		} else {
			anyValue.put("stringValue", String.valueOf(value));
		}
		return anyValue;
	}

	static void putNumberValue(Map<String, Object> target, Number value) {
		if (value == null) {
			throw new IllegalArgumentException("OTLP metric values must not be null");
		}
		if (isIntegral(value)) {
			target.put("asInt", integralString(value));
		} else {
			double doubleValue = value.doubleValue();
			if (!Double.isFinite(doubleValue)) {
				throw new IllegalArgumentException("OTLP metric values must be finite");
			}
			target.put("asDouble", doubleValue);
		}
	}

	private static boolean isIntegral(Number value) {
		if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof BigInteger) {
			return true;
		}
		if (value instanceof BigDecimal decimalValue) {
			return decimalValue.stripTrailingZeros().scale() <= 0;
		}
		return false;
	}

	private static String integralString(Number value) {
		if (value instanceof BigDecimal decimalValue) {
			return decimalValue.toBigIntegerExact().toString();
		}
		return value.toString();
	}
}
