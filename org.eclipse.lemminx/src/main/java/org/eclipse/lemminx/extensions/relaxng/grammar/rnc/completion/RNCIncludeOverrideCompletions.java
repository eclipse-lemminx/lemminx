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
import java.util.Set;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonObject;

/**
 * Provides completion items for overriding definitions from an included RNC
 * file inside an {@code include { ... }} body.
 */
class RNCIncludeOverrideCompletions {

	static RNCInclude findEnclosingInclude(RNCDocument document, int offset) {
		for (RNCInclude include : document.getIncludes()) {
			if (!include.hasBody()) {
				continue;
			}
			int bodyOpen = include.getBodyOpenOffset();
			int bodyClose = include.getBodyCloseOffset();
			if (bodyOpen < offset && (bodyClose == -1 || offset <= bodyClose)) {
				return include;
			}
		}
		return null;
	}

	static void collectIncludeOverrideCompletions(List<CompletionItem> items, RNCInclude include,
			RNCDocument rncDocument, RNCCompletionRequest request, IRNCDocumentProvider documentProvider) {
		String resolvedIncludeUri = RNCIncludeUtils.resolveUri(include, rncDocument.getUri());
		RNCDocument includedDoc = documentProvider.getDocument(resolvedIncludeUri);
		if (includedDoc == null) {
			return;
		}

		Set<String> alreadyOverridden = include.getOverriddenNames();
		Range replaceRange = request.getReplaceRange();
		String docUri = rncDocument.getUri();
		int sortIndex = 0;

		RNCStart startDecl = includedDoc.getStartDecl();
		if (startDecl != null && !alreadyOverridden.contains(RNCKeywords.START)) {
			String originalText = startDecl.getText();
			if (originalText != null) {
				addOverrideItem(items, RNCKeywords.START, originalText.trim(), sortIndex++, replaceRange,
						docUri, resolvedIncludeUri);
			}
		}
		for (RNCDefine def : includedDoc.getDefines()) {
			String name = def.getName();
			if (name == null || alreadyOverridden.contains(name)) {
				continue;
			}
			String originalText = def.getText();
			if (originalText != null) {
				addOverrideItem(items, name, originalText.trim(), sortIndex++, replaceRange,
						docUri, resolvedIncludeUri);
			}
		}
	}

	private static void addOverrideItem(List<CompletionItem> items, String name, String insertText,
			int sortIndex, Range replaceRange, String docUri, String includeFileUri) {
		CompletionItem item = new CompletionItem(name);
		item.setKind(CompletionItemKind.Property);
		item.setDetail("Override from included file");
		item.setFilterText(name);
		item.setInsertTextFormat(InsertTextFormat.PlainText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, insertText)));
		item.setSortText("!" + String.format("%02d", sortIndex));
		JsonObject data = RNCCompletionResolver.createResolveData(docUri, RNCCompletionResolver.RESOLVE_OVERRIDE);
		data.addProperty(RNCCompletionResolver.INCLUDE_FILE_URI_FIELD, includeFileUri);
		item.setData(data);
		items.add(item);
	}
}
