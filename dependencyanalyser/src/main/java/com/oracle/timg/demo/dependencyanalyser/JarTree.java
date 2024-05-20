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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
public class JarTree extends TreeMap<String, IdentifiedJarFile> implements Comparable<JarTree> {
	private static final long serialVersionUID = -1545900389748317346L;
	private IdentifiedJarVersion root = new IdentifiedJarVersion();
	private final File origionalSourceFile;

	public JarTree(String groupId, String artifactId, String jarFileVersionString, String jarFileType,
			ScopeType jarScope) {
		this(groupId, artifactId, jarFileVersionString, jarFileType, jarScope, null);
	}

	public JarTree(String groupId, String artifactId, String jarFileVersionString, String jarFileType,
			ScopeType jarScope, File origionalSourceFile) {
		this.origionalSourceFile = origionalSourceFile;
		IdentifiedJarFile identifiedJarFile = new IdentifiedJarFile(groupId, artifactId);
		IdentifiedJarVersion identifiedJarVersion = new IdentifiedJarVersion(jarFileVersionString.trim(),
				identifiedJarFile, jarFileType, jarScope);
		identifiedJarFile.put(jarFileVersionString.trim(), identifiedJarVersion);
		this.root = identifiedJarVersion;
		this.put(IdentifiedJarFile.processString(groupId, artifactId).trim(), identifiedJarFile);
		identifiedJarVersion.addSourceProject(this);
	}

	public String dumpTree(IdentifiedJarVersion jarVersionCurrent, String indent, ScopeType scopeType, int depth) {
		// do a depth first dump with indentation
		String resp = indent;
		String subIndent = indent + "|---";
		// try to get the dependency link scope types
		List<ScopeType> dependentScopes;
		if (jarVersionCurrent.getDependsOnThis().size() == 0) {
			dependentScopes = new ArrayList<>(1);
			dependentScopes.add(ScopeType.ROOT);
		} else {
			dependentScopes = jarVersionCurrent.getDependsOnThis().stream().map(dependecyOn -> dependecyOn.getScope())
					.collect(Collectors.toList());
		}
		resp += jarVersionCurrent.toString() + " (" + dependentScopes + " refs = " + jarVersionCurrent.getRefCount()
				+ ")\n";
		if (depth == 0) {
			return resp + "\n" + "Reached specified depth\n";
		}
		int newDepth;
		if (depth > 0) {
			newDepth = depth - 1;
		} else {
			newDepth = depth;
		}
		// process the dependencies
		String dependencyString = jarVersionCurrent.getDependsOn().stream()
				.map(dependency -> dumpTree(dependency.getTarget(), subIndent, dependency.getScope(), newDepth))
				.collect(Collectors.joining());
		return resp + dependencyString;

	}

	public String dumpTree() {
		return this.dumpTree("", -1);
	}

	public String dumpTree(String indent) {
		return this.dumpTree(indent, -1);
	}

	public String dumpTree(String indent, int depth) {
		return this.dumpTree(root, indent, root.getScope(), depth);
	}

	@Override
	public int compareTo(JarTree other) {
		return this.root.compareTo(other.root);
	}

	public String listJarFiles() {
		return this.entrySet().stream().map(entry -> entry.getValue().toVersionList(""))
				.collect(Collectors.joining("\n"));
	}

	public String listJarFileDetails() {
		return listJarFileDetails(false);
	}

	public String listJarFileDetails(boolean includeSources) {
		return this.entrySet().stream().map(entry -> entry.getValue().toVersionDetails("", includeSources))
				.collect(Collectors.joining("\n"));
	}

	public Set<IdentifiedJarFile> getJarFilesWithNoVersions() {
		return this.values().stream().filter(jarFile -> jarFile.isEmpty()).collect(Collectors.toSet());
	}

	public Set<IdentifiedJarFile> getJarFilesWithMoreThanSpecifiedVersions(int maxVersionCount) {
		return this.values().stream().filter(jarFile -> jarFile.size() > maxVersionCount).collect(Collectors.toSet());
	}

	public Set<IdentifiedJarVersion> getJarVersionsWithNoDependencies() {
		return this.values().stream().flatMap(jarFile -> jarFile.values().stream())
				.filter(version -> version.getDependsOn().size() == 0).collect(Collectors.toSet());
	}

	public Set<IdentifiedJarVersion> getJarVersionsWithNoDependOn() {
		return this.values().stream().flatMap(jarFile -> jarFile.values().stream())
				.filter(version -> version.getDependsOnThis().size() == 0).collect(Collectors.toSet());
	}

	public String getRootDescription() {
		String resp = this.getRoot().toString();
		if (origionalSourceFile == null) {
			resp += " no origional source file available";
		} else {
			resp += " from origional source " + origionalSourceFile.getPath();
		}
		return resp;
	}
}
