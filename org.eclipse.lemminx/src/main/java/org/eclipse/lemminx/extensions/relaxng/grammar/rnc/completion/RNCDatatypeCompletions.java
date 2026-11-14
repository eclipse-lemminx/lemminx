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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypeLibrary;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypesDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.google.gson.JsonObject;

/**
 * Provides completion items for datatype prefixes ({@code xsd:integer}) and
 * datatype library URIs ({@code datatypes xsd = |}).
 */
class RNCDatatypeCompletions {

	/**
	 * Collects datatype name completions when the cursor follows a {@code prefix:}
	 * pattern (e.g. {@code xsd:in|}).
	 *
	 * @return {@code true} if the cursor is in a datatype prefix context,
	 *         {@code false} otherwise.
	 */
	static void collectDatatypeCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		collectDatatypeCompletions(items, request.getRNCDocument(), request.getReplaceRange());
	}

	static void collectDatatypeLibraryUriCompletions(List<CompletionItem> items, Range replaceRange) {
		for (RNCDatatypeLibrary library : RNCDatatypeLibrary.getLibraries()) {
			String uri = library.getUri();
			if (uri.isEmpty()) {
				continue;
			}
			String quotedUri = "\"" + uri + "\"";
			CompletionItem item = new CompletionItem(quotedUri);
			item.setKind(CompletionItemKind.Value);
			item.setFilterText(quotedUri);
			item.setDetail(library.getDescription());
			item.setInsertTextFormat(InsertTextFormat.PlainText);
			item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, quotedUri)));
			items.add(item);
		}
	}

	public static void collectDatatypeCompletions(List<CompletionItem> items, RNCDocument rncDocument,
			Range replaceRange) {
		String docUri = rncDocument.getUri();
		for (RNCDatatypesDecl decl : rncDocument.getDatatypesDecls()) {
			String prefix = decl.getPrefix();
			String uri = decl.getUri();
			RNCDatatypeLibrary library = RNCDatatypeLibrary.get(uri);
			if (library != null) {
				for (RNCDatatypeLibrary.Entry entry : library.getEntries()) {
					String name = entry.getName();
					String qualifiedName = prefix + ":" + name;
					CompletionItem item = new CompletionItem(qualifiedName);
					item.setKind(CompletionItemKind.TypeParameter);
					item.setDetail("Datatype");
					item.setFilterText(qualifiedName);
					item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, qualifiedName)));
					JsonObject data = RNCCompletionResolver.createResolveData(docUri,
							RNCCompletionResolver.RESOLVE_DATATYPE);
					data.addProperty(RNCCompletionResolver.DATATYPE_LIBRARY_URI_FIELD, library.getUri());
					item.setData(data);
					items.add(item);
				}
			}
		}
	}
}
