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

	private final XMLFormatterDocumentNew formatterDocument;

	public DOMTextFormatter(XMLFormatterDocumentNew formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	public void formatText(DOMText textNode, XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		String text = formatterDocument.getText();
		int availableLineWidth = parentConstraints.getAvailableLineWidth();

		int spaceStart = -1;
		int spaceEnd = -1;

		for (int i = textNode.getStart(); i < textNode.getEnd(); i++) {
			char c = text.charAt(i);
			if (Character.isWhitespace(c)) {
				// Whitespaces...
				if (spaceStart == -1) {
					spaceStart = i;
				} else {
					spaceEnd = i;
				}
			} else {
				// Text content...
				int contentStart = i;
				while (i < textNode.getEnd() + 1 && !Character.isWhitespace(text.charAt(i + 1))) {
					i++;
				}

				int contentEnd = i;
				availableLineWidth -= (contentEnd - contentStart);

				if (formatElementCategory != FormatElementCategory.PreserveSpace
						&& formatElementCategory != FormatElementCategory.IgnoreSpace) {
					if (availableLineWidth <= 0) {
						if (spaceStart != -1) {
							insertLineBreak(spaceStart, contentStart, edits);
							availableLineWidth = getMaxLineWidth();
						}
					} else {
						replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
					}
				}

				spaceStart = -1;
				spaceEnd = -1;
			}
		}
		if (formatElementCategory != FormatElementCategory.PreserveSpace
				&& formatElementCategory != FormatElementCategory.IgnoreSpace) {
			replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
		}
	}

	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	private void insertLineBreak(int start, int end, List<TextEdit> edits) {
		formatterDocument.insertLineBreak(start, end, edits);
	}

	private void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		formatterDocument.replaceSpacesWithOneSpace(spaceStart, spaceEnd, edits);
	}
}
