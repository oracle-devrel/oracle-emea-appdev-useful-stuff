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

import com.oracle.bmc.vault.model.Secret;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "gateway.instance.secret.newinstancesecretmode", value = "REUSE_SECRET", defaultValue = "unknown")
@Requires(property = "gateway.instance.secret.reuse.secret")
@Log
public class NewInstanceSecretProviderReuseSecret implements NewInstanceSecretProvider {
	@SuppressWarnings("unused")
	private final VaultServiceDetails vaultServiceDetails;
	private final String secretName;
	private final Secret secret;
	private final String secretOcid;
	private final String secretContents;

	@Inject
	public NewInstanceSecretProviderReuseSecret(VaultServiceDetails vaultServiceDetails,
			@Property(name = "gateway.instance.secret.reuse.secret") String secretName)
			throws MissingOciResourceException, IllegalArgumentException, IOException {
		// stash the inputs away incase we decide to do something with them later on
		this.vaultServiceDetails = vaultServiceDetails;
		this.secretName = secretName;
		// get the data
		this.secret = vaultServiceDetails.getVaultProcessor().getSecretByName(vaultServiceDetails.getVault(),
				secretName);
		if (this.secret == null) {
			throw new MissingOciResourceException("Can't locate secret " + secretName);
		}
		this.secretOcid = secret.getId();
		secretContents = vaultServiceDetails.getVaultProcessor().getSecretContents(secretOcid);
	}

	@Override
	public String getConfig() {
		String resp = "SecretName=" + secretName + ", secretOcid=" + secret.getId() + ", Secret contents="
				+ secretContents;
		return resp;
	}

	@Override
	public String getVaultSecretOcidForNewInstance(String instanceIdentifier) throws MissingOciResourceException {
		log.fine("Reusing the existing vault secret for  instance " + instanceIdentifier);
		return secretOcid;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Reuse secret handler config: " + getConfig());
	}
}
