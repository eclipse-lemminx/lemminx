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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix LessthanInAttValue error.
 *
 * Replace '&lt;' with '&amp;lt;' in attribute values.
 */
public class LessthanInAttValueCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int startOffset = document.offsetAt(diagnosticRange.getStart());
			int endOffset = document.offsetAt(diagnosticRange.getEnd());
			CharSequence text = document.getTextSequence();
			for (int i = startOffset; i < endOffset; i++) {
				if (text.charAt(i) == '<') {
					Position ltStart = document.positionAt(i);
					Position ltEnd = document.positionAt(i + 1);
					Range ltRange = new Range(ltStart, ltEnd);
					boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
								.isSnippetEditSupported();
						CodeAction replaceAction = CodeActionFactory.replace("Replace '<' with '&lt;'",
								ltRange, snippetSupport ? "&lt;$0" : "&lt;", document.getTextDocument(),
								diagnostic, snippetSupport);
						codeActions.add(replaceAction);
					break;
				}
			}
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

}
