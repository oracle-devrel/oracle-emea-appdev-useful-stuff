package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import java.util.ArrayList;
import java.util.List;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class NumberDataPoint {
	private List<KeyValue> attributes = new ArrayList<>();
	// OTLP JSON represents fixed64/uint64 timestamp fields as decimal strings to
	// avoid precision loss in JSON clients.
	private String startTimeUnixNano;
	private String timeUnixNano;
	private Double asDouble;
	private Long asInt;
	private List<Exemplar> exemplars = new ArrayList<>();
	private Integer flags;

}
