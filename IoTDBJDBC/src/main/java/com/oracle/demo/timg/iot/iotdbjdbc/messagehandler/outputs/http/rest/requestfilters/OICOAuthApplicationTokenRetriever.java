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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.exceptions.HttpClientException;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;

@Singleton
@Requires(property = IoTOutputHttpClientCommonFilterSettings.AUTH_TYPE, value = "OAUTH2")
@Requires(property = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_ID)
@Requires(property = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_SECRET)
@Requires(property = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_SCOPE)

@Log
public class OICOAuthApplicationTokenRetriever {
	// this basically automates the following request (the text below applies
	// micronaut substitutions)
	// curl -u 'client id:client secret' \
	// -X POST
	// https://${micronaut.http.services.oicoauthtoken.url}/${messagehandler.output.iotoutputhttprestclient.filtersettings.oauth.path:/oauth2/v1/token}
	// \
	// --data
	// 'grant_type=client_credentials&scope=${messagehandler.output.iotoutputhttprestclient.filtersettings.oauth.scope}'
	private final static String GRANT_TYPE_DEFAULT = "client_credentials";
	private final static String AUTH_BASIC = "BASIC";
	private final String oauthClientId;
	private final String oauthClientSecret;
	private final String oauthGrantType;
	private final String oauthScope;
	private final String credentialsBase64;

	@Inject
	public OICOAuthApplicationTokenRetriever(
			@Property(name = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_ID) String clientId,
			@Property(name = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_SECRET) String clientSecret,
			@Property(name = IoTOutputHttpClientCommonFilterSettings.OAUTH_CLIENT_SCOPE) String scope,
			@Property(name = IoTOutputHttpClientCommonFilterSettings.OAUTH_GRANT_TYPE, defaultValue = GRANT_TYPE_DEFAULT) String grantType) {
		this.oauthClientId = clientId;
		this.oauthClientSecret = clientSecret;
		this.oauthScope = scope;
		this.oauthGrantType = grantType;
		String credentials = oauthClientId + ":" + oauthClientSecret;
		credentialsBase64 = AUTH_BASIC + " " + Base64.getEncoder().encodeToString(credentials.getBytes());
	}

	@Getter
	private LocalDateTime currentTokenRenewTime = null;

	private String currentToken = null;
	@Getter
	private String tokenType;

	@Inject
	private OICOAuthTokenRequester authTokenRequester;

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

	public String getToken() throws IDCSOAuthTokenRetrievalException {
		if ((currentToken == null) || (currentTokenRenewTime == null)
				|| LocalDateTime.now().isAfter(currentTokenRenewTime)) {
			AuthTokenResponse atr;
			try {
				atr = authTokenRequester.getOAuthToken(credentialsBase64, oauthScope, oauthGrantType);
			} catch (HttpClientException e) {
				throw new IDCSOAuthTokenRetrievalException("Problem getting the OAuth token " + e.getLocalizedMessage(),
						e);
			}
			this.currentToken = atr.getAccessToken();
			this.tokenType = atr.getTokenType();
			// get a new renewal time, allow 60 seconds for processing the renewal if we
			// need to, yes we should probably allow for better control or retrieval times
			// but this is a demo, and not supposed to be production.
			this.currentTokenRenewTime = LocalDateTime.now().plusSeconds(atr.getExpiresIn() - 60);
		}
		// we have a current token and it is still valid
		return currentToken;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Startup event received for OICOAuthApplicationTokenRetriever scope=" + oauthScope + ", grantType="
				+ oauthGrantType);
	}
}