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
package com.oracle.timg.demo.iot.demogateway.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.oracle.bmc.iot.model.DigitalTwinInstance;
import com.oracle.timg.demo.iot.demogateway.caches.AuthIdToAuthValueCache;
import com.oracle.timg.demo.iot.demogateway.caches.IdToInstanceMapping;
import com.oracle.timg.demo.iot.demogateway.eventdatatransformer.EventDataIncommingFormatException;
import com.oracle.timg.demo.iot.demogateway.eventdatatransformer.EventDataTransformException;
import com.oracle.timg.demo.iot.demogateway.eventdatatransformer.EventDataTransformService;
import com.oracle.timg.demo.iot.demogateway.instancekeytransformer.InstanceKeyIncommingFormatException;
import com.oracle.timg.demo.iot.demogateway.instancekeytransformer.InstanceKeyTransformException;
import com.oracle.timg.demo.iot.demogateway.instancekeytransformer.InstanceKeyTransformService;
import com.oracle.timg.demo.iot.demogateway.iotupload.IotServiceClient;
import com.oracle.timg.demo.iot.demogateway.ociinterations.IotServiceDetails;
import com.oracle.timg.demo.iot.demogateway.ociinterations.MissingOciResourceException;
import com.oracle.timg.demo.iot.demogateway.ociinterations.NewInstanceSecretProvider;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

/*
 * Using the @Context annotation is general a bad thing as it will force the instantiation at start up, not using lazy instantiation.
 * 
 * We care about creating this class because it's not directly referenced by the controller or classes that are injected / created in 
 * it's tree as this is decoupled and only communicates by a queue. If this class wasn't instantiated then there would be nothing to 
 * pull events off the queue. That would be a bad thing !
 * 
 * Of course we could tell micronaut to instantiate all the @Singleton beans it controls which would have the same effect, but this
 * way we know for certain it's going to be started
 */
@Context
@Log
@Singleton
public class EventQueueProcessor implements Runnable {
	private EventQueue eventQueue;
	private final InstanceKeyTransformService instanceKeyTransformerService;
	private final EventDataTransformService eventDataTransformerService;
	private final ExecutorService executors;
	private final IdToInstanceMapping idToInstanceMapping;
	private final NewInstanceSecretProvider newInstanceSecretProvider;
	private final IotServiceDetails iotServiceDetails;
	private final AuthIdToAuthValueCache authIdToAuthValueCache;
	private final IotServiceClient iotServiceClient;
	private final EventDataPending eventDataPending;
	private final String iotDomainOcid;
	private final String digitalTwinModelOcid;
	private final String digitalTwinAdaptorOcid;
	private Future<?> ourFuture;
	private boolean running = false;
	private final boolean uploaddata;
	private final boolean multithreaduploads;
	private final boolean onInstanceCreationGetErrorResubmit;

	@Inject
	public EventQueueProcessor(@Property(name = "gateway.uploaddata", defaultValue = "true") boolean uploaddata,
			@Property(name = "gateway.multithreaduploads", defaultValue = "true") boolean multithreaduploads,
			@Property(name = "gateway.instance.oninstancecreationgeterrorresubmit", defaultValue = "true") boolean onInstanceCreationGetErrorResubmit,
			EventQueue eventQueue, InstanceKeyTransformService instanceKeyTransformerService,
			EventDataTransformService eventDataTransformerService, IdToInstanceMapping idToInstanceMapping,
			NewInstanceSecretProvider newInstanceSecretProvider, IotServiceDetails iotServiceDetails,
			AuthIdToAuthValueCache authIdToAuthValueCache, IotServiceClient iotServiceClient,
			EventDataPending eventDataPending) {

		this.uploaddata = uploaddata;
		if (!uploaddata) {
			log.warning(
					"Danger Will Robinson, Danger. Uploads to the IoT service are disabled, the gateway will only accept data and call any reformatters");
		}
		this.multithreaduploads = multithreaduploads;
		this.onInstanceCreationGetErrorResubmit = onInstanceCreationGetErrorResubmit;
		this.eventQueue = eventQueue;
		this.instanceKeyTransformerService = instanceKeyTransformerService;
		log.info("InstanceKeyTransformer config is " + instanceKeyTransformerService.getConfig());

		this.eventDataTransformerService = eventDataTransformerService;
		log.info("EventDataTransformer config is " + eventDataTransformerService.getConfig());

		this.idToInstanceMapping = idToInstanceMapping;
		this.newInstanceSecretProvider = newInstanceSecretProvider;
		this.iotServiceDetails = iotServiceDetails;
		this.authIdToAuthValueCache = authIdToAuthValueCache;
		this.iotServiceClient = iotServiceClient;
		this.eventDataPending = eventDataPending;
		// as we are going to use the ocid based auth setup as the auth info in the
		// instance model is an ocid we might as well get the other ocid's while we're
		// at it rather than create new ones each time
		this.iotDomainOcid = iotServiceDetails.getIotDomain().getId();
		this.digitalTwinModelOcid = iotServiceDetails.getDigitalTwinModel().getId();
		this.digitalTwinAdaptorOcid = iotServiceDetails.getDigitalTwinAdapter().getId();
		// something to run our threads
		this.executors = Executors.newCachedThreadPool();
	}

