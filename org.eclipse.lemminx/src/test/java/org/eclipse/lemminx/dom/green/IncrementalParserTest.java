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
package org.eclipse.lemminx.dom.green;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IncrementalParser}.
 */
public class IncrementalParserTest {

	@Test
	public void insertTextInElement() {
		String oldText = "<root><a>hello</a><b>world</b></root>";
		String newText = "<root><a>helloX</a><b>world</b></root>";
		// Insert 'X' at offset 14 (after "hello", before "</a>")
		assertIncrementalParse(oldText, newText, 14, 0, 1);
	}

	@Test
	public void deleteTextInElement() {
		String oldText = "<root><a>hello</a><b>world</b></root>";
		String newText = "<root><a>hell</a><b>world</b></root>";
		// Delete 'o' at offset 13
		assertIncrementalParse(oldText, newText, 13, 1, 0);
	}

	@Test
	public void replaceTextInElement() {
		String oldText = "<root><a>hello</a><b>world</b></root>";
		String newText = "<root><a>hi</a><b>world</b></root>";
		// Replace "hello" (offset 9, length 5) with "hi" (length 2)
		assertIncrementalParse(oldText, newText, 9, 5, 2);
	}

	@Test
	public void insertInNestedElement() {
		String oldText = "<root><outer><inner>text</inner></outer></root>";
		String newText = "<root><outer><inner>textX</inner></outer></root>";
		// Insert 'X' at offset 23 (after "text")
		assertIncrementalParse(oldText, newText, 23, 0, 1);
	}

	@Test
	public void insertAttributeValue() {
		String oldText = "<root><a id=\"old\">text</a></root>";
		String newText = "<root><a id=\"new\">text</a></root>";
		// Replace "old" with "new" at offset 13
		assertIncrementalParse(oldText, newText, 13, 3, 3);
	}

	@Test
	public void insertInLargeDocument() {
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		for (int i = 0; i < 100; i++) {
			sb.append("  <item id=\"").append(i).append("\">value").append(i).append("</item>\n");
		}
		sb.append("</root>");
		String oldText = sb.toString();

		// Find where item 50 is and modify its text
		int idx = oldText.indexOf(">value50<");
		String newText = oldText.substring(0, idx + 1) + "MODIFIED50" + oldText.substring(idx + 8);
		// Replace "value50" (7 chars) with "MODIFIED50" (10 chars) at offset idx+1
		assertIncrementalParse(oldText, newText, idx + 1, 7, 10);
	}

