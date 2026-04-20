package com.oracle.demo.timg.iot.iotdbjdbc.aqdata;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class IoTDataCore {
	private String digitalTwinInstanceId;
}
