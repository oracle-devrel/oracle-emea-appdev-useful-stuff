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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;

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
@Requires(property = "micronaut.http.services.normalizeddataiotoutputhttpoicclient.url")
@Requires(property = "messagehandler.output.normalizeddata.httpoicclient.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.normalizeddata.httpoicclient.order")
@Log
public class NormalizedDataHttpOICOutput implements NormalizedDataMessageHandler {
	private final NormalizedDataIoTOutputHttpOICClient httpOicClient;
	private final int order;
	private final boolean sentDataIsCompleted;
	private final String targetUrl;
	private final boolean sendtojsonobject;

	@Inject
	public NormalizedDataHttpOICOutput(NormalizedDataIoTOutputHttpOICClient httpOicClient,
			@Property(name = "messagehandler.output.normalizeddata.httpoicclient.order") int order,
			@Property(name = "micronaut.http.services.normalizeddataiotoutputhttpoicclient.url") String targetUrl,
			@Property(name = "messagehandler.output.normalizeddata.httpoicclient.sentdataiscompleted", defaultValue = "true") boolean sentDataIsCompleted,
			@Property(name = "messagehandler.output.normalizeddata.httpoicclient.sendtojsonobject", defaultValue = "true") boolean sendtojsonobject) {
		log.info("In normalized data http client constructor");
		this.httpOicClient = httpOicClient;
		this.order = order;
		this.sentDataIsCompleted = sentDataIsCompleted;
		this.targetUrl = targetUrl;
		this.sendtojsonobject = sendtojsonobject;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info(() -> "NormalizedData is " + input);
		HttpResponse<Void> result;
		NormalizedDataTransfer normalizedDataTransfer = NormalizedDataTransfer.buildNormalizedDataTransfer(input);
		try {
			if (sendtojsonobject) {
				log.info(
						() -> "Making OIC call, Sending json to json object with content of " + normalizedDataTransfer);
				result = httpOicClient.postNormalizedDataAsJsonToJsonObject(normalizedDataTransfer);
			} else {
				log.info(() -> "Making OIC call, Sending json to string with content of " + normalizedDataTransfer);
				result = httpOicClient.postNormalizedDataAsJsonToString(normalizedDataTransfer);
			}
		} catch (HttpClientException e) {
			log.warning(
					"HttpOICClient exception making call postNormalizedDataAsJsonObject - " + e.getLocalizedMessage());
			e.printStackTrace();
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
		return "IoT HTTP Normalized Data OIC Client";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " targetting " + targetUrl;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("NormalizedDataHttpOICOutput " + this.getConfig());
	}

}
