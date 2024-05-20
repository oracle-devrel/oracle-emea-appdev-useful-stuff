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

import java.util.Map;
import java.util.TreeMap;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.java.Log;

@Log
@Data
public class JarTreeCombiner {
	public final static String PROJECT_FILE_TYPE = "project";
	public final static ScopeType PROJECT_SCOPE_TYPE = ScopeType.PROJECT;
	private JarTree projectTop;
	@EqualsAndHashCode.Exclude
	private Map<String, IdentifiedJarVersion> projectJarVersions = new TreeMap<>();

	public JarTreeCombiner(String groupId, String artifactId, String projectVersionString) {
		this.projectTop = new JarTree(groupId, artifactId, projectVersionString, PROJECT_FILE_TYPE, PROJECT_SCOPE_TYPE);
		projectTop.getRoot().getParent().setFromSource(false);
		projectTop.getRoot().setFromSource(false);
		// get the "root" project jar version we just created and stash it for later
		// access
		projectJarVersions.put(projectTop.getRoot().toString().trim(), projectTop.getRoot());

	}

	public void addJarTree(JarTree jarTree) {
		// force up the root connection to our project
		IdentifiedJarVersion incomingRootJarVersion = jarTree.getRoot();
		// if the jar file is already processed then no need to setup the new one
		String incommingRoofJarFileName = incomingRootJarVersion.getParent().toString();
		IdentifiedJarFile newRootJarFile = projectTop.get(incommingRoofJarFileName.trim());
		if (newRootJarFile == null) {
			log.finer("Cannot locate incomming root jar file " + incommingRoofJarFileName + ", creating");
			newRootJarFile = incomingRootJarVersion.getParent().duplicateCore();
			// stash it in the new jar tree
			projectTop.put(newRootJarFile.toString().trim(), newRootJarFile);
			// in this case the jar file will (should) also be in the incoming tree, so
			// reduce the ref count
			// to reflect we've set it up already
			newRootJarFile.decrCount();
		} else {
			log.finer("Located an existing jar file for the project root of " + incommingRoofJarFileName);
		}
		// sort out the jar version
		String incomingRootJarVersionName = incomingRootJarVersion.getVersion();
		IdentifiedJarVersion newRootJarVersion = newRootJarFile.get(incomingRootJarVersionName.trim());
		if (newRootJarVersion == null) {
			log.finer("Cannot locate incomming root jar version " + incomingRootJarVersionName + ", creating");
			newRootJarVersion = incomingRootJarVersion.duplicateCore(newRootJarFile);
			// add the new version info to the new jar file
			newRootJarFile.put(incomingRootJarVersionName.trim(), newRootJarVersion);
			// put the new jar version in the main stash so we can get it later when setting
			// up the dependencies
			projectJarVersions.put(newRootJarVersion.toString().trim(), newRootJarVersion);
		} else {
			log.finer("Located incomming root jar version " + incomingRootJarVersionName + ", reusing");
		}
		newRootJarVersion.incrRefCount();
		// setup the dependency, this will link the project root to the incoming tree
		// root duplicate
		Dependency dependency = new Dependency(projectTop.getRoot(), newRootJarVersion, PROJECT_SCOPE_TYPE);
		// add the dependencies in place
		projectTop.getRoot().getDependsOn().add(dependency);
		newRootJarVersion.getDependsOnThis().add(dependency);
		// copy over all of the jar files and version details
		addAllJarFilesAndVersions(jarTree);
		// now we have everything we need for the dependencies
		addAllDependencies(jarTree);
	}

	private void addAllJarFilesAndVersions(JarTree incommingJarTree) {
		// for every incoming jar file get the jar file from the main tree, creating if
		// needed
		incommingJarTree.entrySet().stream().forEach(jarFileEntry -> {
			String jarName = jarFileEntry.getKey();
			IdentifiedJarFile incommingJarFile = jarFileEntry.getValue();
			IdentifiedJarFile mainJarFile = projectTop.get(jarName.trim());
			// we don't have this yet create a new one
			if (mainJarFile == null) {
				mainJarFile = incommingJarFile.duplicateCore();
				projectTop.put(jarName.trim(), mainJarFile);
			}
			mainJarFile.incrRefCount();
			// for the next stream to work the jar file must be final so
			IdentifiedJarFile finalMainJarFile = mainJarFile;
			// for every version of that jar file add it to the main tree version (the jar
			// file itself must exist as we have just created it if needed)
			incommingJarFile.entrySet().stream().forEach(jarVersionEntry -> {
				String versionName = jarVersionEntry.getKey();
				IdentifiedJarVersion incommingJarVersion = jarVersionEntry.getValue();
				IdentifiedJarVersion mainJarVersion = finalMainJarFile.get(versionName.trim());
				// if the version does not exist in the main jar version then create and add it
				// then add the original jar versions source projects (the source JarTree) to
				// the
				// main trees version source projects (let's us identify the source of a
				// conflict)
				if (mainJarVersion == null) {
					// this will copy over the original source project
					mainJarVersion = incommingJarVersion.duplicateCore(finalMainJarFile);
					finalMainJarFile.put(versionName.trim(), mainJarVersion);
					// stash a direct link to the version to speed up access later
					projectJarVersions.put(mainJarVersion.toString().trim(), mainJarVersion);
				} else {
					// the version exists, but we need to add the source projects to the existing
					// version
					mainJarVersion.addSourceProjects(incommingJarVersion.getSourceProjects());
				}
				// increment the ref count
				mainJarVersion.incrRefCount();
			});
		});
	}

	private void addAllDependencies(JarTree incommingJarTree) {
		// for every incoming jar file get the jar file from the main tree, then the
		// versions, then the dependencies
		incommingJarTree.values().stream().flatMap(incommingJarFile -> incommingJarFile.values().stream())
				.flatMap(incommingJarVersion -> incommingJarVersion.getDependsOn().stream())
				.forEach(incommingTreeDependency -> {
					// the dependency will reference the original jar tree jar versions, but we need
					// the main tree versions
					// so need to get the main tree version entries
					String incommingSourceJarVersionName = incommingTreeDependency.getSource().toString();
					IdentifiedJarVersion mainTreeSourceJarVersion = projectJarVersions
							.get(incommingSourceJarVersionName.trim());
					if (mainTreeSourceJarVersion == null) {
						log.severe("Cannot locate the version " + incommingSourceJarVersionName
								+ " in the main tree, but it should be there");
						return;
					}
					String incommingTargetJarVersionName = incommingTreeDependency.getTarget().toString();
					IdentifiedJarVersion mainTreeTargetJarVersion = projectJarVersions
							.get(incommingTargetJarVersionName.trim());
					if (mainTreeTargetJarVersion == null) {
						log.severe("Cannot locate the version " + incommingTargetJarVersionName
								+ " in the main tree, but it should be there");
						return;
					}
					// we have the matching source and dest, let's create a new dependency for them
					Dependency mainTreeDependency = incommingTreeDependency.dupeCore(mainTreeSourceJarVersion,
							mainTreeTargetJarVersion);
					// add the dependency
					mainTreeSourceJarVersion.getDependsOn().add(mainTreeDependency);
					mainTreeTargetJarVersion.getDependsOnThis().add(mainTreeDependency);
				});
	}
}
