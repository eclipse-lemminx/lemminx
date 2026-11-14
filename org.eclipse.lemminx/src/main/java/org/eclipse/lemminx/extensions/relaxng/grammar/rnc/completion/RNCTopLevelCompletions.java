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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.commons.snippets.SnippetRegistry;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypeLibrary;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNamespaceLibrary;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Range;

/**
 * Provides completion items for top-level RNC declarations ({@code namespace},
 * {@code default}, {@code datatypes}, {@code start}, {@code include},
 * {@code div}) and namespace URI completions.
 */
class RNCTopLevelCompletions {

	private final SnippetRegistry snippetRegistry;

	RNCTopLevelCompletions() {
		snippetRegistry = new SnippetRegistry("rnc", true);
	}

	void collectTopLevelCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		RNCDocument rncDocument = request.getRNCDocument();
		boolean snippetsSupported = request.isSnippetsSupported();
		boolean triggerSuggestSupported = request.isTriggerSuggestSupported();
		Range replaceRange = request.getReplaceRange();
		String lineSeparator = request.getLineSeparator();
		boolean hasStart = rncDocument.getStartDecl() != null;
		List<String> defineNames = getDefineNames(rncDocument);

		if (rncDocument.hasNoContent()) {
			collectNewFileSnippets(items, snippetsSupported, replaceRange, lineSeparator);
		}

		// namespace ${1:prefix} = "${2:uri}"
		StringBuilder sb = new StringBuilder(RNCKeywords.NAMESPACE + " ");
		if (snippetsSupported) {
			SnippetsBuilder.placeholders(1, "prefix", sb);
			sb.append(" = \"");
			SnippetsBuilder.placeholders(2, "uri", sb);
			sb.append("\"");
			items.add(RNCCompletions.createSnippet(RNCKeywords.NAMESPACE, sb.toString(), "Namespace declaration",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.NAMESPACE, replaceRange));
		}

		// default namespace
		if (!rncDocument.hasDefaultNamespace()) {
			sb.setLength(0);
			sb.append(RNCKeywords.DEFAULT + " " + RNCKeywords.NAMESPACE + " = \"");
			if (snippetsSupported) {
				SnippetsBuilder.placeholders(1, "uri", sb);
				sb.append("\"");
				items.add(RNCCompletions.createSnippet(RNCKeywords.DEFAULT + " " + RNCKeywords.NAMESPACE, sb.toString(),
						"Default namespace declaration", CompletionItemKind.Keyword, replaceRange));
			} else {
				items.add(RNCCompletions.createKeyword(RNCKeywords.DEFAULT, replaceRange));
			}
		}

