package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

public final class NormalizedDataMetricDtoExample {
	private NormalizedDataMetricDtoExample() {
	}

	public static MetricsData createGaugeMetric(NormalizedData normalizedData) {
		return new NormalizedDataMetricsDataBuilder()
				.serviceName("IoTDBJDBC")
				.scope("com.oracle.demo.timg.iot.iotdbjdbc", "1.0.0")
				.metric("iot.normalized", "1", "IoT normalized data value")
				.gaugeMetric(normalizedData)
				.build();
	}
}
