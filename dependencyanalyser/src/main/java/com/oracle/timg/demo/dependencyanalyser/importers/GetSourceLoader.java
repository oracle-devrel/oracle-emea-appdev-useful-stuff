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

import com.oracle.timg.demo.dependencyanalyser.DependencyProcessorException;
import com.oracle.timg.demo.dependencyanalyser.OptionsData;

public class GetSourceLoader {

	public static final String POM_XML = "pom.xml";
	public final static String MAVEN_GRAPH_FILE_SUFFIX = ".mvn-graphml.xml";
	public final static String JAR_FILE_SUFFIX = ".jar";

	public static SourceLoader getSourceLoader(OptionsData options, File source) throws DependencyProcessorException {
		if (!source.exists()) {
			throw new DependencyProcessorException(
					"Source file " + source.getAbsolutePath() + " does not exist, cannot process it");
		}
		if (!source.isFile()) {
			throw new DependencyProcessorException(
					"Source file " + source.getAbsolutePath() + " is not a file, cannot process it");
		}
		if (!source.canRead()) {
			throw new DependencyProcessorException(
					"Source file " + source.getAbsolutePath() + " is not readable, cannot process it");
		}
		// need to work out what type of source this is.
		String sourceName = source.getName();
		if (sourceName.equalsIgnoreCase(POM_XML)) {
			return new MavenPomXMLLoader(options);
		}
		// if it's a graphml file we can process it instantly
		if (source.getName().endsWith(MAVEN_GRAPH_FILE_SUFFIX)) {
			return new MavenGraphLoader(options);
		}
		// if it's a graphml file we can process it instantly
		if (source.getName().endsWith(JAR_FILE_SUFFIX)) {
			return new JarFileLoader(options);
		}
		throw new DependencyProcessorException("Unable to locate a loader for file type " + sourceName);
	}

}
