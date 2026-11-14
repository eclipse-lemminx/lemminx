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

import java.util.List;

import org.eclipse.lemminx.commons.SnippetsBuilder;
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
		String lineSeparator = request.getLineSeparator();
		if (snippetsSupported) {
			StringBuilder sb = new StringBuilder(RNCKeywords.ELEMENT + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			sb.append(" {").append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ELEMENT, sb.toString(), "Element pattern",
					CompletionItemKind.Keyword, replaceRange));

			sb.setLength(0);
			sb.append(RNCKeywords.ATTRIBUTE + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			sb.append(" { ");
			SnippetsBuilder.placeholders(2, "text", sb);
			sb.append(" }");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ATTRIBUTE, sb.toString(), "Attribute pattern",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.ELEMENT, replaceRange));
			items.add(RNCCompletions.createKeyword(RNCKeywords.ATTRIBUTE, replaceRange));
		}
		items.add(RNCCompletions.createKeyword(RNCKeywords.TEXT, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.EMPTY, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.NOT_ALLOWED, replaceRange));
		if (snippetsSupported) {
			StringBuilder lsb = new StringBuilder(RNCKeywords.LIST + " {");
			lsb.append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, lsb);
			lsb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.LIST, lsb.toString(), "List pattern",
					CompletionItemKind.Keyword, replaceRange));

			StringBuilder msb = new StringBuilder(RNCKeywords.MIXED + " {");
			msb.append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, msb);
			msb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.MIXED, msb.toString(), "Mixed content pattern",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.LIST, replaceRange));
			items.add(RNCCompletions.createKeyword(RNCKeywords.MIXED, replaceRange));
		}
		if (snippetsSupported) {
			StringBuilder esb = new StringBuilder(RNCKeywords.EXTERNAL + " \"");
			SnippetsBuilder.tabstops(1, esb);
			esb.append("\"$0");
			items.add(RNCCompletions.createSnippet(RNCKeywords.EXTERNAL, esb.toString(), "External grammar reference",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.EXTERNAL, replaceRange));
		}
		items.add(RNCCompletions.createKeyword(RNCKeywords.GRAMMAR, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.PARENT, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.STRING, replaceRange));
		items.add(RNCCompletions.createKeyword(RNCKeywords.TOKEN, replaceRange));
	}

	static void collectStartPatternCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		boolean snippetsSupported = request.isSnippetsSupported();
		Range replaceRange = request.getReplaceRange();
		String lineSeparator = request.getLineSeparator();
		if (snippetsSupported) {
			StringBuilder sb = new StringBuilder(RNCKeywords.ELEMENT + " ");
			SnippetsBuilder.placeholders(1, "name", sb);
			sb.append(" {").append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ELEMENT, sb.toString(), "Element pattern",
					CompletionItemKind.Keyword, replaceRange));

			sb.setLength(0);
			sb.append(RNCKeywords.EXTERNAL + " \"");
			SnippetsBuilder.tabstops(1, sb);
			sb.append("\"$0");
			items.add(RNCCompletions.createSnippet(RNCKeywords.EXTERNAL, sb.toString(), "External grammar reference",
					CompletionItemKind.Keyword, replaceRange));

			sb.setLength(0);
			sb.append(RNCKeywords.GRAMMAR + " {").append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.GRAMMAR, sb.toString(), "Grammar block",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.ELEMENT, replaceRange));
			items.add(RNCCompletions.createKeyword(RNCKeywords.EXTERNAL, replaceRange));
			items.add(RNCCompletions.createKeyword(RNCKeywords.GRAMMAR, replaceRange));
		}
	}
}
