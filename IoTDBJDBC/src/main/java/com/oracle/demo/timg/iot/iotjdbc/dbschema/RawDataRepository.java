package com.oracle.demo.timg.iot.iotjdbc.dbschema;

import java.util.List;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.ORACLE)
public interface RawDataRepository extends CrudRepository<RawData, RawDataId> {
	@Override
	@Query("""
			SELECT
			  DIGITAL_TWIN_INSTANCE_ID, ENDPOINT, CONTENT_TYPE, CONTENT, TIME_RECEIVED
			FROM
			  yieh4avra3mt2__iot.raw_data
			""")
	List<RawData> findAll();

	@Query("""
			SELECT
			  DIGITAL_TWIN_INSTANCE_ID, ENDPOINT, CONTENT_TYPE, CONTENT, TIME_RECEIVED
			FROM
			  yieh4avra3mt2__iot.raw_data
			WHERE
			  DIGITAL_TWIN_INSTANCE_ID = :digitalTwinInstanceId
			""")

	List<RawData> findByIdDigitalTwinInstanceId(String digitalTwinInstanceId);

	@Query("""
			SELECT
			  DIGITAL_TWIN_INSTANCE_ID, ENDPOINT, CONTENT_TYPE, CONTENT, TIME_RECEIVED
			FROM
			  yieh4avra3mt2__iot.raw_data
			WHERE
			  DIGITAL_TWIN_INSTANCE_ID = :digitalTwinInstanceId AND ENDPOINT = :endpoint
			""")

	List<RawData> findByIdDigitalTwinInstanceIdAndIdEndpoint(String digitalTwinInstanceId, String endpoint);
}