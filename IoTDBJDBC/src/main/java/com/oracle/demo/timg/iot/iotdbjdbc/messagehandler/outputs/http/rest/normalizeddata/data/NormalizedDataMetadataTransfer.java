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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.http.rest.normalizeddata.transferdataobjects;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.iotdbutils.DeviceModelInstancesCache;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
@Log
public class NormalizedDataMetadataTransfer {
	private String externalKey;
	private String digitalTwinInstanceId;
	private String digitalTwinInstanceDisplayName;
	private String digitalTwinModelId;
	private String digitalTwinModelDisplayName;
	private NormalizedDataEventTransfer normalizedDataEventTransfer;

	public static NormalizedDataMetadataTransfer buildTransfer(NormalizedData input,
			DeviceModelInstancesCache deviceModelInstancesCache) {
		NormalizedDataEventTransfer normalizedDataEventTransfer = NormalizedDataEventTransfer.builder()
				.contentPath(input.getContentPath()).contentType(input.getContentType()).content(input.getContent())
				.timeObserved(input.getTimeObserved()).build();
		String instanceId = input.getDigitalTwinInstanceId();
		NormalizedDataMetadataTransferBuilder normalizedDataMetadataTransferBuilder = NormalizedDataMetadataTransfer
				.builder().digitalTwinInstanceId(instanceId).normalizedDataEventTransfer(normalizedDataEventTransfer);
		// try and work out what else is available, if we can find it add it
		try {
			normalizedDataMetadataTransferBuilder
					.externalKey(deviceModelInstancesCache.getExternalKeyByInstanceId(instanceId, false));
		} catch (Exception e) {
			log.info("Problem getting external key from instance Id " + instanceId);
		}
		try {
			normalizedDataMetadataTransferBuilder.digitalTwinInstanceDisplayName(
					deviceModelInstancesCache.getInstanceDisplayNameByInstanceId(instanceId, false));
		} catch (Exception e) {
			log.info("Problem getting display name from instance Id " + instanceId);
		}
		try {
			normalizedDataMetadataTransferBuilder
					.digitalTwinModelId(deviceModelInstancesCache.getModelIdByInstanceId(instanceId, false));
		} catch (Exception e) {
			log.info("Problem getting model Id from instance Id " + instanceId);
		}
		try {
			normalizedDataMetadataTransferBuilder
					.digitalTwinModelDisplayName(deviceModelInstancesCache.getModelNameByInstanceId(instanceId, false));
		} catch (Exception e) {
			log.info("Problem getting model display name from instance Id " + instanceId);
		}
		return normalizedDataMetadataTransferBuilder.build();
	}
}
