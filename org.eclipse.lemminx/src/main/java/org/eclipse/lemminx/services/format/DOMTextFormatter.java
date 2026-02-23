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
 * DOM text formatter.
 * 
 * @author Angelo ZERR
 *
 */
public class DOMTextFormatter {

	private final XMLFormatterDocument formatterDocument;

	public DOMTextFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	public void formatText(DOMText textNode, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		if ((textNode.getStart() > end && end != -1) || textNode.getEnd() < start) {
			return;
		}
		// Don't format the spacing in text for case of preserve empty content setting
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		if (formatElementCategory == FormatElementCategory.PreserveSpace && isTrimTrailingWhitespace()) {
			CharSequence text = formatterDocument.getText();
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
		CharSequence text = formatterDocument.getText();
		int availableLineWidth = parentConstraints.getAvailableLineWidth();
		int indentLevel = parentConstraints.getIndentLevel();
		boolean isMixedContent = formatElementCategory == FormatElementCategory.MixedContent;

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
					int maxLineWidth = getMaxLineWidth();
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
						availableLineWidth = maxLineWidth - (contentEnd - contentStart)
								- mixedContentIndentLevel * getTabSize();
						containsNewLine = false;
						spaceStart = -1;
						spaceEnd = -1;
						continue;
					} else if (containsNewLine && !isJoinContentLines() && !isMixedContent) {
						availableLineWidth = maxLineWidth - (contentEnd - contentStart)
								- indentLevel * getTabSize();
					}
				}
				if (containsNewLine && !isJoinContentLines() && !isMixedContent) {
					replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd,
							indentLevel, edits);
					containsNewLine = false;
				} else if (isJoinContentLines() || !containsNewLine || isMixedContent) {
					replaceSpacesWithOneSpace(spaceStart, spaceEnd - 1, edits);
					containsNewLine = false;
				}
				spaceStart = -1;
				spaceEnd = -1;
			}
		}
		if (formatElementCategory != FormatElementCategory.IgnoreSpace && spaceEnd + 1 != text.length()) {
			// Don't format final spaces if text is at the end of the file
			if ((!containsNewLine || isJoinContentLines() || isMixedContent)
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
				availableLineWidth = getMaxLineWidth() - (textEnd - textStart) - mixedContentIndentLevel * getTabSize();
			} else {
				if (formatElementCategory == FormatElementCategory.NormalizeSpace) {
					// Decrement indent level if is mixed content and text content is the last child
					indentLevel--;
				}
				replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd + 1, indentLevel,
						edits);
				if (isMaxLineWidthSupported()) {
					availableLineWidth = getMaxLineWidth() - (textEnd - textStart) - indentLevel * getTabSize();
				}
			}
		} else if (isTrimTrailingWhitespace()) {
			removeLeftSpaces(spaceStart, lineSeparatorOffset, edits);
		}
		if (isMaxLineWidthSupported()) {
			parentConstraints.setAvailableLineWidth(availableLineWidth);
		}
	}

	private static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	private void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		formatterDocument.replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
	}

	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	private void replaceLeftSpacesWithIndentationPreservedNewLines(int spaceStart, int spaceEnd,
			int indentLevel, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationPreservedNewLines(spaceStart, spaceEnd, indentLevel,
				edits);
	}

	private void removeLeftSpaces(int leftLimit, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(leftLimit, to, "", edits);
	}

	private boolean isJoinContentLines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinContentLines();
	}

	private boolean isTrimTrailingWhitespace() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isTrimTrailingWhitespace();
	}

	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}

}
