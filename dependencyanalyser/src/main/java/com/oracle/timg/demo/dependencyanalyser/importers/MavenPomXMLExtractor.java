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
import java.io.InputStream;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarVersion;
import com.oracle.timg.demo.dependencyanalyser.ScopeType;

import lombok.NonNull;
import lombok.extern.java.Log;

@Log
public class MavenPomXMLExtractor implements XMLLoader, MavenPomDetailsBuilder {
	public final static String PARENT_NAME = "parent";
	public final static String PROPERTIES_NAME = "properties";
	public final static String GROUP_ID_NAME = "groupId";
	public final static String ARTIFACT_ID_NAME = "artifactId";
	public final static String VERSION_NAME = "version";
	private static XPathExpression GROUP_ID_XPATH, ARTIFACT_ID_XPATH, VERSION_XPATH;
	private static XPathExpression GROUP_ID_PARENT_XPATH, ARTIFACT_ID_PARENT_XPATH, VERSION_PARENT_XPATH;
	private Document document;
	private Element documentRoot;
	private String sourceName;

	private MavenPomXMLExtractor(String sourceName) throws XMLProcessorException {
		this.sourceName = sourceName;
		// only need these once
		if (GROUP_ID_XPATH == null) {
			GROUP_ID_XPATH = compileXPath("./" + GROUP_ID_NAME);
			ARTIFACT_ID_XPATH = compileXPath("./" + ARTIFACT_ID_NAME);
			VERSION_XPATH = compileXPath("./" + VERSION_NAME);
			GROUP_ID_PARENT_XPATH = compileXPath("./" + PARENT_NAME + "/" + GROUP_ID_NAME);
			ARTIFACT_ID_PARENT_XPATH = compileXPath("./" + PARENT_NAME + "/" + ARTIFACT_ID_NAME);
			VERSION_PARENT_XPATH = compileXPath("./" + PARENT_NAME + "/" + VERSION_NAME);
		}
	}

	public MavenPomXMLExtractor(@NonNull File sourceFile) throws XMLProcessorException {
		this(sourceFile.getAbsolutePath());
		document = readXMLDocumentFromFile(sourceFile);
		documentRoot = document.getDocumentElement();
	}

	public MavenPomXMLExtractor(@NonNull InputStream sourceInputStream) throws XMLProcessorException {
		this("Source is input stream");
		document = readXMLDocumentFromFile(sourceInputStream);
		documentRoot = document.getDocumentElement();
	}

	public IdentifiedJarVersion getJarVersion() throws XMLProcessorException {
		// maven allows you to inherit any of groupid, artifactid or version from the
		// parent, though it also seems that some IDE's (e.g. eclipse) require at least
		// one of groupId or artifactId to be set for the actuall artifact
		String groupId = getNodeContentsByXPath(documentRoot, GROUP_ID_XPATH);
		if (groupId == null) {
			// try to get the parent artifact id and use that
			groupId = getNodeContentsByXPath(documentRoot, GROUP_ID_PARENT_XPATH);
			if (groupId == null) {
				throw new XMLProcessorException("Can't locate " + GROUP_ID_NAME
						+ " in the document (not set for the pom.xml for the artifact or parent");
			}
			log.finer("for " + sourceName + " Located " + GROUP_ID_NAME + " in parent details");
		} else {
			log.finer("for " + sourceName + " Located " + GROUP_ID_NAME + " in artifact details");
		}
		String artifactId = getNodeContentsByXPath(documentRoot, ARTIFACT_ID_XPATH);
		if (artifactId == null) {
			// try to get the parent artifact id and use that
			artifactId = getNodeContentsByXPath(documentRoot, ARTIFACT_ID_PARENT_XPATH);
			if (artifactId == null) {
				throw new XMLProcessorException("Can't locate " + ARTIFACT_ID_NAME
						+ " in the document (not set for the pom.xml for the artifact or parent");
			}
			log.finer("for " + sourceName + " Located " + ARTIFACT_ID_NAME + " in parent details");
		} else {
			log.finer("for " + sourceName + " Located " + ARTIFACT_ID_NAME + " in artifact details");
		}
		String version = getNodeContentsByXPath(documentRoot, VERSION_XPATH);
		if (version == null) {
			// try to get the parent version and use that
			version = getNodeContentsByXPath(documentRoot, VERSION_PARENT_XPATH);
			if (version == null) {
				throw new XMLProcessorException("Can't locate " + VERSION_NAME
						+ " in the document (not set for the pom.xml for the artifact or parent");
			}
			log.finer("for " + sourceName + " Located " + VERSION_NAME + " in parent details");
		} else {
			log.finer("for " + sourceName + " Located " + VERSION_NAME + " in artifact details");
		}
		return buildIdentifiedJarVersion(groupId, artifactId, version, ScopeType.INPUT);
	}

	public String getVariable(String varName) throws XMLProcessorException {
		// if it's an absolute path just go to it
		String varValue = null;
		if (varName.startsWith("/") || varName.startsWith(".")) {
			return getNodeContentsByXPath(documentRoot, compileXPath(varName));
		}
		// OK it's not an absolute or relative path
		// try looking in the properties
		String varPath = "/" + PROPERTIES_NAME + "/" + varName;
		varValue = getNodeContentsByXPath(documentRoot, compileXPath(varPath));
		if (varValue != null) {
			return varValue;
		}
		// that didn't work, try looking in general from the root (e.g. it's
		// parent.version)
		varValue = getNodeContentsByXPath(documentRoot, compileXPath("/" + varName));
		// result will be null or the value
		return varValue;
	}

	public String getXPathWithVarSubstitution(String xPath) throws XMLProcessorException {
		String value = getNodeContentsByXPath(documentRoot, compileXPath(xPath));
		// if it wasn't there return null
		if (value == null) {
			return null;
		}
		// if it is there AND starts with ${ and ends with } then try and lookup the
		// value
		if (!(value.startsWith("${") && value.endsWith("}"))) {
			// it's not a variable, return it directly
			return value;
		}
		// try and get it as a property
		String varName = value.substring(2, value.length() - 1);
		String varPath = "/" + PROPERTIES_NAME + "/" + varName;
		value = getNodeContentsByXPath(document, compileXPath(varPath));

		if (value != null) {
			// we found is in the properties
			return value;
		}
		// that didn't work but it's still a variable, try looking for the variable name
		// in the general document
		value = getNodeContentsByXPath(document, compileXPath(varName));
		// result will be null or the value
		return value;
	}
}
