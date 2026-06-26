package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb.otlp;

import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
@Serdeable
public class MetricsData {
	private List<ResourceMetrics> resourceMetrics = new ArrayList<>();
}
