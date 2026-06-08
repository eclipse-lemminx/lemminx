/*******************************************************************************
* Copyright (c) 2022, 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.utils;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Utilities for {@link TextEdit}.
 * 
 * @author Angelo ZERR
 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils} for generic LSP4J utilities
 *             or {@link DOMTextEditUtils} for DOM-specific utilities
 */
@Deprecated
public class TextEditUtils {

	/**
	 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils#toEitherTextEdits(List)}
	 */
	@Deprecated
	public static List<Either<TextEdit, SnippetTextEdit>> toEitherTextEdits(List<TextEdit> textEdits) {
		return org.eclipse.lemminx.commons.TextEditUtils.toEitherTextEdits(textEdits);
	}

	/**
	 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils#createTextEditIfNeeded(int, int, String, TextDocument)}
	 */
	@Deprecated
	public static TextEdit createTextEditIfNeeded(int from, int to, String expectedContent, TextDocument textDocument) {
		return org.eclipse.lemminx.commons.TextEditUtils.createTextEditIfNeeded(from, to, expectedContent, textDocument);
	}

	/**
	 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils#applyEdits(TextDocument, List)}
	 */
	@Deprecated
	public static String applyEdits(TextDocument document, List<? extends TextEdit> edits) throws BadLocationException {
		return org.eclipse.lemminx.commons.TextEditUtils.applyEdits(document, edits);
	}

	/**
	 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils#adjustOffsetWithLeftWhitespaces(int, int, String)}
	 */
	@Deprecated
	public static int adjustOffsetWithLeftWhitespaces(int leftLimit, int to, String text) {
		return org.eclipse.lemminx.commons.TextEditUtils.adjustOffsetWithLeftWhitespaces(leftLimit, to, text);
	}

	/**
	 * @deprecated Use {@link DOMTextEditUtils#creatTextDocumentEdit(DOMDocument, List)}
	 */
	@Deprecated
	public static TextDocumentEdit creatTextDocumentEdit(DOMDocument document, List<TextEdit> textEdits) {
		return DOMTextEditUtils.creatTextDocumentEdit(document, textEdits);
	}
	
	/**
	 * @deprecated Use {@link org.eclipse.lemminx.commons.TextEditUtils#createWorkspaceEdit(List)}
	 */
	@Deprecated
	public static WorkspaceEdit createWorkspaceEdit(List<Either<TextDocumentEdit, ResourceOperation>> documentChanges) {
		return org.eclipse.lemminx.commons.TextEditUtils.createWorkspaceEdit(documentChanges);
	}
}