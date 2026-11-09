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
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DTDAttlistDecl;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for unterminated enumeration in DTD ATTLIST declarations.
 *
 * <p>
 * {@code <!ATTLIST root status (one|two >} → inserts {@code ) #REQUIRED}
 * (or snippet {@code ) ${1|#REQUIRED,#IMPLIED|}$0}).
 * </p>
 */
public class EnumerationUnterminatedCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();
		Position insertPos = diagnosticRange.getEnd();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node instanceof DTDAttlistDecl) {
				// ATTLIST context: close enumeration and add default declaration
				String insertText = snippetSupport
						? ") ${1|#REQUIRED,#IMPLIED|}$0"
						: ") #REQUIRED";
				codeActions.add(CodeActionFactory.insert(
						"Close with ')' and add default", insertPos, insertText,
						document.getTextDocument(), diagnostic, snippetSupport));
				return;
			}
		} catch (BadLocationException e) {
			// fall through to default behavior
		}

		// ELEMENT context or fallback: just close parenthesis
		String insertText = snippetSupport ? ")$0" : ")";
		codeActions.add(CodeActionFactory.insert(
				"Close with ')'", insertPos, insertText,
				document.getTextDocument(), diagnostic, snippetSupport));
	}
}
