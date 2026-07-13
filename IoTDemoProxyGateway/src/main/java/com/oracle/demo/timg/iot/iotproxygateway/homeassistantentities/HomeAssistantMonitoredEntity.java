package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTType;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Data
@NoArgsConstructor
@Log
@Serdeable
public class HomeAssistantMonitoredEntity {
	private String name;
	private String entityid;
	private IoTType iottype;
	private String fieldname;
	private SendMode sendmode = SendMode.ALWAYS; // default to always as it's the broadest option
	private boolean dontsendifunavailable = true;
}
