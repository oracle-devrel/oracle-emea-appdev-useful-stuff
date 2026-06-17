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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.MessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;

@Log
public abstract class DeviceModelMessageFilterCore implements MessageHandler {

	@Inject
	private DeviceModelInstancesCache deviceModelInstancesCache;
	private final int order;
	private final Set<String> modelNames;
	private final Set<String> matchingInstances = new HashSet<>();
	private final Set<String> nonMatchingInstances = new HashSet<>();
	private final boolean caseInsensitive;
	private final boolean nullModelIdIsError;
	private final String childName;

	public DeviceModelMessageFilterCore(int order, @NotNull @NotBlank List<String> modelNames, boolean caseInsensitive,
			boolean nullModelIdIsError, String childName) {
		this.order = order;
		this.modelNames = caseInsensitive
				? modelNames.stream().map(name -> name.toLowerCase()).collect(Collectors.toSet())
				: new HashSet<>(modelNames);
		this.nullModelIdIsError = nullModelIdIsError;
		this.caseInsensitive = caseInsensitive;
		this.childName = childName;
	}

	@Override
	public void configure() throws Exception {
		log.info("Configuring");
		deviceModelInstancesCache.configure();
		log.info("Configured");

	}

	@Override
	public void unconfigure() throws Exception {
		deviceModelInstancesCache.unconfigure();
		log.info("Clearing old cached results");
		// just in case we are called multiple times reset the sets
		matchingInstances.clear();
		nonMatchingInstances.clear();
	}

	public boolean doesIoTDataCoreMatchModel(final String instanceId) throws Exception {
		// final String instanceId = input.getDigitalTwinInstanceId();
		// have we checked and determined it's not a match before ?
		if (nonMatchingInstances.contains(instanceId)) {
			log.fine(() -> "instance is already in the non matching set, " + instanceId);
			return false;
		}
		if (matchingInstances.contains(instanceId)) {
			log.fine(() -> "instance is already in the matching set, " + instanceId);
			return true;
		}
		log.fine(() -> "instance is unknown retrieving its model, " + instanceId);
		// we don't know about it, using the device ID query the DB to get the model id,
		// note that we will assume for now that if we haven't found it previously we
		// haven't found it this time
		String instanceModelNameRetrieved = deviceModelInstancesCache.getModelNameByInstanceId(instanceId, true);
		// if we're being case instensitive map thew name we got to lower case
		String instanceModelName;
		if (instanceModelNameRetrieved != null) {
			instanceModelName = caseInsensitive ? instanceModelNameRetrieved.toLowerCase() : instanceModelNameRetrieved;
		} else {
			instanceModelName = null;
		}
		// can't use a lambda for the debugging unless we do this as instanceModelId
		// must be final
		log.fine("instance has model name of, " + instanceModelName);
		if (instanceModelName == null) {
			// no model id found, this I guess is possible for an instance that is not
			// connected to a model, but we are dealing with normalized data here, which
			// should always have a model, add to the non matching for future use
			nonMatchingInstances.add(instanceId);
			if (nullModelIdIsError) {
				log.severe(() -> "Error, was handed instance id that does not have a model id, " + instanceId);
			} else {
				log.info(() -> "Handed instance id that does not have a model id, " + instanceId);

			}
			return false;
		} else if (modelNames.contains(instanceModelName)) {
			// it matches, stash the result for later and carry on with it
			log.fine(() -> "previously unknown instance " + instanceId + "has model name  " + instanceModelName
					+ " that matches a known model name ");
			matchingInstances.add(instanceId);
			return true;
		} else {
			// no match, remember that
			log.fine(() -> "previously unknown instance " + instanceId + "has model name " + instanceModelName
					+ " that does not match a known model name ");
			nonMatchingInstances.add(instanceId);
			return false;
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Device Model filter for " + childName;
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " currently has " + matchingInstances.size()
				+ " matches with model names " + modelNames + " and " + nonMatchingInstances.size()
				+ " known non matches";
	}

	@Override
	public String toString() {
		return getName() + "(" + getConfig() + ")";
	}
}
