package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import jakarta.ws.rs.PathParam;

@Client(id = "iotoutputhttpclient", path = "${messagehandler.output.iotoutputhttpclient:/api/v1/iotdata}")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface IoTOutputHttpClient {
	@Post(value = "/rawdata/string/{digitaltwinid}/{endpoint}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public boolean postRawDataAsString(@PathParam(value = "digitaltwinid") String digitalTwinId,
			@PathParam(value = "endpoint") String endpoint, @PathParam(value = "timestamp") String timestamp,
			@Body String content);

	@Post(value = "/rawdata/base64/{digitaltwinid}/{endpoint}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public boolean postRawDataAsBase64(@PathParam(value = "digitaltwinid") String digitalTwinId,
			@PathParam(value = "endpoint") String endpoint, @PathParam(value = "timestamp") String timestamp,
			@Body String base64content);
}
