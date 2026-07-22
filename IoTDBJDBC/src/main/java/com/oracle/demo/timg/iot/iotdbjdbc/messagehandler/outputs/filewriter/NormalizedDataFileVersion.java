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

import java.io.StringReader;
import java.io.StringWriter;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;

@Log
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
public class NormalizedDataFileVersion {
	private final static OracleJsonFactory factory = new OracleJsonFactory();
	private String digitalTwinInstanceDisplayName;
	private String contentPath;
	private String timeObserved;
	private String contentType;
	private String content;
	private String contentJsonValue;
	private OracleJsonType contentJsonType;

	// build our version, but replace the instance id with the instance display
	// name, that is (hopefully) portable (if it's been set)
	public static NormalizedDataFileVersion buildFrom(NormalizedData input, String instanceDisplayName) {
		StringWriter contentOuputWriter = new StringWriter();
		OracleJsonGenerator oracleJsonGenerator = NormalizedDataFileVersion.factory
				.createJsonTextGenerator(contentOuputWriter);
		oracleJsonGenerator.write(input.getContentJsonValue()).close();
		return NormalizedDataFileVersion.builder().digitalTwinInstanceDisplayName(instanceDisplayName)
				.contentPath(input.getContentPath()).timeObserved(input.getTimeObserved())
				.contentType(input.getContentType()).content(input.getContent())
				.contentJsonValue(contentOuputWriter.toString()).contentJsonType(input.getContentJsonType()).build();
	}

	// build the NormalizedData version from our input

	public NormalizedData buildTo(String instanceId) {
		return NormalizedDataFileVersion.buildTo(this, instanceId);
	}

	public static NormalizedData buildTo(NormalizedDataFileVersion input, String instanceId) {
		StringReader contentInputReader = new StringReader(input.getContentJsonValue());
		OracleJsonParser oracleJsonParser = NormalizedDataFileVersion.factory.createJsonTextParser(contentInputReader);
		OracleJsonValue oracleJsonValue = oracleJsonParser.getValue();
		return NormalizedData.builder().digitalTwinInstanceId(instanceId).contentPath(input.getContentPath())
				.timeObserved(input.getTimeObserved()).contentType(input.getContentType()).content(input.getContent())
				.contentJsonValue(oracleJsonValue).contentJsonType(input.getContentJsonType()).build();
	}
}
