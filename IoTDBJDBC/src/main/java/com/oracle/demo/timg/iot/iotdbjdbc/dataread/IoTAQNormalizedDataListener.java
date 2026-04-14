package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
				log.info("Received a null message");
				return;
			}
			processAQMessage(message);
			connection.commit();
		} catch (SQLException e) {
			log.warning("SQL Exception while getting message from AQ");
		}
	}

	@Override
	public int getOrder() {
		return order;
	}
}
