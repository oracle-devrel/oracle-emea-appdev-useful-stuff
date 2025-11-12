package com.oracle.demo.timg.iot.iotsonnenuploader.sonnencontroller;

import static io.micronaut.http.HttpHeaders.ACCEPT;
import static io.micronaut.http.HttpHeaders.USER_AGENT;

import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;
import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenStatus;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;

@Client(id = "sonnenbattery", path = "/api/v2")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
@Header(name = ACCEPT, value = "application/json")
@Requires(property = SonnenBatteryHttpClientSettings.PREFIX + ".authToken")
public interface SonnenBatteryClient {

	@Get("/configurations")
	public SonnenConfiguration fetchConfiguration();

	@Get("/status")
	public SonnenStatus fetchStatus();

}
