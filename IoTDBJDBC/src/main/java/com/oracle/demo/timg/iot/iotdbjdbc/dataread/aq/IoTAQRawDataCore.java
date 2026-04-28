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

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Locale;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import lombok.Getter;
import lombok.extern.java.Log;
import oracle.jdbc.aq.AQMessage;

@Log
public abstract class IoTAQRawDataCore extends IoTAQCore {
	private static final int DIGITAL_TWIN_INSTANCE_ID_COLUMN = 0;
	private static final int ENDPOINT_COLUMN = 1;
	private static final int CONTENT_COLUMN = 2;
	private static final int CONTENT_TYPE_COLUMN = 3;
	private static final int TIME_RECEIVED_COLUMN = 4;
	public static final String SQL_QUEUE_NAME = "raw_data_in";
	@Getter
	private final String payloadType;

	public IoTAQRawDataCore(DBConnectionSupplier dbConnectionSupplier, String schemaName, int jdbcValidationTimeout,
			String aqsubscribername) throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, SQL_QUEUE_NAME, jdbcValidationTimeout, aqsubscribername);
		this.payloadType = (super.getQueueName() + "_TYPE").toUpperCase(Locale.ROOT);
	}

	/**
	 * @param message
	 * @throws SQLException
	 */
	protected void outputAQMessage(AQMessage message) throws SQLException {
		RawData rawData;
		try {
			rawData = convertToRawData(message.getStructPayload().getAttributes());
		} catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}
		log.info("Received raw data " + rawData);
	}

	protected static RawData convertToRawData(Object[] attributes) throws ClassCastException, SQLException {
		// JDBC exposes the raw_data_in ADT payload as a Struct, so the values arrive
		// in the database type's declared attribute order:
		// DIGITAL_TWIN_INSTANCE_ID, ENDPOINT, CONTENT, CONTENT_TYPE, TIME_RECEIVED.
		// https://docs.oracle.com/en-us/iaas/Content/internet-of-things/iot-domain-database-schema.htm#queues__raw-data-queues
		String digitalTwinInstanceId = stringValue(attributes, DIGITAL_TWIN_INSTANCE_ID_COLUMN);
		String endpoint = stringValue(attributes, ENDPOINT_COLUMN);
		byte content[] = blobValue(attributes, CONTENT_COLUMN);
		String contentType = stringValue(attributes, CONTENT_TYPE_COLUMN);
		String timeReceived = stringValue(attributes, TIME_RECEIVED_COLUMN);
		return RawData.builder().digitalTwinInstanceId(digitalTwinInstanceId).endpoint(endpoint)
				.contentType(contentType).content(content).timeReceived(timeReceived).build();
	}

	private static byte[] blobValue(Object attributes[], int i) throws ClassCastException, SQLException {
		Object content = attributes[i];
		if (content instanceof Blob blob) {
			return blob.getBytes(1, (int) blob.length());
		} else {
			throw new ClassCastException("Expected a blob, but got " + content.getClass().getName());
		}
	}

	private static String stringValue(Object[] attributes, int index) {
		Object value = index < attributes.length ? attributes[index] : null;
		return value == null ? "" : value.toString();
	}
}
