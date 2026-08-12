package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.requestfilters;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.IoTOutputHttpRestClientNormalizedDataSettings;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.rawdata.IoTOutputHttpRestClientRawDataSettings;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;

public class IoTOutputHttpRestClientEnableRequestFilter implements Condition {

	@Override
	public boolean matches(ConditionContext context) {
		return context.getProperty(IoTOutputHttpRestClientNormalizedDataSettings.ENABLED_PROPERTY, Boolean.class)
				.orElse(false)
				|| context.getProperty(IoTOutputHttpRestClientRawDataSettings.ENABLED_PROPERTY, Boolean.class)
						.orElse(false);
	}

}
