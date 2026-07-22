package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.filewriter;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Serdeable
public abstract class IoTDataCoreFileVersion {
	private String digitalTwinInstanceDisplayName;
}
