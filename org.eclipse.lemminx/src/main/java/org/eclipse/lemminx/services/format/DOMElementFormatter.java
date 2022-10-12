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
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.EmptyElements;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.TextEdit;

/**
 * DOM element formatter.
 * 
 * @author Angelo ZERR
 *
 */
public class DOMElementFormatter {

	private final XMLFormatterDocumentNew formatterDocument;

	private final DOMAttributeFormatter attributeFormatter;

	public DOMElementFormatter(XMLFormatterDocumentNew formatterDocument, DOMAttributeFormatter attributeFormatter) {
		this.formatterDocument = formatterDocument;
		this.attributeFormatter = attributeFormatter;
	}

	public void formatElement(DOMElement element, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		EmptyElements emptyElements = getEmptyElements(element);

		// Format start tag element with proper indentation
		int indentLevel = parentConstraints.getIndentLevel();
		int nb = formatStartTagElement(element, parentConstraints, emptyElements, end, edits);

		if (emptyElements == EmptyElements.ignore) {
			// Format children of the element
			XMLFormattingConstraints constraints = new XMLFormattingConstraints();
			constraints.copyConstraints(parentConstraints);
			if ((element.isClosed())) {
				constraints.setIndentLevel(indentLevel + 1);
			}
			constraints.setFormatElementCategory(getFormatElementCategory(element, parentConstraints));
			constraints.setAvailableLineWidth(getMaxLineWidth() - nb);

			formatChildren(element, constraints, start, end, edits);

			// Format end tag element with proper indentation
			if (element.hasEndTag()) {
				formatEndTagElement(element, parentConstraints, constraints, edits);
			}
		}
	}

