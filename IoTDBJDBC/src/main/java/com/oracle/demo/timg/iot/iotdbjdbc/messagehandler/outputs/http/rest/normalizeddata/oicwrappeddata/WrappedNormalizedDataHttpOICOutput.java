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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.oicwrappeddata;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.NormalizedDataMetadataTransfer;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
// need the username and password
@Requires(property = IoTOutputHttpOICClientWrappedNormalizedDataSettings.URL)
@Requires(property = IoTOutputHttpOICClientWrappedNormalizedDataSettings.ENABLED_PROPERTY, value = "true", defaultValue = "false")
@Requires(property = IoTOutputHttpOICClientWrappedNormalizedDataSettings.ORDER_PROPERTY)
@Log
public class WrappedNormalizedDataHttpOICOutput implements NormalizedDataMessageHandler {
	private final WrappedNormalizedDataIoTOutputHttpOICClient wrappedHttpOicClient;
	private final DeviceModelInstancesCache deviceModelInstancesCache;
	private final int order;
	private final boolean sentDataIsCompleted;
	private final String targetUrl;
	private final boolean sendtojsonobject;
	@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_PROPERTY, defaultValue = IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_DEFAULT)
	private String path;

	@Inject
	public WrappedNormalizedDataHttpOICOutput(WrappedNormalizedDataIoTOutputHttpOICClient wrappedHttpOicClient,
			DeviceModelInstancesCache deviceModelInstancesCache,
			@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.ORDER_PROPERTY) int order,
			@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.URL) String targetUrl,
			@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.SENT_DATA_IS_COMPLETED_PROPERTY, defaultValue = "true") boolean sentDataIsCompleted,
			@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.SEND_TO_JSON_PROPERTY, defaultValue = "true") boolean sendtojsonobject) {
		log.info("In wrapped normalized data http oic client constructor");
		this.wrappedHttpOicClient = wrappedHttpOicClient;
		this.deviceModelInstancesCache = deviceModelInstancesCache;
		this.order = order;
		this.sentDataIsCompleted = sentDataIsCompleted;
		this.targetUrl = targetUrl;
		this.sendtojsonobject = sendtojsonobject;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info(() -> "NormalizedData is " + input);
		HttpResponse<Void> result;
		NormalizedDataMetadataTransfer normalizedDataTransfer = NormalizedDataMetadataTransfer.buildTransfer(input,
				deviceModelInstancesCache);
		try {
			if (sendtojsonobject) {
				log.info(() -> "Making OIC call, Sending wrapped json to json object with content of "
						+ normalizedDataTransfer);
				result = wrappedHttpOicClient.postWrappedNormalizedDataAsJsonToJsonObject(normalizedDataTransfer);
			} else {
				log.info(() -> "Making OIC call, Sending wrapped json to string with content of "
						+ normalizedDataTransfer);
				result = wrappedHttpOicClient.postWrappedNormalizedDataAsJsonToString(normalizedDataTransfer);
			}
		} catch (HttpClientException e) {
			log.warning("HttpOICClient exception making call postWrappedNormalizedDataAsJsonToXXXX - "
					+ e.getLocalizedMessage());
			NormalizedData[] returnResp = new NormalizedData[1];
			returnResp[0] = input;
			return returnResp;
		}

		log.info("() -> Send result is " + result.getStatus());

		NormalizedData results[];
		if (result.getStatus() == HttpStatus.OK) {
			if (sentDataIsCompleted) {
				results = new NormalizedData[1];
				results[0] = input;
			} else {
				results = new NormalizedData[0];
			}
		} else {
			results = new NormalizedData[1];
			results[0] = input;
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "IoT HTTP Wrapped Normalized Data OIC Client";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " targetting " + targetUrl + " and path " + path;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("WrappedNormalizedDataHttpOICSimpleOutput " + this.getConfig());
	}

}
