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
		dataSource.setUser(username);
		dataSource.setPassword(password);
	}

//	private RawDataRepository rawDataRepository;
//
//	@Inject
//	public IoTJDBCReader(RawDataRepository rawDataRepository) {
//		this.rawDataRepository = rawDataRepository;
//	}
//
//	public List<RawData> getRawData() {
//		return rawDataRepository.findAll();
//	}
	public List<RawData> getRawData() throws SQLException {
		List<RawData> results = new LinkedList<>();

		String queryString = "SELECT DIGITAL_TWIN_INSTANCE_ID, ENDPOINT,CONTENT_TYPE, CONTENT,  TIME_RECEIVED FROM "
				+ schemaName + ".raw_data";
		try (Connection conn = dataSource.getConnection();
				Statement st = conn.createStatement();
				ResultSet rs = st.executeQuery(queryString)) {
			int i = 1;
			if (rs.next()) {
				RawDataId id = new RawDataId(rs.getString("DIGITAL_TWIN_INSTANCE_ID"), rs.getString("ENDPOINT"),
						rs.getDate("TIME_RECEIVED"));
				RawData rawData = new RawData(id, rs.getString("CONTENT_TYPE"), rs.getString("CONTENT"));
				results.add(rawData);
				System.out.println("Result set row " + i + " = " + rawData);
			}
		}
		return results;
	}
}
