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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction;

import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Base class for simple RNC code actions that insert text at the diagnostic's
 * end position.
 *
 * <p>Subclasses provide the insert text and title; this class handles the
 * boilerplate of creating the {@link CodeAction} with the correct range.</p>
 *
 * <p>Use case: code actions for missing tokens (missing assign, missing name,
 * missing URI) share the same pattern — insert a snippet at the end of the
 * error range.</p>
 *
 * @see missing_assignCodeAction
 * @see missing_nameCodeAction
 * @see missing_uriCodeAction
 */
public abstract class AbstractRNCInsertCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Creates a {@link CodeAction} that inserts text at the end of the
	 * diagnostic range. Delegates to {@link #getTitle()} for the action label
	 * and {@link #getInsertText(boolean)} for the text to insert.
	 *
	 * @param rncDocument      the RNC document.
	 * @param diagnostic       the diagnostic to fix.
	 * @param document         the text document.
	 * @param snippetSupported {@code true} if the client supports snippet syntax
	 *                         (tab stops, placeholders).
	 * @param codeActions      the list to add the created code action to.
	 * @param cancelChecker    the cancel checker.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		Position insertPos = diagnostic.getRange().getEnd();
		String text = getInsertText(snippetSupported);
		codeActions.add(CodeActionFactory.insert(getTitle(), insertPos, text, document, diagnostic, snippetSupported));
	}

	/**
	 * Returns the human-readable title for the code action, displayed in the
	 * editor's quick-fix menu (e.g. "Add '='", "Add name", "Add URI").
	 *
	 * @return the code action title.
	 */
	protected abstract String getTitle();

	/**
	 * Returns the text to insert at the diagnostic's end position.
	 *
	 * <p>When {@code snippetSupported} is {@code true}, the returned text may
	 * contain LSP snippet syntax ({@code $0} for final cursor, {@code ${1:placeholder}}
	 * for editable placeholders) to guide the user after the fix is applied.</p>
	 *
	 * @param snippetSupported {@code true} if the client supports snippet syntax.
	 * @return the text to insert (plain or snippet).
	 * @see org.eclipse.lemminx.commons.SnippetsBuilder
	 */
	protected abstract String getInsertText(boolean snippetSupported);
}
