package com.carrotgarden.maven.flatten;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;
import static org.codehaus.plexus.util.StringUtils.*;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Simplify Maven project pom.xml descriptor for publication.
 */
@Mojo(name = "flatten", //
		defaultPhase = LifecyclePhase.PREPARE_PACKAGE, //
		requiresDependencyResolution = ResolutionScope.TEST, //
		requiresProject = true)
public class FlattenMojo extends AbstractMojo {

	/**
	 * Default pattern to parse dependency:resolve report.
	 */
	static final String DEPENDENCY_REGEX = "^[\\s]*([^:\\s]+):([^:\\s]+):([^:\\s]+):([^:\\s]+):([^:\\s]+)[\\s]*.*$";

	/**
	 * Current maven project.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	/**
	 * Current maven session.
	 */
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	MavenSession session;

	/**
	 * Maven build plugin manager component.
	 */
	@Component()
	BuildPluginManager manager;

	/**
	 * Eclipse integration context.
	 */
	@Component()
	BuildContext buildContext;
	/**
	 * Flag to skip this goal execution.
	 */
	@Parameter(property = "flatten.skip", defaultValue = "false")
	boolean skip;

	/**
	 * List of pom.xml model members to remove. Used by
	 * {@link #performRemoveMembers}.
	 */
	@Parameter(property = "flatten.memberRemoveList")
	String[] memberRemoveList = new String[] {};

	/**
	 * Erase dependencies based on artifact scope. Used by
	 * {@link #performEraseScopes}.
	 */
	@Parameter(property = "flatten.scopeEraseList", defaultValue = "test")
	String[] scopeEraseList = new String[] {};

	/**
	 * List of project packaging types that trigger pom.xml switch for publication.
	 * Used by {@link #performSwitchPomXml}.
	 */
	@Parameter(property = "flatten.packagingSwitchList", defaultValue = "jar,war,ear,bundle,maven-plugin")
	String[] packagingSwitchList = new String[] {};

	/**
	 * Absolute file path for the generated flattened pom.xml. Main output produced
	 * by this goal.
	 */
	@Parameter(property = "flatten.targetPomFile", defaultValue = "${project.build.directory}/flatten/pom.xml.flatten")
	File targetPomFile;

	/**
	 * Absolute file path for the resolved artifact dependency list. Used by
	 * {@link #performDependencyResolve}.
	 */
	@Parameter(property = "flatten.resolveListFile", defaultValue = "${project.build.directory}/flatten/dependency.list")
	File resolveListFile;

	/**
	 * Behavior of {@link #performDependencyResolve}: artifact scope descriptor to
	 * include in the dependency resolution.
	 */
	@Parameter(property = "flatten.includeScope", defaultValue = "compile")
	String includeScope;

	/**
	 * Behavior of {@link #performDependencyResolve}: exclude transitive dependency
	 * artifacts from the resolution result.
	 */
	@Parameter(property = "flatten.excludeTransitive", defaultValue = "true")
	boolean excludeTransitive;

	/**
	 * Execution step 1. Invoke {@code maven-dependency-plugin:resolve} to resolve
	 * and filter project dependencies with {@link #includeScope},
	 * {@link #excludeTransitive}. Alternative to step 2.
	 * 
	 * @see <a hfef=
	 *      "https://maven.apache.org/plugins/maven-dependency-plugin/resolve-mojo.html">maven-dependency-plugin:resolve</a
	 */
	@Parameter(property = "flatten.performDependencyResolve", defaultValue = "true")
	boolean performDependencyResolve;

	/**
	 * Execution step 2. Erase dependency by scope names defined in
	 * {@link #scopeEraseList}. Alternative to step 1.
	 */
	@Parameter(property = "flatten.performEraseScopes", defaultValue = "false")
	boolean performEraseScopes;

	/**
	 * Execution step 3. Remove pom.xml members matched by xml tag names defined in
	 * the {@link #memberRemoveList}.
	 */
	@Parameter(property = "flatten.performRemoveMembers", defaultValue = "true")
	boolean performRemoveMembers;

	/**
	 * Execution step 4. Override project maven identity with:
	 * {@link #overrideGroupId} {@link #overrideArtifactId}
	 */
	@Parameter(property = "flatten.performOverrideIdentity", defaultValue = "false")
	boolean performOverrideIdentity;

	/**
	 * Execution step 5. Replace project pom.xml with generated flattened pom.xml
	 * for publication. Actual switch depends on condition
	 * {@link #packagingSwitchList}.
	 */
	@Parameter(property = "flatten.performSwitchPomXml", defaultValue = "true")
	boolean performSwitchPomXml;

