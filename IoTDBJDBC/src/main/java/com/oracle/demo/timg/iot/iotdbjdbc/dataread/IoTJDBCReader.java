package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawDataId;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
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
public class IoTJDBCReader {
	private final DBConnectionSupplier dbConnectionSupplier;
	private final String schemaName;
	private final int jdbcValidationTimeout;
	private Connection conn;

	@Inject
	public IoTJDBCReader(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "iotdatacache.valudationtimeout", defaultValue = "5") int jdbcValidationTimeout)
			throws SQLException, Exception {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName;
		this.jdbcValidationTimeout = jdbcValidationTimeout;
		conn = dbConnectionSupplier.getNewConnection(schemaName);
	}

	public List<RawData> getRawData() throws SQLException, Exception {
		List<RawData> results = new LinkedList<>();
		if (!conn.isValid(jdbcValidationTimeout)) {
			conn = dbConnectionSupplier.getNewConnection(schemaName);
		}
		// note that we are assuming here that the current scheme for the connection has
		// been set.
		String queryString = "SELECT DIGITAL_TWIN_INSTANCE_ID, ENDPOINT,CONTENT_TYPE, CONTENT,  TIME_RECEIVED FROM raw_data";
		try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(queryString)) {
			int i = 1;
			if (rs.next()) {
				RawDataId id = new RawDataId(rs.getString("DIGITAL_TWIN_INSTANCE_ID"), rs.getString("ENDPOINT"),
						rs.getTimestamp("TIME_RECEIVED"));
				RawData rawData = new RawData(id, rs.getString("CONTENT_TYPE"), rs.getBlob("CONTENT").toString());
				results.add(rawData);
				System.out.println("Result set row " + i + " = " + rawData);
			}
		} catch (Exception e) {
			log.severe("Problem connecting to DB " + e.getLocalizedMessage());
		}
		return results;
	}
}
