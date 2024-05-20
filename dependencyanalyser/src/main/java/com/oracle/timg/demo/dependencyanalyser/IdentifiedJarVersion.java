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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
public class IdentifiedJarVersion implements Comparable<IdentifiedJarVersion> {
	@EqualsAndHashCode.Include
	@NonNull
	private final String version;
	@EqualsAndHashCode.Exclude
	@NonNull
	private final IdentifiedJarFile parent;
	@EqualsAndHashCode.Exclude
	@NonNull
	private final String type;
	@EqualsAndHashCode.Exclude
	@NonNull
	private final ScopeType scope;
	// what depends on us - assumes that a dependency of say
	// com.fred.test.program:1.2.1 is always the same actual target of a dependency
	@EqualsAndHashCode.Exclude
	private final Set<Dependency> dependsOn = new TreeSet<>();
	// the things that depend on us (for reporting on conflict trees)
	@EqualsAndHashCode.Exclude
	private final LinkedList<Dependency> dependsOnThis = new LinkedList<>();
	// used to track the source projects this jar version came from
	@EqualsAndHashCode.Exclude
	private final Set<JarTree> sourceProjects = new TreeSet<>();
	@EqualsAndHashCode.Exclude
	private int refCount = 0;
	@EqualsAndHashCode.Exclude
	private boolean fromSource = true;

	public IdentifiedJarVersion() {
		this.version = "";
		this.parent = null;
		this.type = "";
		this.scope = ScopeType.ERROR;

	}

	public IdentifiedJarVersion(@NonNull String version, @NonNull IdentifiedJarFile parent, @NonNull String type,
			@NonNull ScopeType scope) {
		this(version, parent, type, scope, null);
	}

	public IdentifiedJarVersion(String version, IdentifiedJarFile parent, String type, ScopeType scope,
			JarTree sourceProject) {
		this.version = version;
		this.parent = parent;
		this.type = type;
		this.scope = scope;
		if (sourceProject != null) {
			this.sourceProjects.add(sourceProject);
		}
	}

	public IdentifiedJarVersion duplicateCore(@NonNull IdentifiedJarFile newParent) {
		IdentifiedJarVersion newJarVersion = new IdentifiedJarVersion(version, newParent, type, scope);
		newJarVersion.addSourceProjects(sourceProjects);
		// duplicates are by definition not from the core
		newJarVersion.setFromSource(false);
		return newJarVersion;
	}

	public boolean equals(@NonNull IdentifiedJarVersion other) {
		return version.equals(other.version);
	}

	public int hashCode() {
		return version.hashCode();
	}

	public int incrRefCount() {
		refCount++;
		return refCount;
	}

	public int decrCount() {
		refCount--;
		return refCount;
	}

	public void addSourceProjects(@NonNull Collection<JarTree> sourceProjectToAdd) {
		this.sourceProjects.addAll(sourceProjectToAdd);
	}

	@Override
	public String toString() {
		return processString(parent.getGroupId(), parent.getArtifactId(), version);
	}

	public String toFullerString(String indentString) {
		return indentString + processString(parent.getGroupId(), parent.getArtifactId(), version) + "Has "
				+ dependsOn.size() + " Dependencies and " + dependsOnThis.size() + " that depend on it";
	}

	public static String processString(String groupId, String artifactId, String version) {
		return groupId + ":" + artifactId + ":" + version;
	}

	public String toFullString(String indentString) {
		return toFullString(indentString, false);
	}

	public String toFullString(String indentString, boolean includeSources) {
		String vsn = indentString + version + " refs = " + getRefCount() + "\n";
		String dependsOnString = indentString + "    " + "Depends on\n";
		if (dependsOn.size() > 0) {
			dependsOnString += dependsOn.stream()
					.map(dependency -> dependency.getTarget().toString() + " (" + dependency.getScope() + ")")
					.collect(Collectors.joining("\n" + indentString + "        ", indentString + "        ", "\n"));
		}
		String dependsOnThisString = indentString + "    " + "Depended on by\n";
		if (dependsOnString.length() > 0) {
			dependsOnThisString += dependsOnThis.stream()
					.map(depender -> depender.getSource().toString() + " (" + depender.getScope() + ")")
					.collect(Collectors.joining("\n" + indentString + "        ", indentString + "        ", "\n"));
		}
		String resp = vsn + dependsOnString + dependsOnThisString;
		if (includeSources) {
			String sources = indentString + "    " + "In source\n";
			sources += sourceProjects.stream().map(sourceTree -> sourceTree.getRootDescription())
					.collect(Collectors.joining("\n" + indentString + "        ", indentString + "        ", "\n"));
			resp = resp += sources;
		}
		return resp;
	}

	@Override
	public int compareTo(IdentifiedJarVersion target) {
		return this.version.compareTo(target.version);
	}

	public void addSourceProject(@NonNull JarTree sourceProject) {
		this.sourceProjects.add(sourceProject);
	}
}
