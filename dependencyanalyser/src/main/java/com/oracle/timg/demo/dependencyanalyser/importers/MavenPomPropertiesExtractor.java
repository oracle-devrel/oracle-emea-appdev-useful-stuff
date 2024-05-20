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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarVersion;
import com.oracle.timg.demo.dependencyanalyser.ScopeType;

import lombok.NonNull;

public class MavenPomPropertiesExtractor implements XMLLoader, MavenPomDetailsBuilder {
	public final static String GROUP_ID_NAME = "groupId";
	public final static String ARTIFACT_ID_NAME = "artifactId";
	public final static String VERSION_NAME = "version";
	private Properties pomProperties = new Properties();

	public MavenPomPropertiesExtractor(@NonNull File sourceInputFile)
			throws MavenPomPropertiesProcessorException, FileNotFoundException {
		this(new FileInputStream(sourceInputFile));
	}

	public MavenPomPropertiesExtractor(@NonNull InputStream sourceInputStream)
			throws MavenPomPropertiesProcessorException {
		try {
			pomProperties.load(sourceInputStream);
		} catch (IOException e) {
			throw new MavenPomPropertiesProcessorException(
					"Unable to load the pom properties from the provided input stream because "
							+ e.getLocalizedMessage());
		}
	}

	public IdentifiedJarVersion getJarVersion() throws MavenPomPropertiesProcessorException {
		String groupId = pomProperties.getProperty(GROUP_ID_NAME);
		if (groupId == null) {
			throw new MavenPomPropertiesProcessorException(
					"Can't locate " + GROUP_ID_NAME + " in the pom properties input");
		}
		String artifactId = pomProperties.getProperty(ARTIFACT_ID_NAME);
		if (artifactId == null) {
			throw new MavenPomPropertiesProcessorException(
					"Can't locate " + ARTIFACT_ID_NAME + " in the pom properties input");
		}
		String version = pomProperties.getProperty(VERSION_NAME);
		if (version == null) {
			throw new MavenPomPropertiesProcessorException(
					"Can't locate " + VERSION_NAME + " in the pom properties input");
		}
		return buildIdentifiedJarVersion(groupId, artifactId, version, ScopeType.INPUT);

	}
}
