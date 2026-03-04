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
package com.oracle.timg.demo.iot.demogateway.eventdatatransformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;

@Singleton
@Requires(property = "gateway.eventdatatransformer.xmlinputtojsonoutput.enabled", value = "true", defaultValue = "false")
public class EventDataTransformerXmlInputToJsonOutput implements EventDataTransformer {
	@Property(name = "gateway.eventdatatransformer.xmlinputtojsonoutput.order", defaultValue = "20")
	@Getter
	private int order;
	@Getter
	private final String name = "Xml Input To Json Output";
	@Inject
	private XmlMapper xmlMapper;
	@Inject
	private ObjectMapper objectMapper;

	@Override
	public String reformatEventData(@NotBlank @NonNull String receivedEventData)
			throws EventDataIncommingFormatException, EventDataTransformException {
		JsonNode node;
		try {
			node = xmlMapper.readTree(receivedEventData);
		} catch (JsonProcessingException e) {
			throw new EventDataIncommingFormatException(
					"Can't parse the incomming XML data due to " + e.getLocalizedMessage(), e);
		}
		try {
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new EventDataTransformException(
					"Problem serializing the processed XML data due to " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String getConfig() {
		return "XML incomming to Json outgoing event data reformatter";
	}

}
