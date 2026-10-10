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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.colors.settings.XMLColors;
import org.eclipse.lemminx.extensions.filepath.settings.FilePathMapping;
import org.eclipse.lemminx.extensions.references.settings.XMLReferences;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DocumentMatcher} matching logic.
 *
 * <p>
 * {@link DocumentMatcher} matches documents using 6 criteria
 * (all optional, combined with AND):
 * </p>
 * <ul>
 * <li>{@code pattern} — glob on the file URI
 * (inherited from {@link PathPatternMatcher})</li>
 * <li>{@code namespaceURI} — root element namespace</li>
 * <li>{@code rootElement} — root element local name</li>
 * <li>{@code publicId} — DOCTYPE public ID</li>
 * <li>{@code systemId} — DOCTYPE system ID</li>
 * <li>{@code grammarURI} — resolved grammar URI (DOCTYPE, xml-model, etc.)</li>
 * </ul>
 *
 * <p>
 * Each test method documents the use case it covers, so new contributors
 * can understand how the matcher is intended to be used.
 * </p>
 */
public class DocumentMatcherTest {

	// ==========================================
	// Pattern (file path glob) matching
	// ==========================================

	// Use case: match all POM files by name
	@Test
	public void testPatternMatch_pomXml() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/pom.xml");
		assertTrue(matcher.matches(parse("<project/>", "file:///home/user/project/pom.xml")));
	}

	// Use case: POM pattern should not match arbitrary XML files
	@Test
	public void testPatternNoMatch_otherXml() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/pom.xml");
		assertFalse(matcher.matches(parse("<root/>", "file:///home/user/project/web.xml")));
	}

	// Use case: match all XML files in a docs/ folder
	@Test
	public void testPatternMatch_docsFolder() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/docs/**/*.xml");
		assertTrue(matcher.matches(parse("<book/>", "file:///home/user/project/docs/guide/chapter1.xml")));
	}

	// ==========================================
	// Namespace URI matching
	// ==========================================

	// Use case: match DocBook 5 documents by namespace
	@Test
	public void testNamespaceMatch_docbook() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		assertTrue(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"><title>Test</title></book>",
				"file:///test.xml")));
	}

	// Use case: namespace doesn't match
	@Test
	public void testNamespaceNoMatch() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		assertFalse(matcher.matches(parse(
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"/>",
				"file:///pom.xml")));
	}

	// Use case: match multiple DocBook versions with glob
	@Test
	public void testNamespaceMatch_glob() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook*"));
		assertTrue(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"/>",
				"file:///test.xml")));
		assertTrue(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook/5.1\"/>",
				"file:///test.xml")));
	}

	// Use case: multiple namespace URIs (different vocabularies)
	@Test
	public void testNamespaceMatch_multipleURIs() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList(
				"http://docbook.org/ns/docbook*",
				"http://www.w3.org/1999/xhtml"));
		assertTrue(matcher.matches(parse(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"/>",
				"file:///test.html")));
	}

	// Use case: document without namespace doesn't match
	@Test
	public void testNamespaceNoMatch_noNamespace() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		assertFalse(matcher.matches(parse("<root/>", "file:///test.xml")));
	}

	// ==========================================
	// Root element matching
	// ==========================================

	// Use case: match MyBatis documents by root element <mapper>
	@Test
	public void testRootElementMatch_mapper() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("mapper"));
		assertTrue(matcher.matches(parse("<mapper/>", "file:///mapper.xml")));
	}

	// Use case: root element doesn't match
	@Test
	public void testRootElementNoMatch() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("mapper"));
		assertFalse(matcher.matches(parse("<configuration/>", "file:///config.xml")));
	}

	// Use case: match multiple root elements (mapper OR configuration)
	@Test
	public void testRootElementMatch_multipleNames() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("mapper", "configuration"));
		assertTrue(matcher.matches(parse("<mapper/>", "file:///mapper.xml")));
		assertTrue(matcher.matches(parse("<configuration/>", "file:///config.xml")));
		assertFalse(matcher.matches(parse("<project/>", "file:///pom.xml")));
	}

	// Use case: root element with namespace — matches on local name only
	@Test
	public void testRootElementMatch_withNamespace() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("project"));
		assertTrue(matcher.matches(parse(
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"/>",
				"file:///pom.xml")));
	}

	// Use case: disambiguate <project> with rootElement + namespaceURI (AND)
	// Maven <project> vs Ant <project> — use namespace to distinguish
	@Test
	public void testRootElementAndNamespace_disambiguate() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("project"));
		matcher.setNamespaceURI(Arrays.asList("http://maven.apache.org/POM/4.0.0"));

		// Maven <project> with namespace → matches
		assertTrue(matcher.matches(parse(
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"/>",
				"file:///pom.xml")));

		// Ant <project> without namespace → doesn't match (namespace criterion fails)
		assertFalse(matcher.matches(parse(
				"<project name=\"myapp\" default=\"build\"/>",
				"file:///build.xml")));
	}

	// Use case: no root element in document (e.g. empty or comment-only)
	@Test
	public void testRootElement_noRootInDocument() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("project"));
		assertFalse(matcher.matches(parse("<!-- empty -->", "file:///test.xml")));
	}

	// Use case: glob on root element name
	@Test
	public void testRootElementMatch_glob() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setRootElement(Arrays.asList("web-app*"));
		assertTrue(matcher.matches(parse("<web-app/>", "file:///web.xml")));
		assertTrue(matcher.matches(parse("<web-app-2/>", "file:///web.xml")));
		assertFalse(matcher.matches(parse("<webapp/>", "file:///web.xml")));
	}

	// ==========================================
	// Public ID matching
	// ==========================================

	// Use case: match MyBatis mapper by public ID
	@Test
	public void testPublicIdMatch_mybatis() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper 3.0//EN"));
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// Use case: public ID doesn't match
	@Test
	public void testPublicIdNoMatch() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper 3.0//EN"));
		assertFalse(matcher.matches(parse(
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0//EN\" \"xhtml.dtd\"><html/>",
				"file:///test.html")));
	}

	// Use case: match any MyBatis DTD version with glob
	@Test
	public void testPublicIdMatch_glob() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper*"));
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// ==========================================
	// System ID matching
	// ==========================================

	// Use case: match by system ID
	@Test
	public void testSystemIdMatch() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setSystemId(Arrays.asList("http://mybatis.org/dtd/mybatis-3-mapper.dtd"));
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// Use case: system ID with glob
	@Test
	public void testSystemIdMatch_glob() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setSystemId(Arrays.asList("http://mybatis.org/dtd/*"));
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// ==========================================
	// Grammar URI matching
	// ==========================================

	// Use case: match by DOCTYPE system ID via grammarURI
	@Test
	public void testGrammarURIMatch_doctype() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setGrammarURI(Arrays.asList("http://mybatis.org/dtd/mybatis-3-mapper.dtd"));
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// Use case: match by xml-model href via grammarURI
	@Test
	public void testGrammarURIMatch_xmlModel() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setGrammarURI(Arrays.asList("http://docbook.org/xml/5.0/rng/docbook.rng*"));
		assertTrue(matcher.matches(parse(
				"<?xml-model href=\"http://docbook.org/xml/5.0/rng/docbook.rng\"?><book/>",
				"file:///test.xml")));
	}

	// ==========================================
	// AND combination of criteria
	// ==========================================

	// Use case: match DocBook files only in docs/ folder
	@Test
	public void testAndCombination_patternAndNamespace() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/docs/**/*.xml");
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));

		// Both criteria match
		assertTrue(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"/>",
				"file:///home/project/docs/chapters/guide.xml")));

		// Only pattern matches, not namespace
		assertFalse(matcher.matches(parse(
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"/>",
				"file:///home/project/docs/chapters/pom.xml")));

		// Only namespace matches, not pattern
		assertFalse(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"/>",
				"file:///home/project/src/guide.xml")));
	}

	// Use case: match MyBatis by both public ID and system ID
	@Test
	public void testAndCombination_publicIdAndSystemId() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper 3.0//EN"));
		matcher.setSystemId(Arrays.asList("http://mybatis.org/dtd/mybatis-3-mapper.dtd"));

		assertTrue(matcher.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
	}

	// Use case: match only when pattern + namespace + publicId all match
	@Test
	public void testAndCombination_threeCriteria() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/config/**/*.xml");
		matcher.setNamespaceURI(Arrays.asList("http://www.springframework.org/schema/beans"));
		matcher.setPublicId(Arrays.asList("-//SPRING//DTD BEAN*"));

		// All three match
		assertTrue(matcher.matches(parse(
				"<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN 2.0//EN\" \"spring-beans.dtd\">"
						+ "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>",
				"file:///home/project/config/app/context.xml")));

		// Pattern doesn't match
		assertFalse(matcher.matches(parse(
				"<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN 2.0//EN\" \"spring-beans.dtd\">"
						+ "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>",
				"file:///home/project/src/context.xml")));
	}

	// ==========================================
	// Edge cases
	// ==========================================

	// Use case: matcher with no criteria never matches
	@Test
	public void testNoCriteria_neverMatches() {
		DocumentMatcher matcher = new DocumentMatcher();
		assertFalse(matcher.matches(parse("<root/>", "file:///test.xml")));
	}

	// Use case: empty lists count as no criteria
	@Test
	public void testEmptyLists_neverMatches() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Collections.emptyList());
		assertFalse(matcher.matches(parse("<root/>", "file:///test.xml")));
	}

	// Use case: document without root element
	@Test
	public void testNoRootElement() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		assertFalse(matcher.matches(parse("<!-- empty -->", "file:///test.xml")));
	}

	// Use case: document without DOCTYPE
	@Test
	public void testNoDoctype() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper 3.0//EN"));
		assertFalse(matcher.matches(parse("<root/>", "file:///test.xml")));
	}

	// Use case: pattern only (no document-level criteria) still matches
	@Test
	public void testPatternOnly_matchesWithoutDocumentCriteria() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setPattern("**/*.xml");
		assertTrue(matcher.matches(parse("<root/>", "file:///home/user/test.xml")));
	}

	// Use case: namespace only (no file pattern) matches any file
	@Test
	public void testNamespaceOnly_matchesAnyFile() {
		DocumentMatcher matcher = new DocumentMatcher();
		matcher.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook"));
		assertTrue(matcher.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"/>",
				"file:///any/path/anywhere.xml")));
	}

	// ==========================================
	// Subclass matching (XMLColors, XMLReferences, etc.)
	// ==========================================

	// Use case: XMLColors can match DocBook documents by namespace
	@Test
	public void testXMLColors_matchByNamespace() {
		XMLColors colors = new XMLColors();
		colors.setNamespaceURI(Arrays.asList("http://docbook.org/ns/docbook*"));
		assertTrue(colors.matches(parse(
				"<book xmlns=\"http://docbook.org/ns/docbook\"/>",
				"file:///test.xml")));
		assertFalse(colors.matches(parse(
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"/>",
				"file:///pom.xml")));
	}

	// Use case: XMLColors with pattern only (backward compatible with PathPatternMatcher)
	@Test
	public void testXMLColors_matchByPatternOnly() {
		XMLColors colors = new XMLColors();
		colors.setPattern("**/*.svg");
		assertTrue(colors.matches(parse("<svg/>", "file:///home/user/image.svg")));
		assertFalse(colors.matches(parse("<root/>", "file:///home/user/data.xml")));
	}

	// Use case: XMLReferences can match MyBatis documents by public ID
	@Test
	public void testXMLReferences_matchByPublicId() {
		XMLReferences refs = new XMLReferences();
		refs.setPublicId(Arrays.asList("-//mybatis.org//DTD Mapper*"));
		assertTrue(refs.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"mybatis.dtd\"><mapper/>",
				"file:///mapper.xml")));
		assertFalse(refs.matches(parse(
				"<root/>",
				"file:///other.xml")));
	}

	// Use case: XMLReferences with pattern + namespace AND combination
	@Test
	public void testXMLReferences_andCombination() {
		XMLReferences refs = new XMLReferences();
		refs.setPattern("**/config/**/*.xml");
		refs.setNamespaceURI(Arrays.asList("http://www.springframework.org/schema/beans"));
		// Both criteria match
		assertTrue(refs.matches(parse(
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"/>",
				"file:///home/project/config/app/context.xml")));
		// Only pattern matches
		assertFalse(refs.matches(parse(
				"<root/>",
				"file:///home/project/config/app/other.xml")));
		// Only namespace matches
		assertFalse(refs.matches(parse(
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"/>",
				"file:///home/project/src/context.xml")));
	}

	// Use case: FilePathMapping can match by namespace
	@Test
	public void testFilePathMapping_matchByNamespace() {
		FilePathMapping mapping = new FilePathMapping();
		mapping.setNamespaceURI(Arrays.asList("http://www.w3.org/1999/xhtml"));
		assertTrue(mapping.matches(parse(
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"/>",
				"file:///test.html")));
		assertFalse(mapping.matches(parse(
				"<root/>",
				"file:///test.xml")));
	}

	// Use case: XMLSymbolFilter can match by system ID
	@Test
	public void testXMLSymbolFilter_matchBySystemId() {
		XMLSymbolFilter filter = new XMLSymbolFilter();
		filter.setSystemId(Arrays.asList("http://mybatis.org/dtd/*"));
		assertTrue(filter.matches(parse(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
						+ "<mapper/>",
				"file:///mapper.xml")));
		assertFalse(filter.matches(parse(
				"<root/>",
				"file:///other.xml")));
	}

	// --- Helper ---

	private static DOMDocument parse(String content, String uri) {
		return DOMParser.getInstance().parse(content, uri, null);
	}
}
