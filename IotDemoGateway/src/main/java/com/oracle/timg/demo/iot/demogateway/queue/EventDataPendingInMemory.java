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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
@Requires(property = "gateway.eventdatapending.type", value = "IN_MEMORY", defaultValue = "unknown")

/*
 * this class is used to hold additional events that have arrived while the
 * device for the event is being created, this should reduce (but I don't think
 * can guarantee) the chance that event data would be sent in that window
 * 
 */
public class EventDataPendingInMemory implements EventDataPending {
	// no point in this being an MT safe map as we will be synchronizing at the
	// class level
	private final Map<String, List<EventQueueData>> pendingInstanceCreation = new HashMap<>();

	/**
	 * if there is a pending registration add the event to the queue of pending
	 * calls for that device and return true (indicating that the caller should not
	 * proceed, if there is no pending registration return false
	 * 
	 * @param data
	 * @return
	 */
	@Override
	public boolean ifPendingRegistrationAddToList(EventQueueData eventData) {
		log.finer(() -> "Checking for creation in process for event from source " + eventData.getInstanceKey());
		synchronized (pendingInstanceCreation) {
			if (pendingInstanceCreation.containsKey(eventData.getInstanceKey())) {
				log.finer(() -> "Event from source " + eventData.getInstanceKey()
						+ " relates to a instance thats being created, queueing it");
				pendingInstanceCreation.get(eventData.getInstanceKey()).add(eventData);
				return true;
			} else {
				log.finest(() -> "Event from source " + eventData.getInstanceKey()
						+ " is not related to an instance being created");
				return false;
			}
		}
	}

	@Override
	public boolean createPendingEntry(EventQueueData eventData) {
		synchronized (pendingInstanceCreation) {
			log.fine(() -> "Creating pending queue for Event from source " + eventData.getInstanceKey());
			// there should not be any entry here, but just in case
			if (pendingInstanceCreation.containsKey(eventData.getInstanceKey())) {
				// there is a list, so just return, send false back so a caller can know if
				// there was a problem
				return false;
			} else {
				pendingInstanceCreation.put(eventData.getInstanceKey(), new LinkedList<>());
				return true;
			}
		}
	}

	/**
	 * Do the actual event transfer in a separate thread to remove blocking
	 * situations
	 *
	 * @param eventData  a "inflight" event to be sent first and is used to figure
	 *                   out what pending events (if any) to transfer over
	 * @param eventQueue the queue to transfer any pending events to
	 * @param executor   used to generate the thread to do the transfer
	 * @return -1 if there was no list of pending events (probably a coding or
	 *         timing error) or the number of events that are going to be transfered
	 *         to the events queue
	 */
	@Override
	public int resubmitPendingRegistrationEvents(EventQueueData eventData, EventQueue eventQueue, Executor executor) {
		List<EventQueueData> eventsList;
		synchronized (pendingInstanceCreation) {
			// get the saved list (if there was one) while removing it from the map
			eventsList = pendingInstanceCreation.remove(eventData.getInstanceKey());
			if (eventsList == null) {
				return -1;
			}

		}
		eventsList.addFirst(eventData);
		return uploadPendingRegistrationEvents(eventData, eventQueue, executor, eventsList);
	}

	@Override
	public int deletePendingregistrationEvents(EventQueueData eventData) {
		List<EventQueueData> eventsList;
		synchronized (pendingInstanceCreation) {
			// get the saved list (if there was one) while removing it from the map
			eventsList = pendingInstanceCreation.remove(eventData.getInstanceKey());
			if (eventsList == null) {
				return -1;
			}

		}
		return eventsList.size();
	}

	/**
	 * @param eventData
	 * @param eventQueue
	 * @param executor
	 * @param eventsList
	 * @return
	 */
	private int uploadPendingRegistrationEvents(EventQueueData eventData, EventQueue eventQueue, Executor executor,
			List<EventQueueData> eventsList) {
		//
		// have to stash this as the transfer might start before we can return causing
		// sync problems,
		int eventsPendingCount = eventsList.size();
		// this might possibly result in a new event arriving and having it go into the
		// queue before or in the wrong please of the rest of the events, but as
		// hopefully all the events have a time stamp they will order themselves in the
		// iot instance
		// the other option would mean blocking until the events have been transfered
		// which may be problematic, esp if the event queue was size limited as the
		// thread calling us may then deadlock and be unable to remove events from the
		// queue,
		log.fine(() -> "Scheduling requeue for pending events for " + eventData.getInstanceKey());
		executor.execute(() -> transferEventData(eventData, eventsList, eventQueue));
		return eventsPendingCount;
	}

	/**
	 * Do the actual event transfer in a separate thread to remove blocking
	 * situations
	 *
	 * @param eventData  is used to figure out what pending events (if any) to
	 *                   transfer over
	 * @param eventQueue the queue to transfer any pending events to
	 * @param executor   used to generate the thread to do the transfer
	 * @return -1 if there was no list of pending events (probably a coding or
	 *         timing error) or the number of events that are going to be transfered
	 *         to the events queue
	 */
	@Override
	public int clearPendingRegistrationEvents(EventQueueData eventData, EventQueue eventQueue, Executor executor) {
		List<EventQueueData> eventsList;
		synchronized (pendingInstanceCreation) {
			// get the saved list (if there was one) while removing it from the map
			eventsList = pendingInstanceCreation.remove(eventData.getInstanceKey());
			if (eventsList == null) {
				return -1;
			}
		}
		return uploadPendingRegistrationEvents(eventData, eventQueue, executor, eventsList);
	}

	private void transferEventData(EventQueueData eventData, List<EventQueueData> eventsList, EventQueue eventQueue) {
		// move each pending event onto the main events queue in the order they were
		// received
		log.fine(() -> "Requeue of " + eventsList.size() + " pending events for " + eventData.getInstanceKey());
		eventQueue.addToQueue(eventsList);
	}

	@Override
	public String getConfig() {
		return "EventDataPendingInMemory";
	}

	@Override
	public String getStatus() {
		synchronized (pendingInstanceCreation) {
			int pendingEvents = pendingInstanceCreation.values().stream().mapToInt(List::size).sum();
			return "There are " + pendingInstanceCreation.size() + " lists with a total of " + pendingEvents
					+ " pending";
		}
	}

	@PostConstruct
	public void postConstruct() {
		log.info(getConfig() + " - " + getStatus());
	}
}
