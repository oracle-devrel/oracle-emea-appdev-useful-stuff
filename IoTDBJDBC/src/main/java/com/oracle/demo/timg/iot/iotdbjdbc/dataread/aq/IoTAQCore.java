package com.oracle.demo.timg.iot.iotdbjdbc.dataread.aq;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;

import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import lombok.extern.java.Log;
import oracle.jdbc.OracleConnection;

@Log

public abstract class IoTAQCore {
	private final static int ORACLE_AQ_ALREADY_SUBSCRIBED_ERROR_CODE = 24034;
	public final static String SCHEMA_SUFFIX = "__IOT";
	protected final DBConnectionSupplier dbConnectionSupplier;
	protected final String schemaName;
	protected final String aqsubscribername;
	protected final int jdbcValidationTimeout;

	protected final String normalisedQueueName;
	protected OracleConnection connection;

	public IoTAQCore(DBConnectionSupplier dbConnectionSupplier, String schemaName, String queueName,
			int jdbcValidationTimeout, String aqsubscribername) throws SQLException, Exception {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName + SCHEMA_SUFFIX;
		this.normalisedQueueName = (schemaName + "." + queueName).toUpperCase();
		this.jdbcValidationTimeout = jdbcValidationTimeout;
		this.aqsubscribername = aqsubscribername;
		connection = dbConnectionSupplier.getNewConnection(schemaName);
	}

	protected void addSubscriber(String rule) throws SQLException {
		log.info("Adding subscriber " + aqsubscribername);
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
		log.info("Added subscriber " + aqsubscribername);
	}

	protected void removeSubscriber() throws SQLException {
		log.info("Removing Subscriber " + aqsubscribername);
		try (CallableStatement statement = connection
				.prepareCall("begin dbms_aqadm.remove_subscriber(queue_name => ?, subscriber => ?); end;")) {
			statement.setString(1, normalisedQueueName);
			statement.setObject(2, createSubscriberStruct());
			statement.execute();
		}
		log.info("Removed subscriber " + aqsubscribername);
	}

	private Struct createSubscriberStruct() throws SQLException {
		return connection.createStruct("SYS.AQ$_AGENT", new Object[] { aqsubscribername, null, 0 });
	}
}
