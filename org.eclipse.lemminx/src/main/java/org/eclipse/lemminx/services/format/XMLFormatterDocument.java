/*******************************************************************************
* Copyright (c) 2022, 2023 Red Hat Inc. and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMCDATASection;
import org.eclipse.lemminx.dom.DOMComment;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMDocumentType;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMProcessingInstruction;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.services.extensions.format.IFormatterParticipant;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.TextEditUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * XML formatter which generates several text edit to remove, add,
 * update spaces / indent.
 * 
 * @author Angelo ZERR
 * 
 */
public class XMLFormatterDocument {

	private static final Logger LOGGER = Logger.getLogger(XMLFormatterDocument.class.getName());

	private static final String XML_SPACE_ATTR = "xml:space";

	private static final String XML_SPACE_ATTR_DEFAULT = "default";

	private static final String XML_SPACE_ATTR_PRESERVE = "preserve";

	private final DOMDocument xmlDocument;
	private final TextDocument textDocument;
	private final String lineDelimiter;
	private final SharedSettings sharedSettings;
	
	// Reusable StringBuilder for indentation to reduce object allocation
	private final StringBuilder indentBuilder;

	private final DOMProcessingInstructionFormatter processingInstructionFormatter;

	private final DOMDocTypeFormatter docTypeFormatter;

	private final DOMElementFormatter elementFormatter;

	private final DOMAttributeFormatter attributeFormatter;

	private final DOMTextFormatter textFormatter;

	private final DOMCommentFormatter commentFormatter;

	private final DOMCDATAFormatter cDATAFormatter;

	private final Collection<IFormatterParticipant> formatterParticipants;

	private final Map<String, Collection<CMDocument>> formattingContext;

	private int startOffset = -1;
	private int endOffset = -1;

	private CancelChecker cancelChecker;

