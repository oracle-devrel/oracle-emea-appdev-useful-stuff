package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler;

import java.util.concurrent.ThreadLocalRandom;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "normalizeddata.handler.randomfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "normalizeddata.handler.randomfilter.order")
@Log
public class NormalizedDataRandomFilterMessageHandler implements NormalizedDataMessageHandler {
	private final int order;
	private final boolean passthrough;

	public NormalizedDataRandomFilterMessageHandler(
			@Property(name = "normalizeddata.handler.randomfilter.order") int order,
			@Property(name = "normalizeddata.handler.randomfilter.passthrough", defaultValue = "true") boolean passthrough) {
		this.order = order;
		this.passthrough = passthrough;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		NormalizedData results[];
		if (ThreadLocalRandom.current().nextBoolean()) {
			log.info("Random filter is true, will pass on input " + input);
			results = new NormalizedData[1];
			results[0] = input;
		} else {
			log.info("Random filter is false, blocking input " + input);
			results = new NormalizedData[0];
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Random filter handler";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder();
	}

}
