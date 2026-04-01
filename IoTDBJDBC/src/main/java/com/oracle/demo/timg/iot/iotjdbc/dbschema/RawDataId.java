package com.oracle.demo.timg.iot.iotjdbc.dbschema;

import java.time.ZonedDateTime;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.Embeddable;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Serdeable
@Introspected
@Embeddable

public class RawDataId {
	@MappedProperty(value = "DIGITAL_TWIN_INSTANCE_ID")
	@NotNull
	private String digitalTwinInstanceId;
	@MappedProperty(value = "ENDPOINT")
	@NotNull
	private String endpoint;
	@MappedProperty(value = "TIME_RECEIVED")
	@NotNull
	private ZonedDateTime timeReceived;
}