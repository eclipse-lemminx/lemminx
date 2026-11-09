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

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for MSG_MARKUP_NOT_RECOGNIZED_IN_DTD.
 *
 * <p>
 * Removes invalid content inside a DTD internal subset.
 * </p>
 *
 * <pre>
 * &lt;!DOCTYPE root [
 *     &lt;!ELEMENT root EMPTY&gt;
 *     Bad Value                ← "Remove the invalid content"
 *     &lt;!ATTLIST root attr CDATA #IMPLIED&gt;
 * ]&gt;
 * </pre>
 */
public class MSG_MARKUP_NOT_RECOGNIZED_IN_DTDCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();

		int startLine = diagnosticRange.getStart().getLine();
		int endLine = diagnosticRange.getEnd().getLine();
		Range removeRange;
		if (startLine == endLine) {
			removeRange = new Range(new Position(startLine, 0), new Position(startLine + 1, 0));
		} else {
			removeRange = diagnosticRange;
		}

		CodeAction action = CodeActionFactory.remove(
				"Remove the invalid content", removeRange,
				document.getTextDocument(), diagnostic);
		codeActions.add(action);
	}
}
