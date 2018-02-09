
### Flatten Maven Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/flatten-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/flatten-maven-plugin) 
[![Download](https://api.bintray.com/packages/random-maven/maven/flatten-maven-plugin/images/download.svg) ](https://bintray.com/random-maven/maven/flatten-maven-plugin/_latestVersion)
[![Travis Status](https://travis-ci.org/random-maven/flatten-maven-plugin.svg?branch=master)](https://travis-ci.org/random-maven/flatten-maven-plugin/builds)


Similar plugins
* [mojohaus/flatten-maven-plugin](https://github.com/mojohaus/flatten-maven-plugin)

Plugin features
* replaces published identity
* resolves dependency version ranges
* excludes dependencies based on scope
* optionally includes transitive dependencies
* removes `pom.xml` members based on xml tag names
* switches project `pom.xml` with generated `pom.xml.flatten`  

Maven goals
* [flatten:flatten](https://random-maven.github.io/flatten-maven-plugin/flatten-mojo.html)

### Plugin demo

Compare results
* original [pom.xml](https://raw.githubusercontent.com/random-maven/flatten-maven-plugin/master/demo/pom.xml)
* generated [pom.xml.flatten](https://raw.githubusercontent.com/random-maven/flatten-maven-plugin/master/demo/pom.xml.flatten)

### Usage examples

Test projects
* basic project [it/test-1](https://github.com/random-maven/flatten-maven-plugin/blob/master/src/it/test-1/pom.xml)
* scala identity [it/test-2](https://github.com/random-maven/flatten-maven-plugin/blob/master/src/it/test-2/pom.xml)

####  `flatten:flatten` - produce deployment `pom.xml.flatten`

```
mvn clean package -P flatten
```

```xml
        <profile>
            <id>flatten</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>com.carrotgarden.maven</groupId>
                        <artifactId>flatten-maven-plugin</artifactId>
                        <configuration>

                            <!-- Control dependency resolution. -->
                            <performDependencyResolve>true</performDependencyResolve>
                            <includeScope>runtime</includeScope>
                            <excludeTransitive>false</excludeTransitive>

                            <!-- Remove these pom.xml members. -->
                            <performRemoveMembers>true</performRemoveMembers>
                            <memberRemoveList>
                                <member>parent</member>
                                <member>properties</member>
                                <member>distributionManagement</member>
                                <member>dependencyManagement</member>
                                <member>repositories</member>
                                <member>pluginRepositories</member>
                                <member>build</member>
                                <member>profiles</member>
                                <member>reporting</member>
                            </memberRemoveList>

                            <!-- Change published artifact identity. -->
                            <performOverrideIdentity>true</performOverrideIdentity>
                            <overrideArtifactId>${project.artifactId}_2.12</overrideArtifactId>

                            <!-- Switch project from pom.xml to pom.xml.flatten. -->
                            <performSwitchPomXml>true</performSwitchPomXml>
                            <packagingSwitchList>
                                <packaging>jar</packaging>
                            </packagingSwitchList>

                        </configuration>
                        <executions>
                            <!-- Activate "flatten:flatten" during "prepare-package" -->
                            <execution>
                                <goals>
                                    <goal>flatten</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
```

### Build yourself

```
cd /tmp
git clone git@github.com:random-maven/flatten-maven-plugin.git
cd flatten-maven-plugin
./mvnw.sh clean install -B -P skip-test
```
