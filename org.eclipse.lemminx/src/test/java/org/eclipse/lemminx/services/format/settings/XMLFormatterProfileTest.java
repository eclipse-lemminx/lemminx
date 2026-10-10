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
package org.eclipse.lemminx.services.format.settings;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import java.util.Arrays;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.MixedContent;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.settings.XMLFormattingProfile;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * Tests for XML formatting with {@code xml.format.profiles} overrides.
 *
 * <p>
 * Profiles allow per-document format settings based on matching criteria
 * (file path, namespace URI, DOCTYPE public/system ID, grammar URI).
 * The first matching profile's format overrides are merged onto the global
 * format settings.
 * </p>
 *
 * <p>
 * {@link XMLFormattingProfile} extends {@link org.eclipse.lemminx.settings.DocumentMatcher}
 * with boxed format override fields directly (flat JSON, no nested
 * {@code format: {}} object).
 * </p>
 *
 * <h3>Test scenarios</h3>
 * <ul>
 * <li>Profile overrides splitAttributes for POM files (pattern match)</li>
 * <li>Profile overrides mixedContent for DocBook files (namespace match)</li>
 * <li>Profile overrides by public ID (MyBatis)</li>
 * <li>No matching profile → global settings used</li>
 * <li>First matching profile wins</li>
 * <li>Partial override — only specified fields change</li>
 * <li>Namespace glob matches multiple versions</li>
 * <li>AND combination — pattern + namespace</li>
 * </ul>
 */
public class XMLFormatterProfileTest {

	// ==========================================
	// Pattern-based profile (file path matching)
	// ==========================================

	// Use case: POM files use forceExpandMultiline split, other files use preserve.
	// The profile matches by file path pattern "**/pom.xml".
	// Expected: attributes split onto separate lines because the profile overrides
	// the global splitAttributes=preserve.
	@Test
	public void testProfileOverridesSplitAttributes_pomByPattern() throws BadLocationException {
		// Global: attributes on one line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);

		// Profile: for POM files, force each attribute on its own line
		XMLFormattingProfile pomProfile = new XMLFormattingProfile();
		pomProfile.setPattern("**/pom.xml");
		pomProfile.setSplitAttributes("forceExpandMultiline");
		settings.getFormattingSettings().setProfiles(Arrays.asList(pomProfile));

		// URI matches "**/pom.xml" → profile applies, attributes split
		String content = "<project attr1=\"a\" attr2=\"b\" />";
		String expected = "<project" + lineSeparator() + //
				"    attr1=\"a\"" + lineSeparator() + //
				"    attr2=\"b\" />";
		assertFormat(content, expected, settings, "file:///home/user/pom.xml");
	}

	// Use case: a non-POM file should NOT match the POM profile.
	// Expected: attributes stay on one line (global preserve applies).
	@Test
	public void testProfileNoMatch_notPom() throws BadLocationException {
		// Global: attributes on one line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);

		// Profile: only for POM files
		XMLFormattingProfile pomProfile = new XMLFormattingProfile();
		pomProfile.setPattern("**/pom.xml");
		pomProfile.setSplitAttributes("forceExpandMultiline");
		settings.getFormattingSettings().setProfiles(Arrays.asList(pomProfile));

		// URI "web.xml" does NOT match "**/pom.xml" → global settings, no change
		String content = "<root attr1=\"a\" attr2=\"b\" />";
		assertFormat(content, content, settings, "file:///home/user/web.xml");
	}

	// ==========================================
	// Namespace-based profile
	// ==========================================

