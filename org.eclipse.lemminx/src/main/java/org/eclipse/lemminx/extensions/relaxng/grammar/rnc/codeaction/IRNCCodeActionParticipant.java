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

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Interface for RNC code action participants.
 *
 * <p>Each implementation provides quick fixes for a specific
 * {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode}.
 * Participants are registered in {@link RNCCodeActions} and invoked when the
 * editor requests code actions for a diagnostic.</p>
 *
 * <p>Use case: the user sees an error squiggle on an unclosed block and clicks
 * the light bulb — the matching participant generates a "Add closing '}'"
 * quick fix.</p>
 *
 * @see RNCCodeActions
 */
public interface IRNCCodeActionParticipant {

	void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker);
}
