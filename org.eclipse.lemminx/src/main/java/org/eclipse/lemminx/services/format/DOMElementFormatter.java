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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.EmptyElements;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.TextEdit;

/**
 * DOM element formatter.
 *
 * @author Angelo ZERR
 *
 */
public class DOMElementFormatter {

	private final XMLFormatterDocument formatterDocument;

	private final DOMAttributeFormatter attributeFormatter;

	public DOMElementFormatter(XMLFormatterDocument formatterDocument, DOMAttributeFormatter attributeFormatter) {
		this.formatterDocument = formatterDocument;
		this.attributeFormatter = attributeFormatter;
	}

	public void formatElement(DOMElement element, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		FormatElementCategory formatElementCategory = getFormatElementCategory(element, parentConstraints);
		EmptyElements emptyElements = getEmptyElements(element, formatElementCategory);

		// Format start tag element with proper indentation
		int indentLevel = parentConstraints.getIndentLevel();
		int width = formatStartTagElement(element, parentConstraints, emptyElements, start, end, edits);
		parentConstraints.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - width);

		// Set indent level for text in mixed content
		int mixedIndentLevel = parentConstraints.getMixedContentIndentLevel();
		if (mixedIndentLevel == 0
				&& parentConstraints.getFormatElementCategory() == FormatElementCategory.MixedContent) {
			parentConstraints.setMixedContentIndentLevel(indentLevel);
		}

