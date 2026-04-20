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

import com.oracle.demo.timg.iot.iotdbjdbc.aqdata.RawData;

import jakarta.inject.Singleton;

@Singleton
/**
 * do some form of handling of the normalized data.
 * 
 * The handlerCore will construct a chain of handlers based on it's
 * configuration, identifying them by matching the name returned by the
 * getHandlerName() method.
 * 
 * It is possible for a handler to be in more than once chain, but this will be
 * cause a failure UNLESS isStateless() returns true.
 * 
 */
public interface RawDataMessageHandler extends MessageHandler {

	/**
	 * If transforming the input should not modify the input object, but instead
	 * create a new version(s), the resulting objects will be passed to the next
	 * stage in the order they are presented in the response..
	 * 
	 * This approach allows a transform to split up (or otherwise enrich) the data,
	 * OR to combine data, for example taking several individual inputs representing
	 * individual elements and once all elements of a resulting object have been
	 * provided then generating a single object combining the inputs in some way as
	 * an output(s).
	 * 
	 * This can be used as a filter by returning only objects that pass the filter
	 * 
	 * For output the result could be all outputs, or perhaps limited to onl'y
	 * objects that were not uploaded (this is application specific)
	 * 
	 * While this can throw an exception (in which case it will be treated as if it
	 * had returned an empty result set) the best practice is for it to handle
	 * exceptions and just return an empty array.
	 * 
	 * @param input
	 * @return
	 */
	public RawData[] processRawData(RawData input) throws Exception;

}