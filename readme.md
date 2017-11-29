
### flatten-maven-plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/mojohaus/versions-maven-plugin.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/flatten-maven-plugin/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.carrotgarden.maven/flatten-maven-plugin) 
[![Download](https://api.bintray.com/packages/random-maven/maven/flatten-maven-plugin/images/download.svg) ](https://bintray.com/random-maven/maven/flatten-maven-plugin/_latestVersion)

Similar plugins
* [mojohaus/flatten-maven-plugin](https://github.com/mojohaus/flatten-maven-plugin)

Plugin features
* resolves dependency version ranges
* excludes dependencies based on scope
* optionally includes transitive dependencies
* removes pom.xml members based on xml tag names
* switches deployment pom.xml with generated flattened pom.xml  

Maven goals
* [flatten:flatten](https://random-maven.github.io/flatten-maven-plugin/flatten-mojo.html)

Usage example
```xml
        <profile>
            <id>flatten</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>com.carrotgarden.maven</groupId>
                        <artifactId>flatten-maven-plugin</artifactId>
                        <configuration>
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
                            <!-- Control dependency resolution. -->
                            <performDependencyResolve>true</performDependencyResolve>
                            <includeScope>runtime</includeScope>
                            <!-- Replace pom.xml for deployment. -->
                            <performSwitchPomXml>true</performSwitchPomXml>
                            <packagingSwitchList>
                                <packaging>jar</packaging>
                            </packagingSwitchList>
                        </configuration>
                        <executions>
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
