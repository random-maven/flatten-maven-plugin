package com.carrotgarden.maven.flatten;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.dependency.resolvers.ResolveDependenciesMojo;
import org.apache.maven.plugins.dependency.utils.DependencyStatusSets;

/**
 * Resolve dependencies of current project. <br/>
 * Used internally by goal="flatten". <br/>
 * Injects resolution results into plugin context for use by other Mojo. <br/>
 * Actually invokes goal="dependency:resolve" and overrides parameter defaults:
 * <code>excludeTransitive</code>, <code>includeScope</code>,
 * <code>silent</code>, <code>sort</code>, <code>outputFile</code>. <br/>
 * See <a href=
 * "https://maven.apache.org/plugins/maven-dependency-plugin/resolve-mojo.html">maven-dependency-plugin</a>
 */
@Mojo( //
		name = "resolve", //
		defaultPhase = LifecyclePhase.PREPARE_PACKAGE, //
		requiresDependencyResolution = ResolutionScope.TEST, //
		requiresProject = true //
)
public class ResolveMojo extends ResolveDependenciesMojo implements Context {

	/**
	 * If we should exclude transitive dependencies.
	 * 
	 * Override goal="dependency:resolve"/excludeTransitive.
	 * 
	 * Default is "true", different from goal="dependency:resolve".
	 */
	@Parameter(property = "flatten.excludeTransitive", defaultValue = "true")
	boolean excludeTransitive;

	/**
	 * Scope to include.
	 * 
	 * Override goal="dependency:resolve"/includeScope.
	 * 
	 * Default is "compile", different from goal="dependency:resolve".
	 */
	@Parameter(property = "flatten.includeScope", defaultValue = "compile")
	String includeScope;

	/**
	 * If the plugin should be silent.
	 * 
	 * Override goal="dependency:resolve"/silent.
	 * 
	 * Default is "true", different from goal="dependency:resolve".
	 */
	@Parameter(property = "flatten.silent", defaultValue = "true")
	boolean silent;

	/**
	 * Sort the output list of resolved artifacts alphabetically.
	 * 
	 * Override goal="dependency:resolve"/sort.
	 * 
	 * Default is "true", different from goal="dependency:resolve".
	 */
	@Parameter(property = "flatten.sort", defaultValue = "true")
	boolean sort;

	/**
	 * If specified, this parameter will cause the dependencies to be written to the
	 * path specified, instead of writing to the console.
	 * 
	 * Override goal="dependency:resolve"/outputFile.
	 * 
	 * Default is "resolve.log", different from goal="dependency:resolve".
	 */
	@Parameter(property = "flatten.outputFile", defaultValue = "${project.build.directory}/flatten/resolve.log")
	File outputFile;

	/**
	 * Inject resolved dependencies in plugin context.
	 */
	@Override
	protected void doExecute() throws MojoExecutionException {
		try {

			// override defaults
			super.excludeTransitive = excludeTransitive;
			super.includeScope = includeScope;
			super.outputFile = outputFile;
			super.setSilent(silent);
			Support.privateFieldSet(ResolveDependenciesMojo.class, this, "sort", sort);

			// invoke "dependnecy:resolve"
			super.doExecute();
			DependencyStatusSets results = getResults();
			Set<Artifact> resolved = results.getResolvedDependencies();

			// share resolution result
			contextResolvedPersist(resolved);

		} catch (Throwable error) {
			throw new MojoExecutionException("Resolve failure", error);
		}
	}

}
