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

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.IoTOutputHttpOICClientSettings;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

// needs the credentials

@Requires(property = IoTOutputHttpOICClientSettings.ENABLED_PROPERTY, value = "true", defaultValue = "false")
@Client(id = "normalizeddataiotoutputhttpoicclient", path = "${" + IoTOutputHttpOICClientSettings.TARGET_PATH_PROPERTY
		+ ":/api/v1/iotdata/normalizeddata/oic}")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface NormalizedDataIoTOutputHttpOICClient {
	@Post(consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<Void> postNormalizedDataAsJsonToJsonObject(@Body NormalizedDataTransfer normalizedDataTransfer);

	@Post(value = "/string", consumes = MediaType.APPLICATION_JSON)
	public HttpResponse<Void> postNormalizedDataAsJsonToString(@Body NormalizedDataTransfer normalizedDataTransfer);
}
