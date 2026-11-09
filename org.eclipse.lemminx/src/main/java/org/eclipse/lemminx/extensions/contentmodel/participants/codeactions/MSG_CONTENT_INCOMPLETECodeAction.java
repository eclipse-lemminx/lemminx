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

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLElementGenerator;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix DTD MSG_CONTENT_INCOMPLETE error by inserting expected
 * child elements.
 */
public class MSG_CONTENT_INCOMPLETECodeAction implements ICodeActionParticipant {

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

			if (!element.isEmpty()) {
				new MSG_CONTENT_INVALIDCodeAction().doCodeAction(request, codeActions, cancelChecker);
				return;
			}

			XMLElementGenerator generator = request.getXMLGenerator();
			String insertStrAll = generator.generateMissingElements(request.getComponent(ContentModelManager.class),
					element, false);
			String insertStrRequired = generator.generateMissingElements(
					request.getComponent(ContentModelManager.class), element, true);

			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();

			Range targetRange;
			if (element.isSelfClosed()) {
				int replaceStart = element.getEnd() - 2;
				String text = document.getText();
				if (replaceStart > 0 && text.charAt(replaceStart - 1) == ' ') {
					replaceStart--;
				}
				targetRange = new Range(document.positionAt(replaceStart), document.positionAt(element.getEnd()));
				String endTag = "</" + element.getTagName() + ">";
				insertStrAll = ">" + insertStrAll + endTag;
				insertStrRequired = ">" + insertStrRequired + endTag;
			} else {
				Position childElementPositionStartTag = document.positionAt(element.getStartTagCloseOffset() + 1);
				Position childElementPositionEndTag = document.positionAt(element.getEndTagOpenOffset());
				targetRange = new Range(childElementPositionStartTag, childElementPositionEndTag);
			}

			codeActions.add(CodeActionFactory.replace("Insert all expected elements",
					targetRange, insertStrAll, document.getTextDocument(), diagnostic, snippetSupport));

			codeActions.add(CodeActionFactory.replace("Insert only required elements",
					targetRange, insertStrRequired, document.getTextDocument(), diagnostic, snippetSupport));

		} catch (BadLocationException e) {
			// do nothing
		}
	}
}
