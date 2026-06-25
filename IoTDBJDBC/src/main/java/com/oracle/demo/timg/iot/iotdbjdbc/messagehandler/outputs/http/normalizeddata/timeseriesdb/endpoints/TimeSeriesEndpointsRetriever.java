package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.jdbc.pool.OracleDataSource;

/*
 *  the URL will need to have the DB wallet downloaded. assuming it's stored in configsecure/timeseriesdb it might look somethign like
 *  telemetry_high?TNS_ADMIN=configsecure/timeseriesdb
 */
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesEndpointsRetriever.TIME_SERIES_JDBC_CONNECTION_NAME)
@Requires(property = TimeSeriesEndpointsRetriever.TIME_SERIES_JDBC_USERNAME)
@Requires(property = TimeSeriesEndpointsRetriever.TIME_SERIES_JDBC_PASSWORD)
@Log
@Singleton
public class TimeSeriesEndpointsRetriever {
	public final static String DRIVER_URL_SEP = "@";
	public final static String DRIVER_TNS_NAME = "?TNS_ADMIN=";
	public final static String TIME_SERIES_JDBC_PROPERTIES = "timeseriesdb.jdbc";
	public final static String TIME_SERIES_JDBC_CONNECTION_NAME = ".connectionname";
	public final static String TIME_SERIES_JDBC_WALLET_PATH = ".walletpath";
	public final static String TIME_SERIES_JDBC_USERNAME = ".username";
	public final static String TIME_SERIES_JDBC_PASSWORD = ".password";
	public final static String TIME_SERIES_JDBC_DRIVER = ".driver";
	public final static String TIME_SERIES_ENDPOINTS_QUERY = "select dbms_cloud_telemetry_ingest.get_ingestion_endpoints as endpoints from dual";
	private final String connectionname, walletpath, username, password, driver, connectionstring;
	private final OracleDataSource dataSource;
	@Inject
	private ObjectMapper mapper;
	private TimeSeriesEndpointsQueryParams endpointsQueryParams;
	private TimeSeriesEndpointsResponse endpointsResponse;

	@Inject
	public TimeSeriesEndpointsRetriever(@Property(name = TIME_SERIES_JDBC_CONNECTION_NAME) String connectionname,
			@Property(name = TIME_SERIES_JDBC_WALLET_PATH) String walletpath,
			@Property(name = TIME_SERIES_JDBC_USERNAME) String username,
			@Property(name = TIME_SERIES_JDBC_PASSWORD) String password,
			@Property(name = TIME_SERIES_JDBC_DRIVER, defaultValue = "jdbc:oracle:thin:") String driver)
			throws SQLException, URISyntaxException, IOException {

		this.connectionname = connectionname;
		this.walletpath = walletpath;
		this.username = username;
		this.password = password;
		this.driver = driver;
		this.connectionstring = driver + DRIVER_URL_SEP + connectionname + DRIVER_TNS_NAME + walletpath;
		log.info("Using connection string " + connectionstring);
		try {
			dataSource = new OracleDataSource();
		} catch (SQLException e) {
			log.severe("SQLException creating OracleDataSource " + e.getLocalizedMessage());
			throw e;
		}
		dataSource.setURL(connectionstring);
		if (username.length() > 0) {
			log.info("Setting username");
			dataSource.setUser(username);
		}

		if (password.length() > 0) {
			log.info("Setting password");
			dataSource.setPassword(password);
		}
		try {
			loadEndpointsQueryParams();
		} catch (URISyntaxException e) {
			log.severe("URISyntaxException getting query params  " + e.getLocalizedMessage());
			throw e;
		} catch (SQLException e) {
			log.severe("SQLException getting query params " + e.getLocalizedMessage());
			throw e;
		} catch (IOException e) {
			log.severe("IOException getting query params " + e.getLocalizedMessage());
			throw e;
		}
	}

	public TimeSeriesEndpointsQueryParams getQueryParams() {
		if (endpointsQueryParams == null) {
			try {
				loadEndpointsQueryParams();
			} catch (URISyntaxException | SQLException | IOException e) {
				log.severe("Problem getting the query params " + e.getLocalizedMessage());
				return null;
			}
		}
		return endpointsQueryParams;
	}

	private void loadEndpointsQueryParams()
			throws URISyntaxException, SQLException, IOException, TimeSeriesEndpointsNothingRetrievedException {
		// try to request the endpoints form the database, micronaut will handle the
		// connection side of things
		try (Connection connection = dataSource.getConnection();
				Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(TIME_SERIES_ENDPOINTS_QUERY)) {
			Clob endpointsClob = rs.getClob("endpoints");
			if (endpointsClob == null) {
				throw new TimeSeriesEndpointsNothingRetrievedException("Retrieved clob is null");
			}
			String endpoints = endpointsClob.getSubString(1, (int) endpointsClob.length());
			endpointsResponse = mapper.readValue(endpoints, TimeSeriesEndpointsResponse.class);
		}
		// let's try and get the params from these
		endpointsQueryParams = TimeSeriesEndpointsQueryParams.builder()
				.metricsQueryX(endpointsResponse.getOtlp().getQueryParam("x"))
				.metricsQueryY(endpointsResponse.getOtlp().getQueryParam("y"))
				.oauthQueryX(endpointsResponse.getToken().getQueryParam("x"))
				.oauthQueryY(endpointsResponse.getToken().getQueryParam("y")).build();
		log.info("Endpoint query params " + endpointsQueryParams);
	}

	@EventListener
	public void onStartup(StartupEvent event) {
		log.info("Startup event received for TimeSeriesEndpointsRetriever, params are " + getQueryParams());
	}
}
