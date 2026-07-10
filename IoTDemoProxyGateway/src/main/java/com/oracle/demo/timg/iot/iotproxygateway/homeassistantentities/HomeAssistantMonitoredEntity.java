package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTCoreEvent;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTType;
import com.oracle.demo.timg.iot.iotproxygateway.mqtt.MqttUploadHandler;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.java.Log;

@Data
@NoArgsConstructor
@Log
@EachProperty(value = PropertyNames.HOME_ASSISTANT_MONITORED_ENTITIES_LIST, primary = "name", list = true)
public class HomeAssistantMonitoredEntity implements Runnable {
	private String name;
	private Boolean doupload;
	private Duration initaldelay = Duration.ofSeconds(5);
	private Duration retrievalrate = Duration.ofSeconds(10);
	private String entityid;
	private IoTType iottype;
	private String devicekey;
	private String endpoint;
	@ToString.Exclude
	@JsonIgnore
	private final static ZoneId utcTz = ZoneId.of("UTC");
	@ToString.Exclude
	@Inject
	private HomeAssistantHttpClient homeAssistantClient;
	@ToString.Exclude
	@Inject
	private MqttUploadHandler mqttUploadHandler;
	@ToString.Exclude
	@Inject
	private ObjectMapper mapper;
	@ToString.Exclude
	private HomeAssistantState laststate;

	@Inject
	public HomeAssistantMonitoredEntity(@Parameter String name) {
		log.info("Monitored entity in constructor for " + name);
		// we need a state to compare our updates to
		this.laststate = HomeAssistantState.builder().last_updated(Instant.EPOCH.atZone(ZoneOffset.UTC)).build();
	}

	@PostConstruct
	void postConstruct() {
		log.info("constructed monitoired entity " + this);
	}

	@Override
	public void run() {
		log.info("Running monitored entity " + name);
//		HomeAssistantState state;
		String stateString;
		try {
			stateString = homeAssistantClient.getState(entityid);
		} catch (Exception e) {
			log.warning("Unable to get monitored entity " + this + " because " + e.getLocalizedMessage());
			return;
		}
		if (stateString == null) {
			log.warning("Returned state of monitored entity " + this + " is null");
			return;
		}
		log.info("Returned state string is :" + stateString);
		HomeAssistantState state;
		try {
			state = mapper.readValue(stateString, HomeAssistantState.class);
		} catch (Exception e) {
			log.warning("Unable to de-serialize the state because " + e.getLocalizedMessage());
			return;
		}
		log.info("Extracted state is " + state);
		// has it been updated ? HA docs indicate that we can only rely on last_changed
		// but we actually want last updated as in many cases an update with the same
		// value is valid
		// the constructor forces the last_updated to be set, so we only need to think
		// about ensuring that the
		// incoming last updated is set (as the incoming one becomes the last state)
		if (state.getLast_updated() == null) {
			state.setLast_updated(ZonedDateTime.now(utcTz));
		}
		if (state.getLast_updated().isAfter(laststate.getLast_updated())) {
			log.info("HA Stats have been updated for " + name + " they are  " + state);
		} else {
			log.info("HA Stats have not been updated for " + name + " they are  " + state);
		}
		// switch the saved state so we have the current timestamp to compare to next
		// time we get an new state from HomeAssistant
		laststate = state;
		try {
			// convert it into the object we need
			IoTCoreEvent ioTCoreEvent = iottype.createEventFrom(state);
			log.info("converted HA state to core is " + ioTCoreEvent);
			mqttUploadHandler.upload(ioTCoreEvent, this);
		} catch (Exception e) {
			log.info("Exception testing timestamps or other actions " + e.getLocalizedMessage());
		}
	}
}