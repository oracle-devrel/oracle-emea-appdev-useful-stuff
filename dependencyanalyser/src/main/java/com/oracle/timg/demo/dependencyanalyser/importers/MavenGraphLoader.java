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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oracle.timg.demo.dependencyanalyser.Dependency;
import com.oracle.timg.demo.dependencyanalyser.DependencyProcessorException;
import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarFile;
import com.oracle.timg.demo.dependencyanalyser.IdentifiedJarVersion;
import com.oracle.timg.demo.dependencyanalyser.JarTree;
import com.oracle.timg.demo.dependencyanalyser.OptionsData;
import com.oracle.timg.demo.dependencyanalyser.ScopeType;

import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * Loads the graph output of maven into the jartree to generate the mvn grapu :
 * 'mvn -f <pom.xml> dependency:tree -DoutputType=graphml -DoutputFile=<output
 * file name>' Always write the output to a file, dumping it to std out is a
 * prioblem as mvn puts a bunch or
 */
@Log
public class MavenGraphLoader implements SourceLoader, XMLLoader {
	public final static String GRAPH_ELEMENT_NAME = "graph";
	public final static String DATA_ELEMENT_NAME = "data";
	public final static String NODE_ELEMENT_NAME = "node";
	public final static String EDGE_ELEMENT_NAME = "edge";
	public final static String SHAPE_NODE_ELEMENT_NAME = "ShapeNode";
	public final static String NODE_LABEL_ELEMENT_NAME = "NodeLabel";
	public final static String SOURCE_ATTRIBUTE_NAME = "source";
	public final static String TARGET_ATTRIBUTE_NAME = "target";
	public final static String POLY_LINE_EDGE_ELEMENT_NAME = "PolyLineEdge";
	public final static String EDGE_LABEL_ELEMENT_NAME = "EdgeLabel";

	// the xpaths we care about
	private final XPathExpression ALL_GRAPHS_XPATH, ALL_NODES_XPATH, ALL_EDGES_XPATH, NODE_DATA_SHAPE_NODE_XPATH,
			EDGE_DATA_POLY_EDGE_LABEL_XPATH;
	// stash this for now in case in the future we need to look into it
	protected final OptionsData optionsData;

	public MavenGraphLoader(OptionsData optionsData) throws XMLProcessorException {
		this.optionsData = optionsData;
		// have to do this here rather than in the variables declaration to handle the
		// exceptions
		ALL_GRAPHS_XPATH = compileXPath("//" + GRAPH_ELEMENT_NAME);
		// these two are relative to the graph so need a relative xpath
		ALL_NODES_XPATH = compileXPath("./" + NODE_ELEMENT_NAME);
		ALL_EDGES_XPATH = compileXPath("./" + EDGE_ELEMENT_NAME);
		NODE_DATA_SHAPE_NODE_XPATH = compileXPath(
				"./" + DATA_ELEMENT_NAME + "/" + SHAPE_NODE_ELEMENT_NAME + "/" + NODE_LABEL_ELEMENT_NAME);
		EDGE_DATA_POLY_EDGE_LABEL_XPATH = compileXPath(
				"./" + DATA_ELEMENT_NAME + "/" + POLY_LINE_EDGE_ELEMENT_NAME + "/" + EDGE_LABEL_ELEMENT_NAME);
	}

