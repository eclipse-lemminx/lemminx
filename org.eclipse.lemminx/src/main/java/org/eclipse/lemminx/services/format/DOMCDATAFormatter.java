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
 * DOM CDATA section formatter.
 */
public class DOMCDATAFormatter {
	private final XMLFormatterDocument formatterDocument;

	public DOMCDATAFormatter(XMLFormatterDocument formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	public void formatCDATASection(DOMCDATASection cDATANode, XMLFormattingConstraints parentConstraints,
			List<TextEdit> edits) {
		CharSequence text = formatterDocument.getText();
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

	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	private boolean isJoinCDATALines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isJoinCDATALines();
	}

	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}

	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	private void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		formatterDocument.replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
	}

	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	private boolean isMaxLineWidthSupported() {
		return formatterDocument.isMaxLineWidthSupported();
	}
}
