package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.io.IOException;

import io.micronaut.core.type.Argument;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serializer;

public class LongAsStringSerializer implements Serializer<Long> {
	@Override
	public void serialize(Encoder encoder, EncoderContext context, Argument<? extends Long> type, Long value)
			throws IOException {
		if (value == null) {
			encoder.encodeNull();
			return;
		}
		encoder.encodeString(value.toString());
	}
}
