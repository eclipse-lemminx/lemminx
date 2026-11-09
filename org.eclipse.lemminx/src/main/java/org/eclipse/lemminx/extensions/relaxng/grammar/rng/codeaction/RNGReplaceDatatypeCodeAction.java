/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction;

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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to replace an unrecognized datatype value in the {@code type}
 * attribute of a {@code <data>} or {@code <value>} element.
 *
 * <p>
 * The list of valid type values is retrieved from the content model and
 * filtered by the effective {@code datatypeLibrary} attribute via
 * {@link CMAttributeDeclaration#getEnumerationValues(DOMElement)}.
 * </p>
 */
public class RNGReplaceDatatypeCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMAttr attr = document.findAttrAt(offset);
			if (attr == null) {
				return;
			}
			DOMElement element = attr.getOwnerElement();
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
			for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
				CMAttributeDeclaration cmAttribute = cmDocument.findCMAttribute(attr);
				if (cmAttribute != null) {
					Collection<String> types = cmAttribute.getEnumerationValues(element);
					boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
							.isSnippetEditSupported();
					String currentType = attr.getValue();
					for (String type : types) {
						if (type.equals(currentType)) {
							continue;
						}
						String replacement = "\"" + type + "\"";
						CodeAction replaceAction = CodeActionFactory.replace(
								"Replace with '" + type + "'",
								diagnosticRange, replacement, document.getTextDocument(),
								diagnostic, snippetSupport);
						codeActions.add(replaceAction);
					}
				}
			}
		} catch (BadLocationException e) {
			// Do nothing
		}
	}
}
