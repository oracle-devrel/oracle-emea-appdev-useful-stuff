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

package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.outputs.filewriter;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;

import io.micronaut.http.MediaType;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Serdeable
public class RawDataFileVersion {

	private String digitalTwinInstanceDisplayName;
	private String endpoint;
	// we don't want to dump raw text
	@ToString.Exclude
	private byte content[];
	private String contentType;
	private String timeReceived;

	public String getContentString() {
		if (getMediaType().isTextBased()) {
			return new String(content);
		} else {
			return "Blob data, non next based content type of " + content.length + " bytes";
		}
	}

	public MediaType getMediaType() {
		if ((contentType == null) || contentType.isEmpty()) {
			return MediaType.TEXT_PLAIN_TYPE;
		}
		return MediaType.of(contentType);
	}

	// build our version, but replace the instance id with the instance display
	// name, that is (hopefully) portable (if it's been set)
	public static RawDataFileVersion buildFrom(RawData input, String instanceDisplayName) {
		return RawDataFileVersion.builder().digitalTwinInstanceDisplayName(instanceDisplayName)
				.timeReceived(input.getTimeReceived()).content(input.getContent()).contentType(input.getContentType())
				.build();
	}

	// build the RawData version from our input
	public static RawData buildTo(RawDataFileVersion input, String instanceId) {
		return RawData.builder().digitalTwinInstanceId(instanceId).timeReceived(input.getTimeReceived())
				.contentType(input.getContentType()).content(input.getContent()).build();
	}
}
