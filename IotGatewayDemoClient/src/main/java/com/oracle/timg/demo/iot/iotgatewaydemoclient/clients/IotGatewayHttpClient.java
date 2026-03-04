package com.oracle.timg.demo.iot.iotgatewaydemoclient.clients;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client(id = "iotgatewayserver", path = "/data")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface IotGatewayHttpClient {

	@Post("/newevent/{sourceId}")
	@Consumes(value = MediaType.TEXT_PLAIN)
	public void processIncommingEvent(@PathVariable(name = "sourceId") String sourceId, @Body String payload);

}
