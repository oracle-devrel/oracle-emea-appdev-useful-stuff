/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ENABLED, value = "true", defaultValue = "false")
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_USERNAME)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_PASSWORD)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_TENANCY_OCID)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_DATABASE_NAME)
@Requires(property = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_ORDER)
@Data
// flag as jackson serdable
@Serdeable
public class TimeSeriesDBCredentials {
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_USERNAME)
	@JsonProperty(value = "username")
	private String username;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_PASSWORD)
	@JsonProperty(value = "password")
	private String password;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_TENANCY_OCID)
	@JsonProperty(value = "tenant_name")
	private String tenancyOcid;
	@Property(name = TimeSeriesDBProperties.TIME_SERIES_PROPERTY_OAUTH_DATABASE_NAME)
	@JsonProperty(value = "database_name")
	private String databaseName;
}