	private int formatStartTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			EmptyElements emptyElements, int end, List<TextEdit> edits) {
		if (!element.hasStartTag()) {
			// ex : </
			return element.getEnd() - element.getStart();
		}
		int width = 0;
		int indentLevel = parentConstraints.getIndentLevel();
		FormatElementCategory formatElementCategory = parentConstraints.getFormatElementCategory();
		int startTagOffset = element.getStartTagOpenOffset();
		boolean addLineSeparator = element.getParentElement() == null && element.getPreviousSibling() == null;
		if (end != -1 && startTagOffset > end) {
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
			int parentStartCloseOffset = element.getParentElement().getStartTagCloseOffset() + 1;
			if (parentStartCloseOffset != startTagOffset
					&& StringUtils.isWhitespace(formatterDocument.getText(), parentStartCloseOffset,
							startTagOffset)) {
				int nbSpaces = replaceLeftSpacesWithIndentation(indentLevel, parentStartCloseOffset, startTagOffset,
						!addLineSeparator, edits);
				width = element.getTagName() != null ? nbSpaces + element.getTagName().length() + 1 : nbSpaces;
				if (!addLineSeparator) {
					width -= formatterDocument.getLineDelimiter().length();
				}
			}
			break;
		case IgnoreSpace:
			// If preserve new lines
			int preservedNewLines = getPreservedNewlines();
			int currentNewLineCount = XMLFormatterDocumentNew.getExistingNewLineCount(formatterDocument.getText(),
					startTagOffset,
					formatterDocument.getLineDelimiter());
			if (currentNewLineCount > preservedNewLines) {
				replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, 0, startTagOffset,
						preservedNewLines + 1, edits);
			} else {
				// remove spaces and indent
				int nbSpaces = replaceLeftSpacesWithIndentation(indentLevel, 0, startTagOffset, !addLineSeparator,
						edits);
				width = element.getTagName() != null ? nbSpaces + element.getTagName().length() + 1 : nbSpaces;
				if (!addLineSeparator) {
					width -= formatterDocument.getLineDelimiter().length();
				}
			}
		case NormalizeSpace:
			width++;
			break;
		}
		parentConstraints.setAvailableLineWidth(parentConstraints.getAvailableLineWidth() - width);
		if (formatElementCategory != FormatElementCategory.PreserveSpace) {
			formatAttributes(element, parentConstraints, edits);

			boolean formatted = false;
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
				}
				break;
			}
			default:
			}

			if (!formatted) {
				if (element.isStartTagClosed() || element.isSelfClosed()) {
					formatElementStartTagOrSelfClosed(element, parentConstraints, edits);
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
			for (DOMAttr attr : attributes) {
				// Format current attribute
				attributeFormatter.formatAttribute(attr, prevOffset, singleAttribute, true, parentConstraints, edits);
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
	private void formatElementStartTagOrSelfClosed(DOMElement element, XMLFormattingConstraints parentConstraints,
			List<TextEdit> edits) {
		// <foo| >
		// <foo| />
		int startTagClose = element.getOffsetBeforeCloseOfStartTag();
		// <foo |>
		// <foo |/>
		int startTagOpen = element.getOffsetAfterStartTag();
		String replace = "";
		boolean spaceBeforeEmptyCloseTag = isSpaceBeforeEmptyCloseTag();
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
				return;
			}
		} else if (shouldFormatClosingBracketNewLine(element)) {
			int indentLevel = parentConstraints.getIndentLevel();
			replaceLeftSpacesWithIndentation(indentLevel + getSplitAttributesIndentSize(), startTagOpen, startTagClose,
					true, edits);
			return;
		}
		if (element.isSelfClosed()) {
			if (spaceBeforeEmptyCloseTag) {
				// <foo attr1=""/> --> <foo attr1=""[space] />
				replace = replace + " ";
			}
		}
		// remove spaces from the offset of start tag and start tag close
		// <foo|[space][space]|> --> <foo>
		// <foo attr1="" attr2="" |[space][space]|> --> <foo>
		replaceLeftSpacesWith(startTagOpen, startTagClose, replace, edits);
	}

	private int formatEndTagElement(DOMElement element, XMLFormattingConstraints parentConstraints,
			XMLFormattingConstraints constraints, List<TextEdit> edits) {
		// 1) remove / add some spaces on the left of the end tag element
		// before formatting : [space][space]</a>
		// after formatting : </a>
		int indentLevel = parentConstraints.getIndentLevel();
		FormatElementCategory formatElementCategory = constraints.getFormatElementCategory();
		int endTagOffset = element.getEndTagOpenOffset();

		switch (formatElementCategory) {
		case PreserveSpace:
			// Preserve existing spaces
			break;
		case MixedContent:
			// Remove spaces and indent if the last child is an element, not text
			// before formatting: <a> example text <b> </b> [space][space]</a>
			// after formatting: <a> example text <b> </b>\n</a>
			if ((element.getLastChild().isElement() || element.getLastChild().isComment())
					&& Character.isWhitespace(formatterDocument.getText().charAt(endTagOffset - 1))
					&& !isPreserveEmptyContent()) {
				replaceLeftSpacesWithIndentation(indentLevel, element.getStartTagCloseOffset(), endTagOffset, true,
						edits);
			}
			break;
		case IgnoreSpace:
			// If preserve new lines
			int preservedNewLines = getPreservedNewlines();
			int currentNewLineCount = XMLFormatterDocumentNew.getExistingNewLineCount(formatterDocument.getText(),
					endTagOffset,
					formatterDocument.getLineDelimiter());
			if (currentNewLineCount > preservedNewLines) {
				replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, element.getStartTagCloseOffset(),
						endTagOffset, preservedNewLines + 1, edits);
			} else {
				// remove spaces and indent
				replaceLeftSpacesWithIndentation(indentLevel, element.getStartTagCloseOffset(), endTagOffset, true,
						edits);
				break;
			}
		case NormalizeSpace:
			break;
		}
		// 2) remove some spaces between the end tag and and close bracket
		// before formatting : <a></a[space][space]>
		// after formatting : <a></a>
		if (element.isEndTagClosed()) {
			int endTagCloseOffset = element.getEndTagCloseOffset();
			removeLeftSpaces(element.getEndTagOpenOffset(), endTagCloseOffset, edits);
		}
		return 0;
	}

	/**
	 * Return the option to use to generate empty elements.
	 * 
	 * @param element the DOM element
	 * @return the option to use to generate empty elements.
	 */
	private EmptyElements getEmptyElements(DOMElement element) {
		EmptyElements emptyElements = getEmptyElements();
		if (emptyElements != EmptyElements.ignore) {
			if (element.isClosed() && element.isEmpty()) {
				// Element is empty and closed
				switch (emptyElements) {
				case expand:
				case collapse: {
					if (isPreserveEmptyContent()) {
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
				&& isSplitAttributes() && !isSingleAttribute);
	}

	private void replaceLeftSpacesWith(int from, int to, String replace, List<TextEdit> edits) {
		formatterDocument.replaceLeftSpacesWith(from, to, replace, edits);
	}

	private int replaceLeftSpacesWithIndentation(int indentLevel, int from, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentation(indentLevel, from, to, addLineSeparator, edits);
	}

	private int replaceLeftSpacesWithIndentationWithMultiNewLines(int indentLevel, int from, int to, int newLineCount,
			List<TextEdit> edits) {
		return formatterDocument.replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, from, to, newLineCount,
				edits);
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

	private int getPreservedNewlines() {
		return formatterDocument.getSharedSettings().getFormattingSettings().getPreservedNewlines();
	}

	private boolean isPreserveAttributeLineBreaks() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveAttributeLineBreaks();
	}

	private boolean isSplitAttributes() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isSplitAttributes();
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

	private boolean isPreserveEmptyContent() {
		return formatterDocument.getSharedSettings().getFormattingSettings().isPreserveEmptyContent();
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
}
