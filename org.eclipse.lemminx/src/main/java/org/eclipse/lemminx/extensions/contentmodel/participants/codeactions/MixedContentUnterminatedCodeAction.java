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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix MixedContentUnterminated error by inserting the missing
 * {@code )*} or {@code *} before {@code >}.
 *
 * <ul>
 * <li>{@code <!ELEMENT root (#PCDATA|item >} → inserts {@code )*}</li>
 * <li>{@code <!ELEMENT root (#PCDATA|item) >} → inserts {@code *}</li>
 * </ul>
 */
public class MixedContentUnterminatedCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			CharSequence text = document.getTextSequence();
			int pos = offset - 1;
			while (pos >= 0 && Character.isWhitespace(text.charAt(pos))) {
				pos--;
			}
			boolean hasCloseParen = pos >= 0 && text.charAt(pos) == ')';
			String insertText = hasCloseParen ? "*" : ")*";
			String label = hasCloseParen ? "Close with '*'" : "Close with ')*'";

			CodeAction action = CodeActionFactory.insert(label, diagnosticRange.getStart(),
					snippetSupport ? insertText + "$0" : insertText,
					document.getTextDocument(), diagnostic, snippetSupport);
			codeActions.add(action);
		} catch (BadLocationException e) {
		}
	}
}
