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
package org.eclipse.lemminx.extensions.minify.participants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.XMLMinifier;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolverRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
 
/**
 * CodeAction resolver for the minify action.
 */
public class MinifyCodeActionResolver implements ICodeActionResolvesParticipant {

	private static final Logger LOGGER = Logger.getLogger(MinifyCodeActionResolver.class.getName());

	public static final String PARTICIPANT_ID = MinifyCodeActionResolver.class.getName();

	private final XMLMinifier minifier;

	public MinifyCodeActionResolver(XMLMinifier minifier) {
		this.minifier = minifier;
	}

	@Override
	public CodeAction resolveCodeAction(ICodeActionResolverRequest request, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		CodeAction resolved = request.getUnresolved();

		try {
			// Compute the minify edits
			List<? extends TextEdit> edits = minifier.minify(document, null, request.getSharedSettings());

			// Create workspace edit
			if (edits != null && !edits.isEmpty()) {
				// Convert to List<TextEdit>
				List<TextEdit> textEdits = new ArrayList<>(edits);
				TextDocumentEdit textDocumentEdit = CodeActionFactory.insertEdits(document.getTextDocument(),
						textEdits);
				WorkspaceEdit workspaceEdit = new WorkspaceEdit(
						Collections.singletonList(Either.forLeft(textDocumentEdit)));
				resolved.setEdit(workspaceEdit);
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while resolving minify code action", e);
		}

		return resolved;
	}
}
