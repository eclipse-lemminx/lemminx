/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix unterminated DTD declaration errors by inserting a closing
 * {@code >}.
 *
 * <ul>
 * <li>{@code <!ELEMENT root EMPTY} → inserts {@code >}</li>
 * <li>{@code <!ENTITY myEntity "value"} → inserts {@code >}</li>
 * <li>{@code <!NOTATION name SYSTEM "id"} → inserts {@code >}</li>
 * </ul>
 */
public class ElementDeclUnterminatedCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		// Close with '>
		CodeAction closeAction = CodeActionFactory.insert("Close with '>'", diagnosticRange.getEnd(),
				snippetSupport ? ">$0" : ">",
				document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(closeAction);
	}

}
