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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http;

import java.util.Base64;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.output.rawdata.httpclient.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.output.rawdata.httpclient.enabled.order")
@Log
public class RawDataHttpOutput implements RawDataMessageHandler {
	private final IoTOutputHttpClient httpClient;
	private final int order;
	private final HttpOutputType type;

	@Inject
	public RawDataHttpOutput(IoTOutputHttpClient httpClient,
			@Property(name = "messagehandler.output.rawdata.httpclient.enabled.order") int order,
			@Property(name = "messagehandler.output.rawdata.httpclient.enabled.type", defaultValue = "STRING") HttpOutputType type) {
		this.httpClient = httpClient;
		this.order = order;
		this.type = type;
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		log.finer(() -> "RawData is " + input);
		switch (type) {
		case BASE64_BYTES: {
			String bodyContent = Base64.getEncoder().encodeToString(input.getContent());
			boolean result = httpClient.postRawDataAsBase64(input.getDigitalTwinInstanceId(), input.getEndpoint(),
					input.getContentType(), bodyContent);
			RawData results[] = new RawData[1];
			results[0] = input;
			return results;
		}
		case STRING: {
			if (input.getMediaType().isTextBased()) {
				boolean result = httpClient.postRawDataAsBase64(input.getDigitalTwinInstanceId(), input.getEndpoint(),
						input.getContentType(), input.getContentString());

				RawData results[] = new RawData[1];
				results[0] = input;
				return results;
			} else {
				throw new NotAStringBasedMediaType("Media type "+)
			}
		}
		default:
			throw new InvalidHttpOutputTypeException("Processing type " + type + " is unknown");
			break;

		}
		return new RawData[0];
		RawData results[];
		if (input.getContentType().equalsIgnoreCase(type)) {
			log.fine(() -> input.getContentType() + " is the same type as  " + type);

		} else {
			log.fine(() -> input.getContentType() + " is a different type than  " + type);
			results = new RawData[0];
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "IoT HTTP Client";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " output type " + type;
	}

}
