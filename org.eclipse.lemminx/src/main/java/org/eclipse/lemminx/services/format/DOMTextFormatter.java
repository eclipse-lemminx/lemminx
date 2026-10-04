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

import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats text nodes within XML elements according to their parent's
 * {@link FormatElementCategory}.
 *
 * <p>
 * Text formatting behavior varies by category:
 * </p>
 * <ul>
 * <li><b>PreserveSpace</b> — text is kept as-is; only trailing whitespace
 * on empty lines is trimmed when {@code trimTrailingWhitespace} is enabled.
 * Use case: {@code <pre>  keep  spaces  </pre>} → unchanged.</li>
 *
 * <li><b>NormalizeSpace</b> — consecutive whitespace is collapsed to a
 * single space. Line breaks are preserved (unless {@code joinContentLines}).
 * When {@code maxLineWidth} is exceeded, text wraps to a new line.
 * Use case: {@code <p>  hello   world  </p>} → {@code <p>hello world</p>}.</li>
 *
 * <li><b>MixedContent</b> — inline whitespace between text and child
 * elements is collapsed to one space. Text wraps at {@code maxLineWidth}.
 * Use case: {@code <p>text <b>bold</b> more</p>} → whitespace preserved
 * around child elements.</li>
 *
 * <li><b>IgnoreSpace</b> — text nodes (typically whitespace-only between
 * elements) are handled by element indentation, not here.</li>
 * </ul>
 *
 * <p>
 * The formatter tracks {@code availableLineWidth} through each text token.
 * When the remaining width goes negative, the next whitespace gap is
 * replaced with a newline + indentation (text wrapping).
 * </p>
 *
 * @author Angelo ZERR
 */
public class DOMTextFormatter {

	/** The parent formatter document that provides settings, text access, and edit helpers. */
	private final XMLFormatterDocument formatterDocument;

