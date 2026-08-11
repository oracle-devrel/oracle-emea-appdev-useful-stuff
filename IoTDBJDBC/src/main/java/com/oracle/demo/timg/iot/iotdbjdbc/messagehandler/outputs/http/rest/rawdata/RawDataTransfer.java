package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.rawdata;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class RawDataTransfer {
	private String digitalTwinInstanceId;
	private String endpoint;
	// we don't want to dump raw text
	@ToString.Exclude
	private byte content[];
	private String contentType;
	private String timeReceived;

	public static RawDataTransfer buildRawDataTransfer(RawData input) {
		return RawDataTransfer.builder().digitalTwinInstanceId(input.getDigitalTwinInstanceId())
				.endpoint(input.getEndpoint()).content(input.getContent()).contentType(input.getContentType())
				.timeReceived(input.getTimeReceived()).build();
	}
}
