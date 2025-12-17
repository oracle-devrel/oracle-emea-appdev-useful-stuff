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
package com.oracle.demo.timg.iot.iotsonnenuploader.uploadermqtt;

import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;
import com.oracle.demo.timg.iot.iotsonnenuploader.mqtt.MqttSonnenBatteryPublisher;
import com.oracle.demo.timg.iot.iotsonnenuploader.sonnenbatteryhttpclient.SonnenBatteryClient;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "mqtt.statusupload.enabled", value = "true", defaultValue = "false")
public class StatusUploaderMqtt {
	@Inject
	private SonnenBatteryClient client;
	@Inject
	private MqttSonnenBatteryPublisher mqttSonnenBatteryPublisher;

	@ExecuteOn(TaskExecutors.IO)
	@Scheduled(fixedRate = "${mqtt.statusupload.frequency:10s}", initialDelay = "${mqtt.statusupload.initialdelay:10s}")
	public SonnenStatus processStatus() {
		SonnenStatus status;
		try {
			status = client.fetchStatus();
		} catch (HttpClientException e) {
			log.warning("HttpClientExcepton getting status from sonnen due to " + e.getLocalizedMessage()
					+ ", no data to upload, for service " + e.getServiceId());
			return null;
		}
		log.info("Retrieved status from battery : " + status);
		try {
			CompletableFuture<Void> publishResp = mqttSonnenBatteryPublisher.publishSonnenStatus(status);
			publishResp.thenRun(() -> log.info("Published status to mqtt"));
		} catch (Exception e) {
			log.warning("Problem publishing status to mqtt, " + e.getLocalizedMessage());
		}
		return status;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for status mqtt uploader");
	}
}