	/**
	 * Default pom.xml encoding java charset name for text operations. Used when not
	 * defined in pom.xml.
	 */
	@Parameter(property = "flatten.encoding", defaultValue = "UTF-8")
	String encoding;

	/**
	 * Override project group id via {@link #performOverrideIdentity}.
	 */
	@Parameter(property = "flatten.overrideGroupId", defaultValue = "${project.groupId}")
	String overrideGroupId;

	/**
	 * Override project artifact id via {@link #performOverrideIdentity}.
	 */
	@Parameter(property = "flatten.overrideArtifactId", defaultValue = "${project.artifactId}")
	String overrideArtifactId;

	/**
	 * Regular expression used to extract artifacts from dependency report. Format:
	 * <code>groupId:artifactId:type:version:scope -- module mod-name (mod-type)</code>.
	 * Must provide exactly 5 regex capture groups.
	 */
	@Parameter(property = "flatten.dependencyRegex", defaultValue = DEPENDENCY_REGEX)
	String dependencyRegex;

	/**
	 * Use model char set with fall back to {@link #encoding}
	 */
	Charset modelCharset(Model model) {
		String encoding = model.getModelEncoding();
		if (isEmpty(encoding)) {
			encoding = this.encoding;
		}
		return Charset.forName(encoding);
	}

	/**
	 * Serialize maven model into pom.xml file.
	 */
	void writePom(Model pomModel, File pomFile, Charset charset) throws Exception {
		MavenXpp3Writer pomWriter = new MavenXpp3Writer();
		StringWriter textWriter = new StringWriter(64 * 1024);
		pomWriter.write(textWriter, pomModel);
		String pomText = textWriter.toString();
		Files.write(pomFile.toPath(), pomText.getBytes(charset));
	}

	/**
	 * Find a model method by name and number of parameters.
	 */
	Method findMethod(Model model, String name, int count) throws Exception {
		Method[] methodList = model.getClass().getMethods();
		for (Method method : methodList) {
			boolean hasName = name.equals(method.getName());
			boolean hasCount = count == method.getParameterCount();
			if (hasName && hasCount) {
				return method;
			}
		}
		throw new RuntimeException("Method not found: " + name);
	}

