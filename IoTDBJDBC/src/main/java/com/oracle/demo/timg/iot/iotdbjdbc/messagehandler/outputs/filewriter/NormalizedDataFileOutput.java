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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.filewriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = FileWriterProperties.NORMALIZED_DATA_FILE_OUTPUT_ENABLED, value = "true", defaultValue = "false")
@Log
public class NormalizedDataFileOutput implements NormalizedDataMessageHandler {
	private final ObjectMapper mapper;
	private final DeviceModelInstancesCache deviceModelInstancesCache;
	private final int order;
	private final Duration recordDuration;
	private final String outputFile;
	private BufferedWriter output;
	private Instant endTime;

	@Inject
	public NormalizedDataFileOutput(ObjectMapper mapper, DeviceModelInstancesCache deviceModelInstancesCache,
			@Property(name = FileWriterProperties.NORMALIZED_DATA_FILE_OUTPUT_ORDER) int order,
			@Property(name = FileWriterProperties.NORMALIZED_DATA_FILE_OUTPUT_DURATION, defaultValue = "1h") Duration recordDuration,
			@Property(name = FileWriterProperties.NORMALIZED_DATA_FILE_OUTPUT_TARGET_FILE) String outputFile)
			throws IOException {
		this.mapper = mapper;
		this.deviceModelInstancesCache = deviceModelInstancesCache;
		this.order = order;
		this.recordDuration = recordDuration;
		this.outputFile = outputFile;
	}

	@Override
	public void configure() throws Exception {
		this.output = new BufferedWriter(new FileWriter(outputFile));
		this.endTime = Instant.now().plus(recordDuration);
	}

	@Override
	public void unconfigure() throws Exception {
		this.output.close();
		this.output = null;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Normalized Data file output";
	}

	@Override
	public String getConfig() {
		return getName() + ", order " + order + " writing to " + outputFile + " for " + recordDuration;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		// whatever happens we just pass this on
		NormalizedData[] returnData = new NormalizedData[1];
		returnData[0] = input;
		// if the data
		if (Instant.now().isAfter(endTime)) {
			return returnData;
		}
		String deviceDisplayName = deviceModelInstancesCache
				.getInstanceDisplayNameByInstanceId(input.getDigitalTwinInstanceId(), true);
		log.info("Input NormalizedData " + input);
		NormalizedDataFileVersion dataFileVersion = NormalizedDataFileVersion.buildFrom(input, deviceDisplayName);
		log.info("Converted NormalizedDataFileVersion " + dataFileVersion);
		String outputString = mapper.writeValueAsString(dataFileVersion);
		log.info("Saving " + outputString);
		output.write(outputString);
		output.newLine();

		return returnData;
	}

}
