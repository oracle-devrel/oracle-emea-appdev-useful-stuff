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
package com.oracle.demo.timg.iot.iotdbjdbc.oci;

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
	public OracleConnection getNewConnection(String switchToSchema) throws SQLException, Exception {
		log.fine("Getting access token");
		AccessToken accessToken = dbTokenRetriever.generateAccessToken();
		log.finer("Creating connection");
		OracleConnectionBuilder builder = dataSource.createConnectionBuilder();
		builder.accessToken(accessToken);
		OracleConnection connection = builder.build();
		log.finer("Created connection, turning off auto commit as this will be for reads");
		connection.setAutoCommit(false);
		log.finer("Auth commit disabled");

		if (switchToSchema != null) {
			log.fine("Switching connection current schema to " + switchToSchema);
			try (Statement st = connection.createStatement();
					ResultSet rsSchema = st.executeQuery("alter session set current_schema=" + switchToSchema)) {
			} catch (SQLException e) {
				log.severe("Problem connecting to DB " + e.getLocalizedMessage());
				throw e;
			}
			log.fine("Connection current schema set to " + switchToSchema);
		}
		return connection;
	}
}