	/**
	 * Capitalize first letter of a name.
	 */
	String capitalFirst(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Remove member with xml tag name from the maven model.
	 */
	void removeMember(Model model, String name) throws Exception {
		String setter = "set" + capitalFirst(name);
		Method method = findMethod(model, setter, 1);
		method.invoke(model, new Object[] { null });
	}

	/**
	 * Create parent directory tree for a file.
	 */
	void ensureParent(File file) {
		File dir = file.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	/**
	 * Determine pom.xml switch behaviour based on project packaging type.
	 */
	boolean hasPackagingSwitch() {
		for (String packaging : packagingSwitchList) {
			if (packaging.equals(project.getPackaging())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove model dependencies configured in {@link #scopeEraseList}.
	 */
	void removeScope(Model model, String scope) {
		List<Dependency> source = model.getDependencies();
		if (source == null || source.isEmpty()) {
			return;
		}
		List<Dependency> target = new ArrayList<Dependency>();
		for (Dependency dep : source) {
			String depScope = dep.getScope();
			if (!isEmpty(depScope) && depScope.equals(scope)) {
				continue; // remove
			} else {
				target.add(dep); // keep
			}
		}
		model.setDependencies(target);
	}

	/**
	 * Generate dependency resolution report.
	 * 
	 * @see <a href="https://github.com/TimMoore/mojo-executor">mojo-executor</a>
	 * @see <a hfef=
	 *      "https://maven.apache.org/plugins/maven-dependency-plugin/resolve-mojo.html">maven-dependency-plugin:resolve</a
	 */
	void reportDependency() throws Exception {
		executeMojo( //
				plugin( //
						groupId("org.apache.maven.plugins"), //
						artifactId("maven-dependency-plugin") //
				), //
				goal("resolve"), //
				configuration( //
						element("includeParents", "false"), //
						element("appendOutput", "false"), //
						element("outputScope", "true"), //
						element("includeScope", includeScope), //
						element("excludeTransitive", Boolean.toString(excludeTransitive)), //
						element("outputFile", resolveListFile.getAbsolutePath()) //
				), //
				executionEnvironment(project, session, manager) //
		);
	}

	/**
	 * Parse report generated by dependency:resolve.
	 */
	List<Dependency> parseDependency(Model model) throws Exception {
		Pattern pattern = Pattern.compile(dependencyRegex);
		byte[] content = Files.readAllBytes(resolveListFile.toPath());
		Charset charset = modelCharset(model);
		String fileText = new String(content, charset);
		String[] lineList = fileText.split("\n");
		List<Dependency> result = new ArrayList<Dependency>();
		for (String line : lineList) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				Dependency entry = new Dependency();
				entry.setGroupId(matcher.group(1));
				entry.setArtifactId(matcher.group(2));
				entry.setType(matcher.group(3));
				entry.setVersion(matcher.group(4));
				entry.setScope(matcher.group(5));
				result.add(entry);
			}
		}
		return result;
	}

	/**
	 * Replace dependencies with dependency:resolve result.
	 */
	void resolveDependency(Model model) throws Exception {
		ensureParent(resolveListFile);
		reportDependency();
		List<Dependency> dependencyList = parseDependency(model);
		model.setDependencies(dependencyList);
	}

	/**
	 * Remove artifacts in scopes configured via {@link #scopeEraseList}.
	 */
	void eraseScopes(Model model) throws Exception {
		for (String scope : scopeEraseList) {
			removeScope(model, scope);
		}
	}

	/**
	 * Remove model members configured in {@link #memberRemoveList}.
	 */
	void removeMembers(Model model) throws Exception {
		for (String member : memberRemoveList) {
			removeMember(model, member);
		}
	}

	/**
	 * Serialize generated maven model into a target pom.xml.
	 */
	void persistModel(Model model) throws Exception {
		File file = targetPomFile;
		Charset charset = modelCharset(model);
		ensureParent(file);
		writePom(model, file, charset);
		buildContext.refresh(file);
	}

	/**
	 * Replace model identity: inside pom.xml.
	 */
	void overrideIdentity(Model model) {
		model.setGroupId(overrideGroupId);
		model.setArtifactId(overrideArtifactId);
	}

	/**
	 * Replace artifact identity: for the repository/${...}.jar.
	 */
	void overrideIdentity(Artifact artifact) {
		artifact.setGroupId(overrideGroupId);
		artifact.setArtifactId(overrideArtifactId);
	}

	/**
	 * Replace project build identity: for the ./target/${...}.jar.
	 */
	void overrideIdentity(Build build) {
		// Use maven convention.
		String finalName = overrideArtifactId + "-" + project.getVersion();
		build.setFinalName(finalName);
	}

	/**
	 * Replace project identity for multiple targets.
	 */
	void overrideIdentity(MavenProject project) {
		overrideIdentity(project.getModel());
		overrideIdentity(project.getArtifact());
		overrideIdentity(project.getBuild());
	}

	/**
	 * Replace attached project pom.xml with generated target pom.xml.
	 */
	void switchProjectPomXml() throws Exception {
		File file = targetPomFile;
		project.setPomFile(file);
	}

	/**
	 * Invoke execution steps in order.
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (buildContext.isIncremental()) {
			getLog().info("Skipping incremental execution.");
			return;
		}
		if (skip) {
			getLog().info("Skipping plugin goal execution.");
			return;
		}
		File sourcePomFile = project.getModel().getPomFile();
		try {
			buildContext.removeMessages(sourcePomFile);

			// Note: after clone():
			// - do not interpolate anything any more
			// - only remove content or add static content
			final Model flatModel = project.getModel().clone();

			// Change pom.xml.flatten.
			if (performDependencyResolve) {
				getLog().info("Resolving dependencies.");
				resolveDependency(flatModel);
			}
			if (performEraseScopes) {
				getLog().info("Erasing dependency scopes.");
				eraseScopes(flatModel);
			}
			if (performRemoveMembers) {
				getLog().info("Removing pom.xml model members.");
				removeMembers(flatModel);
			}

			// Change pom.xml.flatten and active project.
			if (performOverrideIdentity) {
				getLog().info("Overriding project maven identity.");
				// Change model clone, affects pom.xml.flatten.
				overrideIdentity(flatModel);
				// Change active project, affects following phases.
				overrideIdentity(project);
			}

			// Persist pom.xml.flatten.
			persistModel(flatModel);

			// Switch pom.xml -> pom.xml.flatten.
			if (performSwitchPomXml && hasPackagingSwitch()) {
				getLog().info("Switching project to flattened pom.xml.");
				switchProjectPomXml();
			}
		} catch (Throwable e) {
			String message = "Flatten failure";
			buildContext.addMessage(sourcePomFile, 1, 1, message, BuildContext.SEVERITY_ERROR, e);
			throw new MojoFailureException(message, e);
		}
	}

}
