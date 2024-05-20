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
import java.util.Set;
import java.util.stream.Collectors;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.Messages;

import com.oracle.timg.demo.dependencyanalyser.importers.GetSourceLoader;
import com.oracle.timg.demo.dependencyanalyser.importers.SourceLoader;

import lombok.extern.java.Log;

@Log
public class DependencyScanner {

	public final static void main(String args[]) throws Exception {
		// process the args
		OptionsData options = new OptionsData();
		CmdLineParser parser = new CmdLineParser(options);
		try {
			// parse the arguments.
			parser.parseArgument(args);
			if (options.sources.isEmpty()) {
				throw new CmdLineException(parser, Messages.DEFAULT_META_REST_OF_ARGUMENTS_HANDLER,
						"No sources provided");
			}
		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();
			return;
		}
		log.fine("Processed options are :\n" + options);
		// place to put the results
		JarTreeCombiner combiner = new JarTreeCombiner(options.getGroupid(), options.getArtifactid(),
				options.getVersion());
		// let's process the args
		options.getSources().stream().forEach(source -> processSource(options, source, combiner));

		log.fine("Combiner root post combine is\n"
				+ combiner.getProjectTop().getRoot().getParent().toVersionDetails(""));
		JarTree combinedTree = combiner.getProjectTop();
		Set<IdentifiedJarFile> multiVersionJarFiles = combinedTree.getJarFilesWithMoreThanSpecifiedVersions(1);
		log.info("Count of combined Jar files with two or more versions\n" + multiVersionJarFiles.size());
		log.info("Details of jar files with multiple versions\n" + multiVersionJarFiles.stream()
				.map(jarFile -> jarFile.toVersionDetails("", true)).collect(Collectors.joining("\n", "", "\n")));
		log.info(
				"Combined Jar versions with no dependencies " + combinedTree.getJarVersionsWithNoDependencies().size());
		log.info("Combined Jar versions with no depended on " + combinedTree.getJarVersionsWithNoDependOn().size());
	}

	private static void processSource(OptionsData options, File source, JarTreeCombiner combiner) {
		log.info("Loading source " + source.getPath());
		SourceLoader loader;
		try {
			loader = GetSourceLoader.getSourceLoader(options, source);
		} catch (DependencyProcessorException e) {
			log.warning(
					"SourceLoader cannot process source " + source.getPath() + " due to " + e.getLocalizedMessage());
			return;
		}
		JarTree loadedJarTree;
		try {
			loadedJarTree = loader.loadDependencyTree(source);
		} catch (DependencyProcessorException e) {
			log.warning("SourceLoader cannot load source " + source.getPath() + " due to " + e.getLocalizedMessage());
			return;
		}
		log.info("Combining source " + source.getPath() + " with " + combiner.getProjectJarVersions().size()
				+ " jar versions already loaded");
		// now add it to the tree wew're building up
		combiner.addJarTree(loadedJarTree);
	}
}
