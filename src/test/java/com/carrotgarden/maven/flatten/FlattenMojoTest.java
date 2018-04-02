package com.carrotgarden.maven.flatten;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FlattenMojoTest {

	@Test
	public void testRegex() throws Exception {

		// Model model = new Model();
		//
		// File resolveListFile = new File("src/test/resources/dependency.list");
		// String dependencyRegex = FlattenMojo.DEPENDENCY_REGEX;
		//
		// FlattenMojo mojo = new FlattenMojo();
		// mojo.resolveListFile = resolveListFile;
		// mojo.dependencyRegex = dependencyRegex;
		//
		// List<Dependency> list = mojo.parseDependency(model);
		//
		// String source = list.toString();
		//
		// String target = "[Dependency {groupId=org.scala-lang.modules,
		// artifactId=scala-xml_2.12, version=1.0.6, type=jar}, Dependency
		// {groupId=org.sonatype.plexus, artifactId=plexus-build-api, version=0.0.7,
		// type=jar}]";
		//
		// System.out.println("list=" + list);
		//
		// assertEquals(source, target);

	}

}
