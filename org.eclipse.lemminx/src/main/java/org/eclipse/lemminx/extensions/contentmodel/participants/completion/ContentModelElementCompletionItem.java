/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.completion;

import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.services.extensions.completion.AbstractElementCompletionItem;
import org.eclipse.lemminx.services.extensions.completion.ICompletionRequest;
import org.eclipse.lsp4j.MarkupContent;

/**
 * Element completion item created from a content model (schema/grammar element
 * declaration).
 *
 */
public class ContentModelElementCompletionItem
		extends AbstractElementCompletionItem<CMElementDeclaration, XMLElementGenerator> {

	public ContentModelElementCompletionItem(String tagName, CMElementDeclaration elementDeclaration,
			XMLElementGenerator generator, ICompletionRequest request) {
		super(tagName, elementDeclaration, generator, request);
	}

	@Override
	protected MarkupContent generateDocumentation() {
		CMElementDeclaration elementDeclaration = getSourceElement();
		ICompletionRequest request = getRequest();
		return XMLElementGenerator.createMarkupContent(elementDeclaration, request);
	}

	@Override
	protected String generateFullElementContent(boolean generateEndTag) {
		CMElementDeclaration elementDeclaration = getSourceElement();
		String tagName = getTagName();
		String prefix = null;
		int index = tagName.indexOf(':');
		if (index != -1) {
			prefix = tagName.substring(0, index);
		}
		XMLElementGenerator generator = getGenerator();
		return generator.generate(elementDeclaration, prefix, generateEndTag);
	}
}
