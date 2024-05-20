# Dependency Scanner

This program will scan the dependency sources it is given and build a combined dependency tree combining the separate dependency trees for the inputs. This allows the detection of conflicting dependencies. While maven (and other build tools) can of course determine conflicting dependencies in the jar files they manage (and copy into the resulting targets) this only applies at compile time, not run time, where a jar file is provided by the end user (or the end user replaces a jar file in the inputs). The goal of this program is to enable the provision of a set of the top level input jar files and the dependency trees for each will be checked for conflicting versions. For example a developer may build an application against what will ultimately be a user supplied library (the maven `provided` scope). At the compile stage the application will be compiled against the version in that build, however a user may subsequently use a different version of that library (and the jar files associated with it) If the original compiled version had a different version of the library which was replaced (or the provided version was replaced by the developer mandated version) then conflicts may occur.

## Usage
The class `com.oracle.timg.demo.dependencyanalyser.DependencyScanner` is the main class. is supports the following arguments:

  `-d`/`--directory` followed by working directory location - required when doing something that generates an intermediate file, for example extracting a pom.xml or generating a graph-xml file. In practice (with the current loaders) this basically means in all situation. Defaults to the current directory and can be absolute or relative, it must exist and of course you must be able to read and write it.

  `-g`/`--groupid` followed by groupid - Optional and used for setting the groupid at the root of the combined dependency tree, defaults to `my.company` if not set. Recommended if listing jar files or similar so you can determine what the actual tree root will be in the output
  
  `-a`/`--artifactid`  followed by artifactid  - Optional and used for setting the artifact at the root of the combined dependency tree, defaults to `myartifact` if not set. Recommended if listing jar files or similar so you can determine what the actual tree root will be in the output
  
  `-v`/`--version` followed by version string  - Optional and used for setting the version at the root of the combined dependency tree, defaults to `0.0.0` if not set. Recommended if listing jar files or similar so you can determine what the actual tree root will be in the output
  
  `-m`/`--maven-path` followed by path - Required for all sources that are maven based (so pom.xmnl files of jar files with a ponm.xml contents. Specified the location of the mvn executabler. Note that this **MUST** be the full absolute path, there is no default as this is system dependent.
  
  `-r`/`--resolve-dependencies` - If set will call any pre-resolving task needed before generating dependency data, used for example to ensure that the local mavan repo has ll of the dependencies downloaded. Defaults to false.
  
  `-i`/`--retain-intermediates` - If set will retain the intermediate files (e.g. extracted pom.xml) used for the processing stages, this can be useful for debugging purposes. Defaults to false.
  
  `-V`/`--Verbose` - outputs info re the sub processes that are run (e.g. command lines being used, output from the sub processes)
  
  
  The remaining arguments are treated as input sources. Currently the following are supported.
  
  Files with the suffix `.mvn-graphml.xml` These are assumed to contain a GraphML "dump" of the dependencies in the format that Maven outputs. The file will be directly loaded and to locate the root of the dependency tree all loaded jars are scanned to find a jar version which does not depend on any other source. This resulting tree will be added to the combined dependencies tree. The root of this source will be created as a sub node under the combined root tree and will have it's scope set to 'ROOT',
  
  Files with the name `pom.xml` - These files will be passed to maven to create a maven dependency graqph file (with the suffix `.mvn-graphml.xml`) which will then be loaded and added to the combined dependencies tree using the process above if the `-r` flag is set then maven will be asked to do a dependency resolve stage before processing. The .mvn-graplml.xml file will be generated using a temporary name based on thye grupid, artifactid and version located in the ponm.xml file and will be written to the location specified by the `-d` flag, if the `-i` flag is set this temporary file will be retained, otherwise it will be deleted when the jvm exits. If the `-V` flag is set the the command(s) used to run maven and any maven output will be displayed.
  
  Files with the suffix `.jar` are examined to see if they contain a `pom.xml` file and optionally a `pom.properties` file. These are expected to be under the `META-INF/maven/<groupid>/<artifactid>` "directory" in the jar file (If Maven is packaging the jar file it will copy the files there automatically, Gradle seem to also generate at least a `pom.xml` file which is places there as well). If present the `pom.properties` file will be used to get the groupid, artifactid and version, if `pom.properties` is not present these will be extracted from the `pom.xml` entri. The `META-INF/maven/<groupid>/<artifactid>/pom.xml` entry in the jar file will then be extracted and copied into a temporaty file and then the process above is done as with a "normal" pom.xml file. 
  
## Outputs

  The DependencyScanner class is provided as an example, it loads each of the source arguments in turn and then generates the output. There are a number of utility medhods in the JarTree class that can be used to locate jar file versions based on different criteria and also to dump the dependeny tree.
  
  Count of the number of Jar files where there are two or move versions in the combined dependency tree
  
  Information on each oif the jar files with multiple versions including the jar file versions that depend in this version, any jar file versions that they depend on and also details of the original source jar file version to help identify wherte conflicting versions originate from (this maps to the input sources).
  
  It will also list the number of jar file versions that have no dependencies (i.e. "leaf" jar file versions) and also the bymber of jar file versions. Depending on the other flags and arguments other output may of course be provided. 
  
  Below is the core output from test code. This is deliberately using different versions of Lombok, please note that the number of jar file versions with nothign depending should always be one (this being the "root / project" dependency.
  
  ```text
  May 20, 2024 2:42:23 PM com.oracle.timg.demo.dependencyanalyser.DependencyScanner main
INFO: Count of combined Jar files with two or more versions
3
May 20, 2024 2:42:23 PM com.oracle.timg.demo.dependencyanalyser.DependencyScanner main
INFO: Details of jar files with multiple versions
org.projectlombok:lombok
    1.18.30 refs = 1
        Depends on
        Depended on by
            com.oracle.labs.helidon.fileio:filewriter:1.0.0 (PROVIDED)
        In source
            com.oracle.labs.helidon.fileio:filewriter:1.0.0 from origional source testdata/filewriter.mvn-graphml.xml

    1.18.32 refs = 2
        Depends on
        Depended on by
            com.oracle.timg.demo:dependencyanalyser:0.0.1-SNAPSHOT (PROVIDED)
            com.oracle.labs.helidon.fileio:filereader:1.0.0 (PROVIDED)
        In source
            com.oracle.timg.demo:dependencyanalyser:0.0.1-SNAPSHOT from origional source testdata/pom.xml
            com.oracle.labs.helidon.fileio:filereader:1.0.0 from origional source testdata/filereader.mvn-graphml.xml

org.apiguardian:apiguardian-api
    1.0.0 refs = 1
        Depends on
        Depended on by
            com.oracle.labs.helidon.fileio:filereader:1.0.0 (COMPILE)
        In source
            com.oracle.labs.helidon.fileio:filereader:1.0.0 from origional source testdata/filereader.mvn-graphml.xml

    1.1.2 refs = 1
        Depends on
        Depended on by
            org.junit.jupiter:junit-jupiter-api:5.9.3 (TEST)
        In source
            com.oracle.labs.helidon.fileio:filewriter:1.0.0 from origional source testdata/filewriter.mvn-graphml.xml

jakarta.activation:jakarta.activation-api
    1.2.2 refs = 1
        Depends on
        Depended on by
            jakarta.xml.bind:jakarta.xml.bind-api:2.3.3 (COMPILE)
        In source
            com.oracle.timg.demo:dependencyanalyser:0.0.1-SNAPSHOT from origional source testdata/pom.xml

    2.1.1 refs = 2
        Depends on
        Depended on by
            com.oracle.labs.helidon.fileio:filewriter:1.0.0 (COMPILE)
            com.oracle.labs.helidon.fileio:filereader:1.0.0 (COMPILE)
        In source
            com.oracle.labs.helidon.fileio:filewriter:1.0.0 from origional source testdata/filewriter.mvn-graphml.xml


May 20, 2024 2:42:23 PM com.oracle.timg.demo.dependencyanalyser.DependencyScanner main
INFO: Combined Jar versions with no dependencies 87
May 20, 2024 2:42:23 PM com.oracle.timg.demo.dependencyanalyser.DependencyScanner main
INFO: Combined Jar versions with no depended on 1
```
  
  

## Known limitations

This program uses Mavan to build the per project tree, the program then loads the maven dependency output and combines it with all other supplied outputs. Of course this means that the mvn command needs to be available. it also means that it is subject to mavens restrictions and limitations. Known factors that may cause a problem are :

1. You need to have resolved all the dependencies in any pom.xml (or jar file containing it) to do that the program has the option of running a maven resolve stage (set the `-r` flag), this can take a while and you will need a network connection and enough space on the machine you are using to download the dependencies.

2. Maven supports the operation of relative parent pom.xml, for some projects where the jar file (or pom.xml) is not in the local maven repository this may be a problem as the parent pom may not be in the right place in the directory tree, in some situations where there happens to be a pom.xml (not not the expected one) in the location specified by the relativePath this will result in maven loading the incorrect parent pom.
 
3. Of course if mvn output changes (or the mvn command and flags change) then this will be a problem. 

## Extending the code

Is is architecturally possible to add additional sources of dependency information. The new loader needs to implement the SourceLoader interface, the GetSourceLoader class needs to be updated to recognize (by whatever means you like) the new source type and return an instance of the appropriate loader. 

The loadDependencied method needs to return a new Jar Tree which will then be combined with all of the others that have been built. The new Jar Tree has the following criteria.

The getRoot method must return an IdenfitiedJarFileVersion which is the root of the new dependency structure, this must not have anything depending on it.

This must be a tree, no loops are allowed (this is really a limitation of the output mechanisms)

Every IdenfitiedJarFileVersion in the tree must contain the tree being built in the sourceProjecxtrs set (this will be combined with the other sources later and used in the output to trace the source of multiple dependencies
