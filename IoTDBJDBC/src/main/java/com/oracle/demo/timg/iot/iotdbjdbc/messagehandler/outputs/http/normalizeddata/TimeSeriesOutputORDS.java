package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.HttpOutputType;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rawdata.RawDataIoTOutputHttpClient;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.output.normalizeddata.timeseriesords.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.normalizeddata.timeseriesords.enabled.order")
@Log
public class TimeSeriesOutputORDS implements NormalizedDataMessageHandler {

	private final RawDataIoTOutputHttpClient httpClient;
	private final int order;
	private final HttpOutputType type;
	private final boolean sentDataIsCompleted;

	@Inject
	public TimeSeriesOutputORDS(RawDataIoTOutputHttpClient httpClient,
			@Property(name = "messagehandler.output.normalizeddata.timeseriesords.enabled.order") int order,
			@Property(name = "messagehandler.output.rawdata.httpclient.type", defaultValue = "STRING") HttpOutputType type,
			@Property(name = "messagehandler.output.rawdata.httpclient.sentdataiscompleted", defaultValue = "true") boolean sentDataIsCompleted) {
		this.httpClient = httpClient;
		this.order = order;
		this.type = type;
		this.sentDataIsCompleted = sentDataIsCompleted;
	}

	@Override
	public int getOrder() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
