/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.lemminx.services.extensions.rename.IRenameResponse;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Rename response object to store the rename operation results
 */
class RenameResponse implements IRenameResponse {
	private List<Either<TextDocumentEdit, ResourceOperation>> documentChanges = new ArrayList<>();

	@Override
	public void addTextDocumentEdit(TextDocumentEdit textDocumentEdit) {
		if (textDocumentEdit == null) {
			return;
		}
		
		String documentURI = textDocumentEdit.getTextDocument().getUri();
		Optional<TextDocumentEdit> change = documentChanges.stream().filter(Either::isLeft)
				.filter(e -> documentURI.equals(e.getLeft().getTextDocument().getUri()))
				.map(Either::getLeft).findFirst();
		if(change.isPresent()) {
			TextDocumentEdit existingTextDocumentEdit = change.get();
			// Convert Either<TextEdit, SnippetTextEdit> to TextEdit for comparison
			List<Either<TextEdit, SnippetTextEdit>> edits = new ArrayList<>();
			edits.addAll(existingTextDocumentEdit.getEdits());
			textDocumentEdit.getEdits().stream().forEach(te -> {
				if (!edits.contains(te)) {
					edits.add(te);
				}
			});
			existingTextDocumentEdit.setEdits(edits);
		} else {
			documentChanges.add(Either.forLeft(textDocumentEdit));
		}
	}

	@Override
	public void addResourceOperation(ResourceOperation resourceOperation) {
		documentChanges.add(Either.forRight(resourceOperation));
	}
	
	public List<Either<TextDocumentEdit, ResourceOperation>> getDocumentChanges() {
		return documentChanges;
	}
}
