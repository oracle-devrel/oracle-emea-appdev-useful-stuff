/*Copyright (c) 2026 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotproxygateway.gateway;

import java.util.Optional;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayStatsData;
import com.oracle.demo.timg.iot.iotproxygateway.mqtt.MqttGatewayEventPublisher;

import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Requires(property = PropertyNames.MQTT_CLIENT_UPLOAD_ENABLED, value = "true", defaultValue = "false")
@Requires(property = PropertyNames.GATEWAY_STATS_PUBLISH_ENABLED, value = "true", defaultValue = "true")
@Singleton
@Log
public class GatewayStatsDataMqttUploader {
	@Inject
	private GatewayStatsMqttUpload gatewayStats;
	private MqttGatewayEventPublisher gatewayEventPublisher;

	@Inject
	public GatewayStatsDataMqttUploader(Optional<MqttGatewayEventPublisher> gatewayEventPublisherOpt) {
		if (gatewayEventPublisherOpt.isEmpty()) {
			log.warning("gatewayEventPublisher not found, gateway stats data will not be uploaded");
			gatewayEventPublisher = null;
			return;
		} else {
			gatewayEventPublisher = gatewayEventPublisherOpt.get();
		}
	}

	@PostConstruct
	void postConstruct() {
		log.info("GatewayConfigDataMqttUploader starting operation");
	}

	/*
	 * have this use the micronaut scheduler, it's easier
	 */
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_STATS_PUBLISH_RATE + ":120s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_STATS_INITIAL_DELAY + ":30s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processStats() {
		// in theory this should not happen unless someone calls the method directly,
		// but let's do some defensive programming
		if (gatewayEventPublisher == null) {
			return;
		}
		GatewayStatsData gatewayStatsData = gatewayStats.getGatewayStatsData();
		IoTGatewayStatsData ioTGatewayStatsData = IoTGatewayStatsData.builder().payload(gatewayStatsData).build();
		log.fine(() -> "Publishing gateway stats " + ioTGatewayStatsData);
		try {
			gatewayEventPublisher.publishGatewayStats(ioTGatewayStatsData);
			gatewayStats.trackSucessfullUploadCall();
		} catch (Exception e) {
			log.warning("Exception uploading gateway stats " + ioTGatewayStatsData + ", " + e.getLocalizedMessage());
			gatewayStats.trackFailedUploadCall();
		}
	}

}
