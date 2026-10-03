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

import org.eclipse.lemminx.dom.DOMCDATASection;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats CDATA sections ({@code <![CDATA[...]]>}).
 *
 * <p>When the {@code joinCDATALines} setting is enabled, whitespace inside the
 * CDATA content is normalized: multiple spaces/newlines between words are
 * collapsed to a single space, and leading/trailing whitespace inside the
 * brackets is removed.</p>
 *
 * <p>Use case (joinCDATALines enabled):</p>
 * <pre>
 * &lt;![CDATA[  hello   world  ]]&gt;
 * →
 * &lt;![CDATA[hello world]]&gt;
 * </pre>
 *
 * <p>When {@code maxLineWidth} is also enabled, content that exceeds the line
 * width is wrapped to a new line with proper indentation instead of being
 * collapsed to a single space.</p>
 *
 * @author Angelo ZERR
 */
public class DOMCDATAFormatter {
	private final XMLFormatterDocument formatterDocument;

	/**
	 * Creates a new CDATA formatter.
	 *
	 * @param formatterDocument the parent formatter document (provides settings and edit helpers).
	 */
	public DOMCDATAFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	/**
	 * Formats a CDATA section by normalizing internal whitespace when
	 * {@code joinCDATALines} is enabled.
	 *
	 * <p>Three whitespace regions are handled:</p>
	 * <ul>
	 * <li><b>Leading</b> — spaces after {@code <![CDATA[} are removed.
	 *   Use case: {@code <![CDATA[  text]]>} → {@code <![CDATA[text]]>}</li>
	 * <li><b>Trailing</b> — spaces before {@code ]]>} are removed.
	 *   Use case: {@code <![CDATA[text  ]]>} → {@code <![CDATA[text]]>}</li>
	 * <li><b>Internal</b> — consecutive spaces between words are collapsed to one,
	 *   or wrapped to a new line if {@code maxLineWidth} would be exceeded.
	 *   Use case: {@code <![CDATA[aaa   bbb]]>} → {@code <![CDATA[aaa bbb]]>}</li>
	 * </ul>
	 *
	 * @param cDATANode         the CDATA section node to format.
	 * @param parentConstraints the parent element's formatting constraints.
	 * @param edits             the list of text edits to populate.
	 */
	public void formatCDATASection(DOMCDATASection cDATANode, XMLFormattingConstraints parentConstraints,
			List<TextEdit> edits) {
		CharSequence text = formatterDocument.getTextSequence();
		int start = cDATANode.getStart();
		int leftWhitespaceOffset = start > 0 ? start - 1 : 0;

		while (leftWhitespaceOffset > 0 && Character.isWhitespace(text.charAt(leftWhitespaceOffset))) {
			leftWhitespaceOffset--;
		}

		if (isJoinCDATALines()) {
			int availableLineWidth = parentConstraints.getAvailableLineWidth();
			int spaceStart = -1;
			int spaceEnd = -1;
			int contentEnd = -1;
			int cDATAStartContent = cDATANode.getStartContent();
			int cDATAEndContent = cDATANode.getEndContent();

			for (int i = cDATAStartContent; i <= cDATAEndContent; i++) {
				char c = text.charAt(i);
				if (Character.isWhitespace(c)) {
					// Whitespaces
					if (spaceStart == -1) {
						spaceStart = i;
					} else {
						spaceEnd = i;
					}
				} else {
					int contentStart = i;
					while (i < cDATAEndContent && !Character.isWhitespace(text.charAt(i + 1))) {
						i++;
					}
					contentEnd = i;
					if (isMaxLineWidthSupported()) {
						availableLineWidth -= (contentEnd + 1 - contentStart);
						if (availableLineWidth <= 0 && spaceStart != -1) {
							replaceLeftSpacesWithIndentation(parentConstraints.getIndentLevel(), spaceStart,
									contentStart, true, edits);
							int indentSpaces = (getTabSize() * parentConstraints.getIndentLevel());
							availableLineWidth = getMaxLineWidth() - indentSpaces - (contentEnd + 1 - contentStart);
							continue;
						} else if (spaceStart != cDATAStartContent && contentEnd != cDATAEndContent) {
							// Add width for single normalized space
							availableLineWidth--;
						}
					}
					if (spaceStart == cDATAStartContent) {
						// Remove spaces before the start bracket of content
						removeLeftSpaces(spaceStart, contentStart, edits);
						spaceStart = -1;
						spaceEnd = -1;
					} else if (contentEnd == cDATAEndContent) {
						// Remove spaces after the ending bracket of content
						removeLeftSpaces(spaceStart, contentEnd, edits);
						spaceStart = -1;
						spaceEnd = -1;
					} else {
						// Normalize space between content
						replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
						spaceStart = -1;
						spaceEnd = -1;
					}
				}
			}
		}
	}

	/** Delegates to {@link XMLFormatterDocument#removeLeftSpaces}. */
	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	/** Returns true if CDATA lines should be joined (whitespace normalized). */
	private boolean isJoinCDATALines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinCDATALines();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
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

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}
}
