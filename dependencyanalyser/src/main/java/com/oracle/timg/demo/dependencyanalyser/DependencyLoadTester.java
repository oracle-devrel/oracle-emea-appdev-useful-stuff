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
import java.util.stream.Collectors;

import com.oracle.timg.demo.dependencyanalyser.importers.MavenGraphLoader;

import lombok.extern.java.Log;

@Log
public class DependencyLoadTester {

	public final static void main(String args[]) throws Exception {
//		JarTree jarTreeReader = new JarTree("com.oracle.labs.helidon.fileio", "filereader", "1.0.0", "jar",
//				ScopeType.ROOT);
		MavenGraphLoader loader = new MavenGraphLoader(new OptionsData());
		JarTree jarTreeReader = loader.loadDependencyTree(new File(args[0]));
//		log.info("JarFiles list is :\n" + jarTreeReader.listJarFiles());
//		log.info("JarFiles details list is :\n" + jarTreeReader.listJarFileDetails());
		log.info("Jar files with no versions " + jarTreeReader.getJarFilesWithNoVersions().size());
		log.info("Jar files with two or more versions "
				+ jarTreeReader.getJarFilesWithMoreThanSpecifiedVersions(1).size());
		log.info("Jar versions with no dependencies " + jarTreeReader.getJarVersionsWithNoDependencies().size());
		log.info("Jar versions with no depended on " + jarTreeReader.getJarVersionsWithNoDependOn().size());
		log.info("Root jar version details \n" + jarTreeReader.getRoot().getParent().toVersionDetails(""));
		JarTreeCombiner combiner = new JarTreeCombiner("com.oracle.timg", "filehandling", "1.0.0");
//		log.warning(
//				"Combiner root pre combine is\n" + combiner.getProjectTop().getRoot().getParent().toVersionDetails(""));
//		log.warning("combined jar versions pre combine " + combiner.getProjectJarVersions().keySet());
//		// bring in the tree we just loaded
		combiner.addJarTree(jarTreeReader);
//		log.warning("Combiner root post combine is\n"
//				+ combiner.getProjectTop().getRoot().getParent().toVersionDetails(""));
//		log.warning("combined jar versions post combine " + combiner.getProjectJarVersions().keySet());
//		// get the first dependency in ther first version, then dump the target
		JarTree combinedTree = combiner.getProjectTop();
//		IdentifiedJarVersion firstTarget = new ArrayList<>(combinedTree.getRoot().getDependsOn()).get(0).getTarget();
//		log.info("combined first dependency (should be file reader)\n" + firstTarget.getParent().toVersionDetails(""));
////		log.info("Combined JarFiles list is :\n" + combinedTree.listJarFiles());
////		log.info("Combined JarFiles details list is :\n" + combinedTree.listJarFileDetails());
//		log.info("Combined Jar files with no versions " + combinedTree.getJarFilesWithNoVersions().size());
//		log.info("Combined Jar files with two or more versions "
//				+ combinedTree.getJarFilesWithMoreThanSpecifiedVersions(1).size());
//		log.info(
//				"Combined Jar versions with no dependencies " + combinedTree.getJarVersionsWithNoDependencies().size());
//		log.info("Combined Jar versions with no depended on " + combinedTree.getJarVersionsWithNoDependOn().size());
////		log.info("Combined Root jar version details \n" + combinedTree.getRoot().getParent().toVersionDetails(""));
//		log.info("Scanning second tree");
//
		JarTree jarTreeWriter = loader.loadDependencyTree(new File(args[1]));

//		log.warning("doing second combine");
		combiner.addJarTree(jarTreeWriter);
////
		log.warning("Combiner root post second combine is\n"
				+ combiner.getProjectTop().getRoot().getParent().toVersionDetails(""));
		log.warning("combined jar versions post second combine " + combiner.getProjectJarVersions().keySet());
		log.info("Combined final jar tree is :\n" + combinedTree.dumpTree(""));
		log.info("Combined final keys \n" + combinedTree.keySet());
		log.info("Project root final jar version dependencies " + combinedTree.getRoot().getDependsOn());
		log.info("Combined Jar files with two or more versions "
				+ combinedTree.getJarFilesWithMoreThanSpecifiedVersions(1).stream()
						.map(jarversion -> jarversion.toVersionDetails("", true)).collect(Collectors.joining("\n")));
	}

}