	@Override
	public JarTree loadDependencyTree(File sourceFile, File origionalSourceFile) throws DependencyProcessorException {
		JarTree jarTree = new JarTree(origionalSourceFile);
		Map<String, IdentifiedJarVersion> idsToJarVersion = new HashMap<>();
		int jarNodesCount = 0;
		int dependencyCount = 0;
		int processedNodes = 0;
		int processedDependencies = 0;
		Document document;
		try {
			document = readXMLDocumentFromFile(sourceFile);
		} catch (XMLProcessorException e) {
			throw new MavenGraphProcessorException("Problem loading the Maven graphxml file " + sourceFile.getPath(),
					e);
		}
		Element documentRoot = document.getDocumentElement();
		log.fine("Document root is " + documentRoot.getNodeName());
		NodeList graphs = getNodeListByXPath(documentRoot, ALL_GRAPHS_XPATH);
		log.fine("There are " + graphs.getLength() + "+ graphs in the document");
		// seems that the xml stuff doesn't support iterators or anything useful so have
		// to do this the old fashioned way
		for (int graphCounter = 0; graphCounter < graphs.getLength(); graphCounter++) {
			Node graph = graphs.item(graphCounter);
			if (graph.getNodeType() == Node.ELEMENT_NODE) {
				Element graphElement = (Element) graph;
				String graphId = graphElement.getAttribute("id");
				NodeList graphNodes = getNodeListByXPath(graphElement, ALL_NODES_XPATH);// graphElement.getElementsByTagName("node");
				jarNodesCount = graphNodes.getLength();
				NodeList graphEdges = getNodeListByXPath(graphElement, ALL_EDGES_XPATH);// graphElement.getElementsByTagName("edge");
				dependencyCount = graphEdges.getLength();
				log.info(
						"Graph " + graphId + " contains " + jarNodesCount + " nodes and " + dependencyCount + " edges");
				// first create entries for each of the nodes, this will be a IdentifiedJarFile
				// and then a
				// IdentifiedJarVersion. at this point we aren't linking them
				for (int nodeCounter = 0; nodeCounter < graphNodes.getLength(); nodeCounter++) {
					Node jarNode = graphNodes.item(nodeCounter);
					if (jarNode.getNodeType() == Node.ELEMENT_NODE) {
						processedNodes++;
						addJarNode(sourceFile, jarTree, idsToJarVersion, (Element) jarNode);
					} else {
						log.warning("Node " + jarNode + " should be jar file details, but it's not");
					}
				}
				for (int dependencyCounter = 0; dependencyCounter < graphEdges.getLength(); dependencyCounter++) {
					Node dependencyNode = graphEdges.item(dependencyCounter);
					if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
						processedDependencies++;
						addDependency(sourceFile, jarTree, idsToJarVersion, (Element) dependencyNode);
					} else {
						log.warning("Node " + dependencyNode + " should be dependency details, but it's not");
					}
				}
				log.fine("Graph " + graphId + " Processed : " + processedNodes + " jar files with " + jarTree.size()
						+ " jar files added");
				log.fine("Graph " + graphId + " Processed : " + processedDependencies);
			}
		}
		// look for the root, this will have nothing depending on it
		Set<IdentifiedJarVersion> noDependedOn = jarTree.getJarVersionsWithNoDependOn();
		if (noDependedOn.size() == 0) {
			log.severe("Danger, unable to locate a jar version with no dependencies, this jar tree is unusable");
			return null;
		}
		if (noDependedOn.size() >= 2) {
			log.warning(
					"There are multiple jar versions which have nothing depending on them, selecting the first as the root, this may not be valid");
		}
		jarTree.setRoot(noDependedOn.toArray(new IdentifiedJarVersion[0])[0]);
		return jarTree;
	}

	private void addJarNode(@NonNull File sourceFile, @NonNull JarTree jarTree,
			Map<String, IdentifiedJarVersion> idsToJarVersion, @NonNull Element jarNode) throws XMLProcessorException {
		// node data looks like this <node id="895812217"><data
		// key="d0"><y:ShapeNode><y:NodeLabel>io.helidon.common.features:helidon-common-features:jar:4.0.6:compile</y:NodeLabel></y:ShapeNode></data></node>
		// get the id
		String versionId = jarNode.getAttribute("id").trim();
		NodeList nodeLabelList = getNodeListByXPath(jarNode, NODE_DATA_SHAPE_NODE_XPATH);
		Node nodeLabelNode = nodeLabelList.item(0);
		String jarInfo = nodeLabelNode.getTextContent().trim();
		String[] jarInfoSplit = jarInfo.split(":");
		if (jarInfoSplit.length < 4) {
			log.warning("Jar details " + jarInfo + "+ in node " + jarNode.getTextContent()
					+ " does not containe the minimum required 4 fields");
			return;
		}
		String groupId = jarInfoSplit[0];
		String artifactId = jarInfoSplit[1];
		// unknown what options there are for type
		String type = jarInfoSplit[2];
		String version = jarInfoSplit[3];
		// the scope is part of the jar info, but I can;t see why as it's the dependency
		// elements that count for it. if it's missing then just set to unknown
		String scopeString = jarInfoSplit.length >= 5 ? jarInfoSplit[4] : "unknown";
		ScopeType scopeType;
		try {
			scopeType = ScopeType.getScope(scopeString);
		} catch (IllegalArgumentException e) {
			log.fine("Loading graphml data from " + sourceFile.getAbsolutePath() + ", node with id " + versionId
					+ " scope " + scopeString + " is not a known scope, defaulting to " + ScopeType.UNKNOWN);
			scopeType = ScopeType.UNKNOWN;
		}

		// we know what the jar stuff looks like, let's try and find it

		// now save the jar file in the map under it's id
		String jarFileInfo = IdentifiedJarFile.processString(groupId, artifactId).trim();
		// do we have this already ?
		IdentifiedJarFile identifiedJarFile = jarTree.get(jarFileInfo.trim());
		if (identifiedJarFile == null) {
			// no existing entry create it
			identifiedJarFile = new IdentifiedJarFile(groupId, artifactId);
			jarTree.put(jarFileInfo.trim(), identifiedJarFile);
		}
		// does the jar file have this version ?
		IdentifiedJarVersion identifiedJarVersion = identifiedJarFile.get(version.trim());
		if (identifiedJarVersion == null) {
			identifiedJarVersion = new IdentifiedJarVersion(version, identifiedJarFile, type, scopeType);
			identifiedJarVersion.addSourceProject(jarTree);
			identifiedJarFile.put(version.trim(), identifiedJarVersion);
		}
		// save the info away to grab it later when the dependencies are processed
		IdentifiedJarVersion oldJarVersion = idsToJarVersion.put(versionId.trim(), identifiedJarVersion);
		if (oldJarVersion != null) {
			log.severe("Jar version with id " + versionId
					+ " was already in the map, this is an export error\nOrigional Jar Version " + oldJarVersion
					+ "\nNew Jar version " + identifiedJarVersion);
		}
	}

	private void addDependency(@NonNull File sourceFile, JarTree jarTree,
			Map<String, IdentifiedJarVersion> idsToJarVersion, Element jarDependency)
			throws MavenGraphProcessorException {
		// now let's scan the list of dependencies and pull those together
		// the dependencies are simpler
		// <edge source="895812217" target="2040609056"><data
		// key="d1"><y:PolyLineEdge><y:EdgeLabel>compile</y:EdgeLabel></y:PolyLineEdge></data></edge>
		// we care about are the source and target and the label (compile etc) as that's
		// the scope info
		String sourceId = jarDependency.getAttribute(SOURCE_ATTRIBUTE_NAME);
		if (sourceId == null) {
			log.warning("Dependency " + jarDependency.getTextContent() + " does not include a source attribute");
		}

		String targetId = jarDependency.getAttribute(TARGET_ATTRIBUTE_NAME);
		if (targetId == null) {
			log.warning("Dependency " + jarDependency.getTextContent() + " does not include a target attribute");
		}
		NodeList targetlList;
		try {
			targetlList = getNodeListByXPath(jarDependency, EDGE_DATA_POLY_EDGE_LABEL_XPATH);
		} catch (XMLProcessorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		if (targetlList.getLength() == 0) {
			log.warning("Node " + jarDependency.getTextContent() + " " + DATA_ELEMENT_NAME + " "
					+ POLY_LINE_EDGE_ELEMENT_NAME + " section does not include a " + EDGE_LABEL_ELEMENT_NAME
					+ " section");
			return;
		}
		Node edgeLabelNode = targetlList.item(0);
		String edgeLabelText = edgeLabelNode.getTextContent();

		// try and locate the source and target jar versions
		IdentifiedJarVersion sourceJarVersion = idsToJarVersion.get(sourceId.trim());
		if (sourceJarVersion == null) {
			log.warning("Dependency " + jarDependency.getTextContent() + " has a source id which is not known");
			return;
		}
		IdentifiedJarVersion targetJarVersion = idsToJarVersion.get(targetId.trim());
		if (targetJarVersion == null) {
			log.warning("Dependency " + jarDependency.getTextContent() + " has a target id which is not known");
			return;
		}
		ScopeType dependencyScope;
		try {
			dependencyScope = ScopeType.getScope(edgeLabelText);
		} catch (IllegalArgumentException e) {
			log.warning("Loading graphml data from " + sourceFile.getAbsolutePath() + ", edge with sourceId " + sourceId
					+ " to target id " + targetId + " scope " + edgeLabelText + " is not a known scope, defaulting to "
					+ ScopeType.UNKNOWN);
			dependencyScope = ScopeType.UNKNOWN;
		}
		// create the dependency info, this will seutp the links in the source and
		// target for us as well.
		Dependency dependency = new Dependency(sourceJarVersion, targetJarVersion, dependencyScope);
		// add the dependencies in place
		sourceJarVersion.getDependsOn().add(dependency);
		targetJarVersion.getDependsOnThis().add(dependency);
	}

}
