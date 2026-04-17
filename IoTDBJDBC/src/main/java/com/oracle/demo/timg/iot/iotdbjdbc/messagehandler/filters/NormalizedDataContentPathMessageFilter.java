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

import java.util.regex.Pattern;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "normalizeddata.handler.contentpathfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "normalizeddata.handler.contentpathfilter.order")
@Log
public class NormalizedDataContentPathMessageFilter implements NormalizedDataMessageHandler {
	private final int order;
	private final String regexpPattern;
	private final boolean caseInsensitive;
	private final FindOutcomes findOutcomes;
	private final Pattern pattern;

	public NormalizedDataContentPathMessageFilter(
			@Property(name = "normalizeddata.handler.contentpathfilter.order") int order,
			@Property(name = "normalizeddata.handler.contentpathfilter.regexp") String regexpPattern,
			@Property(name = "normalizeddata.handler.contentpathfilter.caseinsensitive", defaultValue = "false") boolean caseInsensitive,
			@Property(name = "normalizeddata.handler.contentpathfilter.findoutcome", defaultValue = "FOUND") FindOutcomes findOutcomes) {
		this.order = order;
		this.regexpPattern = regexpPattern;
		this.caseInsensitive = caseInsensitive;
		int flags = 0;
		if (caseInsensitive) {
			flags |= Pattern.CASE_INSENSITIVE;
		}
		this.pattern = Pattern.compile(regexpPattern, flags);
		this.findOutcomes = findOutcomes;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.finer("NormalizedData is " + input);
		NormalizedData results[];
		// are we acting as a terminator or a step in the process ?
		boolean match = switch (findOutcomes) {
		case FOUND -> pattern.matcher(input.getContentPath()).find();
		case NOT_FOUND -> !pattern.matcher(input.getContentPath()).find();
		};
		if (match) {
			log.fine(findOutcomes + " is " + match + " for pattern " + regexpPattern + " case insensitive "
					+ caseInsensitive + " in content path " + input);
			results = new NormalizedData[1];
			results[0] = input;
		} else {
			log.fine(findOutcomes + " is " + match + " for pattern " + regexpPattern + " case insensitive "
					+ caseInsensitive + "in content path " + input);
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
		return "Content path filter";
	}

	@Override
	public String getConfig() {
		return getName() + " order " + getOrder() + " will match " + regexpPattern + ", caseInsensitive is "
				+ caseInsensitive + ", findoutcomes is " + findOutcomes;
	}

}
