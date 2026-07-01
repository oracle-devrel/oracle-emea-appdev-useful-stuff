package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.io.IOException;

import io.micronaut.core.type.Argument;
import io.micronaut.serde.Decoder;
import io.micronaut.serde.Deserializer;
import io.micronaut.serde.Encoder;
import io.micronaut.serde.Serializer;

public class LongAsStringSerializer implements Serializer<Long>, Deserializer<Long> {
	@Override
	public void serialize(Encoder encoder, EncoderContext context, Argument<? extends Long> type, Long value)
			throws IOException {
		if (value == null) {
			encoder.encodeNull();
			return;
		}
		encoder.encodeString(value.toString());
	}

	@Override
	public Long deserialize(Decoder decoder, DecoderContext context, Argument<? super Long> type) throws IOException {
		String value = decoder.decodeStringNullable();
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			throw decoder.createDeserializationException("Expected a decimal string containing a Long value", value);
		}
	}

	@Override
	public boolean allowNull() {
		return true;
	}
}
