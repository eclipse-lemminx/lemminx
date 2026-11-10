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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.folding;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAttribute;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDatatypesDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDiv;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCElement;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNamespaceDecl;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeKind;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides folding ranges for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Folding ranges are computed from the AST ({@link RNCDocument}). Each
 * top-level declaration (define, start, include, div, element, attribute)
 * that spans more than one line produces a {@link FoldingRangeKind#Region}
 * fold. The range of a define/start extends to just before the next
 * declaration (same approach as document symbols).</p>
 *
 * <p>Three kinds of folding ranges are computed:</p>
 * <ul>
 *   <li><strong>Region (declaration)</strong> — any top-level declaration
 *       that spans more than one line.</li>
 *   <li><strong>Region (declaration group)</strong> — consecutive
 *       namespace/datatypes declarations grouped together.</li>
 *   <li><strong>Comment</strong> — consecutive runs of single-line comments
 *       ({@code #} or {@code ##}) that span more than one line.</li>
 * </ul>
 */
public class RNCFoldings {

	/**
	 * Computes folding ranges for the given RNC document.
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort the computation if the document changes.
	 * @return a list of {@link FoldingRange} objects, possibly empty.
	 */
	public List<FoldingRange> getFoldingRanges(RNCDocument rncDocument, CancelChecker cancelChecker) {
		List<FoldingRange> ranges = new ArrayList<>();
		TextDocument textDocument = rncDocument.getTextDocument();
		CharSequence text = textDocument.getTextSequence();

		List<RNCNode> declarations = collectDeclarations(rncDocument);

		int declGroupStartLine = -1;
		int declGroupEndLine = -1;

		for (int i = 0; i < declarations.size(); i++) {
			cancelChecker.checkCanceled();
			RNCNode decl = declarations.get(i);

			if (decl instanceof RNCNamespaceDecl || decl instanceof RNCDatatypesDecl) {
				try {
					int line = textDocument.positionAt(decl.getStart()).getLine();
					if (declGroupStartLine == -1) {
						declGroupStartLine = line;
					}
					declGroupEndLine = line;
				} catch (BadLocationException e) {
					// ignore
				}
				continue;
			}

			flushDeclGroup(declGroupStartLine, declGroupEndLine, ranges);
			declGroupStartLine = -1;
			declGroupEndLine = -1;

			int consumed = 0;
			if (decl instanceof RNCDefine || decl instanceof RNCStart) {
				int j = i + 1;
				while (j < declarations.size()
						&& (declarations.get(j) instanceof RNCElement
								|| declarations.get(j) instanceof RNCAttribute)) {
					j++;
				}
				consumed = j - i - 1;
			}

			int rangeEnd;
			int afterIndex = i + 1 + consumed;
			if (afterIndex < declarations.size()) {
				rangeEnd = trimTrailingWhitespace(text, declarations.get(afterIndex).getStart());
			} else {
				rangeEnd = trimTrailingWhitespace(text, text.length());
			}

			try {
				int startLine = textDocument.positionAt(decl.getStart()).getLine();
				int endLine = textDocument.positionAt(Math.max(0, rangeEnd - 1)).getLine();
				if (endLine > startLine) {
					FoldingRange range = new FoldingRange(startLine, endLine);
					range.setKind(FoldingRangeKind.Region);
					ranges.add(range);
				}
			} catch (BadLocationException e) {
				// ignore
			}

			for (int j = i; j <= i + consumed; j++) {
				collectNestedFoldingRanges(declarations.get(j), textDocument, ranges);
			}
			i += consumed;
		}

		flushDeclGroup(declGroupStartLine, declGroupEndLine, ranges);

		addCommentFoldingRanges(text, ranges, cancelChecker);

		return ranges;
	}

	private static void flushDeclGroup(int startLine, int endLine, List<FoldingRange> ranges) {
		if (startLine != -1 && endLine > startLine) {
			FoldingRange range = new FoldingRange(startLine, endLine);
			range.setKind(FoldingRangeKind.Region);
			ranges.add(range);
		}
	}

	private static void collectNestedFoldingRanges(RNCNode parent, TextDocument textDocument,
			List<FoldingRange> ranges) {
		for (RNCNode child : parent.getChildren()) {
			if (child instanceof RNCElement || child instanceof RNCAttribute) {
				try {
					int startLine = textDocument.positionAt(child.getStart()).getLine();
					int endLine = textDocument.positionAt(Math.max(0, child.getEnd() - 1)).getLine();
					if (endLine > startLine) {
						FoldingRange range = new FoldingRange(startLine, endLine);
						range.setKind(FoldingRangeKind.Region);
						ranges.add(range);
					}
				} catch (BadLocationException e) {
					// ignore
				}
				collectNestedFoldingRanges(child, textDocument, ranges);
			}
		}
	}

	private static List<RNCNode> collectDeclarations(RNCDocument rncDocument) {
		List<RNCNode> declarations = new ArrayList<>();
		for (RNCNode child : rncDocument.getChildren()) {
			if (child instanceof RNCStart || child instanceof RNCDefine
					|| child instanceof RNCNamespaceDecl || child instanceof RNCDatatypesDecl
					|| child instanceof RNCInclude || child instanceof RNCElement
					|| child instanceof RNCAttribute
					|| child instanceof RNCDiv) {
				declarations.add(child);
			}
		}
		return declarations;
	}

	private static int trimTrailingWhitespace(CharSequence text, int offset) {
		while (offset > 0) {
			char ch = text.charAt(offset - 1);
			if (ch != '\n' && ch != '\r' && ch != ' ' && ch != '\t') {
				break;
			}
			offset--;
		}
		return offset;
	}

	private static void addCommentFoldingRanges(CharSequence text, List<FoldingRange> ranges,
			CancelChecker cancelChecker) {
		int commentStartLine = -1;
		int commentEndLine = -1;
		int len = text.length();
		int line = 0;
		int i = 0;

		while (i < len) {
			cancelChecker.checkCanceled();
			int lineStart = i;
			while (i < len && text.charAt(i) != '\n') {
				i++;
			}

			boolean isComment = false;
			for (int j = lineStart; j < i; j++) {
				char ch = text.charAt(j);
				if (ch == ' ' || ch == '\t') {
					continue;
				}
				if (ch == '#') {
					isComment = true;
				}
				break;
			}

			if (isComment) {
				if (commentStartLine == -1) {
					commentStartLine = line;
				}
				commentEndLine = line;
			} else {
				boolean isBlank = (i == lineStart) || isBlankLine(text, lineStart, i);
				if (!isBlank) {
					if (commentStartLine != -1 && commentEndLine > commentStartLine) {
						FoldingRange range = new FoldingRange(commentStartLine, commentEndLine);
						range.setKind(FoldingRangeKind.Comment);
						ranges.add(range);
					}
					commentStartLine = -1;
					commentEndLine = -1;
				}
			}

			if (i < len) {
				i++;
			}
			line++;
		}

		if (commentStartLine != -1 && commentEndLine > commentStartLine) {
			FoldingRange range = new FoldingRange(commentStartLine, commentEndLine);
			range.setKind(FoldingRangeKind.Comment);
			ranges.add(range);
		}
	}

	private static boolean isBlankLine(CharSequence text, int start, int end) {
		for (int i = start; i < end; i++) {
			char ch = text.charAt(i);
			if (ch != ' ' && ch != '\t' && ch != '\r') {
				return false;
			}
		}
		return true;
	}
}
