/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.settings;

import java.util.List;

/**
 * Settings for the workspace Bloom index ({@code xml.workspaceIndex}).
 *
 * <p><b>Disabled by default</b> — users must opt in by setting
 * {@code "xml.workspaceIndex.enabled": true} in their VS Code settings.
 *
 * <p>When enabled, the server maintains a per-file Bloom filter that powers
 * cross-file grammar reference features: Find References on grammar files,
 * the "N references" code lens, and automatic reference updates on rename.
 */
public class XMLWorkspaceIndexSettings {

	/**
	 * Default directory names that are excluded from the workspace scan.
	 * These directories are skipped entirely during the initial index build.
	 */
	public static final List<String> DEFAULT_EXCLUDE_DIRECTORIES = List.of(
			".git", ".hg", ".svn",
			"node_modules",
			"target", "build",
			".gradle",
			"out", "dist", "bin"
	);

	/**
	 * Default maximum number of characters read from grammar files (.xsd, .dtd, .rng…)
	 * during indexing. Grammar files larger than this limit are marked as partially
	 * indexed and always returned as candidates, avoiding false negatives.
	 *
	 * <p>512 KB covers the vast majority of real-world XSD files and keeps the
	 * transient String allocation below the G1GC humongous-object threshold
	 * (typically half the region size), preventing OOM in workspaces with many
	 * large grammar files. Users with very large schemas and ample heap may
	 * increase this value via {@code xml.workspaceIndex.grammarHeadLimit}.
	 */
	public static final int DEFAULT_GRAMMAR_HEAD_LIMIT = 512 * 1024;

	private boolean enabled = false;

	private List<String> excludeDirectories = DEFAULT_EXCLUDE_DIRECTORIES;

	private int grammarHeadLimit = DEFAULT_GRAMMAR_HEAD_LIMIT;

	/**
	 * Returns {@code true} when the workspace Bloom index is active.
	 *
	 * <p><b>Use case:</b> {@link WorkspaceIndexPlugin} checks this flag on every
	 * settings change to decide whether to trigger a scan or clear the index.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the workspace Bloom index.
	 *
	 * <p><b>Use case:</b> Bound to {@code "xml.workspaceIndex.enabled"} in the client
	 * settings JSON. When switched to {@code false}, the plugin immediately clears
	 * the index so no stale candidates are served. When switched back to {@code true},
	 * a fresh workspace scan is triggered.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the directory names to skip during the workspace scan.
	 * Any directory whose name exactly matches an entry in this list is skipped
	 * entirely (along with all its descendants).
	 */
	public List<String> getExcludeDirectories() {
		return excludeDirectories;
	}

	/**
	 * Sets the directory names to skip during the workspace scan.
	 * Falls back to {@link #DEFAULT_EXCLUDE_DIRECTORIES} when {@code null} is supplied.
	 *
	 * <p><b>Use case:</b> Bound to {@code "xml.workspaceIndex.excludeDirectories"}.
	 * A project with a heavy {@code generated-sources} directory can add it here so
	 * the initial scan does not spend time indexing machine-generated XML.
	 */
	public void setExcludeDirectories(List<String> excludeDirectories) {
		this.excludeDirectories = excludeDirectories != null ? excludeDirectories : DEFAULT_EXCLUDE_DIRECTORIES;
	}

	/**
	 * Returns the maximum number of characters read from grammar files during indexing.
	 * Grammar files larger than this limit are marked as partially indexed and always
	 * returned as candidates (conservative correctness, no false negatives).
	 */
	public int getGrammarHeadLimit() {
		return grammarHeadLimit;
	}

	/**
	 * Sets the maximum number of characters read from grammar files during indexing.
	 * Non-positive values are silently replaced with {@link #DEFAULT_GRAMMAR_HEAD_LIMIT}.
	 *
	 * <p><b>Use case:</b> Bound to {@code "xml.workspaceIndex.grammarHeadLimit"}.
	 * Teams with unusually large XSD files (e.g. 2 MB) and ample heap may raise this
	 * limit to capture type definitions beyond the 512 KB default. Teams with tight
	 * memory budgets may lower it, accepting that more grammar files are marked as
	 * partially indexed and always returned as candidates.
	 */
	public void setGrammarHeadLimit(int grammarHeadLimit) {
		this.grammarHeadLimit = grammarHeadLimit > 0 ? grammarHeadLimit : DEFAULT_GRAMMAR_HEAD_LIMIT;
	}

	/**
	 * Copies all settings from {@code newSettings} into this instance.
	 *
	 * <p><b>Use case:</b> Called when the client sends {@code didChangeConfiguration}
	 * and the existing settings object needs to be updated in place (e.g. inside a
	 * cached {@link org.eclipse.lemminx.settings.XMLGeneralClientSettings} that other
	 * components hold a reference to).
	 */
	public void merge(XMLWorkspaceIndexSettings newSettings) {
		this.enabled = newSettings.isEnabled();
		this.excludeDirectories = newSettings.getExcludeDirectories();
		this.grammarHeadLimit = newSettings.getGrammarHeadLimit();
	}
}
