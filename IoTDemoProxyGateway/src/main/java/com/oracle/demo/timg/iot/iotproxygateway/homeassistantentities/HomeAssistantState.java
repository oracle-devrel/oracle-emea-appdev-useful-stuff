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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Serdeable
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class HomeAssistantState {
	@JsonIgnore
	public final static String STATE_UNAVAILABLE = "unavailable";
	// get the UTC TZ once to speed things later
	@JsonIgnore
	private final static ZoneId UTCTZ = ZoneId.of("UTC");
	@JsonIgnore
	private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX");

	private String entity_id;
	private String state;
	private Map<String, Object> attributes;

	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_changed = ZonedDateTime.now(UTCTZ);
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_reported = ZonedDateTime.now(UTCTZ);
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXX")
	@Builder.Default
	private ZonedDateTime last_updated = ZonedDateTime.now(UTCTZ);

	private Map<String, Object> context;

	public boolean isStateUnavailableOrMissing() {
		return (state == null) || (state.equalsIgnoreCase(STATE_UNAVAILABLE));
	}

	public String getLastChangedAsString() {
		return last_changed.format(formatter);
	}

	public String getLastReportedAsString() {
		return last_reported.format(formatter);
	}

	public String getLastUpdatedAsString() {
		return last_updated.format(formatter);
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

	public final static String SWITCH_ON = "on";

	public Boolean getStateStringMatchesValue(@NonNull String defaultValue, @NonNull String valueToReturnTrue) {
		String stateValue = state;
		if (state.equalsIgnoreCase(STATE_UNAVAILABLE)) {
			stateValue = defaultValue;
		}
		return stateValue.equalsIgnoreCase(valueToReturnTrue);
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
