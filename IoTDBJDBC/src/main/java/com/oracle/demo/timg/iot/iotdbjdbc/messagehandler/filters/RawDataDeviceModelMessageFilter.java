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

import java.sql.PreparedStatement;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.oci.DBConnectionSupplier;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.rawdata.devicemodelfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.rawdata.devicemodelfilter.order")
@Requires(property = "messagehandler.filter.rawdata.devicemodelfilter.modelname")
@Requires(property = "iotdatacache.schemaname")
@Log
public class RawDataDeviceModelMessageFilter extends DeviceModelMessageFilterCore implements RawDataMessageHandler {

	private PreparedStatement selectModelIdByInstanceIdPS;

	@Inject
	public RawDataDeviceModelMessageFilter(DBConnectionSupplier dbConnectionSupplier,
			@Property(name = "iotdatacache.schemaname") String schemaName,
			@Property(name = "messagehandler.filter.rawdata.devicemodelfilter.order") int order,
			@Property(name = "messagehandler.filter.rawdata.devicemodelfilter.modelname") @NotNull @NotBlank String modelName,
			@Property(name = "messagehandler.filter.rawdata.devicemodelfilter.preloadexistinginstances", defaultValue = "true") boolean preloadExisting,
			@Property(name = "messagehandler.filter.rawdata.devicemodelfilter.nullmodelidiserror", defaultValue = "false") boolean nullModelIdIsError) {
		super(dbConnectionSupplier, schemaName, order, modelName, preloadExisting, nullModelIdIsError);
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		if (doesIoTDataCoreMatchModel(input)) {
			RawData result[] = new RawData[1];
			result[0] = input;
			return result;
		} else {
			return new RawData[0];
		}
	}
}
