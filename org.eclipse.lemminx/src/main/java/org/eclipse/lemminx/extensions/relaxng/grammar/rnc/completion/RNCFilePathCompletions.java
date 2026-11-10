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

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.filepath.participants.FilePathCompletionResult;
import org.eclipse.lemminx.utils.CompletionSortTextHelper;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides file path completion items for {@code include} and {@code external}
 * string literals in RNC documents.
 */
class RNCFilePathCompletions {

	private static final Logger LOGGER = Logger.getLogger(RNCFilePathCompletions.class.getName());

	static void collectFilePathCompletions(List<CompletionItem> items, RNCCompletionRequest request) {
		TextDocument textDocument = request.getTextDocument();
		CharSequence text = request.getText();
		int offset = request.getOffset();

		int literalStart = -1;
		int literalEnd = -1;

		for (int i = offset - 1; i >= 0; i--) {
			char ch = text.charAt(i);
			if (ch == '"' || ch == '\'') {
				literalStart = i + 1;
				break;
			}
			if (ch == '\n' || ch == '\r') {
				break;
			}
		}
		if (literalStart == -1) {
			return;
		}

		char openQuote = text.charAt(literalStart - 1);
		for (int i = offset; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == openQuote || ch == '\n' || ch == '\r') {
				literalEnd = i;
				break;
			}
		}
		if (literalEnd == -1) {
			literalEnd = text.length();
		}

		FilePathCompletionResult result = FilePathCompletionResult.create(
				text, textDocument.getUri(), literalStart, literalEnd, offset, null);
		Path baseDir = result.getBaseDir();
		if (baseDir == null) {
			return;
		}

		try {
			Range replaceRange = new Range(
					textDocument.positionAt(result.getStartOffset()),
					textDocument.positionAt(result.getEndOffset()));
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
				for (Path entry : stream) {
					File file = entry.toFile();
					if (file.isDirectory() || DOMUtils.isRelaxNGUriCompactSyntax(file.getName())) {
						String fileName = FilesUtils.encodePath(file.getName());
						CompletionItem item = new CompletionItem(fileName);
						item.setKind(file.isDirectory() ? CompletionItemKind.Folder : CompletionItemKind.File);
						item.setSortText(CompletionSortTextHelper.getSortText(item.getKind()));
						item.setFilterText(fileName);
						item.setTextEdit(Either.forLeft(new TextEdit(replaceRange, fileName)));
						items.add(item);
					}
				}
			}
		} catch (BadLocationException | IOException e) {
			LOGGER.log(Level.SEVERE, "Error while computing include file path completions", e);
		}
	}
}
