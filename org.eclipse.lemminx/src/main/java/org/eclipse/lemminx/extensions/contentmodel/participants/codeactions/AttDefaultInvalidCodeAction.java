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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DTDAttlistDecl;
import org.eclipse.lemminx.dom.DTDDeclParameter;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action to fix MSG_ATT_DEFAULT_INVALID error by replacing the invalid
 * default value with a valid enumeration value.
 *
 * {@code <!ATTLIST root status (one|two|three) "four">} → replaces "four" with
 * "one", "two" or "three"
 */
public class AttDefaultInvalidCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();

		try {
			int offset = document.offsetAt(diagnosticRange.getStart());
			DTDAttlistDecl attlist = findAttlistDeclContaining(document, offset);
			if (attlist == null) {
				return;
			}

			List<String> enumValues = parseEnumValues(attlist.attributeType,
					document.getTextSequence());
			if (enumValues.isEmpty()) {
				return;
			}

			for (String value : enumValues) {
				String replaceText = snippetSupport
						? "\"" + value + "\"$0"
						: "\"" + value + "\"";
				CodeAction action = CodeActionFactory.replace(
						"Replace with '\"" + value + "\"'", diagnosticRange, replaceText,
						document.getTextDocument(), diagnostic, snippetSupport);
				codeActions.add(action);
			}
		} catch (BadLocationException e) {
			// do nothing
		}
	}

	private DTDAttlistDecl findAttlistDeclContaining(DOMDocument document, int offset) {
		DOMDocumentType docType = document.getDoctype();
		if (docType == null) {
			return null;
		}
		for (DOMNode child : docType.getChildren()) {
			if (child instanceof DTDAttlistDecl) {
				DTDAttlistDecl attlist = (DTDAttlistDecl) child;
				if (offset >= attlist.getStart() && offset <= attlist.getEnd()) {
					List<DTDAttlistDecl> internalChildren = attlist.getInternalChildren();
					if (internalChildren != null) {
						for (DTDAttlistDecl innerDecl : internalChildren) {
							if (offset >= innerDecl.getStart() && offset <= innerDecl.getEnd()) {
								return innerDecl;
							}
						}
					}
					return attlist;
				}
			}
		}
		return null;
	}

	/**
	 * Parses enumeration values from an ATTLIST attribute type parameter by
	 * scanning the document text directly.
	 *
	 * <p>
	 * {@code (one|two|three)} → {@code ["one", "two", "three"]}
	 * </p>
	 */
	private List<String> parseEnumValues(DTDDeclParameter typeParam, CharSequence text) {
		List<String> values = new ArrayList<>();
		if (typeParam == null) {
			return values;
		}
		int start = typeParam.getStart();
		int end = typeParam.getEnd();
		if (end - start < 2 || text.charAt(start) != '(' || text.charAt(end - 1) != ')') {
			return values;
		}
		int wordStart = start + 1;
		for (int i = start + 1; i < end; i++) {
			char c = text.charAt(i);
			if (c == '|' || c == ')') {
				if (i > wordStart) {
					values.add(text.subSequence(wordStart, i).toString().trim());
				}
				wordStart = i + 1;
			}
		}
		return values;
	}
}
