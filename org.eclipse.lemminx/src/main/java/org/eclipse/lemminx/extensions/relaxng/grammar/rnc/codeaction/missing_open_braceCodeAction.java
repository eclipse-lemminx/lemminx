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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code missing-open-brace} error — inserts
 * {@code &#123;} after an element or attribute name that is missing its
 * body block.
 *
 * <p>If the remaining text after the name already contains an unmatched
 * {@code &#125;}, only the opening brace is inserted. Otherwise both
 * braces are inserted.</p>
 */
public class missing_open_braceCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Inserts an opening {@code &#123;} (and optionally closing {@code &#125;})
	 * brace after the element or attribute name.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		Position insertPos = diagnostic.getRange().getEnd();
		String text = computeInsertText(rncDocument, insertPos);
		codeActions.add(CodeActionFactory.insert("Add '{ }'", insertPos, text, document, diagnostic, snippetSupported));
	}

	private static String computeInsertText(RNCDocument rncDocument, Position pos) {
		try {
			CharSequence source = rncDocument.getSource();
			int offset = rncDocument.getTextDocument().offsetAt(pos);
			if (!hasUnmatchedClosingBrace(source, offset)) {
				return " { }";
			}
			boolean hasSpaceAfter = offset < source.length() && source.charAt(offset) == ' ';
			return hasSpaceAfter ? " {" : " { ";
		} catch (BadLocationException e) {
			return " { }";
		}
	}

	private static boolean hasUnmatchedClosingBrace(CharSequence source, int offset) {
		int depth = 0;
		for (int i = offset; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '{') {
				depth++;
			} else if (ch == '}') {
				if (depth == 0) {
					return true;
				}
				depth--;
			}
		}
		return false;
	}
}
