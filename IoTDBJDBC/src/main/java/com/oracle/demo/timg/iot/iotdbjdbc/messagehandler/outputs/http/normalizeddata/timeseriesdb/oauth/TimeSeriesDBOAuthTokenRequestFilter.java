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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.oauth;

import java.util.UUID;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBCredentials;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

/*
 * 
 */
// @ClientFilter(patterns = { "${" + TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_PATH + "}" })
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@ClientFilter(patterns = "/tel/token")
@Log
public class TimeSeriesDBOAuthTokenRequestFilter {
	public final static String HEADER_REQUEST_ID = "Request-Id";
	private final String username;
	private final String password;

	@Inject
	public TimeSeriesDBOAuthTokenRequestFilter(TimeSeriesDBCredentials credentials) {
		log.info("In TimeSeriesDBOAuthTokenRequestFilter constructor");
		this.username = credentials.getUsername();
		this.password = credentials.getPassword();
	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) {
		// need to add a request id, apparently this should be unique
		String randomUUID = UUID.randomUUID().toString();
		request.getHeaders().add(HEADER_REQUEST_ID, randomUUID);
		log.info("Added header " + HEADER_REQUEST_ID + " with id " + randomUUID);
		log.info("request uri " + request.getUri().toASCIIString());
		log.info("request path " + request.getPath());
		log.info("request params = " + request.getParameters().asMap().toString());
		log.info("request headers = " + request.getHeaders().asMap().toString());
		log.info("Request body " + request.getBody(String.class).orElse("No body set"));
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesDBOAuthTokenRequestFilter username=" + this.username);
	}
}