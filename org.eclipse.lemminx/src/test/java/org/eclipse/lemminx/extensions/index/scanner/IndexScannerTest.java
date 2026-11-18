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
package org.eclipse.lemminx.extensions.index.scanner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IndexScanner} — token types, values, positions, and the
 * {@code maxChars} limit.
 */
public class IndexScannerTest {

	private static IndexScanner scanner(String xml) {
		return new IndexScanner(new StringReader(xml));
	}

	private static IndexScanner scanner(String xml, int maxChars) {
		return new IndexScanner(new StringReader(xml), maxChars);
	}

	/** Collects all token types until EOS. */
	private static List<IndexTokenType> tokenTypes(String xml) throws IOException {
		List<IndexTokenType> result = new ArrayList<>();
		try (IndexScanner s = scanner(xml)) {
			IndexTokenType t;
			while ((t = s.next()) != IndexTokenType.EOS) {
				result.add(t);
			}
		}
		return result;
	}

	// ---- Empty / trivial ----

	@Test
	public void testEmptyDocument() throws IOException {
		try (IndexScanner s = scanner("")) {
			assertEquals(IndexTokenType.EOS, s.next());
			assertFalse(s.isPartial());
		}
	}

	@Test
	public void testXmlDeclarationPI() throws IOException {
		try (IndexScanner s = scanner("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")) {
			assertEquals(IndexTokenType.PI_NAME, s.next());
			assertEquals("xml", s.getValue());
			assertEquals(IndexTokenType.PI_CONTENT, s.next());
			// content = everything between PI name and '?>'
			assertTrue(s.getValue().contains("version"));
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	// ---- Processing instructions ----

	@Test
	public void testXmlModelPINameAndContent() throws IOException {
		try (IndexScanner s = scanner("<?xml-model href=\"schema.xsd\"?>")) {
			assertEquals(IndexTokenType.PI_NAME, s.next());
			assertEquals("xml-model", s.getValue());
			assertEquals(IndexTokenType.PI_CONTENT, s.next());
			assertEquals("href=\"schema.xsd\"", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testXmlModelPIWithSingleQuotedHref() throws IOException {
		try (IndexScanner s = scanner("<?xml-model href='schema.xsd'?>")) {
			s.next(); // PI_NAME
			assertEquals(IndexTokenType.PI_CONTENT, s.next());
			assertEquals("href='schema.xsd'", s.getValue());
		}
	}

	@Test
	public void testPIFollowedByElement() throws IOException {
		List<IndexTokenType> types = tokenTypes("<?xml-model href=\"s.xsd\"?><root/>");
		assertEquals(List.of(IndexTokenType.PI_NAME, IndexTokenType.PI_CONTENT, IndexTokenType.START_TAG), types);
	}

	// ---- Start tags ----

	@Test
	public void testStartTagLocalName() throws IOException {
		try (IndexScanner s = scanner("<root/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals("root", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testStartTagStripsNamespacePrefix() throws IOException {
		try (IndexScanner s = scanner("<xs:element/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals("element", s.getValue());
		}
	}

	@Test
	public void testStartTagStripsLongPrefix() throws IOException {
		try (IndexScanner s = scanner("<wsdl:definitions/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals("definitions", s.getValue());
		}
	}

	@Test
	public void testMultipleStartTags() throws IOException {
		try (IndexScanner s = scanner("<a><b><c/></b></a>")) {
			assertEquals(IndexTokenType.START_TAG, s.next()); assertEquals("a", s.getValue());
			assertEquals(IndexTokenType.START_TAG, s.next()); assertEquals("b", s.getValue());
			assertEquals(IndexTokenType.START_TAG, s.next()); assertEquals("c", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	// ---- Attributes ----

	@Test
	public void testAttributeNameAndValue() throws IOException {
		try (IndexScanner s = scanner("<root attr=\"value\"/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next());
			assertEquals("attr", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals("value", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testAttributeNamePreservesNamespacePrefix() throws IOException {
		try (IndexScanner s = scanner("<root xsi:schemaLocation=\"ns path\"/>")) {
			s.next(); // START_TAG
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next());
			assertEquals("xsi:schemaLocation", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals("ns path", s.getValue());
		}
	}

	@Test
	public void testXmlnsAttribute() throws IOException {
		try (IndexScanner s = scanner("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>")) {
			s.next(); // START_TAG
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next());
			assertEquals("xmlns:xsi", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals("http://www.w3.org/2001/XMLSchema-instance", s.getValue());
		}
	}

	@Test
	public void testMultipleAttributes() throws IOException {
		try (IndexScanner s = scanner("<root a=\"1\" b=\"2\" c=\"3\"/>")) {
			s.next(); // START_TAG
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next()); assertEquals("a", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next()); assertEquals("1", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next()); assertEquals("b", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next()); assertEquals("2", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next()); assertEquals("c", s.getValue());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next()); assertEquals("3", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testSingleQuotedAttributeValue() throws IOException {
		try (IndexScanner s = scanner("<root attr='hello'/>")) {
			s.next(); // START_TAG
			s.next(); // ATTRIBUTE_NAME
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals("hello", s.getValue());
		}
	}

	@Test
	public void testAttributeWithEmptyValue() throws IOException {
		try (IndexScanner s = scanner("<root attr=\"\"/>")) {
			s.next(); // START_TAG
			s.next(); // ATTRIBUTE_NAME
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals("", s.getValue());
		}
	}

	// ---- DOCTYPE ----

	@Test
	public void testDoctypeSystemId() throws IOException {
		try (IndexScanner s = scanner("<!DOCTYPE root SYSTEM \"file.dtd\"><root/>")) {
			assertEquals(IndexTokenType.DOCTYPE_SYSTEM_ID, s.next());
			assertEquals("file.dtd", s.getValue());
			assertEquals(IndexTokenType.START_TAG, s.next());
		}
	}

	@Test
	public void testDoctypePublicAndSystemId() throws IOException {
		try (IndexScanner s = scanner("<!DOCTYPE root PUBLIC \"-//W3C//DTD//EN\" \"file.dtd\"><root/>")) {
			assertEquals(IndexTokenType.DOCTYPE_PUBLIC_ID, s.next());
			assertEquals("-//W3C//DTD//EN", s.getValue());
			assertEquals(IndexTokenType.DOCTYPE_SYSTEM_ID, s.next());
			assertEquals("file.dtd", s.getValue());
			assertEquals(IndexTokenType.START_TAG, s.next());
		}
	}

	@Test
	public void testDoctypeWithInternalSubset() throws IOException {
		try (IndexScanner s = scanner("<!DOCTYPE root SYSTEM \"file.dtd\" [<!ELEMENT root EMPTY>]><root/>")) {
			assertEquals(IndexTokenType.DOCTYPE_SYSTEM_ID, s.next());
			assertEquals("file.dtd", s.getValue());
			// internal subset is skipped
			assertEquals(IndexTokenType.START_TAG, s.next());
		}
	}

	// ---- Comment and CDATA skipping ----

	@Test
	public void testCommentSkipped() throws IOException {
		try (IndexScanner s = scanner("<!-- comment --><root/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals("root", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testCommentBetweenElements() throws IOException {
		try (IndexScanner s = scanner("<a><!-- skip --><b/></a>")) {
			assertEquals(IndexTokenType.START_TAG, s.next()); assertEquals("a", s.getValue());
			assertEquals(IndexTokenType.START_TAG, s.next()); assertEquals("b", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	@Test
	public void testCDATASkipped() throws IOException {
		try (IndexScanner s = scanner("<root><![CDATA[some & content <inner/>]]></root>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals("root", s.getValue());
			assertEquals(IndexTokenType.EOS, s.next());
		}
	}

	// ---- maxChars limit and isPartial() ----

	@Test
	public void testNoLimitFullScan() throws IOException {
		try (IndexScanner s = scanner("<root attr=\"val\"/>")) {
			while (s.next() != IndexTokenType.EOS) { }
			assertFalse(s.isPartial(), "Full scan must not be partial");
		}
	}

	@Test
	public void testMaxCharsZeroIsAlwaysPartial() throws IOException {
		try (IndexScanner s = scanner("<root/>", 0)) {
			while (s.next() != IndexTokenType.EOS) { }
			assertTrue(s.isPartial(), "0-char limit must trigger partial");
		}
	}

	@Test
	public void testMaxCharsLimitTriggersPartial() throws IOException {
		String xml = "<root attr=\"value\" other=\"data\"/>";
		// limit cuts into the middle of the element
		try (IndexScanner s = scanner(xml, 10)) {
			while (s.next() != IndexTokenType.EOS) { }
			assertTrue(s.isPartial(), "Short limit must trigger partial");
		}
	}

	@Test
	public void testMaxCharsExceedsFileLengthIsNotPartial() throws IOException {
		String xml = "<root/>";
		try (IndexScanner s = scanner(xml, xml.length() + 100)) {
			while (s.next() != IndexTokenType.EOS) { }
			assertFalse(s.isPartial(), "Limit beyond file length must not be partial");
		}
	}

	@Test
	public void testMaxCharsLargeFileCutsEarly() throws IOException {
		// Build a large element list; the limit cuts before reaching the end
		StringBuilder sb = new StringBuilder("<root>");
		for (int i = 0; i < 100; i++) {
			sb.append("<item a=\"v").append(i).append("\"/>");
		}
		sb.append("</root>");
		String xml = sb.toString();
		int limit = xml.length() / 2;
		try (IndexScanner s = scanner(xml, limit)) {
			while (s.next() != IndexTokenType.EOS) { }
			assertTrue(s.isPartial(), "Half-file limit must trigger partial");
		}
	}

	// ---- Position tracking ----

	@Test
	public void testStartTagPosition() throws IOException {
		// <root/>   col: 0123456
		// 'r' is at col 1 (after '<'), "root" is 4 chars → endChar = 1+4 = 5
		try (IndexScanner s = scanner("<root/>")) {
			assertEquals(IndexTokenType.START_TAG, s.next());
			assertEquals(0, s.getLine());
			assertEquals(1, s.getStartChar());
			assertEquals(5, s.getEndChar());
		}
	}

	@Test
	public void testAttributeNamePosition() throws IOException {
		// <root attr="val"/>
		//  0123456789...
		// 'a' in "attr" is at col 6
		try (IndexScanner s = scanner("<root attr=\"val\"/>")) {
			s.next(); // START_TAG
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next());
			assertEquals(0, s.getLine());
			assertEquals(6, s.getStartChar());  // 'a' at col 6
			assertEquals(10, s.getEndChar());   // 6 + 4 = 10
		}
	}

	@Test
	public void testAttributeValuePosition() throws IOException {
		// <root attr="val"/>
		//  012345678901234567
		// opening '"' at col 11, 'v' in "val" at col 12
		try (IndexScanner s = scanner("<root attr=\"val\"/>")) {
			s.next(); // START_TAG
			s.next(); // ATTRIBUTE_NAME
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals(0, s.getLine());
			assertEquals(12, s.getStartChar());  // 'v' after '"' at col 11 → col 12
			assertEquals(15, s.getEndChar());    // 12 + 3 = 15
		}
	}

	@Test
	public void testDoctypeSystemIdPosition() throws IOException {
		// <!DOCTYPE root SYSTEM "file.dtd">
		//  0         1         2         3
		//  0123456789012345678901234567890123
		// '"' at col 22, 'f' at col 23, "file.dtd" is 8 chars → endChar = 23+8 = 31
		try (IndexScanner s = scanner("<!DOCTYPE root SYSTEM \"file.dtd\">")) {
			assertEquals(IndexTokenType.DOCTYPE_SYSTEM_ID, s.next());
			assertEquals(0, s.getLine());
			assertEquals(23, s.getStartChar());
			assertEquals(31, s.getEndChar());
			assertEquals("file.dtd", s.getValue());
		}
	}

	@Test
	public void testLineTrackingAfterNewline() throws IOException {
		// "<root\n  attr=\"val\"/>" — attr is on line 1
		try (IndexScanner s = scanner("<root\n  attr=\"val\"/>")) {
			s.next(); // START_TAG (line 0)
			assertEquals(IndexTokenType.ATTRIBUTE_NAME, s.next());
			assertEquals(1, s.getLine());
			assertEquals(IndexTokenType.ATTRIBUTE_VALUE, s.next());
			assertEquals(1, s.getLine());
		}
	}

	@Test
	public void testXmlModelPIContentPosition() throws IOException {
		// <?xml-model href="schema.xsd"?>
		//  01234567890123456789012345678901
		// PI content starts at 'h' (col 12, after the space following "xml-model")
		try (IndexScanner s = scanner("<?xml-model href=\"schema.xsd\"?>")) {
			s.next(); // PI_NAME
			assertEquals(IndexTokenType.PI_CONTENT, s.next());
			assertEquals(0, s.getLine());
			assertEquals(12, s.getStartChar()); // 'h' in "href"
		}
	}
}
