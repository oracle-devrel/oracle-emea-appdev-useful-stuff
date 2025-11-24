/*Copyright (c) 2025 Oracle and/or its affiliates.

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
package com.oracle.demo.timg.iot.iotsonnenuploader.mqtt;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import com.oracle.demo.timg.iot.iotsonnenuploader.commanddata.CommandReceived;
import com.oracle.demo.timg.iot.iotsonnenuploader.commanddata.CommandResponse;
import com.oracle.demo.timg.iot.iotsonnenuploader.commanddata.CommandStatus;
import com.oracle.demo.timg.iot.iotsonnenuploader.devicesettings.DeviceSettings;
import com.oracle.demo.timg.iot.iotsonnenuploader.incommingdata.SonnenConfiguration;

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
@MqttProperties({ @MqttProperty(name = "username", value = "${device.id}"),
		@MqttProperty(name = "password", value = "ExamplePassword") })
@Requires(property = DeviceSettings.PREFIX + ".id")
public class MqttCommandHandler {
	@Inject
	public MqttCommandResponsePublisher responsePublisher;
	@Inject
	private ObjectMapper mapper;

	@ExecuteOn(TaskExecutors.IO)
	@Topic("house/sonnencommand/${" + DeviceSettings.PREFIX + ".id}")
	public void receive(CommandReceived command) throws IOException {
		ZonedDateTime cmdReceived = ZonedDateTime.now();
		log.info("Received command " + command);
		// process it
		CommandResponse resp;
		switch (command.getCommandIdentifier()) {
		case SET_PLACEHOLDER:
			// change the placeholder in the configuration
			SonnenConfiguration.PLACE_HOLDER_VALUE = command.getData();
			resp = CommandResponse.builder().cmdReceived(cmdReceived).cmdActioned(ZonedDateTime.now())
					.cmdStatus(CommandStatus.SUCEEDED)
					.cmdResponse("Set SonnenConfiguration status to " + command.getData()).build();
			break;
		default:
			resp = CommandResponse.builder().cmdReceived(cmdReceived).cmdActioned(ZonedDateTime.now())
					.cmdStatus(CommandStatus.UNKNOWN)
					.cmdResponse("Command with identifier " + command.getCommandIdentifier() + " is unknown").build();
			break;

		}
		log.info("Sending command response " + resp);
		CompletableFuture<Void> publishResp = responsePublisher.publishCommandResponse(resp);
		publishResp.thenRun(() -> log.info("Sent command response " + resp));
	}
}