	/**
	 * Creates a new text formatter.
	 *
	 * @param formatterDocument the parent formatter document (provides settings and edit helpers).
	 */
	public DOMTextFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	/**
	 * Formats a text node: normalizes whitespace, wraps at
	 * {@code maxLineWidth}, and trims trailing spaces.
	 *
	 * <p>Use case (NormalizeSpace):</p>
	 * <pre>
	 * &lt;p&gt;  hello   world  &lt;/p&gt;  →  &lt;p&gt;hello world&lt;/p&gt;
	 * </pre>
	 *
	 * <p>Use case (maxLineWidth wrapping):</p>
	 * <pre>
	 * &lt;p&gt;aaa bbb ccc ddd&lt;/p&gt;  (maxLineWidth=10)
	 * →
	 * &lt;p&gt;aaa bbb
	 *   ccc ddd&lt;/p&gt;
	 * </pre>
	 *
	 * @param textNode          the text node to format.
	 * @param parentConstraints the parent's formatting constraints (modified:
	 *                          {@code availableLineWidth} is updated).
	 * @param start             start offset of the formatting range.
	 * @param end               end offset of the formatting range (-1 for no limit).
	 * @param edits             the list of text edits to populate.
	 */
	public void formatText(DOMText textNode, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		if ((textNode.getStart() > end && end != -1) || textNode.getEnd() < start) {
			return;
		}
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		// PreserveSpace with trimTrailingWhitespace: only trim trailing
		// spaces on empty lines, keep all other whitespace.
		if (formatElementCategory == FormatElementCategory.PreserveSpace && isTrimTrailingWhitespace()) {
			CharSequence text = formatterDocument.getTextSequence();
			int i = text.length() - 1;
			char curr = text.charAt(i);
			boolean removeSpaces = true;
			int lineSeparatorOffset = i + 1;

			while (i >= 0) {
				curr = text.charAt(i);
				if (isLineSeparator(curr)) {
					// remove spaces in an empty line
					// ex:
					// [space][space] --> remove
					if (removeSpaces && textNode.getEnd() > lineSeparatorOffset) {
						removeLeftSpaces(i + 1, lineSeparatorOffset, edits);
					}
					removeSpaces = true;
					lineSeparatorOffset = i;
				} else if (removeSpaces && (!Character.isWhitespace(curr) || isLineSeparator(curr))
						&& textNode.getEnd() > lineSeparatorOffset) {
					// remove spaces after some content at the end of the line
					// ex: <a> </a> [space][space] --> remove
					removeLeftSpaces(i, lineSeparatorOffset, edits);
					removeSpaces = false;
				}
				i--;
			}
			return;
		} else if (formatElementCategory == FormatElementCategory.PreserveSpace) {
			return;
		}
		CharSequence text = formatterDocument.getTextSequence();
		int availableLineWidth = parentConstraints.getAvailableLineWidth();
		int indentLevel = parentConstraints.getIndentLevel();
		boolean isMixedContent = formatElementCategory == FormatElementCategory.MixedContent;

		int spaceStart = -1;
		int spaceEnd = -1;
		int lineSeparatorOffset = -1;
		boolean containsNewLine = false;
		boolean closingBracketHandled = false;

		int textStart = textNode.getStart();
		int textEnd = textNode.getEnd();

		for (int i = textStart; i < textEnd; i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				// Whitespaces...
				if (isLineSeparator(c)) {
					if (!containsNewLine) {
						lineSeparatorOffset = i;
					}
					containsNewLine = true;
				}
				if (spaceStart == -1) {
					spaceStart = i;
				} else {
					spaceEnd = i;
				}
			} else {
				// Text content...
				spaceEnd = i;
				int contentStart = i;
				while (i + 1 < textEnd && !Character.isWhitespace(text.charAt(i + 1))) {
					i++;
				}
				int contentEnd = i + 1;
				// #1649: closingBracketNewLine — move first text to new line
				if (!closingBracketHandled && parentConstraints.isClosingBracketNewLine()
						&& formatElementCategory == FormatElementCategory.NormalizeSpace) {
					closingBracketHandled = true;
					int replaceFrom = spaceStart != -1 ? spaceStart : contentStart;
					replaceLeftSpacesWithIndentation(indentLevel, replaceFrom, contentStart,
							true, edits);
					if (isMaxLineWidthSupported()) {
						availableLineWidth = formatterDocument.getNewLineAvailableWidth(indentLevel)
								- (contentEnd - contentStart);
					}
					spaceStart = -1;
					spaceEnd = -1;
					containsNewLine = false;
					continue;
				}
				if (isMaxLineWidthSupported()) {
					availableLineWidth -= contentEnd - contentStart;
					if (textStart != contentStart && availableLineWidth >= 0
							&& (isJoinContentLines() || !containsNewLine || isMixedContent)) {
						// Decrement width for normalized space between text content (not done at
						// beginning)
						availableLineWidth--;
					}
					if (availableLineWidth < 0 && spaceStart != -1) {
						int mixedContentIndentLevel = parentConstraints.getMixedContentIndentLevel() == 0 ? indentLevel
								: parentConstraints.getMixedContentIndentLevel();
						replaceLeftSpacesWithIndentation(mixedContentIndentLevel, spaceStart, contentStart,
								true, edits);
						availableLineWidth = formatterDocument.getNewLineAvailableWidth(mixedContentIndentLevel)
								- (contentEnd - contentStart);
						containsNewLine = false;
						spaceStart = -1;
						spaceEnd = -1;
						continue;
					} else if (containsNewLine && !isJoinContentLines() && !isMixedContent) {
						availableLineWidth = formatterDocument.getNewLineAvailableWidth(indentLevel)
								- (contentEnd - contentStart);
					}
				}
				if (containsNewLine && !isJoinContentLines() && !isMixedContent) {
					replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd,
							indentLevel, edits);
					containsNewLine = false;
				// Use case (#1026): <a>b  c</a> — don't collapse internal whitespace
				// unless joinContentLines or mixedContent is on.
				} else if (isJoinContentLines() || isMixedContent) {
					replaceSpacesWithOneSpace(spaceStart, spaceEnd - 1, edits);
					containsNewLine = false;
				}
				spaceStart = -1;
				spaceEnd = -1;
			}
		}
		// Fix single-char trailing newline (LF): when the text ends with a lone '\n'
		// before the closing tag, only spaceStart is set. Without this, spaceEnd+1
		// evaluates to 0 and the indentation edit is skipped.
		// Use case: <doc>\nContent\n</doc> → </doc> must be indented.
		if (spaceStart != -1 && spaceEnd == -1 && containsNewLine) {
			spaceEnd = spaceStart;
		}
		// #1649: trailing whitespace handled by formatEndTagElement
		if (parentConstraints.isClosingBracketNewLine()
				&& formatElementCategory == FormatElementCategory.NormalizeSpace) {
			// skip — formatEndTagElement places end tag on new line
		} else if (formatElementCategory != FormatElementCategory.IgnoreSpace && spaceEnd + 1 != text.length()) {
			// Don't format final spaces if text is at the end of the file
			if (formatElementCategory == FormatElementCategory.NormalizeSpace
					&& isMaxLineWidthSupported() && availableLineWidth < 0
					&& spaceStart == -1
					&& !Character.isWhitespace(text.charAt(textStart))) {
				// NormalizeSpace (text-only) element where single-word text exceeds
				// maxLineWidth: keep text inline to avoid zigzag effect where text is
				// moved to a new line but the end tag stays inline.
				// availableLineWidth intentionally stays negative so the parent element
				// can wrap at the next word boundary.
			} else if ((!containsNewLine || isJoinContentLines() || isMixedContent)
					&& (!isMaxLineWidthSupported() || availableLineWidth >= 0)) {
				// Replace spaces with single space in the case of:
				// 1. there is no new line
				// 2. isJoinContentLines
				replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
				if (isMaxLineWidthSupported() && spaceStart != -1) {
					availableLineWidth--;
				}
			} else if (isMaxLineWidthSupported() && availableLineWidth < 0
					&& !Character.isWhitespace(text.charAt(textStart))) {
				// if there is no space between element tag and text but text exceeds max line
				// width, move text to new line. (when text is only one term)
				// ex: ...<example>|text </example>
				int mixedContentIndentLevel = parentConstraints.getMixedContentIndentLevel() == 0 ? indentLevel
						: parentConstraints.getMixedContentIndentLevel();
				replaceLeftSpacesWithIndentationPreservedNewLines(textStart, textStart, mixedContentIndentLevel,
						edits);
				availableLineWidth = formatterDocument.getNewLineAvailableWidth(mixedContentIndentLevel) - (textEnd - textStart);
			} else {
				if (formatElementCategory == FormatElementCategory.NormalizeSpace) {
					// Decrement indent level if is mixed content and text content is the last child
					indentLevel--;
				}
				replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd + 1, indentLevel,
						edits);
				if (isMaxLineWidthSupported()) {
					availableLineWidth = formatterDocument.getNewLineAvailableWidth(indentLevel) - (textEnd - textStart);
				}
			}
		} else if (isTrimTrailingWhitespace()) {
			removeLeftSpaces(spaceStart, lineSeparatorOffset, edits);
		}
		if (isMaxLineWidthSupported()) {
			parentConstraints.setAvailableLineWidth(availableLineWidth);
		}
	}

	/** Returns true if the character is a line separator ({@code \r} or {@code \n}). */
	private static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	/** Delegates to {@link XMLFormatterDocument#replaceSpacesWithOneSpace}. */
	/** Delegates to {@link XMLFormatterDocument#replaceSpacesWithOneSpace}. */
	private void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		formatterDocument.replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentation}. */
	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationPreservedNewLines}. */
	/** Delegates to {@link XMLFormatterDocument#replaceLeftSpacesWithIndentationPreservedNewLines}. */
	private void replaceLeftSpacesWithIndentationPreservedNewLines(int spaceStart, int spaceEnd,
			int indentLevel, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd, indentLevel,
				edits);
	}

	/** Removes whitespace by delegating to {@link XMLFormatterDocument#replaceLeftSpacesWith} with empty replacement. */
	/** Removes whitespace by replacing with an empty string. */
	private void removeLeftSpaces(int leftLimit, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(leftLimit, to, "", edits);
	}

	/** Returns true if content line breaks should be joined into one line. */
	/** Returns true if content lines should be joined (whitespace-only lines removed). */
	private boolean isJoinContentLines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinContentLines();
	}

	/** Returns true if trailing whitespace on lines should be removed. */
	/** Returns true if trailing whitespace should be trimmed. */
	private boolean isTrimTrailingWhitespace() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isTrimTrailingWhitespace();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

}
