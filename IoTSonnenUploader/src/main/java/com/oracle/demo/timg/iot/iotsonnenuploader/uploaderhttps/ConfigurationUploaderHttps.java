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
package com.oracle.demo.timg.iot.iotsonnenuploader.uploaderhttps;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;
import com.oracle.demo.timg.iot.iotsonnenuploader.iotservicehttpsclient.IoTServiceClientHttps;
import com.oracle.demo.timg.iot.iotsonnenuploader.sonnenbatteryhttpclient.SonnenBatteryClient;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "iotservicehttps.configurationupload.enabled", value = "true", defaultValue = "false")
public class ConfigurationUploaderHttps {
	public final String SEND_TYPE_PLAIN = "plain";
	@Inject
	private SonnenBatteryClient sonnenClient;
	@Inject
	private IoTServiceClientHttps iotServiceClient;

	@Property(name = "iotservicehttps.configurationupload.sendtype", defaultValue = "PLAIN")
	private SendType sendType;

	@Inject
	private ObjectMapper mapper;

	@Scheduled(fixedRate = "${iotservicehttps.configurationupload.frequency:120s}", initialDelay = "${iotservicehttps.configurationupload.initialdelay:5s}")
	@ExecuteOn(TaskExecutors.IO)
	public SonnenConfiguration processConfiguration() {
		SonnenConfiguration conf;
		try {
			conf = sonnenClient.fetchConfiguration();
		} catch (HttpClientException e) {
			log.warning("HttpClientException getting configuration from sonnen, " + e.getLocalizedMessage()
					+ "no data to upload for service " + e.getServiceId());
			return null;
		}
		log.info("Retrieved configuration from battery : " + conf);
		CompletableFuture<Void> publishResp;
		try {
			switch (sendType) {
			case PLAIN:
				publishResp = iotServiceClient.sendConfigurationPlainText(mapper.writeValueAsString(conf));
				break;
			case JSON:
				publishResp = iotServiceClient.sendConfigurationJson(conf);
				break;
			default:
				log.severe("Unknown sendtype " + sendType + " cannot upload to IOT Service");
				return null;
			}
		} catch (IOException e) {
			log.warning("Unable to serialize configuration object to a string " + conf);
			return null;
		}
		publishResp.thenRun(() -> log.info("Published configuration using http with payload type " + sendType))
				.exceptionally(e -> {
					log.warning("Problem configuration status using http with payload type " + sendType
							+ ", exception details " + e.getLocalizedMessage());
					return null;
				});
		return conf;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for configuration https uploader, sendtype=" + sendType);
	}
}
