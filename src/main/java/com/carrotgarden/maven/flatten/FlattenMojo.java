package com.carrotgarden.maven.flatten;

import static org.codehaus.plexus.util.StringUtils.isEmpty;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
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
@Mojo( //
		name = "flatten", //
		defaultPhase = LifecyclePhase.PREPARE_PACKAGE, //
		requiresDependencyResolution = ResolutionScope.TEST, //
		requiresProject = true)
public class FlattenMojo extends AbstractMojo implements Context {

	/**
	 * Current Maven project.
	 */
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	/**
	 * Current Maven session.
	 */
	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	MavenSession mavenSession;

	/**
	 * Maven build plugin manager component.
	 */
	@Component()
	BuildPluginManager buildManager;

	/**
	 * Eclipse integration context.
	 */
	@Component()
	BuildContext buildContext;

	/**
	 * This plugin descriptor.
	 */
	@Parameter(defaultValue = "${plugin}", required = true, readonly = true)
	PluginDescriptor pluginMeta;

	/**
	 * Flag to skip this goal execution.
	 */
	@Parameter(property = "flatten.skip", defaultValue = "false")
	boolean skip;

	/**
	 * Propagate dependency exclusions during {@link #performDependencyResolve}.
	 */
	@Parameter(property = "flatten.resolveExclusions", defaultValue = "true")
	boolean resolveExclusions;

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
	 * Execution step 1. Invoke {@code maven-dependency-plugin:resolve} to resolve
	 * and filter project dependencies with {@link #includeScope},
	 * {@link #ecludeTransitive}. Alternative to step 2.
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
	 * Invoke another goal from this plugin.
	 */
	void executeSelfMojo(String goal) throws Exception {
		Support.executePluginMojo(pluginMeta, buildManager, mavenSession, goal);
	}

	/**
	 * Replace dependencies with result of goal="dependency:resolve".
	 */
	void resolveReplaceDependency(Model model) throws Exception {
		Set<Artifact> artifactList = contextResolvedExtract();
		List<Dependency> depenencyList = new ArrayList<Dependency>();
		for (Artifact artifact : artifactList) {
			Dependency dependency = new Dependency();
			Support.resolveApplyDeclared(artifact, dependency);
			if (resolveExclusions) {
				Support.resolveApplyExclusions(model, artifact, dependency);
			}
			depenencyList.add(dependency);
		}
		model.setDependencies(depenencyList);
	}

	/**
	 * Remove artifacts in scopes configured via {@link #scopeEraseList}.
	 */
	void eraseScopes(Model model) throws Exception {
		for (String scope : scopeEraseList) {
			Support.removeScope(model, scope);
		}
	}

	/**
	 * Remove model members configured in {@link #memberRemoveList}.
	 */
	void removeMembers(Model model) throws Exception {
		for (String member : memberRemoveList) {
			Support.removeMember(model, member);
		}
	}

	/**
	 * Serialize generated maven model into a target pom.xml.
	 */
	void persistModel(Model model) throws Exception {
		File file = targetPomFile;
		Charset charset = modelCharset(model);
		Support.ensureParent(file);
		Support.writePom(model, file, charset);
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
				executeSelfMojo("resolve");
				resolveReplaceDependency(flatModel);
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
