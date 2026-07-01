/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
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
	public final static String TIME_SERIES_JDBC_CONNECTION_NAME = TIME_SERIES_JDBC_PROPERTIES + ".connectionname";
	public final static String TIME_SERIES_JDBC_WALLET_PATH = TIME_SERIES_JDBC_PROPERTIES + ".walletpath";
	public final static String TIME_SERIES_JDBC_USERNAME = TIME_SERIES_JDBC_PROPERTIES + ".username";
	public final static String TIME_SERIES_JDBC_PASSWORD = TIME_SERIES_JDBC_PROPERTIES + ".password";
	public final static String TIME_SERIES_JDBC_DRIVER = TIME_SERIES_JDBC_PROPERTIES + ".driver";
	public final static String TIME_SERIES_ENDPOINTS_QUERY = "select dbms_cloud_telemetry_ingest.get_ingestion_endpoints as endpoints from dual";
	private final String connectionname, walletpath, username, password, driver, connectionstring;
	private final OracleDataSource dataSource;
	private final ObjectMapper mapper;
	private TimeSeriesEndpointsQueryParams endpointsQueryParams;
	private TimeSeriesEndpointsResponse endpointsResponse;

	@Inject
	public TimeSeriesEndpointsRetriever(ObjectMapper mapper,
			@Property(name = TIME_SERIES_JDBC_CONNECTION_NAME) String connectionname,
			@Property(name = TIME_SERIES_JDBC_WALLET_PATH) String walletpath,
			@Property(name = TIME_SERIES_JDBC_USERNAME) String username,
			@Property(name = TIME_SERIES_JDBC_PASSWORD) String password,
			@Property(name = TIME_SERIES_JDBC_DRIVER, defaultValue = "jdbc:oracle:thin:") String driver)
			throws SQLException, URISyntaxException, IOException, TimeSeriesEndpointsNothingRetrievedException,
			TimeSeriesEndpointsNoRowsRetrievedException {
		this.mapper = mapper;
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
		} catch (TimeSeriesEndpointsNothingRetrievedException e) {
			log.severe("Returned query params is null " + e.getLocalizedMessage());
			throw e;
		} catch (TimeSeriesEndpointsNoRowsRetrievedException e) {
			log.severe("No rows retrieved from query " + TIME_SERIES_ENDPOINTS_QUERY + ", " + e.getLocalizedMessage());
			throw e;
		}
	}

	public TimeSeriesEndpointsQueryParams getQueryParams() {
		if (endpointsQueryParams == null) {
			try {
				loadEndpointsQueryParams();
			} catch (URISyntaxException | SQLException | IOException | TimeSeriesEndpointsNothingRetrievedException
					| TimeSeriesEndpointsNoRowsRetrievedException e) {
				log.severe("Problem getting the query params " + e.getLocalizedMessage());
				return null;
			}
		}
		return endpointsQueryParams;
	}

	private void loadEndpointsQueryParams() throws URISyntaxException, SQLException, IOException,
			TimeSeriesEndpointsNothingRetrievedException, TimeSeriesEndpointsNoRowsRetrievedException {
		// try to request the endpoints form the database, micronaut will handle the
		// connection side of things
		try (Connection connection = dataSource.getConnection();
				Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(TIME_SERIES_ENDPOINTS_QUERY)) {
			if (rs.next()) {
				Clob endpointsClob = rs.getClob("endpoints");
				if (endpointsClob == null) {
					throw new TimeSeriesEndpointsNothingRetrievedException("Retrieved clob is null");
				}
				String endpoints = endpointsClob.getSubString(1, (int) endpointsClob.length());
				log.info("Retrieved endpoints data " + endpoints);
				endpointsResponse = mapper.readValue(endpoints, TimeSeriesEndpointsResponse.class);
			} else {
				throw new TimeSeriesEndpointsNoRowsRetrievedException(
						"No rows in the result set, has the DB been configured for the telemetry ?");
			}
		}
		log.finer("Build endpoints info is " + endpointsResponse);
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