	@PreDestroy
	public void preDestroy() {
		stopProcessing();
	}

	public void stopProcessing() {
		// flag to stop processing
		running = false;
		// interrupt any wait
		ourFuture.cancel(true);
		log.info("There are " + eventQueue.getQueueSize() + " events being discarded from the queue");
	}

	@PostConstruct
	public void postConstruct() {
		log.info("Post Construct EventQueueProcessor (eventQueue) is " + eventQueue.toString());
		running = true;
		// start up the core processing thread to run through the requests
		ourFuture = executors.submit(this);
	}

	@Override
	public void run() {
		while (running) {
			try {
				log.fine(() -> "Requesting next event queue data item from the event queue");
				EventQueueData eventData = eventQueue.getNext();
				if (multithreaduploads) {
					log.finer(() -> "Got event queue data item from the event queue, stating new thread to process it");
					executors.execute(() -> processDequeuedEvent(eventData));
				} else {
					log.finer(
							() -> "Got event queue data item from the event queue, using existing thread to process it");
					processDequeuedEvent(eventData);
				}
			} catch (InterruptedException e) {
				if (!running) {
					return;
				} else {
					log.warning("InterruptedException while getting event data from queue in running state, "
							+ e.getLocalizedMessage());
					continue;
				}
			}
		}
	}

	@AllArgsConstructor
	private class CreateDigitalTwinInstanceResponse {
		boolean created;
		DigitalTwinInstance dti;
	}

