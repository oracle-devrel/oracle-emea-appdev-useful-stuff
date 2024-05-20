/*Copyright (c) 2024 Oracle and/or its affiliates.

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
package com.oracle.timg.demo.dependencyanalyser;

import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class IdentifiedJarFile extends TreeMap<String, IdentifiedJarVersion> implements Comparable<IdentifiedJarFile> {
	private static final long serialVersionUID = 7088951357215826846L;
	@EqualsAndHashCode.Include
	@NonNull
	private final String groupId;
	@EqualsAndHashCode.Include
	@NonNull
	private final String artifactId;
	@EqualsAndHashCode.Exclude
	private boolean fromSource = true;
	@EqualsAndHashCode.Exclude
	private int refCount = 0;

	public IdentifiedJarFile duplicateCore() {
		IdentifiedJarFile newJarFile = new IdentifiedJarFile(this.groupId, this.artifactId);
		newJarFile.setFromSource(false);
		return newJarFile;
	}

	public int incrRefCount() {
		refCount++;
		return refCount;
	}

	public int decrCount() {
		refCount--;
		return refCount;
	}

	@Override
	public String toString() {
		return processString(groupId, artifactId);
	}

	public static String processString(String groupId, String artifactId) {
		return groupId + ":" + artifactId;
	}

	public String toVersionList(String indentString) {
		return indentString + toString() + "\n" + this.values().stream()
				.map(version -> indentString + "    " + version.getVersion()).collect(Collectors.joining("\n"));
	}

	public String toVersionDetails(String indentString) {
		return toVersionDetails(indentString, false);
	}

	public String toVersionDetails(String indentString, boolean includeSources) {
		return indentString + toString() + "\n"
				+ this.values().stream().map(version -> version.toFullString(indentString + "    ", includeSources))
						.collect(Collectors.joining("\n"));
	}

	@Override
	public int compareTo(IdentifiedJarFile other) {
		return this.toString().compareTo(other.toString());
	}

}
