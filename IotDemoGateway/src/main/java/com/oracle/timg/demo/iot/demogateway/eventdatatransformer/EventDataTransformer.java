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
package com.oracle.timg.demo.iot.demogateway.eventdatatransformer;

import com.oracle.timg.demo.iot.demogateway.instancekeytransformer.InstanceKeyIncommingFormatException;
import com.oracle.timg.demo.iot.demogateway.instancekeytransformer.InstanceKeyTransformException;

import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

public interface EventDataTransformer extends Comparable<EventDataTransformer> {
	/**
	 * takes the incoming event data and reformats it into a String for upload to
	 * the IoT service (that's currently JSON)
	 * 
	 * @param recievedEventData, cannot be null or empty
	 * @return
	 * @throws InstanceKeyIncommingFormatException - if the incoming data can't be
	 *                                             parsed or is badly formatted.
	 * @throws InstanceKeyTransformException     - if the reformatting engine
	 *                                             itself hit a problem
	 */
	public String reformatEventData(@NotBlank @NonNull String recievedEventData)
			throws EventDataIncommingFormatException, EventDataTransformException;

	/**
	 * returns reformatter instance specific data to describe the reformatter and
	 * settings.
	 * 
	 * @return
	 */
	public String getConfig();

	/**
	 * returns the name of this reformatter
	 */
	public String getName();

	/**
	 * the sort order to be applied to the reformatters as in some cases there may
	 * be order dependencies
	 * 
	 * @return
	 */
	public int getOrder();

	@Override
	public default int compareTo(EventDataTransformer otherEventDataReformatter) {
		if (otherEventDataReformatter == null) {
			throw new NullPointerException("otherEventDataReformatter must not be null");
		}
		return Integer.compare(this.getOrder(), otherEventDataReformatter.getOrder());
	}
}
