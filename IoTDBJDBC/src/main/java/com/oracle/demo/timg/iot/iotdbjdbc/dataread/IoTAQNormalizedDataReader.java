package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import static com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTAQNormalizedDataCore.convertToNormalizedData;

import java.sql.SQLException;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.jdbc.aq.AQDequeueOptions;
import oracle.jdbc.aq.AQMessage;

@Singleton
@Log
public class IoTAQNormalizedDataReader extends IoTAQNormalizedDataCore implements Runnable {

	private boolean stopped = false;
	private Thread currentThread;

	@Inject
	public IoTAQNormalizedDataReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.valudationtimeout", defaultValue = "5") int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aqsubscribername", defaultValue = "aqreader") String aqsubscribername)
			throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, jdbcValidationTimeout, aqsubscribername);
	}

	public void readAQMessages() throws SQLException {
		log.info("Setting up AQ reader");
		AQDequeueOptions dequeueOptions = new AQDequeueOptions();
		dequeueOptions.setDequeueMode(AQDequeueOptions.DequeueMode.REMOVE);
		dequeueOptions.setWait(10);
		dequeueOptions.setNavigation(AQDequeueOptions.NavigationOption.FIRST_MESSAGE);
		dequeueOptions.setConsumerName(aqsubscribername);
		while (!stopped) {
			// read a value
			AQMessage message = connection.dequeue(normalisedQueueName, dequeueOptions, "JSON");
			// I guess if we have a timeout while waiting for a message to be available to
			// dequeue there will be a null message
			if (message == null) {
				log.info("Received a null message");
				continue;
			}
			NormalizedData normalizedData = convertToNormalizedData(message.getJSONPayload());
			log.info("Received " + normalizedData);
			connection.commit();
		}
	}

	public void stopAQAccess() {
		log.info("Stopping reading");
		this.stopped = true;
		// interrupt the thread if it's not null
		if (currentThread != null) {
			log.info("Interrupting thread");
			currentThread.interrupt();
		}
	}

	@Override
	public void run() {
		// save the thread we're running in so we can interrupt it later
		currentThread = Thread.currentThread();
		try {
			log.info("Setting up subscriber");
			addSubscriber(null);
		} catch (SQLException e) {
			log.severe("SQLException while setting up subscriber, " + e.getLocalizedMessage());
			return;
		}

		try {
			log.info("Starting read loop");
			readAQMessages();
		} catch (SQLException e) {
			log.severe("SQLException in read loop will attempt to remove subsciber, " + e.getLocalizedMessage());
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
