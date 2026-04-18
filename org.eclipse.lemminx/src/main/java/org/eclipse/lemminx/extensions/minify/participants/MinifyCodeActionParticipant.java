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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.XMLMinifier;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionParticipant;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionRequest;
import org.eclipse.lemminx.services.extensions.codeaction.ICodeActionResolvesParticipant;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

import com.google.gson.JsonObject;

/**
 * Code action participant to provide minify as a source action.
 */
public class MinifyCodeActionParticipant implements ICodeActionParticipant {

	private static final String SOURCE_MINIFY_KIND = CodeActionKind.Source + ".minify";

	private final Map<String, ICodeActionResolvesParticipant> resolveCodeActionParticipants;
	private final XMLMinifier minifier;

	public MinifyCodeActionParticipant() {
		this.minifier = new XMLMinifier();
		this.resolveCodeActionParticipants = new HashMap<>();
		this.resolveCodeActionParticipants.put(MinifyCodeActionResolver.PARTICIPANT_ID,
				new MinifyCodeActionResolver(minifier));
	}

	@Override
	public void doCodeActionUnconditional(ICodeActionRequest request, List<CodeAction> codeActions,
			CancelChecker cancelChecker) throws CancellationException {

		// Only support clients that can resolve code actions
		// The edits will be computed only when the user clicks on the action
		if (!request.canSupportResolve()) {
			return;
		}

		DOMDocument document = request.getDocument();

		CodeAction minifyAction = new CodeAction("Minify XML");
		minifyAction.setKind(SOURCE_MINIFY_KIND);

		JsonObject data = DataEntryField.createData(document.getDocumentURI(),
				MinifyCodeActionResolver.PARTICIPANT_ID);
		minifyAction.setData(data);

		codeActions.add(minifyAction);
	}

	@Override
	public ICodeActionResolvesParticipant getResolveCodeActionParticipant(String participantId) {
		return resolveCodeActionParticipants.get(participantId);
	}
}
