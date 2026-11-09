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
 * Code action to insert external ID literal(s) after SYSTEM or PUBLIC keyword.
 *
 * <ul>
 * <li>{@code <!NOTATION name SYSTEM>} → inserts {@code "systemId"}</li>
 * <li>{@code <!NOTATION name PUBLIC>} → inserts {@code "publicId"}</li>
 * </ul>
 */
public class ExternalIdCodeAction implements ICodeActionParticipant {

	public enum Kind {
		SYSTEM, PUBLIC
	}

	private final Kind kind;

	public ExternalIdCodeAction(Kind kind) {
		this.kind = kind;
	}

	@Override
	public void doCodeAction(ICodeActionRequest request, List<CodeAction> codeActions, CancelChecker cancelChecker) {
		Diagnostic diagnostic = request.getDiagnostic();
		DOMDocument document = request.getDocument();
		Range diagnosticRange = diagnostic.getRange();
		boolean snippetSupport = request.getSharedSettings().getWorkspaceSettings().isSnippetEditSupported();
		Position insertPos = diagnosticRange.getEnd();

		switch (kind) {
		case SYSTEM: {
			// <!NOTATION name SYSTEM> → insert ' "systemId"'
			String insertText = snippetSupport
					? " \"${1:systemId}\"$0"
					: " \"systemId\"";
			codeActions.add(CodeActionFactory.insert(
					"Insert SYSTEM literal", insertPos, insertText,
					document.getTextDocument(), diagnostic, snippetSupport));
			break;
		}
		case PUBLIC: {
			// <!NOTATION name PUBLIC> → insert ' "publicId"'
			String insertText = snippetSupport
					? " \"${1:publicId}\"$0"
					: " \"publicId\"";
			codeActions.add(CodeActionFactory.insert(
					"Insert PUBLIC literal", insertPos, insertText,
					document.getTextDocument(), diagnostic, snippetSupport));
			break;
		}
		}
	}
}
