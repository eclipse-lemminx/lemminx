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
 * Simple textual glob matching utility for non-file-path strings such as
 * namespace URIs, DOCTYPE public/system IDs, and grammar URIs.
 *
 * <p>
 * Java NIO {@link java.nio.file.PathMatcher} only works for file-system
 * paths. This class provides lightweight glob matching for arbitrary
 * text strings using two wildcards:
 * </p>
 * <ul>
 * <li>{@code *} — matches any sequence of characters (including empty)</li>
 * <li>{@code ?} — matches exactly one character</li>
 * </ul>
 *
 * <h3>Use case: match DocBook namespace with trailing wildcard</h3>
 * <pre>
 * GlobMatcher.match("http://docbook.org/ns/docbook*", "http://docbook.org/ns/docbook");     // true
 * GlobMatcher.match("http://docbook.org/ns/docbook*", "http://docbook.org/ns/docbook/5.1"); // true
 * </pre>
 *
 * <h3>Use case: match any MyBatis DTD version</h3>
 * <pre>
 * GlobMatcher.match("-//mybatis.org//DTD Mapper*", "-//mybatis.org//DTD Mapper 3.0//EN"); // true
 * </pre>
 *
 * @see DocumentMatcher
 */
public final class GlobMatcher {

	private GlobMatcher() {
	}

	/**
	 * Matches a glob pattern against a text string.
	 *
	 * <p>
	 * Uses an iterative greedy algorithm with a single backtrack point,
	 * running in O(n·m) time. Consecutive {@code *} wildcards are collapsed
	 * to avoid redundant work.
	 * </p>
	 *
	 * @param pattern the glob pattern (supports {@code *} and {@code ?}).
	 * @param text    the text to match.
	 * @return {@code true} if the text matches the pattern.
	 */
	public static boolean match(String pattern, String text) {
		int pi = 0, ti = 0;
		int starPi = -1, starTi = -1;

		while (ti < text.length()) {
			if (pi < pattern.length() && pattern.charAt(pi) == '*') {
				// Use case: "http://docbook.org/ns/docbook*" — star consumes
				// any trailing characters in the namespace URI.
				// Collapse consecutive stars (e.g. "**" behaves like "*")
				while (pi < pattern.length() && pattern.charAt(pi) == '*') {
					pi++;
				}
				// Record position after star(s) for backtracking
				starPi = pi;
				starTi = ti;
			} else if (pi < pattern.length()
					&& (pattern.charAt(pi) == '?' || pattern.charAt(pi) == text.charAt(ti))) {
				// Use case: "mybatis-?-mapper.dtd" — '?' matches exactly
				// one character (e.g. '3'), literal chars match themselves.
				pi++;
				ti++;
			} else if (starPi >= 0) {
				// Mismatch after a star: backtrack — let the star consume
				// one more character from text and retry from there.
				pi = starPi;
				ti = ++starTi;
			} else {
				// No star to backtrack to — pattern does not match.
				return false;
			}
		}
		// Use case: trailing stars match empty suffix
		// (e.g. pattern "foo*" matches text "foo")
		while (pi < pattern.length() && pattern.charAt(pi) == '*') {
			pi++;
		}
		return pi == pattern.length();
	}

	/**
	 * Checks if any glob pattern in the list matches the given value.
	 *
	 * <p>
	 * Use case: check if a document's namespace URI matches any of the
	 * configured namespace patterns (OR semantics).
	 * </p>
	 *
	 * @param patterns the list of glob patterns.
	 * @param value    the value to match, or {@code null}.
	 * @return {@code true} if any pattern matches the value; {@code false}
	 *         if the value is {@code null} or no pattern matches.
	 */
	public static boolean matchAny(List<String> patterns, String value) {
		if (value == null) {
			return false;
		}
		for (String p : patterns) {
			if (p != null && match(p, value)) {
				return true;
			}
		}
		return false;
	}
}
