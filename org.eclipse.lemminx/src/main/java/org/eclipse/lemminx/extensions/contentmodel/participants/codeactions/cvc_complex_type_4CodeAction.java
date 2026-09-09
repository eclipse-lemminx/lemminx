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

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.utils.XMLGenerator;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix cvc-complex-type.4 error.
 *
 */
public class cvc_complex_type_4CodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range range = request.getRange();
		if (diagnostic == null) {
			return;
		}

		if (codeAlreadyActionExists(codeActions, diagnostic)) {
			return;
		}

		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(range.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (!node.isElement()) {
				return;
			}
			SharedSettings sharedSettings = request.getSharedSettings();
			DOMElement element = (DOMElement) node;
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
			for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
				CMElementDeclaration elementDeclaration = cmDocument.findCMElement(element);
				if (elementDeclaration != null) {
					List<CMAttributeDeclaration> requiredAttributes = elementDeclaration.getAttributes().stream()
							.filter(CMAttributeDeclaration::isRequired) //
							.filter(cmAttr -> !element.hasAttribute(cmAttr.getLocalName())) //
							.collect(Collectors.toList());

					// CodeAction doesn't support snippet ->
					// https://github.com/Microsoft/language-server-protocol/issues/592
					boolean supportSnippet = false;
					XMLGenerator generator = new XMLGenerator(sharedSettings, "", "", supportSnippet, 0);
					String xmlAttributes = generator.generate(requiredAttributes, element.getTagName());

					// Insert after the last existing attribute (if any),
					// otherwise after the tag name.
					Position insertPosition = getInsertAttrPosition(element, document, diagnosticRange);
					CodeAction insertRequiredAttributesAction = CodeActionFactory.insert("Insert required attributes",
							insertPosition, xmlAttributes, document.getTextDocument(), diagnostic);
					codeActions.add(insertRequiredAttributesAction);
				}
			}
		} catch (Exception e) {
			// Do nothing
		}
	}

	private static Position getInsertAttrPosition(DOMElement element, DOMDocument document, Range diagnosticRange) {
		if (element.hasAttributes()) {
			DOMAttr lastAttr = null;
			for (DOMAttr attr : element.attributes()) {
				lastAttr = attr;
			}
			if (lastAttr != null) {
				try {
					return document.positionAt(lastAttr.getEnd());
				} catch (Exception e) {
					// Fall through to default
				}
			}
		}
		return diagnosticRange.getEnd();
	}

	private boolean codeAlreadyActionExists(List<CodeAction> codeActions, Diagnostic diagnostic) {
		for (CodeAction codeAction : codeActions) {
			for (Diagnostic codeActionDiagnostic : codeAction.getDiagnostics()) {
				if (codeActionDiagnostic.getCode().equals(diagnostic.getCode())
						&& codeActionDiagnostic.getRange().equals(diagnostic.getRange())) {
					return true;
				}
			}
		}
		return false;

	}

}
