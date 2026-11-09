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
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.xsd.utils.XSDUtils;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix src-element.1 and src-attribute.1 errors where both
 * 'default' and 'fixed' attributes are present. Offers to remove either one.
 */
public class RemoveDefaultOrFixedCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		try {
			int offset = document.offsetAt(diagnostic.getRange().getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node == null || !node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;
			if (element.getAttributeNode(XSDUtils.DEFAULT_ATTR) == null || element.getAttributeNode(XSDUtils.FIXED_ATTR) == null) {
				return;
			}
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();
			removeAttribute(element, XSDUtils.DEFAULT_ATTR, document, diagnostic, codeActions, snippetSupport);
			removeAttribute(element, XSDUtils.FIXED_ATTR, document, diagnostic, codeActions, snippetSupport);
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

	private static void removeAttribute(DOMElement element, String attrName, DOMDocument document,
			Diagnostic diagnostic, List<CodeAction> codeActions, boolean snippetSupport) throws BadLocationException {
		DOMAttr attr = element.getAttributeNode(attrName);
		if (attr != null) {
			int startOffset = attr.getStart();
			int endOffset = attr.getEnd();
			CharSequence text = document.getTextSequence();
			if (startOffset > 0) {
				char ch = text.charAt(startOffset - 1);
				if (ch == ' ' || ch == '\t') {
					startOffset--;
				}
			}
			Range attrRange = new Range(document.positionAt(startOffset), document.positionAt(endOffset));
			CodeAction removeAction = CodeActionFactory.replace(
					"Remove '" + attrName + "' attribute",
					attrRange, snippetSupport ? "$0" : "", document.getTextDocument(), diagnostic,
					snippetSupport);
			codeActions.add(removeAction);
		}
	}

}
