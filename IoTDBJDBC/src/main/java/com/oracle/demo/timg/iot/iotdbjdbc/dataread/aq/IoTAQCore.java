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

package com.oracle.demo.timg.iot.iotdbjdbc.dataread.aq;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Locale;

import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import lombok.extern.java.Log;
import oracle.jdbc.OracleConnection;

@Log

public abstract class IoTAQCore {
	private final static int ORACLE_AQ_ALREADY_SUBSCRIBED_ERROR_CODE = 24034;
	public final static String SCHEMA_SUFFIX = "__IOT";
	protected final DBConnectionSupplier dbConnectionSupplier;
	protected final String schemaName;
	protected final String aqsubscribername;
	protected final int jdbcValidationTimeout;

	protected final String queueName;
	protected OracleConnection connection;

	public IoTAQCore(DBConnectionSupplier dbConnectionSupplier, String schemaName, String queueName,
			int jdbcValidationTimeout, String aqsubscribername) throws SQLException, Exception {
		this.dbConnectionSupplier = dbConnectionSupplier;
		this.schemaName = schemaName + SCHEMA_SUFFIX;
		this.queueName = (schemaName + "." + queueName).toUpperCase(Locale.ROOT);
		this.jdbcValidationTimeout = jdbcValidationTimeout;
		this.aqsubscribername = aqsubscribername;
		connection = dbConnectionSupplier.getNewConnection(schemaName);
	}

	protected void addSubscriber(String rule) throws SQLException {
		log.info("Adding subscriber " + aqsubscribername);
		try (CallableStatement statement = connection.prepareCall("begin dbms_aqadm.add_subscriber("
				+ "queue_name => ?, " + "subscriber => ?, " + "rule => ?, " + "transformation => null, "
				+ "queue_to_queue => false, " + "delivery_mode => dbms_aqadm.persistent_or_buffered); end;")) {
			statement.setString(1, queueName);
			statement.setObject(2, createSubscriberStruct());
			if (rule == null) {
				statement.setNull(3, Types.VARCHAR);
			} else {
				statement.setString(3, rule);
			}
			try {
				statement.execute();
			} catch (SQLException e) {
				if (e.getErrorCode() == ORACLE_AQ_ALREADY_SUBSCRIBED_ERROR_CODE) {
					log.warning(() -> "Subscriber " + aqsubscribername + " is already subscribed to queue " + queueName
							+ ", continuing");
				} else {
					// was another error code
					log.severe("SQLException subscribing " + aqsubscribername + " to queue " + queueName + ", "
							+ e.getLocalizedMessage());
					throw e;
				}
			}
		}
		log.info("Added subscriber " + aqsubscribername);
	}

	protected void removeSubscriber() throws SQLException {
		log.info("Removing Subscriber " + aqsubscribername);
		try (CallableStatement statement = connection
				.prepareCall("begin dbms_aqadm.remove_subscriber(queue_name => ?, subscriber => ?); end;")) {
			statement.setString(1, queueName);
			statement.setObject(2, createSubscriberStruct());
			statement.execute();
		}
		log.info("Removed subscriber " + aqsubscribername);
	}

	private Struct createSubscriberStruct() throws SQLException {
		return connection.createStruct("SYS.AQ$_AGENT", new Object[] { aqsubscribername, null, 0 });
	}
}
