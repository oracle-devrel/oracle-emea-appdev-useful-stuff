package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSeriesEndpointsQueryParams {
	private String metricsQueryX;
	private String metricsQueryY;
	private String oauthQueryX;
	private String oauthQueryY;
}
