package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawDataId;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
//import com.oracle.demo.timg.iot.iotdbjdbc.dbschema.RawDataRepository;
import oracle.jdbc.pool.OracleDataSource;

@Singleton

/*
 * this class does business logic stuff needed to retrieve the data from the IOT
 * instance
 * 
 * it sits on the data source which uses the file system based db token. it
 * really needs to be updated to handle a dynamic token refresh, but that can be
 * done later
 */
public class IoTJDBCReader {
	private OracleDataSource dataSource;
	private String url;
	private String schemaName;
	private String username;
	private String password;

	@Inject
	public IoTJDBCReader(@Property(name = "datasources.default.url") String url,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "datasources.default.username", defaultValue = "") String username,
			@Property(name = "datasources.default.password", defaultValue = "") String password) throws SQLException {
		this.url = url;
		this.schemaName = schemaName;
		this.username = username;
		this.password = password;
		dataSource = new OracleDataSource();
		dataSource.setURL(url);
		if (username.length() > 0) {
			dataSource.setUser(username);
		}

		if (password.length() > 0) {
			dataSource.setPassword(password);
		}
	}

	public List<RawData> getRawData() throws SQLException {
		List<RawData> results = new LinkedList<>();

		String queryString = "SELECT DIGITAL_TWIN_INSTANCE_ID, ENDPOINT,CONTENT_TYPE, CONTENT,  TIME_RECEIVED FROM raw_data";
		try (Connection conn = dataSource.getConnection();
				Statement st = conn.createStatement();
				// for efficiency this should only be done when we get a new connection that has
				// not had it's current scheme altered, but for not this is a simple approach
				ResultSet rsSchema = st.executeQuery("alter session set current_schema=" + schemaName);
				ResultSet rs = st.executeQuery(queryString)) {
			int i = 1;
			if (rs.next()) {
				RawDataId id = new RawDataId(rs.getString("DIGITAL_TWIN_INSTANCE_ID"), rs.getString("ENDPOINT"),
						rs.getDate("TIME_RECEIVED"));
				RawData rawData = new RawData(id, rs.getString("CONTENT_TYPE"), rs.getBlob("CONTENT").toString());
				results.add(rawData);
				System.out.println("Result set row " + i + " = " + rawData);
			}
		}
		return results;
	}
}
