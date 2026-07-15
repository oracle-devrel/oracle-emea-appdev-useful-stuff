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

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBCredentials;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints.TimeSeriesEndpointsQueryParams;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints.TimeSeriesEndpointsRetriever;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;

@Singleton
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Log
public class TimeSeriesDBOAuthTokenRetriever {
	@Inject
	private ObjectMapper mapper;
	private final TimeSeriesDBCredentials tsDBuserCredentials;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_RENEWAL_PREEMPT, defaultValue = "PT60S")
	private Duration renewalPreempt;
	private final String queryX;
	private final String queryY;
	@Getter
	private LocalDateTime currentTokenRenewTime = null;

	private String currentToken = null;
	@Getter
	private String tokenType;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_DEBUG_OAUTH, defaultValue = "false")
	boolean debugOauth = false;

	@Inject
	public TimeSeriesDBOAuthTokenRetriever(TimeSeriesEndpointsRetriever endpointsRetriever,
			TimeSeriesDBCredentials tsDBuserCredentials) {
		TimeSeriesEndpointsQueryParams endpointsQueryParams = endpointsRetriever.getQueryParams();
		this.queryX = endpointsQueryParams.getOauthQueryX();
		this.queryY = endpointsQueryParams.getOauthQueryY();
		log.info("Constructor params for OAUTH queryX=" + queryX + ", queryY=" + queryY);
		this.tsDBuserCredentials = tsDBuserCredentials;
		if (debugOauth) {
			log.info("Oauth retrieve credentials are " + tsDBuserCredentials);
		}
	}

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
			log.info("Retrieveing oauth token from time series DB");
			try {
				String credentials = mapper.writeValueAsString(tsDBuserCredentials);
				log.fine(() -> "Setting body to " + credentials);
				String oauthRespStr = authClient.getOAuthToken(queryX, queryY, credentials);
				log.fine(() -> "OAuth response is " + oauthRespStr);
				atr = mapper.readValue(oauthRespStr, OAuthTokenResponse.class);
			} catch (HttpClientException e) {
				log.warning("Problem getting the OAuth token " + e.getLocalizedMessage());
				throw new OAuthTokenRetrievalException("Problem getting the OAuth token " + e.getLocalizedMessage(), e);
			} catch (IOException e) {
				log.warning("IOException in mapping, this should not happen " + e.getLocalizedMessage());
				throw new OAuthTokenRetrievalException(
						"IOException in mapping, this should not happen " + e.getLocalizedMessage(), e);
			}
			// make sure that the fields we need have been set
			if (atr.checkInvalid()) {
				throw new OAuthTokenRetrievalException("Returned token details have null or missing values " + atr
						+ ", maybe an authentication issue. Check the properties username, password, tenancyocid and databasename under "
						+ TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH);
			}
			this.currentToken = atr.getAccessToken();
			this.tokenType = atr.getTokenType();
			// get a new renewal time, allow 60 seconds for processing the renewal if we
			// need to, yes we should probably allow for better control or retrieval times
			// but this is a demo, and not supposed to be production.
			this.currentTokenRenewTime = LocalDateTime.now().plusSeconds(atr.getExpiresIn()).minus(renewalPreempt);
			log.info("Got token details with type " + atr.getTokenType() + " and expiring in " + atr.getExpiresIn()
					+ " seconds");
		} else {
			log.info("Using existing token");
		}
		// we have a current token and it is still valid
		return currentToken;
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesDBOAuthTokenRetriever tsDBuserCredentials="
				+ tsDBuserCredentials.safeToString() + ", queryX=" + queryX + ", queryY=" + queryY);
		if (debugOauth) {
			log.info("Full credentials are " + tsDBuserCredentials.toString());
		}
	}
}