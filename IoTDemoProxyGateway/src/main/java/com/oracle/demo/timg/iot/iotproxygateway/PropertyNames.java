package com.oracle.demo.timg.iot.iotproxygateway;

public class PropertyNames {
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
	public final static String GATEWAY_STATS_PUBLISH_ENABLED = GATEWAY_CONFIG + ".enabled";
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
