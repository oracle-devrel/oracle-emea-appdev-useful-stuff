package com.oracle.demo.timg.iot.iotproxygateway.homeassistantentities;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Serdeable
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantState {
	@ToString.Exclude
	@JsonIgnore
	public final static String STATE_UNAVAILABLE = "unavailable";
	// get the UTC TZ once to speed things later
	@ToString.Exclude
	@JsonIgnore
	private final static ZoneId utcTz = ZoneId.of("UTC");
	private String entity_id;
	private String state;
	private Map<String, String> attributes;

	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_changed = ZonedDateTime.now(utcTz);
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_reported = ZonedDateTime.now(utcTz);
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_updated = ZonedDateTime.now(utcTz);

	private Map<String, String> context;

	public Long getLastChangedAsLong() {
		return last_changed.toInstant().toEpochMilli();
	}

	public Long getLastReportedAsLong() {
		return last_reported.toInstant().toEpochMilli();
	}

	public Long getLastUpdatedAsLong() {
		return last_updated.toInstant().toEpochMilli();
	}

	public Double getStateAsDouble(String defaultValue) {
		if (state.equalsIgnoreCase(STATE_UNAVAILABLE)) {
			return Double.valueOf(defaultValue);
		} else {
			return Double.valueOf(state);
		}
	}

	public Double getStateAsDouble() {
		return Double.valueOf(state);
	}

	public Integer getStateAsInteger(String defaultValue) {
		if (state.equalsIgnoreCase(STATE_UNAVAILABLE)) {
			return Integer.valueOf(defaultValue);
		} else {
			return Integer.valueOf(state);
		}
	}

	public Integer getStateAsInteger() {
		return Integer.valueOf(state);
	}

	public String getStateAsString() {
		return state;
	}

	public Boolean getStateAsBoolean(String defaultValue) {
		if (state.equalsIgnoreCase(STATE_UNAVAILABLE)) {
			return Boolean.valueOf(defaultValue);
		} else {
			return Boolean.valueOf(state);
		}
	}

	public Boolean getStateAsBoolean() {
		return Boolean.valueOf(state);
	}
}
