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
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypesDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNamespaceDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code unclosed-block} error — inserts a closing
 * {@code &#125;} brace.
 *
 * <p>The insertion position is determined by examining the unclosed node's
 * children: if a top-level construct (define, start, namespace, datatypes)
 * is incorrectly nested inside the unclosed block, the closing brace is
 * inserted before it. Otherwise, it is appended at the end of the document.</p>
 *
 * <p>Use case: the user writes {@code element foo &#123;} and forgets to close
 * it before writing the next define. The code action inserts {@code &#125;}
 * at the correct position, separating the two definitions.</p>
 */
public class unclosed_blockCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Inserts a closing {@code &#125;} brace at the inferred position — before
	 * the next top-level construct, or at the end of the document.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		TextDocument textDocument = rncDocument.getTextDocument();
		CharSequence text = textDocument.getTextSequence();
		int len = text.length();

		try {
			String lineSeparator = textDocument.lineDelimiter(0);
			int diagnosticOffset = textDocument.offsetAt(diagnostic.getRange().getStart());
			RNCNode unclosedNode = rncDocument.findNodeAt(diagnosticOffset);

			int insertOffset = -1;
			for (RNCNode child : unclosedNode.getChildren()) {
				if (child instanceof RNCDefine || child instanceof RNCStart
						|| child instanceof RNCNamespaceDecl || child instanceof RNCDatatypesDecl) {
					insertOffset = child.getStart();
					break;
				}
			}

			if (insertOffset != -1) {
				Position insertPos = textDocument.positionAt(insertOffset);
				String insertText = "}" + lineSeparator;
				codeActions.add(CodeActionFactory.insert("Add closing '}'", insertPos, insertText, document,
						diagnostic, snippetSupported));
			} else {
				boolean endsWithNewline = len > 0 && (text.charAt(len - 1) == '\n');
				String insertText = endsWithNewline ? "}" : lineSeparator + "}";
				Position insertPos = textDocument.positionAt(len);
				codeActions.add(CodeActionFactory.insert("Add closing '}'", insertPos, insertText, document,
						diagnostic, snippetSupported));
			}
		} catch (BadLocationException e) {
			// ignore
		}
	}
}
