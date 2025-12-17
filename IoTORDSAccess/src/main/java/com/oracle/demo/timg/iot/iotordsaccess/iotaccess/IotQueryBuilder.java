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
package com.oracle.demo.timg.iot.iotordsaccess.iotaccess;

import com.oracle.demo.timg.iot.iotordsaccess.ords.query.BooleanAnd;
import com.oracle.demo.timg.iot.iotordsaccess.ords.query.OrderBy;
import com.oracle.demo.timg.iot.iotordsaccess.ords.query.OrderByDirection;
import com.oracle.demo.timg.iot.iotordsaccess.ords.query.Query;
import com.oracle.demo.timg.iot.iotordsaccess.ords.query.ValueEqualsString;

public class IotQueryBuilder {
	public final static String DT_INSTANCE_ID = "digital_twin_instance_id";
	public final static String CONTENT_PATH = "content_path";
	public final static String TIME_OBSERVED = "time_observed";

	public static String buildHistoryMostRecent(String digitalTwinInstanceId, String contentPath) {
		BooleanAnd band = new BooleanAnd();
		band.addValueTest(ValueEqualsString.builder().name(DT_INSTANCE_ID).value(digitalTwinInstanceId).build());
		band.addValueTest(ValueEqualsString.builder().name(CONTENT_PATH).value(contentPath).build());
		OrderBy orderBy = OrderBy.builder().name(TIME_OBSERVED).direction(OrderByDirection.DESC).build();
		return Query.builder().booleanTest(band).orderBy(orderBy).build().toQueryString();
	}

	public static String buildSnapshotData(String digitalTwinInstanceId) {
		BooleanAnd band = new BooleanAnd();
		band.addValueTest(ValueEqualsString.builder().name(DT_INSTANCE_ID).value(digitalTwinInstanceId).build());
		OrderBy orderBy = OrderBy.builder().name(CONTENT_PATH).direction(OrderByDirection.ASC).build();
		return Query.builder().booleanTest(band).orderBy(orderBy).build().toQueryString();
	}

	public static String buildSnapshotData(String digitalTwinInstanceId, String contentPath) {
		BooleanAnd band = new BooleanAnd();
		band.addValueTest(ValueEqualsString.builder().name(DT_INSTANCE_ID).value(digitalTwinInstanceId).build());
		band.addValueTest(ValueEqualsString.builder().name(CONTENT_PATH).value(contentPath).build());
		return Query.builder().booleanTest(band).build().toQueryString();
	}
}
