package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.math.BigDecimal;

public final class OtlpAttributeUtils {
	private OtlpAttributeUtils() {
	}

	public static KeyValue attribute(String key, String value) {
		return attribute(key, stringValue(value));
	}

	public static KeyValue attribute(String key, AnyValue value) {
		KeyValue keyValue = new KeyValue();
		keyValue.setKey(key);
		keyValue.setValue(value);
		return keyValue;
	}

	public static AnyValue contentValue(String rawValue) {
		if (rawValue == null) {
			return stringValue(null);
		}

		String value = rawValue.strip();
		if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
			return boolValue(Boolean.valueOf(value));
		}
		try {
			return doubleValue(new BigDecimal(value).doubleValue());
		} catch (NumberFormatException e) {
			return stringValue(unquote(value));
		}
	}

	public static AnyValue stringValue(String value) {
		AnyValue anyValue = new AnyValue();
		anyValue.setStringValue(value);
		return anyValue;
	}

	public static AnyValue boolValue(Boolean value) {
		AnyValue anyValue = new AnyValue();
		anyValue.setBoolValue(value);
		return anyValue;
	}

	public static AnyValue doubleValue(Double value) {
		AnyValue anyValue = new AnyValue();
		anyValue.setDoubleValue(value);
		return anyValue;
	}

	private static String unquote(String value) {
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
