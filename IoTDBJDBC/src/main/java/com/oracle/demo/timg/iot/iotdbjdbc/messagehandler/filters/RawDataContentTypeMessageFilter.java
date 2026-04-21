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

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.rawdata.contenttype.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.rawdata.contenttype.order")
@Log
public class RawDataContentTypeMessageFilter implements RawDataMessageHandler {
	private final int order;
	private final String type;

	public RawDataContentTypeMessageFilter(
			@Property(name = "messagehandler.filter.rawdata.contenttype.order") int order,
			@Property(name = "messagehandler.filter.rawdata.contenttype.type") String type) {
		this.order = order;
		this.type = type;
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		log.finer(() -> "RawData is " + input);
		RawData results[];
		if (input.getContentType().equalsIgnoreCase(type)) {
			log.fine(() -> input.getContentType() + " is the same type as  " + type);
			results = new RawData[1];
			results[0] = input;
		} else {
			log.fine(() -> input.getContentType() + " is a different type than  " + type);
			results = new RawData[0];
		}
		return results;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public String getName() {
		return "Content type filter";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " will match type " + type;
	}

}
