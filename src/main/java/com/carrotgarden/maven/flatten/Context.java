package com.carrotgarden.maven.flatten;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.ContextEnabled;

/**
 * Plugin context accessor.
 */
public interface Context extends ContextEnabled {

	String RESOLVED_KEY = "flatten-maven-plugin/resolved";

	@SuppressWarnings("unchecked")
	default Map<String, Object> context() {
		Map<String, Object> map = getPluginContext();
		if (map == null) {
			return Collections.EMPTY_MAP;
		} else {
			return map;
		}
	}

	@SuppressWarnings("unchecked")
	default Set<Artifact> contextResolvedExtract() {
		Set<Artifact> set = (Set<Artifact>) context().get(RESOLVED_KEY);
		if (set == null) {
			return Collections.EMPTY_SET;
		} else {
			return set;
		}
	}

	default void contextResolvedPersist(Set<Artifact> resolved) {
		context().put(RESOLVED_KEY, resolved);
	}

}
