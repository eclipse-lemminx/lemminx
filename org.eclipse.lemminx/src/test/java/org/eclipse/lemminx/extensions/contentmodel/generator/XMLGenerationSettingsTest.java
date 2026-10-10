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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.lemminx.extensions.contentmodel.generator.XMLGenerationSettings.GenerationProfile;
import org.eclipse.lemminx.settings.GlobMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLGenerationSettings} profile resolution and glob matching.
 *
 * <p>
 * The textual glob matching for HTTP URIs now uses {@link GlobMatcher} which
 * treats {@code *} as matching any sequence of characters (including {@code /}).
 * For file URIs, Java NIO glob matching is used via {@link org.eclipse.lemminx.settings.PathPatternMatcher}.
 * </p>
 */
@DisplayName("XML Generation Settings Test")
public class XMLGenerationSettingsTest {

	// ---------- GlobMatcher tests (for http/https URIs) ----------

	@Nested
	@DisplayName("Textual glob matching for HTTP URIs")
	class MatchGlobTest {

		// Use case: match Spring beans XSD with ** prefix
		@Test
		public void doubleStarMatchesAnyPath() {
			assertTrue(GlobMatcher.match("**/*spring-beans*.xsd",
					"https://www.springframework.org/schema/beans/spring-beans-3.0.xsd"));
		}

		// Use case: * matches across / for URIs (unlike file system globs)
		@Test
		public void singleStarMatchesAcrossSlash() {
			assertTrue(GlobMatcher.match("*.xsd", "schema.xsd"));
			assertTrue(GlobMatcher.match("*.xsd", "path/schema.xsd"));
		}

		// Use case: ? matches exactly one character
		@Test
		public void questionMarkMatchesSingleChar() {
			assertTrue(GlobMatcher.match("schema?.xsd", "schema1.xsd"));
			assertFalse(GlobMatcher.match("schema?.xsd", "schema12.xsd"));
		}

		// Use case: exact literal match
		@Test
		public void literalMatch() {
			assertTrue(GlobMatcher.match("maven-4.0.0.xsd", "maven-4.0.0.xsd"));
			assertFalse(GlobMatcher.match("maven-4.0.0.xsd", "maven-4.1.0.xsd"));
		}

		// Use case: special characters in pattern
		@Test
		public void specialCharactersMatch() {
			assertTrue(GlobMatcher.match("file(1).xsd", "file(1).xsd"));
		}

		// Use case: Maven XSD URL matching
		@Test
		public void mavenPomPattern() {
			assertTrue(GlobMatcher.match("**/*maven*.xsd",
					"https://maven.apache.org/xsd/maven-4.0.0.xsd"));
			assertFalse(GlobMatcher.match("**/*maven*.xsd",
					"https://example.com/spring-beans.xsd"));
		}
	}

	// ---------- Profile.matches() tests ----------

	@Nested
	@DisplayName("Profile matching (PathPatternMatcher + textual fallback)")
	class ProfileMatchesTest {

		// Use case: match HTTP grammar URI
		@Test
		public void matchesHttpURI() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*maven*.xsd");
			assertTrue(profile.matches("https://maven.apache.org/xsd/maven-4.0.0.xsd"));
			assertFalse(profile.matches("https://example.com/spring-beans.xsd"));
		}

