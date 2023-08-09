/*Copyright (c) 2023 Oracle and/or its affiliates.

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
package com.oracle.timg.hmac;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.HmacUtils;

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
public class VerifyHmacFunction {
	private static final String DEFAULT_HMAC_ALGORITHM = "HmacMD5";
	public static final Integer HTTP_RESPONSE_OK = 200;
	public static final Integer HTTP_RESPONSE_UNAUTHORIZED = 401;

	public final static String CONFIG_HMAC_SALT = "salt";
	public final static String CONFIG_HMAC_ALGORITHM = "hmac-algorithm";
	// if set must be set to the value if one of the constants HMAC_SOURCE_CONFIG or
	// HMAC_SOURCE_VAULT (case ignored)
	// if set to HMAC_SOURCE_CONFIG the text in hmac-secret is used
	// if set to HMAC_SOURCE_VAULT then hmac-secret should contain the OCID of the
	// vault secret to retrieve
	// if not set or not a recognized option then HMAC_SOURCE_CONFIG is used
	public final static String CONFIG_HMAC_SECRET_SOURCE = "hmac-secret-source";
	public final static String HMAC_SOURCE_CONFIG = "config";
	public final static String HMAC_SOURCE_VAULT = "vault";
	public final static String CONFIG_HMAC_SECRET = "hmac-secret";
	public final static String CONFIG_INCOMMING_HMAC_HEADER = "incomming-hmac-header";
	public final static String CONFIG_FIELDS_TO_CALCULATE_HMAC_WITH = "calculate-hmac-using";
	public final static String CONFIG_HMAC_CALCULATION_FIELD_SEPARATOR = "separate-input-fields-using";
	public final static String FIELDS_TO_CALCULATE_HMAC_WITH_SEPARATOR = ",";
	public final static String CALCULATE_HMAC_BODY_NAME = "BODY";
	public final static String CALCULATE_HMAC_SALT_NAME = "SALT";
	public static String HMAC_ALGORITHM = null;
	public static String HMAC_SECRET_SOURCE = null;
	public static String HMAC_SECRET = null;
	public static String HMAC_SALT = null;
	public static String HMAC_HMAC_CALCULATION_FIELD_SEPARATOR = "";
	public static String INCOMMING_HMAC_HEADER = null;
	public static List<String> FIELDS_TO_CALCULATE_HMAC_WITH;
	// these are used to track config errors.
	private boolean errorFound = false;
	private String error = "";

	@FnConfiguration
	public void config(final RuntimeContext ctx) {
		log.info("Loading settings from function config");
		HMAC_ALGORITHM = ctx.getConfigurationByKey(CONFIG_HMAC_ALGORITHM).orElse(DEFAULT_HMAC_ALGORITHM);
		HMAC_SECRET_SOURCE = ctx.getConfigurationByKey(CONFIG_HMAC_SECRET_SOURCE).orElse(HMAC_SOURCE_CONFIG);
		HMAC_SECRET = ctx.getConfigurationByKey(CONFIG_HMAC_SECRET).orElse(null);
		INCOMMING_HMAC_HEADER = ctx.getConfigurationByKey(CONFIG_INCOMMING_HMAC_HEADER).orElse(null);
		HMAC_SALT = ctx.getConfigurationByKey(CONFIG_HMAC_SALT).orElse(null);
		HMAC_HMAC_CALCULATION_FIELD_SEPARATOR = ctx.getConfigurationByKey(CONFIG_HMAC_CALCULATION_FIELD_SEPARATOR)
				.orElse("");
		// Note that the CONFIG_FIELDS_TO_CALCULATE_HMAC_WITH is case insensitive FOR
		// HTTP REQUESTS ONLY
		// as all real headers are mapped to lower case for http requests
		// FOR auth type requests the input fields are case sensitive in the request
		FIELDS_TO_CALCULATE_HMAC_WITH = new ArrayList<>();
		String inputFields = ctx.getConfigurationByKey(CONFIG_FIELDS_TO_CALCULATE_HMAC_WITH)
				.orElse(CALCULATE_HMAC_BODY_NAME);
		Collections.addAll(FIELDS_TO_CALCULATE_HMAC_WITH, inputFields.split(FIELDS_TO_CALCULATE_HMAC_WITH_SEPARATOR));
		log.info("HMAC secret source is " + HMAC_SECRET_SOURCE);
		if (HMAC_SECRET_SOURCE.equalsIgnoreCase(HMAC_SOURCE_VAULT)) {
			log.info("Loading secret from vault");
			if (HMAC_SECRET == null) {
				if (errorFound) {
					error += "\n";
				}
				error += "Config property HMAC_SECRET is not set, so no vault secret OCID is available";
				errorFound = true;
			} else {
				// we have something, try and get it
				var authProvider = ResourcePrincipalAuthenticationDetailsProvider.builder().build();
				var secretsClient = SecretsClient.builder().build(authProvider);
				var secretResp = secretsClient
						.getSecretBundle(GetSecretBundleRequest.builder().secretId(HMAC_SECRET).build());
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
					HMAC_SECRET = new String(Base64.getDecoder().decode(bundleContent.getContent()));
				} catch (ClassCastException e) {
					if (errorFound) {
						error += "\n";
					}
					error += "Can't convert the secret content details to a Base64 version. origional type was "
							+ secretBundle.getSecretBundleContent().getClass().getCanonicalName();
					errorFound = true;
				}
			}
		} else {
			log.info("Using secret from the config");
			if (HMAC_SECRET == null) {
				if (errorFound) {
					error += "\n";
				}
				error += "No secret - is " + CONFIG_HMAC_SECRET + " set in the configuration ?";
				errorFound = true;
			}
		}

		if (INCOMMING_HMAC_HEADER == null) {
			if (errorFound) {
				error += "\n";
			}
			error += "Config property INCOMMING_HMAC_HEADER is not set, don't know where to get the incommign HMAC from";
			errorFound = true;
		}
		if (errorFound) {
			log.error(error);
		}
	}

	/**
	 * IMPORTANT, The API Gateway must transfer the headers across using names that
	 * matches the names in the function configuration. It can of course modify
	 * those names if the original ones do not match In the event that the body of
	 * the request is to be processed then it must be present in the request with a
	 * field named (all upper case)
	 * 
	 * @param request
	 * @return
	 */
	public AuthResponse handleAPIGWAuthenticationRequest(AuthRequest request) {
		log.info("Recieved auth request is " + request);
		Map<String, String> fields = request.getData();
		AuthResponse response = new AuthResponse();
		response.setActive(false);
		// try and stop the response from be in reused without calling the function by
		// setting a very short cache time
		response.setExpiresAt(DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now().plusSeconds(1)));
		// if there is an error in the configuration do not proceed and default to
		// denying access.
		if (errorFound) {
			log.error(error);
			response.getContext().put("responseMessage", error);
			return response;
		}
		// load up the other fields in the map, the body will have been added by the API
		// GW code
		if (HMAC_SALT != null) {
			fields.put(CALCULATE_HMAC_SALT_NAME, HMAC_SALT);
		}
		String inputHmac = fields.get(INCOMMING_HMAC_HEADER);
		if (inputHmac == null) {
			log.info("No input HMAC header found");
			return response;
		}
		String calculatedHmac = processRequest(request.getData());
		boolean hmacok = calculatedHmac.equalsIgnoreCase(inputHmac);
		log.info("Calculated hmac is :" + calculatedHmac + "\nSpecified hmac is :" + inputHmac + "\nThey are "
				+ (hmacok ? "Identical" : "Not Identical"));
		response.setActive(hmacok);
		return response;
	}

	public String handleHttpRequest(HTTPGatewayContext hctx, String body) {
		// if there is an error in the configuration do not proceed
		if (errorFound) {
			log.error(error);
			return error;
		}
		// copy across the headers, we look for the value of the first occurence if
		// there are multiple headers
		Map<String, String> fields = hctx.getHeaders().asMap().entrySet().stream()
				.collect(Collectors.toMap(entry -> mungeHeader(entry.getKey()),
						entry -> entry.getValue().isEmpty() ? null : entry.getValue().get(0)));
		// if set put the salt and body of the request in the headers map to make
		// processing easier.
		// if there had been a header in there named Body or other combination then it
		// will have been replaced with body (lower case)
		// so this won;t overrite it.
		if (body != null) {
			fields.put(CALCULATE_HMAC_BODY_NAME, body);
		}
		if (HMAC_SALT != null) {
			fields.put(CALCULATE_HMAC_SALT_NAME, HMAC_SALT);
		}

		String inputHmac = fields.get(INCOMMING_HMAC_HEADER);
		String calculatedHmac = processRequest(fields);
		if (inputHmac == null) {
			return "No input HMAC header found\nCalculatedHmac is " + calculatedHmac;
		}
		String resp = "Calculated hmac is :" + calculatedHmac + "\nSpecified hmac is :" + inputHmac + "\nThey are "
				+ (calculatedHmac.equalsIgnoreCase(inputHmac) ? "Identical" : "Not Identical");
		log.info(resp);
		return resp;
	}

	public final static String mungeHeader(String header) {
		// replace _ with - and map to lower case
		return header.replace('_', '-').toLowerCase();
	}

	public String processRequest(Map<String, String> fields) {
		boolean addedFirstField = false;
		String dataToCheck = "";
		for (String field : FIELDS_TO_CALCULATE_HMAC_WITH) {
			// If we've added the first field then we will consider adding a separator
			// between the fields
			if (addedFirstField) {
				dataToCheck += HMAC_HMAC_CALCULATION_FIELD_SEPARATOR;
			} else {
				addedFirstField = true;
			}
			if (field.equalsIgnoreCase(INCOMMING_HMAC_HEADER)) {
				if (errorFound) {
					error += "\n";
				}
				error += "Cannot include the incomming mac header (" + INCOMMING_HMAC_HEADER
						+ ") in the MAC calculation";
				errorFound = true;
			} else {
				String inputHeader = fields.get(field);
				if (inputHeader == null) {
					if (errorFound) {
						error += "\n";
					}
					error += "Header " + field + " is missing, but it's included in the MAC comparisson";
					errorFound = true;
				} else {
					dataToCheck += inputHeader;
				}
			}
		}
		if (errorFound) {
			return error;
		}
		log.info("Data to check is " + dataToCheck);
		return calculateHMAC(HMAC_ALGORITHM, dataToCheck, HMAC_SECRET);
	}

	private String calculateHMAC(String algorithm, String dataToCheck, String key) {
		return new HmacUtils(algorithm, key).hmacHex(dataToCheck);
	}

}