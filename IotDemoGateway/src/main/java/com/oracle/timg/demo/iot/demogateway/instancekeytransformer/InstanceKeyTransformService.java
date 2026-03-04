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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.extern.java.Log;

@Singleton
@Log

public class InstanceKeyTransformService {
	// this will inject a list of possible transformers
	// of course if the transformers are blocked because they are not instantiated
	// (maybe they require properties not set) they will not be included here
	@Inject
	private List<InstanceKeyTransformer> transformers;

	public String reformatInstanceKey(@NotBlank @NonNull String recievedEventKey)
			throws InstanceKeyIncommingFormatException, InstanceKeyTransformException {
		// go through the transformers list passing the output of each entry into the
		// following one as input
		String tempResp = recievedEventKey;
		for (InstanceKeyTransformer transformer : transformers) {
			try {
				tempResp = transformer.reformatInstanceKey(tempResp);
			} catch (InstanceKeyIncommingFormatException eIncomming) {
				throw new InstanceKeyIncommingFormatException("InstanceKeyTransformer " + transformer.getName()
						+ " threw an InstanceKeyIncommingFormatException processing incomming key " + tempResp
						+ " completed formatters are " + completedtransformers(transformer), eIncomming);
			} catch (InstanceKeyTransformException eReformatting) {
				throw new InstanceKeyTransformException("InstanceKeyTransformer " + transformer.getName()
						+ " threw an InstanceKeyTransformException processing incomming key " + tempResp
						+ " completed formatters are " + completedtransformers(transformer), eReformatting);
			}
		}
		String response = tempResp;
		log.fine(() -> "Source instance key " + recievedEventKey + " resulting instance key " + response);
		return tempResp;
	}

	private String completedtransformers(InstanceKeyTransformer failedTransformater) {
		return transformers.stream().takeWhile(r -> !(r == failedTransformater)).map(r -> r.getName())
				.collect(Collectors.joining(", "));
	}

	public String getConfig() {
		return "There are  " + transformers.size() + " transformers which are : " + transformers.stream()
				.map(r -> r.getName() + " (config " + r.getConfig() + ")").collect(Collectors.joining(", "));
	}

	public String getName() {
		return "Core InstanceKeyTransformService itself";
	}

	@PostConstruct
	public void postConstruct() {
		// there is no need to add a passthrough default as in practical terms this is
		// what will happen if no transformers are specified
		// make sure any transformers are correctly sorted to the order they report (or
		// more hopefully in the config file)
		transformers = transformers.stream().sorted().toList();
		log.info(this.toString());
	}

	@Override
	public String toString() {
		return getName() + ", " + getConfig();
	}
}
