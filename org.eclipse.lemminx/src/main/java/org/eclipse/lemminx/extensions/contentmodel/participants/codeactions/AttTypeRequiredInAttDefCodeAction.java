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
 * Code action to fix AttTypeRequiredInAttDef error by replacing the invalid
 * attribute type with a valid DTD attribute type.
 *
 * {@code <!ATTLIST root status invalid>} → replaces {@code invalid} with
 * {@code CDATA #REQUIRED}, {@code ID #REQUIRED}, etc.
 */
public class AttTypeRequiredInAttDefCodeAction implements ICodeActionParticipant {

	private static final String[] VALID_ATT_TYPES = {
			"CDATA", "ID", "IDREF", "IDREFS",
			"ENTITY", "ENTITIES", "NMTOKEN", "NMTOKENS"
	};

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		for (String type : VALID_ATT_TYPES) {
			String replaceText = snippetSupport
					? type + " ${1|#REQUIRED,#IMPLIED|}$0"
					: type + " #REQUIRED";
			CodeAction action = CodeActionFactory.replace(
					"Replace with '" + type + "'", diagnosticRange, replaceText,
					document.getTextDocument(), diagnostic, snippetSupport);
			codeActions.add(action);
		}
	}
}
