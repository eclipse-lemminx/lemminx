/*******************************************************************************
* Copyright (c) 2019, 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * XMLPositionUtilityTest
 */
public class XMLPositionUtilityTest {

	@Test
	public void testGetMatchingEndTagPositionMiddle() {
		String initialText = "<Apple>\n" + "  <Or|ange></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange></Or|ange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagPositionBeginning() {
		String initialText = "<Apple>\n" + "  <|Orange></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange></|Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagPositionEnd() {
		String initialText = "<Apple>\n" + "  <Orange|></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange></Orange|>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagPositionAttributes() {
		String initialText = "<Apple>\n" + "  <Orange| amount=\"1\"></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange amount=\"1\"></Orange|>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagNoResult() {
		String initialText = "<Apple>\n" + "  |<Orange></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagNoResult2() {
		String initialText = "<Apple>\n" + "  <Orange |></Orange>\n" + // Because there is a space
				"</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange ></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagTextBetween() {
		String initialText = "<Apple>\n" + "  <Orange|>Text Between</Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange>Text Between</Orange|>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagElementBetween() {
		String initialText = "<Apple>\n" + "  <Orange|><Lemon></Lemon></Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange><Lemon></Lemon></Orange|>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingStartTagPositionMiddle() {
		String initialText = "<Apple>\n" + "  <Orange></Or|ange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Or|ange></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingStartTagPositionBeginning() {
		String initialText = "<Apple>\n" + "  <Orange></|Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <|Orange></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingStartTagPositionEnd() {
		String initialText = "<Apple>\n" + "  <Orange></Orange|>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange|></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingStartTagPositionAttributes() {
		String initialText = "<Apple>\n" + "  <Orange amount=\"1\"></Orange|>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <Orange| amount=\"1\"></Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagPositionAttributesPrefixed() {
		String initialText = "<Apple>\n" + "  <prefix:Orange| amount=\"1\"></prefix:Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <prefix:Orange amount=\"1\"></prefix:Orange|>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void testGetMatchingEndTagPositionPrefixed() {
		String initialText = "<Apple>\n" + "  <pref|ix:Orange></prefix:Orange>\n" + "</Apple>";

		String expectedText = "<Apple>\n" + "  <prefix:Orange></pref|ix:Orange>\n" + "</Apple>";

		testMatchingTagPosition(initialText, expectedText);
	}

	@Test
	public void entityReference() {
		assertEntityReferenceOffset("|", -1, -1);
		assertEntityReferenceOffset("ab|cd", -1, -1);
		assertEntityReferenceOffset("&|", 0, -1);
		assertEntityReferenceOffset("&a|", 0, -1);
		assertEntityReferenceOffset("\n&|", 1, -1);
		assertEntityReferenceOffset("\n&a|", 1, -1);
		assertEntityReferenceOffset("&ab|cd;&efgh;", 0, 6);
		assertEntityReferenceOffset("& ab|cd;&efgh;", -1, 7);
	}

	private static void assertEntityReferenceOffset(String xml, int start, int end) {
		int offset = xml.indexOf('|');
		xml = xml.substring(0, offset) + xml.substring(offset + 1, xml.length());
		Assertions.assertEquals(start, XMLPositionUtility.getEntityReferenceStartOffset(xml, offset), "test for start offset ");
		Assertions.assertEquals(end, XMLPositionUtility.getEntityReferenceEndOffset(xml, offset), "Test for end offset ");
	}

	private static void testMatchingTagPosition(String initialCursorText, String expectedCursorText) {

		int offset = initialCursorText.indexOf('|');
		initialCursorText = initialCursorText.substring(0, offset) + initialCursorText.substring(offset + 1);
		DOMDocument xmlDocument = DOMParser.getInstance().parse(initialCursorText, "testURI", null);
		Position initialCursorPosition;
		Position newCursorPosition;
		int newCursorOffset = -1;
		try {
			initialCursorPosition = xmlDocument.positionAt(offset);
			newCursorPosition = XMLPositionUtility.getMatchingTagPosition(xmlDocument, initialCursorPosition);
			if (newCursorPosition != null) { // a result for a matching position was found
				newCursorOffset = xmlDocument.offsetAt(newCursorPosition);
			}
		} catch (BadLocationException e) {
			fail(e.getMessage());
			return;
		}

		StringBuilder sBuffer = new StringBuilder(initialCursorText);
		String actualOutputString;
		if (newCursorOffset > -1) {
			actualOutputString = sBuffer.insert(newCursorOffset, "|").toString();
		} else { // no matching position was found
			actualOutputString = initialCursorText;
		}

		assertEquals(expectedCursorText, actualOutputString);
	}

	@Test
	public void testFindUnclosedParentElementSimple() {
		String xml = "<root>|";
		testFindUnclosedParentElement(xml, "root");
	}

	@Test
	public void testFindUnclosedParentElementWithContent() {
		String xml = "<root>content|";
		testFindUnclosedParentElement(xml, "root");
	}

	@Test
	public void testFindUnclosedParentElementNested() {
		String xml = "<root><child>|</root>";
		testFindUnclosedParentElement(xml, "child");
	}

	@Test
	public void testFindUnclosedParentElementClosed() {
		String xml = "<root></root>|";
		testFindUnclosedParentElement(xml, null);
	}

	@Test
	public void testFindUnclosedParentElementSelfClosed() {
		String xml = "<root/>|";
		testFindUnclosedParentElement(xml, null);
	}

	@Test
	public void testFindUnclosedParentElementWithAttributes() {
		String xml = "<root attr=\"value\">|";
		testFindUnclosedParentElement(xml, "root");
	}

	@Test
	public void testFindUnclosedParentElementBeforeStartTag() {
		String xml = "|<root>";
		testFindUnclosedParentElement(xml, null);
	}

	@Test
	public void testFindUnclosedParentElementInStartTag() {
		String xml = "<ro|ot>";
		testFindUnclosedParentElement(xml, null);
	}

	@Test
	public void testFindUnclosedParentElementMultipleNested() {
		String xml = "<root><parent><child>|";
		testFindUnclosedParentElement(xml, "child");
	}

	@Test
	public void testFindUnclosedParentElementAfterClosedChild() {
		String xml = "<root><child></child>|";
		testFindUnclosedParentElement(xml, "root");
	}

	/**
		* Test findUnclosedParentElement for the given XML content.
		* The '|' character marks the cursor position.
		*
		* @param xml the XML content with cursor position marked by '|'
		* @param expectedTagName the expected tag name of the unclosed parent element, or null if none expected
		*/
	private static void testFindUnclosedParentElement(String xml, String expectedTagName) {
		int offset = xml.indexOf('|');
		if (offset == -1) {
			fail("XML must contain '|' to mark cursor position");
		}
		
		String xmlWithoutCursor = xml.substring(0, offset) + xml.substring(offset + 1);
		DOMDocument document = DOMParser.getInstance().parse(xmlWithoutCursor, "test.xml", null);
		
		var node = document.findNodeAt(offset);
		var unclosedElement = XMLPositionUtility.findUnclosedParentElement(node, offset);
		
		if (expectedTagName == null) {
			Assertions.assertNull(unclosedElement, "Expected no unclosed parent element");
		} else {
			Assertions.assertNotNull(unclosedElement, "Expected to find unclosed parent element");
			Assertions.assertEquals(expectedTagName, unclosedElement.getTagName(),
				"Expected unclosed parent element tag name to be '" + expectedTagName + "'");
		}
	}
}