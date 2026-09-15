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
package org.eclipse.lemminx.extensions.contentmodel.generator;

import java.util.List;

import org.eclipse.lemminx.settings.PathPatternMatcher;

/**
 * Settings for XML document generation from grammar.
 *
 * <p>
 * These settings control how {@link XMLDocumentGenerator} produces XML content
 * from a grammar (XSD, DTD, RelaxNG, RNC). They can be configured globally via
 * client settings or per-grammar via generation profiles.
 * </p>
 */
public class XMLGenerationSettings {

	public static final int DEFAULT_MAX_DEPTH = 10;

	public static final boolean DEFAULT_OPTIONAL_ELEMENTS = false;

	public static final boolean DEFAULT_TYPE_DEFAULTS = true;

	private int maxDepth = DEFAULT_MAX_DEPTH;

	private boolean optionalElements = DEFAULT_OPTIONAL_ELEMENTS;

	private boolean typeDefaults = DEFAULT_TYPE_DEFAULTS;

	private List<GenerationProfile> profiles;

	public XMLGenerationSettings() {
	}

	/**
	 * Returns the maximum depth for nested element generation.
	 *
	 * @return the maximum depth (default 10).
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Sets the maximum depth for nested element generation.
	 *
	 * @param maxDepth the maximum depth.
	 */
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * Returns whether optional elements should be generated.
	 *
	 * @return true if optional elements should be generated (default true).
	 */
	public boolean isOptionalElements() {
		return optionalElements;
	}

	/**
	 * Sets whether optional elements should be generated.
	 *
	 * @param optionalElements true to generate optional elements.
	 */
	public void setOptionalElements(boolean optionalElements) {
		this.optionalElements = optionalElements;
	}

	/**
	 * Returns whether type-aware default values should be generated
	 * (e.g., "0" for xs:integer, "2026-01-01" for xs:date).
	 *
	 * @return true if type-aware defaults should be generated (default true).
	 */
	public boolean isTypeDefaults() {
		return typeDefaults;
	}

	/**
	 * Sets whether type-aware default values should be generated.
	 *
	 * @param typeDefaults true to generate type-aware defaults.
	 */
	public void setTypeDefaults(boolean typeDefaults) {
		this.typeDefaults = typeDefaults;
	}

	/**
	 * Returns the generation profiles list.
	 *
	 * @return the profiles list, or null if none.
	 */
	public List<GenerationProfile> getProfiles() {
		return profiles;
	}

	/**
	 * Sets the generation profiles list.
	 *
	 * @param profiles the profiles list.
	 */
	public void setProfiles(List<GenerationProfile> profiles) {
		this.profiles = profiles;
	}

	/**
	 * Returns a resolved copy of these settings by applying the first matching
	 * profile for the given grammar URI. If no profile matches, returns a copy with
	 * the global settings.
	 *
	 * <p>
	 * Profile matching uses {@link PathPatternMatcher} for {@code file://} URIs
	 * (Java NIO glob) and a simple textual glob match for {@code http://} /
	 * {@code https://} URIs.
	 * </p>
	 *
	 * @param grammarURI the grammar file URI to match against profile patterns.
	 * @return the resolved settings.
	 */
	public XMLGenerationSettings resolve(String grammarURI) {
		XMLGenerationSettings resolved = new XMLGenerationSettings();
		resolved.setMaxDepth(this.maxDepth);
		resolved.setOptionalElements(this.optionalElements);
		resolved.setTypeDefaults(this.typeDefaults);

		if (profiles != null && grammarURI != null) {
			for (GenerationProfile profile : profiles) {
				if (profile.matches(grammarURI)) {
					if (profile.getMaxDepth() != null) {
						resolved.setMaxDepth(profile.getMaxDepth());
					}
					if (profile.getOptionalElements() != null) {
						resolved.setOptionalElements(profile.getOptionalElements());
					}
					if (profile.getTypeDefaults() != null) {
						resolved.setTypeDefaults(profile.getTypeDefaults());
					}
					break;
				}
			}
		}

		return resolved;
	}

