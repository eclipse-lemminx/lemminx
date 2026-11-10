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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.sr;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testSelectionRangeFor;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code textDocument/selectionRange} on RNC files.
 *
 * <p>Each cursor position (marked with {@code |}) expands outward:
 * word at cursor → body content of enclosing element/attribute →
 * element/attribute node → define → document.</p>
 */
public class RNCSelectionRangeTest {

	// ---- Cursor on define name ----

	@Test
	public void testOnDefineName() throws BadLocationException {
		// Cursor at start of "root" (define name). The parser sets define.end to after
		// "=" only; the logical range spans the full "name = body" text.
		// "root = element root { text }"
		//  ^
		testSelectionRangeFor("|root = element root { text }",
				sr(
						r(0, 0, 0, 4),  // word "root" (== define name, deduplicated with name range)
						r(0, 0, 0, 28)  // full define (logical range: "root = element root { text }")
				));
	}

	@Test
	public void testOnDefineNameMid() throws BadLocationException {
		testSelectionRangeFor("ro|ot = element root { text }",
				sr(
						r(0, 0, 0, 4),  // word "root"
						r(0, 0, 0, 28)  // full define
				));
	}

	// ---- Cursor inside element body ----

	@Test
	public void testInsideElementBodyOnWord() throws BadLocationException {
		// "root = element root { text }"
		//                        ^
		testSelectionRangeFor("root = element root { |text }",
				sr(
						r(0, 22, 0, 26), // word "text"
						r(0, 21, 0, 27), // body content between { and }
						r(0, 7, 0, 28),  // element node
						r(0, 0, 0, 28)   // full define
				));
	}

	@Test
	public void testInsideElementBodyMidWord() throws BadLocationException {
		testSelectionRangeFor("root = element root { con|tent }",
				sr(
						r(0, 22, 0, 29), // word "content"
						r(0, 21, 0, 30), // body content between { and }
						r(0, 7, 0, 31),  // element node
						r(0, 0, 0, 31)   // full define
				));
	}

	// ---- Cursor inside attribute body ----

	@Test
	public void testInsideAttributeBody() throws BadLocationException {
		// root = element root {
		//   attribute id { text }
		//                  ^
		// }
		String value = "root = element root {" + LS +
				"  attribute id { |text }" + LS +
				"}";
		testSelectionRangeFor(value,
				sr(
						r(1, 17, 1, 21), // word "text"
						r(1, 16, 1, 22), // attribute body content between { and }
						r(1, 2, 1, 23),  // attribute node
						r(0, 21, 2, 0),  // element body content
						r(0, 7, 2, 1),   // element node
						r(0, 0, 2, 1)    // full define
				));
	}

	@Test
	public void testInsideNestedAttribute() throws BadLocationException {
		String value = "root = element root {" + LS +
				"  attribute id { |text }," + LS +
				"  attribute class { text }" + LS +
				"}";
		testSelectionRangeFor(value,
				sr(
						r(1, 17, 1, 21), // word "text"
						r(1, 16, 1, 22), // attribute body content between { and }
						r(1, 2, 1, 23),  // attribute node
						r(0, 21, 3, 0),  // element body content
						r(0, 7, 3, 1),   // element node
						r(0, 0, 3, 1)    // full define
				));
	}

	// ---- Cursor on start declaration ----

	@Test
	public void testOnStartKeyword() throws BadLocationException {
		// "|start = root" — 12 chars total
		testSelectionRangeFor("|start = root",
				sr(
						r(0, 0, 0, 5),  // word "start"
						r(0, 0, 0, 12)  // full start node
				));
	}

	@Test
	public void testOnStartValue() throws BadLocationException {
		// "start = r|oot"
		testSelectionRangeFor("start = r|oot",
				sr(
						r(0, 8, 0, 12), // word "root"
						r(0, 0, 0, 12)  // full start node
				));
	}

	// ---- Cursor on namespace / datatypes declaration ----

