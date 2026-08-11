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

import java.util.Base64;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.HttpOutputType;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.InvalidHttpOutputTypeException;

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
@Requires(property = "micronaut.http.services.normalizeddataiotoutputhttpclient.url")
@Requires(property = "messagehandler.output.normalizeddata.httpclient.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.normalizeddata.httpclient.order")
@Log
public class NormalizedDataHttpOutput implements NormalizedDataMessageHandler {
	private final NormalizedDataIoTOutputHttpClient httpClient;
	private final int order;
	private final HttpOutputType type;
	private final boolean useAuthentication;
	private final boolean sentDataIsCompleted;
	private final String targetUrl;

	@Inject
	public NormalizedDataHttpOutput(NormalizedDataIoTOutputHttpClient httpClient,
			@Property(name = "messagehandler.output.normalizeddata.httpclient.order") int order,
			@Property(name = "messagehandler.output.normalizeddata.httpclient.type", defaultValue = "STRING") HttpOutputType type,
			@Property(name = "messagehandler.output.normalizeddata.httpclient.useauthentication", defaultValue = "false") boolean useAuthentication,
			@Property(name = "messagehandler.output.normalizeddata.httpclient.sentdataiscompleted", defaultValue = "true") boolean sentDataIsCompleted,
			@Property(name = "micronaut.http.services.normalizeddataiotoutputhttpclient.url", defaultValue = "URL is missing") String targetUrl) {
		log.info("In normalized data http client constructor");
		this.httpClient = httpClient;
		this.order = order;
		this.type = type;
		this.useAuthentication = useAuthentication;
		this.sentDataIsCompleted = sentDataIsCompleted;
		this.targetUrl = targetUrl;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.info(() -> "NormalizedData is " + input);
		HttpResponse<String> result;
		switch (type) {
		case BASE64_BYTES: {
			String bodyContent = Base64.getEncoder().encodeToString(input.getContent().getBytes());
			if (useAuthentication) {
				try {
					log.info("Making authenticated call , Sending base64 content of " + bodyContent);
					result = httpClient.postNormalizedDataAuthenticatedAsBase64(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), bodyContent);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataAuthenticatedAsBase64 - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			} else {
				try {
					log.info("Making unauthenticated call, Sending base64 content of " + bodyContent);
					result = httpClient.postNormalizedDataUnauthenticatedAsBase64(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), bodyContent);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataUnauthenticatedAsBase64 - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			}

			log.info("() -> Send base 64 result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;

		}
		case STRING: {
			if (useAuthentication) {
				try {
					log.info(() -> "Making authenticated call , Sending string content of " + input.getContent());
					result = httpClient.postNormalizedDataAuthenticatedAsString(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), input.getContent());
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataAuthenticatedAsString - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			} else {
				try {
					log.info(() -> "Making unauthenticated call, Sending string content of " + input.getContent());
					result = httpClient.postNormalizedDataUnauthenticatedAsString(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), input.getContent());
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataUnauthenticatedAsString - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			}
			log.info("() -> Send result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;
		}
		case JSON_OBJECT: {
			NormalizedDataTransfer normalizedDataTransfer = NormalizedDataTransfer.buildNormalizedDataTransfer(input);
			if (useAuthentication) {
				try {
					log.info(() -> "Making authenticated call , Sending json object with content of "
							+ normalizedDataTransfer);
					result = httpClient.postNormalizedDataAuthenticatedAsJsonObject(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), normalizedDataTransfer);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataAuthenticatedAsJsonObject - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			} else {
				try {
					log.info(() -> "Making unauthenticated call, Sending json object with content of "
							+ normalizedDataTransfer);
					result = httpClient.postNormalizedDataUnauthenticatedAsJsonObject(input.getDigitalTwinInstanceId(),
							input.getContentPath(), input.getTimeObserved(), normalizedDataTransfer);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postNormalizedDataUnauthenticatedAsJsonObject - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					NormalizedData[] returnResp = new NormalizedData[1];
					returnResp[0] = input;
					return returnResp;
				}
			}
			log.info("() -> Send result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;
		}
		default:
			throw new InvalidHttpOutputTypeException("Processing type " + type + " is unknown");
		}
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
		return "IoT HTTP Normalized Data Client";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " output type " + type + " targetting " + targetUrl;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("NormalizedDataHttpOutput " + this.getConfig());
	}

}
