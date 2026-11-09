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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for missing SYSTEM/PUBLIC keyword after notation name.
 *
 * {@code <!NOTATION name>} → inserts {@code SYSTEM "systemId"} or
 * {@code PUBLIC "publicId"}
 */
public class NotationDeclTypeCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();
		Position insertPos = diagnosticRange.getEnd();

		// <!NOTATION name> → insert ' SYSTEM "systemId"'
		String systemText = snippetSupport
				? " SYSTEM \"${1:systemId}\"$0"
				: " SYSTEM \"systemId\"";
		codeActions.add(CodeActionFactory.insert(
				"Insert 'SYSTEM' declaration", insertPos, systemText,
				document.getTextDocument(), diagnostic, snippetSupport));

		// <!NOTATION name> → insert ' PUBLIC "publicId"'
		String publicText = snippetSupport
				? " PUBLIC \"${1:publicId}\"$0"
				: " PUBLIC \"publicId\"";
		codeActions.add(CodeActionFactory.insert(
				"Insert 'PUBLIC' declaration", insertPos, publicText,
				document.getTextDocument(), diagnostic, snippetSupport));
	}
}
