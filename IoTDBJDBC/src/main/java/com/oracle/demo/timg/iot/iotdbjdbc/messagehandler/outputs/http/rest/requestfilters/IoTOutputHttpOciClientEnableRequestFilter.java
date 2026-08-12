package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.requestfilters;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.oicsimple.IoTOutputHttpOICClientNormalizedDataSimpleSettings;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.oicwrappeddata.IoTOutputHttpOICClientWrappedNormalizedDataSettings;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import lombok.extern.java.Log;

@Log
public class IoTOutputHttpOciClientEnableRequestFilter implements Condition {

	@Override
	public boolean matches(ConditionContext context) {
		boolean normDataEnabled = context
				.getProperty(IoTOutputHttpOICClientNormalizedDataSimpleSettings.ENABLED_PROPERTY, Boolean.class)
				.orElse(false);
		boolean wrappedDataEnabled = context
				.getProperty(IoTOutputHttpOICClientWrappedNormalizedDataSettings.ENABLED_PROPERTY, Boolean.class)
				.orElse(false);
		boolean result = normDataEnabled || wrappedDataEnabled;
		log.info("IoTOutputHttpOciClientEnableRequestFilter testing for "
				+ IoTOutputHttpOICClientNormalizedDataSimpleSettings.ENABLED_PROPERTY + " is " + normDataEnabled);
		log.info("IoTOutputHttpOciClientEnableRequestFilter testing for "
				+ IoTOutputHttpOICClientWrappedNormalizedDataSettings.ENABLED_PROPERTY + " is " + wrappedDataEnabled);
		log.info("IoTOutputHttpOciClientEnableRequestFilter combined result is " + result);
		return result;
	}

}