		// Use case: match file URI (uses Java NIO glob)
		@Test
		public void matchesFileURI() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("maven*.xsd");
			String fileURI = new java.io.File(System.getProperty("java.io.tmpdir"), "maven-4.0.0.xsd")
					.toURI().toString();
			assertTrue(profile.matches(fileURI));
		}

		// Use case: empty pattern should not match
		@Test
		public void emptyPatternDoesNotMatch() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("");
			assertFalse(profile.matches("https://example.com/schema.xsd"));
		}

		// Use case: null pattern should not match
		@Test
		public void nullPatternDoesNotMatch() {
			GenerationProfile profile = new GenerationProfile();
			assertFalse(profile.matches("https://example.com/schema.xsd"));
		}
	}

	// ---------- resolve tests ----------

	@Nested
	@DisplayName("Profile resolution")
	class ResolveTest {

		// Use case: no profiles → global settings
		@Test
		public void resolveWithNoProfiles() {
			XMLGenerationSettings settings = new XMLGenerationSettings();
			settings.setMaxDepth(5);
			settings.setOptionalElements(false);
			settings.setTypeDefaults(false);

			XMLGenerationSettings resolved = settings.resolve("file:///schema.xsd");

			assertEquals(5, resolved.getMaxDepth());
			assertFalse(resolved.isOptionalElements());
			assertFalse(resolved.isTypeDefaults());
		}

		// Use case: matching profile overrides specific fields
		@Test
		public void resolveWithMatchingProfile() {
			XMLGenerationSettings settings = new XMLGenerationSettings();

			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*maven*.xsd");
			profile.setMaxDepth(2);
			profile.setOptionalElements(false);
			settings.setProfiles(Collections.singletonList(profile));

			XMLGenerationSettings resolved = settings
					.resolve("https://maven.apache.org/xsd/maven-4.0.0.xsd");

			assertEquals(2, resolved.getMaxDepth());
			assertFalse(resolved.isOptionalElements());
			assertTrue(resolved.isTypeDefaults());
		}

		// Use case: non-matching profile → global settings
		@Test
		public void resolveWithNonMatchingProfile() {
			XMLGenerationSettings settings = new XMLGenerationSettings();
			settings.setMaxDepth(10);

			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*spring*.xsd");
			profile.setMaxDepth(3);
			settings.setProfiles(Collections.singletonList(profile));

			XMLGenerationSettings resolved = settings
					.resolve("https://maven.apache.org/xsd/maven-4.0.0.xsd");

			assertEquals(10, resolved.getMaxDepth());
		}

		// Use case: first matching profile wins when multiple match
		@Test
		public void resolveFirstMatchingProfileWins() {
			XMLGenerationSettings settings = new XMLGenerationSettings();

			GenerationProfile mavenProfile = new GenerationProfile();
			mavenProfile.setPattern("**/*maven*.xsd");
			mavenProfile.setMaxDepth(2);

			GenerationProfile catchAllProfile = new GenerationProfile();
			catchAllProfile.setPattern("**/*.xsd");
			catchAllProfile.setMaxDepth(5);

			settings.setProfiles(Arrays.asList(mavenProfile, catchAllProfile));

			XMLGenerationSettings resolved = settings
					.resolve("https://maven.apache.org/xsd/maven-4.0.0.xsd");

			assertEquals(2, resolved.getMaxDepth());
		}

		// Use case: partial override — only specified fields change
		@Test
		public void resolveProfilePartialOverride() {
			XMLGenerationSettings settings = new XMLGenerationSettings();
			settings.setMaxDepth(10);
			settings.setOptionalElements(true);
			settings.setTypeDefaults(true);

			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*.xsd");
			profile.setMaxDepth(3);
			settings.setProfiles(Collections.singletonList(profile));

			XMLGenerationSettings resolved = settings.resolve("https://example.com/schema.xsd");

			assertEquals(3, resolved.getMaxDepth());
			assertTrue(resolved.isOptionalElements());
			assertTrue(resolved.isTypeDefaults());
		}

		// Use case: null grammar URI → profiles not checked
		@Test
		public void resolveWithNullGrammarURI() {
			XMLGenerationSettings settings = new XMLGenerationSettings();
			settings.setMaxDepth(7);

			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*.xsd");
			profile.setMaxDepth(3);
			settings.setProfiles(Collections.singletonList(profile));

			XMLGenerationSettings resolved = settings.resolve(null);

			assertEquals(7, resolved.getMaxDepth());
		}

		// Use case: empty profile list → global settings
		@Test
		public void resolveWithEmptyProfiles() {
			XMLGenerationSettings settings = new XMLGenerationSettings();
			settings.setMaxDepth(8);
			settings.setProfiles(Collections.emptyList());

			XMLGenerationSettings resolved = settings.resolve("https://example.com/schema.xsd");

			assertEquals(8, resolved.getMaxDepth());
		}
	}
}