	@Test
	public void structuralSharingTopLevel() {
		// Structural sharing works at document level: prolog and comment
		// are reused when editing inside the root element
		String oldText = "<?xml version=\"1.0\"?><!-- comment --><root>text</root>";
		String newText = "<?xml version=\"1.0\"?><!-- comment --><root>textX</root>";

		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenNode oldProlog = oldDoc.child(0);
		GreenNode oldComment = oldDoc.child(1);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 46, 0, 1, "test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		assertSame(oldProlog, newDoc.child(0),
				"Prolog should be structurally shared (before edit)");
		assertSame(oldComment, newDoc.child(1),
				"Comment should be structurally shared (before edit)");
		assertNotSame(oldDoc.child(2), newDoc.child(2),
				"Root element should be new (contains the edit)");
	}

	@Test
	public void fallbackOnTagNameChange() {
		String oldText = "<root><a>text</a></root>";
		String newText = "<root><b>text</b></root>";
		// Change tag name — should fall back to full reparse but still produce correct result
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 7, 1, 1, "test.xml", null);
		assertNotNull(newDoc);
		assertEquals(newText.length(), newDoc.width());
	}

	@Test
	public void fallbackOnEmptyOldDoc() {
		String oldText = "";
		String newText = "<root/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 0, 7, "test.xml", null);
		assertNotNull(newDoc);
		assertEquals(newText.length(), newDoc.width());
	}

	@Test
	public void fallbackOnMultiNodeInsert() {
		String oldText = "<root><a>text</a></root>";
		String newText = "<root><a>text</a><b/></root>";
		// Insert '<b/>' after </a> — creates new sibling, should fall back
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 17, 0, 4, "test.xml", null);
		assertNotNull(newDoc);
		assertEquals(newText.length(), newDoc.width());
	}

	@Test
	public void resultMatchesFullParse() {
		String oldText = "<root><a>hello</a><b>world</b></root>";
		String newText = "<root><a>hello there</a><b>world</b></root>";
		// Insert " there" at offset 14
		assertIncrementalMatchesFullParse(oldText, newText, 14, 0, 6);
	}

	@Test
	public void resultMatchesFullParseOnDelete() {
		String oldText = "<root attr=\"value\"><child>some text</child></root>";
		String newText = "<root attr=\"value\"><child>text</child></root>";
		// Delete "some " at offset 26
		assertIncrementalMatchesFullParse(oldText, newText, 26, 5, 0);
	}

	@Test
	public void resultMatchesFullParseNestedEdit() {
		String oldText = "<a><b><c>deep</c></b></a>";
		String newText = "<a><b><c>deeper</c></b></a>";
		assertIncrementalMatchesFullParse(oldText, newText, 12, 1, 3);
	}

	@Test
	public void insertNewChildElement() {
		String oldText = "<root><a>text</a></root>";
		String newText = "<root><a>text<b/></a></root>";
		// Insert '<b/>' inside <a> at offset 13
		assertIncrementalMatchesFullParse(oldText, newText, 13, 0, 4);
	}

	@Test
	public void editInDocWithProlog() {
		String oldText = "<?xml version=\"1.0\"?>\n<root><a>text</a></root>";
		String newText = "<?xml version=\"1.0\"?>\n<root><a>textX</a></root>";
		assertIncrementalMatchesFullParse(oldText, newText, 34, 0, 1);
	}

	@Test
	public void editInDocWithComment() {
		String oldText = "<root><!-- comment --><a>text</a></root>";
		String newText = "<root><!-- comment --><a>textX</a></root>";
		assertIncrementalMatchesFullParse(oldText, newText, 28, 0, 1);
	}

	@Test
	public void multipleTopLevelElementsEditMiddle() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><bx/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 5, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(2), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void multipleTopLevelElementsEditFirst() {
		String oldText = "<a/><b/><c/>";
		String newText = "<ax/><b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 4, 5, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(1), newDoc.child(1));
		assertSame(oldDoc.child(2), newDoc.child(2));
	}

	@Test
	public void multipleTopLevelElementsEditLast() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><b/><cx/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 8, 4, 5, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(1));
	}

	@Test
	public void insertElementBetween() {
		String oldText = "<a/><c/>";
		String newText = "<a/><b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 0, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void deleteElementFromMiddle() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 0, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(2), newDoc.child(1));
	}

	@Test
	public void appendElementAtEnd() {
		String oldText = "<a/><b/>";
		String newText = "<a/><b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 8, 0, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(1));
	}

	@Test
	public void prependElementAtStart() {
		String oldText = "<a/><b/>";
		String newText = "<z/><a/><b/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 0, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(1));
		assertSame(oldDoc.child(1), newDoc.child(2));
	}

	@Test
	public void unclosedElementInvalidatesSuffix() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><b><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 3, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void largeDocumentPrefixSuffixReuse() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 200; i++) {
			sb.append("<item").append(i).append("/>");
		}
		String oldText = sb.toString();
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);

		int editOffset = 0;
		for (int i = 0; i < 100; i++) {
			editOffset += oldDoc.child(i).width();
		}
		int oldChildWidth = oldDoc.child(100).width();

		String replacement = "<modified/>";
		String newText = oldText.substring(0, editOffset) + replacement
				+ oldText.substring(editOffset + oldChildWidth);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, editOffset, oldChildWidth, replacement.length(),
				"test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		for (int i = 0; i < 100; i++) {
			assertSame(oldDoc.child(i), newDoc.child(i),
					"Prefix child " + i + " should be reused");
		}
		for (int i = 101; i < 200; i++) {
			assertSame(oldDoc.child(i), newDoc.child(i),
					"Suffix child " + i + " should be reused");
		}
	}

	@Test
	public void editWithWhitespaceGaps() {
		String oldText = "<a/>\n<b/>\n<c/>\n<d/>";
		String newText = "<a/>\n<bx/>\n<c/>\n<d/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 5, 4, 5, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void singleRootElementFallsBack() {
		String oldText = "<root>text</root>";
		String newText = "<root>textX</root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 10, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void parseRangeSingleElement() {
		String text = "<a/><b/><c/>";
		GreenDocument rangeDoc = GreenTreeBuilder.parseRange(
				text, "test.xml", 4, 8, null);
		assertEquals(4, rangeDoc.width());
		assertEquals(1, rangeDoc.childCount());
		GreenElement b = (GreenElement) rangeDoc.child(0);
		assertEquals("b", b.tag());
	}

	@Test
	public void parseRangeMultipleElements() {
		String text = "<a/><b/><c/><d/>";
		GreenDocument rangeDoc = GreenTreeBuilder.parseRange(
				text, "test.xml", 4, 12, null);
		assertEquals(8, rangeDoc.width());
		assertEquals(2, rangeDoc.childCount());
	}

	@Test
	public void parseRangeFullDocument() {
		String text = "<a/><b/><c/>";
		GreenDocument rangeDoc = GreenTreeBuilder.parseRange(
				text, "test.xml", 0, text.length(), null);
		GreenDocument fullDoc = GreenTreeBuilder.parse(text, "test.xml", null);
		assertGreenTreesEqual(fullDoc, rangeDoc, "");
	}

	@Test
	public void editPreservesDocWidth() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><longer-element/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 16, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
	}

	@Test
	public void replaceAtPositionZero() {
		String oldText = "<a/><b/><c/>";
		String newText = "<x/><b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 4, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(1), newDoc.child(1));
		assertSame(oldDoc.child(2), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void deleteAtPositionZero() {
		String oldText = "<a/><b/><c/>";
		String newText = "<b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 4, 0, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(1), newDoc.child(0));
		assertSame(oldDoc.child(2), newDoc.child(1));
	}

	@Test
	public void deleteLastElement() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><b/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 8, 4, 0, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(1));
	}

	@Test
	public void editSpanningTwoNodes() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><x/><c/>";
		// Replace <b/> entirely
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editWithDoctype() {
		String oldText = "<?xml version=\"1.0\"?><!DOCTYPE root><root>text</root>";
		String newText = "<?xml version=\"1.0\"?><!DOCTYPE root><root>textX</root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenNode oldProlog = oldDoc.child(0);
		GreenNode oldDoctype = oldDoc.child(1);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 45, 0, 1, "test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		assertSame(oldProlog, newDoc.child(0),
				"Prolog should be reused");
		assertSame(oldDoctype, newDoc.child(1),
				"Doctype should be reused");
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editInsertMalformedXml() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><b<c/>";
		// Delete "/>" from <b/> — creates malformed XML
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 6, 2, 0, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editInCDATASection() {
		String oldText = "<a/><b><![CDATA[data]]></b><c/>";
		String newText = "<a/><b><![CDATA[dataX]]></b><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 19, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editInComment() {
		String oldText = "<a/><!-- comment --><b/>";
		String newText = "<a/><!-- commentX --><b/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 15, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void replaceEntireDocument() {
		String oldText = "<root>old</root>";
		String newText = "<new>content</new>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, oldText.length(), newText.length(), "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editWithProcessingInstruction() {
		String oldText = "<?xml version=\"1.0\"?><?pi target?><root>text</root>";
		String newText = "<?xml version=\"1.0\"?><?pi target?><root>textX</root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 43, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(1));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editSelfClosingToOpen() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/><b>text</b><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 4, 11, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(2), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void whitespaceOnlyDocument() {
		String oldText = "   \n  ";
		String newText = "   \n  <a/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 6, 0, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void sequentialIncrementalEdits() {
		String text1 = "<a/><b/><c/>";
		String text2 = "<a/><bx/><c/>";
		String text3 = "<a/><bxy/><c/>";

		GreenDocument doc1 = GreenTreeBuilder.parse(text1, "test.xml", null);
		GreenDocument doc2 = IncrementalParser.incrementalParse(
				doc1, text2, 4, 4, 5, "test.xml", null);
		GreenDocument doc3 = IncrementalParser.incrementalParse(
				doc2, text3, 4, 5, 6, "test.xml", null);

		assertEquals(text3.length(), doc3.width());
		assertSame(doc1.child(0), doc3.child(0));
		assertSame(doc1.child(2), doc3.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(text3, "test.xml", null),
				doc3, "");
	}

	@Test
	public void editConvertOpenToSelfClosing() {
		String oldText = "<a/><b>text</b><c/>";
		String newText = "<a/><b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 11, 4, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(2), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void deleteAllContent() {
		String oldText = "<a/><b/><c/>";
		String newText = "";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, 12, 0, "test.xml", null);
		assertEquals(0, newDoc.width());
		assertEquals(0, newDoc.childCount());
	}

	@Test
	public void insertAtExactNodeBoundary() {
		String oldText = "<a/><b/><c/>";
		String newText = "<a/>X<b/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void deleteSpanningThreeNodes() {
		String oldText = "<a/><b/><c/><d/><e/>";
		String newText = "<a/><e/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 12, 0, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(4), newDoc.child(1));
	}

	@Test
	public void veryLargeInsert() {
		String oldText = "<a/><b/>";
		StringBuilder sb = new StringBuilder("<a/>");
		for (int i = 0; i < 50; i++) {
			sb.append("<x").append(i).append("/>");
		}
		sb.append("<b/>");
		String newText = sb.toString();
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 4, 0, newText.length() - 8, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(0), newDoc.child(0));
		assertSame(oldDoc.child(1), newDoc.child(newDoc.childCount() - 1));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void editInDocWithNamespaces() {
		String oldText = "<root xmlns:ns=\"http://example.com\"><ns:child>text</ns:child></root>";
		String newText = "<root xmlns:ns=\"http://example.com\"><ns:child>textX</ns:child></root>";
		assertIncrementalMatchesFullParse(oldText, newText, 50, 0, 1);
	}

	@Test
	public void editPrologContent() {
		String oldText = "<?xml version=\"1.0\"?><a/><b/>";
		String newText = "<?xml version=\"2.0\"?><a/><b/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 15, 3, 3, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertSame(oldDoc.child(1), newDoc.child(1));
		assertSame(oldDoc.child(2), newDoc.child(2));
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentIntoRootElement() {
		String oldText = "<root><a/><b/><c/></root>";
		String newText = "<root><a/><bx/><c/></root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenElement oldRoot = (GreenElement) oldDoc.child(0);
		GreenNode oldA = oldRoot.child(0);
		GreenNode oldC = oldRoot.child(2);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 10, 4, 5, "test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		GreenElement newRoot = (GreenElement) newDoc.child(0);
		assertSame(oldA, newRoot.child(0), "Child <a/> before edit should be reused");
		assertSame(oldC, newRoot.child(2), "Child <c/> after edit should be reused");
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentDeepNested() {
		String oldText = "<root><outer><a/><b/><c/></outer></root>";
		String newText = "<root><outer><a/><bx/><c/></outer></root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenElement oldRoot = (GreenElement) oldDoc.child(0);
		GreenElement oldOuter = (GreenElement) oldRoot.child(0);
		GreenNode oldA = oldOuter.child(0);
		GreenNode oldC = oldOuter.child(2);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 17, 4, 5, "test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		GreenElement newRoot = (GreenElement) newDoc.child(0);
		GreenElement newOuter = (GreenElement) newRoot.child(0);
		assertSame(oldA, newOuter.child(0), "Nested <a/> should be reused");
		assertSame(oldC, newOuter.child(2), "Nested <c/> should be reused");
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentWithManyChildren() {
		StringBuilder sb = new StringBuilder("<root>");
		for (int i = 0; i < 100; i++) {
			sb.append("<item").append(i).append("/>");
		}
		sb.append("</root>");
		String oldText = sb.toString();
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenElement oldRoot = (GreenElement) oldDoc.child(0);

		int editOffset = oldRoot.childrenStartRel();
		for (int i = 0; i < 50; i++) {
			editOffset += oldRoot.child(i).width();
		}
		int oldChildWidth = oldRoot.child(50).width();
		String replacement = "<modified/>";

		String newText = oldText.substring(0, editOffset) + replacement
				+ oldText.substring(editOffset + oldChildWidth);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, editOffset, oldChildWidth, replacement.length(),
				"test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		GreenElement newRoot = (GreenElement) newDoc.child(0);
		for (int i = 0; i < 50; i++) {
			assertSame(oldRoot.child(i), newRoot.child(i),
					"Prefix child " + i + " inside root should be reused");
		}
		for (int i = 51; i < 100; i++) {
			assertSame(oldRoot.child(i), newRoot.child(i),
					"Suffix child " + i + " inside root should be reused");
		}
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentFallsBackOnAttributeEdit() {
		String oldText = "<root><a id=\"old\"/><b/></root>";
		String newText = "<root><a id=\"new\"/><b/></root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 13, 3, 3, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentTextCoalescing() {
		String oldText = "<root>hello</root>";
		String newText = "<root>helloX</root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 11, 0, 1, "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void descentWithPrologAndDoctype() {
		String oldText = "<?xml version=\"1.0\"?><!DOCTYPE root><root><a/><b/><c/></root>";
		String newText = "<?xml version=\"1.0\"?><!DOCTYPE root><root><a/><bx/><c/></root>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenNode oldProlog = oldDoc.child(0);
		GreenNode oldDoctype = oldDoc.child(1);

		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 46, 4, 5, "test.xml", null);

		assertEquals(newText.length(), newDoc.width());
		assertSame(oldProlog, newDoc.child(0), "Prolog should be reused");
		assertSame(oldDoctype, newDoc.child(1), "Doctype should be reused");

		GreenElement oldRoot = (GreenElement) oldDoc.child(2);
		GreenElement newRoot = (GreenElement) newDoc.child(2);
		assertSame(oldRoot.child(0), newRoot.child(0), "<a/> inside root should be reused");
		assertSame(oldRoot.child(2), newRoot.child(2), "<c/> inside root should be reused");
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void middleEndLessThanMiddleStartFallsBack() {
		// Large deletion: remove middle 2 nodes from 3, leaving prefix+suffix
		// that overlap in the new shorter text
		String oldText = "<a>xxxxxxxxxxxxxxxxxxxx</a><b/><c>xxxxxxxxxxxxxxxxxxxx</c>";
		// Delete everything between <a>...</a> and <c>...</c> AND shrink content
		String newText = "<a/><c/>";
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, 0, oldText.length(), newText.length(), "test.xml", null);
		assertEquals(newText.length(), newDoc.width());
		assertGreenTreesEqual(
				GreenTreeBuilder.parse(newText, "test.xml", null),
				newDoc, "");
	}

	@Test
	public void parseIncrementalIntegration() {
		String oldText = "<a/><b>text</b><c/>";
		String newText = "<a/><b>textX</b><c/>";

		TextDocument textDoc = new TextDocument(newText, "test://test.xml");
		GreenDocument oldGreen = GreenTreeBuilder.parse(oldText, "test://test.xml", null);

		DOMDocument incremental = DOMParser.getInstance().parseIncremental(
				textDoc, oldGreen, 11, 0, 1, null, true, null);
		DOMDocument full = DOMParser.getInstance().parse(
				new TextDocument(newText, "test://test.xml"), null, true, null);

		assertNotNull(incremental);
		assertNotNull(incremental.getGreenDocument());
		assertEquals(full.getChildren().size(), incremental.getChildren().size());
		assertEquals(newText.length(), incremental.getEnd());
	}

	private void assertIncrementalParse(String oldText, String newText,
			int editStart, int deleteLength, int insertLength) {
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);
		GreenDocument newDoc = IncrementalParser.incrementalParse(
				oldDoc, newText, editStart, deleteLength, insertLength, "test.xml", null);

		assertNotNull(newDoc, "Incremental parse should produce a result");
		assertEquals(newText.length(), newDoc.width(),
				"New document width should match new text length");
	}

	private void assertIncrementalMatchesFullParse(String oldText, String newText,
			int editStart, int deleteLength, int insertLength) {
		GreenDocument oldDoc = GreenTreeBuilder.parse(oldText, "test.xml", null);

		GreenDocument incremental = IncrementalParser.incrementalParse(
				oldDoc, newText, editStart, deleteLength, insertLength, "test.xml", null);

		GreenDocument fullParse = GreenTreeBuilder.parse(newText, "test.xml", null);

		assertGreenTreesEqual(fullParse, incremental, "");
	}

	private void assertGreenTreesEqual(GreenNode expected, GreenNode actual, String path) {
		assertEquals(expected.nodeType(), actual.nodeType(),
				path + ": nodeType mismatch");
		assertEquals(expected.width(), actual.width(),
				path + ": width mismatch");
		assertEquals(expected.closed(), actual.closed(),
				path + ": closed mismatch");

		if (expected instanceof GreenElement && actual instanceof GreenElement) {
			assertEquals(((GreenElement) expected).tag(), ((GreenElement) actual).tag(),
					path + ": tag mismatch");
		}

		GreenNode[] expectedChildren = expected.children();
		GreenNode[] actualChildren = actual.children();
		assertEquals(expectedChildren.length, actualChildren.length,
				path + ": child count mismatch");

		for (int i = 0; i < expectedChildren.length; i++) {
			assertGreenTreesEqual(expectedChildren[i], actualChildren[i],
					path + "/child[" + i + "]");
		}
	}
}
