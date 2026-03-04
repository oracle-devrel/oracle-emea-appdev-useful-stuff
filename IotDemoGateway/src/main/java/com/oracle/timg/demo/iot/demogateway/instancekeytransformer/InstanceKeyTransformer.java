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
package com.oracle.timg.demo.iot.demogateway.instancekeytransformer;

import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;

/**
 * There must be at most one instance of the reformatter created, and it must be
 * a singleton (at some point it might be useful to allow for a chain of them I
 * suppose, but as they say that's left as an exercise for the reader) To ensure
 * this we use the Micronaut dynamic class injection, but indicate the
 * reformatter to inject using the required property
 * gateway.instance.keyreformatter
 * 
 * Note that specifying this in a reformatter means that the property value to
 * be REMOVE_SPECIAL_CHARACTERS for the instance to be created and thus
 * injected, using the default value means if the property is not set it's value
 * is assumed to be unknown (which won't match and therefore it won't be
 * created). If there are no reformatters that have a matching property then a
 * default pass through one is generated, this just passes the input straight
 * through with no change.
 * 
 * @Requires(property = "gateway.instance.keyreformatter", value =
 *                    "REMOVE_SPECIAL_CHARACTERS", defaultValue = "unknown")
 */
public interface InstanceKeyTransformer extends Comparable<InstanceKeyTransformer> {
	/**
	 * takes the incoming event key and reformats it into a String for upload to the
	 * IoT service (that's currently JSON)
	 * 
	 * @param recievedEventKey, cannot be null or empty
	 * @return
	 * @throws InstanceKeyIncommingFormatException - if the incoming key can't be
	 *                                             parsed or is badly formatted.
	 * @throws InstanceKeyTransformException     - if the reformatting engine
	 *                                             itself hit a problem
	 */
	public String reformatInstanceKey(@NotBlank @NonNull String recievedEventKey)
			throws InstanceKeyIncommingFormatException, InstanceKeyTransformException;

	/**
	 * returns reformatter instance specific data to describe the reformatter and
	 * settings.
	 * 
	 * @return
	 */
	public String getConfig();

	/**
	 * gets the name of this reformatter for use in diagnostocs
	 * 
	 * @return
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
	public default int compareTo(InstanceKeyTransformer otherInstanceKeyReformatter) {
		if (otherInstanceKeyReformatter == null) {
			throw new NullPointerException("otherInstanceKeyReformatter must not be null");
		}
		return Integer.compare(this.getOrder(), otherInstanceKeyReformatter.getOrder());
	}

}