	@Test
	public void testOnNamespaceDecl() throws BadLocationException {
		// "namespace xsd = ..." — 60 chars total
		testSelectionRangeFor("namespace |xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"",
				sr(
						r(0, 10, 0, 13), // word "xsd"
						r(0, 0, 0, 60)   // full namespace decl node
				));
	}

	@Test
	public void testOnDatatypesDecl() throws BadLocationException {
		// "datatypes xsd = ..." — 60 chars total
		testSelectionRangeFor("datatypes |xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"",
				sr(
						r(0, 10, 0, 13), // word "xsd"
						r(0, 0, 0, 60)   // full datatypes decl node
				));
	}

	// ---- Cursor on element keyword / name ----

	@Test
	public void testOnElementKeyword() throws BadLocationException {
		// "root = e|lement root { text }"
		//           ^-- cursor on "element" keyword itself
		testSelectionRangeFor("root = e|lement root { text }",
				sr(
						r(0, 7, 0, 14), // word "element"
						r(0, 7, 0, 28), // element node (cursor before {, no body content range)
						r(0, 0, 0, 28)  // full define
				));
	}

	@Test
	public void testOnElementName() throws BadLocationException {
		// "root = element r|oot { text }"
		//                  ^-- cursor on the element's name "root"
		testSelectionRangeFor("root = element r|oot { text }",
				sr(
						r(0, 15, 0, 19), // word "root"
						r(0, 7, 0, 28),  // element node (cursor before {, no body content range)
						r(0, 0, 0, 28)   // full define
				));
	}

	// ---- Quoted names on element and attribute ----

	@Test
	public void testElementWithQuotedName() throws BadLocationException {
		// root = element "my-element" { text }
		//                  ^-- cursor inside the quoted name
		// The parser does not set a full range for elements with quoted names
		// (isNameToken returns false for string literals), so the element node
		// is not found by findNodeAt at this offset; the chain stops at document.
		testSelectionRangeFor("root = element \"my-e|lement\" { text }",
				sr(
						r(0, 16, 0, 26), // word "my-element" (identifier chars; quotes excluded)
						r(0, 0, 0, 36)   // document/define
				));
	}

	@Test
	public void testAttributeWithQuotedName() throws BadLocationException {
		// root = element root { attribute "xml:lang" { text } }
		//                                   ^-- cursor in "xml:lang"
		// Same parser limitation as testElementWithQuotedName: the attribute node
		// range covers only the "attribute" keyword, so findNodeAt returns the
		// enclosing element instead of the attribute for this offset.
		testSelectionRangeFor("root = element root { attribute \"xml:l|ang\" { text } }",
				sr(
						r(0, 33, 0, 41), // word "xml:lang" (colon is identifier char)
						r(0, 21, 0, 52), // element body content
						r(0, 7, 0, 53),  // element node
						r(0, 0, 0, 53)   // full define
				));
	}

	// ---- String literal ("foo") as body content ----

	@Test
	public void testStringLiteralInElementBody() throws BadLocationException {
		// root = element root { "hello" }
		//                        ^-- cursor inside the string value
		testSelectionRangeFor("root = element root { \"hel|lo\" }",
				sr(
						r(0, 23, 0, 28), // word "hello" (letters inside the quotes)
						r(0, 21, 0, 30), // element body content between { and }
						r(0, 7, 0, 31),  // element node
						r(0, 0, 0, 31)   // full define
				));
	}

	@Test
	public void testStringLiteralInAttributeBody() throws BadLocationException {
		// root = element root {
		//   attribute lang { "en" }
		//                     ^-- cursor inside fixed value "en"
		// }
		String value = "root = element root {" + LS +
				"  attribute lang { \"e|n\" }" + LS +
				"}";
		testSelectionRangeFor(value,
				sr(
						r(1, 20, 1, 22), // word "en"
						r(1, 18, 1, 24), // attribute body content between { and }
						r(1, 2, 1, 25),  // attribute node
						r(0, 21, 2, 0),  // element body content
						r(0, 7, 2, 1),   // element node
						r(0, 0, 2, 1)    // full define
				));
	}

	// ---- The reviewer's exact scenario ----
	// When the cursor is in a datatype reference inside an element body,
	// expanding selection must NOT jump to the entire document immediately.

