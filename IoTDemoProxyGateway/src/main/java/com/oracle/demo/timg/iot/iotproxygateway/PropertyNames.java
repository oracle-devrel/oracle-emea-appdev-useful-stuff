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
package com.oracle.demo.timg.iot.iotproxygateway;

public class PropertyNames {

	public final static String RECORD_CLIENT_ENABLED = "record.enabled";
	public final static String RECORD_OUTPUT_FILE = "record.output.file";
	public final static String RECORD_OUTPUT_FILE_DEFAULT = "recordeddata.txt";
	public final static String RECORD_OUTPUT_DIRECTORY = "record.output.directory";
	public final static String RECORD_OUTPUT_DIRECTORY_DEFAULT = "saved";
	public final static String RECORD_OUTPUT_PREFIX_WITH_DTG = "record.output.prefixwithdtg";
	public final static String RECORD_DURATION = "record.duration";
	public final static String RECORD_EXIT_AFTER_RECORDING_STOP = "record.exitafterrecordingstop";

	public final static String MQTT_CLIENT_UPLOAD_ENABLED = "mqtt.client.enabled";
	public final static String MQTT_CLIENT_DEVICE_ID = "mqtt.client.client-id";
	public final static String MQTT_CLIENT_USERNAME = "mqtt.client.user-name";
	public final static String MQTT_CLIENT_PASSWORD = "mqtt.client.password";
	public final static String MQTT_CLIENT_SERVER_URI = "mqtt.client.server-uri";

	public final static String GATEWAY = "gateway";
	public final static String GATEWAY_IDENTITY = GATEWAY + ".identity";
	public final static String GATEWAY_DEVICE_NAME = GATEWAY_IDENTITY + ".name";
	public final static String GATEWAY_DEVICE_KEY = GATEWAY_IDENTITY + ".devicekey";
	public final static String GATEWAY_ENDPOINT = GATEWAY + ".endpoint";
	public final static String GATEWAY_BASE_ENDPOINT = GATEWAY_ENDPOINT + ".base";
	public final static String GATEWAY_STATS_ENDPOINT = GATEWAY_ENDPOINT + ".gatewaystats";
	public final static String GATEWAY_CONFIG_ENDPOINT = GATEWAY_ENDPOINT + ".gatewayconfig";
	public final static String GATEWAY_ENTITIES_ENDPOINT = GATEWAY_ENDPOINT + ".entities";

	public final static String GATEWAY_CONFIG = GATEWAY + ".config";
	public final static String GATEWAY_CONFIG_PUBLISH_ENABLED = GATEWAY_CONFIG + ".enabled";
	public final static String GATEWAY_CONFIG_PUBLISH_RATE = GATEWAY_CONFIG + ".publishrate";
	public final static String GATEWAY_CONFIG_INITIAL_DELAY = GATEWAY_CONFIG + ".initialdelay";
	public final static String GATEWAY_STATS = GATEWAY + ".stats";
	public final static String GATEWAY_STATS_PUBLISH_ENABLED = GATEWAY_STATS + ".enabled";
	public final static String GATEWAY_STATS_PUBLISH_RATE = GATEWAY_STATS + ".publishrate";
	public final static String GATEWAY_STATS_INITIAL_DELAY = GATEWAY_STATS + ".initialdelay";
	public final static String GATEWAY_STATS_SUCESSFULL_RETRIEVE_WINDOW = GATEWAY_STATS + ".sucessfullretrievewindow";
	public final static String GATEWAY_STATS_FAILED_RETRIEVE_WINDOW = GATEWAY_STATS + ".failedretrievewindow";
	public final static String GATEWAY_STATS_SUCESSFULL_UPLOAD_WINDOW = GATEWAY_STATS + ".sucessfulluploadwindow";
	public final static String GATEWAY_STATS_FAILED_UPLOAD_WINDOW = GATEWAY_STATS + ".faileduploadwindow";

	public final static String HOME_ASSISTANT = "home-assistant";
	public final static String HOME_ASSISTANT_MONITORED_ENTITIES_LIST = HOME_ASSISTANT + ".monitored-entities";
	public final static String HOME_ASSISTANT_API = HOME_ASSISTANT + ".api";
	public final static String HOME_ASSISTANT_API_AUTH_TOKEN = HOME_ASSISTANT_API + ".auth_token";
	public final static String HOME_ASSISTANT_API_PROTOCOL = HOME_ASSISTANT_API + ".protocol";
	public final static String HOME_ASSISTANT_API_HOSTNAME = HOME_ASSISTANT_API + ".host";
	public final static String HOME_ASSISTANT_API_PORT = HOME_ASSISTANT_API + ".port";
	public final static String HOME_ASSISTANT_API_URL = HOME_ASSISTANT_API + ".url";
}
