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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLGenerationSettings} profile resolution and glob matching.
 */
@DisplayName("XML Generation Settings Test")
public class XMLGenerationSettingsTest {

	// ---------- matchGlob tests (for http/https URIs) ----------

	@Nested
	@DisplayName("Textual glob matching for HTTP URIs")
	class MatchGlobTest {

		@Test
		public void doubleStarMatchesAnyPath() {
			assertTrue(GenerationProfile.matchGlob("**/*spring-beans*.xsd",
					"https://www.springframework.org/schema/beans/spring-beans-3.0.xsd"));
		}

		@Test
		public void singleStarDoesNotMatchSlash() {
			assertTrue(GenerationProfile.matchGlob("*.xsd", "schema.xsd"));
			assertFalse(GenerationProfile.matchGlob("*.xsd", "path/schema.xsd"));
		}

		@Test
		public void questionMarkMatchesSingleChar() {
			assertTrue(GenerationProfile.matchGlob("schema?.xsd", "schema1.xsd"));
			assertFalse(GenerationProfile.matchGlob("schema?.xsd", "schema12.xsd"));
		}

		@Test
		public void literalMatch() {
			assertTrue(GenerationProfile.matchGlob("maven-4.0.0.xsd", "maven-4.0.0.xsd"));
			assertFalse(GenerationProfile.matchGlob("maven-4.0.0.xsd", "maven-4.1.0.xsd"));
		}

		@Test
		public void specialCharactersMatch() {
			assertTrue(GenerationProfile.matchGlob("file(1).xsd", "file(1).xsd"));
		}

		@Test
		public void mavenPomPattern() {
			assertTrue(GenerationProfile.matchGlob("**/*maven*.xsd",
					"https://maven.apache.org/xsd/maven-4.0.0.xsd"));
			assertFalse(GenerationProfile.matchGlob("**/*maven*.xsd",
					"https://example.com/spring-beans.xsd"));
		}
	}

	// ---------- Profile.matches() tests ----------

	@Nested
	@DisplayName("Profile matching (PathPatternMatcher + textual fallback)")
	class ProfileMatchesTest {

		@Test
		public void matchesHttpURI() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("**/*maven*.xsd");
			assertTrue(profile.matches("https://maven.apache.org/xsd/maven-4.0.0.xsd"));
			assertFalse(profile.matches("https://example.com/spring-beans.xsd"));
		}

		@Test
		public void matchesFileURI() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("maven*.xsd");
			// PathPatternMatcher auto-prefixes "**/" when pattern doesn't start
			// with *, ?, or /. So "maven*.xsd" becomes "**/maven*.xsd".
			String fileURI = new java.io.File(System.getProperty("java.io.tmpdir"), "maven-4.0.0.xsd")
					.toURI().toString();
			assertTrue(profile.matches(fileURI));
		}

		@Test
		public void emptyPatternDoesNotMatch() {
			GenerationProfile profile = new GenerationProfile();
			profile.setPattern("");
			assertFalse(profile.matches("https://example.com/schema.xsd"));
		}

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
