package com.carrotgarden.maven.flatten;

import static org.codehaus.plexus.util.StringUtils.isEmpty;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Mojo support functions.
 */
public class Support {

	/**
	 * Capitalize first letter of a name.
	 */
	public static String capitalFirst(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	/**
	 * Create parent directory tree for a file.
	 */
	public static void ensureParent(File file) {
		File dir = file.getParentFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	/**
	 * Invoke another goal from the plugin.
	 * 
	 * Configuration priority: plugin overrides executions.
	 */
	public static void executePluginMojo(PluginDescriptor pluginMeta, BuildPluginManager buildManager,
			MavenSession mavenSession, String goal) throws Exception {
		MojoDescriptor mojoMeta = pluginMeta.getMojo(goal);
		Xpp3Dom pluginConfig = (Xpp3Dom) pluginMeta.getPlugin().getConfiguration();
		Xpp3Dom executionConfig = Support.toXpp3Dom(mojoMeta.getMojoConfiguration());
		Xpp3Dom[] executionEntryList = executionConfig.getChildren();
		if (pluginConfig != null && executionEntryList != null) {
			for (Xpp3Dom entry : executionEntryList) {
				Xpp3Dom pluginEntry = pluginConfig.getChild(entry.getName());
				if (pluginEntry != null) {
					executionConfig.addChild(pluginEntry);
				}
			}
		}
		MojoExecution mojoExecution = new MojoExecution(mojoMeta, executionConfig);
		buildManager.executeMojo(mavenSession, mojoExecution);
	}

	/**
	 * Find a model method by name and number of parameters.
	 */
	public static Method findMethod(Model model, String name, int count) throws Exception {
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
	 * Set value to private field.
	 */
	public static <T> void privateFieldSet(Class<T> klaz, T inst, String fieldName, Object fieldValue)
			throws Exception {
		Field field = klaz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(inst, fieldValue);
	}

	/**
	 * Remove member with xml tag name from the maven model.
	 */
	public static void removeMember(Model model, String name) throws Exception {
		String setter = "set" + capitalFirst(name);
		Method method = findMethod(model, setter, 1);
		method.invoke(model, new Object[] { null });
	}

	/**
	 * Remove model dependencies configured in {@link #scopeEraseList}.
	 */
	public static void removeScope(Model model, String scope) {
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
	 * Copy values from artifact to dependency.
	 */
	public static void resolveApplyDeclared(Artifact artifact, Dependency dependency) {
		dependency.setGroupId(artifact.getGroupId());
		dependency.setArtifactId(artifact.getArtifactId());
		dependency.setVersion(artifact.getVersion());
		dependency.setType(artifact.getType());
		dependency.setScope(artifact.getScope());
		dependency.setClassifier(artifact.getClassifier());
	}

	/**
	 * Apply exclusions from matching dependency list.
	 */
	public static void resolveApplyExclusions(List<Dependency> depenendencyList, Artifact artifact,
			Dependency dependency) {
		Dependency matching = resolveLocateMatching(depenendencyList, artifact);
		if (matching == null) {
			return;
		}
		List<Exclusion> exclusions = matching.getExclusions();
		if (exclusions != null && !exclusions.isEmpty()) {
			dependency.setExclusions(matching.getExclusions());
		}
		String optional = matching.getOptional();
		if (optional != null) {
			dependency.setOptional(matching.getOptional());
		}
	}

	/**
	 * Apply dependency exclusions from the model.
	 */
	public static void resolveApplyExclusions(Model model, Artifact artifact, Dependency dependency) {
		DependencyManagement management = model.getDependencyManagement();
		if (management != null && management.getDependencies() != null) {
			resolveApplyExclusions(management.getDependencies(), artifact, dependency);
		}
		if (model.getDependencies() != null) {
			resolveApplyExclusions(model.getDependencies(), artifact, dependency);
		}
	}

	/**
	 * Locate dependency matching an artifact.
	 */
	public static Dependency resolveLocateMatching(List<Dependency> depenendencyList, Artifact artifact) {
		for (Dependency dependency : depenendencyList) {
			boolean hasType = StringUtils.equals(dependency.getType(), artifact.getType());
			boolean hasGroup = StringUtils.equals(dependency.getGroupId(), artifact.getGroupId());
			boolean hasArtifact = StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactId());
			boolean hasClassifier = StringUtils.equals(dependency.getClassifier(), artifact.getClassifier());
			if (hasType && hasGroup && hasArtifact && hasClassifier) {
				return dependency;
			}
		}
		return null;
	}

	/**
	 * Converts PlexusConfiguration to a Xpp3Dom.
	 */
	public static Xpp3Dom toXpp3Dom(PlexusConfiguration config) {
		Xpp3Dom result = new Xpp3Dom(config.getName());
		result.setValue(config.getValue(null));
		for (String name : config.getAttributeNames()) {
			result.setAttribute(name, config.getAttribute(name));
		}
		for (PlexusConfiguration child : config.getChildren()) {
			result.addChild(toXpp3Dom(child));
		}
		return result;
	}

	/**
	 * Serialize Maven model into pom.xml file.
	 */
	public static void writePom(Model pomModel, File pomFile, Charset charset) throws Exception {
		MavenXpp3Writer pomWriter = new MavenXpp3Writer();
		StringWriter textWriter = new StringWriter(64 * 1024);
		pomWriter.write(textWriter, pomModel);
		String pomText = textWriter.toString();
		Files.write(pomFile.toPath(), pomText.getBytes(charset));
	}

}