	// Use case: DocBook documents use preserved mixed content.
	// The profile matches by root namespace "http://docbook.org/ns/docbook".
	// Expected: "text   <emphasis>bold</emphasis>   more" is kept as-is
	// because mixedContent=preserve prevents whitespace normalization.
	@Test
	public void testProfileOverridesMixedContent_docbookByNamespace() throws BadLocationException {
		// Global: normalize mixed content (collapse whitespace)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		// Profile: for DocBook namespace, preserve mixed content
		XMLFormattingProfile docbookProfile = new XMLFormattingProfile();
		docbookProfile.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		docbookProfile.setMixedContent("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(docbookProfile));

		// XML has DocBook namespace → profile applies, whitespace preserved
		// <para xmlns="...">text   <emphasis>bold</emphasis>   more</para>
		String content = "<para xmlns=\"http://docbook.org/ns/docbook\">text   <emphasis>bold</emphasis>   more</para>";
		assertFormat(content, content, settings, "file:///test.xml");
	}

	// Use case: non-DocBook file → profile doesn't match, global normalize applies.
	// Expected: extra whitespace in "text   <b>bold</b>   more" is collapsed
	// to single spaces.
	@Test
	public void testProfileNoMatch_notDocbook() throws BadLocationException {
		// Global: normalize mixed content
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		// Profile: only DocBook namespace
		XMLFormattingProfile docbookProfile = new XMLFormattingProfile();
		docbookProfile.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		docbookProfile.setMixedContent("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(docbookProfile));

		// XML has no namespace → profile doesn't match → global normalize
		// "text   <b>bold</b>   more" → "text <b>bold</b> more"
		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings, "file:///test.xml",
				te(0, 7, 0, 10, " "),
				te(0, 21, 0, 24, " "));
	}

	// Use case: glob on namespace URI matches DocBook version variants.
	// "http://docbook.org/ns/docbook*" matches "http://docbook.org/ns/docbook/5.1".
	// Expected: mixed content preserved because namespace matches the glob pattern.
	@Test
	public void testProfileNamespaceGlob() throws BadLocationException {
		// Global: normalize
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		// Profile: glob pattern with * suffix to match DocBook 5.x variants
		XMLFormattingProfile docbookProfile = new XMLFormattingProfile();
		docbookProfile.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook*"));
		docbookProfile.setMixedContent("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(docbookProfile));

		// Namespace "http://docbook.org/ns/docbook/5.1" matches glob → preserved
		// <para xmlns="http://docbook.org/ns/docbook/5.1">text   <emphasis>bold</emphasis></para>
		String content = "<para xmlns=\"http://docbook.org/ns/docbook/5.1\">text   <emphasis>bold</emphasis></para>";
		assertFormat(content, content, settings, "file:///test.xml");
	}

	// ==========================================
	// Public ID profile (DOCTYPE matching)
	// ==========================================

	// Use case: MyBatis mapper files use preserve split attributes
	@Test
	public void testProfileByPublicId_mybatis() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		XMLFormattingProfile mybatisProfile = new XMLFormattingProfile();
		mybatisProfile.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper 3.0//EN"));
		mybatisProfile.setSplitAttributes("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(mybatisProfile));

		// MyBatis file → profile applies, attributes preserved
		String content = "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">" + lineSeparator() + //
				"<mapper attr1=\"a\" attr2=\"b\" />";
		assertFormat(content, content, settings, "file:///mapper.xml");
	}

	// ==========================================
	// System ID profile
	// ==========================================

	// Use case: match by system ID with glob
	@Test
	public void testProfileBySystemId() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setSystemId(Arrays.asList("http://mybatis.org/dtd/*"));
		profile.setSplitAttributes("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		String content = "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">" + lineSeparator() + //
				"<mapper attr1=\"a\" attr2=\"b\" />";
		assertFormat(content, content, settings, "file:///mapper.xml");
	}

	// ==========================================
	// First matching profile wins
	// ==========================================

	// Use case: multiple profiles, first match takes precedence
	@Test
	public void testFirstMatchWins() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);

		// Profile 1: matches all XML files in docs/
		XMLFormattingProfile docsProfile = new XMLFormattingProfile();
		docsProfile.setPattern("**/docs/**/*.xml");
		docsProfile.setSplitAttributes("forceExpandMultiline");

		// Profile 2: matches all XML files (broader)
		XMLFormattingProfile allProfile = new XMLFormattingProfile();
		allProfile.setPattern("**/*.xml");
		allProfile.setSplitAttributes("force");

		settings.getFormattingSettings().setProfiles(Arrays.asList(docsProfile, allProfile));

		// File in docs/ → first profile matches (forceExpandMultiline)
		String content = "<root attr1=\"a\" attr2=\"b\" />";
		String expected = "<root" + lineSeparator() + //
				"    attr1=\"a\"" + lineSeparator() + //
				"    attr2=\"b\" />";
		assertFormat(content, expected, settings, "file:///home/project/docs/chapters/guide.xml");
	}

