/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix cvc-attribute-3 error.
 *
 */
public class s4s_elt_invalid_content_3CodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		try {
			int offset = document.offsetAt(diagnostic.getRange().getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node != null && node.isElement()) {
				DOMElement element = (DOMElement) node;
				int startOffset = element.getStartTagOpenOffset();
				int endOffset;
				if (element.isSelfClosed()) {
					endOffset = element.getEnd();
				} else {
					endOffset = element.getEndTagCloseOffset() + 1;
				}

				Range diagnosticRange = XMLPositionUtility.createRange(startOffset, endOffset, document);
				boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
						.isSnippetEditSupported();
				CodeAction removeContentAction = CodeActionFactory.replace("Remove element",
						diagnosticRange, snippetSupport ? "$0" : "", document.getTextDocument(), diagnostic,
						snippetSupport);
				codeActions.add(removeContentAction);
			}

		} catch (BadLocationException e) {
			// Do nothing
		}
	}

}
