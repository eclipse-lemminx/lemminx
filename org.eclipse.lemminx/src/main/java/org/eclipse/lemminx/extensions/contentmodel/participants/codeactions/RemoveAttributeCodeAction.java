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
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Base code action to remove an undeclared attribute and position the cursor at
 * the removal location via SnippetTextEdit.
 */
public class RemoveAttributeCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int offset = document.offsetAt(diagnosticRange.getEnd());
			DOMAttr attr = document.findAttrAt(offset);
			if (attr != null) {
				int startOffset = attr.getStart();
				int endOffset = attr.getEnd();
				CharSequence text = document.getTextSequence();
				if (startOffset > 0) {
					char ch = text.charAt(startOffset - 1);
					if (ch == ' ' || ch == '\t') {
						startOffset--;
					}
				}
				Range attrRange = new Range(document.positionAt(startOffset), document.positionAt(endOffset));
				boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings()
						.isSnippetEditSupported();
				CodeAction removeAttributeAction = CodeActionFactory.replace(
						"Remove '" + attr.getName() + "' attribute",
						attrRange, snippetSupport ? "$0" : "", document.getTextDocument(), diagnostic,
						snippetSupport);
				codeActions.add(removeAttributeAction);
			}
		} catch (BadLocationException e) {
			// Do nothing
		}
	}

}
