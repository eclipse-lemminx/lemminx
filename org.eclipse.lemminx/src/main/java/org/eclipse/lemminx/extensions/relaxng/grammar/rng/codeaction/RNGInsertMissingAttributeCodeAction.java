/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rng.codeaction;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to insert a missing required attribute in an RNG grammar element
 * using SnippetTextEdit for cursor placement inside the attribute value.
 *
 * <p>
 * Use cases:
 * </p>
 * <ul>
 * <li>{@code missing_href_attribute}: inserts {@code href="$1"} on
 * {@code <include>} or {@code <externalRef>}.</li>
 * <li>{@code missing_name_attribute}: inserts {@code name="$1"} on
 * {@code <define>}, {@code <ref>}, etc.</li>
 * <li>{@code missing_type_attribute}: inserts {@code type="$1"} on
 * {@code <data>}.</li>
 * </ul>
 */
public class RNGInsertMissingAttributeCodeAction implements ICodeActionParticipant {

	private final String attributeName;

	public RNGInsertMissingAttributeCodeAction(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DOMNode node = document.findNodeAt(offset);
			if (node == null || !node.isElement()) {
				return;
			}
			DOMElement element = (DOMElement) node;
			if (element.hasAttribute(attributeName)) {
				return;
			}
			Position insertPosition = getInsertPosition(element, document);
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();
			String insertText = " " + attributeName + "=\""
					+ (snippetSupport ? "$1" : "") + "\"";
			CodeAction insertAction = CodeActionFactory.insert(
					"Insert '" + attributeName + "' attribute",
					insertPosition, insertText, document.getTextDocument(), diagnostic,
					snippetSupport);
			codeActions.add(insertAction);
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

	private static Position getInsertPosition(DOMElement element, DOMDocument document)
			throws BadLocationException {
		if (element.hasAttributes()) {
			DOMAttr lastAttr = null;
			for (DOMAttr attr : element.attributes()) {
				lastAttr = attr;
			}
			if (lastAttr != null) {
				return document.positionAt(lastAttr.getEnd());
			}
		}
		String tagName = element.getTagName();
		if (tagName != null) {
			return document.positionAt(element.getStart() + 1 + tagName.length());
		}
		return document.positionAt(element.getStart());
	}
}
