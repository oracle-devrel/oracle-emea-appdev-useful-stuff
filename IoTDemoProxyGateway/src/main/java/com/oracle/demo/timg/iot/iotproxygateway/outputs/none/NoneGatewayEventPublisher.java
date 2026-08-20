package com.oracle.demo.timg.iot.iotproxygateway.outputs.none;

import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayEventPublisher;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "NONE")
public class NoneGatewayEventPublisher implements GatewayEventPublisher {

	@Override
	public CompletableFuture<Void> publishGatewayStats(IoTGatewayStatsData data) {
		log.info("None Publishing gateway stats data " + data);
		return null;
	}

	@Override
	public CompletableFuture<Void> publishGatewayConfig(IoTGatewayConfigData data) {
		log.info("None Publishing gateway config data " + data);
		return null;
	}

}
