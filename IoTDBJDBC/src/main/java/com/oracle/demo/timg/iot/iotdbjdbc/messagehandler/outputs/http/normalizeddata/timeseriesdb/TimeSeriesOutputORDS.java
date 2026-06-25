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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.OAuthTokenRetrievalException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth.TimeSeriesDBOAuthTokenRetriever;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp.OtlpMetricsJsonBuilder;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp.OtlpNormalizedDataMetricMapper;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ORDER)
@Log
// TODO, remove this context
@Context
public class TimeSeriesOutputORDS implements NormalizedDataMessageHandler {

	private final int order;
	private final boolean sentDataIsCompleted;
	private final String metricNamePrefix = "iot.normalized";
	@Inject
	private TimeSeriesDBOAuthTokenRetriever authTokenRetriever;

	@Inject
	public TimeSeriesOutputORDS(@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ORDER) int order,
			@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_SENT_DATA_IS_COMPLETED, defaultValue = "true") boolean sentDataIsCompleted) {
		this.order = order;
		this.sentDataIsCompleted = sentDataIsCompleted;
	}

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
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info("Getting auth token");
		log.info(authTokenRetriever.getToken() + " of type " + authTokenRetriever.getTokenType());
		return new NormalizedData[0];
	}

	long resetTime = System.currentTimeMillis();

	@Scheduled(fixedRate = "60s", initialDelay = "20s")
	public void resetToken() {
		log.info("Setting token expiry in 1 milis from now");
		authTokenRetriever.forceTokenRetrievalAfter(Duration.ofMillis(1));
		resetTime = System.currentTimeMillis() + 60000;
		log.info("Token should expire for next get token call");
	}

	@Scheduled(fixedRate = "10s", initialDelay = "20s")
	public void testToken() {
		long timetoreset = (resetTime - System.currentTimeMillis()) / 1000;
		log.info("Getting token, seconds to reset is " + timetoreset);
		try {
			log.info("Token retrieved is " + authTokenRetriever.getToken());
		} catch (OAuthTokenRetrievalException e) {
			log.severe("Exception getting token");
			e.printStackTrace();
		}
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesOutputORDS");
	}

	public OtlpMetricsJsonBuilder addGauge(OtlpMetricsJsonBuilder builder, NormalizedData input) {
		return builder.addGauge(metricName(input), null, null, metricValue(input), observedAt(input),
				attributes(input));
	}

	public Map<String, Object> attributes(NormalizedData input) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		putIfPresent(attributes, "iot.digital_twin.instance_id", input.getDigitalTwinInstanceId());
		putIfPresent(attributes, "iot.content.path", input.getContentPath());
		putIfPresent(attributes, "iot.content.type", input.getContentType());
		if (input.getContentJsonType() != null) {
			attributes.put("iot.content.json_type", input.getContentJsonType().toString());
		}
		return attributes;
	}

	public String metricName(NormalizedData input) {
		String contentPath = input.getContentPath();
		if (!hasText(contentPath)) {
			return metricNamePrefix + ".value";
		}
		String sanitizedPath = contentPath.strip().replace('\\', '/').replaceAll("^/+", "").replace('/', '.')
				.replaceAll("[^A-Za-z0-9_.-]+", "_").replaceAll("\\.+", ".");
		if (!hasText(sanitizedPath)) {
			sanitizedPath = "value";
		}
		return metricNamePrefix + "." + sanitizedPath;
	}

	public BigDecimal metricValue(NormalizedData input) {
		String rawValue = input.getContent();
		if (!hasText(rawValue) && input.getContentJsonValue() != null) {
			rawValue = input.getContentJsonValue().toString();
		}
		if (!hasText(rawValue)) {
			throw new IllegalArgumentException("NormalizedData content is empty and cannot be converted to a metric");
		}
		String normalized = rawValue.strip();
		if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
			normalized = normalized.substring(1, normalized.length() - 1);
		}
		return new BigDecimal(normalized);
	}

	public Instant observedAt(NormalizedData input) {
		String timeObserved = input.getTimeObserved();
		if (!hasText(timeObserved)) {
			return Instant.now();
		}
		String normalized = timeObserved.strip();
		try {
			return Instant.parse(normalized);
		} catch (RuntimeException e) {
			// Try the next common shape.
		}
		String isoLike = normalized.indexOf('T') >= 0 ? normalized : normalized.replace(' ', 'T');
		try {
			return OffsetDateTime.parse(isoLike).toInstant();
		} catch (RuntimeException e) {
			// Try a local timestamp and assume UTC.
		}
		return LocalDateTime.parse(isoLike).toInstant(ZoneOffset.UTC);
	}

	private static void putIfPresent(Map<String, Object> target, String key, Object value) {
		if (hasText(key) && value != null) {
			target.put(key, value);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public static Map<String, Object> constructOtlpMetricsPayload(NormalizedData normalizedData) {
		OtlpNormalizedDataMetricMapper mapper = new OtlpNormalizedDataMetricMapper();
		OtlpMetricsJsonBuilder builder = OtlpMetricsJsonBuilder.create().service("IoTDBJDBC")
				.resourceAttribute("digitaltwin.id", normalizedData.getDigitalTwinInstanceId())
				.resourceAttribute("digitaltwin.model", "local-example")
				.scope("com.oracle.demo.timg.iot.iotdbjdbc.examples", "1.0.0");

		mapper.addGauge(builder, normalizedData);
		return builder.build();
	}
}
