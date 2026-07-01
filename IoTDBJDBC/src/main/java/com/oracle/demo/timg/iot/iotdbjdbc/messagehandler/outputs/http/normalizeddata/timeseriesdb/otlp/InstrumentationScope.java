package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class InstrumentationScope {
	private String name;
	private String version;
	private List<KeyValue> attributes = new ArrayList<>();
	private Integer droppedAttributesCount;
}
