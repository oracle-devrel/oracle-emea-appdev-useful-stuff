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

import java.time.Duration;
import java.time.LocalDateTime;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBCredentials;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;

@Singleton
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_X)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_Y)
@Log
public class TimeSeriesDBOAuthTokenRetriever {
	@Inject
	private TimeSeriesDBCredentials tsDBuserCredentials;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_RENEWAL_PREEMPT, defaultValue = "PT60S")
	private Duration renewalPreempt;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_X)
	private String queryX;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_Y)
	private String queryY;
	@Getter
	private LocalDateTime currentTokenRenewTime = null;

	private String currentToken = null;
	@Getter
	private String tokenType;

	@Inject
	private TimeSeriesDBOAuthClient authClient;

	/**
	 * for testing only
	 * 
	 */
	public void deleteTokenDetails() {
		this.currentToken = null;
		this.currentTokenRenewTime = null;
		this.tokenType = null;
	}

	/**
	 * for testing only
	 * 
	 */
	public void forceTokenRetrievalAfter(Duration expiryOffset) {
		currentTokenRenewTime = LocalDateTime.now().plus(expiryOffset);
	}

	public synchronized String getToken() throws OAuthTokenRetrievalException {
		if ((currentToken == null) || (currentTokenRenewTime == null)
				|| LocalDateTime.now().isAfter(currentTokenRenewTime)) {
			OAuthTokenResponse atr;
			try {
				atr = authClient.getOAuthToken(queryX, queryY, tsDBuserCredentials);
			} catch (HttpClientException e) {
				throw new OAuthTokenRetrievalException("Problem getting the OAuth token " + e.getLocalizedMessage(), e);
			}
			this.currentToken = atr.getAccessToken();
			this.tokenType = atr.getTokenType();
			// get a new renewal time, allow 60 seconds for processing the renewal if we
			// need to, yes we should probably allow for better control or retrieval times
			// but this is a demo, and not supposed to be production.
			this.currentTokenRenewTime = LocalDateTime.now().plusSeconds(atr.getExpiresIn()).minus(renewalPreempt);
		}
		// we have a current token and it is still valid
		return currentToken;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesDBOAuthTokenRetriever tsDBuserCredentials=" + tsDBuserCredentials
				+ ", queryX=" + queryX + ", queryY=" + queryY);
	}
}