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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.CMElementDeclaration;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonObject;
import com.thaiopensource.relaxng.pattern.CMRelaxNGElementDeclaration;

/**
 * CodeAction to insert expected child element given a list of choices
 * 
 * Given this XML where the expected child elements are not present as defined
 * in the relaxNG schema:
 * 
 * <article></article> Error: element "article" incomplete; expected element
 * "title", "title2" or "titleOtherChoice"
 * 
 * To fix the error, there will three code actions generated to insert each
 * option
 * 
 */

public class required_elements_missing_expectedCodeAction implements ICodeActionParticipant {

	private final Map<String, ICodeActionResolvesParticipant> resolveCodeActionParticipants;

	public required_elements_missing_expectedCodeAction() {
		// Register available resolvers.
		resolveCodeActionParticipants = new HashMap<>();
		resolveCodeActionParticipants.put(required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
				new required_elements_missing_expectedCodeActionResolver());
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		Diagnostic diagnostic = request.getDiagnostic();
		try {
			Range diagnosticRange = diagnostic.getRange();
			int startOffset = document.offsetAt(diagnosticRange.getStart()) + 1;
			DOMNode node = document.findNodeAt(startOffset);
			String insertStr = null;

			if (node == null || !node.isElement()) {
				return;
			}

			DOMElement element = (DOMElement) node;
			XMLElementGenerator generator = request.getXMLGenerator();
			ContentModelManager contentModelManager = request.getComponent(ContentModelManager.class);

			for (CMDocument cmDocument : contentModelManager.findCMDocument(element)) {
				CMElementDeclaration matchesCMElement = cmDocument.findCMElement(element);
				if (matchesCMElement != null) {
					// Add a code action option for each element choice
					for (CMElementDeclaration child : (((CMRelaxNGElementDeclaration) matchesCMElement).getPossibleRequiredElements())) {
						if (!request.canSupportResolve()) {
							StringBuilder generatedXml = new StringBuilder();
							generatedXml
									.append(generator.generate(child, null, true, false, 1, true));
							insertStr = generatedXml.toString();
						}
						CodeAction insertElementCodeAction = createInsertMissingElementCodeAction(child, element,
								insertStr, request, cancelChecker);
						codeActions
								.add(insertElementCodeAction);
					}
				}
			}
		} catch (BadLocationException e) {
			// do nothing
		}
	}

	private static CodeAction createInsertMissingElementCodeAction(CMElementDeclaration element, DOMElement domElement,
			String insertStr, ICodeActionRequest request, CancelChecker cancelChecker)
			throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = "Insert expected element '" + element.getLocalName() + "'";
		if (request.canSupportResolve()) {
			JsonObject data = DataEntryField.createData(document.getDocumentURI(),
					required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID);
			return insertMissingExpectedElementUnresolvedCodeAction(title, diagnostic, data, element.getLocalName());
		} else {
			return insertMissingExpectedElementCodeAction(document, title, domElement, insertStr, diagnostic);
		}
	}

	private static CodeAction insertMissingExpectedElementUnresolvedCodeAction(String title,
			Diagnostic diagnostic, JsonObject data, String elementName) {
		CodeAction codeAction = new CodeAction(title);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		data.addProperty(DATA_ELEMENT_FIELD, elementName);
		codeAction.setData(data);

		return codeAction;
	}

	public ICodeActionResolvesParticipant getResolveCodeActionParticipant(String participantId) {
		return resolveCodeActionParticipants.get(participantId);
	}

	private static CodeAction insertMissingExpectedElementCodeAction(DOMDocument document, String title,
			DOMElement element, String insertStr, Diagnostic diagnostic) throws BadLocationException {
		Position childElementPositionStartTag = document.positionAt(element.getStartTagCloseOffset() + 1);
		Position childElementPositionEndTag = document.positionAt(element.getEndTagOpenOffset());

		Range targetRange = new Range(childElementPositionStartTag, childElementPositionEndTag);
		return CodeActionFactory.replace(title, targetRange, insertStr, document.getTextDocument(), diagnostic);
	}
}