	/**
	 * @param eventData
	 * 
	 */
	public void processDequeuedEvent(EventQueueData eventData) {
		try {
			// if there is a pending registration in progress then add this to the queue and
			// return
			if (eventDataPending.ifPendingRegistrationAddToList(eventData)) {
				// got a true result, so the device registration is in process, it will have
				// been added to the queue so for now just more on to the next item
				log.fine(() -> "Event data relates to a instance whose registation pending, it will be processed later "
						+ eventData);
				return;
			} else {
				log.finest(
						() -> "Even data is not relating to the pending instance creation list, it may be new or a instance that has been fuly created "
								+ eventData);
			}

			// try to reformat the data
			String reformattedInstanceKey;
			try {
				reformattedInstanceKey = instanceKeyTransformerService.reformatInstanceKey(eventData.getInstanceKey());
			} catch (InstanceKeyIncommingFormatException e) {
				log.info("Problem with the format of the instance key " + e.getLocalizedMessage());
				return;
			} catch (InstanceKeyTransformException e) {
				log.info("Problem reformatting the instance key " + e.getLocalizedMessage());
				e.printStackTrace();
				return;
			}
			log.finer(() -> "Instancekey source = " + eventData.getInstanceKey() + ", reformatted = "
					+ reformattedInstanceKey);

			String reformattedPayload;
			try {
				reformattedPayload = eventDataTransformerService.reformatEventData(eventData.getPayload());
			} catch (EventDataIncommingFormatException e) {
				log.info("Problem with the format of the event data " + "e.getLocalizedMessage()");
				return;
			} catch (EventDataTransformException e) {
				log.info("Problem reformatting the event data " + "e.getLocalizedMessage()");
				e.printStackTrace();
				return;
			}

			log.finer(() -> "Payload source = " + eventData.getPayload() + ", reformatted = " + reformattedPayload);

			if (uploaddata) {
				CreateDigitalTwinInstanceResponse createDigitalTwinInstanceResponse;
				try {
					createDigitalTwinInstanceResponse = createOrGetDigitalTwinInstance(reformattedInstanceKey,
							eventData);
				} catch (MissingOciResourceException e) {
					log.warning("Exception getting or creating the digital twin instance for " + reformattedInstanceKey
							+ ", " + e.getLocalizedMessage());
					// it's possible that we have in-flight data that's arrived since we started
					// processing and is held in a pending queue
					// we should either re-submit it or ditch it
					if (onInstanceCreationGetErrorResubmit) {
						// we are resubmitting, include the current event, if the error is temporary
						// this will trigger the process to create a new instance
						int resubmitCount = eventDataPending.resubmitPendingRegistrationEvents(eventData, eventQueue,
								executors);
						log.info(() -> "Resumbitting " + resubmitCount
								+ " events including the current and pending ones");
					} else {
						int dropCount = eventDataPending.deletePendingregistrationEvents(eventData);
						log.info(() -> "Dropped " + dropCount + " events including the current and pending ones");
					}
					return;
				}
				if (createDigitalTwinInstanceResponse.dti == null) {
					log.warning("Unable to get or create the digital twin instance for " + reformattedInstanceKey);
				} else {
					try {
						uploadEventToIotService(reformattedPayload, createDigitalTwinInstanceResponse.dti);
					} catch (MissingOciResourceException e) {
						log.warning("Problem uploading to IoT service " + e.getLocalizedMessage());
						return;
					}
				}
				// if the instance was created then there may be queues events that arrived
				// while the creation was underway so
				if (createDigitalTwinInstanceResponse.created) {
					log.info("Created initial device for source " + eventData.getInstanceKey()
							+ " and uploaded it's data, re-queuing any remaining events");
					eventDataPending.clearPendingRegistrationEvents(eventData, eventQueue, executors);
				}
			} else {
				log.info(() -> "upload is disabled, reformattedInstanceKey=" + reformattedInstanceKey
						+ ", reformattedPayload=" + reformattedPayload);
			}
		} catch (Exception e) {
			log.severe("Exception processing the item eventData, " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private CreateDigitalTwinInstanceResponse createOrGetDigitalTwinInstance(String sourceId, EventQueueData eventData)
			throws MissingOciResourceException {
		log.finer(() -> "Looking for instance " + sourceId);
		// do we have an instance cached already for this ?
		DigitalTwinInstance dti = idToInstanceMapping.getDigitalTwinInstance(sourceId);
		// if there is no instance then we need to create one
		if (dti != null) {
			log.finer(() -> "Located existing instance for " + sourceId);
			return new CreateDigitalTwinInstanceResponse(false, dti);
		}
		// this is being created, we need to add this to the queue to be processed.
		eventDataPending.createPendingEntry(eventData);
		log.info(() -> "Didn't locate instance, having to create it " + sourceId);
		// need to build one
		// let's get the certificate OCID - depending on if we are reusing or generating
		// a new one this may take some time ! Here we are using an injected instance
		// selected based on the users config so we don't know what will actually happen
		String authOcid = newInstanceSecretProvider.getVaultSecretOcidForNewInstance(sourceId);
		dti = iotServiceDetails.getIoTProcessor().createDigitalTwinInstance(iotDomainOcid, sourceId, authOcid, sourceId,
				"Gateway generated device for " + sourceId, digitalTwinModelOcid, digitalTwinAdaptorOcid);
		// stash the details so we don't try and create it again later
		idToInstanceMapping.save(sourceId, dti);
		log.info(() -> "Built instance " + sourceId);
		// the auth value will be figured out later
		return new CreateDigitalTwinInstanceResponse(true, dti);
	}

	private void uploadEventToIotService(String reformattedPayload, DigitalTwinInstance dti)
			throws MissingOciResourceException {
		// get the credentials info, the device must have existed (or been created) to
		// get to here
		String authOcid = dti.getAuthId();
		String authData = authIdToAuthValueCache.getAuthData(authOcid);
		if (authData == null) {
			throw new MissingOciResourceException("can't locate auth data for instance " + dti.getDisplayName()
					+ " which has auth id of " + dti.getAuthId());
		}
		iotServiceClient.sendEvent(dti.getExternalKey(), authData, reformattedPayload);
	}

}
