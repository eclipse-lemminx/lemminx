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
package org.eclipse.lemminx.extensions.index;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.settings.XMLWorkspaceIndexSettings;

/**
 * Compiled form of the {@code xml.workspaceIndex.headLimits} setting.
 *
 * <p>Keys in the raw settings map are glob patterns (e.g. {@code "**&#47;*.xsd"},
 * {@code "**&#47;vendor&#47;**"}). This class compiles them once into an ordered list
 * of {@link Rule}s and resolves the limit for a given file path efficiently.
 *
 * <p><b>Matching rule:</b> rules are sorted by pattern length descending;
 * the longest (most-specific) pattern wins. The first matching rule is returned.
 *
 * <p><b>Rule types and fast paths:</b>
 * <ul>
 *   <li><b>Match-all</b> ({@code "*"}, {@code "**"}, {@code "**&#47;*"}) - always
 *       matches, O(1). {@code "*"} is accepted as a user-friendly alias for
 *       "any file" even though bare {@code *} does not cross path separators in
 *       strict glob semantics.</li>
 *   <li><b>Extension-only</b> ({@code "*.ext"} or {@code "**&#47;*.ext"}) - matched
 *       via a string comparison on the file extension, O(1), no
 *       {@link PathMatcher} needed.</li>
 *   <li><b>Path-based</b> (everything else, e.g. {@code "**&#47;vendor&#47;**"}) - uses
 *       {@link PathMatcher#matches(Path)} on the absolute path, computed lazily
 *       only when the rule is first reached. When the pattern ends with
 *       {@code *.ext}, an O(1) extension pre-filter skips the
 *       {@link PathMatcher} call for files whose extension does not match.</li>
 * </ul>
 *
 * @see XMLWorkspaceIndexSettings#parseSizeLimit(String)
 */
public final class HeadLimitRules {

	/** Sentinel used when no {@code headLimits} are configured — no limit for any file. */
	public static final HeadLimitRules NONE = new HeadLimitRules(Collections.emptyList());

	private final List<Rule> rules;

	private HeadLimitRules(List<Rule> rules) {
		this.rules = rules;
	}

	/**
	 * Compiles a raw glob-pattern→size-string map into a {@link HeadLimitRules} instance.
	 *
	 * @param raw map from glob pattern to size string; {@code null} or empty → {@link #NONE}.
	 * @return compiled rules, ready for repeated {@link #getLimit(Path)} calls.
	 */
	public static HeadLimitRules compile(Map<String, String> raw) {
		if (raw == null || raw.isEmpty()) {
			return NONE;
		}
		List<Map.Entry<String, String>> entries = new ArrayList<>(raw.entrySet());
		// Sort by pattern length descending — longest = most specific, checked first
		entries.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

		List<Rule> rules = new ArrayList<>(entries.size());
		for (Map.Entry<String, String> entry : entries) {
			String pattern = entry.getKey();
			int limit = XMLWorkspaceIndexSettings.parseSizeLimit(entry.getValue());
			if (isMatchAll(pattern)) {
				rules.add(new MatchAllRule(limit));
			} else if (isExtensionOnly(pattern)) {
				rules.add(new ExtensionRule(extractExtension(pattern), limit));
			} else {
				try {
					PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
					String extFilter = extractGlobExtFilter(pattern);
					rules.add(new GlobRule(matcher, extFilter, limit));
				} catch (Exception e) {
					// Invalid glob pattern — skip silently rather than breaking indexing
				}
			}
		}
		return new HeadLimitRules(rules);
	}

