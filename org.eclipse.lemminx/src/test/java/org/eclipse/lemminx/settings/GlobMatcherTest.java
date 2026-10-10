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

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlobMatcher} — textual glob matching for non-file-path
 * strings (namespace URIs, DOCTYPE public/system IDs, grammar URIs).
 *
 * <p>
 * Wildcards:
 * </p>
 * <ul>
 * <li>{@code *} — matches any sequence of characters (including empty)</li>
 * <li>{@code ?} — matches exactly one character</li>
 * </ul>
 */
public class GlobMatcherTest {

	// ==========================================
	// Exact match (no wildcards)
	// ==========================================

	// Use case: exact namespace URI match
	@Test
	public void testExactMatch() {
		assertTrue(GlobMatcher.match("http://docbook.org/ns/docbook", "http://docbook.org/ns/docbook"));
	}

	// Use case: exact string, different text
	@Test
	public void testExactNoMatch() {
		assertFalse(GlobMatcher.match("http://docbook.org/ns/docbook", "http://docbook.org/ns/other"));
	}

	// Use case: exact match — text has trailing characters
	@Test
	public void testExactNoMatch_textLonger() {
		assertFalse(GlobMatcher.match("http://docbook.org/ns/docbook", "http://docbook.org/ns/docbook/5.1"));
	}

	// Use case: exact match — pattern is longer than text
	@Test
	public void testExactNoMatch_patternLonger() {
		assertFalse(GlobMatcher.match("http://docbook.org/ns/docbook/5.1", "http://docbook.org/ns/docbook"));
	}

	// ==========================================
	// Star wildcard (*)
	// ==========================================

	// Use case: trailing wildcard matches empty string
	@Test
	public void testStar_trailingMatchesEmpty() {
		assertTrue(GlobMatcher.match("http://docbook.org/ns/docbook*", "http://docbook.org/ns/docbook"));
	}

	// Use case: trailing wildcard matches additional path segments
	@Test
	public void testStar_trailingMatchesMore() {
		assertTrue(GlobMatcher.match("http://docbook.org/ns/docbook*", "http://docbook.org/ns/docbook/5.1"));
	}

	// Use case: trailing wildcard doesn't match different prefix
	@Test
	public void testStar_trailingNoMatch() {
		assertFalse(GlobMatcher.match("http://docbook.org/ns/docbook*", "http://docbook.org/ns/other"));
	}

	// Use case: leading wildcard
	@Test
	public void testStar_leading() {
		assertTrue(GlobMatcher.match("*docbook", "http://docbook.org/ns/docbook"));
		assertFalse(GlobMatcher.match("*docbook", "http://docbook.org/ns/docbook/5.1"));
	}

	// Use case: wildcard in the middle — match any DTD version
	@Test
	public void testStar_middle() {
		assertTrue(GlobMatcher.match("-//mybatis.org//DTD Mapper*//EN", "-//mybatis.org//DTD Mapper 3.0//EN"));
	}

