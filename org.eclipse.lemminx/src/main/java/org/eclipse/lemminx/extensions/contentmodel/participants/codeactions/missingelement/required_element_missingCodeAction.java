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

import static org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.MissingElementDataConstants.DATA_REQUIRED_FIELD;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
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

/**
 * CodeAction to insert expected child elements within an element
 * 
 * Given this XML where the expected child elements are not present as defined
 * in the relaxNG schema:
 * 
 * <servlet></servlet> Error: Child elements are missing from element: - servlet
 * 
 * 
 * To fix the error, the code action will suggest inserting the expected
 * elements inside the parent tag
 * 
 */

public class required_element_missingCodeAction implements ICodeActionParticipant {

	private final Map<String, ICodeActionResolvesParticipant> resolveCodeActionParticipants;

	public required_element_missingCodeAction() {
		// Register available resolvers.
		resolveCodeActionParticipants = new HashMap<>();
		resolveCodeActionParticipants.put(required_element_missingCodeActionResolver.PARTICIPANT_ID,
				new required_element_missingCodeActionResolver());
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		Diagnostic diagnostic = request.getDiagnostic();
		try {
			Range diagnosticRange = diagnostic.getRange();
			int startOffset = document.offsetAt(diagnosticRange.getStart()) + 1;
			DOMNode node = document.findNodeAt(startOffset);

			if (node == null || !node.isElement()) {
				return;
			}

			DOMElement element = (DOMElement) node;
			XMLElementGenerator generator = request.getXMLGenerator();

			String insertStrRequired = null;
			String insertStrAll = null;

			if (!request.canSupportResolve()) {
				insertStrRequired = generator.generateMissingElements(request.getComponent(ContentModelManager.class),
						element, true);
				insertStrAll = generator.generateMissingElements(request.getComponent(ContentModelManager.class),
						element, false);
			}
			CodeAction insertRequriedExpectedElementCodeAction = createInsertExpectedElementsCodeAction(true,
					element, insertStrRequired, request, cancelChecker);

			CodeAction insertAllExpectedElementCodeAction = createInsertExpectedElementsCodeAction(false,
					element, insertStrAll, request, cancelChecker);

			codeActions.add(insertRequriedExpectedElementCodeAction);
			codeActions.add(insertAllExpectedElementCodeAction);

		} catch (BadLocationException e) {
			// do nothing
		}
	}

	private static CodeAction createInsertExpectedElementsCodeAction(boolean isGenerateRequired, DOMElement domElement,
			String insertStr, ICodeActionRequest request, CancelChecker cancelChecker)
			throws BadLocationException {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		String title = isGenerateRequired ? "Insert only required elements" : "Insert all expected elements";
		if (request.canSupportResolve()) {
			JsonObject data = DataEntryField.createData(document.getDocumentURI(),
					required_element_missingCodeActionResolver.PARTICIPANT_ID);
			return insertExpectedElementsUnresolvedCodeAction(title, diagnostic, data, isGenerateRequired);
		} else {
			return insertExpectedElementCodeAction(document, title, domElement, insertStr, diagnostic);
		}
	}

	private static CodeAction insertExpectedElementsUnresolvedCodeAction(String title,
			Diagnostic diagnostic, JsonObject data, boolean isGenerateRequired) {
		CodeAction codeAction = new CodeAction(title);
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		String required_field = isGenerateRequired ? "true" : "false";
		data.addProperty(DATA_REQUIRED_FIELD, required_field);
		codeAction.setData(data);

		return codeAction;
	}

	public ICodeActionResolvesParticipant getResolveCodeActionParticipant(String participantId) {
		return resolveCodeActionParticipants.get(participantId);
	}

	private static CodeAction insertExpectedElementCodeAction(DOMDocument document, String title,
			DOMElement element, String insertStr, Diagnostic diagnostic) throws BadLocationException {
		Position childElementPositionStartTag = document.positionAt(element.getStartTagCloseOffset() + 1);
		Position childElementPositionEndTag = document.positionAt(element.getEndTagOpenOffset());

		Range targetRange = new Range(childElementPositionStartTag, childElementPositionEndTag);
		return CodeActionFactory.replace(title, targetRange, insertStr, document.getTextDocument(), diagnostic);
	}
}
