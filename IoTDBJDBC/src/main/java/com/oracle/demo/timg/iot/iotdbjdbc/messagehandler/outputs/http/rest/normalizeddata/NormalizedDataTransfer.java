package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class NormalizedDataTransfer {
	private String digitalTwinInstanceId;
	private String contentPath;
	private String contentType;
	private String content;
	private String timeObserved;

	public static NormalizedDataTransfer buildNormalizedDataTransfer(NormalizedData input) {
		return NormalizedDataTransfer.builder().digitalTwinInstanceId(input.getDigitalTwinInstanceId())
				.contentPath(input.getContentPath()).contentType(input.getContentType()).content(input.getContent())
				.timeObserved(input.getTimeObserved()).build();
	}
}
