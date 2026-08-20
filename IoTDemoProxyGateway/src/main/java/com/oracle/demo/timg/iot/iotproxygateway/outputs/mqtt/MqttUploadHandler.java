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
package com.oracle.demo.timg.iot.iotproxygateway.outputs.mqtt;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.oracle.demo.timg.iot.iotproxygateway.PropertyNames;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.gateway.GatewayStatsTrackingData;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantEntityUploadHandler;
import com.oracle.demo.timg.iot.iotproxygateway.inputs.homeassistant.HomeAssistantMonitoredEntitySet;
import com.oracle.demo.timg.iot.iotproxygateway.iotdata.IoTEntityData;
import com.oracle.demo.timg.iot.iotproxygateway.outputs.OperatingModeOutput;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.ToString;
import lombok.extern.java.Log;

@Requires(property = PropertyNames.OPERATING_MODE_OUTPUT, value = "MQTT", defaultValue = "MQTT")
@Singleton
@Log
public class MqttUploadHandler implements HomeAssistantEntityUploadHandler {
	@Inject
	private GatewayStatsTrackingData gatewayStats;
	// private GatewayStats gatewayStats;
	private final String topicBase;
	@ToString.Exclude
	private final MqttHomeAssistantEntityPublisher mqttHomeAssistantEntityPublisher;
	@ToString.Exclude
	private final ObjectMapper mapper;

	@Inject
	public MqttUploadHandler(Optional<MqttHomeAssistantEntityPublisher> mqttHomeAssistantEntityPublisherOptional,
			ObjectMapper mapper,
			@Property(name = PropertyNames.GATEWAY_BASE_ENDPOINT, defaultValue = "house/homeassistant") String endpointBase,
			@Property(name = PropertyNames.GATEWAY_ENTITIES_ENDPOINT, defaultValue = "entities") String endpointEntities) {
		log.info("Constructing MqttUploadHandler");
		if (mqttHomeAssistantEntityPublisherOptional.isEmpty()) {
			log.info("Uploads to MQTT are turned off, this is probabaly a programming bug see property "
					+ PropertyNames.OPERATING_MODE_OUTPUT + " which could be one of " + OperatingModeOutput.values());
			mqttHomeAssistantEntityPublisher = null;
		} else {
			this.mqttHomeAssistantEntityPublisher = mqttHomeAssistantEntityPublisherOptional.get();
		}
		this.mapper = mapper;
		this.topicBase = endpointBase + "/" + endpointEntities;
	}

	@PostConstruct
	void postConstruct() {
		log.info("mqtt entity uploader configued with topic prefix " + topicBase);
	}

	@Override
	public void upload(Map<String, Object> ioTCoreEvent, HomeAssistantMonitoredEntitySet entity) {
		IoTEntityData ioTEntityData = IoTEntityData.builder().devicekey(entity.getDevicekey()).payload(ioTCoreEvent)
				.build();
		String mappedToJson;
		try {
			mappedToJson = mapper.writeValueAsString(ioTEntityData);
		} catch (IOException e) {
			log.severe("Error converting to Json, this should not have happened, " + e.getLocalizedMessage());
			return;
		}
		String topic = topicBase + "/" + entity.getEndpoint();
		log.info("Sending to topic " + topic + ", mapped event as json is " + mappedToJson);
		// need to get the right topic details to match against the expected IoT topic

		// try and sent it
		if (entity.getDoupload()) {
			try {
				if (mqttHomeAssistantEntityPublisher != null) {
					mqttHomeAssistantEntityPublisher.publishHomeAssistantData(topic, mappedToJson);
					gatewayStats.trackSucessfullUploadCall();
				} else {
					log.info("No mqtt uploader set for entity " + entity.getName() + " for data " + mappedToJson);
				}
			} catch (Exception e) {
				log.severe("Exception uploading event (" + mappedToJson + ") to Iot, " + e.getLocalizedMessage());
				gatewayStats.trackFailedUploadCall();
			}
		} else {
			log.info("Uploads disabled for entity " + entity.getName() + " for data " + mappedToJson);
		}
	}
}
