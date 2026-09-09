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
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix RelaxNG required_attribute_missing,
 * required_attributes_missing, and required_attributes_missing_expected errors.
 *
 * <p>
 * Inserts missing required attributes on the element. First tries globally
 * required attributes (required in all {@code <choice>} branches). If all
 * globally required attributes are already present, falls back to
 * context-aware lookup via {@code getPossibleAttributes(element)} which
 * narrows to the matching choice branch based on existing attribute values.
 * </p>
 */
public class required_attribute_missingCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range range = request.getRange();
		if (diagnostic == null) {
			return;
		}

		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(range.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (!node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
			for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
				CMElementDeclaration elementDeclaration = cmDocument.findCMElement(element);
				if (elementDeclaration != null) {
					// First try globally required attributes (required in all choice branches)
					List<CMAttributeDeclaration> requiredAttributes = elementDeclaration.getAttributes().stream()
							.filter(CMAttributeDeclaration::isRequired)
							.filter(cmAttr -> !element.hasAttribute(cmAttr.getLocalName()))
							.collect(Collectors.toList());

					if (requiredAttributes.isEmpty()) {
						// All globally required attributes are present, but the validator
						// still reports a missing attribute. This happens with <choice>
						// groups: e.g. Type="Bar" selects the Bar branch, making BarAttr
						// required in context. Fall back to context-aware lookup.
						Collection<CMAttributeDeclaration> possibleAttributes = elementDeclaration
								.getPossibleAttributes(element);
						requiredAttributes = possibleAttributes.stream()
								.filter(cmAttr -> !element.hasAttribute(cmAttr.getLocalName()))
								.collect(Collectors.toList());
					}

					if (requiredAttributes.isEmpty()) {
						return;
					}

					// Build the attribute insertion text. XMLGenerator.generate()
					// filters by isRequired() internally, which drops context-aware
					// attributes from choice branches. Build the text directly.
					StringBuilder xmlAttributes = new StringBuilder();
					for (CMAttributeDeclaration attr : requiredAttributes) {
						xmlAttributes.append(' ');
						xmlAttributes.append(attr.getLocalName());
						xmlAttributes.append("=\"\"");
					}

					// Insert after the last existing attribute (if any),
					// otherwise after the tag name.
					Position insertPosition = getInsertAttrPosition(element, document, diagnosticRange);

					CodeAction insertRequiredAttributesAction = CodeActionFactory.insert(
							"Insert required attributes",
							insertPosition, xmlAttributes.toString(), document.getTextDocument(), diagnostic);
					codeActions.add(insertRequiredAttributesAction);
				}
			}
		} catch (Exception e) {
			// Do nothing
		}
	}

	/**
	 * Returns the position where new attributes should be inserted: after the
	 * last existing attribute's closing quote, or after the tag name if no
	 * attributes exist.
	 */
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
}