		if (emptyElements == EmptyElements.ignore) {
			// Format children of the element
			XMLFormattingConstraints constraints = new XMLFormattingConstraints();
			constraints.copyConstraints(parentConstraints);
			if ((element.isClosed())) {
				constraints.setIndentLevel(indentLevel + 1);
			}
			constraints.setFormatElementCategory(formatElementCategory);

			formatChildren(element, constraints, start, end, edits);

			// Format end tag element with proper indentation
			if (element.hasEndTag() && (element.getEndTagOpenOffset() > start
					&& (end == -1 || element.getEndTagCloseOffset() < end))) {
				width = formatEndTagElement(element, parentConstraints, constraints, edits);
				parentConstraints.setAvailableLineWidth(constraints.getAvailableLineWidth() - width);
			}
		}
	}

	private int formatStartTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			EmptyElements emptyElements, int start, int end, List<TextEdit> edits) {
		if (!element.hasStartTag()) {
			// ex : </
			return element.getEnd() - element.getStart();
		}
		int indentLevel = parentConstraints.getIndentLevel();
		int width = element.getTagName() != null ? element.getTagName().length() + 1 : 0;
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		int startTagOpenOffset = element.getStartTagOpenOffset();
		int startTagCloseOffset = element.getStartTagCloseOffset();

		if (end != -1 && startTagOpenOffset > end
				|| start != -1 && startTagCloseOffset != -1 && startTagCloseOffset < start
				|| start != -1 && element.isSelfClosed() && element.getEnd() < start) {
			return 0;
		}
		switch (formatElementCategory) {
		case PreserveSpace:
			// Preserve existing spaces
			break;
		case MixedContent:
			// Remove spaces and indent if the content between start tag and parent start
			// tag is some white spaces
			// before formatting: <a> [space][space] <b> </b> example text </a>
			// after formatting: <a>\n <b> </b> example text </a>
			int parentStartCloseOffset = element.getParentElement() != null ? element.getParentElement().getStartTagCloseOffset() + 1 : 0;
			if ((parentStartCloseOffset != startTagOpenOffset
					&& StringUtils.isWhitespace(formatterDocument.getText(), parentStartCloseOffset,
							startTagOpenOffset))) {
				replaceLeftSpacesWithIndentationPreservedNewLines(parentStartCloseOffset, startTagOpenOffset,
						indentLevel, edits);
				parentConstraints.setAvailableLineWidth(getMaxLineWidth());
				width += indentLevel * getTabSize();
			}
			break;
		case IgnoreSpace:
			if (element.getParentNode().isOwnerDocument() && element.getParentNode().getFirstChild() == element) {
				// If the element is at the start of the file, remove new lines and spaces
				replaceLeftSpacesWithIndentation(indentLevel, 0, startTagOpenOffset, false, edits);
				break;
			}
			replaceLeftSpacesWithIndentationPreservedNewLines(0, startTagOpenOffset,
					indentLevel, edits);
			width += indentLevel * getTabSize();
			parentConstraints.setAvailableLineWidth(getMaxLineWidth());
			break;
		case NormalizeSpace:
			break;
		}
		parentConstraints.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - width);
		if (formatElementCategory != FormatElementCategory.PreserveSpace) {
			formatAttributes(element, parentConstraints, edits);
			boolean formatted = false;
			width = 0;
			switch (emptyElements) {
			case expand: {
				if (element.isSelfClosed()) {
					// expand empty element: <example /> -> <example></example>
					StringBuilder tag = new StringBuilder();
					tag.append(">");
					tag.append("</");
					tag.append(element.getTagName());
					tag.append('>');
					// get the from offset:
					// - <foo| />
					// - <foo attr1="" attr2=""| />
					int from = getOffsetAfterStartTagOrLastAttribute(element);
					// get the to offset:
					// - <foo />|
					// - <foo attr1="" attr2="" />|
					int to = element.getEnd();
					// replace with ></foo>
					// - <foo></foo>
					// - <foo attr1="" attr2=""></foo>
					createTextEditIfNeeded(from, to, tag.toString(), edits);
					formatted = true;
					// add 4 to width for the additional tag name and '></...>'
					width += element.getTagName() != null ? element.getTagName().length() + 4 : 0;
				}
				break;
			}
			case collapse: {
				// collapse empty element: <example></example> -> <example />
				if (!element.isSelfClosed() && (end == -1 || element.getEndTagOpenOffset() + 1 < end)
						&& (shouldCollapseEmptyElement(element, formatterDocument.getSharedSettings()))) {
					// Do not collapse if range is does not cover the element or is prohibited by
					// grammar constraint
					StringBuilder tag = new StringBuilder();
					if (isSpaceBeforeEmptyCloseTag()) {
						tag.append(" ");
					}
					tag.append("/>");
					// get the from offset:
					// - <foo| ></foo>
					// - <foo attr1="" attr2=""| ></foo>
					int from = getOffsetAfterStartTagOrLastAttribute(element);
					// get the to offset:
					// - <foo ></foo>|
					// - <foo attr1="" attr2="" ></foo>|
					int to = element.getEnd();
					// replace with />
					// - <foo />
					// - <foo attr1="" attr2="" />
					createTextEditIfNeeded(from, to, tag.toString(), edits);
					formatted = true;
					width++;
				}
				break;
			}
			default:
				// count width of closing bracket '>'
				width++;
			}

			if (!formatted) {
				if (element.isStartTagClosed() || element.isSelfClosed()) {
					width = formatElementStartTagOrSelfClosed(element, parentConstraints, edits);
				}
			}
		}
		return width;
	}

	private static int getOffsetAfterStartTagOrLastAttribute(DOMElement element) {
		DOMAttr attr = getLastAttribute(element);
		if (attr != null) {
			return attr.getEnd();
		}
		return element.getOffsetAfterStartTag();
	}

	private int formatAttributes(DOMElement element, XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		if (element.hasAttributes()) {
			List<DOMAttr> attributes = element.getAttributeNodes();
			// initialize the previous offset with the start tag:
			// <foo| attr1="" attr2="">.
			int prevOffset = element.getOffsetAfterStartTag();
			boolean singleAttribute = attributes.size() == 1;
			boolean isFirstAttr = true;
			for (DOMAttr attr : attributes) {
				// Format current attribute
				attributeFormatter.formatAttribute(attr, prevOffset, singleAttribute, true, isFirstAttr,
						parentConstraints, edits);
				isFirstAttr = false;
				// set the previous offset with end of the current attribute:
				// <foo attr1=""| attr2="".
				prevOffset = attr.getEnd();
			}
		}
		return 0;
	}

	/**
	 * Formats the start tag's closing bracket (>) according to
	 * {@code XMLFormattingOptions#isPreserveAttrLineBreaks()}
	 *
	 * {@code XMLFormattingOptions#isPreserveAttrLineBreaks()}: If true, must add a
	 * newline + indent before the closing bracket if the last attribute of the
	 * element and the closing bracket are in different lines.
	 *
	 * @param element
	 * @throws BadLocationException
	 */
	private int formatElementStartTagOrSelfClosed(DOMElement element, XMLFormattingConstraints parentConstraints,
			List<TextEdit> edits) {
		// <foo| >
		// <foo| />
		int startTagClose = element.getOffsetBeforeCloseOfStartTag();
		// <foo |>
		// <foo |/>
		int startTagOpen = element.getOffsetAfterStartTag();
		String replace = "";
		boolean spaceBeforeEmptyCloseTag = isSpaceBeforeEmptyCloseTag();
		int width = 0;
		if (isPreserveAttributeLineBreaks() && element.hasAttributes()
				&& hasLineBreak(getLastAttribute(element).getEnd(), startTagClose)) {
			spaceBeforeEmptyCloseTag = false;
			int indentLevel = parentConstraints.getIndentLevel();
			if (indentLevel == 0) {
				// <foo\n
				// attr1="" >

				// Add newline when there is no indent
				replace = formatterDocument.getLineDelimiter();
			} else {
				// <foo>\n
				// <bar\n
				// attr1="" >
				// Add newline with indent according to indent level
				replaceLeftSpacesWithIndentation(indentLevel, startTagOpen, startTagClose, true, edits);
				return width;
			}
		} else if (shouldFormatClosingBracketNewLine(element)) {
			int indentLevel = parentConstraints.getIndentLevel();
			if (getSplitAttributes() == SplitAttributes.splitNewLine) {
				replaceLeftSpacesWithIndentation(indentLevel + getSplitAttributesIndentSize(), startTagOpen,
						startTagClose, true, edits);
				return (indentLevel + getSplitAttributesIndentSize()) * getTabSize();
			} else { /* splitAttributes == alignWithFirstAttr */
				int indentOffset = indentLevel * getTabSize() + element.getTagName().length() + 2;
				replaceLeftSpacesWithIndentationWithOffsetSpaces(indentOffset, startTagOpen, startTagClose, edits);
				return indentOffset;
			}
		}
		if (element.isSelfClosed()) {
			if (spaceBeforeEmptyCloseTag) {
				// <foo attr1=""/> --> <foo attr1=""[space] />
				replace = replace + " ";
				width++; // add width for [space]
			}
			width++; // add width for '/'
		}
		// remove spaces from the offset of start tag and start tag close
		// <foo|[space][space]|> --> <foo>
		// <foo attr1="" attr2="" |[space][space]|> --> <foo>
		replaceLeftSpacesWith(startTagOpen, startTagClose, replace, edits);
		width++; // add width for '>'
		return width;
	}

	private int formatEndTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			XMLFormattingConstraints constraints, List<TextEdit> edits) {
		// 1) remove / add some spaces on the left of the end tag element
		// before formatting : [space][space]</a>
		// after formatting : </a>
		int indentLevel = parentConstraints.getIndentLevel();
		FormatElementCategory formatElementCategory = constraints.getFormatElementCategory();
		int endTagOpenOffset = element.getEndTagOpenOffset();
		int startTagCloseOffset = element.getStartTagCloseOffset();

		int width = element.getTagName() != null ? element.getTagName().length() + 2 : 0;

		switch (formatElementCategory) {
		case PreserveSpace:
			// Preserve existing spaces
			break;
		case MixedContent:
			// Remove spaces and indent if the last child is an element, not text
			// before formatting: <a> example text <b> </b> [space][space]</a>
			// after formatting: <a> example text <b> </b>\n</a>
			DOMNode lastChild = element.getLastChild();
			if (lastChild != null
					&& (lastChild.isElement() || lastChild.isComment())
					&& Character.isWhitespace(formatterDocument.getText().charAt(endTagOpenOffset - 1))) {
				replaceLeftSpacesWithIndentationPreservedNewLines(startTagCloseOffset, endTagOpenOffset,
						indentLevel, edits);
				width += indentLevel * getTabSize();
			}
			break;
		case IgnoreSpace:
			replaceLeftSpacesWithIndentationPreservedNewLines(startTagCloseOffset, endTagOpenOffset,
					indentLevel, edits);
			width += indentLevel * getTabSize();
			break;
		case NormalizeSpace:
			break;
		}
		// 2) remove some spaces between the end tag and and close bracket
		// before formatting : <a></a[space][space]>
		// after formatting : <a></a>
		if (element.isEndTagClosed()) {
			int endTagCloseOffset = element.getEndTagCloseOffset();
			removeLeftSpaces(element.getEndTagOpenOffset(), endTagCloseOffset, edits);
			width++;
		}
		return width;
	}

	/**
	 * Return the option to use to generate empty elements.
	 *
	 * @param element the DOM element
	 * @return the option to use to generate empty elements.
	 */
	private EmptyElements getEmptyElements(DOMElement element, FormatElementCategory formatElementCategory) {
		EmptyElements emptyElements = getEmptyElements();
		if (emptyElements != EmptyElements.ignore) {
			if (element.isClosed() && element.isEmpty()) {
				// Element is empty and closed
				switch (emptyElements) {
				case expand:
				case collapse: {
					if (formatElementCategory == FormatElementCategory.PreserveSpace) {
						// preserve content
						if (element.hasChildNodes()) {
							// The element is empty and contains somes spaces which must be preserved
							return EmptyElements.ignore;
						}
					}
					return emptyElements;
				}
				default:
					return emptyElements;
				}
			}
		}
		return EmptyElements.ignore;
	}

	/**
	 * Return true if conditions are met to format according to the
	 * closingBracketNewLine setting.
	 *
	 * 1. splitAttribute must be set to true 2. there must be at least 2 attributes
	 * in the element
	 *
	 * @param element the DOM element
	 * @return true if should format according to closingBracketNewLine setting.
	 */
	private boolean shouldFormatClosingBracketNewLine(DOMElement element) {
		boolean isSingleAttribute = element.getAttributeNodes() != null ? element.getAttributeNodes().size() == 1
				: true;
		return (formatterDocument.getSharedSettings().getFormattingSettings().getClosingBracketNewLine()
				&& getSplitAttributes() != SplitAttributes.preserve && !isSingleAttribute);
	}

	private void replaceLeftSpacesWith(int from, int to, String replace, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(from, to, replace, edits);
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

	private void replaceLeftSpacesWithIndentationWithOffsetSpaces(int spaceCount, int from, int to,
			List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWithIndentationWithOffsetSpaces(spaceCount, from, to, true, edits);
	}

	private void removeLeftSpaces(int from, int to, List<TextEdit> edits) {
		formatterDocument.removeLeftSpaces(from, to, edits);
	}

	private void createTextEditIfNeeded(int from, int to, String expectedContent, List<TextEdit> edits) {
		formatterDocument.createTextEditIfNeeded(from, to, expectedContent, edits);
	}

	/**
	 * Returns true if the DOM document have some line break in the given range
	 * [from, to] and false otherwise.
	 *
	 * @param from the from offset range.
	 * @param to   the to offset range.
	 *
	 * @return true if the DOM document have some line break in the given range
	 *         [from, to] and false otherwise.
	 */
	private boolean hasLineBreak(int from, int to) {
		return formatterDocument.hasLineBreak(from, to);
	}

	/**
	 * Returns the last attribute of the given DOMelement and null otherwise.
	 *
	 * @param element the DOM element.
	 *
	 * @return the last attribute of the given DOMelement and null otherwise.
	 */
	private static DOMAttr getLastAttribute(DOMElement element) {
		if (!element.hasAttributes()) {
			return null;
		}
		List<DOMAttr> attributes = element.getAttributeNodes();
		return attributes.get(attributes.size() - 1);
	}

	private boolean isPreserveAttributeLineBreaks() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveAttributeLineBreaks();
	}

	private SplitAttributes getSplitAttributes() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributes();
	}

	private int getSplitAttributesIndentSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getSplitAttributesIndentSize();
	}

	private boolean isSpaceBeforeEmptyCloseTag() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isSpaceBeforeEmptyCloseTag();
	}

	private EmptyElements getEmptyElements() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getEmptyElements();
	}

	private void formatChildren(DOMElement element, XMLFormattingConstraints constraints, int start, int end,
			List<TextEdit> edits) {
		formatterDocument.formatChildren(element, constraints, start, end, edits);
	}

	private FormatElementCategory getFormatElementCategory(DOMElement element,
			XMLFormattingConstraints parentConstraints) {
		return formatterDocument.getFormatElementCategory(element, parentConstraints);
	}

	private boolean shouldCollapseEmptyElement(DOMElement element, SharedSettings settings) {
		return formatterDocument.shouldCollapseEmptyElement(element, settings);
	}

	private int getMaxLineWidth() {
		return formatterDocument.getMaxLineWidth();
	}

	private int getTabSize() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getTabSize();
	}
}
