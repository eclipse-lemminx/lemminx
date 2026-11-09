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

import java.util.Collection;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix cvc-complex-type.3.1 error (fixed value mismatch).
 * Offers to replace the attribute value with the correct fixed value,
 * and to remove the attribute entirely.
 */
public class cvc_complex_type_3_1CodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getEnd());
			DOMAttr attr = document.findAttrAt(offset);
			if (attr == null) {
				return;
			}

			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

			DOMElement element = attr.getOwnerElement();
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
			Collection<CMDocument> cmDocuments = contentModelManager.findCMDocument(element);
			for (CMDocument cmDocument : cmDocuments) {
				CMAttributeDeclaration cmAttribute = cmDocument.findCMAttribute(attr);
				if (cmAttribute != null) {
					String fixedValue = cmAttribute.getDefaultValue();
					if (fixedValue != null) {
						Range rangeValue = new Range(
								new Position(diagnosticRange.getStart().getLine(),
										diagnosticRange.getStart().getCharacter() + 1),
								new Position(diagnosticRange.getEnd().getLine(),
										diagnosticRange.getEnd().getCharacter() - 1));
						String replaceText = snippetSupport
								? CodeActionFactory.escapeSnippetText(fixedValue) + "$0"
								: fixedValue;
						CodeAction replaceAction = CodeActionFactory.replace(
								"Replace with '" + fixedValue + "'",
								rangeValue, replaceText,
								document.getTextDocument(), diagnostic, snippetSupport);
						codeActions.add(replaceAction);
					}
					break;
				}
			}

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
					"Remove '" + attr.getName() + "' attribute",
					attrRange, snippetSupport ? "$0" : "", document.getTextDocument(), diagnostic,
					snippetSupport);
			codeActions.add(removeAction);

		} catch (BadLocationException e) {
			// Do nothing
		}
	}

}
