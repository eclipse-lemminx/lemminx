/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.contentmodel.participants.inlinecompletion.XMLCloseTagInlineCompletionParticipant;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.InlineCompletionContext;
import org.eclipse.lsp4j.InlineCompletionList;
import org.eclipse.lsp4j.InlineCompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for XMLCloseTagInlineCompletionParticipant
 */
public class XMLCloseTagInlineCompletionTest {

	private XMLLanguageService languageService;
	private static final CancelChecker NULL_CHECKER = () -> {
	};

	@BeforeEach
	public void initializeLanguageService() {
		languageService = new XMLLanguageService();
		// Register the close tag participant
		languageService.registerInlineCompletionParticipant(new XMLCloseTagInlineCompletionParticipant());
	}

	@Test
	public void testCloseTagSuggestionAfterOpenTag() throws BadLocationException {
		String xml = "<root>|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertTrue(result.getItems().size() > 0, "Should suggest closing tag");
		
		String suggestion = getInsertTextAsString(result.getItems().get(0).getInsertText());
		assertEquals("</root>", suggestion);
	}

	@Test
	public void testCloseTagSuggestionAfterContent() throws BadLocationException {
		String xml = "<root>content|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertTrue(result.getItems().size() > 0, "Should suggest closing tag after content");
		
		String suggestion = getInsertTextAsString(result.getItems().get(0).getInsertText());
		assertEquals("</root>", suggestion);
	}

	@Test
	public void testCloseTagSuggestionForNestedElement() throws BadLocationException {
		String xml = "<root><child>|</root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertTrue(result.getItems().size() > 0, "Should suggest closing tag for nested element");
		
		String suggestion = getInsertTextAsString(result.getItems().get(0).getInsertText());
		assertEquals("</child>", suggestion);
	}

	@Test
	public void testNoSuggestionForClosedElement() throws BadLocationException {
		String xml = "<root></root>|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		// Should not suggest anything after a closed element
		assertEquals(0, result.getItems().size());
	}

	@Test
	public void testNoSuggestionForSelfClosingElement() throws BadLocationException {
		String xml = "<root/>|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		// Should not suggest anything after a self-closing element
		assertEquals(0, result.getItems().size());
	}

	@Test
	public void testCloseTagWithAttributes() throws BadLocationException {
		String xml = "<root attr=\"value\">|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertTrue(result.getItems().size() > 0, "Should suggest closing tag for element with attributes");
		
		String suggestion = getInsertTextAsString(result.getItems().get(0).getInsertText());
		assertEquals("</root>", suggestion);
	}

	@Test
	public void testCloseTagWithWhitespace() throws BadLocationException {
		String xml = "<root>  |";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertTrue(result.getItems().size() > 0, "Should suggest closing tag after whitespace");
		
		String suggestion = getInsertTextAsString(result.getItems().get(0).getInsertText());
		assertEquals("</root>", suggestion);
	}

	/**
	 * Test inline completion for the given XML content.
	 * The '|' character marks the cursor position.
	 * 
	 * @param xml the XML content with cursor position marked by '|'
	 * @return the inline completion list
	 * @throws BadLocationException if the position is invalid
	 */
	private InlineCompletionList testInlineCompletionFor(String xml) throws BadLocationException {
		int offset = xml.indexOf('|');
		if (offset == -1) {
			throw new IllegalArgumentException("XML must contain '|' to mark cursor position");
		}
		
		String xmlWithoutCursor = xml.substring(0, offset) + xml.substring(offset + 1);
		DOMDocument document = DOMParser.getInstance().parse(xmlWithoutCursor, "test.xml", null);
		Position position = document.positionAt(offset);
		
		InlineCompletionContext context = new InlineCompletionContext();
		context.setTriggerKind(InlineCompletionTriggerKind.Invoked);
		
		SharedSettings settings = new SharedSettings();
		
		return languageService.doInlineCompletion(document, position, context, settings, NULL_CHECKER);
	}

	/**
	 * Helper method to extract string from Either<String, StringValue>
	 */
	private String getInsertTextAsString(Either<String, org.eclipse.lsp4j.StringValue> insertText) {
		if (insertText == null) {
			return null;
		}
		if (insertText.isLeft()) {
			return insertText.getLeft();
		} else {
			return insertText.getRight().getValue();
		}
	}
}