	/**
	 * Returns the head-read limit (in chars) for {@code path}.
	 * Returns {@link Integer#MAX_VALUE} when no rule matches (no limit — read the whole file).
	 *
	 * @param path the file being indexed; may be relative or absolute.
	 * @return character limit, or {@link Integer#MAX_VALUE} for no limit.
	 */
	public int getLimit(Path path) {
		if (rules.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		String ext = getExtension(path);
		Path absolutePath = null; // computed lazily, only when a GlobRule is reached
		for (Rule rule : rules) {
			if (rule instanceof MatchAllRule) {
				return rule.limit;
			} else if (rule instanceof ExtensionRule) {
				if (((ExtensionRule) rule).ext.equals(ext)) {
					return rule.limit;
				}
			} else {
				GlobRule glob = (GlobRule) rule;
				// O(1) extension pre-filter: skip PathMatcher for wrong extensions
				if (glob.extFilter != null && !glob.extFilter.equals(ext)) {
					continue;
				}
				if (absolutePath == null) {
					absolutePath = path.toAbsolutePath();
				}
				if (glob.matcher.matches(absolutePath)) {
					return rule.limit;
				}
			}
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Returns {@code true} for catch-all patterns: {@code "*"}, {@code "**"},
	 * {@code "**&#47;*"}. {@code "*"} is accepted as a user-friendly alias; in strict
	 * glob it would only match files at the filesystem root, which is never useful.
	 */
	static boolean isMatchAll(String pattern) {
		return "*".equals(pattern) || "**".equals(pattern) || "**/*".equals(pattern);
	}

	/**
	 * Returns {@code true} when the pattern is extension-only: {@code "*.ext"} or
	 * {@code "**&#47;*.ext"} with no additional path separators in the final segment.
	 */
	static boolean isExtensionOnly(String pattern) {
		String p = pattern.startsWith("**/") ? pattern.substring(3) : pattern;
		return p.startsWith("*.") && p.indexOf('/') < 0 && p.indexOf('\\') < 0;
	}

	/** Extracts the extension from an extension-only pattern (e.g. {@code "**&#47;*.XSD"} to {@code "xsd"}). */
	static String extractExtension(String pattern) {
		int dot = pattern.lastIndexOf('.');
		return dot >= 0 ? pattern.substring(dot + 1).toLowerCase() : "";
	}

	/**
	 * Extracts an extension pre-filter from a path-based glob pattern, or returns
	 * {@code null} if the pattern does not end with a fixed extension.
	 *
	 * <p>Examples:
	 * <ul>
	 *   <li>{@code "**&#47;schemas&#47;*.xsd"} - returns {@code "xsd"}</li>
	 *   <li>{@code "**&#47;vendor&#47;**"} - returns {@code null} (no fixed extension)</li>
	 * </ul>
	 */
	static String extractGlobExtFilter(String pattern) {
		int dot = pattern.lastIndexOf('.');
		if (dot <= 0) {
			return null;
		}
		// The char before the dot must be '*' (e.g. "*.xsd"), not a letter
		if (pattern.charAt(dot - 1) != '*') {
			return null;
		}
		String ext = pattern.substring(dot + 1);
		// Extension must not contain path separators
		if (ext.indexOf('/') >= 0 || ext.indexOf('\\') >= 0) {
			return null;
		}
		return ext.isEmpty() ? null : ext.toLowerCase();
	}

	private static String getExtension(Path path) {
		String name = path.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
	}

	private abstract static class Rule {
		final int limit;
		Rule(int limit) { this.limit = limit; }
	}

	/** Matches any file; used for {@code "*"}, {@code "**"}, {@code "**&#47;*"}. */
	private static final class MatchAllRule extends Rule {
		MatchAllRule(int limit) { super(limit); }
	}

	/** Matches by file extension only — O(1), no {@link PathMatcher} needed. */
	private static final class ExtensionRule extends Rule {
		final String ext;
		ExtensionRule(String ext, int limit) { super(limit); this.ext = ext; }
	}

	/**
	 * Matches by full path glob. {@code extFilter} (may be {@code null}) is an O(1)
	 * pre-filter: if the file extension does not match, the {@link PathMatcher} is skipped.
	 */
	private static final class GlobRule extends Rule {
		final PathMatcher matcher;
		final String extFilter;
		GlobRule(PathMatcher matcher, String extFilter, int limit) {
			super(limit);
			this.matcher = matcher;
			this.extFilter = extFilter;
		}
	}
}
