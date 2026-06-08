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
package org.eclipse.lemminx.utils;

import java.util.List;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Utilities for {@link TextEdit} specific to DOM documents.
 * 
 * @author Arun Venmany
 *
 */
public class DOMTextEditUtils {

	/**
	 * Creates a TextDocumentEdit object for the specified document and list of text edits
	 *
	 * @param document Document to be changed
	 * @param textEdits a list of text edit changes
	 * @return A Text Document Edit object
	 */
	public static TextDocumentEdit creatTextDocumentEdit(DOMDocument document, List<TextEdit> textEdits) {
		VersionedTextDocumentIdentifier projectVersionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getDocumentURI(), document.getTextDocument().getVersion());
		// Convert List<TextEdit> to List<Either<TextEdit, SnippetTextEdit>> for LSP4J 1.0.0
		List<Either<TextEdit, SnippetTextEdit>> edits = org.eclipse.lemminx.commons.TextEditUtils.toEitherTextEdits(textEdits);
		return new TextDocumentEdit(projectVersionedTextDocumentIdentifier, edits);
	}

	private DOMTextEditUtils() {
		// Utility class, no instantiation
	}
}