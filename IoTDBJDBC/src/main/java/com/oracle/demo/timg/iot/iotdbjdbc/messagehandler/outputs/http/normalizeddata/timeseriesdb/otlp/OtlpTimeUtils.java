package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.time.Instant;

public final class OtlpTimeUtils {
	private static final long NANOS_PER_SECOND = 1_000_000_000L;

	private OtlpTimeUtils() {
	}

	public static long unixNano(String iso8601ZuluTimestamp) {
		return unixNano(Instant.parse(iso8601ZuluTimestamp));
	}

	public static long unixNano(Instant instant) {
		return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), NANOS_PER_SECOND), instant.getNano());
	}
}
