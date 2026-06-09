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
package org.eclipse.lemminx.extensions.contentmodel.participants.inlinecompletion;

import java.util.List;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionRequest;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Inline completion participant that suggests closing tags for open XML elements.
 * 
 * This participant provides inline completion suggestions when the user is typing
 * inside an XML element that needs to be closed.
 */
public class XMLCloseTagInlineCompletionParticipant implements IInlineCompletionParticipant {

	@Override
	public void onInlineCompletion(IInlineCompletionRequest request, List<InlineCompletionItem> list,
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
			char charBefore = text.charAt(offset - 1);
			
			// Suggest closing tag after '>' or after content
			if (charBefore == '>' || Character.isLetterOrDigit(charBefore) || Character.isWhitespace(charBefore)) {
				// Check if we're inside an element that needs closing
				DOMElement parentElement = this.findUnclosedParentElement(node, offset);
				if (parentElement != null) {
					String tagName = parentElement.getTagName();
					if (tagName != null && !tagName.isEmpty()) {
						String closingTag = "</" + tagName + ">";
						
						InlineCompletionItem item = new InlineCompletionItem();
						item.setInsertText(closingTag);
						list.add(item);
					}
				}
			}
		}
	}
	
	/**
	 * Finds the nearest parent element that is not closed.
	 *
	 * @param node the starting node
	 * @param offset the current cursor offset
	 * @return the unclosed parent element, or null if none found
	 */
	private DOMElement findUnclosedParentElement(DOMNode node, int offset) {
		DOMNode current = node;
		
		while (current != null) {
			if (current.isElement()) {
				DOMElement element = (DOMElement) current;
				// Check if the element is not self-closing
				if (!element.isSelfClosed()) {
					// Check if we're after the start tag but before any end tag
					int startTagEnd = element.getStartTagCloseOffset();
					int endTagStart = element.hasEndTag() ? element.getEndTagOpenOffset() : -1;
					
					// If we're after the start tag close and either there's no end tag or we're before it
					if (startTagEnd != -1 && offset > startTagEnd &&
						(endTagStart == -1 || offset < endTagStart)) {
						return element;
					}
				}
			}
			current = current.getParentNode();
		}
		
		return null;
	}
}