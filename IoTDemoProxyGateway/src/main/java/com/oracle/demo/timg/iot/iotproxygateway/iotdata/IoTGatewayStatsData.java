package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStatsData;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Serdeable
@Builder
public class IoTGatewayStatsData {
	// get the UTC TZ once to speed things later
	@ToString.Exclude
	@JsonIgnore
	private final static ZoneId utcTz = ZoneId.of("UTC");
	// note that for Indirectly connected devices the timestamp must be within the
	// payload sub object as the current configuration of the gateway envelope means
	// that's all that's passed on to the indirectly connected devices
	// for the telemetry on the gateway itself then we can get to the envelope
	// attributes before the contentRoot is applied (if there is one and its not $)
	// so we can if we want we can have the timestamp at the outer (envelope) level
	// or within the payload,
	@Builder.Default
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSXXX")
	private ZonedDateTime timestamp = ZonedDateTime.now(utcTz);
	private final String devicekey = null;
	private GatewayStatsData payload;
}
