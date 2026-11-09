/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import static org.eclipse.lemminx.utils.StringUtils.isSimilar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix cvc-complex-type.3.2.2 error.
 *
 */
public class cvc_complex_type_3_2_2CodeAction extends RemoveAttributeCodeAction {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getEnd());
			DOMAttr attr = document.findAttrAt(offset);
			if (attr != null) {
				DOMElement element = attr.getOwnerElement();
				String attrName = attr.getLocalName();
				ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
				Collection<CMDocument> cmDocuments = contentModelManager.findCMDocument(element);
				for (CMDocument cmDocument : cmDocuments) {
					CMElementDeclaration cmElement = cmDocument.findCMElement(element);
					if (cmElement != null) {
						Collection<CMAttributeDeclaration> cmAttributes = cmElement.getAttributes();

						Collection<String> similarNames = new TreeSet<>(Collator.getInstance());
						List<String> requiredNames = new ArrayList<>();
						Collection<String> otherNames = new TreeSet<>(Collator.getInstance());

						for (CMAttributeDeclaration cmAttribute : cmAttributes) {
							String validName = cmAttribute.getLocalName();
							if (validName.equals(attrName)) {
								continue;
							}
							if (element.getAttributeNode(validName) != null) {
								continue;
							}
							if (isSimilar(validName, attrName)) {
								similarNames.add(validName);
							} else if (cmAttribute.isRequired()) {
								requiredNames.add(validName);
							} else {
								otherNames.add(validName);
							}
						}

						boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
								.isSnippetEditSupported();

						if (!similarNames.isEmpty()) {
							for (String similarName : similarNames) {
								String replaceText = snippetSupport
										? CodeActionFactory.escapeSnippetText(similarName) + "$0"
										: similarName;
								CodeAction similarCodeAction = CodeActionFactory.replace(
										"Did you mean '" + similarName + "'?", diagnosticRange, replaceText,
										document.getTextDocument(), diagnostic, snippetSupport);
								codeActions.add(similarCodeAction);
							}
						} else {
							for (String requiredName : requiredNames) {
								String replaceText = snippetSupport
										? CodeActionFactory.escapeSnippetText(requiredName) + "$0"
										: requiredName;
								CodeAction requiredCodeAction = CodeActionFactory.replace(
										"Replace with '" + requiredName + "'", diagnosticRange, replaceText,
										document.getTextDocument(), diagnostic, snippetSupport);
								codeActions.add(requiredCodeAction);
							}
							for (String otherName : otherNames) {
								String replaceText = snippetSupport
										? CodeActionFactory.escapeSnippetText(otherName) + "$0"
										: otherName;
								CodeAction otherCodeAction = CodeActionFactory.replace(
										"Replace with '" + otherName + "'", diagnosticRange, replaceText,
										document.getTextDocument(), diagnostic, snippetSupport);
								codeActions.add(otherCodeAction);
							}
						}
						break;
					}
				}
			}
		} catch (Exception e) {
			// Do nothing
		}
		super.doCodeAction(request, codeActions, cancelChecker);
	}

}
