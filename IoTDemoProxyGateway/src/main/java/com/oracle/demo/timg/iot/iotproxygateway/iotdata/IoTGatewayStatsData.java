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
package com.oracle.demo.timg.iot.iotproxygateway.iotdata;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oracle.demo.timg.iot.iotproxygateway.gateway.GatewayStatsData;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Serdeable
@Builder
public class IoTGatewayStatsData {
	// get the UTC TZ once to speed things later
	@ToString.Exclude
	@JsonIgnore
	private final static ZoneId utcTz = ZoneId.of("UTC");
	// note that for Indirectly connected devices the timestamp must be within the
	// payload sub object as the current configuration of the gateway envelope means
	// that's all that's passed on to the indirectly connected devices
	// for the telemetry on the gateway itself then we can get to the envelope
	// attributes before the contentRoot is applied (if there is one and its not $)
	// so we can if we want we can have the timestamp at the outer (envelope) level
	// or within the payload,
	@Builder.Default
	@JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSSSXXX")
	private ZonedDateTime timestamp = ZonedDateTime.now(utcTz);
	private final String devicekey = null;
	private GatewayStatsData payload;
}
