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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTDBClient;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.aq.AQDequeueOptions;
import oracle.jdbc.aq.AQMessage;
import oracle.jdbc.aq.AQNotificationEvent;
import oracle.jdbc.aq.AQNotificationListener;
import oracle.jdbc.aq.AQNotificationRegistration;

@Singleton
@Log
@Requires(property = "iotdatacache.aq.listener.enabled", value = "true", defaultValue = "false")
@Requires(property = "iotdatacache.aq.listener.order")
public class IoTAQNormalizedDataListener extends IoTAQNormalizedDataCore
		implements AQNotificationListener, IoTDBClient {
	public final static String QUEUE_SUBSCRIBER_SUFFIX = "listener";
	private ExecutorService executor;
	private final AQDequeueOptions dequeueOptions;
	private final int order;

	private AQNotificationRegistration aqNotificationRegistration;

	@Inject
	public IoTAQNormalizedDataListener(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.validationtimeout", defaultValue = "5") int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aqsubscribername", defaultValue = "aqreader") String aqsubscribername,
			@Property(name = "iotdatacache.aq.listener.order") int order) throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, jdbcValidationTimeout, aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
		this.order = order;
		// setup the dequeue details as they can be reused each time we get a message
		dequeueOptions = new AQDequeueOptions();
		dequeueOptions.setDequeueMode(AQDequeueOptions.DequeueMode.REMOVE);
		dequeueOptions.setWait(10);
		dequeueOptions.setNavigation(AQDequeueOptions.NavigationOption.FIRST_MESSAGE);
		dequeueOptions.setConsumerName(aqsubscribername + QUEUE_SUBSCRIBER_SUFFIX);
	}

	@Override
	public void configureDBClient(String filteringRule) throws Exception {
		executor = Executors.newCachedThreadPool();
		// the aq options have been set in the constructor, but we need to tell the
		// database about us
		super.addSubscriber(filteringRule);
	}

	@Override
	public void startDBProcessing() throws Exception {
		log.info("Setting up AQ registration");
		Properties globalOptions = new Properties();
		String[] queueNameArr = new String[1];
		queueNameArr[0] = normalisedQueueName;
		Properties[] opt = new Properties[1];
		opt[0] = new Properties();
		opt[0].setProperty(OracleConnection.NTF_AQ_PAYLOAD, "true");
		AQNotificationRegistration[] regArr = connection.registerAQNotification(queueNameArr, opt, globalOptions);
		aqNotificationRegistration = regArr[0];
		log.info("AQ Notification registration completed, adding listener");
		aqNotificationRegistration.addListener(this, executor);
		log.info("AQ Notification listened added");
	}

	@Override
	public void stopDBProcessing() throws Exception {
		aqNotificationRegistration.removeListener(this);
	}

	@Override
	public void unconfigureDBClient() throws Exception {
		super.removeSubscriber();
		executor.shutdown();
	}

	@Override
	public void onAQNotification(AQNotificationEvent aqNotificationEvent) {
		// the notification could have come from multiple AQ queues (if we had
		// subscribed to multiple ones) in that case the aqNotificationEvent woudl have
		// to be examined to make sure that we were going to get data from the right
		// queue, however for now at least we are only looking at a single AQ so we
		// don't need to bother
		try {
			// this will only get a single message, so hopefully there will be a
			// notifications for each message
			AQMessage message = connection.dequeue(normalisedQueueName, dequeueOptions, "JSON");
			// I guess if we have a timeout while waiting for a message to be available to
			// dequeue there will be a null message
			if (message == null) {
				log.finest("Received a null message");
				return;
			}
			outputAQMessage(message);
			connection.commit();
		} catch (SQLException e) {
			log.warning("SQL Exception while getting message from AQ");
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getConfig() {
		return "Order " + getOrder() + " client name " + aqsubscribername;
	}
}
