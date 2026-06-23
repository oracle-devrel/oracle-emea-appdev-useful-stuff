package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rawdata;

import static io.micronaut.http.HttpHeaders.USER_AGENT;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import jakarta.ws.rs.PathParam;

// needs the credentials
@Requires(property = RawDataIoTOutputHttpClientSettings.PREFIX + ".username")
@Requires(property = RawDataIoTOutputHttpClientSettings.PREFIX + ".password")
@Client(id = "iotoutputhttpclient", path = "${messagehandler.output.rawdata.iotoutputhttpclient:/api/v1/iotdata}")
@Header(name = USER_AGENT, value = "Micronaut HTTP Client")
public interface RawDataIoTOutputHttpClient {
	@Post(value = "/rawdata/string/{digitaltwinid}/{endpoint}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public boolean postRawDataAsString(@PathParam("digitaltwinid") String digitaltwinid,
			@PathParam("endpoint") String endpoint, @PathParam("timestamp") String timestamp, @Body String content);

	@Post(value = "/rawdata/base64/{digitaltwinid}/{endpoint}/{timestamp}", consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
	public boolean postRawDataAsBase64(@PathParam("digitaltwinid") String digitaltwinid,
			@PathParam("endpoint") String endpoint, @PathParam("timestamp") String timestamp,
			@Body String base64content);
}
