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
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import lombok.Data;

@Data
public class OptionsData {
	@Option(name = "-V", aliases = "--Verbose", usage = "Set to true to enable verpose output of intermediate stages (e.g. the mvn sub processes).  Defaults to false", metaVar = "true or false")
	public boolean verboseOutput = false;
	@Option(name = "-d", aliases = "--directory", usage = "Name of the temporary directory to use when generating temp files. Defaults to the current directory if not specified")
	public File workingDirectory = new File(".");
	@Option(name = "-g", aliases = "--groupid", usage = "The group Id to use for in the root of the  combined dependency tree", metaVar = "my.group.id")
	public String groupid = "my.company";
	@Option(name = "-a", aliases = "--artifactid", usage = "The artifact Id to use for in the root of the  combined dependency tree", metaVar = "codeproject")
	public String artifactid = "myartifact";
	@Option(name = "-v", aliases = "--version", usage = "The version to use for in the root of the  combined dependency tree", metaVar = "1.2.3+SP2")
	public String version = "0.0.0";
	@Option(name = "-m", aliases = "--maven-path", usage = "The version to use for in the root of the  combined dependency tree", metaVar = "1.2.3+SP2")
	public File mvnPath = null;
	@Option(name = "-r", aliases = "--resolve-dependencies", usage = "Set to enable mvn dependency resolution before building the dependency tree, default to false.", metaVar = "true or false")
	public boolean resolveDependencies = false;
	@Option(name = "-i", aliases = "--retain-intermediates", usage = "Set to true to retain the intermediate files, default to false", metaVar = "true or false")
	public boolean retainIntermediateFiles = false;
	@Argument(required = true, usage = "Input(s) can be pom.xml, mvn graph outputs (suffix .mvn-graphml)")
	public List<File> sources;
}