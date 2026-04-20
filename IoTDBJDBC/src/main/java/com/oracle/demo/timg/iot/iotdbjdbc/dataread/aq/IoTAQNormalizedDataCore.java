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

import java.sql.SQLException;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import lombok.extern.java.Log;
import oracle.jdbc.aq.AQMessage;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

@Log
public abstract class IoTAQNormalizedDataCore extends IoTAQCore {
	private static final String VALUE_COLUMN_NAME = "value";
	private static final String TIME_OBSERVED_COLUMN_NAME = "timeObserved";
	private static final String CONTENT_PATH_COLUMN_NAME = "contentPath";
	private static final String DIGITAL_TWIN_INSTANCE_ID_COLUMN_NAME = "digitalTwinInstanceId";
	public static final String SQL_QUEUE_NAME = "normalized_data";

	public IoTAQNormalizedDataCore(DBConnectionSupplier dbConnectionSupplier, String schemaName,
			int jdbcValidationTimeout, String aqsubscribername) throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, SQL_QUEUE_NAME, jdbcValidationTimeout, aqsubscribername);
	}

	/**
	 * @param message
	 * @throws SQLException
	 */
	protected void outputAQMessage(AQMessage message) throws SQLException {
		NormalizedData normalizedData = convertToNormalizedData(message.getJSONPayload());
		log.info("Received " + normalizedData);
	}

	protected static NormalizedData convertToNormalizedData(OracleJsonDatum payloadDatum) throws SQLException {
		OracleJsonObject payload = (OracleJsonObject) payloadDatum.toJdbc();

		String digitalTwinInstanceId = payload.getString(DIGITAL_TWIN_INSTANCE_ID_COLUMN_NAME, "");
		String contentPath = payload.getString(CONTENT_PATH_COLUMN_NAME, "");
		String timeObserved = payload.getString(TIME_OBSERVED_COLUMN_NAME, "");
		OracleJsonValue valueJson = payload.get(VALUE_COLUMN_NAME);
		String contentType = valueJson.getOracleJsonType().toString();
		// use content as that's the column name
		String content = (valueJson == null ? "" : valueJson.toString());
		return NormalizedData.builder().digitalTwinInstanceId(digitalTwinInstanceId).contentPath(contentPath)
				.timeObserved(timeObserved).contentType(contentType).content(content).build();
	}
}
