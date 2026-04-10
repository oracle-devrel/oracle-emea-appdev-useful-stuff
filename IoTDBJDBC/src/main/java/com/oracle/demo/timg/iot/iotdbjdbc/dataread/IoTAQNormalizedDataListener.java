package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
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
public class IoTAQNormalizedDataListener extends IoTAQNormalizedDataCore implements AQNotificationListener {

	private ExecutorService executor = Executors.newCachedThreadPool();
	private final AQDequeueOptions dequeueOptions;

	private AQNotificationRegistration aqNotificationRegistration;

	@Inject
	public IoTAQNormalizedDataListener(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,

			@Property(name = "iotdatacache.valudationtimeout", defaultValue = "5") int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aqsubscribername", defaultValue = "aqreader") String aqsubscribername)
			throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, jdbcValidationTimeout, aqsubscribername);
		// setup the dequeue details as they can be reused
		dequeueOptions = new AQDequeueOptions();
		dequeueOptions.setDequeueMode(AQDequeueOptions.DequeueMode.REMOVE);
		dequeueOptions.setWait(10);
		dequeueOptions.setNavigation(AQDequeueOptions.NavigationOption.FIRST_MESSAGE);
		dequeueOptions.setConsumerName(aqsubscribername);
	}

	private void registerForAQNotifications() throws SQLException, Exception {
		if (executor.isShutdown()) {
			throw new Exception("System has been shutdown");
		}
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

	/**
	 * note that this is a persistent call, once made you can't restart things
	 * 
	 * @throws SQLException
	 */
	private void stopAQNotifications() throws SQLException {
		aqNotificationRegistration.removeListener(this);
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
			NormalizedData normalizedData = convertToNormalizedData(message.getJSONPayload());
			log.info("Received " + normalizedData);
			connection.commit();
		} catch (SQLException e) {
			log.warning("SQL Exception while getting message from AQ");
		}
	}

	public boolean startListening() throws Exception {
		try {
			log.info("Setting up subscriber");
			addSubscriber(null);
		} catch (SQLException e) {
			log.severe("SQLException while setting up subscriber, " + e.getLocalizedMessage());
			return false;
		}

		try {
			log.info("Register for notifications");
			registerForAQNotifications();
		} catch (SQLException e) {
			log.severe("SQLException registering for notificaions, " + e.getLocalizedMessage());
			log.info("Attempting to remove subscriber");
			try {
				removeSubscriber();
				return false;
			} catch (SQLException e1) {
				log.severe("SQLException unregistering");
				return false;
			}
		}
		log.info("Setup subscription and registered for notifications");
		return true;
	}

	public void stopListening() {
		try {
			stopAQNotifications();
		} catch (SQLException e) {
			log.severe("SQLException stopping the notifications, " + e.getLocalizedMessage());
		}
		try {
			log.info("Attempting to remove subscriber");
			removeSubscriber();
		} catch (SQLException e) {
			log.severe("SQLException removing subsciber, " + e.getLocalizedMessage());
			return;
		}

		log.info("Completed");
	}
}
