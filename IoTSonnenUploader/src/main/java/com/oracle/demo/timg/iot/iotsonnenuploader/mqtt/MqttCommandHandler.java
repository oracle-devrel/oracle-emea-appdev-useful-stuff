package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.commanddata.CommandResponse;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mqtt.annotation.MqttSubscriber;
import io.micronaut.mqtt.annotation.Topic;
import io.micronaut.mqtt.annotation.v5.MqttProperties;
import io.micronaut.mqtt.annotation.v5.MqttProperty;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.java.Log;

@Log
@MqttSubscriber
@MqttProperties({ @MqttProperty(name = "username", value = "tims"),
		@MqttProperty(name = "password", value = "ExamplePassword") })
@Requires(property = MqttDeviceSettings.PREFIX + ".id")
public class MqttCommandHandler {
	@Inject
	public MqttCommandResponsePublisher responsePublisher;
	@Inject
	private ObjectMapper mapper;

	@ExecuteOn(TaskExecutors.IO)
	@Topic("house/sonnen/command/${" + MqttDeviceSettings.PREFIX + ".id}")
	public void receive(String command) throws IOException {
		if (command.length() == 0) {
			log.info("Monitor recieved zero length config");
		}
		log.info("Recieved command " + command);
		CommandResponse resp = CommandResponse.builder().cmdReceived(ZonedDateTime.now())
				.cmdActioned(ZonedDateTime.now().plusSeconds(5)).cmdLength(command.length()).cmdStatus(true)
				.cmdResponse("Completed command " + command).build();
		log.info("Sending response " + resp);
		String respString = mapper.writeValueAsString(resp);
		CompletableFuture<Void> publishResp = responsePublisher.publishCommandResponse(respString.getBytes());
		publishResp.thenRun(() -> log.info("Sent response " + respString));
	}
}
