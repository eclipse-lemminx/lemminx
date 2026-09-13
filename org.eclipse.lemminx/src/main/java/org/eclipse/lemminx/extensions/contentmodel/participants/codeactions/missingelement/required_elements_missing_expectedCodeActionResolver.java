/**
 * Copyright (c) 2023 Red Hat Inc. and others.
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
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement;

import static org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.MissingElementDataConstants.DATA_ELEMENT_FIELD;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolverRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.thaiopensource.relaxng.pattern.CMRelaxNGElementDeclaration;

/**
 * CodeAction resolver used to generate missing element of choice
 * 
 */

public class required_elements_missing_expectedCodeActionResolver implements ICodeActionResolvesParticipant {

	public static final String PARTICIPANT_ID = required_elements_missing_expectedCodeActionResolver.class
			.getName();

	@Override
	public CodeAction resolveCodeAction(ICodeActionResolverRequest request, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		Diagnostic diagnostic = request.getUnresolved().getDiagnostics().get(0);
		CodeAction resolved = request.getUnresolved();

		try {
			Range diagnosticRange = diagnostic.getRange();
			int startOffset = document.offsetAt(diagnosticRange.getStart()) + 1;
			DOMNode node = document.findNodeAt(startOffset);

			if (node == null || !node.isElement()) {
				return null;
			}

			DOMElement element = (DOMElement) node;
			Position childElementPositionStartTag = document.positionAt(element.getStartTagCloseOffset() + 1);
			Position childElementPositionEndTag = document.positionAt(element.getEndTagOpenOffset());

			Range targetRange = new Range(childElementPositionStartTag, childElementPositionEndTag);
			XMLElementGenerator generator = request.getXMLGenerator(targetRange);

			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);

			for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
				CMElementDeclaration matchesCMElement = cmDocument.findCMElement(element);
				if (matchesCMElement != null) {
					// Add a code action option for each element choice
					for (CMElementDeclaration child : ((CMRelaxNGElementDeclaration) matchesCMElement)
							.getPossibleRequiredElements()) {
						if (child.getLocalName().equals(request.getDataProperty(DATA_ELEMENT_FIELD))) {
							StringBuilder generatedXml = new StringBuilder();
							generatedXml
									.append(generator.generate(child, null, true, false, 1, true));
							String replaceText = generatedXml.toString();
							resolved.setEdit(CodeActionFactory.getReplaceWorkspaceEdit(replaceText, targetRange,
									document.getTextDocument()));
						}
					}
				}
			}
		} catch (BadLocationException e) {
			// do nothing
		}
		return resolved;
	}
}
