package com.oracle.demo.timg.iot.iotdbjdbc.oci;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * This class is based on the DatabaseConnector by Philippe Vanhaesendonck but
 * using micronaut dependency injection
 */

public interface DBConnectionSupplier {

	/**
	 * gets a connection
	 * 
	 * @return
	 * @throws SQLException
	 */
	public default Connection getNewConnection() throws SQLException {
		return getNewConnection(null);
	}

	/**
	 * gets a connection, if switchToSchema is not null alters the connection
	 * current scheme to the specified schema
	 * 
	 * @return
	 * @throws SQLException
	 */
	public Connection getNewConnection(String switchToSchema) throws SQLException;
}
