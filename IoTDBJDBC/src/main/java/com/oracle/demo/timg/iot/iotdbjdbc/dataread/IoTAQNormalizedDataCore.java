package com.oracle.demo.timg.iot.iotdbjdbc.dataread;

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
	public static final String SQL_QUEUE_NAME = "normalized_data";

	public IoTAQNormalizedDataCore(DBConnectionSupplier dbConnectionSupplier, String schemaName,
			int jdbcValidationTimeout, String aqsubscribername) throws SQLException, Exception {
		super(dbConnectionSupplier, schemaName, SQL_QUEUE_NAME, jdbcValidationTimeout, aqsubscribername);
	}

	/**
	 * @param message
	 * @throws SQLException
	 */
	protected void processAQMessage(AQMessage message) throws SQLException {
		NormalizedData normalizedData = convertToNormalizedData(message.getJSONPayload());
		log.info("Received " + normalizedData);
	}

	protected static NormalizedData convertToNormalizedData(OracleJsonDatum payloadDatum) throws SQLException {
		OracleJsonObject payload = (OracleJsonObject) payloadDatum.toJdbc();

		String ocid = payload.getString("digitalTwinInstanceId", "");
		String contentPath = payload.getString("contentPath", "");
		String timeObserved = payload.getString("timeObserved", "");
		OracleJsonValue valueJson = payload.get("value");
		String contentType = valueJson.getOracleJsonType().toString();
		// use content as that's the column name
		String content = (valueJson == null ? "" : valueJson.toString());
		return new NormalizedData(ocid, contentPath, timeObserved, contentType, content);
	}
}
