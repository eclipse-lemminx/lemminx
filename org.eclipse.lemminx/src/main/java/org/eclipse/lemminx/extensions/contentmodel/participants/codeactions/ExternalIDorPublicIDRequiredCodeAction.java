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
 * Code action to fix ExternalIDorPublicIDRequired error by inserting PUBLIC or
 * SYSTEM keyword.
 *
 * {@code <!NOTATION name1 >} → inserts {@code PUBLIC "publicId"} or
 * {@code SYSTEM "systemId"}
 */
public class ExternalIDorPublicIDRequiredCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		CodeAction publicAction = CodeActionFactory.insert("Insert PUBLIC", diagnosticRange.getEnd(),
				snippetSupport ? " PUBLIC \"${1:publicId}\"$0" : " PUBLIC \"publicId\"",
				document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(publicAction);

		CodeAction systemAction = CodeActionFactory.insert("Insert SYSTEM", diagnosticRange.getEnd(),
				snippetSupport ? " SYSTEM \"${1:systemId}\"$0" : " SYSTEM \"systemId\"",
				document.getTextDocument(), diagnostic, snippetSupport);
		codeActions.add(systemAction);
	}
}
