/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DTDEntityDecl;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for MSG_NOTATION_NOT_DECLARED_FOR_UNPARSED_ENTITYDECL.
 *
 * <p>
 * {@code <!ENTITY logo SYSTEM "logo.gif" NDATA gif>} where notation {@code gif}
 * is not declared → inserts {@code <!NOTATION gif SYSTEM "">} before the entity.
 * </p>
 */
public class NotationNotDeclaredForEntityCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DTDEntityDecl entity = findEntityDeclContaining(document, offset);
			if (entity == null) {
				return;
			}

			String notationName = findNdataNotationName(entity, document);
			if (notationName == null) {
				return;
			}

			int entityLine = diagnosticRange.getStart().getLine();
			String lineText = document.lineText(entityLine);
			String indent = extractLeadingWhitespace(lineText);
			String delimiter = document.lineDelimiter(entityLine);
			boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
					.isSnippetEditSupported();

			String insertText;
			if (snippetSupport) {
				insertText = indent + "<!NOTATION " + notationName + " SYSTEM \"$1\">$0" + delimiter;
			} else {
				insertText = indent + "<!NOTATION " + notationName + " SYSTEM \"\">" + delimiter;
			}
			Position insertPos = new Position(entityLine, 0);

			CodeAction action = CodeActionFactory.insert(
					"Declare notation '" + notationName + "'", insertPos, insertText,
					document.getTextDocument(), diagnostic, snippetSupport);
			codeActions.add(action);
		} catch (BadLocationException e) {
			// do nothing
		}
	}

	private DTDEntityDecl findEntityDeclContaining(DOMDocument document, int offset) {
		DOMDocumentType docType = document.getDoctype();
		if (docType == null) {
			return null;
		}
		for (DOMNode child : docType.getChildren()) {
			if (child instanceof DTDEntityDecl && offset >= child.getStart() && offset <= child.getEnd()) {
				return (DTDEntityDecl) child;
			}
		}
		return null;
	}

	/**
	 * Scans the entity declaration text for {@code NDATA notationName} and returns
	 * the notation name.
	 */
	private String findNdataNotationName(DTDEntityDecl entity, DOMDocument document) {
		CharSequence text = document.getTextSequence();
		int start = entity.getStart();
		int end = entity.getEnd();
		for (int i = start; i < end - 5; i++) {
			if (text.charAt(i) == 'N' && text.charAt(i + 1) == 'D' && text.charAt(i + 2) == 'A'
					&& text.charAt(i + 3) == 'T' && text.charAt(i + 4) == 'A') {
				int nameStart = i + 5;
				while (nameStart < end && Character.isWhitespace(text.charAt(nameStart))) {
					nameStart++;
				}
				int nameEnd = nameStart;
				while (nameEnd < end && !Character.isWhitespace(text.charAt(nameEnd)) && text.charAt(nameEnd) != '>') {
					nameEnd++;
				}
				if (nameEnd > nameStart) {
					return text.subSequence(nameStart, nameEnd).toString();
				}
			}
		}
		return null;
	}

	private static String extractLeadingWhitespace(String line) {
		int i = 0;
		while (i < line.length() && Character.isWhitespace(line.charAt(i)) && line.charAt(i) != '\n'
				&& line.charAt(i) != '\r') {
			i++;
		}
		return line.substring(0, i);
	}
}
