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
import org.eclipse.lemminx.dom.DTDDeclNode;
import org.eclipse.lemminx.dom.DTDElementDecl;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix MSG_OPEN_PAREN_OR_ELEMENT_TYPE_REQUIRED_IN_CHILDREN and
 * MSG_CONTENTSPEC_REQUIRED_IN_ELEMENTDECL errors by inserting or replacing a
 * content specification.
 *
 * <ul>
 * <li>{@code <!ELEMENT root >} → inserts {@code EMPTY}, {@code ANY} or
 * {@code (#PCDATA)}</li>
 * <li>{@code <!ELEMENT root XXXX>} → replaces {@code XXXX} with
 * {@code EMPTY}, {@code ANY} or {@code (#PCDATA)}</li>
 * </ul>
 */
public class ContentSpecRequiredCodeAction implements ICodeActionParticipant {

	private static final String[] CONTENT_SPECS = {
			"EMPTY", "ANY", "(#PCDATA)"
	};

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node instanceof DTDElementDecl) {
				DTDDeclNode decl = (DTDDeclNode) node;
				if (decl.unrecognized != null) {
					// <!ELEMENT root XXXX> → replace XXXX
					Range replaceRange = XMLPositionUtility.createRange(
							decl.unrecognized.getStart(), decl.unrecognized.getEnd(), document);
					for (String spec : CONTENT_SPECS) {
						String text = snippetSupport ? spec + "$0" : spec;
						codeActions.add(CodeActionFactory.replace(
								"Replace with '" + spec + "'", replaceRange, text,
								document.getTextDocument(), diagnostic, snippetSupport));
					}
					return;
				}
			}
		} catch (BadLocationException e) {
			// fall through to insert
		}

		// <!ELEMENT root > → insert after element name
		Position insertPos = diagnosticRange.getEnd();
		for (String spec : CONTENT_SPECS) {
			String insertText = snippetSupport
					? " " + spec + "$0"
					: " " + spec;
			codeActions.add(CodeActionFactory.insert(
					"Insert '" + spec + "'", insertPos, insertText,
					document.getTextDocument(), diagnostic, snippetSupport));
		}
	}
}
