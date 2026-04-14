package com.oracle.demo.timg.iot.iotdbjdbc.tester;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.dataread.IoTDBClient;
import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawDataId;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton

/*
 * this class does business logic stuff needed to retrieve the data from the IOT
 * instance
 * 
 * it sits on the data source which uses the file system based db token. it
 * really needs to be updated to handle a dynamic token refresh, but that can be
 * done later
 */
@Log
@Requires(property = "iotdatacache.jdbc.doconnectiontestread.enabled", value = "true", defaultValue = "false")
@Requires(property = "iotdatacache.jdbc.doconnectiontestread.order")
public class IoTJDBCConnectionTestReader implements IoTDBClient {
	private final DBConnectionSupplier dbConnectionSupplier;
	private final String schemaName;
	private final int jdbcValidationTimeout;
	private final int order;
	private Connection conn;

	@Inject
	public IoTJDBCConnectionTestReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.valudationtimeout", defaultValue = "5") int jdbcValidationTimeout,
			@Property(name = "iotdatacache.jdbc.doconnectiontestread.order") int order) throws SQLException, Exception {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName;
		this.jdbcValidationTimeout = jdbcValidationTimeout;
		this.order = order;
	}

	@Override
	public void configureDBClient(String filteringRule) throws Exception {
		conn = dbConnectionSupplier.getNewConnection(schemaName);
	}

	@Override
	public void startDBProcessing() throws Exception {
		// strictly speaking this should be done in a separate thread to allow other
		// things to run, but this is test code
		log.info("Getting sample raw data using JDBC");
		String entries = getRawData().stream().map(rd -> rd.toString()).collect(Collectors.joining("\n"));
		log.info("Raw data entries are :\n" + entries);
	}

	@Override
	public void stopDBProcessing() throws Exception {
		// nothing to do, we do it all in the start
	}

	@Override
	public void unconfigureDBClient() throws Exception {
		conn.close();
	}

	public List<RawData> getRawData() throws SQLException, Exception {
		List<RawData> results = new LinkedList<>();
		if (!conn.isValid(jdbcValidationTimeout)) {
			conn = dbConnectionSupplier.getNewConnection(schemaName);
		}
		// note that we are assuming here that the current schema for the connection has
		// been set.
		String queryString = "SELECT DIGITAL_TWIN_INSTANCE_ID, ENDPOINT,CONTENT_TYPE, CONTENT, TIME_RECEIVED FROM raw_data ORDER BY TIME_RECEIVED DESC FETCH FIRST 5 ROWS ONLY";
		try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(queryString)) {
			int i = 1;
			while (rs.next()) {
				RawDataId id = new RawDataId(rs.getString("DIGITAL_TWIN_INSTANCE_ID"), rs.getString("ENDPOINT"),
						rs.getTimestamp("TIME_RECEIVED"));
				RawData rawData = new RawData(id, rs.getString("CONTENT_TYPE"), rs.getBlob("CONTENT").toString());
				results.add(rawData);
				log.info("Result set row " + i + " = " + rawData);
				i++;
			}
		} catch (Exception e) {
			log.severe("Problem connecting to DB " + e.getLocalizedMessage());
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}
}
