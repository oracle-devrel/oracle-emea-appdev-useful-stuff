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
package com.oracle.timg.demo.dependencyanalyser.importers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.kazurayam.subprocessj.Subprocess;
import com.kazurayam.subprocessj.Subprocess.CompletedProcess;
import com.oracle.timg.demo.dependencyanalyser.DependencyProcessorException;
import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarFile;
import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarVersion;
import com.oracle.timg.demo.dependencyanalyser.JarTree;
import com.oracle.timg.demo.dependencyanalyser.OptionsData;
import com.oracle.timg.demo.dependencyanalyser.ScopeType;

import lombok.extern.java.Log;

/**
 * Loads the graph output of maven into the jartree to generate the mvn grapu :
 * 'mvn -f <pom.xml> dependency:tree -DoutputType=graphml -DoutputFile=<output
 * file name>' Always write the output to a file, dumping it to std out is a
 * prioblem as mvn puts a bunch or
 */
@Log
public class MavenPomXMLLoader extends MavenGraphLoader {
	public final static String MVN_DEPENDENCY_RESOLVE_CMD = "dependency:resolve";
	public final static String MVN_DEPENDENCY_TREE_CMD = "dependency:tree";
	public final static String MVN_DEPENDENCY_GRAPH_OUTPUT_FLAG = "-DoutputType=graphml";
	public final static String MVN_DEPENDENCY_OUTPUT_FILE_FLAG = "-DoutputFile="; // must be followed by a file name,
																					// but we can generate a temp one
																					// for now
	// mvn dependency:tree-DoutputType=graphml-DoutputFile=filewriter.txt
	protected IdentifiedJarVersion sourceJarVersion;
	protected IdentifiedJarFile sourceJarFile;

	public MavenPomXMLLoader(OptionsData optionsData) throws DependencyProcessorException {
		super(optionsData);
		// we need a maven executable
		if (optionsData.mvnPath == null) {
			throw new MavenPomXMLProcessorException(
					"Processing a pom.xml file requires that path to the maven executable be set");
		}
		if (!optionsData.mvnPath.exists()) {
			throw new MavenPomXMLProcessorException(
					"The maven executable location " + optionsData.mvnPath.getPath() + " does not exist");
		}
		if (!optionsData.mvnPath.canExecute()) {
			throw new MavenPomXMLProcessorException(
					"The maven executable location " + optionsData.mvnPath.getPath() + " is not executable");
		}
		// make sure we have a usable working directory we can write to
		if (optionsData.workingDirectory == null) {
			throw new MavenPomXMLProcessorException("Processing a pom.xml file requires a working directory be set");
		}
		if (!optionsData.workingDirectory.exists()) {
			throw new MavenPomXMLProcessorException(
					"The working directory location " + optionsData.workingDirectory.getPath() + " does not exist");
		}
		if (!optionsData.workingDirectory.isDirectory()) {
			throw new MavenPomXMLProcessorException(
					"The working directory location " + optionsData.workingDirectory.getPath() + " is not a directory");
		}
		if (!optionsData.workingDirectory.canWrite()) {
			throw new MavenPomXMLProcessorException(
					"The working directory location " + optionsData.workingDirectory.getPath() + " is not writable");
		}
		if (!optionsData.workingDirectory.canRead()) {
			throw new MavenPomXMLProcessorException(
					"The working directory location " + optionsData.workingDirectory.getPath() + " is not readable");
		}

	}

	@Override
	public JarTree loadDependencyTree(File sourceFile, File origionalSourceFile) throws DependencyProcessorException {
		// make sure that the source is there
		validateSource(sourceFile);
		log.info("Loading the provided maven pom file " + sourceFile.getPath());
		// next we generate the output
		// load the pom.xml file to play with
		MavenPomXMLExtractor pomExtractor;
		try {
			pomExtractor = new MavenPomXMLExtractor(sourceFile);
		} catch (XMLProcessorException e) {
			throw new MavenPomXMLProcessorException("Can't load the maven pom file " + e.getLocalizedMessage(), e);
		}

		log.fine("Loading the maven pom project details");
		// get the jar details from it
		IdentifiedJarVersion pomDetails;
		try {
			pomDetails = pomExtractor.getJarVersion();
		} catch (XMLProcessorException e) {
			throw new MavenPomXMLProcessorException(
					"Can't extract the maven project details " + e.getLocalizedMessage(), e);
		}
		log.fine("Maven pom core details extracted, result is " + pomDetails);
		return loadDependencyTree(sourceFile, origionalSourceFile, pomDetails);
	}

	/*
	 * for some inputs there are other ways to get the details (e.g. in a jar there
	 * is the pom.properties file which is easier to parse)
	 */
	public JarTree loadDependencyTree(File sourceFile, String groupId, String artifactId, String version,
			File origionalSourceFile) throws DependencyProcessorException {
		// make sure that the source is there
		validateSource(sourceFile);
		IdentifiedJarFile jarFile = new IdentifiedJarFile(groupId, artifactId);
		IdentifiedJarVersion pomDetails = new IdentifiedJarVersion(version, jarFile, "InputPom", ScopeType.INPUT);
		log.fine("Maven pom core details built, result is " + pomDetails);
		return loadDependencyTree(sourceFile, origionalSourceFile, pomDetails);
	}

