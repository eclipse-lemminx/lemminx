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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNodeType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Range;

/**
 * Provides completion items for RNC pattern keywords ({@code element},
 * {@code attribute}, {@code text}, {@code empty}, etc.).
 */
class RNCPatternCompletions {

	static void collectPatternCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		boolean snippetsSupported = request.isSnippetsSupported();
		Range replaceRange = request.getReplaceRange();
		boolean insideAttribute = isInsideAttribute(request);

		// element, attribute, mixed, external: not allowed inside an attribute
		if (!insideAttribute) {
			addElement(items, request);
			addAttribute(items, request);
		}

		// text, empty, notAllowed, string, token
		items.add(RNCCompletions.createKeyword(RNCKeywords.TEXT, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.EMPTY, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.NOT_ALLOWED, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.STRING, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.TOKEN, replaceRange));

		// list (valid inside an attribute), mixed (not valid inside an attribute)
		addBlock(items, request, RNCKeywords.LIST, "List pattern");
		if (!insideAttribute) {
			addBlock(items, request, RNCKeywords.MIXED, "Mixed content pattern");
			addExternal(items, request);
		}

		// grammar, parent
		addBlock(items, request, RNCKeywords.GRAMMAR, "Grammar block");
		addParent(items, request);
	}

	static void collectStartPatternCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		addElement(items, request);
		addExternal(items, request);
		addBlock(items, request, RNCKeywords.GRAMMAR, "Grammar block");
	}

	// ---------------------------------------------------------------------
	// Context
	// ---------------------------------------------------------------------

	/**
	 * Returns true if the completion offset is inside an {@code attribute { ... }}
	 * pattern. Walks up the parents because {@code findNodeAt} returns the deepest
	 * node (name, pattern, etc.), not necessarily the container. Stops at the first
	 * enclosing element, since an attribute outside of it doesn't apply.
	 */
	private static boolean isInsideAttribute(RNCCompletionRequest request) {
		RNCNode node = request.getRNCDocument().findNodeAt(request.getOffset());
		while (node != null) {
			RNCNodeType type = node.getNodeType();
			if (type == RNCNodeType.Attribute) {
				return true;
			}
			if (type == RNCNodeType.Element) {
				return false;
			}
			node = node.getParent();
		}
		return false;
	}

	// ---------------------------------------------------------------------
	// Snippet helpers
	// ---------------------------------------------------------------------

	/** element name { } */
	private static void addElement(List<CompletionItem> items, RNCCompletionRequest request) {
		Range replaceRange = request.getReplaceRange();
		if (request.isSnippetsSupported()) {
			StringBuilder sb = new StringBuilder(RNCKeywords.ELEMENT + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			sb.append(" {").append(request.getLineSeparator()).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(request.getLineSeparator()).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ELEMENT, sb.toString(), "Element pattern",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.ELEMENT, replaceRange));
		}
	}

	/** attribute name { text | empty | notAllowed | string | token } */
	private static void addAttribute(List<CompletionItem> items, RNCCompletionRequest request) {
		Range replaceRange = request.getReplaceRange();
		if (request.isSnippetsSupported()) {
			StringBuilder sb = new StringBuilder(RNCKeywords.ATTRIBUTE + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			sb.append(" { ");
			SnippetsBuilder.choice(2, Arrays.asList(RNCKeywords.TEXT, RNCKeywords.STRING, RNCKeywords.TOKEN,
					RNCKeywords.EMPTY, RNCKeywords.NOT_ALLOWED), sb);
			sb.append(" }");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ATTRIBUTE, sb.toString(), "Attribute pattern",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.ATTRIBUTE, replaceRange));
		}
	}

	/** external "uri" */
	private static void addExternal(List<CompletionItem> items, RNCCompletionRequest request) {
		Range replaceRange = request.getReplaceRange();
		if (request.isSnippetsSupported()) {
			StringBuilder sb = new StringBuilder(RNCKeywords.EXTERNAL + " \"");
			SnippetsBuilder.tabstops(1, sb);
			sb.append("\"$0");
			items.add(RNCCompletions.createSnippet(RNCKeywords.EXTERNAL, sb.toString(),
					"External grammar reference", CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.EXTERNAL, replaceRange));
		}
	}

	/** parent name */
	private static void addParent(List<CompletionItem> items, RNCCompletionRequest request) {
		Range replaceRange = request.getReplaceRange();
		if (request.isSnippetsSupported()) {
			StringBuilder sb = new StringBuilder(RNCKeywords.PARENT + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			SnippetsBuilder.tabstops(0, sb);
			items.add(RNCCompletions.createSnippet(RNCKeywords.PARENT, sb.toString(),
					"Reference to a definition of the parent grammar", CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.PARENT, replaceRange));
		}
	}

	/** keyword { } (list, mixed, grammar) */
	private static void addBlock(List<CompletionItem> items, RNCCompletionRequest request, String keyword,
			String documentation) {
		Range replaceRange = request.getReplaceRange();
		if (request.isSnippetsSupported()) {
			String lineSeparator = request.getLineSeparator();
			StringBuilder sb = new StringBuilder(keyword + " {");
			sb.append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(keyword, sb.toString(), documentation,
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(keyword, replaceRange));
		}
	}
}