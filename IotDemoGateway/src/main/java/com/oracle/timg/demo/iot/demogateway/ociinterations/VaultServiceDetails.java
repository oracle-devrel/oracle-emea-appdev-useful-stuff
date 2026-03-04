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

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.keymanagement.model.Vault;
import com.oracle.timg.oci.authentication.AuthenticationProcessor;
import com.oracle.timg.oci.identity.IdentityProcessor;
import com.oracle.timg.oci.vault.VaultProcessor;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;

@Log
@Singleton
@Requires(property = "gateway.instance.secret.vault.compartment")
@Requires(property = "gateway.instance.secret.vault.name")
public class VaultServiceDetails {
	private final AuthenticationProcessor authProcessor;
	private final IdentityProcessor identityProcessor;
	@Getter
	private final VaultProcessor vaultProcessor;
	@Getter
	private final String compartmentName;
	@Getter
	private final String vaultName;
	@Getter
	private final Compartment compartment;
	@Getter
	private final Vault vault;

	@Inject
	public VaultServiceDetails(AuthenticationProcessorDetails authProcessorData,
			IdentityProcessorDetails identityProcessorDetails,
			@Property(name = "gateway.instance.secret.vault.compartment") String compartmentName,
			@Property(name = "gateway.instance.secret.vault.name") String vaultName)
			throws MissingOciResourceException, IllegalArgumentException, IOException {
		this.authProcessor = authProcessorData.getAuthenticationProcessor();
		this.identityProcessor = identityProcessorDetails.getIdentityProcessor();
		this.compartmentName = compartmentName;
		this.vaultName = vaultName;
		this.vaultProcessor = new VaultProcessor(authProcessor);
		// get the data
		try {
			this.compartment = identityProcessor.locateCompartmentByPath(compartmentName);
		} catch (Exception e) {
			throw new MissingOciResourceException(
					"Problem getting compartment " + compartmentName + " " + e.getLocalizedMessage(), e);
		}
		if (this.compartment == null) {
			throw new MissingOciResourceException("Can't locate compartment " + compartmentName);
		}
		log.info("Located compartment " + compartmentName + ", ocid=" + compartment.getId());

		this.vault = vaultProcessor.getVaultByName(compartment, vaultName);
		if (this.compartment == null) {
			throw new MissingOciResourceException("Can't locate vault " + vaultName);
		}
		log.info("Located vault " + vaultName + " in compartment " + compartmentName + ", ocid=" + vault.getId());
	}

	public String getConfig() {
		String resp = "CompartmentName=" + compartmentName + ", compartmentOcid=" + compartment.getId();
		resp += ", VaultName=" + vaultName + ", vaultOcid=" + vault.getId();
		return resp;
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Vault Service Details " + getConfig());
	}
}