	public JarTree loadDependencyTree(File sourceFile, File origionalSourceFile, IdentifiedJarVersion pomDetails)
			throws DependencyProcessorException {
		// load the pom.xml file and setup the info on it
		// unless they have overridden the dependency resolution flag first resolve the
		// dependencies to the local maven cache, that way we know we will have all of
		// the contents locally before we then build the graphml file and process it
		if (optionsData.isResolveDependencies()) {
			// command is <mvn cmd path> dependency:resolve -f <pom file>
			CompletedProcess resolveCP;
			try {
				List<String> resolveList = Arrays.asList(optionsData.mvnPath.getAbsolutePath(), "-f",
						sourceFile.getAbsolutePath(), MVN_DEPENDENCY_RESOLVE_CMD);
				if (optionsData.isVerboseOutput()) {
					log.info("About to run the following command to resolve the dependencies, this may take a while:\n"
							+ resolveList);
				}
				resolveCP = new Subprocess().cwd(new File(".")).run(resolveList);
			} catch (IOException | InterruptedException e) {
				throw new MavenPomXMLProcessorException(
						"mvn dependency resolution did not complete sucesfully, " + e.getLocalizedMessage(), e);
			}
			if (optionsData.isVerboseOutput()) {
				log.info("Maven dependency resolve standard output is :\n" + resolveCP.stdout());
				log.info("Maven dependency resolve error output is :\n" + resolveCP.stderr());
			}
			int resolveReturnCode = resolveCP.returncode();
			if (resolveReturnCode != 0) {
				throw new MavenPomXMLProcessorException(
						"mvn dependency resolution did not complete sucesfully, return code was " + resolveReturnCode);
			}
		}
		// work out where to save the mvn-graphml file, we will include the name details
		// in this
		String prefix = pomDetails.getParent().getGroupId() + "." + pomDetails.getParent().getArtifactId() + "-"
				+ pomDetails.getVersion() + "-";
		Path graphMLPath;
		try {
			graphMLPath = Files.createTempFile(optionsData.getWorkingDirectory().toPath(), prefix,
					GetSourceLoader.MAVEN_GRAPH_FILE_SUFFIX);
		} catch (IOException e) {
			throw new MavenPomXMLProcessorException(
					"Can't create temp file for graphml output in " + optionsData.getWorkingDirectory()
							+ " using prefix " + prefix + " and suffix " + GetSourceLoader.MAVEN_GRAPH_FILE_SUFFIX);
		}
		File tempGraphMl = graphMLPath.toFile();
		// note that maven seems to set it's working directory to where the pom.xml file
		// is, if the location of the graphxml file is relative then maven will if
		// needed create directories relative to that. This can mean that the graphxml
		// file is generated in an unexpected location. to solve that we use an absolute
		// path
		String tempGraphMLFileName = tempGraphMl.getAbsolutePath();
		// if needed mark this as something to delete when the JVM closes
		if (!optionsData.isRetainIntermediateFiles()) {
			tempGraphMl.deleteOnExit();
			log.info("Will delete temp graphml file  " + tempGraphMLFileName + " on exit");
		} else {
			log.info("Will retain temp graphml  file " + tempGraphMLFileName + " on exit");
		}
		// build a graph from it
		CompletedProcess dependencyTreeCP;
		try {
			List<String> dependencyList = Arrays.asList(optionsData.mvnPath.getAbsolutePath(), "-f",
					sourceFile.getAbsolutePath(), MVN_DEPENDENCY_TREE_CMD, MVN_DEPENDENCY_GRAPH_OUTPUT_FLAG,
					MVN_DEPENDENCY_OUTPUT_FILE_FLAG + tempGraphMLFileName);
			log.info("About to run the following command to get the dependency tree, this may take a while:\n"
					+ dependencyList);
			dependencyTreeCP = new Subprocess().run(dependencyList);
		} catch (IOException | InterruptedException e) {
			throw new MavenPomXMLProcessorException(
					"mvn dependency resolution did not complete sucesfully, " + e.getLocalizedMessage(), e);
		}
		if (optionsData.isVerboseOutput()) {
			log.info("Maven dependency tree standard output is :\n" + dependencyTreeCP.stdout());
			log.info("Maven dependency tree error output is :\n" + dependencyTreeCP.stderr());
		}

		int dependencyTreeReturnCode = dependencyTreeCP.returncode();
		if (dependencyTreeReturnCode != 0) {
			throw new MavenPomXMLProcessorException(
					"mvn dependency tree computation did not complete sucesfully, return code was "
							+ dependencyTreeReturnCode);
		}
		// now we can get the superclass to operate on the graph output
		return super.loadDependencyTree(tempGraphMl, origionalSourceFile);
	}
}
