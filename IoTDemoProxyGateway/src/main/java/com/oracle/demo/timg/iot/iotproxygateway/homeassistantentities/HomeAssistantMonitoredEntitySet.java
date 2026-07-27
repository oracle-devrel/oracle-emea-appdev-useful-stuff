/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStats;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
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
public class HomeAssistantMonitoredEntitySet implements Runnable {
	// get the UTC TZ once to speed things later
	private final static ZoneId UTC_TZ = ZoneId.of("UTC");
	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX");
	private final static ZonedDateTime EPOCH_TIME = Instant.EPOCH.atZone(UTC_TZ);
	private final static HomeAssistantState EPOCH_HA_STATE = HomeAssistantState.builder().last_changed(EPOCH_TIME)
			.last_reported(EPOCH_TIME).last_updated(EPOCH_TIME).build();
	private String name;
	private Boolean doupload;
	private Duration initaldelay = Duration.ofSeconds(5);
	private Duration retrievalrate = Duration.ofSeconds(10);
	private String devicekey;
	private String endpoint;
	private TimestampMode timestampMode = TimestampMode.LATEST;
	private List<HomeAssistantMonitoredEntity> monitoredentities;
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
	private Map<String, HomeAssistantState> laststates = new HashMap<>();

	@Inject
	public HomeAssistantMonitoredEntitySet(@Parameter String name) {
		log.info("Monitored entity in constructor for " + name);
	}

	@PostConstruct
	void postConstruct() {
		log.fine("Configuring initial last states for monitored entity " + this.name);
		// make sure that all of the entities are valid
		String problemEntities = monitoredentities.stream().filter(entity -> entity.missingFields())
				.map(entity -> this.name + "/" + entity.getName()).collect(Collectors.joining(","));
		if ((problemEntities != null) && (problemEntities.length() > 0)) {
			log.warning("Entity set " + this.getName() + " has incomplete entities " + problemEntities
					+ ". They will be removed from processing");
			monitoredentities = monitoredentities.stream().filter(entity -> !entity.missingFields()).toList();
		}
		// for all of the states we are monitoring setup a last state entry so we have a
		// known good start
		monitoredentities.stream().forEach(entity -> laststates.put(entity.getName(), EPOCH_HA_STATE));
		log.fine("constructed monitored entity " + this);
	}

