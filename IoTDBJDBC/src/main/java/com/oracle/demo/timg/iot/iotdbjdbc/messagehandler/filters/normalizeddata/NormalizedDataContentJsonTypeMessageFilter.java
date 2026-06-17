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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.normalizeddata;

import java.util.Set;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.FindOutcomes;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;
import oracle.sql.json.OracleJsonValue.OracleJsonType;

@Singleton
@Requires(property = "messagehandler.filter.normalizeddata.contentjsontypefilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.normalizeddata.contentjsontypefilter.order")
@Log
public class NormalizedDataContentJsonTypeMessageFilter implements NormalizedDataMessageHandler {

	private final int order;
	private final Set<OracleJsonType> contentTypes;
	private final FindOutcomes findOutcomes;

	/**
	 * the schema name is from the IoT service, basically the data cache user name
	 * like all handlers order is where in the list this is executed
	 * the modelNames are one or more names, they are loaded as a list. for  properties file that is done like this :
	 * messagehandler.filter.normalizeddata.contentjsontypefilter.contentype[0]=DECIMAL
     * messagehandler.filter.normalizeddata.contentjsontypefilter.contentype[1]=OBJECT
     * messagehandler.filter.normalizeddata.contentjsontypefilter.contentype[2]=DOUBLE
	 * 
	// @formatter:off
	 * For a yaml file the names are provided like this
	 * messagehandler:
	 *   filter:
	 *     normalizeddata:
	 *       contentjsontypefilter:
	 *         contentype:
	 *           - DECIMAL
	 *           - OBJECT
	 *           - DOUBLE
	 * 
	// @formatter:on
     * Note that content type names MUST be capable of being converted using the OracleJsonType.valueOf(name) so you should only use names that match that
	 * @param order where in the filter order this should be run
	 * @param contentTypes a list of the OracleJsonTYpe names (e.g. OBJECT, STRING, FLOAT etc.)
	 * @param filterOnMatches if true will accept the only content types specified, if false will reject content types specified
	 */
	@Inject
	public NormalizedDataContentJsonTypeMessageFilter(
			@Property(name = "messagehandler.filter.normalizeddata.contentjsontypefilter.order") int order,
			@Property(name = "messagehandler.filter.normalizeddata.contentjsontypefilter.contentype") Set<OracleJsonType> contentTypes,
			@Property(name = "messagehandler.filter.normalizeddata.contentjsontypefilter.findoutcome", defaultValue = "FOUND") FindOutcomes findOutcomes) {
		this.order = order;
		this.contentTypes = contentTypes;
		this.findOutcomes = findOutcomes;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		// is it in the types we were provided with ?
		boolean typeIsPresent = contentTypes.contains(input.getContentJsonType());
		// are we accepting or rejecting inputs of those types ?
		boolean match = switch (findOutcomes) {
		case FOUND -> typeIsPresent;
		case NOT_FOUND -> !typeIsPresent;
		};
		// if it passes then hand it on, otherwise don't
		NormalizedData results[];
		if (match) {
			results = new NormalizedData[1];
			results[0] = input;
		} else {
			results = new NormalizedData[0];
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
		return getName() + " order " + getOrder() + ", " + contentTypes + " (" + contentTypes.size()
				+ " elements), findoutcomes is " + findOutcomes;
	}
}
