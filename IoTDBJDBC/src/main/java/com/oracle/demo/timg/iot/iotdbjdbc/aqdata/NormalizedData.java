package com.oracle.demo.timg.iot.iotdbjdbc.aqdata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalizedData {
	private String digitalTwinInstanceId;
	private String contentPath;
	private String timeObserved;
	private String content;
	public static final String SQL_QUEUE_NAME = "normalized_data";
}
