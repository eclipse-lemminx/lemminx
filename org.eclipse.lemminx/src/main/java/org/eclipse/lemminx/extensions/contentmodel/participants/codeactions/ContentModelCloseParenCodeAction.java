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

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to close a missing parenthesis in DTD content model declarations.
 *
 * <ul>
 * <li>{@code <!ELEMENT root (a|b >} with insertText {@code )} → inserts
 * {@code )}</li>
 * <li>{@code <!ELEMENT root (#PCDATA|item >} with insertText {@code )*} →
 * inserts {@code )*}</li>
 * </ul>
 */
public class ContentModelCloseParenCodeAction implements ICodeActionParticipant {

	private final String insertText;
	private final String label;

	public ContentModelCloseParenCodeAction(String insertText, String label) {
		this.insertText = insertText;
		this.label = label;
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		CodeAction action = CodeActionFactory.insert(label, diagnosticRange.getEnd(),
				snippetSupport ? insertText + "$0" : insertText,
				document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(action);
	}
}
