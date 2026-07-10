package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Serdeable
@Builder
public class GatewayConfigData {
	private Long successfullharetrievetimewindow;
	private Long failedharetrievetimewindow;
	private Long successfulluploadtimewindow;
	private Long faileduploadtimewindow;
}
