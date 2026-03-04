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

import com.oracle.bmc.identity.model.Compartment;
import com.oracle.bmc.iot.model.DigitalTwinAdapter;
import com.oracle.bmc.iot.model.DigitalTwinModel;
import com.oracle.bmc.iot.model.IotDomain;
import com.oracle.bmc.iot.model.IotDomainGroup;
import com.oracle.timg.oci.authentication.AuthenticationProcessor;
import com.oracle.timg.oci.identity.IdentityProcessor;
import com.oracle.timg.oci.iot.IotProcessor;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.java.Log;

@Requires(property = "gateway.iotservice.domaingroup.compartment")
@Requires(property = "gateway.iotservice.domaingroup.name")
@Requires(property = "gateway.iotservice.domain.name")
@Requires(property = "gateway.iotservice.digitaltwinmodel.name")
@Requires(property = "gateway.iotservice.digitaltwinadapter.name")
@Log
@Singleton
/**
 * If no specific reformatter is available this will use the passthrough
 * formatter by default
 */
public class IotServiceDetails {
	private final AuthenticationProcessor authProcessor;
	private final IdentityProcessor identityProcessor;
	@Getter
	private final IotProcessor ioTProcessor;

	@Getter
	private final String compartmentName;
	@Getter
	private final String iotDomainGroupName;
	@Getter
	private final String iotDomainName;
	@Getter
	private final String digitalTwinModelName;
	@Getter
	private final String digitalTwinAdapterName;
	@Getter
	private final Compartment compartment;
	@Getter
	private final IotDomainGroup iotDomainGroup;
	@Getter
	private final IotDomain iotDomain;
	@Getter
	private final DigitalTwinModel digitalTwinModel;
	@Getter
	private final DigitalTwinAdapter digitalTwinAdapter;

	@Inject
	public IotServiceDetails(AuthenticationProcessorDetails authProcessorData, IdentityProcessorDetails identityProcessorDetails,
			@Property(name = "gateway.iotservice.domaingroup.compartment") String compartmentName,
			@Property(name = "gateway.iotservice.domaingroup.name") String iotDomainGroupName,
			@Property(name = "gateway.iotservice.domain.name") String iotDomainName,
			@Property(name = "gateway.iotservice.digitaltwinmodel.name") String digitalTwinModelName,
			@Property(name = "gateway.iotservice.digitaltwinadapter.name") String digitalTwinAdapterName)
			throws Exception {
		this.compartmentName = compartmentName;
		this.iotDomainGroupName = iotDomainGroupName;
		this.iotDomainName = iotDomainName;
		this.digitalTwinModelName = digitalTwinModelName;
		this.digitalTwinAdapterName = digitalTwinAdapterName;

		// setup the processors to capture the data
		this.authProcessor = authProcessorData.getAuthenticationProcessor();
		this.identityProcessor = identityProcessorDetails.getIdentityProcessor();
		this.ioTProcessor = new IotProcessor(authProcessor);

		this.compartment = identityProcessor.locateCompartmentByPath(compartmentName);
		if (compartment == null) {
			throw new MissingOciResourceException("Cannot locate compartment " + compartmentName);
		}
		log.info("Located compartment " + compartmentName + ", ocid=" + compartment.getId());

		this.iotDomainGroup = ioTProcessor.getIotDomainGroup(compartment.getId(), iotDomainGroupName);
		if (iotDomainGroup == null) {
			throw new MissingOciResourceException(
					"Cannot locate iot domain group " + iotDomainGroupName + " in compartment " + compartmentName);
		}
		log.info("Located IOTDomainGroup " + iotDomainGroupName + " in compartment " + compartmentName + ", ocid="
				+ iotDomainGroup.getId());

		this.iotDomain = ioTProcessor.getIotDomainInDomainGroup(iotDomainGroup, iotDomainName);
		if (iotDomain == null) {
			throw new MissingOciResourceException("Cannot locate iot domain " + iotDomainName + " in iot domain group "
					+ iotDomainGroupName + " in compartment " + compartmentName);
		}
		log.info("Locateed iot domain " + iotDomainName + " in iot domain group " + iotDomainGroupName
				+ " in compartment " + compartmentName + ", ocid=" + iotDomain.getId());

		this.digitalTwinModel = ioTProcessor.getDigitalTwinModel(iotDomain, digitalTwinModelName);
		if (digitalTwinModel == null) {
			throw new MissingOciResourceException(
					"Cannot locate digitalTwinModel " + digitalTwinModelName + " in iot domain " + iotDomainName
							+ " in iot domain group " + iotDomainGroupName + " in compartment " + compartmentName);
		}
		log.info("Located digitalTwinModel " + digitalTwinModelName + " in iot domain " + iotDomainName
				+ " in iot domain group " + iotDomainGroupName + " in compartment " + compartmentName + ", ocid="
				+ digitalTwinModel.getId());

		this.digitalTwinAdapter = ioTProcessor.getDigitalTwinAdapter(iotDomain, digitalTwinAdapterName);
		if (digitalTwinAdapter == null) {
			throw new MissingOciResourceException(
					"Cannot locate digitalTwinAdapter " + digitalTwinAdapterName + " in iot domain " + iotDomainName
							+ " in iot domain group " + iotDomainGroupName + " in compartment " + compartmentName);
		}
		log.info("Located digitalTwinAdapter " + digitalTwinAdapterName + " in iot domain " + iotDomainName
				+ " in iot domain group " + iotDomainGroupName + " in compartment " + compartmentName + ", ocid="
				+ digitalTwinAdapter.getId());

	}

	public String getConfig() {
		String resp = "compartmentName=" + compartmentName + ", compartmentOcid=" + compartment.getId();
		resp += ", iotDomainGroupName=" + iotDomainGroupName + ", iotDomainGroupNameOcid=" + iotDomainGroup.getId();
		resp += ", iotDomainName=" + iotDomainName + ", iotDomainNameOcid=" + iotDomain.getId();
		resp += ", igitalTwinModelName=" + digitalTwinModelName + ", digitalTwinModelOcid=" + digitalTwinModel.getId();
		resp += ", digitalTwinAdapterName=" + digitalTwinAdapterName + ", digitalTwinAdapterOcid="
				+ digitalTwinAdapter.getId();

		return resp;
	}

	@PostConstruct
	private void loadExistingDigitalTwinInstances() {
		log.info("iotservice config data " + getConfig());
	}
}
