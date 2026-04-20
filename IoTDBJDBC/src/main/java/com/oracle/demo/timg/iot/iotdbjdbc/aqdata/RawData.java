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

package com.oracle.demo.timg.iot.iotdbjdbc.aqdata;

import java.util.Arrays;
import java.util.HashSet;

import io.micronaut.http.MediaType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class RawData extends IoTDataCore {
	// there should be a way to use the MediaType.isTextBased here
	private final static HashSet<String> STRING_OUTPUT_TYPES = new HashSet<>(Arrays.asList(
			MediaType.APPLICATION_JSON.toLowerCase(), MediaType.APPLICATION_JSON_SCHEMA.toLowerCase(),
			MediaType.APPLICATION_XML.toLowerCase(), MediaType.TEXT_CSV.toLowerCase(),
			MediaType.TEXT_HTML.toLowerCase(), MediaType.TEXT_XML.toLowerCase(), MediaType.TEXT_JSON.toLowerCase(),
			MediaType.TEXT_MARKDOWN.toLowerCase(), MediaType.TEXT_PLAIN.toLowerCase()));
	private String endpoint;
	private byte content[];
	private String contentType;
	private String timeReceived;

	public String getContent() {
		if (MediaType.of(contentType).isTextBased()) {
			return new String(content);
		} else {
			return "Blob data, non next based content type of " + content.length + " bytes";
		}
	}
}
