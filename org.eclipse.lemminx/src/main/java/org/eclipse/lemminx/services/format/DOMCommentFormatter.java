/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format;

import java.util.List;

import org.eclipse.lemminx.dom.DOMComment;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats XML comment nodes ({@code <!-- ... -->}).
 *
 * <p>
 * Comment formatting handles two main concerns:
 * </p>
 * <ul>
 * <li><b>Indentation</b> — aligns the comment with its surrounding elements
 * when it appears on its own line.
 * Use case:
 * <pre>
 * &lt;root&gt;
 *          &lt;!-- misindented --&gt;
 * &lt;/root&gt;
 * →
 * &lt;root&gt;
 *   &lt;!-- misindented --&gt;
 * &lt;/root&gt;
 * </pre></li>
 *
 * <li><b>Line wrapping</b> — when {@code maxLineWidth} is enabled, comment
 * content that exceeds the limit wraps at word boundaries.
 * Use case (maxLineWidth=40):
 * <pre>
 * &lt;!-- This is a very long comment that exceeds the max line width --&gt;
 * →
 * &lt;!-- This is a very long comment
 * that exceeds the max line width --&gt;
 * </pre></li>
 * </ul>
 *
 * <p>
 * The {@code joinCommentLines} setting controls whether existing line breaks
 * inside comments are collapsed. When {@code true}, multi-line comments are
 * joined into a single line (then re-wrapped by maxLineWidth if needed).
 * When {@code false}, existing line breaks are preserved.
 * </p>
 */
public class DOMCommentFormatter {

	private final XMLFormatterDocument formatterDocument;

	/**
	 * Creates a new comment formatter.
	 *
	 * @param formatterDocument the parent formatter document (provides settings and edit helpers).
	 */
	public DOMCommentFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	/**
	 * Formats a comment node: adjusts indentation and wraps content at
	 * {@code maxLineWidth} word boundaries.
	 *
	 * <p>Use case (indentation):</p>
	 * <pre>
	 * &lt;root&gt;
	 *              &lt;!-- comment --&gt;
	 * &lt;/root&gt;
	 * →
	 * &lt;root&gt;
	 *   &lt;!-- comment --&gt;
	 * &lt;/root&gt;
	 * </pre>
	 *
	 * <p>Use case (line wrapping with maxLineWidth):</p>
	 * <pre>
	 * &lt;!-- word1 word2 word3 word4 word5 --&gt;
	 * →
	 * &lt;!-- word1 word2
	 * word3 word4 word5 --&gt;
	 * </pre>
	 *
	 * @param commentNode       the comment node to format.
	 * @param parentConstraints the parent's formatting constraints
	 *                          (availableLineWidth is updated).
	 * @param startRange        the start offset of the formatting range.
	 * @param endRange          the end offset of the formatting range.
	 * @param edits             the list of text edits to populate.
	 */
	public void formatComment(DOMComment commentNode, XMLFormattingConstraints parentConstraints, int startRange,
			int endRange, List<TextEdit> edits) {

		// Skip unclosed comments (e.g., "<!-- no closing")
		if (commentNode.getEnd() == commentNode.getEndContent()) {
			return;
		}

		CharSequence text = formatterDocument.getTextSequence();
		int availableLineWidth = parentConstraints.getAvailableLineWidth();
		int start = commentNode.getStart();
		int leftWhitespaceOffset = start > 0 ? start - 1 : 0;

		// Scan left to find the start of whitespace before the comment
		while (leftWhitespaceOffset > 0 && Character.isWhitespace(text.charAt(leftWhitespaceOffset))) {
			leftWhitespaceOffset--;
		}

		int indentLevel = parentConstraints.getIndentLevel();
		int tabSize = getTabSize();
		int maxLineWidth = getMaxLineWidth();

		// Normalize indentation when the comment is on its own line
		if (formatterDocument.hasLineBreak(leftWhitespaceOffset, start) && startRange < start) {
			replaceLeftSpacesWithIndentationPreservedNewLines(0, start, indentLevel, edits);
			availableLineWidth = maxLineWidth - tabSize * indentLevel;
		}
		int spaceStart = -1;
		int spaceEnd = -1;
		availableLineWidth -= 4; // account for '<!--'
		int whiteSpaceOffset = -1;

		// Walk through comment content character by character, tracking
		// word boundaries and available line width for wrapping.
		for (int i = commentNode.getStartContent(); i < commentNode.getEndContent(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				if (isLineSeparator(c) && !isJoinCommentLines()) {
					// Preserve existing line breaks: reset width for the new line
					availableLineWidth = maxLineWidth;
				}
				whiteSpaceOffset = i;

				if (spaceStart == -1) {
					spaceStart = i;
				} else {
					spaceEnd = i;
				}
			} else {
				spaceEnd = i;
				// Ensure the edit is within the selected range
				if (startRange != -1 && endRange != -1 && (startRange > spaceStart || endRange < spaceEnd)) {
					return;
				}
				int contentStart = i;
				while (i + 1 < commentNode.getEnd() && !Character.isWhitespace(text.charAt(i + 1))) {
					i++;
				}
				int contentEnd = i + 1;
				if (isMaxLineWidthSupported()) {
					// Track width: subtract space + word width from available
					if (commentNode.getStartContent() != contentStart && isJoinCommentLines()
							&& availableLineWidth >= 0) {
						// joinCommentLines: count one space between words
						availableLineWidth--;
					} else {
						// Preserve original whitespace width
						availableLineWidth -= spaceEnd - whiteSpaceOffset;
					}
					availableLineWidth -= (contentEnd - contentStart);
					if (availableLineWidth < 0 && spaceStart != -1) {
						// Word overflows maxLineWidth → wrap to new line
						// Use case: <!-- word1 word2| → <!-- word1\n  word2
						replaceLeftSpacesWithIndentation(indentLevel, spaceStart, contentStart,
								true, edits);
						int indentSpaces = tabSize * indentLevel;
						availableLineWidth = maxLineWidth - indentSpaces - (contentEnd - contentStart);
						spaceStart = -1;
						spaceEnd = -1;
						continue;
					}
				}
				if (isJoinCommentLines()) {
					replaceSpacesWithOneSpace(spaceStart, spaceEnd - 1, edits);
				}
				spaceStart = -1;
				spaceEnd = -1;
			}
		}
		if (isJoinCommentLines()) {
			replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
			if (isMaxLineWidthSupported()) {
				availableLineWidth--;
				parentConstraints.setAvailableLineWidth(availableLineWidth);
			}
		}
	}

	/** Returns true if existing line breaks inside comments should be collapsed. */
	private boolean isJoinCommentLines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinCommentLines();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

	/** Delegates to {@link XMLFormatterDocument#replaceSpacesWithOneSpace}. */
	private void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		formatterDocument.replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationPreservedNewLines}. */
	private void replaceLeftSpacesWithIndentationPreservedNewLines(int spaceStart, int spaceEnd,
			int indentLevel, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd, indentLevel,
				edits);
	}

	/**
	 * Returns {@code true} if the character is a line separator ({@code \r} or {@code \n}).
	 */
	private static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

}