	/**
	 * XML formatter document.
	 */
	public XMLFormatterDocument(DOMDocument xmlDocument, Range range, SharedSettings sharedSettings,
			Collection<IFormatterParticipant> formatterParticipants) {
		this.xmlDocument = xmlDocument;
		this.textDocument = xmlDocument.getTextDocument();
		this.lineDelimiter = computeLineDelimiter(textDocument);
		if (range != null) {
			try {
				startOffset = textDocument.offsetAt(range.getStart());
				endOffset = textDocument.offsetAt(range.getEnd());
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		this.sharedSettings = sharedSettings;
		this.formatterParticipants = formatterParticipants;
		this.docTypeFormatter = new DOMDocTypeFormatter(this);
		this.attributeFormatter = new DOMAttributeFormatter(this);
		this.elementFormatter = new DOMElementFormatter(this, attributeFormatter);
		this.processingInstructionFormatter = new DOMProcessingInstructionFormatter(this, attributeFormatter);
		this.textFormatter = new DOMTextFormatter(this);
		this.commentFormatter = new DOMCommentFormatter(this);
		this.cDATAFormatter = new DOMCDATAFormatter(this);
		this.formattingContext = new HashMap<>();
		// Pre-allocate reusable StringBuilder for indentation (max reasonable indent: 100 levels * 4 spaces)
		this.indentBuilder = new StringBuilder(400);
	}

	private static String computeLineDelimiter(TextDocument textDocument) {
		try {
			return textDocument.lineDelimiter(0);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return System.lineSeparator();
	}

	/**
	 * Returns a List containing multiple TextEdit, containing the newly formatted
	 * changes of an XML document.
	 * 
	 * @return List containing multiple TextEdit of an XML document.
	 * 
	 * @throws BadLocationException
	 */
	public List<? extends TextEdit> format() throws BadLocationException {
		return format(xmlDocument, startOffset, endOffset);
	}

	public List<? extends TextEdit> format(DOMDocument document, int start, int end) {
		// Pre-allocate list capacity based on document size to reduce reallocations
		// Estimate: 1 edit per 100 characters for typical XML formatting
		int estimatedCapacity = Math.min(textDocument.getTextLength() / 100, 10000);
		List<TextEdit> edits = new ArrayList<>(estimatedCapacity);

		// get initial document region
		DOMNode currentDOMNode = getDOMNodeToFormat(document, start, end);

		if (currentDOMNode != null) {
			int startOffset = currentDOMNode.getStart();

			XMLFormattingConstraints parentConstraints = getNodeConstraints(currentDOMNode);
			if (isMaxLineWidthSupported()) {
				// initialize available line width
				int lineWidth = getMaxLineWidth();

				try {
					int lineOffset = textDocument.lineOffsetAt(startOffset);
					lineWidth = lineWidth - (startOffset - lineOffset);
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
				parentConstraints.setAvailableLineWidth(lineWidth);
			}

			// format all siblings (and their children) as long they
			// overlap with start/end offset
			if (currentDOMNode.isElement()) {
				parentConstraints.setFormatElementCategory(getFormatElementCategory((DOMElement) currentDOMNode, null));
			} else {
				parentConstraints.setFormatElementCategory(FormatElementCategory.IgnoreSpace);
			}
			formatSiblings(edits, currentDOMNode, parentConstraints, start, end);
		}

		boolean insertFinalNewline = isInsertFinalNewline();
		CharSequence xml = textDocument.getTextSequence();
		int endDocument = xml.length() - 1;
		if (isTrimFinalNewlines() && (end == -1 || endDocument < end)) {
			trimFinalNewlines(insertFinalNewline, edits);
		}
		if (insertFinalNewline) {
			if (endDocument >= 0) {
				char c = xml.charAt(endDocument);
				if (c != '\n' && (end == -1 || endDocument < end)) {
					try {
						Position pos = textDocument.positionAt(endDocument);
						pos.setCharacter(pos.getCharacter() + 1);
						Range range = new Range(pos, pos);
						edits.add(new TextEdit(range, lineDelimiter));
					} catch (BadLocationException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
		if (isTrimTrailingWhitespace()) {
			int i = xml.length() - 1;
			int lineDelimiterOffset = i + 1;
			char curr = xml.charAt(i);
			boolean removeSpaces = true;

			// removes spaces and new lines at the end of xml
			if (isTrimFinalNewlines() && !isLineSeparator(curr) && (end == -1 || endDocument < end)) {
				while (Character.isWhitespace(curr) && i > 0) {
					i--;
					curr = xml.charAt(i);
				}
				removeLeftSpaces(i, lineDelimiterOffset, edits);
				removeSpaces = false;
			}
			if (!isTrimFinalNewlines()) {
				while (i >= 0) {
					curr = xml.charAt(i);
					if (isLineSeparator(curr)) {
						// remove spaces in an empty line
						// ex:
						// [space][space] --> remove
						if (removeSpaces) {
							removeLeftSpaces(i + 1, lineDelimiterOffset, edits);
						}
						removeSpaces = true;
						lineDelimiterOffset = i;
					} else if (removeSpaces && (!Character.isWhitespace(curr) || isLineSeparator(curr))) {
						// remove spaces after some content at the end of the line
						// ex: <a> </a> [space][space] --> remove
						removeLeftSpaces(i, lineDelimiterOffset, edits);
						removeSpaces = false;
						return edits;
					}
					i--;
				}
			}
		}
		return edits;
	}

	/**
	 * Returns the DOM node to format according to the given range and the DOM
	 * document otherwise.
	 * 
	 * @param document the DOM document.
	 * @param start    the start range offset and -1 otherwise.
	 * @param end      the end range offset and -1 otherwise.
	 * 
	 * @return the DOM node to format according to the given range and the DOM
	 *         document otherwise.
	 */
	private static DOMNode getDOMNodeToFormat(DOMDocument document, int start, int end) {
		if (start != -1 && end != -1) {
			DOMNode startNode = document.findNodeAt(start);
			DOMNode endNode = document.findNodeBefore(end);

			if (endNode.getStart() == start) {
				// ex :
				// <div>
				// |<img />|
				// </div>
				return endNode;
			}

			if (isCoverNode(startNode, endNode)) {
				return startNode;
			} else if (isCoverNode(endNode, startNode)) {
				return endNode;
			} else {
				DOMNode startParent = startNode.getParentNode();
				DOMNode endParent = endNode.getParentNode();
				while (startParent != null && endParent != null) {
					if (isCoverNode(startParent, endParent)) {
						return startParent;
					} else if (isCoverNode(endParent, startParent)) {
						return endParent;
					}
					startParent = startParent.getParentNode();
					endParent = endParent.getParentNode();
				}
			}
		}
		return document;
	}

	private static boolean isCoverNode(DOMNode startNode, DOMNode endNode) {
		return (startNode.getStart() < endNode.getStart() && startNode.getEnd() > endNode.getEnd())
				|| startNode == endNode;
	}

	/**
	 * Returns the DOM node constraints of the given DOM node.
	 * 
	 * @param node the DOM node.
	 * 
	 * @return the DOM node constraints of the given DOM node.
	 */
	private XMLFormattingConstraints getNodeConstraints(DOMNode node) {
		XMLFormattingConstraints result = new XMLFormattingConstraints();
		// Compute the indent level according to the parent node.
		int indentLevel = 0;
		while (node != null) {
			node = node.getParentElement();
			if (node != null) {
				indentLevel++;
			}
		}
		result.setIndentLevel(indentLevel);
		return result;
	}

	private void formatSiblings(List<TextEdit> edits, DOMNode domNode, XMLFormattingConstraints parentConstraints,
			int start, int end) {
		DOMNode currentDOMNode = domNode;
		while (currentDOMNode != null) {
			if (cancelChecker != null) {
				cancelChecker.checkCanceled();
			}
			format(currentDOMNode, parentConstraints, start, end, edits);
			currentDOMNode = currentDOMNode.getNextSibling();
		}
	}

	public void format(DOMNode child, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {

		switch (child.getNodeType()) {

			case Node.DOCUMENT_TYPE_NODE:
				DOMDocumentType docType = (DOMDocumentType) child;
				docTypeFormatter.formatDocType(docType, parentConstraints, start, end, edits);
				break;

			case Node.DOCUMENT_NODE:
				DOMDocument document = (DOMDocument) child;
				formatChildren(document, parentConstraints, start, end, edits);
				break;

			case DOMNode.PROCESSING_INSTRUCTION_NODE:
				DOMProcessingInstruction processingInstruction = (DOMProcessingInstruction) child;
				processingInstructionFormatter.formatProcessingInstruction(processingInstruction, parentConstraints,
						edits);
				break;

			case Node.ELEMENT_NODE:
				DOMElement element = (DOMElement) child;
				elementFormatter.formatElement(element, parentConstraints, start, end, edits);
				break;

			case Node.TEXT_NODE:
				DOMText textNode = (DOMText) child;
				textFormatter.formatText(textNode, parentConstraints, start, end, edits);
				break;

			case Node.COMMENT_NODE:
				DOMComment commentNode = (DOMComment) child;
				commentFormatter.formatComment(commentNode, parentConstraints, start, end, edits);
				break;

			case Node.CDATA_SECTION_NODE:
				DOMCDATASection cDATANode = (DOMCDATASection) child;
				cDATAFormatter.formatCDATASection(cDATANode, parentConstraints, edits);
				break;

			default:
				// unknown, so just leave alone for now but make sure to update
				// available line width
				if (isMaxLineWidthSupported()) {
					int width = updateLineWidthWithLastLine(child, parentConstraints.getAvailableLineWidth());
					parentConstraints.setAvailableLineWidth(width);
				}
		}
	}

	public void formatChildren(DOMNode currentDOMNode, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		for (DOMNode child : currentDOMNode.getChildren()) {
			format(child, parentConstraints, start, end, edits);
		}
	}

	public void formatAttributeValue(DOMAttr attr, XMLFormattingConstraints parentConstraints, List<TextEdit> edits) {
		if (formatterParticipants != null) {
			for (IFormatterParticipant formatterParticipant : formatterParticipants) {
				try {
					if (formatterParticipant.formatAttributeValue(attr, this, parentConstraints,
							getFormattingSettings(), edits)) {
						return;
					}
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error while processing format attributes for the participant '"
							+ formatterParticipant.getClass().getName() + "'.", e);
				}
			}
		}
	}

	public void removeLeftSpaces(int leftLimit, int to, List<TextEdit> edits) {
		replaceLeftSpacesWith(leftLimit, to, "", edits);
	}

	public void replaceLeftSpacesWithOneSpace(int leftLimit, int to, List<TextEdit> edits) {
		replaceLeftSpacesWith(leftLimit, to, " ", edits);
	}

	void replaceLeftSpacesWith(int leftLimit, int to, String replacement, List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, to);
		if (from >= 0) {
			createTextEditIfNeeded(from, to, replacement, edits);
		}
	}

	void replaceQuoteWithPreferred(int from, int to, List<TextEdit> edits) {
		createTextEditIfNeeded(from, to, getQuotationAsString(), edits);
	}

	public int adjustOffsetWithLeftWhitespaces(int leftLimit, int to) {
		return TextEditUtils.adjustOffsetWithLeftWhitespaces(leftLimit, to, textDocument.getTextSequence());
	}

	public int replaceLeftSpacesWithIndentation(int indentLevel, int leftLimit, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, to);
		if (from >= 0) {
			String expectedSpaces = getIndentSpaces(indentLevel, addLineSeparator);
			createTextEditIfNeeded(from, to, expectedSpaces, edits);
			return expectedSpaces.length();
		}
		return 0;
	}

	public int replaceLeftSpacesWithIndentationWithMultiNewLines(int indentLevel, int leftLimit, int offset,
			int newLineCount, List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, offset);
		if (from >= 0) {
			String expectedSpaces = getIndentSpacesWithMultiNewLines(indentLevel, newLineCount);
			createTextEditIfNeeded(from, offset, expectedSpaces, edits);
			return expectedSpaces.length();
		}
		return 0;
	}

	public int replaceLeftSpacesWithIndentationWithOffsetSpaces(int indentSpace, int leftLimit, int to,
			boolean addLineSeparator, List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, to);
		if (from >= 0) {
			String expectedSpaces = getIndentSpacesWithOffsetSpaces(indentSpace, addLineSeparator);
			createTextEditIfNeeded(from, to, expectedSpaces, edits);
			return expectedSpaces.length();
		}
		return 0;
	}

	public void replaceLeftSpacesWithIndentationPreservedNewLines(int spaceStart, int spaceEnd,
			int indentLevel, List<TextEdit> edits) {
		int preservedNewLines = getFormattingSettings().getPreservedNewlines();
		int currentNewLineCount = XMLFormatterDocument.getExistingNewLineCount(
				textDocument.getTextSequence(), spaceEnd, lineDelimiter);
		if (currentNewLineCount > preservedNewLines) {
			replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, spaceStart,
					spaceEnd, preservedNewLines + 1, edits);
		} else {
			int newLineCount = currentNewLineCount == 0 ? 1 : currentNewLineCount;
			replaceLeftSpacesWithIndentationWithMultiNewLines(indentLevel, spaceStart, spaceEnd,
					newLineCount, edits);
		}
	}

	boolean hasLineBreak(int from, int to) {
		CharSequence text = textDocument.getTextSequence();
		for (int i = from; i < to; i++) {
			char c = text.charAt(i);
			if (isLineSeparator(c)) {
				return true;
			}
		}
		return false;
	}

	public int getNormalizedLength(int from, int to) {
		CharSequence text = textDocument.getTextSequence();
		int contentOffset = 0;
		for (int i = from; i < to; i++) {
			if (Character.isWhitespace(text.charAt(i)) && !Character.isWhitespace(text.charAt(i + 1))) {
				to -= contentOffset;
				contentOffset = 0;
			} else if (Character.isWhitespace(text.charAt(i))) {
				contentOffset++;
			}
		}
		return to;
	}

	public int getOffsetWithPreserveLineBreaks(int from, int to, int tabSize, boolean isInsertSpaces) {
		int initialTo = to;
		CharSequence text = textDocument.getTextSequence();
		for (int i = to; i > from; i--) {
			if (text.charAt(i) == '\t') {
				to -= tabSize;
			} else if (isLineSeparator(text.charAt(i))) {
				int prevIndent = 0;
				for (int j = i + 1; j < initialTo; j++) {
					if (text.charAt(j) == '\t' && !isInsertSpaces) {
						prevIndent += tabSize;
					} else if (Character.isWhitespace(text.charAt(j))) {
						prevIndent++;
					} else {
						to += (prevIndent - tabSize);
						return to;
					}
				}
			} else if (text.charAt(i) == ' ' && StringUtils.isQuote(text.charAt(i - 1))) {
				int j = 1;
				while (text.charAt(i + j) == ' ') {
					to++;
					j++;
				}
				to--;
			} else {
				to--;
			}
		}
		return to;
	}

	// DTD formatting

	// ------- Utilities method

	int updateLineWidthWithLastLine(DOMNode child, int availableLineWidth) {
		CharSequence text = textDocument.getTextSequence();
		int lineWidth = availableLineWidth;
		int end = child.getEnd();
		// Check if next char after the end of the DOM node is a new line feed.
		if (end < text.length()) {
			char c = text.charAt(end);
			if (isLineSeparator(c)) {
				// ex: <?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n
				return getMaxLineWidth();
			}
		}
		for (int i = end - 1; i > child.getStart(); i--) {
			char c = text.charAt(i);
			if (isLineSeparator(c)) {
				return lineWidth;
			} else {
				lineWidth--;
			}
		}
		return lineWidth;
	}

	private static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

	public int getLineBreakOffset(int startAttr, int start) {
		CharSequence text = textDocument.getTextSequence();
		for (int i = startAttr; i < start; i++) {
			char c = text.charAt(i);
			if (isLineSeparator(c)) {
				return i;
			}
		}
		return -1;
	}

	void insertLineBreak(int start, int end, List<TextEdit> edits) {
		createTextEditIfNeeded(start, end, lineDelimiter, edits);
	}

	void replaceSpacesWithOneSpace(int spaceStart, int spaceEnd, List<TextEdit> edits) {
		if (spaceStart >= 0) {
			spaceEnd = spaceEnd == -1 ? spaceStart + 1 : spaceEnd + 1;
			// Replace several spaces with one space
			// <foo>a[space][space][space]b</foo>
			// --> <foo>a[space]b</foo>
			replaceLeftSpacesWithOneSpace(spaceStart, spaceEnd, edits);
		}
	}

	/**
	 * Returns the format element category of the given DOM element.
	 *
	 * @param element           the DOM element.
	 * @param parentConstraints the parent constraints.
	 *
	 * @return the format element category of the given DOM element.
	 */
	public FormatElementCategory getFormatElementCategory(DOMElement element,
			XMLFormattingConstraints parentConstraints) {
		if (!element.isClosed()) {
			return parentConstraints.getFormatElementCategory();
		}

		// Get the category from the settings
		FormatElementCategory fromSettings = getFormattingSettings().getFormatElementCategory(element);
		if (fromSettings != null) {
			return fromSettings;
		}

		// Get the category from the participants (ex : from the XSD/DTD grammar
		// information)
		for (IFormatterParticipant participant : formatterParticipants) {
			FormatElementCategory fromParticipant = participant.getFormatElementCategory(element, parentConstraints,
					formattingContext, sharedSettings);
			if (fromParticipant != null) {
				return fromParticipant;
			}
		}

		if (XML_SPACE_ATTR_PRESERVE.equals(element.getAttribute(XML_SPACE_ATTR))) {
			return FormatElementCategory.PreserveSpace;
		}

		if (parentConstraints != null) {
			if (parentConstraints.getFormatElementCategory() == FormatElementCategory.PreserveSpace) {
				if (!XML_SPACE_ATTR_DEFAULT.equals(element.getAttribute(XML_SPACE_ATTR))) {
					return FormatElementCategory.PreserveSpace;
				}
			}
		}

		boolean hasElement = false;
		boolean hasText = false;
		boolean onlySpaces = true;
		for (DOMNode child : element.getChildren()) {
			if (child.isElement() || child.isComment() || child.isProcessingInstruction()) {
				hasElement = true;
			} else if (child.isText()) {
				onlySpaces = ((Text) child).isElementContentWhitespace();
				if (!onlySpaces) {
					hasText = true;
				}
			}
			if (hasElement && hasText) {
				return FormatElementCategory.MixedContent;
			}
		}
		if (hasElement && onlySpaces) {
			return FormatElementCategory.IgnoreSpace;
		}
		return FormatElementCategory.NormalizeSpace;
	}

	void createTextEditIfNeeded(int from, int to, String expectedContent, List<TextEdit> edits) {
		TextEdit edit = TextEditUtils.createTextEditIfNeeded(from, to, expectedContent, textDocument);
		if (edit != null) {
			edits.add(edit);
		}
	}

	public boolean shouldCollapseEmptyElement(DOMElement element, SharedSettings sharedSettings) {
		for (IFormatterParticipant participant : formatterParticipants) {
			if (!participant.shouldCollapseEmptyElement(element, sharedSettings)) {
				return false;
			}
		}
		return true;
	}

	private String getIndentSpaces(int level, boolean addLineSeparator) {
		// Reuse StringBuilder to avoid object allocation
		indentBuilder.setLength(0);
		if (addLineSeparator) {
			indentBuilder.append(lineDelimiter);
		}

		for (int i = 0; i < level; i++) {
			if (isInsertSpaces()) {
				for (int j = 0; j < getTabSize(); j++) {
					indentBuilder.append(" ");
				}
			} else {
				indentBuilder.append("\t");
			}
		}
		return indentBuilder.toString();
	}

	/**
	 * Return the expected indent spaces and new lines with the specified number of
	 * new lines.
	 * 
	 * @param level        the indent level.
	 * @param newLineCount the number of new lines to be added.
	 * 
	 * @return the expected indent spaces and new lines with the specified number of
	 *         new lines.
	 */
	private String getIndentSpacesWithMultiNewLines(int level, int newLineCount) {
		// Reuse StringBuilder to avoid object allocation
		indentBuilder.setLength(0);
		while (newLineCount != 0) {
			indentBuilder.append(lineDelimiter);
			newLineCount--;
		}

		for (int i = 0; i < level; i++) {
			if (isInsertSpaces()) {
				for (int j = 0; j < getTabSize(); j++) {
					indentBuilder.append(" ");
				}
			} else {
				indentBuilder.append("\t");
			}
		}
		return indentBuilder.toString();
	}

	private String getIndentSpacesWithOffsetSpaces(int spaceCount, boolean addLineSeparator) {
		// Reuse StringBuilder to avoid object allocation
		indentBuilder.setLength(0);
		if (addLineSeparator) {
			indentBuilder.append(lineDelimiter);
		}
		int spaceOffset = spaceCount % getTabSize();

		for (int i = 0; i < spaceCount / getTabSize(); i++) {
			if (isInsertSpaces()) {
				for (int j = 0; j < getTabSize(); j++) {
					indentBuilder.append(" ");
				}
			} else {
				indentBuilder.append("\t");
			}
		}

		for (int i = 0; i < spaceOffset; i++) {
			indentBuilder.append(" ");
		}

		return indentBuilder.toString();
	}

	private void trimFinalNewlines(boolean insertFinalNewline, List<TextEdit> edits) {
		CharSequence xml = textDocument.getTextSequence();
		int end = xml.length() - 1;
		int i = end;
		while (i >= 0 && isLineSeparator(xml.charAt(i))) {
			i--;
		}
		if (end > i) {
			if (insertFinalNewline) {
				// re-adjust offset to keep insert final new line
				i++;
				if (xml.charAt(end - 1) == '\r') {
					i++;
				}
			}
			if (end > i) {
				try {
					Position endPos = textDocument.positionAt(end + 1);
					Position startPos = textDocument.positionAt(i + 1);
					Range range = new Range(startPos, endPos);
					edits.add(new TextEdit(range, ""));
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Return the number of new lines in the whitespaces to the left of the given
	 * offset.
	 *
	 * @param text      the xml text.
	 * @param offset    the offset to begin the count from.
	 * @param delimiter the delimiter.
	 *
	 * @return the number of new lines in the whitespaces to the left of the given
	 *         offset.
	 */
	public static int getExistingNewLineCount(CharSequence text, int offset, String delimiter) {
		boolean delimiterHasTwoCharacters = delimiter.length() == 2;
		int newLineCounter = 0;
		for (int i = offset; i > 1; i--) {
			CharSequence c;
			if (!Character.isWhitespace(text.charAt(i - 1))) {
				if (!delimiterHasTwoCharacters) {
					c = String.valueOf(text.charAt(i));
					if (delimiter.equals(c)) {
						newLineCounter++;
					}
				}
				return newLineCounter;
			}
			if (delimiterHasTwoCharacters) {
				c = text.subSequence(i - 2, i);
				if (c.equals(delimiter)) {
					newLineCounter++;
					i--; // skip the second char of the delimiter
				}
			} else {
				c = String.valueOf(text.charAt(i));
				if (delimiter.equals(c)) {
					newLineCounter++;
				}
			}
		}
		return newLineCounter;
	}

	public boolean isMaxLineWidthSupported() {
		return getMaxLineWidth() != 0;
	}

	public int getMaxLineWidth() {
		return getFormattingSettings().getMaxLineWidth();
	}

	private int getTabSize() {
		return getFormattingSettings().getTabSize();
	}

	private boolean isInsertSpaces() {
		return getFormattingSettings().isInsertSpaces();
	}

	private boolean isTrimFinalNewlines() {
		return getFormattingSettings().isTrimFinalNewlines();
	}

	private boolean isInsertFinalNewline() {
		return getFormattingSettings().isInsertFinalNewline();
	}

	private boolean isTrimTrailingWhitespace() {
		return getFormattingSettings().isTrimTrailingWhitespace();
	}

	private String getQuotationAsString() {
		return sharedSettings.getPreferences().getQuotationAsString();
	}

	private XMLFormattingOptions getFormattingSettings() {
		return getSharedSettings().getFormattingSettings();
	}

	SharedSettings getSharedSettings() {
		return sharedSettings;
	}

	String getLineDelimiter() {
		return lineDelimiter;
	}

	CharSequence getText() {
		return textDocument.getTextSequence();
	}

	public int getLineAtOffset(int offset) {
		try {
			return textDocument.lineOffsetAt(offset);
		} catch (BadLocationException e) {
			return -1;
		}
	}
}