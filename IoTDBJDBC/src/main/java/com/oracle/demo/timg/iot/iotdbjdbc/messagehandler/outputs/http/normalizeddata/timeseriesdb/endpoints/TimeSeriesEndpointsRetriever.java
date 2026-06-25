package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.endpoints;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.TimeSeriesDBProperties;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Log
@Singleton
public class TimeSeriesEndpointsRetriever {
	public final static String TIME_SERIES_ENDPOINTS_QUERY = "select dbms_cloud_telemetry_ingest.get_ingestion_endpoints as endpoints from dual";
	private final DataSource dataSource;
	@Inject
	private ObjectMapper mapper;
	private TimeSeriesEndpointsQueryParams endpointsQueryParams;
	private TimeSeriesEndpointsResponse endpointsResponse;

	@Inject
	public TimeSeriesEndpointsRetriever(DataSource dataSource) {
		this.dataSource = dataSource;
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

	void loadEndpointsQueryParams() throws URISyntaxException, SQLException, IOException {
		// try to request the endpoints form the database, micronaut will handle the
		// connection side of things
		try (Connection connection = dataSource.getConnection();
				Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(TIME_SERIES_ENDPOINTS_QUERY)) {
			Clob endpointsClob = rs.getClob("endpoints");
			endpointsResponse = mapper.readValue(endpointsClob.getAsciiStream(), TimeSeriesEndpointsResponse.class);
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
