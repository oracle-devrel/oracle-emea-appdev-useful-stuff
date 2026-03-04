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
package com.oracle.timg.demo.iot.demogateway.caches;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.bmc.iot.model.DigitalTwinInstance;
import com.oracle.bmc.iot.model.DigitalTwinModel;
import com.oracle.timg.demo.iot.demogateway.ociinterations.IotServiceDetails;
import com.oracle.timg.demo.iot.demogateway.ociinterations.MissingOciResourceException;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

/**
 * A very simple class to handle retrieving and caching iot instance data
 * 
 */
@Singleton
@Log

@Requires(property = "gateway.iotservice.digitaltwinmodel.name")
public class IdToInstanceMapping {
	private final IotServiceDetails iotServiceDetails;
	private final String digitalTwinModelName;
	private final DigitalTwinModel digitalTwinModel;
	private final Map<String, DigitalTwinInstance> mappings = new ConcurrentHashMap<>();

	@Inject
	public IdToInstanceMapping(IotServiceDetails iotServiceDetails,
			@Property(name = "gateway.iotservice.digitaltwinmodel.name") String digitalTwinModelName) throws Exception {
		// stash the inputs away
		this.iotServiceDetails = iotServiceDetails;
		this.digitalTwinModelName = digitalTwinModelName;

		this.digitalTwinModel = iotServiceDetails.getIoTProcessor()
				.getDigitalTwinModel(iotServiceDetails.getIotDomain(), digitalTwinModelName);
		if (digitalTwinModel == null) {
			throw new MissingOciResourceException("Cannot locate digital twin model " + digitalTwinModelName
					+ " in iot domain " + iotServiceDetails.getIotDomainName() + " in iot domain group "
					+ iotServiceDetails.getIotDomainGroupName() + " in compartment "
					+ iotServiceDetails.getCompartmentName());
		}

	}

	public DigitalTwinInstance getDigitalTwinInstance(String displayName) {
		log.fine(() -> "Looking for digital twin instance named " + displayName);
		if (mappings.containsKey(displayName)) {
			DigitalTwinInstance dti = mappings.get(displayName);
			log.finer("Located cached digital twin instance named " + displayName);
			return dti;
		}
		// this will return null if not found, for this we need that to trigger a
		// re-search if requested again in case it's been added, so don't add it to the
		// map
		log.finer(() -> "Cant locate cached digital twin instance named " + displayName + " requesting it from oci");
		DigitalTwinInstance dti = iotServiceDetails.getIoTProcessor()
				.getDigitalTwinInstance(iotServiceDetails.getIotDomain(), displayName);
		if (dti != null) {
			log.finer(() -> "Got cached digital twin instance named " + displayName + " from oci");
			mappings.put(displayName, dti);
		} else {
			log.finer(() -> "Can't find digital twin instance named " + displayName + " in oci");
		}
		return dti;
	}

	public String getConfig() {
		String resp = ", digitalTwinModelName=" + digitalTwinModelName + ", digitalTwinModelNameOcid="
				+ digitalTwinModel.getId();
		resp += ", Mappings size=" + mappings.size();
		return resp;
	}

	@PostConstruct
	private void loadExistingDigitalTwinInstances() {
		log.info(() -> "Loading existing digital twin instances from oci");
		// get all existing instances with the model
		List<DigitalTwinInstance> existingInstances = iotServiceDetails.getIoTProcessor()
				.listDigitalTwinInstances(iotServiceDetails.getIotDomain(), digitalTwinModel);
		// to make things easier the display name is being used as the primary key ,
		// also as the external key. This is because we can't search on the external
		// key, in a production setup we'd probably not do this for security reasons,
		// and if we wanted to maintain separate info we'd build a mapping of external
		// key to instance, that might make adding devices outside the gateway more
		// complex though
		existingInstances.stream().forEach((dti) -> {
			mappings.put(dti.getDisplayName(), dti);
			log.info(() -> "Loading existing digital twin instances " + dti.getDisplayName() + " from oci");
		});
	}

	public void save(String sourceId, DigitalTwinInstance dti) {
		mappings.put(sourceId, dti);
		log.info(() -> "Added digital twin instance " + dti.getDisplayName() + " to cache using sourceId " + sourceId);
	}
}
