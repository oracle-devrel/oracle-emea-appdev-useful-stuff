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
package com.oracle.demo.timg.iot.iotdbjdbc.messagehandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.NormalizedData;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.java.Log;

@Singleton
@Log
public class NormalizedDataMessageHandlerService {
	// this will inject a list of possible handlers
	// of course if the transformers are blocked because they are not instantiated
	// (maybe they require properties not set) they will not be included here
	private final ArrayList<NormalizedDataMessageHandler> handlers;

	@Inject
	public NormalizedDataMessageHandlerService(List<NormalizedDataMessageHandler> handlers) {
		this.handlers = new ArrayList<>(handlers.stream().sorted().toList());
		if (handlers.size() == 0) {
			log.warning("No handlers configured");
			return;
		}
		log.info(this.toString());
	}

	public void handle(@NonNull NormalizedData normalizedData) {
		log.info("Handling NormalizedData " + normalizedData);
		if (handlers.size() == 0) {
			log.warning("No NormalizedDataMessageHandler loaded, cannot process " + normalizedData);
			return;
		}
		int handlerIndex = 0;
		handle(handlerIndex, handlers.get(0), normalizedData);
	}

	private void handle(int handlerIndex, @NonNull NormalizedDataMessageHandler handler,
			@NonNull NormalizedData normalizedData) {
		// run the handler and get the response
		NormalizedData handledNormalizedData[];
		try {
			log.info("Calling handler " + handler.getName() + " at index " + handlerIndex + " to process "
					+ normalizedData);
			handledNormalizedData = handler.processNormalizedData(normalizedData);
		} catch (Exception e) {
			log.warning("Exception in handler " + handler.getName() + " with configuration " + handler.getConfig()
					+ " handling normalizedData " + normalizedData);
			return;
		}
		// if there were resulting messages then we should process them provided there
		// is another handler stage
		int nextHandlerIndex = handlerIndex + 1;
		if ((handledNormalizedData.length > 0) && (nextHandlerIndex > handlers.size())) {
			// we can't have got here if there wasn't a handler at this index so
			NormalizedDataMessageHandler nextHandler = handlers.get(nextHandlerIndex);
			// this meets the ordering requirements as arrays.stream returns a sequential
			// stream
			log.info("Resulting data is " + handledNormalizedData.length + " results, calling handler "
					+ nextHandler.getName() + " at index " + nextHandlerIndex + " on them");
			Arrays.stream(handledNormalizedData)
					.forEach(nextNormalizedData -> handle(nextHandlerIndex, nextHandler, nextNormalizedData));
		}

	}

	public String getConfig() {
		return "There are " + handlers.size() + " handlers which are " + handlers.stream()
				.map(h -> h.getName() + " (config" + h.getConfig() + ")").collect(Collectors.joining(", "));
	}

	public String getName() {
		return "Core NormalizedDataMessageHandlerService itself";
	}

	@Override
	public String toString() {
		return getName() + ", " + getConfig();
	}
}
