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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Dispatches RNC code actions to the appropriate
 * {@link IRNCCodeActionParticipant} based on the diagnostic's error code.
 *
 * <p>Maintains a registry of code action participants, one per
 * {@link RNCErrorCode}. When the editor requests code actions for a
 * diagnostic, this class looks up the matching participant and delegates
 * to it.</p>
 *
 * <p>Use case: the user sees a diagnostic "unclosed-block" and requests
 * quick fixes. This class finds the {@link unclosed_blockCodeAction}
 * participant and invokes it to generate the "Add closing '}'" action.</p>
 *
 * @see IRNCCodeActionParticipant
 * @see RNCErrorCode
 */
public class RNCCodeActions {

	private final Map<String, IRNCCodeActionParticipant> codeActionParticipants;

	private final extract_defineCodeAction extractDefine;

	private final IRNCDocumentProvider documentProvider;

	/**
	 * Creates the code action dispatcher and registers all built-in participants.
	 */
	public RNCCodeActions(IRNCDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
		codeActionParticipants = new HashMap<>();
		registerCodeActionParticipants(codeActionParticipants);
		extractDefine = new extract_defineCodeAction();
	}

	private static void registerCodeActionParticipants(Map<String, IRNCCodeActionParticipant> codeActions) {
		codeActions.put(RNCErrorCode.UnclosedBlock.getCode(), new unclosed_blockCodeAction());
		codeActions.put(RNCErrorCode.MissingOpenBrace.getCode(), new missing_open_braceCodeAction());
		codeActions.put(RNCErrorCode.MissingAssign.getCode(), new missing_assignCodeAction());
		codeActions.put(RNCErrorCode.MissingUri.getCode(), new missing_uriCodeAction());
		codeActions.put(RNCErrorCode.MissingName.getCode(), new missing_nameCodeAction());
		codeActions.put(RNCErrorCode.UndefinedPattern.getCode(), new undefined_patternCodeAction());
		codeActions.put(RNCErrorCode.MissingPattern.getCode(), new missing_patternCodeAction());
		codeActions.put(RNCErrorCode.MissingStartElement.getCode(), new missing_start_elementCodeAction());
		codeActions.put(RNCErrorCode.IncludeNotOverriding.getCode(), new include_not_overridingCodeAction());
	}

	/**
	 * Computes code actions for the given range and diagnostic context.
	 *
	 * @param rncDocument      the parsed RNC document.
	 * @param range            the selected range in the document.
	 * @param context          the code action context containing diagnostics.
	 * @param snippetSupported {@code true} if the client supports snippet syntax.
	 * @param resolveSupported {@code true} if the client supports lazy code action resolve.
	 * @param cancelChecker    the cancel checker.
	 * @return the list of applicable code actions.
	 */
	public List<CodeAction> doCodeActions(RNCDocument rncDocument, Range range, CodeActionContext context,
			boolean snippetSupported, boolean resolveSupported, CancelChecker cancelChecker) {
		List<CodeAction> actions = new ArrayList<>();

		// Diagnostic-based quick fixes
		if (context.getDiagnostics() != null) {
			TextDocumentItem document = new TextDocumentItem(rncDocument.getTextDocument().getUri(), "rnc", 0, "");
			for (Diagnostic diag : context.getDiagnostics()) {
				if (diag.getCode() == null || !diag.getCode().isLeft()) {
					continue;
				}
				IRNCCodeActionParticipant participant = codeActionParticipants.get(diag.getCode().getLeft());
				if (participant != null) {
					participant.doCodeAction(rncDocument, diag, document, snippetSupported, actions,
							documentProvider, cancelChecker);
				}
			}
		}

		// Refactoring code actions (only when not triggered by a diagnostic)
		if (context.getDiagnostics() == null || context.getDiagnostics().isEmpty()) {
			extractDefine.doCodeAction(rncDocument, range, snippetSupported, resolveSupported, actions);
		}

		return actions;
	}

	/**
	 * Resolves a deferred code action by computing its workspace edit.
	 *
	 * @param rncDocument      the parsed RNC document.
	 * @param unresolved       the code action to resolve.
	 * @param snippetSupported {@code true} if the client supports snippet syntax.
	 * @return the resolved code action with its edit populated.
	 */
	public CodeAction resolveCodeAction(RNCDocument rncDocument, CodeAction unresolved,
			boolean snippetSupported) {
		return extractDefine.resolveCodeAction(rncDocument, unresolved, snippetSupported);
	}
}
