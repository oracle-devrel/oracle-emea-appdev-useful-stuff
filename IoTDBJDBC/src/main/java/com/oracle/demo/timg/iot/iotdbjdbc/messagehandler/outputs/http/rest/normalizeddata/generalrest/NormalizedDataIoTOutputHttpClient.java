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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.generalrest;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.transferdataobjects.NormalizedDataTransfer;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import jakarta.ws.rs.PathParam;

// needs the credentials

@Requires(property = IoTOutputHttpRestClientNormalizedDataSettings.ENABLED_PROPERTY, value = "true", defaultValue = "false")
@Client(id = "normalizeddataiotoutputhttpclient", path = "${"
		+ IoTOutputHttpRestClientNormalizedDataSettings.TARGET_PATH_PROPERTY + ":"
		+ IoTOutputHttpRestClientNormalizedDataSettings.TARGET_PATH_DEFAULT + "}")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface NormalizedDataIoTOutputHttpClient {
	@Post(value = "/authenticated/jsonobject/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> postNormalizedDataAuthenticatedAsJsonObject(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body NormalizedDataTransfer normalizedDataTransfer);

	@Post(value = "/authenticated/string/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public HttpResponse<String> postNormalizedDataAuthenticatedAsString(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body String content);

	@Post(value = "/authenticated/base64/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public HttpResponse<String> postNormalizedDataAuthenticatedAsBase64(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body String base64content);

	@Post(value = "/unauthenticated/jsonobject/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
	public HttpResponse<String> postNormalizedDataUnauthenticatedAsJsonObject(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body NormalizedDataTransfer normalizedDataTransfer);

	@Post(value = "/unauthenticated/string/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public HttpResponse<String> postNormalizedDataUnauthenticatedAsString(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body String content);

	@Post(value = "/unauthenticated/base64/{digitaltwinid}/{contentpath}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public HttpResponse<String> postNormalizedDataUnauthenticatedAsBase64(
			@PathParam("digitaltwinid") String digitaltwinid, @PathParam("contentpath") String contentpath,
			@PathParam("timestamp") String timestamp, @Body String base64content);
}
