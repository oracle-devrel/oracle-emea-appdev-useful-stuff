package com.oracle.demo.timg.iot.iotproxygateway.outputs.recorder;

import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantEntityRetrieveStatus;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log
@Serdeable
@Builder
public class EntityInfo {
	@NonNull
	private HomeAssistantEntityRetrieveStatus retrieveStatus;
	@NonNull
	private String entitySetName;
	@NonNull
	private String entityid;
}
