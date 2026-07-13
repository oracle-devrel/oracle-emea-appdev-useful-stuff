package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStats;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
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
	private String fieldname;
	private IoTType iottype;
	private String devicekey;
	private String endpoint;
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
	@Inject
	private GatewayStats gatewayStats;
	@ToString.Exclude
	private HomeAssistantState laststate;

	@Inject
	public HomeAssistantMonitoredEntity(@Parameter String name) {
		log.info("Monitored entity in constructor for " + name);
		// we need a state to compare our updates to
		ZonedDateTime initial = Instant.EPOCH.atZone(ZoneOffset.UTC);
		this.laststate = HomeAssistantState.builder().last_updated(initial).last_changed(initial).last_reported(initial)
				.build();
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
			gatewayStats.trackFailedHARetrieveCall();
			return;
		}
		if (stateString == null) {
			log.warning("Returned state of monitored entity " + this + " is null");
			gatewayStats.trackFailedHARetrieveCall();
			return;
		}
		log.info("Returned state string is :" + stateString);
		HomeAssistantState state;
		try {
			state = mapper.readValue(stateString, HomeAssistantState.class);
		} catch (Exception e) {
			log.warning("Unable to de-serialize the state because " + e.getLocalizedMessage());
			gatewayStats.trackFailedHARetrieveCall();
			return;
		}
		log.info("Extracted state is " + state);
		gatewayStats.trackSucessfullHARetrieveCall();
		// this section needs to be re-written to allow for multiple types of update
		// reasons, always, when changed, when value retrieved
		// has a new (or the same) value been retrieved for the entity ? HA docs
		// indicate that we can only rely on last_changed
		// but we actually want last updated as in many cases an update with the same
		// value is valid
		// the constructor forces the last_updated to be set, so we only need to think
		// about ensuring that the
		// incoming last updated is set (as the incoming one becomes the last state)
		if (state.getLast_reported() == null) {
			state.setLast_reported(ZonedDateTime.now(ZoneOffset.UTC));
		}
		if (state.getLast_reported().isAfter(laststate.getLast_reported())) {
			log.info("HA Stats have had been updated for " + name + " they are  " + state);
		} else {
			log.info("HA Stats have not been updated for " + name + " they are  " + state);
			return;
		}
		// switch the saved state so we have the current timestamp to compare to next
		// time we get an new state from HomeAssistant
		laststate = state;
		try {
			Map<String, Object> payload = new HashMap<>();
			// add the timestamp
			payload.put(IoTEntityData.TIMESTAMP_FIELD_NAME, state.getLastUpdatedAsString());
			String fieldName = iottype.getFieldName(this);
			Object fieldValue = iottype.createObjectFrom(state);
			payload.put(fieldName, fieldValue);
			// iottype.createEventFrom(state);
			// convert it into the object we need
			// IoTCoreEvent ioTCoreEvent = iottype.createEventFrom(state);
			log.info("Generated payload of HA state is " + payload);
			mqttUploadHandler.upload(payload, this);
		} catch (Exception e) {
			log.info("Exception testing timestamps or other actions " + e.getLocalizedMessage());
		}
	}
}