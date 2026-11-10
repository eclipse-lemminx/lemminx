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
import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code missing-pattern} error — offers quick fixes to
 * fill an empty block with a pattern.
 *
 * <p>Generates code actions to insert common patterns inside an empty
 * {@code &#123; &#125;} block:</p>
 * <ul>
 *   <li>{@code text} — text content</li>
 *   <li>{@code empty} — empty content</li>
 *   <li>{@code element name &#123; &#125;} — nested element</li>
 *   <li>{@code attribute name &#123; text &#125;} — nested attribute</li>
 * </ul>
 *
 * <p>Use case: the user writes {@code element root &#123; &#125;} and sees
 * a "missing-pattern" warning. The code action offers to fill it with
 * {@code text} or a nested element/attribute.</p>
 */
public class missing_patternCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Generates code actions to fill an empty block with common patterns
	 * ({@code text}, {@code empty}, nested element, nested attribute).
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		TextDocument textDocument = rncDocument.getTextDocument();
		Range diagRange = diagnostic.getRange();

		try {
			int diagEnd = textDocument.offsetAt(diagRange.getEnd());
			CharSequence text = textDocument.getTextSequence();

			int braceOpen = findBraceOpen(text, diagEnd);
			if (braceOpen == -1) {
				return;
			}
			int braceClose = findBraceClose(text, braceOpen);

			Position replaceStart = textDocument.positionAt(braceOpen + 1);
			Position replaceEnd = textDocument.positionAt(braceClose);
			Range replaceRange = new Range(replaceStart, replaceEnd);

			String baseIndent = getLineIndent(text, braceOpen);
			String childIndent = baseIndent + "  ";
			String lineSeparator = textDocument.lineDelimiter(0);

			// Code action 1: Insert 'text'
			codeActions.add(CodeActionFactory.replace("Insert 'text'", replaceRange, " text ",
					document, diagnostic, snippetSupported));

			// Code action 2: Insert 'empty'
			codeActions.add(CodeActionFactory.replace("Insert 'empty'", replaceRange, " empty ",
					document, diagnostic, snippetSupported));

			// Code action 3: Insert element
			StringBuilder sb = new StringBuilder();
			sb.append(lineSeparator).append(childIndent);
			if (snippetSupported) {
				sb.append("element ");
				SnippetsBuilder.placeholders(1, "name", sb);
				sb.append(" {").append(lineSeparator).append(childIndent).append("  ");
				SnippetsBuilder.tabstops(0, sb);
				sb.append(lineSeparator).append(childIndent).append("}");
			} else {
				sb.append("element name { }");
			}
			sb.append(lineSeparator).append(baseIndent);
			codeActions.add(CodeActionFactory.replace("Insert element", replaceRange, sb.toString(),
					document, diagnostic, snippetSupported));

			// Code action 4: Insert attribute
			sb.setLength(0);
			sb.append(lineSeparator).append(childIndent);
			if (snippetSupported) {
				sb.append("attribute ");
				SnippetsBuilder.placeholders(1, "name", sb);
				sb.append(" { ");
				SnippetsBuilder.placeholders(2, "text", sb);
				sb.append(" }");
			} else {
				sb.append("attribute name { text }");
			}
			sb.append(lineSeparator).append(baseIndent);
			codeActions.add(CodeActionFactory.replace("Insert attribute", replaceRange, sb.toString(),
					document, diagnostic, snippetSupported));
		} catch (BadLocationException e) {
			// ignore
		}
	}

	private static int findBraceOpen(CharSequence text, int from) {
		int pos = from;
		while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
			pos++;
		}
		if (pos < text.length() && text.charAt(pos) == '{') {
			return pos;
		}
		while (pos < text.length()) {
			if (text.charAt(pos) == '{') {
				return pos;
			}
			pos++;
		}
		return -1;
	}

	private static int findBraceClose(CharSequence text, int braceOpen) {
		int pos = braceOpen + 1;
		while (pos < text.length() && text.charAt(pos) != '}') {
			pos++;
		}
		return pos;
	}

	private static String getLineIndent(CharSequence text, int offset) {
		int lineStart = offset;
		while (lineStart > 0 && text.charAt(lineStart - 1) != '\n' && text.charAt(lineStart - 1) != '\r') {
			lineStart--;
		}
		StringBuilder indent = new StringBuilder();
		for (int i = lineStart; i < offset; i++) {
			char ch = text.charAt(i);
			if (ch == ' ' || ch == '\t') {
				indent.append(ch);
			} else {
				break;
			}
		}
		return indent.toString();
	}
}
