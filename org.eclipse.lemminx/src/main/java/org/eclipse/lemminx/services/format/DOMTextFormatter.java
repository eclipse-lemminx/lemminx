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

import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
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
 * <li><b>MixedContent</b> — inline whitespace is collapsed to one space,
 * existing newlines are preserved. In {@code reflow} mode, text
 * soft-wraps at word boundaries when {@code maxLineWidth} is exceeded.
 * In {@code expand} mode, each text node goes on its own indented line.
 * Use case: {@code <p>text  <b>bold</b>  more</p>} →
 * {@code <p>text <b>bold</b> more</p>}.</li>
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
		// When true, MixedContent collapses whitespace (including newlines)
		// to single spaces — backward-compatible behavior in normalize mode.
		// When false (reflow/expand), newlines are preserved.
		//
		// Use case (normalize, default):
		//   <p>text\n  <b>bold</b></p> → <p>text <b>bold</b></p>
		// Use case (normalize + blockElements=["div"]):
		//   same — blockElements ignored in normalize mode.
		// Use case (reflow):
		//   <p>text\n  <b>bold</b></p> → newline preserved
		boolean mixedContentJoins = isMixedContent && !formatterDocument.isMixedContentReflow();
		// NormalizeSpace inside MixedContent parent with reflow active:
		// join content lines so elements like <if> have compact single-line content.
		// Use case (reflow): <set><if>\n  content</if></set> → <if>content</if>
		boolean isNormalizeInsideMixedContent = formatElementCategory == FormatElementCategory.NormalizeSpace
				&& parentConstraints.getMixedContentIndentLevel() > 0
				&& formatterDocument.isMixedContentReflow();
		boolean effectiveJoinContentLines = isJoinContentLines() || isNormalizeInsideMixedContent;

		// expand mode: non-whitespace text goes on its own line.
		// Use case: <p>text <b>bold</b></p> with mixedContent=expand
		//   → <p>\n  text\n  <b>bold</b>\n</p>
		if (isMixedContent && parentConstraints.isWrapAllChildren()) {
			int textStart = textNode.getStart();
			int textEnd = textNode.getEnd();
			// Find first non-whitespace character
			int contentStart = textStart;
			while (contentStart < textEnd && Character.isWhitespace(text.charAt(contentStart))) {
				contentStart++;
			}
			if (contentStart < textEnd) {
				// Has non-whitespace content: put on its own indented line.
				// Use indentLevel directly: in expand mode, text aligns with
				// sibling elements at the current nesting depth.
				replaceLeftSpacesWithIndentation(indentLevel, textStart, contentStart,
						true, edits);
				// Find last non-whitespace to compute content width and trim trailing
				int contentEnd = textEnd;
				while (contentEnd > contentStart && Character.isWhitespace(text.charAt(contentEnd - 1))) {
					contentEnd--;
				}
				// Remove trailing spaces/tabs (not newlines) after content
				int trimEnd = contentEnd;
				while (trimEnd < textEnd
						&& text.charAt(trimEnd) != '\n' && text.charAt(trimEnd) != '\r') {
					trimEnd++;
				}
				if (trimEnd > contentEnd) {
					formatterDocument.replaceLeftSpacesWith(contentEnd, trimEnd, "", edits);
				}
				availableLineWidth = formatterDocument.getNewLineAvailableWidth(indentLevel)
						- (contentEnd - contentStart);
				parentConstraints.setAvailableLineWidth(availableLineWidth);
			}
			return;
		}

		// Text after a block element: force content to start on a new line.
		// Use case: <update>text <set>...</set> more text</update>
		//   " more text" → "\n  more text" (after block </set>)
		if (isMixedContent && formatterDocument.isMixedContentReflow()
				&& isMaxLineWidthSupported()) {
			DOMNode prevSibling = textNode.getPreviousSibling();
			if (prevSibling != null && prevSibling.isElement()
					&& formatterDocument.isBlockElement((DOMElement) prevSibling)) {
				availableLineWidth = 0;
			}
		}

		int spaceStart = -1;
		int spaceEnd = -1;
		int lineSeparatorOffset = -1;
		boolean containsNewLine = false;

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
				if (isMaxLineWidthSupported()) {
					availableLineWidth -= contentEnd - contentStart;
					boolean removeLeading = isNormalizeInsideMixedContent
							&& spaceStart == textStart && containsNewLine;
					if (textStart != contentStart && availableLineWidth >= 0
							&& (effectiveJoinContentLines || !containsNewLine || mixedContentJoins)
							&& !removeLeading) {
						availableLineWidth--;
					}
					if (availableLineWidth < 0 && spaceStart != -1) {
						int mixedContentIndentLevel = parentConstraints.getMixedContentIndentLevel() == 0 ? indentLevel
								: parentConstraints.getMixedContentIndentLevel();
						replaceLeftSpacesWithIndentation(mixedContentIndentLevel, spaceStart, contentStart,
								true, edits);
						availableLineWidth = formatterDocument.getNewLineAvailableWidth(mixedContentIndentLevel)
								- (contentEnd - contentStart);
						if (formatterDocument.isMixedContentReflow()) {
							parentConstraints.setSoftWrapped(true);
						}
						containsNewLine = false;
						spaceStart = -1;
						spaceEnd = -1;
						continue;
					} else if (containsNewLine && !effectiveJoinContentLines && !mixedContentJoins) {
						int effectiveIndentLevel = isMixedContent
								? (parentConstraints.getMixedContentIndentLevel() == 0
										? indentLevel : parentConstraints.getMixedContentIndentLevel())
								: indentLevel;
						availableLineWidth = formatterDocument.getNewLineAvailableWidth(effectiveIndentLevel)
								- (contentEnd - contentStart);
					}
				}
				if (containsNewLine && !effectiveJoinContentLines && !mixedContentJoins) {
					int effectiveIndentLevel = isMixedContent
							? (parentConstraints.getMixedContentIndentLevel() == 0
									? indentLevel : parentConstraints.getMixedContentIndentLevel())
							: indentLevel;
					replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd,
							effectiveIndentLevel, edits);
					if (formatterDocument.isMixedContentReflow()) {
						parentConstraints.setSoftWrapped(true);
					}
					containsNewLine = false;
				} else if (effectiveJoinContentLines || isMixedContent) {
					if (isNormalizeInsideMixedContent && spaceStart == textStart
							&& containsNewLine) {
						formatterDocument.replaceLeftSpacesWith(spaceStart, spaceEnd, "", edits);
					} else {
						replaceSpacesWithOneSpace(spaceStart, spaceEnd - 1, edits);
					}
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
		if (formatElementCategory != FormatElementCategory.IgnoreSpace && spaceEnd + 1 != text.length()) {
			// Don't format final spaces if text is at the end of the file
			if (formatElementCategory == FormatElementCategory.NormalizeSpace
					&& isMaxLineWidthSupported() && availableLineWidth < 0
					&& spaceStart == -1
					&& !Character.isWhitespace(text.charAt(textStart))
					&& !parentConstraints.isWrapAllChildren()) {
				// NormalizeSpace (text-only) element where single-word text exceeds
				// maxLineWidth: keep text inline to avoid zigzag effect where text is
				// moved to a new line but the end tag stays inline.
				// When wrapAllChildren is active, skip this guard — the end tag will
				// also move to its own line (no zigzag).
				// availableLineWidth intentionally stays negative so the parent element
				// can wrap at the next word boundary.
			} else if (parentConstraints.isStartTagCrossedLine()
					&& textNode.getNextSibling() == null
					&& !textNode.isElementContentWhitespace()) {
				// Start tag spans multiple lines (split attributes) and this is
				// the last non-whitespace text before the end tag. The end tag
				// handler in DOMElementFormatter will place the end tag on its
				// own line. Skip trailing whitespace here to avoid overlapping
				// edits.
			} else if ((!containsNewLine || effectiveJoinContentLines || mixedContentJoins)
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
				int effectiveIndentLevel = indentLevel;
				if (formatElementCategory == FormatElementCategory.NormalizeSpace) {
					effectiveIndentLevel--;
				} else if (isMixedContent && textNode.getNextSibling() == null) {
					// Trailing whitespace before end tag → parent indent level
					effectiveIndentLevel--;
				} else if (isMixedContent) {
					effectiveIndentLevel = parentConstraints.getMixedContentIndentLevel() == 0
							? indentLevel : parentConstraints.getMixedContentIndentLevel();
				}
				replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd + 1,
						effectiveIndentLevel, edits);
				if (isMaxLineWidthSupported()) {
					availableLineWidth = formatterDocument.getNewLineAvailableWidth(effectiveIndentLevel);
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

	/** Removes whitespace by replacing with an empty string. */
	private void removeLeftSpaces(int leftLimit, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(leftLimit, to, "", edits);
	}

	/** Returns true if content lines should be joined. */
	private boolean isJoinContentLines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinContentLines();
	}

	/** Returns true if trailing whitespace should be trimmed. */
	private boolean isTrimTrailingWhitespace() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isTrimTrailingWhitespace();
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

}
