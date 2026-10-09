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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.normalizeddata.consolidator;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.normalizeddata.consolidatorfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.normalizeddata.consolidatorfilter.order")
@Log
/**
 * keep on receiving NormalizedData elements
 */
public class NormalizedDataConsolidator implements NormalizedDataMessageHandler, Runnable {
	// this is keyed on the data deviceid and content path combined, as soon as
	// there is a entry with a new timestamp then the current one is added to the
	// outgoing queue, Additionally when the timestamps exceed the delay then the
	// message sis added to t outgoing queue in the background
	private final Map<String, NormalizedDataLastUpdatedTimestamp> inProgressMessages;
	private final List<NormalizedData> pendingOnwardNormalizedData = new LinkedList<>();
	private final int order;
	private final Duration sendDurationAfterLastMessage;
	private final Duration scanForExpiredMessagesInterval;
	private ScheduledFuture<?> nextOutdatedScanRun;
	private ScheduledExecutorService executor;
	private boolean shutdown = true;

	@Inject
	public NormalizedDataConsolidator(
			@Property(name = "messagehandler.filter.normalizeddata.consolidatorfilter.order") int order,
			@Property(name = "messagehandler.filter.normalizeddata.consolidatorfilter.senddurationafterlastmessage", defaultValue = "PT5S") Duration sendDurationAfterLastMessage,
			@Property(name = "messagehandler.filter.normalizeddata.consolidatorfilter.scanforexpiredmessagesinterval", defaultValue = "PT10S") Duration scanForExpiredMessagesInterval,
			@Property(name = "messagehandler.filter.normalizeddata.consolidatorfilter.inprogresscachesize", defaultValue = "128") int inProgressSize) {
		this.order = order;
		this.sendDurationAfterLastMessage = sendDurationAfterLastMessage;
		this.inProgressMessages = new HashMap<>(inProgressSize);
		this.scanForExpiredMessagesInterval = scanForExpiredMessagesInterval;
		log.info("Completed constructor");
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "NormalizedDataConsolidator";
	}

	@Override
	public String getConfig() {
		return getName() + " currently has " + inProgressMessages + " messages being built and "
				+ pendingOnwardNormalizedData.size() + " in the pending list. Messages not updated for "
				+ sendDurationAfterLastMessage
				+ " will be considered finished and inprogress messages will be scanned every "
				+ scanForExpiredMessagesInterval + " to see if they are finished";
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData inputNormalizedData) throws Exception {
		log.fine(() -> "Processing" + inputNormalizedData);
		// work out the key
		String key = NormalizedDataLastUpdatedTimestamp.getKey(inputNormalizedData);
		// try to get it if from the inprogress map
		NormalizedDataLastUpdatedTimestamp oldNormalizedDataLastUpdatedTimestamp;
		log.finer(() -> "Looking for existing message under " + key);
		synchronized (inProgressMessages) {
			oldNormalizedDataLastUpdatedTimestamp = inProgressMessages.get(key);
		}

		// if we have this already update it, if not create it
		if (oldNormalizedDataLastUpdatedTimestamp != null) {
			log.finer(() -> "Located existing message for " + key);
			// is the timestamp the same ? If not then we need to move it to the outgoing
			// queue and start a new one
			if (oldNormalizedDataLastUpdatedTimestamp.isTimestampIdentical(inputNormalizedData)) {
				log.fine("Timestamps match, updating existing message");
				// they are identical, update it
				oldNormalizedDataLastUpdatedTimestamp.updateWith(inputNormalizedData);
				log.finer(() -> "Updated message is " + oldNormalizedDataLastUpdatedTimestamp);
			} else {
				// it's a different timestamp, put the old on in the outgoing queue, then
				// replace it with the new one
				log.fine("Timestamps differ, queing existing message for output and generating next one");
				synchronized (pendingOnwardNormalizedData) {
					pendingOnwardNormalizedData.add(oldNormalizedDataLastUpdatedTimestamp.retrieveNormalizedData());
				}
				inProgressMessages.put(key, new NormalizedDataLastUpdatedTimestamp(inputNormalizedData));
			}
		} else {
			// it's not there yet, build and add it, no need to change the contents as this
			// is the first value, the builder will setup the time last updated to the
			// current time as it builds the instance.
			log.fine("No existing message, creating a new one");
			inProgressMessages.put(key, new NormalizedDataLastUpdatedTimestamp(inputNormalizedData));
		}
		synchronized (pendingOnwardNormalizedData) {
			int pendingMessagesSize = pendingOnwardNormalizedData.size();
			log.fine(() -> "There are " + pendingMessagesSize + " messages in the pending queue");
			NormalizedData[] pendingNormalizedData = new NormalizedData[pendingMessagesSize];
			pendingOnwardNormalizedData.toArray(pendingNormalizedData);
			pendingOnwardNormalizedData.clear();
			log.fine(() -> "Cleared the pending messages queue and returning " + pendingNormalizedData.length
					+ " consolidated events");
			return pendingNormalizedData;
		}
	}

	/**
	 * do any initial processing that's needed, for example establishing a JDBC
	 * Connection
	 * 
	 * @throws Exception
	 */
	@Override
	public void configure() throws Exception {
		executor = Executors.newSingleThreadScheduledExecutor();
		shutdown = false;
		nextOutdatedScanRun = executor.schedule(this, scanForExpiredMessagesInterval.toNanos(), TimeUnit.NANOSECONDS);
		log.info("Configured");
	}

	/**
	 * do any processing that's needed to tidy things up, for example closing a JDBC
	 * Connection
	 * 
	 * @throws Exception
	 */
	@Override
	public void unconfigure() throws Exception {
		// flag that we are shutdown
		shutdown = true;
		// stop any futures
		nextOutdatedScanRun.cancel(false);
		executor.shutdown();
		log.info("Unconfigured");
	}

	@Override
	public void run() {
		if (shutdown) {
			return;
		}
		Instant transferOlderThan = Instant.now().minus(sendDurationAfterLastMessage);
		synchronized (inProgressMessages) {
			log.fine(() -> "Scanning " + inProgressMessages.size() + " in progress messages for ones ready to send");
			Iterator<Map.Entry<String, NormalizedDataLastUpdatedTimestamp>> i = inProgressMessages.entrySet()
					.iterator();
			while (i.hasNext()) {
				Map.Entry<String, NormalizedDataLastUpdatedTimestamp> entry = i.next();
				if (entry.getValue().lastUpdatedBefore(transferOlderThan)) {
					synchronized (pendingOnwardNormalizedData) {
						log.finer(() -> "Message " + entry.getValue()
								+ " is outside the retain window, adding to outgoing list for later transmission");
						pendingOnwardNormalizedData.add(entry.getValue().retrieveNormalizedData());
					}
					i.remove();
				}
			}
		}
		nextOutdatedScanRun = executor.schedule(this, scanForExpiredMessagesInterval.toNanos(), TimeUnit.NANOSECONDS);
	}
}