	@Override
	public void run() {
		if (monitoredentities.isEmpty()) {
			log.info("Monitored entity set " + name + " has no entities");
			return;
		}
		log.finer("Running monitored entity set " + name);
		Map<String, Object> payload = new HashMap<>();
		ZonedDateTime observationTime = processEntities(payload);
		if (payload.size() == 0) {
			log.finer("Monitored entity set " + name + "payload has no data to upload, returning");
			return;
		}
		// it shouldn't happen but just in case the observation time is null while there
		// are payload entries apply a suitable default
		if (observationTime == null) {
			observationTime = ZonedDateTime.now(UTC_TZ);
		}
		// add the timestamp
		payload.put(IoTEntityData.TIMESTAMP_FIELD_NAME, observationTime.format(formatter));
		try {
			log.finer("Uploading payload of HA state is " + payload);
			mqttUploadHandler.upload(payload, this);
		} catch (Exception e) {
			log.info("Monitored entity set " + name + "Exception getting a state or other actions "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * @param payload
	 */
	private ZonedDateTime processEntities(Map<String, Object> payload) {
		// for each of the entities we are monitoring process it
		// build a stream comparison to work out what the timeObserved should be
		// the processEntiry will provide the field we're looking at
		LongStream entityTimeAsLong = monitoredentities.stream().map(entity -> processEntity(payload, entity))
				.filter(zdt -> zdt != null).mapToLong(zdt -> {
					Instant instant = zdt.toInstant();
					return (Long) Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000L),
							instant.getNano() / 1_000);
				});
		// based on our comparison type calculate the actual microseconds value
		Long sortedTsOpt = switch (timestampMode) {
		case AVERAGE -> Math.round(entityTimeAsLong.average().orElse(0));
		case EARLIEST -> entityTimeAsLong.min().orElse(0);
		case LATEST -> entityTimeAsLong.max().orElse(0);
		};
		// if there is nothing there then it's reasonable to assume nothing was added
		if (sortedTsOpt == 0) {
			return null;
		}
		// try to work out that that means
		long seconds = Math.floorDiv(sortedTsOpt, 1_000_000L);
		long remainingMicros = Math.floorMod(sortedTsOpt, 1_000_000L);

		Instant instant = Instant.ofEpochSecond(seconds, remainingMicros * 1_000L);
		return instant.atZone(UTC_TZ);
	}

	private ZonedDateTime processEntity(Map<String, Object> payload, HomeAssistantMonitoredEntity entity) {
		// get the last state, we know it must be there, but just in case
		HomeAssistantState laststate = laststates.get(entity.getName());
		if (laststate == null) {
			laststate = createCurrentState(entity);
		}
		String stateString;
		try {
			stateString = homeAssistantClient.getState(entity.getEntityid());
		} catch (Exception e) {
			log.warning("Unable to get monitored entity " + this + " because " + e.getLocalizedMessage());
			gatewayStats.trackFailedHARetrieveCall();
			return null;
		}
		if (stateString == null) {
			log.warning("Returned state of monitored entity " + this + " is null");
			gatewayStats.trackFailedHARetrieveCall();
			return null;
		}
		log.finer(() -> "Returned state string is :" + stateString);
		HomeAssistantState state;
		try {
			state = mapper.readValue(stateString, HomeAssistantState.class);
		} catch (Exception e) {
			log.warning("Unable to de-serialize the state because " + e.getLocalizedMessage());
			gatewayStats.trackFailedHARetrieveCall();
			return null;
		}
		log.fine(() -> "Extracted state is " + state);
		gatewayStats.trackSucessfullHARetrieveCall();
		// make sure that we have the relevant times, even if we don't use them here
		// they may be needed on another pass through
		if (state.getLast_changed() == null) {
			state.setLast_changed(ZonedDateTime.now(ZoneOffset.UTC));
		}
		if (state.getLast_reported() == null) {
			state.setLast_reported(ZonedDateTime.now(ZoneOffset.UTC));
		}
		if (state.getLast_updated() == null) {
			state.setLast_updated(ZonedDateTime.now(ZoneOffset.UTC));
		}
		// do we flag to send the data or not ?
		// for now default to false
		boolean proceedToSend = false;
		switch (entity.getSendmode()) {
		case ALWAYS: {
			// Send the update each time we get data from home assistant
			proceedToSend = true;
			break;
		}
		case ON_REPORT: {
			log.finer("Checking if there is a report, retrieved states last report time is "
					+ state.getLastReportedAsString() + ", previous is " + laststate.getLastReportedAsString());
			// send when home assistant updates itself, this is regardless of if HA data has
			// actually changed
			if (state.getLast_reported().isAfter(laststate.getLast_reported())) {
				proceedToSend = true;
			}
			break;
		}
		case ON_CHANGE: {
			log.finer("Checking if there is a change, retrieved states last report time is "
					+ state.getLastChangedAsString() + ", previous is " + laststate.getLastChangedAsString());
			// send when home assistant updates itself, this is regardless of if HA data has
			// actually changed
			if (state.getLast_changed().isAfter(laststate.getLast_changed())) {
				proceedToSend = true;
			}
			break;
		}
		default:
			log.severe("Unsupported sendmode found for HomeAssistantMonitoredEntity " + entity
					+ ", cannot process state " + stateString);
			return null;
		}
		// switch the saved state so we have the current timestamp to compare to next
		// time we get an new state from HomeAssistant
		laststates.put(entity.getName(), state);
		// are we adding this to the upload ?
		if (proceedToSend) {
			if (entity.isDontsendifunavailable() && state.isStateUnavailableOrMissing()) {
				log.finer("donttsendifunavailable is true and state is null or unavailable");
				// the state is not set or unavailable, and we are not sending in this case so
				// don't send it
				return null;
			} else {
				String fieldName = entity.getIottype().getFieldName(entity);
				Object fieldValue = entity.getIottype().createObjectFrom(state);
				payload.put(fieldName, fieldValue);
				// we're sending, so return the last reported time, this will always be the
				// setting we want because it it's ALWAYS that's the best we have, if it's
				// ON)CHANGE then that will have been updated at the last report, and if it's
				// on_report that's theupdate time even if it's not chenaged
				return state.getLast_reported();
			}
		} else {
			// if the entity doesn't pass the comparisons then its time stamps should not be
			// part of the returned data
			return null;
		}
	}

	private HomeAssistantState createCurrentState(HomeAssistantMonitoredEntity entity) {
		ZonedDateTime currentTime = ZonedDateTime.now(UTC_TZ);
		HomeAssistantState currentState = HomeAssistantState.builder().last_changed(currentTime)
				.last_reported(currentTime).last_updated(currentTime).build();
		laststates.put(entity.getName(), currentState);
		return currentState;
	}
}