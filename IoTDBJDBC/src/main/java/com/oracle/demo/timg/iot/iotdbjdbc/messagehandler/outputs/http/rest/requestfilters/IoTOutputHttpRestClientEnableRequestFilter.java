package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.requestfilters;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.generalrest.IoTOutputHttpRestClientNormalizedDataSettings;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.rawdata.IoTOutputHttpRestClientRawDataSettings;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
public class IoTOutputHttpRestClientEnableRequestFilter implements Condition {

	@Inject
	public IoTOutputHttpRestClientEnableRequestFilter() {
		log.info("IoTOutputHttpRestClientEnableRequestFilter In constructor");
	}

	@PostConstruct
	public void postConstruct() {
		log.info("IoTOutputHttpRestClientEnableRequestFilter In post construct");
	}

	@Override
	public boolean matches(ConditionContext context) {
		boolean normDataEnabled = context
				.getProperty(IoTOutputHttpRestClientNormalizedDataSettings.ENABLED_PROPERTY, Boolean.class)
				.orElse(false);
		boolean rawDataEnabled = context
				.getProperty(IoTOutputHttpRestClientRawDataSettings.ENABLED_PROPERTY, Boolean.class).orElse(false);
		boolean result = normDataEnabled || rawDataEnabled;
		log.info("IoTOutputHttpOciClientEnableRequestFilter testing for "
				+ IoTOutputHttpRestClientNormalizedDataSettings.ENABLED_PROPERTY + " is " + normDataEnabled);
		log.info("IoTOutputHttpOciClientEnableRequestFilter testing for "
				+ IoTOutputHttpRestClientRawDataSettings.ENABLED_PROPERTY + " is " + rawDataEnabled);
		log.info("IoTOutputHttpOciClientEnableRequestFilter combined result is " + result);
		return result;
	}

}
