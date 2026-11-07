/**
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Red Hat Inc. - initial API and implementation
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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix XMLDeclUnterminated error.
 *
 * Insert the missing closing delimiter for the XML declaration.
 */
public class XMLDeclUnterminatedCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		try {
			CharSequence text = document.getTextSequence();
			int length = text.length();
			int lastNonWS = length - 1;
			while (lastNonWS >= 0 && Character.isWhitespace(text.charAt(lastNonWS))) {
				lastNonWS--;
			}
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
						.isSnippetEditSupported();
			if (lastNonWS >= 0 && text.charAt(lastNonWS) == '?') {
				Position insertPos = document.positionAt(lastNonWS + 1);
				CodeAction closeAction = CodeActionFactory.insert("Close with '>'", insertPos,
						snippetSupport ? ">$0" : ">", document.getTextDocument(), diagnostic, snippetSupport);
				codeActions.add(closeAction);
			} else {
				Position insertPos = document.positionAt(lastNonWS + 1);
				CodeAction closeAction = CodeActionFactory.insert("Close with '?>'", insertPos,
						snippetSupport ? "?>$0" : "?>", document.getTextDocument(), diagnostic, snippetSupport);
				codeActions.add(closeAction);
			}
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

}
