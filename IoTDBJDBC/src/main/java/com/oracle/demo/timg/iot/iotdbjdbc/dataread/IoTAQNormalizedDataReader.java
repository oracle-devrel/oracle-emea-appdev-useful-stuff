package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.aq.AQDequeueOptions;
import oracle.jdbc.aq.AQMessage;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

@Singleton
@Log
public class IoTAQNormalizedDataReader implements Runnable {
	private final static int ORACLE_AQ_ALREADY_SUBSCRIBED_ERROR_CODE = 24034;
	public final static String SCHEMA_SUFFIX = "__IOT";
	private final DBConnectionSupplier dbConnectionSupplier;
	@Inject
	private ObjectMapper mapper;
	private final String schemaName;
	private final String aqsubscribername;
	private final int jdbcValidationTimeout;

	private final String normalisedQueueName;
	private OracleConnection connection;

	private boolean stopped = false;
	private Thread currentThread;

	@Inject
	public IoTAQNormalizedDataReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.valudationtimeout", defaultValue = "5") int jdbcValidationTimeout,
			@Property(name = "iotdatacache.aqsubscribername", defaultValue = "aqreader") String aqsubscribername)
			throws SQLException, Exception {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName + SCHEMA_SUFFIX;
		this.normalisedQueueName = (schemaName + "." + NormalizedData.SQL_QUEUE_NAME).toUpperCase();
		this.jdbcValidationTimeout = jdbcValidationTimeout;
		this.aqsubscribername = aqsubscribername;
		connection = dbConnectionSupplier.getNewConnection(schemaName);
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
			log.info("Recieved " + normalizedData);
			connection.commit();
		}
	}

	public void stopReading() {
		log.info("Stopping reading");
		this.stopped = true;
		// interrupt the thread if it's not null
		if (currentThread != null) {
			log.info("Interrupting thread");
			currentThread.interrupt();
		}
	}

	private static NormalizedData convertToNormalizedData(OracleJsonDatum payloadDatum) throws SQLException {
		OracleJsonObject payload = (OracleJsonObject) payloadDatum.toJdbc();

		String ocid = payload.getString("digitalTwinInstanceId", "");
		String contentPath = payload.getString("contentPath", "");
		String timeObserved = payload.getString("timeObserved", "");
		OracleJsonValue valueJson = payload.get("value");
		String value = (valueJson == null ? "" : valueJson.toString());
		return new NormalizedData(ocid, contentPath, timeObserved, value);
	}

	private void addSubscriber(String rule) throws SQLException {
		try (CallableStatement statement = connection.prepareCall("begin dbms_aqadm.add_subscriber("
				+ "queue_name => ?, " + "subscriber => ?, " + "rule => ?, " + "transformation => null, "
				+ "queue_to_queue => false, " + "delivery_mode => dbms_aqadm.persistent_or_buffered); end;")) {
			statement.setString(1, normalisedQueueName);
			statement.setObject(2, createSubscriberStruct());
			if (rule == null) {
				statement.setNull(3, Types.VARCHAR);
			} else {
				statement.setString(3, rule);
			}
			try {
				statement.execute();
			} catch (SQLException e) {
				if (e.getErrorCode() == ORACLE_AQ_ALREADY_SUBSCRIBED_ERROR_CODE) {
					log.info("Subscriber " + aqsubscribername + " is already subscribed to queue " + normalisedQueueName
							+ ", continuing");
				} else {
					// was another error code
					log.severe("SQLException subscribing " + aqsubscribername + " to queue " + normalisedQueueName
							+ ", " + e.getLocalizedMessage());
					throw e;
				}
			}
		}
	}

	private void removeSubscriber() throws SQLException {
		try (CallableStatement statement = connection
				.prepareCall("begin dbms_aqadm.remove_subscriber(queue_name => ?, subscriber => ?); end;")) {
			statement.setString(1, normalisedQueueName);
			statement.setObject(2, createSubscriberStruct());
			statement.execute();
		}
	}

	private Struct createSubscriberStruct() throws SQLException {
		return connection.createStruct("SYS.AQ$_AGENT", new Object[] { aqsubscribername, null, 0 });
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
	}
}
