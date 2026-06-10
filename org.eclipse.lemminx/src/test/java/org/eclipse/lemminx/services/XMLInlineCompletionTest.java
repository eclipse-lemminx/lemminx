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

import static org.eclipse.lemminx.XMLAssert.testInlineCompletionFor;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionRequest;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionResponse;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XML inline completion service tests with custom participants.
 */
public class XMLInlineCompletionTest {

	private XMLLanguageService languageService;

	@BeforeEach
	public void initializeLanguageService() {
		languageService = new XMLLanguageService();
	}

	@Test
	public void testInlineCompletionWithNoParticipants() throws BadLocationException {
		String xml = "<root>|</root>";
		testInlineCompletionFor(languageService, xml, null, 0);
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
		testInlineCompletionFor(languageService, xml, "test");
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
		testInlineCompletionFor(languageService, xml, "suggestion1", "suggestion2");
	}

	@Test
	public void testInlineCompletionContext() throws BadLocationException {
		// Register a participant that checks the context
		languageService.registerInlineCompletionParticipant(
			new IInlineCompletionParticipant() {
				@Override
				public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
						CancelChecker cancelChecker) {
					InlineCompletionItem item = new InlineCompletionItem();
					item.setInsertText("context-aware");
					response.addInlineCompletionItem(item);
				}
			}
		);

		String xml = "<root>|</root>";
		testInlineCompletionFor(languageService, xml, "context-aware");
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
		testInlineCompletionFor(languageService, xml1, "offset:6");

		// Test at end of content
		String xml2 = "<root>content|</root>";
		testInlineCompletionFor(languageService, xml2, "offset:13");
	}

	@Test
	public void testInlineCompletionWithEmptyDocument() throws BadLocationException {
		String xml = "|";
		testInlineCompletionFor(languageService, xml, null, 0);
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
		testInlineCompletionFor(languageService, xml, "nested");
	}

}