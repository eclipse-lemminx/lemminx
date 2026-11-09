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
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix IDDefaultTypeInvalid error by replacing the invalid
 * default value with {@code #IMPLIED} or {@code #REQUIRED}.
 *
 * {@code <!ATTLIST root id ID "default">} → replaces {@code "default"} with
 * {@code #IMPLIED} or {@code #REQUIRED}
 */
public class IDDefaultTypeInvalidCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node instanceof DTDAttlistDecl) {
				DTDAttlistDecl attlistDecl = (DTDAttlistDecl) node;
				if (attlistDecl.attributeValue != null) {
					Range valueRange = XMLPositionUtility.createRange(
							attlistDecl.attributeValue.getStart(),
							attlistDecl.attributeValue.getEnd(), document);

					CodeAction impliedAction = CodeActionFactory.replace(
							"Replace with #IMPLIED", valueRange,
							snippetSupport ? "#IMPLIED$0" : "#IMPLIED",
							document.getTextDocument(), diagnostic, snippetSupport);
					codeActions.add(impliedAction);

					CodeAction requiredAction = CodeActionFactory.replace(
							"Replace with #REQUIRED", valueRange,
							snippetSupport ? "#REQUIRED$0" : "#REQUIRED",
							document.getTextDocument(), diagnostic, snippetSupport);
					codeActions.add(requiredAction);
				}
			}
		} catch (BadLocationException e) {
		}
	}
}