	@Test
	public void testDatatypeReferenceInElementBody() throws BadLocationException {
		// song = element tune { compat: }
		//                        ^-- cursor inside "compat:"
		testSelectionRangeFor("song = element tune { com|pat: }",
				sr(
						r(0, 22, 0, 29), // word "compat:" (colon is an identifier char)
						r(0, 21, 0, 30), // element body content between { and }
						r(0, 7, 0, 31),  // element node
						r(0, 0, 0, 31)   // full define (song = ...)
				));
	}

	@Test
	public void testDatatypeReferenceMultilineDoc() throws BadLocationException {
		// Same scenario with surrounding definitions. The "song" define does not span
		// the whole document (there is a datatypes decl above), so the document range
		// is added as a separate step after the define range.
		String value = "datatypes compat = \"http://relaxng.org/ns/compatibility/datatypes/1.0\"" + LS +
				"" + LS +
				"song = element tune { com|pat: }";
		testSelectionRangeFor(value,
				sr(
						r(2, 22, 2, 29), // word "compat:"
						r(2, 21, 2, 30), // element body content between { and }
						r(2, 7, 2, 31),  // element node
						r(2, 0, 2, 31),  // define "song = element tune { compat: }"
						r(0, 0, 2, 31)   // document
				));
	}

	// ---- Multiple cursors ----

	@Test
	public void testMultipleCursors() throws BadLocationException {
		// Two defines: "root = ..." and "content = text". Each define's logical range
		// does not span the whole document, so the document range is added after the
		// define range as a separate step.
		String value = "|root = element root { text }" + LS +
				"|content = text";
		testSelectionRangeFor(value,
				sr(
						r(0, 0, 0, 4),  // word "root"
						r(0, 0, 0, 28), // define "root = element root { text }"
						r(0, 0, 1, 14)  // document
				),
				sr(
						r(1, 0, 1, 7),  // word "content"
						r(1, 0, 1, 14), // define "content = text"
						r(0, 0, 1, 14)  // document
				));
	}

	// ---- Multi-alternative define ----

	@Test
	public void testMultiAlternativeDefine() throws BadLocationException {
		// pattern = element first { text }
		//         | element thi|rd { text }
		// other = text
		//
		// The RNC alternation operator "|" in "        | element" is also detected
		// as a cursor position by the test framework. We therefore supply two expected
		// SelectionRanges: one for the pipe operator position (no word → just the
		// document range) and one for the cursor inside "third".
		String value = "pattern = element first { text }" + LS +
				"        | element thi|rd { text }" + LS +
				"other = text";
		testSelectionRangeFor(value,
				// cursor 1: the "|" alternation operator — not an identifier char so
				//            getWordRange returns a zero-width range at that position,
				//            and no AST node covers the gap so only the document range follows
				sr(r(1, 8, 1, 8), r(0, 0, 2, 12)),
				// cursor 2: inside "third"
				// Note: the framework removes the first "|" before computing this cursor,
				// so all column positions on line 1 shift by 1 (element col 10→9, etc.)
				sr(
						r(1, 17, 1, 22), // word "third"
						r(1, 9, 1, 31),  // element "third" (cursor before {, no body range)
						r(0, 0, 1, 31),  // "pattern" define (first + third)
						r(0, 0, 2, 12)   // document
				));
	}

	// ---- Include and div ----

	@Test
	public void testOnIncludeKeyword() throws BadLocationException {
		// "inc|lude "types.rnc"" — 19 chars
		testSelectionRangeFor("inc|lude \"types.rnc\"",
				sr(
						r(0, 0, 0, 7),  // word "include"
						r(0, 0, 0, 19)  // full include node
				));
	}

	@Test
	public void testOnDivKeyword() throws BadLocationException {
		String value = "d|iv {" + LS +
				"  root = element root { text }" + LS +
				"}";
		testSelectionRangeFor(value,
				sr(
						r(0, 0, 0, 3), // word "div"
						r(0, 0, 2, 1)  // full div node
				));
	}
}
