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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.normalizeddata.timeseriesdb;

public class TimeSeriesDBProperties {
	public static final String TIME_SERIES_PROPERTY_PREFIX = "messagehandler.output.normalizeddata.timeseries";
	public static final String TIME_SERIES_PROPERTY_ENABLED = TIME_SERIES_PROPERTY_PREFIX + ".enabled";
	public static final String TIME_SERIES_PROPERTY_ORDER = TIME_SERIES_PROPERTY_PREFIX + ".order";
	public static final String TIME_SERIES_PROPERTY_SENT_DATA_IS_COMPLETED = TIME_SERIES_PROPERTY_PREFIX
			+ ".sentdataiscompleted";
	// things for the oauth token
	public static final String TIME_SERIES_PROPERTY_OAUTH = TIME_SERIES_PROPERTY_PREFIX + ".oauth";
	public static final String TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS = TIME_SERIES_PROPERTY_OAUTH + ".query";
	public static final String TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_X = TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS
			+ ".x";
	public static final String TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS_Y = TIME_SERIES_PROPERTY_OAUTH_QUERY_PARAMS
			+ ".y";
	public static final String TIME_SERIES_PROPERTY_OAUTH_PATH = TIME_SERIES_PROPERTY_OAUTH + ".path";
	public static final String TIME_SERIES_PROPERTY_OAUTH_RENEWAL_PREEMPT = TIME_SERIES_PROPERTY_OAUTH
			+ ".renewalpreempt";
	public static final String TIME_SERIES_PROPERTY_OAUTH_USERNAME = TIME_SERIES_PROPERTY_OAUTH + ".username";
	public static final String TIME_SERIES_PROPERTY_OAUTH_PASSWORD = TIME_SERIES_PROPERTY_OAUTH + ".password";
	public static final String TIME_SERIES_PROPERTY_OAUTH_TENANCY_OCID = TIME_SERIES_PROPERTY_OAUTH + ".tenancyocid";
	public static final String TIME_SERIES_PROPERTY_OAUTH_DATABASE_NAME = TIME_SERIES_PROPERTY_OAUTH + ".databasename";
	// these are the metrics endpoints
	public static final String TIME_SERIES_PROPERTY_METRICS = TIME_SERIES_PROPERTY_PREFIX + ".metrics";
	public static final String TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS = TIME_SERIES_PROPERTY_METRICS + ".query";
	public static final String TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS_X = TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS
			+ ".x";
	public static final String TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS_Y = TIME_SERIES_PROPERTY_METRICS_QUERY_PARAMS
			+ ".y";
	public static final String TIME_SERIES_PROPERTY_METRICS_PATH = TIME_SERIES_PROPERTY_METRICS + ".path";

	public static final String TIME_SERIES_PROPERTY_DEBUG = TIME_SERIES_PROPERTY_PREFIX + ".debug";
	public static final String TIME_SERIES_PROPERTY_DEBUG_OAUTH = TIME_SERIES_PROPERTY_DEBUG + ".oauth";
	public static final String TIME_SERIES_PROPERTY_DEBUG_NO_UPLOAD = TIME_SERIES_PROPERTY_DEBUG + ".noupload";
}
