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

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import io.micronaut.serde.annotation.SerdeImport;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.java.Log;
import oracle.sql.json.OracleJsonArray;
import oracle.sql.json.OracleJsonBinary;
import oracle.sql.json.OracleJsonDate;
import oracle.sql.json.OracleJsonDecimal;
import oracle.sql.json.OracleJsonDouble;
import oracle.sql.json.OracleJsonFloat;
import oracle.sql.json.OracleJsonIntervalDS;
import oracle.sql.json.OracleJsonIntervalYM;
import oracle.sql.json.OracleJsonNumber;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonString;
import oracle.sql.json.OracleJsonStructure;
import oracle.sql.json.OracleJsonTimestamp;
import oracle.sql.json.OracleJsonTimestampTZ;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;
import oracle.sql.json.OracleJsonVector;

@Log
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@SerdeImport(OracleJsonValue.class)
@SerdeImport(OracleJsonStructure.class)
@SerdeImport(OracleJsonObject.class)
@SerdeImport(OracleJsonArray.class)
@SerdeImport(OracleJsonString.class)
@SerdeImport(OracleJsonNumber.class)
@SerdeImport(OracleJsonDecimal.class)
@SerdeImport(OracleJsonDouble.class)
@SerdeImport(OracleJsonFloat.class)
@SerdeImport(OracleJsonBinary.class)
@SerdeImport(OracleJsonDate.class)
@SerdeImport(OracleJsonTimestamp.class)
@SerdeImport(OracleJsonTimestampTZ.class)
@SerdeImport(OracleJsonIntervalDS.class)
@SerdeImport(OracleJsonIntervalYM.class)
@SerdeImport(OracleJsonVector.class)
@SerdeImport(OracleJsonType.class)
@Serdeable
public class NormalizedDataFileVersion extends IoTDataCoreFileVersion {
	private String contentPath;
	private String timeObserved;
	private String contentType;
	private String content;
	private OracleJsonValue contentJsonValue;
	private OracleJsonType contentJsonType;

	// build our version, but replace the instance id with the instance display
	// name, that is (hopefully) portable (if it's been set)
	public static NormalizedDataFileVersion buildFrom(NormalizedData input, String instanceDisplayName) {
		return NormalizedDataFileVersion.builder().digitalTwinInstanceDisplayName(instanceDisplayName)
				.contentPath(input.getContentPath()).timeObserved(input.getTimeObserved())
				.contentType(input.getContentType()).content(input.getContent())
				.contentJsonValue(input.getContentJsonValue()).contentJsonType(input.getContentJsonType()).build();
	}

	// build the NormalizedData version from our input

	public NormalizedData buildTo(String instanceId) {
		return NormalizedDataFileVersion.buildTo(this, instanceId);
	}

	public static NormalizedData buildTo(NormalizedDataFileVersion input, String instanceId) {
		return NormalizedData.builder().digitalTwinInstanceId(instanceId).contentPath(input.getContentPath())
				.timeObserved(input.getTimeObserved()).contentType(input.getContentType()).content(input.getContent())
				.contentJsonValue(input.getContentJsonValue()).contentJsonType(input.getContentJsonType()).build();
	}
}
