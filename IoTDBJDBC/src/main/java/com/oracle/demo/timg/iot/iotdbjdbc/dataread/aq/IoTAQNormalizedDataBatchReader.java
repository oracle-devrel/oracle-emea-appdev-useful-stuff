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

package com.oracle.demo.timg.iot.iotdbjdbc.dataread.aq;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTDBClient;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandlerService;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Min;
import lombok.extern.java.Log;
import oracle.jdbc.aq.AQDequeueOptions;
import oracle.jdbc.aq.AQMessage;

@Singleton
@Log
@Requires(property = "iotdatacache.aq.normalizeddata.batchreader.enabled", value = "true", defaultValue = "false")
@Requires(property = "iotdatacache.aq.normalizeddata.batchreader.order")
public class IoTAQNormalizedDataBatchReader extends IoTAQNormalizedDataCore implements IoTDBClient, Runnable {

	public final static String QUEUE_SUBSCRIBER_SUFFIX = "batchreader";
	private boolean stopped = false;
	private final AQDequeueOptions dequeueOptions;
	private final int aqReadTimeout;
	private final int aqBatchSize;
	private final int order;
	private Thread currentThread;
	private ExecutorService executor;

	@Inject
	private NormalizedDataMessageHandlerService normalizedDataMessageHandlerService;

	@Inject
	public IoTAQNormalizedDataBatchReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.validationtimeout", defaultValue = "5") @Min(value = 1) int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aq.normalizeddata.subscribername", defaultValue = "aqclientnormalized") String aqsubscribername,
			@Property(name = "iotdatacache.aq.normalizeddata.batchreader.readtimeout", defaultValue = "10") @Min(value = 0) int aqReadTimeout,
			@Property(name = "iotdatacache.aq.normalizeddata.batchreader.batchsize", defaultValue = "10") @Min(value = 1) int aqBatchSize,
			@Property(name = "iotdatacache.aq.normalizeddata.batchreader.order") @Min(value = 0) int order)
			throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, jdbcValidationTimeout, aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
		this.aqReadTimeout = aqReadTimeout;
		this.aqBatchSize = aqBatchSize;
		this.order = order;
		dequeueOptions = new AQDequeueOptions();
		dequeueOptions.setDequeueMode(AQDequeueOptions.DequeueMode.REMOVE);
		dequeueOptions.setWait(aqReadTimeout);
		dequeueOptions.setNavigation(AQDequeueOptions.NavigationOption.FIRST_MESSAGE);
		dequeueOptions.setConsumerName(aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
		log.info("Running with aqTimeout of " + aqReadTimeout + " and batch size of " + aqBatchSize);
	}

	@Override
	public void configureDBClient(String filteringRule) throws Exception {
		executor = Executors.newCachedThreadPool();
		// the aq options have been set in the constructor, but we need to tell the
		// database about us
		super.addSubscriber(filteringRule);
	}

	private void readAQMessages() throws SQLException {
		log.info("Starting to read AQ messages");
		int readCounter = 0;
		while (!stopped) {
			// read a value
			AQMessage messages[];
			try {
				messages = connection.dequeue(getQueueName(), dequeueOptions, "JSON", aqBatchSize);
			} catch (SQLException e) {
				if (e.getErrorCode() == 25228) {
					log.finest("Timeout reading messages");
					continue;
				}
				log.warning("SQLException getting messages, " + e.getLocalizedMessage());
				continue;
			}
			// I guess if we have a timeout while waiting for a message to be available to
			// dequeue there will be a null message
			if (messages == null) {
				log.finest("Received a null message");
				continue;
			}
			log.fine(() -> "Queue returned " + messages.length + " out of a max read size of " + aqBatchSize);
			processRetrievedMessages(readCounter, messages);
			readCounter++;
			connection.commit();
		}
		log.info("Finished to reading AQ messages");
	}

	/**
	 * @param readCounter
	 * @param messages
	 */
	protected void processRetrievedMessages(int readCounter, AQMessage[] messages) {
		IntStream.range(0, messages.length).forEachOrdered((i) -> {
			try {
				NormalizedData normalizedData = convertToNormalizedData(messages[i].getJSONPayload());
				log.finer(() -> "Received message block " + readCounter + ", message no " + i + ", " + normalizedData);
				normalizedDataMessageHandlerService.handle(normalizedData);
			} catch (SQLException e) {
				log.warning(() -> "SQLException processing message block " + readCounter + ", message" + i);
			}
		});
		// ultimately this could be a stream, but we want to track the number when
		// processing so let's use a loop.
//		for (int i = 0; i < messages.length; i++) {
//			try {
//				NormalizedData normalizedData = convertToNormalizedData(messages[i].getJSONPayload());
//				// can't use a lambda here as readCounter AND i are both being updated so can't
//				// use an int stream.
//				log.finer("Received message block " + readCounter + ", message no " + i + ", " + normalizedData);
//				normalizedDataMessageHandlerService.handle(normalizedData);
//			} catch (SQLException e) {
//				log.warning(() -> "SQLException processing message block " + readCounter + ", message" + i);
//			}
//		}
	}

	@Override
	public void startDBProcessing() throws Exception {
		// start this in a separate loop
		executor.execute(() -> this.run());
	}

	@Override
	public void stopDBProcessing() throws Exception {
		log.info("Stopping reading");
		this.stopped = true;
		// interrupt the thread if it's not null
		if (currentThread != null) {
			log.info("Interrupting thread");
			currentThread.interrupt();
		}
	}

	@Override
	public void unconfigureDBClient() throws Exception {
		// stop the executors from accepting new tasks
		executor.shutdown();
		// unconfigure the queue
		removeSubscriber();
	}

	@Override
	public void run() {
		// save the thread we're running in so we can interrupt it later
		currentThread = Thread.currentThread();
		try {
			log.info("Starting read loop");
			readAQMessages();
		} catch (SQLException e) {
			log.severe("SQLException in read loop, " + e.getLocalizedMessage());
		}
		log.info("Completed read thread");
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getConfig() {
		return "Order " + getOrder() + "Read timeout " + aqReadTimeout + " batch size " + aqBatchSize + " client name "
				+ getAqsubscribername();
	}
}
