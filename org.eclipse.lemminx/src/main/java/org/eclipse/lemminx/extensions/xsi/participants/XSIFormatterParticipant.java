/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
*  are made available under the terms of the Eclipse Public License v2.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.xsi.participants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.xsi.XSISchemaModel;
import org.eclipse.lemminx.extensions.xsi.settings.XSISchemaLocationSplit;
import org.eclipse.lemminx.services.extensions.format.IFormatterParticipant;
import org.eclipse.lemminx.services.format.XMLFormatterDocument;
import org.eclipse.lemminx.services.format.XMLFormattingConstraints;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLBuilder;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formatter participant implementation to format xsi:schemaLocation attribute
 * value. The format of the xsi:schemaLocation attribute value depends on the
 * {@link XSISchemaLocationSplit} setting:
 * 
 * <ul>
 * <li>{@link XSISchemaLocationSplit#none} : don't format the xsi:schemaLocation
 * attribute value.</li>
 * <li>{@link XSISchemaLocationSplit#onElement} : generate a line feed for each
 * namespace declaration:
 * 
 * <pre>
 * &lt;beans
        xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:util="http://www.springframework.org/schema/util"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
                            http://www.springframework.org/schema/beans/spring-beans.xsd
                            http://www.springframework.org/schema/util
                            http://www.springframework.org/schema/util/spring-util.xsd"&gt;
 * </pre>
 * 
 * </li>
 * <li>{@link XSISchemaLocationSplit#onPair} : generate a line feed for each
 * location declaration:
 * 
 * <pre>
 * &lt;beans
        xmlns="http://www.springframework.org/schema/beans"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:util="http://www.springframework.org/schema/util"
        xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                            http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd"&gt;
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * @author Angelo ZERR
 *
 */
public class XSIFormatterParticipant implements IFormatterParticipant {

	@Override
	public boolean formatAttributeValue(String name, String valueWithoutQuote, Character quote, DOMAttr attr,
			XMLBuilder xml) {
		if (XSISchemaModel.isXSISchemaLocationAttr(name, attr)) {
			// The attribute is a xsi:schemaLocation
			XMLFormattingOptions formattingOptions = xml.getSharedSettings().getFormattingSettings();
			XSISchemaLocationSplit split = XSISchemaLocationSplit.getSplit(formattingOptions);
			if (split == XSISchemaLocationSplit.none) {
				// don't format the xsi:schemaLocation attribute value
				return false;
			}
			int lineFeed = split == XSISchemaLocationSplit.onElement ? 1 : 2;
			if (quote != null) {
				xml.append(quote);
			}
			List<String> locations = getLocations(valueWithoutQuote);
			String indent = "";
			boolean insertSpaces = formattingOptions.isInsertSpaces();
			int tabSize = formattingOptions.getTabSize();
			for (int i = 0; i < locations.size(); i++) {
				if (i % lineFeed == 0) {
					if (i == 0) {
						indent = getCurrentLineIndent(xml, insertSpaces, tabSize);
					} else {
						xml.linefeed();
						xml.append(indent);
					}
				} else {
					xml.appendSpace();
				}
				xml.append(locations.get(i));
			}
			if (quote != null) {
				xml.append(quote);
			}
			return true;
		}
		return false;
	}

	private static List<String> getLocations(String value) {
		List<String> locations = new ArrayList<>();
		int start = -1;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (Character.isWhitespace(c)) {
				if (start != -1) {
					locations.add(value.substring(start, i));
					start = -1;
				}
			} else if (start == -1) {
				start = i;
			}
		}
		if (start != -1) {
			locations.add(value.substring(start, value.length()));
		}
		return locations;
	}

	public String getCurrentLineIndent(XMLBuilder xml, boolean insertSpaces, int tabSize) {
		int nbChars = 0;
		for (int i = xml.length() - 1; i >= 0; i--) {
			if (xml.charAt(i) == '\r' || xml.charAt(i) == '\n') {
				break;
			}
			if (!insertSpaces && xml.charAt(i) == '\t') {
				nbChars = nbChars + tabSize;
			} else {
				nbChars++;
			}
		}
		StringBuilder indent = new StringBuilder();
		if (insertSpaces || tabSize <= 0) {
			for (int i = 0; i < nbChars; i++) {
				indent.append(" ");
			}
		} else {
			int nbTabs = nbChars / tabSize;
			nbChars = nbChars % tabSize;
			for (int i = 0; i < nbTabs; i++) {
				indent.append("\t");
			}
			for (int i = 0; i < nbChars; i++) {
				indent.append(" ");
			}
		}
		return indent.toString();
	}

	@Override
	public boolean formatAttributeValue(DOMAttr attr, XMLFormatterDocument formatterDocument,
			XMLFormattingConstraints parentConstraints, XMLFormattingOptions formattingOptions, List<TextEdit> edits) {

		XSISchemaLocationSplit split = XSISchemaLocationSplit.getSplit(formattingOptions);

		if (split == XSISchemaLocationSplit.none || !XSISchemaModel.isXSISchemaLocationAttr(attr.getName(), attr)) {
			if (formatterDocument.isMaxLineWidthSupported() && attr.getValue() != null) {
				parentConstraints
						.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - attr.getValue().length());
			}
			return false;
		}

		int firstContentOffset = getFirstContentOffset(attr.getOriginalValue());
		if (firstContentOffset == -1) {
			return false;
		}
		int attrValueStart = attr.getNodeAttrValue().getStart();
		// Remove extra spaces between start of xsi:schemaLocation attribute value quote
		// and actual value
		formatterDocument.removeLeftSpaces(attrValueStart + 1, // <... xsi:schemaLocation="| value"
				// <... xsi:schemaLocation=" |value"
				attrValueStart + firstContentOffset, edits);

		int tabSize = formattingOptions.getTabSize();
		int indentSpaceOffset;
		DOMElement ownerElement = attr.getOwnerElement();
		int startOfLineOffset = ownerElement != null
				? formatterDocument.getLineAtOffset(ownerElement.getStart())
				: formatterDocument.getLineAtOffset(attr.getStart());

		if (!formattingOptions.getSplitAttributes().isPreserve()) {
			if (formattingOptions.getSplitAttributes().isAlignWithFirstAttr() && ownerElement != null) {
				// force-aligned, alignWithFirstAttr, aligned-multiple, preserve-aligned:
				// align xsi:schemaLocation with the first attribute
				indentSpaceOffset = (attrValueStart + 1) - attr.getNodeAttrName().getStart()
						+ ownerElement.getTagName().length() + 2;
			} else {
				// force, force-expand-multiline, splitNewLine, auto:
				// indent xsi:schemaLocation with splitAttributesIndentSize
				indentSpaceOffset = (attrValueStart + 1) - attr.getNodeAttrName().getStart()
						+ formattingOptions.getSplitAttributesIndentSize() * tabSize;
			}
		} else if (formattingOptions.isPreserveAttributeLineBreaks()) {
			indentSpaceOffset = attrValueStart - formatterDocument.getOffsetWithPreserveLineBreaks(startOfLineOffset,
					attrValueStart, tabSize, formattingOptions.isInsertSpaces());
		} else {
			indentSpaceOffset = formatterDocument.getNormalizedLength(startOfLineOffset, attrValueStart + 1)
					- startOfLineOffset;
		}

		int lineFeed = split == XSISchemaLocationSplit.onElement ? 1 : 2;
		int locationNum = 1;
		String attrValue = attr.getOriginalValue();
		int lastAttrValueTermIndex = 0;
		int availableLineWidth = parentConstraints.getAvailableLineWidth();

		for (int i = firstContentOffset; i < attrValue.length(); i++) {
			int from = formatterDocument.adjustOffsetWithLeftWhitespaces(attrValueStart, attrValueStart + i + 1);
			if (Character.isWhitespace(attrValue.charAt(i)) && !Character.isWhitespace(attrValue.charAt(i + 1))
					&& !StringUtils.isQuote(attrValue.charAt(from - attrValueStart))) {
				availableLineWidth -= i - lastAttrValueTermIndex;
				lastAttrValueTermIndex = i;
				if (availableLineWidth < 0 && formatterDocument.isMaxLineWidthSupported()
						&& formattingOptions.getSplitAttributes().isPreserve()) {
					indentSpaceOffset = (attrValueStart + 1) - attr.getNodeAttrName().getStart()
							+ (parentConstraints.getIndentLevel() + 1) * tabSize;
				}
				int attrValuelength = attrValueStart - indentSpaceOffset + attr.getValue().length();
				// Insert newline and indent where required based on setting
				if (locationNum % lineFeed == 0) {
					formatterDocument.replaceLeftSpacesWithIndentationWithOffsetSpaces(indentSpaceOffset,
							attrValueStart, attrValueStart + i + 1, true, edits);
					availableLineWidth = formatterDocument.getMaxLineWidth() - attrValuelength;
				} else {
					formatterDocument.replaceLeftSpacesWithOneSpace(indentSpaceOffset, attrValueStart + i + 1, edits);
					availableLineWidth = availableLineWidth - attrValuelength;
				}
				locationNum++;
			}
		}
		if (formatterDocument.isMaxLineWidthSupported() && attr.getValue() != null) {
			parentConstraints
					.setAvailableLineWidth(availableLineWidth);
		}
		return true;
	}

	/**
	 * Returns the offset from opening quote to first non-whitespace character of an
	 * attribute value
	 *
	 * @param originalValue
	 * @return offset from opening quote to first non-whitespace character of an
	 *         attribute value
	 */
	private static int getFirstContentOffset(String originalValue) {
		if (originalValue == null) {
			return -1;
		}
		for (int i = 1; i < originalValue.length(); i++) {
			if (!Character.isWhitespace(originalValue.charAt(i)) && !StringUtils.isQuote(originalValue.charAt(i))) {
				return i;
			}
		}
		return -1;
	}
}
