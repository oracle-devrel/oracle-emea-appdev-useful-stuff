package com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway;

import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;

public interface GatewayEventPublisher {
	public CompletableFuture<Void> publishGatewayStats(IoTGatewayStatsData data);

	public CompletableFuture<Void> publishGatewayConfig(IoTGatewayConfigData data);
}
