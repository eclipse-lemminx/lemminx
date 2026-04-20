/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Adds a space at the end of the diagnostic range
 *
 */
public class FixMissingSpaceCodeAction implements ICodeActionParticipant {

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		try {
			int startOffset = document.offsetAt(diagnosticRange.getStart());
			int endOffset = document.offsetAt(diagnosticRange.getEnd());
			CharSequence text = document.getTextSequence();
			String value = text.subSequence(startOffset, endOffset).toString();
			codeActions.add(CodeActionFactory.insert("Add space after '" + value + "'", diagnosticRange.getEnd(), " ",
					document.getTextDocument(), diagnostic));
		} catch (BadLocationException | IndexOutOfBoundsException e) {
			codeActions.add(CodeActionFactory.insert("Add space", diagnosticRange.getEnd(), " ",
					document.getTextDocument(), diagnostic));
		}
	}

}
