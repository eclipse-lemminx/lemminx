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

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lsp4j.TextEdit;

/**
 * DOM attribute formatter.
 * 
 * @author Angelo ZERR
 *
 */
public class DOMAttributeFormatter {

	private final XMLFormatterDocumentNew formatterDocument;

	public DOMAttributeFormatter(XMLFormatterDocumentNew formatterDocument) {
		this.formatterDocument = formatterDocument;
	}

	public void formatAttribute(DOMAttr attr, int prevOffset, boolean singleAttribute, boolean useSettings,
			XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		// 1) format before attribute name : indent left of the attribute name
		// ex : <foo[space][space]attr=""
		// --> <foo[space]attr=""
		boolean alreadyIndented = false;
		if (useSettings) {
			int indentLevel = parentConstraints.getIndentLevel();
			if (isPreserveAttributeLineBreaks() && hasLineBreak(prevOffset, attr.getStart())) {
				replaceLeftSpacesWithIndentation(indentLevel + 1, attr.getStart(), true, edits);
				alreadyIndented = true;
			} else if (isSplitAttributes() && !singleAttribute) {
				// move the attribute to a new line and indent it.
				replaceLeftSpacesWithIndentation(indentLevel + getSplitAttributesIndentSize(), attr.getStart(), true,
						edits);
				alreadyIndented = true;
			}
		}
		if (!alreadyIndented) {
			// // remove extra whitespaces between previous attribute
			// attr0='name'[space][space][space]attr1='name' -->
			// attr0='name'[space]attr1='name'

			// Adjust the startAttr to avoid ignoring invalid content
			// ex : <asdf |""`=asdf />
			// must be adjusted with <asdf ""|`=asdf /> to keep the invalid content ""
			int from = prevOffset;
			int to = attr.getStart();
			replaceLeftSpacesWithOneSpace(from, to, edits);
		}

		// 2) format delimiter : remove whitespaces between '='
		// ex : <foo attr = ""
		// --> <foo attr=""
		if (attr.hasDelimiter()) {
			int delimiterOffset = attr.getDelimiterOffset(); // <foo attr =| ""

			// 2.1 Remove extra spaces between end of attribute name and delimiter
			int attrNameEnd = attr.getNodeAttrName().getEnd(); // <foo attr| = ""
			removeLeftSpaces(attrNameEnd, delimiterOffset, edits);

			if (attr.getNodeAttrValue() != null) {
				// 2.2 Remove extra spaces between delimiter and start of attribute value
				int attrValueStart = attr.getNodeAttrValue().getStart(); // <foo attr = |""
				removeLeftSpaces(delimiterOffset, attrValueStart, edits);
			}
		}
	}

	private void replaceLeftSpacesWithOneSpace(int from, int to, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithOneSpace(from, to, edits);
	}

	private void replaceLeftSpacesWithIndentation(int indentLevel, int offset, boolean addLineSeparator,
			List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, offset, addLineSeparator, edits);
	}

	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	private boolean isSplitAttributes() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isSplitAttributes();
	}

	private int getSplitAttributesIndentSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributesIndentSize();
	}

	boolean isPreserveAttributeLineBreaks() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveAttributeLineBreaks();
	}

	private boolean hasLineBreak(int prevOffset, int start) {
		return formatterDocument.hasLineBreak(prevOffset, start);
	}

}
