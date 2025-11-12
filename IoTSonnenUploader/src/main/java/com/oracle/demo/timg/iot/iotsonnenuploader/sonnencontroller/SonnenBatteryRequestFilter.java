package com.oracle.demo.timg.iot.iotsonnenuploader.sonnencontroller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@ClientFilter(patterns = "/api/**")
@Requires(property = SonnenBatteryHttpClientSettings.PREFIX + ".authToken")
@Log
public class SonnenBatteryRequestFilter {
	public final static String HEADER_AUTH_TOKEN = "Auth-Token";
	private final SonnenBatteryHttpClientSettings clientSettings;

	@Inject
	public SonnenBatteryRequestFilter(SonnenBatteryHttpClientSettings clientSettings) {
		this.clientSettings = clientSettings;
	}

	@RequestFilter
	public void doFilter(MutableHttpRequest<?> request) {
		log.finer("Adding header " + HEADER_AUTH_TOKEN);
		request.getHeaders().add(HEADER_AUTH_TOKEN, clientSettings.getAuthToken());
	}
}