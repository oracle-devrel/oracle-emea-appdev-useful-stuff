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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.rawdata;

import java.util.Base64;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.HttpOutputType;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.InvalidHttpOutputTypeException;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.common.NotAStringBasedMediaType;

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
@Requires(property = "micronaut.http.services.rawdataiotoutputhttpclient.url")
@Requires(property = "messagehandler.output.rawdata.httpclient.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.rawdata.httpclient.order")
@Log
public class RawDataHttpOutput implements RawDataMessageHandler {
	private final RawDataIoTOutputHttpClient httpClient;
	private final int order;
	private final HttpOutputType type;
	private final boolean useAuthentication;
	private final boolean sentDataIsCompleted;
	private final String targetUrl;

	@Inject
	public RawDataHttpOutput(RawDataIoTOutputHttpClient httpClient,
			@Property(name = "messagehandler.output.rawdata.httpclient.order") int order,
			@Property(name = "messagehandler.output.rawdata.httpclient.type", defaultValue = "STRING") HttpOutputType type,
			@Property(name = "messagehandler.output.rawdata.httpclient.useauthentication", defaultValue = "false") boolean useAuthentication,
			@Property(name = "messagehandler.output.rawdata.httpclient.sentdataiscompleted", defaultValue = "true") boolean sentDataIsCompleted,
			@Property(name = "micronaut.http.services.rawdataiotoutputhttpclient.url", defaultValue = "URL is missing") String targetUrl) {
		this.httpClient = httpClient;
		this.order = order;
		this.type = type;
		this.useAuthentication = useAuthentication;
		this.sentDataIsCompleted = sentDataIsCompleted;
		this.targetUrl = targetUrl;
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		log.info(() -> "RawData is " + input);
		HttpResponse<String> result;
		switch (type) {
		case BASE64_BYTES: {
			String bodyContent = Base64.getEncoder().encodeToString(input.getContent());
			if (useAuthentication) {
				try {
					log.info(() -> "Making authenticated call, Sending base64 content of " + input.getContent());
					result = httpClient.postRawDataAuthenticatedAsBase64(input.getDigitalTwinInstanceId(),
							input.getEndpoint(), input.getTimeReceived(), bodyContent);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postRawDataAuthenticatedAsBase64 - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					RawData[] returnResp = new RawData[1];
					returnResp[0] = input;
					return returnResp;
				}
			} else {
				try {
					log.info(() -> "Making unauthenticated call , Sending base64 content of " + input.getContent());
					result = httpClient.postRawDataUnauthenticatedAsBase64(input.getDigitalTwinInstanceId(),
							input.getEndpoint(), input.getTimeReceived(), bodyContent);
				} catch (HttpClientException e) {
					log.warning("HttpClient exception making call postRawDataUnauthenticatedAsBase64 - "
							+ e.getLocalizedMessage());
					e.printStackTrace();
					RawData[] returnResp = new RawData[1];
					returnResp[0] = input;
					return returnResp;
				}
			}
			log.info("() -> Send result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;
		}
		case STRING: {
			if (input.getMediaType().isTextBased()) {
				if (useAuthentication) {
					try {
						log.info(() -> "Making authenticated call, Sending string content of " + input.getContent());
						result = httpClient.postRawDataAuthenticatedAsString(input.getDigitalTwinInstanceId(),
								input.getEndpoint(), input.getTimeReceived(), input.getContentString());
					} catch (HttpClientException e) {
						log.warning("HttpClient exception making call postRawDataAuthenticatedAsString - "
								+ e.getLocalizedMessage());
						e.printStackTrace();
						RawData[] returnResp = new RawData[1];
						returnResp[0] = input;
						return returnResp;
					}
				} else {
					try {
						log.info(() -> "Making unauthenticated call , Sending string content of " + input.getContent());
						result = httpClient.postRawDataUnauthenticatedAsString(input.getDigitalTwinInstanceId(),
								input.getEndpoint(), input.getTimeReceived(), input.getContentString());
					} catch (HttpClientException e) {
						log.warning("HttpClient exception making call postRawDataUnauthenticatedAsString - "
								+ e.getLocalizedMessage());
						e.printStackTrace();
						RawData[] returnResp = new RawData[1];
						returnResp[0] = input;
						return returnResp;
					}
				}
			} else {
				throw new NotAStringBasedMediaType("Media type " + input.getMediaType());
			}
			log.info("() -> Send result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;
		}
		case JSON_OBJECT: {
			RawDataTransfer rawDataTransfer = RawDataTransfer.buildRawDataTransfer(input);
			if (input.getMediaType().isTextBased()) {
				if (useAuthentication) {
					try {
						log.info(() -> "Making authenticated call, Sending json object with content of "
								+ input.getContent());
						result = httpClient.postRawDataAuthenticatedAsJsonObject(input.getDigitalTwinInstanceId(),
								input.getEndpoint(), input.getTimeReceived(), rawDataTransfer);
					} catch (HttpClientException e) {
						log.warning("HttpClient exception making call postRawDataAuthenticatedAsString - "
								+ e.getLocalizedMessage());
						e.printStackTrace();
						RawData[] returnResp = new RawData[1];
						returnResp[0] = input;
						return returnResp;
					}
				} else {
					try {
						log.info(() -> "Making unauthenticated call , Sending json object with content of "
								+ input.getContent());
						result = httpClient.postRawDataUnauthenticatedAsJsonObject(input.getDigitalTwinInstanceId(),
								input.getEndpoint(), input.getTimeReceived(), rawDataTransfer);
					} catch (HttpClientException e) {
						log.warning("HttpClient exception making call postRawDataUnauthenticatedAsString - "
								+ e.getLocalizedMessage());
						e.printStackTrace();
						RawData[] returnResp = new RawData[1];
						returnResp[0] = input;
						return returnResp;
					}
				}
			} else {
				throw new NotAStringBasedMediaType("Media type " + input.getMediaType());
			}
			log.info("() -> Send result is " + result.getStatus() + " with body "
					+ result.getBody().orElse("No body content returned"));
			break;
		}
		default:
			throw new InvalidHttpOutputTypeException("Processing type " + type + " is unknown");
		}
		RawData results[];
		if (result.getStatus() == HttpStatus.OK) {
			if (sentDataIsCompleted) {
				results = new RawData[1];
				results[0] = input;
			} else {
				results = new RawData[0];
			}
		} else {
			results = new RawData[1];
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
		return "IoT HTTP Raw Data Client";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " output type " + type + " targetUrl " + targetUrl;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("RawDataHttpOutput " + this.getConfig());
	}
}
