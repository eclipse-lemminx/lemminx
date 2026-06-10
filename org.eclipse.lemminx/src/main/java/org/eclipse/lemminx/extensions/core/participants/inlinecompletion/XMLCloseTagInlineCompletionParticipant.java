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
package org.eclipse.lemminx.extensions.core.participants.inlinecompletion;

import static org.eclipse.lemminx.dom.parser.Constants._FSL;
import static org.eclipse.lemminx.dom.parser.Constants._LAN;
import static org.eclipse.lemminx.dom.parser.Constants._RAN;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionRequest;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionResponse;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Inline completion participant that suggests closing tags for open XML
 * elements.
 *
 * This participant provides inline completion suggestions when the user is
 * typing inside an XML element that needs to be closed.
 */
public class XMLCloseTagInlineCompletionParticipant implements IInlineCompletionParticipant {

	@Override
	public void onInlineCompletion(IInlineCompletionRequest request, IInlineCompletionResponse response,
			CancelChecker cancelChecker) {

		DOMDocument document = request.getXMLDocument();
		int offset = request.getOffset();

		// Find the node at the current position
		DOMNode node = document.findNodeAt(offset);
		if (node == null) {
			return;
		}

		// Check if the cursor is at a position where we should suggest a closing tag
		String text = document.getText();
		if (offset > 0 && offset <= text.length()) {
			int charBefore = text.codePointAt(offset - 1);

			// Suggest closing tag after '>' or after content
			if (charBefore == _RAN || Character.isLetterOrDigit(charBefore) || Character.isWhitespace(charBefore)) {
				// Check if we're inside an element that needs closing
				DOMElement parentElement = XMLPositionUtility.findUnclosedParentElement(node, offset);
				if (parentElement != null) {
					String tagName = parentElement.getTagName();
					if (tagName != null && !tagName.isEmpty()) {
						String closingTag = Character.toString(_LAN) + Character.toString(_FSL) + tagName
								+ Character.toString(_RAN);

						InlineCompletionItem item = new InlineCompletionItem();
						item.setInsertText(closingTag);
						response.addInlineCompletionItem(item);
					}
				}
			}
		}
	}
}