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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public interface XMLLoader {
	public final static XPath xPath = XPathFactory.newInstance().newXPath();

	public default Document readXMLDocumentFromFile(File sourceFile) throws XMLProcessorException {
		System.err.println("Abput to read XML document from " + sourceFile.getPath());
		try {
			return readXMLDocumentFromFile(new BufferedInputStream(new FileInputStream(sourceFile)));
		} catch (FileNotFoundException e) {
			throw new XMLProcessorException("Cant locate file " + sourceFile.getPath());
		}
	}

	public default Document readXMLDocumentFromFile(InputStream sourceInputStream) throws XMLProcessorException {

		// Get Document Builder, note that this DOES NOT create namespace aware
		// documents as doing so seems to cause problems with xpath
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new XMLProcessorException("Can't create XML Document parser " + e.getLocalizedMessage(), e);
		}

		// Build Document
		Document document;
		try {
			document = builder.parse(sourceInputStream);
		} catch (SAXException e) {
			throw new XMLProcessorException("XML Document structure exception " + e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new XMLProcessorException("Can't create XML input stream " + e.getLocalizedMessage(), e);
		}

		// Normalize the XML Structure; It's just too important !!
		document.getDocumentElement().normalize();

		return document;
	}

	public default XPathExpression compileXPath(String xPathExpression) throws XMLProcessorException {
		try {
			return xPath.compile(xPathExpression);
		} catch (XPathExpressionException e) {
			throw new XMLProcessorException(
					"Specified xpath " + xPathExpression + " has a compile error " + e.getLocalizedMessage());
		}
	}

	public default String getNodeContentsByXPath(Node startNode, String xPathExpression) throws XMLProcessorException {
		try {
			return getNodeContentsByXPath(startNode, xPath.compile(xPathExpression));
		} catch (XPathExpressionException e) {
			throw new XMLProcessorException("Provided xpath expression is invalid, " + e.getLocalizedMessage(), e);
		}
	}

	public default String getNodeContentsByXPath(Node startNode, XPathExpression xPathExpression)
			throws XMLProcessorException {
		NodeList list = getNodeListByXPath(startNode, xPathExpression);
		if (list.getLength() == 0) {
			return null;
		}
		Node n = list.item(0);
		return n.getTextContent();
	}

	public default NodeList getNodeListByXPath(Node startNode, String xPathExpression) throws XMLProcessorException {
		if (startNode.getNodeType() == Node.ELEMENT_NODE) {
			return getNodeListByXPath((Element) startNode, xPathExpression);
		} else {
			throw new XMLProcessorException("Provided node is not of type element." + startNode.getTextContent());
		}
	}

	public default NodeList getNodeListByXPath(Element startNode, String xPathExpression) throws XMLProcessorException {
		try {
			return getNodeListByXPath(startNode, xPath.compile(xPathExpression));
		} catch (XPathExpressionException e) {
			throw new XMLProcessorException("Provided xpath expression is invalid, " + e.getLocalizedMessage(), e);
		}
	}

	public default NodeList getNodeListByXPath(Node startNode, XPathExpression xPathExpression)
			throws XMLProcessorException {
		if (startNode.getNodeType() == Node.ELEMENT_NODE) {
			return getNodeListByXPath((Element) startNode, xPathExpression);
		} else {
			throw new XMLProcessorException("Provided node is not of type element." + startNode.getTextContent());
		}
	}

	public default NodeList getNodeListByXPath(Element startNode, XPathExpression xPathExpression)
			throws XMLProcessorException {
		try {
			return (NodeList) xPathExpression.evaluate(startNode, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			throw new XMLProcessorException("Problem processing the xpath " + e.getLocalizedMessage(), e);
		}
	}
}
