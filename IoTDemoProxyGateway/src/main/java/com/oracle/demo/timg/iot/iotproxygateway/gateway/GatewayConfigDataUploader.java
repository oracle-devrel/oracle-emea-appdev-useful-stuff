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
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTGatewayConfigData;

import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Setter;
import lombok.extern.java.Log;

@Requires(property = PropertyNames.GATEWAY_CONFIG_PUBLISH_ENABLED, value = "true", defaultValue = "true")
@Singleton
@Log
public class GatewayConfigDataUploader {
	@Inject
	private GatewayStatsTrackingData gatewayStats;
	private final GatewayEventPublisher gatewayEventPublisher;

	@Setter
	private boolean pauseUploads = false;

	@Inject
	public GatewayConfigDataUploader(Optional<GatewayEventPublisher> gatewayEventPublisherOpt) {
		if (gatewayEventPublisherOpt.isEmpty()) {
			log.warning("gatewayEventPublisher not found, gateway config data will not be uploaded");
			gatewayEventPublisher = null;
			return;
		} else {
			gatewayEventPublisher = gatewayEventPublisherOpt.get();
		}
	}

	@PostConstruct
	void postConstruct() {
		log.info("GatewayConfigDataUploader starting operation");
	}

	/*
	 * have this use the micronaut scheduler, it's easier
	 */
	@Scheduled(fixedRate = "${" + PropertyNames.GATEWAY_CONFIG + ":1200s}", initialDelay = "${"
			+ PropertyNames.GATEWAY_CONFIG_INITIAL_DELAY + ":5s}")
	@ExecuteOn(TaskExecutors.IO)
	public void processConfiguration() {
		// in theory this should not happen unless someone calls the method directly,
		// but let's do some defensive programming
		if (gatewayEventPublisher == null) {
			log.warning("gatewayEventPublisher is null, cant send config");
			return;
		}
		if (pauseUploads) {
			log.info("Would have uploaded gateway config data but pauseUploads is " + pauseUploads);
			return;
		}
		GatewayConfigData gatewayConfigData = gatewayStats.getGatewayConfigData();
		IoTGatewayConfigData ioTGatewayConfigData = IoTGatewayConfigData.builder().payload(gatewayConfigData).build();
		log.info(() -> "Publishing gateway config " + ioTGatewayConfigData);
		try {
			gatewayEventPublisher.publishGatewayConfig(ioTGatewayConfigData);
			gatewayStats.trackSucessfullUploadCall();
		} catch (Exception e) {
			log.warning("Exception uploading gateway config " + ioTGatewayConfigData + ", " + e.getLocalizedMessage());
			gatewayStats.trackFailedUploadCall();
		}

	}
}
