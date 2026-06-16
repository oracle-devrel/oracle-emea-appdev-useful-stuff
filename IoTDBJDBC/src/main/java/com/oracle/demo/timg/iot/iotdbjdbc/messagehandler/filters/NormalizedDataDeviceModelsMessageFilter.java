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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters;

import java.util.List;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.normalizeddata.devicemodelsfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.normalizeddata.devicemodelsfilter.order")
@Requires(property = "messagehandler.filter.normalizeddata.devicemodelsfilter.modelname")
@Requires(property = "iotdatacache.schemaname")
@Log
public class NormalizedDataDeviceModelsMessageFilter extends DeviceModelMessageFilterCore
		implements NormalizedDataMessageHandler {
	/**
	 * the schema name is from the IoT service, basically the data cache user name
	 * like all handlers order is where in the list this is executed
	 * the modelNames are one or more names, they are loaded as a list. for  properties file that is done like this :
	 * messagehandler.filter.normalizeddata.devicemodelsfilter.modelnames[0]=battery
     * messagehandler.filter.normalizeddata.devicemodelsfilter.modelnames[1]=windows
     * messagehandler.filter.normalizeddata.devicemodelsfilter.modelnames[2]=doors
	 * 
	// @formatter:off
	 * For a yaml file the names are provided like this
	 * messagehandler:
	 *   filter:
	 *     normalizeddata:
	 *       devicemodelsfilter:
	 *         modelnames:
	 *           - battery
	 *           - windows
	 *           - doors
	 * 
	// @formatter:on
	 * @param schemaName
	 * @param order
	 * @param modelNames
	 * @param nullModelIdIsError
	 * @param caseInsensitive
	 */
	@Inject
	public NormalizedDataDeviceModelsMessageFilter(@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "messagehandler.filter.normalizeddata.devicemodelsfilter.order") int order,
			@Property(name = "messagehandler.filter.normalizeddata.devicemodelsfilter.modelnames") List<String> modelNames,
			@Property(name = "messagehandler.filter.normalizeddata.devicemodelsfilter.nullmodelidiserror", defaultValue = "true") boolean nullModelIdIsError,
			@Property(name = "messagehandler.filter.normalizeddata.devicemodelsfilter.caseinsensitive", defaultValue = "true") boolean caseInsensitive) {
		super(order, modelNames, caseInsensitive, true, "NormalizedDataDeviceModelListener");
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		if (doesIoTDataCoreMatchModel(input.getDigitalTwinInstanceId())) {
			NormalizedData result[] = new NormalizedData[1];
			result[0] = input;
			return result;
		} else {
			return new NormalizedData[0];
		}
	}
}