		// datatypes ${1:prefix} = "${2:uri}"
		sb.setLength(0);
		sb.append(RNCKeywords.DATATYPES + " ");
		if (snippetsSupported) {
			SnippetsBuilder.placeholders(1, "prefix", sb);
			sb.append(" = \"");
			SnippetsBuilder.placeholders(2, "uri", sb);
			sb.append("\"");
			items.add(RNCCompletions.createSnippet(RNCKeywords.DATATYPES, sb.toString(), "Datatypes declaration",
					CompletionItemKind.Keyword, replaceRange));

			collectDatatypeLibrarySnippets(items, replaceRange);
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.DATATYPES, replaceRange));
		}

		// start = ... or start |= ...
		sb.setLength(0);
		if (hasStart) {
			sb.append(RNCKeywords.START + " |= ");
		} else {
			sb.append(RNCKeywords.START + " = ");
		}
		if (snippetsSupported) {
			if (triggerSuggestSupported) {
				SnippetsBuilder.tabstops(0, sb);
			} else {
				List<String> choices = new ArrayList<>();
				choices.add(RNCKeywords.ELEMENT);
				choices.addAll(defineNames);
				choices.add(RNCKeywords.EXTERNAL);
				choices.add(RNCKeywords.GRAMMAR);
				SnippetsBuilder.choice(1, choices, sb);
			}
			items.add(RNCCompletions.createSnippet(hasStart ? RNCKeywords.START + " |=" : RNCKeywords.START,
					sb.toString(), "Start pattern", CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(hasStart ? RNCKeywords.START + " |=" : RNCKeywords.START, replaceRange));
		}

		// include "$1"$0
		sb.setLength(0);
		sb.append(RNCKeywords.INCLUDE + " \"");
		if (snippetsSupported) {
			SnippetsBuilder.tabstops(1, sb);
			sb.append("\"$0");
			items.add(RNCCompletions.createSnippet(RNCKeywords.INCLUDE, sb.toString(), "Include declaration",
					CompletionItemKind.Keyword, replaceRange));

			// include "$0" { }
			sb.setLength(0);
			sb.append(RNCKeywords.INCLUDE + " \"");
			SnippetsBuilder.tabstops(0, sb);
			sb.append("\" {").append(lineSeparator).append("  ").append(lineSeparator).append("}");
			CompletionItem includeBodyItem = RNCCompletions.createSnippet(RNCKeywords.INCLUDE + " { }", sb.toString(),
					"Include with overrides", CompletionItemKind.Keyword, replaceRange);
			includeBodyItem.setFilterText(RNCKeywords.INCLUDE);
			items.add(includeBodyItem);
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.INCLUDE, replaceRange));
		}

		// div { ... }
		sb.setLength(0);
		sb.append(RNCKeywords.DIV + " {").append(lineSeparator).append("  ");
		if (snippetsSupported) {
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.DIV, sb.toString(), "Division block",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.DIV, replaceRange));
		}

		// Existing defines: propose "defineName |=" and "defineName &=" to extend
		for (String name : defineNames) {
			sb.setLength(0);
			sb.append(name);
			sb.append(" |= ");
			if (snippetsSupported) {
				SnippetsBuilder.placeholders(1, "pattern", sb);
				items.add(RNCCompletions.createSnippet(name + " |=", sb.toString(), "Extend with choice alternative",
						CompletionItemKind.Variable, replaceRange));
			} else {
				items.add(RNCCompletions.createItem(name + " |=", CompletionItemKind.Variable, "Extend with choice alternative",
						replaceRange));
			}

			sb.setLength(0);
			sb.append(name);
			sb.append(" &= ");
			if (snippetsSupported) {
				SnippetsBuilder.placeholders(1, "pattern", sb);
				items.add(RNCCompletions.createSnippet(name + " &=", sb.toString(), "Extend with interleave",
						CompletionItemKind.Variable, replaceRange));
			} else {
				items.add(RNCCompletions.createItem(name + " &=", CompletionItemKind.Variable, "Extend with interleave",
						replaceRange));
			}
		}

		// element (as define form at top level)
		if (snippetsSupported) {
			sb.setLength(0);
			boolean isGrammarForm = hasStart || !defineNames.isEmpty()
					|| !rncDocument.getNamespaceDecls().isEmpty()
					|| !rncDocument.getDatatypesDecls().isEmpty()
					|| !rncDocument.getIncludes().isEmpty();
			if (isGrammarForm) {
				SnippetsBuilder.placeholders(1, "name", sb);
				sb.append(" = " + RNCKeywords.ELEMENT + " ");
				SnippetsBuilder.placeholders(1, "name", sb);
			} else {
				sb.append(RNCKeywords.ELEMENT + " ");
				SnippetsBuilder.placeholders(1, "name", sb);
			}
			sb.append(" {").append(lineSeparator).append("  ");
			SnippetsBuilder.tabstops(0, sb);
			sb.append(lineSeparator).append("}");
			items.add(RNCCompletions.createSnippet(RNCKeywords.ELEMENT, sb.toString(), "Element pattern",
					CompletionItemKind.Keyword, replaceRange));
		} else {
			items.add(RNCCompletions.createKeyword(RNCKeywords.ELEMENT, replaceRange));
		}
	}

	static void collectNamespaceUriCompletions(List<CompletionItem> items, Range replaceRange) {
		for (RNCNamespaceLibrary.Entry ns : RNCNamespaceLibrary.getNamespaces()) {
			items.add(RNCCompletions.createLiteral(ns.getUri(), ns.getDescription(), replaceRange));
		}
	}

	private void collectNewFileSnippets(List<CompletionItem> items, boolean snippetsSupported,
			Range replaceRange, String lineSeparator) {
		items.addAll(snippetRegistry.getCompletionItems(replaceRange, lineSeparator, true,
				snippetsSupported, null, null));
	}

	private static void collectDatatypeLibrarySnippets(List<CompletionItem> items, Range replaceRange) {
		for (RNCDatatypeLibrary library : RNCDatatypeLibrary.getLibraries()) {
			String defaultPrefix = library.getDefaultPrefix();
			if (defaultPrefix == null) {
				continue;
			}
			String uri = library.getUri();
			StringBuilder sb = new StringBuilder(RNCKeywords.DATATYPES + " ");
			SnippetsBuilder.placeholders(1, defaultPrefix, sb);
			sb.append(" = \"");
			sb.append(uri);
			sb.append("\"$0");
			String label = RNCKeywords.DATATYPES + " " + defaultPrefix;
			CompletionItem item = RNCCompletions.createSnippet(label, sb.toString(),
					library.getDescription(), CompletionItemKind.Keyword, replaceRange);
			item.setFilterText(RNCKeywords.DATATYPES);
			items.add(item);
		}
	}

	private static List<String> getDefineNames(RNCDocument rncDocument) {
		List<String> names = new ArrayList<>();
		for (RNCDefine define : rncDocument.getDefines()) {
			String name = define.getName();
			if (name != null) {
				names.add(name);
			}
		}
		return names;
	}
}
