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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import com.oracle.timg.demo.dependencyanalyser.DependencyProcessorException;
import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarVersion;
import com.oracle.timg.demo.dependencyanalyser.JarTree;
import com.oracle.timg.demo.dependencyanalyser.OptionsData;

import lombok.extern.java.Log;

@Log
public class JarFileLoader extends MavenPomXMLLoader {
	public final static String ALLOWABLE_CHARACTERS = "[\\w\\.]";

	public JarFileLoader(OptionsData optionsData) throws DependencyProcessorException {
		super(optionsData);
	}

	@Override
	public JarTree loadDependencyTree(File sourceFile, File origionalSourceFile) throws DependencyProcessorException {
		// will need this later
		File tempPomXMLFile;
		// try and locate the pom.properties in the jar file, this is the simplest
		// option
		try (JarFile jarFile = new JarFile(sourceFile);) {
			// regexp to locate the pom.properties it will be named along the lines of
			// META-INF/maven/<groupId>/<atrifactId>/pom.properties
			// make this final so the lambda doesn't get upset
			final Pattern pomPropertiesPattern = Pattern.compile(
					"META-INF/maven/" + ALLOWABLE_CHARACTERS + "*/" + ALLOWABLE_CHARACTERS + "*/pom.properties",
					Pattern.CASE_INSENSITIVE);
			// try to locate the pom.properties file entry
			Optional<JarEntry> pomPropertiesEntry = jarFile.stream().filter(entry -> {
				Matcher matcher = pomPropertiesPattern.matcher(entry.getName());
				return matcher.find();
			}).findFirst();
			// the pom.xml will be similar
			final Pattern pomXMLPattern = Pattern.compile(
					"META-INF/maven/" + ALLOWABLE_CHARACTERS + "*/" + ALLOWABLE_CHARACTERS + "*/pom.xml",
					Pattern.CASE_INSENSITIVE);
			// try to locate the pom.properties file entry
			Optional<JarEntry> pomXMLEntry = jarFile.stream().filter(entry -> {
				Matcher matcher = pomXMLPattern.matcher(entry.getName());
				return matcher.find();
			}).findFirst();
			// if we have the pom.properties then get the pom info form that, it's "safer"
			IdentifiedJarVersion jarFileIdentifiedJarVerion = null;
			if (pomPropertiesEntry.isPresent()) {
				ZipEntry pomPropertiesZipFile = pomPropertiesEntry.get();
				try {
					MavenPomPropertiesExtractor pomPropertiesExtractor = new MavenPomPropertiesExtractor(
							jarFile.getInputStream(pomPropertiesZipFile));
					jarFileIdentifiedJarVerion = pomPropertiesExtractor.getJarVersion();
					log.fine("Extracted pom info " + jarFileIdentifiedJarVerion + " from  pom.properties in jar file "
							+ sourceFile.getPath());
				} catch (MavenPomPropertiesProcessorException e) {
					log.fine("Problem loading the pom properties because " + e.getLocalizedMessage());
				}
			}
			// if we loaded the jar details then we will use them later, but if we didn't
			// then fall back to trying to get them from the pom
			if (jarFileIdentifiedJarVerion == null) {
				if (!pomXMLEntry.isPresent()) {
					throw new JarFIleProcessorException("Unfortunately jar file " + sourceFile.getPath()
							+ " does not containe a usable pom.properties file or pom.xml file, cannot process this jar file");
				}
				ZipEntry pomXMLZipFile = pomXMLEntry.get();
				try {
					MavenPomXMLExtractor pomXMLExtractor = new MavenPomXMLExtractor(
							jarFile.getInputStream(pomXMLZipFile));
					jarFileIdentifiedJarVerion = pomXMLExtractor.getJarVersion();
					log.fine("Extracted pom info " + jarFileIdentifiedJarVerion + " from  pom.xml in jar file "
							+ sourceFile.getPath());
				} catch (XMLProcessorException e) {
					throw new JarFIleProcessorException("Unfortunately jar file " + sourceFile.getPath()
							+ " does not have a usable pom.xml file, cannot process this jar file");
				}
			}

			if (jarFileIdentifiedJarVerion == null) {
				throw new JarFIleProcessorException("Unfortunately jar file " + sourceFile.getPath()
						+ " does not have a usable pom.xml or pom.properties file, cannot process this jar file");
			}
			// have to check for the pom file directly as we may have gotten the info from
			// the pom.properties file
			if (!pomXMLEntry.isPresent()) {
				throw new JarFIleProcessorException("Unfortunately jar file " + sourceFile.getPath()
						+ " does not containe a pom.xml file, cannot process this jar file");
			}
			// build a name for the pom.xml file to cache
			String prefix = jarFileIdentifiedJarVerion.getParent().getGroupId() + "."
					+ jarFileIdentifiedJarVerion.getParent().getArtifactId() + "-"
					+ jarFileIdentifiedJarVerion.getVersion() + "-";
			Path pomXMLPath;
			try {
				pomXMLPath = Files.createTempFile(optionsData.getWorkingDirectory().toPath(), prefix,
						"." + GetSourceLoader.POM_XML);
			} catch (IOException e) {
				throw new JarFIleProcessorException(
						"Can't create temp file for pom.xml output in " + optionsData.getWorkingDirectory()
								+ " using prefix " + prefix + " and suffix " + "." + GetSourceLoader.POM_XML);
			}

			tempPomXMLFile = pomXMLPath.toFile();
			String tempPomXMLFileName = tempPomXMLFile.getAbsolutePath();
			// if needed mark this as something to delete when the JVM closes
			if (!optionsData.isRetainIntermediateFiles()) {
				tempPomXMLFile.deleteOnExit();
				log.info("Will delete temp pom.xml file  " + tempPomXMLFileName + " on exit");
			} else {
				log.info("Will retain temp pom.xml  file " + tempPomXMLFileName + " on exit");
			}
			ZipEntry pomXMLZipFile = pomXMLEntry.get();
			log.finer("Pom.xml file in " + pomXMLZipFile + " is of path " + pomXMLZipFile.getName() + " and "
					+ pomXMLZipFile.getSize() + " bytes long when uncompressed");
			InputStream pomXmlInputStream;
			try {
				pomXmlInputStream = jarFile.getInputStream(pomXMLZipFile);
			} catch (IOException e) {
				throw new JarFIleProcessorException(
						"Unable to get the pom.xml input stream from " + sourceFile.getPath()
								+ " because of IO problem " + e.getMessage() + ", cannot process this jar file");
			}
			try {
				Files.copy(pomXmlInputStream, pomXMLPath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new JarFIleProcessorException("Unable to extract the pom.xml file from " + sourceFile.getPath()
						+ " because of IO problem " + e.getMessage() + ", cannot process this jar file");
			}
		} catch (IOException e) {
			throw new JarFIleProcessorException(
					"IO problem loading the jar file " + sourceFile.getPath() + ", " + e.getLocalizedMessage());
		}
		// we have the pom.xml extracted in the temp files. now get the superclass to
		// process it
		return super.loadDependencyTree(tempPomXMLFile);
	}
}
