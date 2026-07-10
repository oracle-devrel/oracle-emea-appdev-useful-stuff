package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Serdeable
@Builder
public class GatewayStatsData {
	private Double haretrievesuccess;
	private Double haretrievefail;
	private Double uploadsuccess;
	private Double uploadfail;
}
