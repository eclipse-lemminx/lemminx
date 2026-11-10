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

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides completion items for user-defined references in an RNC document.
 */
class RNCDefineRefCompletions {

	static void collectDefineRefCompletions(List<CompletionItem> items, RNCCompletionRequest request,
			RNCInclude excludeInclude, IRNCDocumentProvider documentProvider) {
		Range replaceRange = request.getReplaceRange();
		for (RNCDefine define : request.getRNCDocument().getAllVisibleDefines(excludeInclude, documentProvider)) {
			items.add(createDefineRefItem(define.getName(), replaceRange));
		}
	}

	static CompletionItem createDefineRefItem(String name, Range replaceRange) {
		CompletionItem item = new CompletionItem(name);
		item.setKind(CompletionItemKind.Reference);
		item.setDetail("define");
		item.setFilterText(name);
		item.setInsertTextFormat(InsertTextFormat.PlainText);
		item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, name)));
		return item;
	}
}
