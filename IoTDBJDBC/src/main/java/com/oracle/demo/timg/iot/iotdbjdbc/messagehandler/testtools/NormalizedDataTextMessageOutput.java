package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.testtools;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "normalizeddata.handler.textoutput.enabled", value = "true", defaultValue = "false")
@Requires(property = "normalizeddata.handler.textoutput.order")
@Log
public class NormalizedDataTextMessageOutput implements NormalizedDataMessageHandler {
	private final int order;
	private final boolean passthrough;

	public NormalizedDataTextMessageOutput(@Property(name = "normalizeddata.handler.textoutput.order") int order,
			@Property(name = "normalizeddata.handler.textoutput.passthrough", defaultValue = "true") boolean passthrough) {
		this.order = order;
		this.passthrough = passthrough;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info("NormalizedData is " + input);
		NormalizedData results[];
		// are we acting as a terminator or a step in the process ?
		if (passthrough) {
			results = new NormalizedData[1];
			results[0] = input;
		} else {
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
		return "Text output handler";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder();
	}

}
