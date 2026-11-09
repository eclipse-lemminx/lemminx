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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.contentmodel.model.CMAttributeDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.codeaction.InsertAttributeCodeAction;

/**
 * Code action to fix required_attribute_missing,
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
public class required_attribute_missingCodeAction extends InsertAttributeCodeAction {

	public required_attribute_missingCodeAction() {
		super();
	}

	@Override
	protected String getCodeActionTitle(Collection<CMAttributeDeclaration> attributes) {
		return "Insert required attributes";
	}

	@Override
	protected Collection<CMAttributeDeclaration> getAttributesToInsert(DOMElement element,
			ICodeActionRequest request) {
		ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);
		for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
			CMElementDeclaration elementDeclaration = cmDocument.findCMElement(element);
			if (elementDeclaration != null) {
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

				if (!requiredAttributes.isEmpty()) {
					return requiredAttributes;
				}
			}
		}
		return Collections.emptyList();
	}
}
