/*Copyright (c) 2024 Oracle and/or its affiliates.

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
package com.oracle.timg.demos.functions.basicauth;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import com.fnproject.fn.api.FnConfiguration;
import com.fnproject.fn.api.RuntimeContext;
import com.fnproject.fn.api.httpgateway.HTTPGatewayContext;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.Base64SecretBundleContentDetails;
import com.oracle.bmc.secrets.model.SecretBundle;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicAuthFunction {
	public static final Integer HTTP_RESPONSE_OK = 200;
	public static final Integer HTTP_RESPONSE_UNAUTHORIZED = 401;

	// set this to true if you are using it for testing, note that this MUST NOT be
	// used in production as it may output base64 and clear text versions of the
	// saved and requested passwords which would end up in the logs
	public final static String CONFIG_TESTING_MODE = "testing-mode";
	public final static String DEFAULT_CONFIG_TESTING_MODE = "false";

	// The names below refer to the variable names IN THIS CODE, to look at the
	// actual
	// config names used please refer to the variables below.
	// CONFIG_AUTH_SOURCE must be set to the value if one of the constants
	// AUTH_SOURCE_CONFIG or
	// AUTH_SOURCE_VAULT (case ignored)
	// if set to the value of AUTH_SOURCE_CONFIG the text in the config
	// CONFIG_AUTH_SOURCE is used
	// if set to the value of AUTH_SOURCE_VAULT then the config in
	// CONFIG_AUTH_SOURCE should contain the OCID of the
	// vault secret to retrieve
	// default are to store the auth data in the config
	// if not set or not a recognized option then AUTH_SOURCE_CONFIG is used
	// the auth info (either directly in the config or retrieved form the vault
	// must be in the form username:password
	// if the auth info is plain text then set the variable in
	// CONFIG_AUTH_SOURCE_FORMAT to the value of AUTH_SOURCE_FORMAT_PLAINTEXT
	// if the auth info is base64 then set the variable in CONFIG_AUTH_SOURCE_FORMAT
	// to the value of AUTH_SOURCE_FORMAT_BASE64
	// default is to store the auth data as plain text
	// CONFIG_RESULT_CACHE_SECONDS is how long a successful response should be
	// cached by the API GW
	// it must be 60 or higher and 3599 or lower, if outside these bounds it will be
	// warped to them
	// if not provided then the cache duration will not be set and the API GW will
	// use it's default.
	// As the basic functions framework may on occasion stop a function if it's not
	// been called for a
	// while resulting in a cold start that may take longer than expected (this can
	// be overridden
	// using provisioned concurrency) it's important to configure an appropriate
	// cache time that ensures
	// that the function remains active but also balances unnecessary calls to the
	// function
	public final static String CONFIG_AUTH_SOURCE = "auth-source";
	public final static String AUTH_SOURCE_CONFIG = "config";
	public final static String AUTH_SOURCE_VAULT = "vault";
	public final static String CONFIG_AUTH_SECRET = "auth-secret";
	public final static String CONFIG_AUTH_SOURCE_FORMAT = "auth-source-format";
	public final static String AUTH_SOURCE_FORMAT_PLAINTEXT = "plaintext";
	public final static String AUTH_SOURCE_FORMAT_BASE64 = "base64";
	public final static String INCOMMING_AUTH_HEADER = "authorization";
	public final static String BASIC_AUTH_TYPE = "Basic";
	public final static String CONFIG_RESULT_CACHE_SECONDS = "result-cache-seconds";

	public final static String DEFAULT_CONFIG_AUTH_SOURCE = AUTH_SOURCE_CONFIG;
	public final static String DEFAULT_CONFIG_AUTH_SOURCE_FORMAT = AUTH_SOURCE_FORMAT_PLAINTEXT;

	public final static int RESULT_CACHE_SECONDS_MIN = 60;
	public final static int RESULT_CACHE_SECONDS_MAX = 3600;

	public final static String OCID_VAULT_SECRET_PREFIX = "ocid1.vaultsecret";
	// these are used to track config errors.
	private boolean errorFound = false;
	private String error = "";
	private boolean testingMode = false;
	private String AUTH_SOURCE;
	private String AUTH_SECRET;
	private String AUTH_SOURCE_FORMAT;
	private Integer RESULT_CACHE_SECONDS;

	@FnConfiguration
	public void config(final RuntimeContext ctx) {
		log.info("Loading settings from function config");
		testingMode = Boolean
				.parseBoolean(ctx.getConfigurationByKey(CONFIG_TESTING_MODE).orElse(DEFAULT_CONFIG_TESTING_MODE));
		AUTH_SOURCE = ctx.getConfigurationByKey(CONFIG_AUTH_SOURCE).orElse(DEFAULT_CONFIG_AUTH_SOURCE);
		AUTH_SECRET = ctx.getConfigurationByKey(CONFIG_AUTH_SECRET).orElse(null);
		AUTH_SOURCE_FORMAT = ctx.getConfigurationByKey(CONFIG_AUTH_SOURCE_FORMAT)
				.orElse(DEFAULT_CONFIG_AUTH_SOURCE_FORMAT);

		String pendingResultCacheSeconds = ctx.getConfigurationByKey(CONFIG_RESULT_CACHE_SECONDS).orElse(null);

		log.info("Config start Auth secret source is " + AUTH_SOURCE);
		if (testingMode) {
			log.info("Config start Auth secret is " + AUTH_SECRET);
		}
		log.info("Config start Auth source format is " + AUTH_SOURCE_FORMAT);
		log.info("Config result cache seconds is " + pendingResultCacheSeconds);
		// only need to check for the vault as if it's config it's already been
		// retrieved
		if (AUTH_SOURCE.equalsIgnoreCase(AUTH_SOURCE_VAULT)) {
			if (AUTH_SECRET == null) {
				if (errorFound) {
					error += "\n";
				}
				String msg = "Config property AUTH_SECRET is not set, so no vault secret OCID is available";
				error += msg;
				errorFound = true;
				log.info(msg);
			} else {
				log.info("Loading auth from vault with potential OCID " + AUTH_SECRET);
				// the value must start with ocid to be a secret
				if (AUTH_SECRET.toLowerCase().startsWith(OCID_VAULT_SECRET_PREFIX.toLowerCase())) {
					// we have something, try and get it
					log.info("Attempting to create ResourcePrincipalAuthenticationDetailsProvider");
					var authProvider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();
					log.info("Attempting to create SecretsClient");
					var secretsClient = SecretsClient.builder().build(authProvider);
					log.info("Attempting to load auth secret from ocid '" + AUTH_SECRET + "'");
					var secretResp = secretsClient
							.getSecretBundle(GetSecretBundleRequest.builder().secretId(AUTH_SECRET).build());
					if (secretResp.get__httpStatusCode__() != 200) {
						if (errorFound) {
							error += "\n";
						}
						error += "Problem requesting vault secret - returned response is "
								+ secretResp.get__httpStatusCode__();
						errorFound = true;
					}
					SecretBundle secretBundle = secretResp.getSecretBundle();
					Base64SecretBundleContentDetails bundleContent;
					try {
						bundleContent = (Base64SecretBundleContentDetails) secretBundle.getSecretBundleContent();
						try {
							AUTH_SECRET = new String(Base64.getDecoder().decode(bundleContent.getContent()));
						} catch (IllegalArgumentException e) {
							error += "Cannot base64 decode secret value from vault data because "
									+ e.getLocalizedMessage();
							errorFound = true;
						}
					} catch (ClassCastException e) {
						if (errorFound) {
							error += "\n";
						}
						error += "Can't convert the secret content details to a Base64 version. origional type was "
								+ secretBundle.getSecretBundleContent().getClass().getCanonicalName();
						errorFound = true;
					}
				} else {
					String msg = "The AUTH_SOURCE is vault, but the AUTH_SECRET does not start with "
							+ OCID_VAULT_SECRET_PREFIX;
					error += msg;
					errorFound = true;
					log.info(msg);
				}
			}
		} else {
			log.info("Using secret from the config");
			if (AUTH_SECRET == null) {
				if (errorFound) {
					error += "\n";
				}
				String msg = "No secret - is " + CONFIG_AUTH_SECRET + " set in the configuration ?";
				error += msg;
				errorFound = true;
				log.info(msg);
			}
		}
		// is the auth secret is base 64 encoded ? (if it was in the vault then it might
		// have been double encoded !)
		log.info("AUTH_SOURCE_FORMAT is " + AUTH_SOURCE_FORMAT);
		if ((AUTH_SOURCE_FORMAT != null) && (AUTH_SOURCE_FORMAT.equalsIgnoreCase(AUTH_SOURCE_FORMAT_BASE64))) {
			log.info("AUTH_SOURCE_FORMAT is " + AUTH_SOURCE_FORMAT_BASE64 + " so it's being decoded from base 64");
			try {
				AUTH_SECRET = new String(Base64.getDecoder().decode(AUTH_SECRET));
			} catch (IllegalArgumentException e) {
				String msg = "Cannot base64 decode secret value because " + e.getLocalizedMessage();
				if (testingMode) {
					msg += " input value " + AUTH_SECRET;
				}
				error += msg;
				errorFound = true;
				log.info(msg);
			}
		} else {
			log.info(
					"Auth secret format is not " + AUTH_SOURCE_FORMAT_BASE64 + " so it is being treated as plain text");
		}
		RESULT_CACHE_SECONDS = null;
		if (pendingResultCacheSeconds != null) {
			try {
				RESULT_CACHE_SECONDS = Integer.parseInt(pendingResultCacheSeconds);
				if (RESULT_CACHE_SECONDS < RESULT_CACHE_SECONDS_MIN) {
					log.warn("results cache seconds " + RESULT_CACHE_SECONDS + " is to low, reset to the minimum of "
							+ RESULT_CACHE_SECONDS_MIN);
					RESULT_CACHE_SECONDS = RESULT_CACHE_SECONDS_MIN;
				}
				if (RESULT_CACHE_SECONDS > RESULT_CACHE_SECONDS_MAX) {
					log.warn("results cache seconds " + RESULT_CACHE_SECONDS + " is to high, reset to the minimum of "
							+ RESULT_CACHE_SECONDS_MAX);
					RESULT_CACHE_SECONDS = RESULT_CACHE_SECONDS_MAX;
				}
			} catch (NumberFormatException e) {
				String msg = "Unable to parse provided results cache seconds (" + pendingResultCacheSeconds
						+ ") to a number";
				error += msg;
				errorFound = true;
				log.info(msg);
			}
		}
		if (errorFound) {
			log.error(error);
		}
		if (RESULT_CACHE_SECONDS == null) {
			log.info("No results cache seconds set, API GW will apply it's default");
		} else {
			log.info("Results cache seconds set to " + RESULT_CACHE_SECONDS);
		}
		if (testingMode) {
			log.info("Final auth secret is '" + AUTH_SECRET + "'");
		}
		log.info("Completed config");
	}

	/**
	 * IMPORTANT, The API Gateway must transfer the authorization across using names
	 * that matches the names in the function configuration. It can of course modify
	 * those names if the original ones do not match In the event that the body of
	 * the request is to be processed then it must be present in the request with a
	 * field named (all upper case)
	 * 
	 * @param request
	 * @return
	 */
	public AuthResponse handleAPIGWAuthenticationRequest(AuthRequest request) {
		if (testingMode) {
			log.info("Recieved auth request is " + request);
		}
		AuthResponse response = new AuthResponse();
		// if there is an error in the configuration do not proceed and default to
		// denying access.
		if (errorFound) {
			log.error(error);
			response.setActive(false);
			response.getContext().put("responseMessage", error);
			return response;
		}
		String auth = request.getData().get(INCOMMING_AUTH_HEADER);
		AuthResponse resp = processRequest(response, auth);
		log.info("Proccessed auth request, returning resp " + resp.toString());
		return resp;
	}

	public String handleHttpRequest(HTTPGatewayContext hctx, String body) {
		AuthResponse response = new AuthResponse();
		// if there is an error in the configuration do not proceed
		if (errorFound) {
			log.error(error);
			response.setActive(false);
			response.getContext().put("responseMessage", error);
			return error;
		}
		String auth = hctx.getHeaders().get(INCOMMING_AUTH_HEADER).orElse(null);
		String resp = processRequest(response, auth).toString();
		log.info("Returning response " + resp);
		return resp;
	}

	public AuthResponse processRequest(AuthResponse response, String authHeader) {
		if (authHeader == null) {
			response.setActive(false);
			response.getContext().put("responseMessage", "No " + INCOMMING_AUTH_HEADER + " present");
			return response;
		}
		// does the header start with Basic
		if (!authHeader.startsWith(BASIC_AUTH_TYPE)) {
			response.setActive(false);
			response.getContext().put("responseMessage",
					"Header" + INCOMMING_AUTH_HEADER + " is not authentication type " + BASIC_AUTH_TYPE);
			return response;
		}
		// get the actual auth content
		String authProvidedBase64 = authHeader.substring(BASIC_AUTH_TYPE.length()).trim();
		if (testingMode) {
			log.info("Extracted header is " + authProvidedBase64);
		}
		// decode it
		String authProvided;
		try {
			authProvided = new String(Base64.getDecoder().decode(authProvidedBase64)).trim();
		} catch (IllegalArgumentException e) {
			String msgString = "Problem decoding incomming auth data " + e.getLocalizedMessage();
			log.error(msgString);
			response.setActive(false);
			response.getContext().put("responseMessage", msgString);
			return response;
		}
		if (testingMode) {
			log.info("Validating auth '" + authProvided + "' against specified secret of '" + AUTH_SECRET + "'");
		}
		boolean codeOk = authProvided.equals(AUTH_SECRET);
		response.setActive(codeOk);
		if (codeOk) {
			if (RESULT_CACHE_SECONDS != null) {
				response.setExpiresAt(
						DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now().plusSeconds(RESULT_CACHE_SECONDS)));
			} else {
				response.setExpiresAt("");
			}
			response.getContext().put("responseMessage", "Authenticated OK");
		} else {
			response.getContext().put("responseMessage",
					"Authentication failed due to invalid username and / or password");
		}
		return response;
	}
}