package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.requestfilters;

public class IoTOutputHttpClientCommonFilterSettings {
	public final static String PREFIX = "messagehandler.output.iotoutputhttprestclient.filtersettings";
	public final static String USERNAME_PROPERTY = PREFIX + ".username";
	public final static String PASSWORD_BASE64_PROPERTY = PREFIX + ".passwordbase64";
	public final static String UNAUTHENTICATED_FILTER_ENABLED_PROPERTY = PREFIX + ".unauthenticated.filter.enabled";
	public final static String AUTH_TYPE = PREFIX + ".authtype";
	public final static String OAUTH = PREFIX + ".oauth";

	public final static String OAUTH_PATH = OAUTH + ".path";
	public final static String OAUTH_CLIENT_ID = OAUTH + ".clientid";
	public final static String OAUTH_CLIENT_SECRET = OAUTH + ".clientsecret";
	public final static String OAUTH_GRANT_TYPE = OAUTH + ".granttype";
	public final static String OAUTH_CLIENT_SCOPE = OAUTH + ".scope";

}