	// ==========================================
	// Partial override — only specified fields change
	// ==========================================

	// Use case: profile overrides only mixedContent, other settings
	// (spaceBeforeEmptyCloseTag) remain from global
	@Test
	public void testPartialOverride() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(true);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setPattern("**/docs/**/*.xml");
		profile.setMixedContent("preserve");
		// spaceBeforeEmptyCloseTag NOT overridden → global value (true) applies
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		// Mixed content preserved (profile override), but empty tag space kept (global)
		String content = "<root>" + lineSeparator() + //
				"  <empty/>" + lineSeparator() + //
				"</root>";
		String expected = "<root>" + lineSeparator() + //
				"  <empty />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, "file:///home/project/docs/chapters/guide.xml",
				te(1, 8, 1, 8, " "));
	}

	// ==========================================
	// No profiles configured → global settings
	// ==========================================

	@Test
	public void testNoProfiles() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings, "file:///test.xml",
				te(0, 7, 0, 10, " "),
				te(0, 21, 0, 24, " "));
	}

	// ==========================================
	// Profile with no format fields → global settings
	// ==========================================

	// Use case: profile matches but has no override fields (all null)
	@Test
	public void testProfileMatchesButNoOverrides() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setPattern("**/*.xml");
		// No format override fields set
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		// Profile matches but applies no overrides → global settings unchanged
		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings, "file:///test.xml",
				te(0, 7, 0, 10, " "),
				te(0, 21, 0, 24, " "));
	}

	// ==========================================
	// AND combination — pattern + namespace
	// ==========================================

	// Use case: only DocBook files in docs/ folder get special formatting
	@Test
	public void testAndCombination_patternAndNamespace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setPattern("**/docs/**/*.xml");
		profile.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook*"));
		profile.setMixedContent("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		// DocBook in docs/ → profile applies
		String content = "<para xmlns=\"http://docbook.org/ns/docbook\">text   <emphasis>bold</emphasis></para>";
		assertFormat(content, content, settings, "file:///home/project/docs/chapters/guide.xml");

		// DocBook NOT in docs/ → global settings
		String content2 = "<para xmlns=\"http://docbook.org/ns/docbook\">text   <emphasis>bold</emphasis>   more</para>";
		String expected2 = "<para xmlns=\"http://docbook.org/ns/docbook\">text <emphasis>bold</emphasis> more</para>";
		assertFormat(content2, expected2, settings, "file:///home/project/src/guide.xml",
				te(0, 48, 0, 51, " "),
				te(0, 76, 0, 79, " "));
	}

	// ==========================================
	// Multiple override fields
	// ==========================================

	// Use case: profile overrides both splitAttributes and closingBracketNewLine
	@Test
	public void testMultipleOverrides() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		settings.getFormattingSettings().setClosingBracketNewLine(false);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setPattern("**/pom.xml");
		profile.setSplitAttributes("forceExpandMultiline");
		profile.setClosingBracketNewLine(true);
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		String content = "<project attr1=\"a\" attr2=\"b\"></project>";
		String expected = "<project" + lineSeparator() + //
				"    attr1=\"a\"" + lineSeparator() + //
				"    attr2=\"b\"" + lineSeparator() + //
				"    ></project>";
		assertFormat(content, expected, settings, "file:///home/user/pom.xml");
	}

	// ==========================================
	// Grammar URI profile
	// ==========================================

	// Use case: match by xml-model href
	@Test
	public void testProfileByGrammarURI() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		XMLFormattingProfile profile = new XMLFormattingProfile();
		profile.setGrammarURI(Arrays.asList("*docbook.rng*"));
		profile.setMixedContent("preserve");
		settings.getFormattingSettings().setProfiles(Arrays.asList(profile));

		String content = "<?xml-model href=\"http://docbook.org/xml/5.0/rng/docbook.rng\"?>" + lineSeparator() + //
				"<para>text   <emphasis>bold</emphasis></para>";
		assertFormat(content, content, settings, "file:///test.xml");
	}

	// --- Helper ---

	private static void assertFormat(String content, String expected, SharedSettings settings, String uri,
			TextEdit... expectedEdits) throws BadLocationException {
		XMLAssert.assertFormat(null, content, expected, settings, uri, false,
				expectedEdits.length == 0 ? null : expectedEdits);
	}
}
