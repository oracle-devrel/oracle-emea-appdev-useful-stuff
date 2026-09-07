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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.requestfilters;

import java.time.format.DateTimeFormatter;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.oicwrappeddata.IoTOutputHttpOICClientWrappedNormalizedDataSettings;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

// enabled if sending to OIC
@Requires(property = IoTOutputHttpOICClientWrappedNormalizedDataSettings.ENABLED_PROPERTY, value = "true", defaultValue = "false")
@Requires(property = IoTOutputHttpClientCommonFilterSettings.AUTH_TYPE, value = "OAUTH2")

// needs a endpoint
@ClientFilter(patterns = { "${" + IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_PROPERTY + ":"
		+ IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_DEFAULT + "}/**" })
@Log
public class IoTOutputHttpOICClientWrappedNormalizedDataOAuthRequestFilter {
	@Property(name = IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_PROPERTY, defaultValue = IoTOutputHttpOICClientWrappedNormalizedDataSettings.TARGET_PATH_DEFAULT
			+ "/**")
	private String patternPath;
	private final OICOAuthApplicationTokenRetriever oauthTokenRetriever;

	@Inject
	public IoTOutputHttpOICClientWrappedNormalizedDataOAuthRequestFilter(
			OICOAuthApplicationTokenRetriever oauthTokenRetriever) {
		this.oauthTokenRetriever = oauthTokenRetriever;
	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) throws IDCSOAuthTokenRetrievalException {
		String token;
		try {
			token = oauthTokenRetriever.getToken();
		} catch (IDCSOAuthTokenRetrievalException e) {
			log.severe("Unable to get OAuth token for OIC, " + e.getLocalizedMessage());
			throw e;
		}
		request.bearerAuth(token);
		log.info(() -> "Added OAuth token");
		log.info(() -> "Request uri " + request.getUri().toASCIIString());
		log.info(() -> "Request path " + request.getPath());
		log.info(() -> "Request params = " + request.getParameters().asMap().toString());
		log.info(() -> "Request headers = " + request.getHeaders().asMap().toString());
		log.info(() -> "Request body " + request.getBody(String.class).orElse("No body set"));
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Post Construct for IoTOutputHttpOICClientWrappedNormalizedDataOAuthRequestFilter token type="
				+ oauthTokenRetriever.getTokenType() + ", renewal time="
				+ oauthTokenRetriever.getCurrentTokenRenewTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

	}
}