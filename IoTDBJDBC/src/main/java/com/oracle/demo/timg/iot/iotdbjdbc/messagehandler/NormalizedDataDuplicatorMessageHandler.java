package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "normalizeddata.handler.duplicator.enabled", value = "true", defaultValue = "false")
@Requires(property = "normalizeddata.handler.duplicator.order")
@Log
public class NormalizedDataDuplicatorMessageHandler implements NormalizedDataMessageHandler {
	private final int count;
	private final int order;

	public NormalizedDataDuplicatorMessageHandler(
			@Property(name = "normalizeddata.handler.duplicator.count", defaultValue = "2") int count,
			@Property(name = "normalizeddata.handler.duplicator.order") int order) {
		this.count = count;
		this.order = order;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info("Creating " + count + " results from " + input);
		NormalizedData results[] = new NormalizedData[count];
		// we're not modifying the input here, so we don't need to create new versions,
		// this assumes however that anything calling this follows the contract and
		// won't modify it's inputs. However as this class is really for testing
		// purposes only I don't see a problem
		for (int i = 0; i < count; i++) {
			results[i] = input;
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Duplicator handler";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder();
	}

}
