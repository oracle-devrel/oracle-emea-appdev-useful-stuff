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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.rawdata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.RawDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.FindOutcomes;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.rawdata.contenttypes.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.rawdata.contenttypes.order")
@Log
/**
 * filter on the content tyope of the raw data, for example application/json
 * (actually at the moment that's the only type supported if you want to do
 * normalization, but it could be useful to filter on non json contentTypes if
 * you want to handle the data process yourself elsewhere
 */
public class RawDataContentTypesMessageFilter implements RawDataMessageHandler {
	private final int order;
	private final Set<String> contentTypes;
	private boolean caseInsensitive;
	private final FindOutcomes findOutcomes;

	public RawDataContentTypesMessageFilter(
			@Property(name = "messagehandler.filter.rawdata.contenttypes.order") int order,
			@Property(name = "messagehandler.filter.rawdata.contenttypes.contenttypes") List<String> contentTypes,
			@Property(name = "messagehandler.filter.rawdata.contenttypes.caseinsensitive", defaultValue = "false") boolean caseInsensitive,
			@Property(name = "messagehandler.filter.rawdata.contenttypes.findoutcome", defaultValue = "FOUND") FindOutcomes findOutcomes) {
		this.order = order;
		this.contentTypes = caseInsensitive
				? contentTypes.stream().map(contentType -> contentType.toLowerCase()).collect(Collectors.toSet())
				: new HashSet<>(contentTypes);
		this.caseInsensitive = caseInsensitive;
		this.findOutcomes = findOutcomes;
	}

	@Override
	public RawData[] processRawData(RawData input) throws Exception {
		log.finer(() -> "RawData is " + input);
		String contentType = caseInsensitive ? input.getContentType().toLowerCase() : input.getContentType();
		// are we acting as a terminator or a step in the process ?
		boolean match = switch (findOutcomes) {
		case FOUND -> contentTypes.contains(contentType);
		case NOT_FOUND -> !contentTypes.contains(contentType);
		};
		RawData results[];
		if (match) {
			log.fine(() -> input.getContentType() + " is the same type as  " + contentType);
			results = new RawData[1];
			results[0] = input;
		} else {
			log.fine(() -> input.getContentType() + " is a different type than  " + contentType);
			results = new RawData[0];
		}
		log.fine(() -> findOutcomes + " is " + match + " for " + contentTypes + " case insensitive " + caseInsensitive
				+ " in content path " + input);
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
		return getName() + " order " + getOrder() + " will match " + contentTypes + " (" + contentTypes.size()
				+ " elements), caseInsensitive is " + caseInsensitive + ", findoutcomes is " + findOutcomes;
	}

}
