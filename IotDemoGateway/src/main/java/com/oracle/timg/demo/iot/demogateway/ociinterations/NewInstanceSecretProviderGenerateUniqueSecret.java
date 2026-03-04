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
package com.oracle.timg.demo.iot.demogateway.ociinterations;

import java.io.IOException;

import com.oracle.bmc.keymanagement.model.Key;
import com.oracle.bmc.vault.model.Secret;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "gateway.instance.secret.newinstancesecretmode", value = "GENERATE_UNIQUE_SECRET", defaultValue = "unknown")
@Requires(property = "gateway.instance.secret.generate.key")
@Log
public class NewInstanceSecretProviderGenerateUniqueSecret implements NewInstanceSecretProvider {
	private final VaultServiceDetails vaultServiceDetails;
	private final String keyName;
	private final String secretPrefix;
	private final int secretLength;
	private final String compartmentOcid;
	private final String vaultOcid;
	private final String keyOcid;
	private final int secretCheckTime;

	@Inject
	public NewInstanceSecretProviderGenerateUniqueSecret(VaultServiceDetails vaultServiceDetails,
			@Property(name = "gateway.instance.secret.generate.key") String keyName,
			@Property(name = "gateway.instance.secret.generate.secretprefix", defaultValue = "iot-gateway-generated-secret") String secretPrefix,
			@Property(name = "gateway.instance.secret.generate.secretlength", defaultValue = "16") int secretLength,
			@Property(name = "gateway.instance.secret.generate.secretchecktime", defaultValue = "300") int secretCheckTime)
			throws MissingOciResourceException, IllegalArgumentException, IOException {
		// stash the inputs away
		this.vaultServiceDetails = vaultServiceDetails;
		this.keyName = keyName;
		this.secretPrefix = secretPrefix;
		this.secretLength = secretLength;
		this.secretCheckTime = secretCheckTime;
		// get the data
		Key key = vaultServiceDetails.getVaultProcessor().getKeyByName(vaultServiceDetails.getVault(), keyName);
		if (key == null) {
			throw new MissingOciResourceException("Can't locate key " + keyName);
		}
		this.keyOcid = key.getId();
		this.vaultOcid = vaultServiceDetails.getVault().getId();
		this.compartmentOcid = vaultServiceDetails.getCompartment().getId();
	}

	@Override
	public String getConfig() {
		String resp = ", KeyName=" + keyName + ", keyOcid=" + keyOcid;
		resp += "secretPrefix = " + secretPrefix + ", Secret length=" + secretLength;
		return resp;
	}

	@Override
	public String getVaultSecretOcidForNewInstance(String instanceIdentifier) throws MissingOciResourceException {
		log.fine("Creating new vault secret for " + instanceIdentifier);
		String secretDisplayName = secretPrefix + "-" + instanceIdentifier;
		Secret vaultSecret = vaultServiceDetails.getVaultProcessor().createSecretGeneratedPassphrase(compartmentOcid,
				secretDisplayName, keyOcid, vaultOcid, secretLength,
				"Encryption key for iot instance " + instanceIdentifier, null);
		Boolean secretCreation = vaultServiceDetails.getVaultProcessor().waitForSecretToBecomeActive(vaultSecret, 10,
				secretCheckTime);
		if (secretCreation == null) {
			throw new MissingOciResourceException(
					"Returned OCID is null, call was probabaly interruped, can't continue");
		}
		if (secretCreation) {
			log.fine("Created secret for instance " + instanceIdentifier + " with ocid " + vaultSecret.getId());
			return vaultSecret.getId();
		}
		throw new MissingOciResourceException("Waited " + secretCheckTime + " seconds for secret " + secretDisplayName
				+ " to be created but no luck");

	}

	@PostConstruct
	public void postConstruct() {
		log.info("Generate unique handler config: " + getConfig());
	}
}
