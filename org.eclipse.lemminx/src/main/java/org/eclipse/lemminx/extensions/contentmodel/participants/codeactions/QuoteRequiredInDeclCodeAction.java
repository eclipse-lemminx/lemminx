/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix QuoteRequiredInPublicID, QuoteRequiredInSystemID, and
 * OpenQuoteMissingInDecl errors by wrapping the unquoted value in double
 * quotes.
 *
 * {@code <!NOTATION name2 SYSTEM asd>} → replaces {@code asd} with
 * {@code "asd"}
 */
public class QuoteRequiredInDeclCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		try {
			int startOffset = document.offsetAt(diagnosticRange.getStart());
			int endOffset = document.offsetAt(diagnosticRange.getEnd());
			String unquotedText = StringUtils.getString(document.getTextSequence(), startOffset, endOffset);
			String quotedText = "\"" + unquotedText + "\"";

			CodeAction action = CodeActionFactory.replace(
					"Wrap with quotes", diagnosticRange,
					snippetSupport ? quotedText + "$0" : quotedText,
					document.getTextDocument(), diagnostic, snippetSupport);
			codeActions.add(action);
		} catch (BadLocationException e) {
		}
	}
}
