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
package org.eclipse.lemminx.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionRequest;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionResponse;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.InlineCompletionContext;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.InlineCompletionList;
import org.eclipse.lsp4j.InlineCompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XML inline completion service tests
 */
public class XMLInlineCompletionTest {

	private XMLLanguageService languageService;
	private static final CancelChecker NULL_CHECKER = () -> {
	};

	@BeforeEach
	public void initializeLanguageService() {
		languageService = new XMLLanguageService();
	}

	@Test
	public void testInlineCompletionWithNoParticipants() throws BadLocationException {
		String xml = "<root>|</root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertEquals(0, result.getItems().size());
	}

	@Test
	public void testInlineCompletionWithCustomParticipant() throws BadLocationException {
		// Register a custom participant that always suggests "test"
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("test");
					response.addInlineCompletionItem(item);
				}
			}
		);

		String xml = "<root>|</root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertEquals(1, result.getItems().size());
		assertEquals("test", getInsertTextAsString(result.getItems().get(0).getInsertText()));
	}

	@Test
	public void testInlineCompletionWithMultipleParticipants() throws BadLocationException {
		// Register multiple participants
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("suggestion1");
					response.addInlineCompletionItem(item);
				}
			}
		);

		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("suggestion2");
					response.addInlineCompletionItem(item);
				}
			}
		);

		String xml = "<root>|</root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		assertEquals(2, result.getItems().size());
		assertEquals("suggestion1", getInsertTextAsString(result.getItems().get(0).getInsertText()));
		assertEquals("suggestion2", getInsertTextAsString(result.getItems().get(1).getInsertText()));
	}

	@Test
	public void testInlineCompletionContext() throws BadLocationException {
		// Register a participant that checks the context
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionContext context = request.getContext();
					assertNotNull(context);
					assertNotNull(context.getTriggerKind());
					
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("context-aware");
					response.addInlineCompletionItem(item);
				}
			}
		);

		String xml = "<root>|</root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertEquals(1, result.getItems().size());
		assertEquals("context-aware", getInsertTextAsString(result.getItems().get(0).getInsertText()));
	}

	@Test
	public void testInlineCompletionAtDifferentPositions() throws BadLocationException {
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					int offset = request.getOffset();
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("offset:" + offset);
					response.addInlineCompletionItem(item);
				}
			}
		);

		// Test at start of content
		String xml1 = "<root>|</root>";
		InlineCompletionList result1 = testInlineCompletionFor(xml1);
		assertEquals(1, result1.getItems().size());
		String insertText1 = getInsertTextAsString(result1.getItems().get(0).getInsertText());
		assertTrue(insertText1.startsWith("offset:"));

		// Test at end of content
		String xml2 = "<root>content|</root>";
		InlineCompletionList result2 = testInlineCompletionFor(xml2);
		assertEquals(1, result2.getItems().size());
		String insertText2 = getInsertTextAsString(result2.getItems().get(0).getInsertText());
		assertTrue(insertText2.startsWith("offset:"));
	}

	@Test
	public void testInlineCompletionWithEmptyDocument() throws BadLocationException {
		String xml = "|";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertNotNull(result.getItems());
		// Should return empty list with no participants
		assertEquals(0, result.getItems().size());
	}

	@Test
	public void testInlineCompletionWithNestedElements() throws BadLocationException {
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("nested");
					response.addInlineCompletionItem(item);
				}
			}
		);

		String xml = "<root><child>|</child></root>";
		InlineCompletionList result = testInlineCompletionFor(xml);
		assertNotNull(result);
		assertEquals(1, result.getItems().size());
		assertEquals("nested", getInsertTextAsString(result.getItems().get(0).getInsertText()));
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