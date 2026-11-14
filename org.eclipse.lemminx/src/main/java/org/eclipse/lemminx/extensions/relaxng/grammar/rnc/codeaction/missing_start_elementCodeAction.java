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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction;

import java.util.List;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code missing-start-element} error.
 *
 * <p>When a grammar has defines but no {@code start = ...} declaration,
 * this code action offers to insert {@code start = <defineName>} for each
 * existing define, at the beginning of the document (before the first define).</p>
 *
 * <p>Use case: the user writes:</p>
 * <pre>
 * root = element root { text }
 * </pre>
 * <p>Jing reports "missing start element". The code action offers
 * "Add 'start = root'" which inserts {@code start = root} before the define.</p>
 */
public class missing_start_elementCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Generates one "Add 'start = ...'" code action per existing define,
	 * inserting the start declaration before the first define.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		if (rncDocument == null) {
			return;
		}
		String lineSeparator = RNCPositionUtils.getLineSeparator(rncDocument.getTextDocument());
		List<RNCDefine> defines = rncDocument.getDefines();
		for (RNCDefine define : defines) {
			String defineName = define.getName();
			if (defineName != null) {
				String insertText = "start = " + defineName + lineSeparator + lineSeparator;
				Position insertPos = new Position(diagnostic.getRange().getStart().getLine(), 0);
				codeActions.add(CodeActionFactory.insert(
						"Add 'start = " + defineName + "'",
						insertPos, insertText, document, diagnostic));
			}
		}
	}
}
