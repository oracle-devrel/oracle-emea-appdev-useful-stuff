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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.NormalizedDataMessageHandler;
import com.oracle.demo.timg.iot.iotdbjdbc.messagehandler.filters.FindOutcomes;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Requires(property = "messagehandler.filter.normalizeddata.contentpathsfilter.enabled", value = "true", defaultValue = "false")
@Requires(property = "messagehandler.filter.normalizeddata.contentpathsfilter.order")
// we don't require  "messagehandler.filter.normalizeddata.contentpathsfilter.matchingcontentpath" as we want the instantiation to be attempted, but then fail if it's missing so there is error info, not a siolent fail
@Log
public class NormalizedDataContentPathsMessageFilter implements NormalizedDataMessageHandler {
	private final int order;
	private final Set<String> contentPaths;
	private final boolean caseInsensitive;
	private final FindOutcomes findOutcomes;

	public NormalizedDataContentPathsMessageFilter(
			@Property(name = "messagehandler.filter.normalizeddata.contentpathsfilter.order") int order,
			@Property(name = "messagehandler.filter.normalizeddata.contentpathsfilter.matchingcontentpath") List<String> contentPaths,
			@Property(name = "messagehandler.filter.normalizeddata.contentpathsfilter.caseinsensitive", defaultValue = "false") boolean caseInsensitive,
			@Property(name = "messagehandler.filter.normalizeddata.contentpathsfilter.findoutcome", defaultValue = "FOUND") FindOutcomes findOutcomes) {
		this.order = order;
		if (caseInsensitive) {
			this.contentPaths = contentPaths.stream().map(path -> path.toLowerCase()).collect(Collectors.toSet());
		} else {
			this.contentPaths = new HashSet<>(contentPaths);
		}
		this.caseInsensitive = caseInsensitive;
		this.findOutcomes = findOutcomes;
	}

	@Override
	public NormalizedData[] processNormalizedData(NormalizedData input) throws Exception {
		log.finer(() -> "NormalizedData is " + input);
		NormalizedData results[];
		String path = caseInsensitive ? input.getContentPath().toLowerCase() : input.getContentPath();
		// are we acting as a terminator or a step in the process ?
		boolean match = switch (findOutcomes) {
		case FOUND -> contentPaths.contains(path);
		case NOT_FOUND -> !contentPaths.contains(path);
		};
		if (match) {
			results = new NormalizedData[1];
			results[0] = input;
		} else {
			results = new NormalizedData[0];
		}
		log.fine(() -> findOutcomes + " is " + match + " for " + contentPaths + " case insensitive " + caseInsensitive
				+ " in content path " + input);
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
		return getName() + " order " + getOrder() + " will match " + contentPaths + " (" + contentPaths.size()
				+ " elements), caseInsensitive is " + caseInsensitive + ", findoutcomes is " + findOutcomes;
	}

}
