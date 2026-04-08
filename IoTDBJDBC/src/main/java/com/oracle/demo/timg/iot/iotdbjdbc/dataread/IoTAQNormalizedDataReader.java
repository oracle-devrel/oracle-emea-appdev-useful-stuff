package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.SQLException;

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
public class IoTAQNormalizedDataReader {
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

	public void stop() {
		this.stopped = true;
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

}