	/**
	 * A generation profile that overrides global settings for grammars matching a
	 * glob pattern.
	 *
	 * <p>
	 * Extends {@link PathPatternMatcher} to reuse the existing glob matching for
	 * {@code file://} URIs (Java NIO). For non-file URIs ({@code http://},
	 * {@code https://}), falls back to a simple textual glob match without regular
	 * expressions.
	 * </p>
	 */
	public static class GenerationProfile extends PathPatternMatcher {

		private Integer maxDepth;

		private Boolean optionalElements;

		private Boolean typeDefaults;

		/**
		 * Returns true if the given grammar URI matches this profile's pattern.
		 *
		 * <p>
		 * For {@code file://} URIs, delegates to {@link PathPatternMatcher#matches(String)}
		 * which uses Java NIO glob matching. For other URIs ({@code http://},
		 * {@code https://}), uses a simple textual glob match.
		 * </p>
		 *
		 * @param grammarURI the grammar URI to match.
		 * @return true if the URI matches this profile's pattern.
		 */
		@Override
		public boolean matches(String grammarURI) {
			if (getPattern() == null || getPattern().isEmpty()) {
				return false;
			}
			if (grammarURI.startsWith("file:")) {
				return super.matches(grammarURI);
			}
			// For http/https URIs, use simple textual glob matching
			return matchGlob(getPattern(), grammarURI);
		}

		public Integer getMaxDepth() {
			return maxDepth;
		}

		public void setMaxDepth(Integer maxDepth) {
			this.maxDepth = maxDepth;
		}

		public Boolean getOptionalElements() {
			return optionalElements;
		}

		public void setOptionalElements(Boolean optionalElements) {
			this.optionalElements = optionalElements;
		}

		public Boolean getTypeDefaults() {
			return typeDefaults;
		}

		public void setTypeDefaults(Boolean typeDefaults) {
			this.typeDefaults = typeDefaults;
		}

		/**
		 * Matches a glob pattern against a text string without using regular
		 * expressions. Used for non-file URIs where Java NIO glob cannot be applied.
		 *
		 * <p>
		 * Supported wildcards:
		 * </p>
		 * <ul>
		 * <li>{@code **} matches any sequence of characters including path
		 * separators</li>
		 * <li>{@code *} matches any sequence of characters except {@code /}</li>
		 * <li>{@code ?} matches any single character</li>
		 * </ul>
		 *
		 * @param pattern the glob pattern.
		 * @param text    the text to match.
		 * @return true if the text matches the pattern.
		 */
		static boolean matchGlob(String pattern, String text) {
			return matchGlob(pattern, 0, text, 0);
		}

		private static boolean matchGlob(String pattern, int pi, String text, int ti) {
			while (pi < pattern.length() && ti < text.length()) {
				char pc = pattern.charAt(pi);
				if (pc == '*') {
					if (pi + 1 < pattern.length() && pattern.charAt(pi + 1) == '*') {
						pi += 2;
						if (pi < pattern.length() && pattern.charAt(pi) == '/') {
							pi++;
						}
						for (int i = ti; i <= text.length(); i++) {
							if (matchGlob(pattern, pi, text, i)) {
								return true;
							}
						}
						return false;
					}
					pi++;
					for (int i = ti; i <= text.length(); i++) {
						if (i > ti && text.charAt(i - 1) == '/') {
							break;
						}
						if (matchGlob(pattern, pi, text, i)) {
							return true;
						}
					}
					return false;
				} else if (pc == '?') {
					pi++;
					ti++;
				} else {
					if (pc != text.charAt(ti)) {
						return false;
					}
					pi++;
					ti++;
				}
			}
			while (pi < pattern.length()) {
				if (pattern.charAt(pi) == '*') {
					pi++;
				} else {
					break;
				}
			}
			return pi == pattern.length() && ti == text.length();
		}
	}
}
