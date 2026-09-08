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

import java.util.Base64;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.oicsimple.IoTOutputHttpOICClientNormalizedDataSimpleSettings;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

// enabled if sending to OIC
@Requires(property = IoTOutputHttpOICClientNormalizedDataSimpleSettings.ENABLED_PROPERTY, value = "true", defaultValue = "false")
@Requires(property = IoTOutputHttpClientCommonFilterSettings.AUTH_TYPE, value = "BASIC", defaultValue = "BASIC")
// needs a endpoint
@ClientFilter(patterns = { "${" + IoTOutputHttpOICClientNormalizedDataSimpleSettings.TARGET_PATH_PROPERTY + ":"
		+ IoTOutputHttpOICClientNormalizedDataSimpleSettings.TARGET_PATH_DEFAULT + "}/**" })
@Log
@Singleton
public class IoTOutputHttpOICClientNormalizedDataBasicRequestFilter {
	@Property(name = IoTOutputHttpOICClientNormalizedDataSimpleSettings.TARGET_PATH_PROPERTY, defaultValue = IoTOutputHttpOICClientNormalizedDataSimpleSettings.TARGET_PATH_DEFAULT
			+ "/**")
	private String patternPath;
	private final String username;
	private final String password;

	@Inject
	public IoTOutputHttpOICClientNormalizedDataBasicRequestFilter(
			@Property(name = IoTOutputHttpOICClientNormalizedDataSimpleSettings.USERNAME_PROPERTY, defaultValue = "") String username,
			@Property(name = IoTOutputHttpOICClientNormalizedDataSimpleSettings.PASSWORD_BASE64_PROPERTY, defaultValue = "") String passwordBase64) {
		if ((username == null) || (username.length() == 0)) {
			this.username = null;
		} else {
			this.username = username;
		}
		if (passwordBase64.length() > 0) {
			this.password = new String(Base64.getDecoder().decode(passwordBase64));
		} else {
			this.password = "";
		}
	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) {
		log.info("Filter authenticated");
		if (username != null) {
			log.info(() -> "Adding user auth username=" + this.username);
			request.basicAuth(this.username, this.password);
		}
		log.info(() -> "Request uri " + request.getUri().toASCIIString());
		log.info(() -> "Request path " + request.getPath());
		log.info(() -> "Request params = " + request.getParameters().asMap().toString());
		log.info(() -> "Request headers = " + request.getHeaders().asMap().toString());
		log.info(() -> "Request body " + request.getBody(String.class).orElse("No body set"));
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Post Construct for IoTOutputHttpOICClientNormalizedDataBasicRequestFilter username=" + this.username
				+ ", password=" + password + " pattern =" + patternPath);
	}
}