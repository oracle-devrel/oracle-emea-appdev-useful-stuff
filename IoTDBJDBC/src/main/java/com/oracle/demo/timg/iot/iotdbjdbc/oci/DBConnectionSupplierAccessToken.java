package com.oracle.demo.timg.iot.iotdbjdbc.oci;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.jdbc.AccessToken;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleConnectionBuilder;
import oracle.jdbc.pool.OracleDataSource;

/**
 * This class is based on the DatabaseConnector by Philippe Vanhaesendonck but
 * using micronaut dependency injection
 */

@Singleton
@Log
@Requires(property = "datasources.default.url")
@Requires(property = "iotdatacache.schemaname")
public class DBConnectionSupplierAccessToken implements DBConnectionSupplier {

	private final DBTokenRetriever dbTokenRetriever;

	public final static String DRIVER_URL_SEP = "@";
	private final String url;
	private final String username;
	private final String password;

	private final OracleDataSource dataSource;

	@Inject
	public DBConnectionSupplierAccessToken(DBTokenRetriever dbTokenRetriever,
			@Property(name = "datasources.default.url") String url,
			@Property(name = "datasources.default.driver", defaultValue = "jdbc:oracle:thin:") String driver,
			@Property(name = "datasources.default.username", defaultValue = "") String username,
			@Property(name = "datasources.default.password", defaultValue = "") String password) throws SQLException {

		this.dbTokenRetriever = dbTokenRetriever;
		this.url = url;
		this.username = username;
		this.password = password;
		dataSource = new OracleDataSource();
		dataSource.setURL(driver + DRIVER_URL_SEP + url);
		if (username.length() > 0) {
			log.info("Setting username");
			dataSource.setUser(username);
		}

		if (password.length() > 0) {
			log.info("Setting password");
			dataSource.setPassword(password);
		}

	}

	/**
	 * Gets an access token using the injected token receiver then gets a database
	 * connection with auto connect turned off, if switchToScheme is not null alters
	 * the connection to use switchToSchema as the default schema
	 */
	@Override
	public Connection getNewConnection(String switchToSchema) throws SQLException, Exception {
		log.info("Getting access token");
		AccessToken accessToken = dbTokenRetriever.generateAccessToken();
		log.info("Creating connection");
		OracleConnectionBuilder builder = dataSource.createConnectionBuilder();
		builder.accessToken(accessToken);
		OracleConnection connection = builder.build();
		connection.setAutoCommit(false);
		log.info("Connection created");

		if (switchToSchema != null) {

			log.info("Switching connection current schema to " + switchToSchema);
			try (Connection conn = dataSource.getConnection();
					Statement st = conn.createStatement();
					// for efficiency this should only be done when we get a new connection that has
					// not had it's current schema altered, but for now this is a simple approach
					ResultSet rsSchema = st.executeQuery("alter session set current_schema=" + switchToSchema)) {
			} catch (SQLException e) {
				log.severe("Problem connecting to DB " + e.getLocalizedMessage());
				throw e;
			}
			log.info("Connection current schema set to " + switchToSchema);
		}
		return connection;
	}
}
