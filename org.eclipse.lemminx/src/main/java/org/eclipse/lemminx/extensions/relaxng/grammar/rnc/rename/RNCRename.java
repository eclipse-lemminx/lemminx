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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.rename;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides rename support for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Supports renaming define names: when the user renames a define like
 * {@code root}, all references to {@code root} across the document are
 * updated consistently.</p>
 *
 * <p>Use case: the user places the cursor on {@code root} and renames it
 * to {@code rootElement}. Both the declaration ({@code root = element ...})
 * and all references ({@code start = root}) are updated.</p>
 *
 * <p>The rename is preceded by a "prepare rename" step that validates the
 * cursor is on a renamable symbol (a define name) and returns the current
 * word range for the editor to pre-fill the rename input box.</p>
 *
 * @see RNCSearchUtils
 */
public class RNCRename {

	private static final Logger LOGGER = Logger.getLogger(RNCRename.class.getName());

	/**
	 * Validates that the cursor is on a renamable define name and returns
	 * the word range for the editor to pre-fill the rename input box.
	 *
	 * @param rncDocument the parsed RNC document.
	 * @param position    the cursor position.
	 * @return the word range and current name, or {@code null} if the cursor
	 *         is not on a renamable symbol.
	 */
	public Either<Range, PrepareRenameResult> prepareRename(RNCDocument rncDocument, Position position) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word == null || word.isEmpty()) {
				return null;
			}

			RNCDefine define = rncDocument.findDefine(word);
			if (define == null) {
				return null;
			}

			Range range = RNCPositionUtils.getWordRange(textDocument, offset, word);
			if (range == null) {
				return null;
			}
			return Either.forRight(new PrepareRenameResult(range, word));
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC prepare rename", e);
		}
		return null;
	}

	/**
	 * Renames the define at the given position, updating its declaration and
	 * all references within the document.
	 *
	 * @param rncDocument   the parsed RNC document.
	 * @param position      the cursor position on the define to rename.
	 * @param newName       the new name to apply.
	 * @param cancelChecker used to check if the operation has been canceled.
	 * @return a {@link WorkspaceEdit} with all text edits, or {@code null} if
	 *         the cursor is not on a renamable define.
	 */
	public WorkspaceEdit doRename(RNCDocument rncDocument, Position position, String newName,
			CancelChecker cancelChecker) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word == null || word.isEmpty()) {
				return null;
			}

			RNCDefine define = rncDocument.findDefine(word);
			if (define == null) {
				return null;
			}

			List<Location> locations = RNCSearchUtils.findReferences(textDocument, text, define, true);
			if (locations.isEmpty()) {
				return null;
			}

			List<TextEdit> edits = new ArrayList<>();
			for (Location location : locations) {
				edits.add(new TextEdit(location.getRange(), newName));
			}

			WorkspaceEdit workspaceEdit = new WorkspaceEdit();
			workspaceEdit.setChanges(Collections.singletonMap(textDocument.getUri(), edits));
			return workspaceEdit;
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC rename", e);
		}
		return null;
	}
}
