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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.client.ClientCommands;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion.RNCCompletionContext.CompletionContextType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Orchestrates RNC completion by delegating to specialized completion classes
 * based on cursor context.
 *
 * <p>Each completion type is handled by its own class:</p>
 * <ul>
 *   <li>{@link RNCTopLevelCompletions} — declaration keywords</li>
 *   <li>{@link RNCPatternCompletions} — pattern keywords</li>
 *   <li>{@link RNCDefineRefCompletions} — define references</li>
 *   <li>{@link RNCDatatypeCompletions} — datatype prefixes and URIs</li>
 *   <li>{@link RNCFilePathCompletions} — file paths for include/external</li>
 *   <li>{@link RNCIncludeOverrideCompletions} — include overrides</li>
 * </ul>
 */
public class RNCCompletions {

	private static final Logger LOGGER = Logger.getLogger(RNCCompletions.class.getName());

	private final RNCTopLevelCompletions topLevelCompletions;

	private final IRNCDocumentProvider documentProvider;

	public RNCCompletions(IRNCDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
		topLevelCompletions = new RNCTopLevelCompletions();
	}

	public CompletionList doComplete(RNCDocument rncDocument, Position position, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		CompletionList list = new CompletionList();
		list.setIsIncomplete(false);
		List<CompletionItem> items = new ArrayList<>();

		try {
			RNCCompletionRequest request = new RNCCompletionRequest(rncDocument, position, sharedSettings);			
			collectContextCompletions(items, request);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC completions", e);
		}

		configureCompletionItems(items, rncDocument.getUri(), sharedSettings.isTriggerSuggestCommandSupport());

		list.setItems(items);
		return list;
	}

	private void collectContextCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		RNCDocument rncDocument = request.getRNCDocument();
		CompletionContextType context = RNCCompletionContext.getCompletionContext(
				request.getText(), request.getOffset());
		RNCInclude enclosingInclude = RNCIncludeOverrideCompletions.findEnclosingInclude(rncDocument, request.getOffset());

		switch (context) {
			case TOP_LEVEL:
				topLevelCompletions.collectTopLevelCompletions(items, request);
				RNCDefineRefCompletions.collectDefineRefCompletions(items, request, enclosingInclude, documentProvider);
				break;
			case AFTER_ASSIGN:
				RNCDatatypeCompletions.collectDatatypeCompletions(items, request);
				RNCPatternCompletions.collectPatternCompletions(items, request);
				RNCDefineRefCompletions.collectDefineRefCompletions(items, request, enclosingInclude, documentProvider);
				break;
			case AFTER_START_ASSIGN:
				RNCPatternCompletions.collectStartPatternCompletions(items, request);
				RNCDefineRefCompletions.collectDefineRefCompletions(items, request, enclosingInclude, documentProvider);
				break;
			case IN_PATTERN:
				RNCDatatypeCompletions.collectDatatypeCompletions(items, request);
				RNCPatternCompletions.collectPatternCompletions(items, request);
				RNCDefineRefCompletions.collectDefineRefCompletions(items, request, enclosingInclude, documentProvider);
				break;
			case AFTER_NAMESPACE_ASSIGN:
				RNCTopLevelCompletions.collectNamespaceUriCompletions(items, request.getReplaceRange());
				break;
			case AFTER_DATATYPES_ASSIGN:
				RNCDatatypeCompletions.collectDatatypeLibraryUriCompletions(items, request.getReplaceRange());
				break;
			case AFTER_INCLUDE:
			case IN_INCLUDE_LITERAL:
			case AFTER_EXTERNAL:
			case IN_EXTERNAL_LITERAL:
				RNCFilePathCompletions.collectFilePathCompletions(items, request);
				break;
			case AFTER_ELEMENT_KEYWORD:
			case AFTER_ATTRIBUTE_KEYWORD:
			case IN_COMMENT:
				break;
			default:
				RNCPatternCompletions.collectPatternCompletions(items, request);
				RNCDefineRefCompletions.collectDefineRefCompletions(items, request, enclosingInclude, documentProvider);
				break;
		}
		if (enclosingInclude != null) {
			RNCIncludeOverrideCompletions.collectIncludeOverrideCompletions(items, enclosingInclude,
					rncDocument, request, documentProvider);
		}
	}

	private static void configureCompletionItems(List<CompletionItem> items, String docUri,
			boolean triggerSuggestSupported) {
		Command triggerSuggestCommand = triggerSuggestSupported
				? new Command("", ClientCommands.TRIGGER_SUGGEST)
				: null;
		for (CompletionItem item : items) {
			if (triggerSuggestCommand != null) {
				String label = item.getLabel();
				if (RNCKeywords.INCLUDE.equals(label) || (RNCKeywords.INCLUDE + " { }").equals(label)
						|| RNCKeywords.EXTERNAL.equals(label)
						|| RNCKeywords.START.equals(label) || (RNCKeywords.START + " |=").equals(label)) {
					item.setCommand(triggerSuggestCommand);
				}
			}
			if (item.getData() != null) {
				continue;
			}
			CompletionItemKind kind = item.getKind();
			if (kind == CompletionItemKind.Keyword) {
				item.setData(RNCCompletionResolver.createResolveData(docUri, RNCCompletionResolver.RESOLVE_KEYWORD));
			} else if (kind == CompletionItemKind.Reference) {
				item.setData(RNCCompletionResolver.createResolveData(docUri, RNCCompletionResolver.RESOLVE_DEFINE_REF));
			}
		}
	}

	// -------------------- Shared Utility Methods --------------------

	static CompletionItem createKeyword(String keyword, Range replaceRange) {
		CompletionItem item = new CompletionItem(keyword);
		item.setKind(CompletionItemKind.Keyword);
		item.setFilterText(keyword);
		item.setInsertTextFormat(InsertTextFormat.PlainText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, keyword)));
		return item;
	}

	static CompletionItem createSnippet(String label, String snippet, String detail,
			CompletionItemKind kind, Range replaceRange) {
		CompletionItem item = new CompletionItem(label);
		item.setKind(kind);
		item.setFilterText(label);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, snippet)));
		item.setInsertTextFormat(InsertTextFormat.Snippet);
		item.setDetail(detail);
		return item;
	}

	static CompletionItem createItem(String label, CompletionItemKind kind, String detail,
			Range replaceRange) {
		CompletionItem item = new CompletionItem(label);
		item.setKind(kind);
		item.setFilterText(label);
		item.setDetail(detail);
		item.setInsertTextFormat(InsertTextFormat.PlainText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, label)));
		return item;
	}

	static CompletionItem createLiteral(String uri, String detail, Range replaceRange) {
		String quotedUri = "\"" + uri + "\"";
		CompletionItem item = new CompletionItem(quotedUri);
		item.setKind(CompletionItemKind.Value);
		item.setFilterText(quotedUri);
		item.setDetail(detail);
		item.setInsertTextFormat(InsertTextFormat.PlainText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, quotedUri)));
		return item;
	}
}
