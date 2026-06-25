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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.Getter;

//{"token":{"url":"https://G9051959400A6D8-TELEMETRY.adb.uk-london-1.oraclecloudapps.com/tel/token?x=zep-111&y=9019"},"otlp":{"url":"https://G9051959400A6D8-TELEMETRY.adb.uk-london-1.oraclecloudapps.com/tel/v1/metrics?x=zep-111&y=9005"}}

@Serdeable
@Data
public class TimeSeriesEndpointsResponse {
	private URLHolder token;
	private URLHolder otlp;

	@Serdeable
	public class URLHolder {
		@Getter
		private String url;
		private Map<String, String> queryParams;

		public String getQueryParam(String name) throws URISyntaxException {
			if (queryParams == null) {
				getQueryParams();
			}
			return queryParams.get(name);
		}

		// get the first query param that matches the provided name.
		private void getQueryParams() throws URISyntaxException {
			URI uri = new URI(url);
			queryParams = new HashMap<>();
			String query = uri.getRawQuery();
			if (query == null || query.isEmpty()) {
				return;
			}
			for (String pair : query.split("&")) {
				String[] keyValue = pair.split("=", 2);
				String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
				String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
				queryParams.put(key, value);
			}
			return;
		}
	}
}
