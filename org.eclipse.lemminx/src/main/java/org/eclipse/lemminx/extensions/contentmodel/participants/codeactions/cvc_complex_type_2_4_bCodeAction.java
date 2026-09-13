/**
 * Copyright (c) 2022 Red Hat Inc. and others.
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

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * CodeAction to insert expected child elements within an element
 * 
 * Given this XML where the expected child elements are not present as defined
 * in the XSD:
 * 
 * <servlet></servlet> Error: Child elements are missing from element: - servlet
 *
 * The following elements are expected: - description - display-name - icon -
 * servlet-name
 * 
 * To fix the error, the code action will suggest inserting the expected
 * elements inside the parent tag
 * 
 */

public class cvc_complex_type_2_4_bCodeAction implements ICodeActionParticipant {

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
			Position childElementPositionStartTag = document.positionAt(element.getStartTagCloseOffset() + 1);
			Position childElementPositionEndTag = document.positionAt(element.getEndTagOpenOffset());

			Range targetRange = new Range(childElementPositionStartTag, childElementPositionEndTag);
			XMLElementGenerator generator = request.getXMLGenerator();

			String insertStrAll = generator.generateMissingElements(request.getComponent(ContentModelManager.class), element, false);
			String insertStrRequired = generator.generateMissingElements(request.getComponent(ContentModelManager.class), element, true);

			CodeAction insertAllExpectedElement = CodeActionFactory.replace("Insert all expected elements",
					targetRange, insertStrAll, document.getTextDocument(), diagnostic);

			codeActions.add(insertAllExpectedElement);

			CodeAction insertRequriedExpectedElement = CodeActionFactory.replace("Insert only required elements",
					targetRange, insertStrRequired, document.getTextDocument(), diagnostic);

			codeActions.add(insertRequriedExpectedElement);

		} catch (BadLocationException e) {
			// do nothing
		}
	}
}
