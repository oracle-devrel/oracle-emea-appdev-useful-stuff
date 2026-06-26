/*Copyright (c) 2025 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb;

import java.sql.SQLException;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.MissingInstanceException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.MissingModelException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints.TimeSeriesEndpointsQueryParams;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints.TimeSeriesEndpointsRetriever;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp.MetricsData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp.NormalizedDataMetricsDataBuilder;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp.OtlpMetricsClient;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ORDER)
@Log
public class TimeSeriesDBOutputOTLP implements NormalizedDataMessageHandler {

	private static final long NANOS_PER_SECOND = 1_000_000_000L;
	private final int order;
	private final boolean sentDataIsCompleted;
	private final DeviceModelInstancesCache deviceModelInstancesCache;
	private final OtlpMetricsClient metricsClient;
	private final String queryX;
	private final String queryY;
	private final ObjectMapper mapper;

	@Inject
	public TimeSeriesDBOutputOTLP(DeviceModelInstancesCache deviceModelInstancesCache, OtlpMetricsClient metricsClient,
			TimeSeriesEndpointsRetriever timeSeriesEndpointsRetriever, ObjectMapper mapper,
			@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ORDER) int order,
			@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_SENT_DATA_IS_COMPLETED, defaultValue = "true") boolean sentDataIsCompleted) {
		this.deviceModelInstancesCache = deviceModelInstancesCache;
		this.metricsClient = metricsClient;
		TimeSeriesEndpointsQueryParams queryParams = timeSeriesEndpointsRetriever.getQueryParams();
		this.queryX = queryParams.getMetricsQueryX();
		this.queryY = queryParams.getMetricsQueryY();
		this.mapper = mapper;
		this.order = order;
		this.sentDataIsCompleted = sentDataIsCompleted;
	}

	@Property(name = "messagehandler.output.normalizeddata.timeseries.doupload", defaultValue = "true")
	private boolean doUpload;

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Timeseries DB OTLP output";
	}

	@Override
	public String getConfig() {
		return getName() + ", order=" + order;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData normalizedData) throws Exception {
		NormalizedDataMetricsDataBuilder builder = new NormalizedDataMetricsDataBuilder().serviceName("IoTDBJDBC")
				.scope("com.oracle.demo.timg.iot.iotdbjdbc", "1.0.0")
				.metric("iot.normalized", "1", "IoT normalized data value").gaugeMetric(normalizedData);

		// we want to have a few "standard" attributes (modelId and name) added beyond
		// it's default setup
		addResourceAttributes(builder, normalizedData);

		MetricsData metricsData = builder.build();

		String metricsDataString = mapper.writeValueAsString(metricsData);
		log.info("About to upload to time series db " + metricsDataString);

		if (doUpload) {
			metricsClient.uploadMetrics(queryX, queryY, metricsData);
		}
		return sentDataIsCompleted ? new NormalizedData[0] : new NormalizedData[] { normalizedData };
	}

	private void addResourceAttributes(NormalizedDataMetricsDataBuilder builder, NormalizedData normalizedData) {
		String instanceId = normalizedData.getDigitalTwinInstanceId();
		// if we have them add the model id and model name
		try {
			String modelId = deviceModelInstancesCache.getModelIdByInstanceId(instanceId, true);
			builder.resourceAttribute("iot.digital_twin.model_id", modelId);
			String modelName;
			try {
				modelName = deviceModelInstancesCache.getModelIdByModelName(modelId, true);
				builder.resourceAttribute("iot.digital_twin.model_name", modelName);
			} catch (MissingModelException e) {
				log.severe("No model name found for modelid " + modelId);
			} catch (SQLException e) {
				log.severe("SQLException getting model name for model id " + modelId + ", " + e.getLocalizedMessage());
			}
		} catch (MissingInstanceException e) {
			log.severe("No model id found for instance id instanceId");
		} catch (SQLException e) {
			log.severe("SQLException getting model id for instanceId " + instanceId + ", " + e.getLocalizedMessage());
		}
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesDBOutputOTLP, queryX=" + queryX + ", queryY=" + queryY);
	}
}