	// Use case: multiple wildcards
	@Test
	public void testStar_multiple() {
		assertTrue(GlobMatcher.match("*docbook*", "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match("*docbook*", "docbook"));
		assertTrue(GlobMatcher.match("*docbook*", "docbook/5.1"));
		assertFalse(GlobMatcher.match("*docbook*", "maven"));
	}

	// Use case: star matches empty string
	@Test
	public void testStar_matchesEmptyInMiddle() {
		assertTrue(GlobMatcher.match("a*b", "ab"));
		assertTrue(GlobMatcher.match("a*b", "axb"));
		assertTrue(GlobMatcher.match("a*b", "axyb"));
		assertFalse(GlobMatcher.match("a*b", "axc"));
	}

	// Use case: star alone matches everything
	@Test
	public void testStar_alone() {
		assertTrue(GlobMatcher.match("*", ""));
		assertTrue(GlobMatcher.match("*", "anything"));
		assertTrue(GlobMatcher.match("*", "http://some/uri/here"));
	}

	// Use case: consecutive stars
	@Test
	public void testStar_consecutive() {
		assertTrue(GlobMatcher.match("**", ""));
		assertTrue(GlobMatcher.match("**", "anything"));
		assertTrue(GlobMatcher.match("a**b", "ab"));
		assertTrue(GlobMatcher.match("a**b", "aXYZb"));
	}

	// ==========================================
	// Question mark wildcard (?)
	// ==========================================

	// Use case: ? matches exactly one character
	@Test
	public void testQuestion_singleChar() {
		assertTrue(GlobMatcher.match("test?xml", "test.xml"));
		assertTrue(GlobMatcher.match("test?xml", "test_xml"));
	}

	// Use case: ? doesn't match empty
	@Test
	public void testQuestion_noMatchEmpty() {
		assertFalse(GlobMatcher.match("test?xml", "testxml"));
	}

	// Use case: ? doesn't match multiple chars
	@Test
	public void testQuestion_noMatchMultiple() {
		assertFalse(GlobMatcher.match("test?xml", "test..xml"));
	}

	// Use case: multiple question marks
	@Test
	public void testQuestion_multiple() {
		assertTrue(GlobMatcher.match("v?.?", "v1.0"));
		assertTrue(GlobMatcher.match("v?.?", "v2.5"));
		assertFalse(GlobMatcher.match("v?.?", "v10.0"));
	}

	// ==========================================
	// Mixed wildcards
	// ==========================================

	// Use case: ? and * combined — ? matches 's' in https, * matches the rest
	@Test
	public void testMixed_questionAndStar() {
		assertTrue(GlobMatcher.match("http?://*", "https://example.com"));
		assertTrue(GlobMatcher.match("http?://*", "httpa://example.com"));
		assertFalse(GlobMatcher.match("http?://*", "ftp://example.com"));
	}

	// Use case: difference between ? and * on the same strings
	// ? matches exactly one character, * matches any sequence (including empty)
	@Test
	public void testDifferenceBetweenQuestionAndStar() {
		// ? requires exactly one character
		assertTrue(GlobMatcher.match("a?c", "abc"));
		assertTrue(GlobMatcher.match("a?c", "axc"));
		assertFalse(GlobMatcher.match("a?c", "ac"));
		assertFalse(GlobMatcher.match("a?c", "axyc"));

		// * matches zero or more characters
		assertTrue(GlobMatcher.match("a*c", "abc"));
		assertTrue(GlobMatcher.match("a*c", "axc"));
		assertTrue(GlobMatcher.match("a*c", "ac"));
		assertTrue(GlobMatcher.match("a*c", "axyc"));

		// real-world: http vs https — ? requires exactly one char between "http" and "://"
		assertTrue(GlobMatcher.match("http?://example.com", "https://example.com"));
		assertFalse(GlobMatcher.match("http?://example.com", "http://example.com"));

		// * allows zero or more chars between "http" and "://"
		assertTrue(GlobMatcher.match("http*://example.com", "https://example.com"));
		assertTrue(GlobMatcher.match("http*://example.com", "http://example.com"));
	}

	// ==========================================
	// Empty strings
	// ==========================================

	// Use case: both empty
	@Test
	public void testEmpty_both() {
		assertTrue(GlobMatcher.match("", ""));
	}

	// Use case: empty pattern, non-empty text
	@Test
	public void testEmpty_pattern() {
		assertFalse(GlobMatcher.match("", "something"));
	}

	// Use case: non-empty pattern, empty text
	@Test
	public void testEmpty_text() {
		assertFalse(GlobMatcher.match("hello", ""));
	}

	// Use case: star pattern, empty text
	@Test
	public void testEmpty_starPattern() {
		assertTrue(GlobMatcher.match("*", ""));
	}

	// ==========================================
	// Real-world use cases
	// ==========================================

	// Use case: DocBook namespace versions
	@Test
	public void testRealWorld_docbookVersions() {
		String pattern = "http://docbook.org/ns/docbook*";
		assertTrue(GlobMatcher.match(pattern, "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match(pattern, "http://docbook.org/ns/docbook/5.0"));
		assertTrue(GlobMatcher.match(pattern, "http://docbook.org/ns/docbook/5.1"));
		assertFalse(GlobMatcher.match(pattern, "http://docbook.org/ns/slides"));
	}

	// Use case: MyBatis DTD public IDs
	@Test
	public void testRealWorld_mybatisPublicIds() {
		String pattern = "-//mybatis.org//DTD *//EN";
		assertTrue(GlobMatcher.match(pattern, "-//mybatis.org//DTD Mapper 3.0//EN"));
		assertTrue(GlobMatcher.match(pattern, "-//mybatis.org//DTD Config 3.0//EN"));
		assertFalse(GlobMatcher.match(pattern, "-//W3C//DTD XHTML 1.0//EN"));
	}

	// Use case: MyBatis DTD system IDs
	@Test
	public void testRealWorld_mybatisSystemIds() {
		String pattern = "http://mybatis.org/dtd/*";
		assertTrue(GlobMatcher.match(pattern, "http://mybatis.org/dtd/mybatis-3-mapper.dtd"));
		assertTrue(GlobMatcher.match(pattern, "http://mybatis.org/dtd/mybatis-3-config.dtd"));
		assertFalse(GlobMatcher.match(pattern, "http://other.org/dtd/something.dtd"));
	}

	// Use case: XHTML namespace
	@Test
	public void testRealWorld_xhtml() {
		assertTrue(GlobMatcher.match("http://www.w3.org/1999/xhtml", "http://www.w3.org/1999/xhtml"));
		assertFalse(GlobMatcher.match("http://www.w3.org/1999/xhtml", "http://www.w3.org/2000/svg"));
	}

	// Use case: Spring DTD
	@Test
	public void testRealWorld_springDtd() {
		String pattern = "-//SPRING//DTD BEAN*";
		assertTrue(GlobMatcher.match(pattern, "-//SPRING//DTD BEAN 2.0//EN"));
		assertTrue(GlobMatcher.match(pattern, "-//SPRING//DTD BEAN//EN"));
		assertFalse(GlobMatcher.match(pattern, "-//SPRING//DTD WEBFLOW//EN"));
	}

	// Use case: grammar URI with schema file
	@Test
	public void testRealWorld_grammarUri() {
		String pattern = "*docbook.rng*";
		assertTrue(GlobMatcher.match(pattern, "http://docbook.org/xml/5.0/rng/docbook.rng"));
		assertTrue(GlobMatcher.match(pattern, "file:///usr/share/xml/docbook/schema/rng/docbook.rng"));
		assertTrue(GlobMatcher.match(pattern, "docbook.rng"));
		assertFalse(GlobMatcher.match(pattern, "maven.xsd"));
	}

	// ==========================================
	// matchAny tests
	// ==========================================

	// Use case: check namespace against multiple patterns
	@Test
	public void testMatchAny_found() {
		assertTrue(GlobMatcher.matchAny(
				Arrays.asList("http://docbook.org/ns/docbook*", "http://www.w3.org/1999/xhtml"),
				"http://docbook.org/ns/docbook/5.1"));
	}

	// Use case: no pattern matches
	@Test
	public void testMatchAny_notFound() {
		assertFalse(GlobMatcher.matchAny(
				Arrays.asList("http://docbook.org/ns/docbook*", "http://www.w3.org/1999/xhtml"),
				"http://maven.apache.org/POM/4.0.0"));
	}

	// Use case: null value
	@Test
	public void testMatchAny_nullValue() {
		assertFalse(GlobMatcher.matchAny(Arrays.asList("*"), null));
	}

	// Use case: empty list
	@Test
	public void testMatchAny_emptyList() {
		assertFalse(GlobMatcher.matchAny(Collections.emptyList(), "something"));
	}

	// Use case: list with null entries
	@Test
	public void testMatchAny_nullEntries() {
		assertTrue(GlobMatcher.matchAny(Arrays.asList(null, "test*"), "testing"));
		assertFalse(GlobMatcher.matchAny(Arrays.asList(null, null), "testing"));
	}

	// ==========================================
	// Multiple wildcards at various positions
	// ==========================================

	// Use case: * at start, middle, and end
	@Test
	public void testStar_startMiddleEnd() {
		assertTrue(GlobMatcher.match("*org*docbook*", "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match("*org*docbook*", "docbook.org/ns/docbook/5.1"));
		assertFalse(GlobMatcher.match("*org*docbook*", "http://maven.apache.org/POM"));
	}

	// Use case: two * wildcards sandwiching exact text
	@Test
	public void testStar_twoStarsExactMiddle() {
		assertTrue(GlobMatcher.match("*/dtd/*mapper*", "http://mybatis.org/dtd/mybatis-3-mapper.dtd"));
		assertFalse(GlobMatcher.match("*/dtd/*mapper*", "http://mybatis.org/schema/config.xsd"));
	}

	// Use case: ** behaves like two consecutive * (each matches any chars)
	@Test
	public void testDoubleStar_behavesLikeStar() {
		assertTrue(GlobMatcher.match("**", ""));
		assertTrue(GlobMatcher.match("**", "anything"));
		assertTrue(GlobMatcher.match("**", "multi/path/value"));
		assertTrue(GlobMatcher.match("http:**docbook*", "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match("http:**docbook*", "http://docbook.org/ns/docbook/5.1"));
	}

	// Use case: ? at multiple positions
	@Test
	public void testQuestion_multiplePositions() {
		assertTrue(GlobMatcher.match("?ttp://*", "http://example.com"));
		assertTrue(GlobMatcher.match("h??p://*", "http://example.com"));
		assertFalse(GlobMatcher.match("?ttp://*", "mailto:someone"));
	}

	// Use case: complex pattern with *, ?, and literal text
	@Test
	public void testComplex_mixedWildcards() {
		assertTrue(GlobMatcher.match("http?://*mybatis*/dtd/*", "https://mybatis.org/dtd/mybatis-3-mapper.dtd"));
		assertTrue(GlobMatcher.match("https://mybatis.?rg/dtd/*", "https://mybatis.org/dtd/config.dtd"));
		assertFalse(GlobMatcher.match("http?://*mybatis*/dtd/*", "ftp://mybatis.org/dtd/config.dtd"));
	}

	// Use case: pattern with * matching across slash boundaries (URI-style)
	@Test
	public void testStar_crossesSlash() {
		assertTrue(GlobMatcher.match("http://*/ns/*", "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match("*://*/ns/*", "http://docbook.org/ns/docbook"));
		assertTrue(GlobMatcher.match("*://*/ns/*", "https://docbook.org/ns/docbook/5.1"));
	}

	// Use case: three wildcards
	@Test
	public void testStar_three() {
		assertTrue(GlobMatcher.match("*a*b*c*", "abc"));
		assertTrue(GlobMatcher.match("*a*b*c*", "XaYbZcW"));
		assertFalse(GlobMatcher.match("*a*b*c*", "acb"));
	}

	// Use case: pathological backtracking — patterns with many stars must
	// complete in O(n·m), not O(n^k). A recursive split-point search would
	// hang on this input; the iterative greedy algorithm handles it instantly.
	@Test
	public void testStar_noExponentialBacktracking() {
		String text = "a".repeat(200);
		assertFalse(GlobMatcher.match("*a*a*a*a*b", text));
	}
}
