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
 * Central coordinator for XML document formatting.
 *
 * <p>
 * Produces a list of {@link TextEdit} that transform the document's whitespace
 * (indentation, line breaks, trailing spaces) without altering content.
 * </p>
 *
 * <h3>Architecture</h3>
 * <p>
 * This class owns the document state and delegates node-specific formatting to
 * specialized formatters:
 * </p>
 * <ul>
 * <li>{@link DOMElementFormatter} — elements (start tag, attributes, end tag)</li>
 * <li>{@link DOMAttributeFormatter} — individual attributes</li>
 * <li>{@link DOMTextFormatter} — text nodes</li>
 * <li>{@link DOMCommentFormatter} — comments</li>
 * <li>{@link DOMCDATAFormatter} — CDATA sections</li>
 * <li>{@link DOMDocTypeFormatter} — DOCTYPE declarations</li>
 * <li>{@link DOMProcessingInstructionFormatter} — processing instructions</li>
 * </ul>
 *
 * <h3>Formatting flow</h3>
 * <ol>
 * <li>Determine the DOM node(s) covered by the requested range.</li>
 * <li>Walk siblings left-to-right, dispatching each node to its formatter.</li>
 * <li>Each formatter adjusts whitespace and tracks {@code availableLineWidth}
 * via {@link XMLFormattingConstraints} to enforce {@code maxLineWidth}.</li>
 * <li>After all nodes, handle {@code trimFinalNewlines},
 * {@code insertFinalNewline}, and {@code trimTrailingWhitespace}.</li>
 * </ol>
 *
 * <h3>Shared utilities</h3>
 * <p>
 * Provides indentation helpers ({@link #getIndentSpaces},
 * {@link #replaceLeftSpacesWithIndentation}), whitespace scanning
 * ({@link #adjustOffsetWithLeftWhitespaces}, {@link #hasLineBreak}), and
 * text edit creation ({@link #createTextEditIfNeeded}) used by all formatters.
 * </p>
 *
 * <h3>{@code @formatter:off/on}</h3>
 * <p>
 * Comments containing {@code @formatter:off} disable formatting until a
 * matching {@code @formatter:on} comment is found (see issue #1648).
 * </p>
 *
 * @author Angelo ZERR
 *
 * @see DOMElementFormatter
 * @see FormatElementCategory
 */
public class XMLFormatterDocument {

	private static final Logger LOGGER = Logger.getLogger(XMLFormatterDocument.class.getName());

	/** The {@code xml:space} attribute name. */
	private static final String XML_SPACE_ATTR = "xml:space";

	/** Value of {@code xml:space} that re-enables default whitespace handling. */
	private static final String XML_SPACE_ATTR_DEFAULT = "default";

	/** Value of {@code xml:space} that preserves all whitespace. */
	private static final String XML_SPACE_ATTR_PRESERVE = "preserve";

	/** The parsed DOM document being formatted. */
	private final DOMDocument xmlDocument;

	/** The underlying text document (provides offset/position conversions). */
	private final TextDocument textDocument;

	/** The line delimiter detected from the document (or system default). */
	private final String lineDelimiter;

	/** User settings: indent size, max line width, quote style, etc. */
	private final SharedSettings sharedSettings;

	/** Reusable StringBuilder for building indentation strings to reduce allocation. */
	private final StringBuilder indentBuilder;

	/** Formatter for processing instruction nodes ({@code <?target data?>}). */
	private final DOMProcessingInstructionFormatter processingInstructionFormatter;

	/** Formatter for DOCTYPE declarations ({@code <!DOCTYPE ...>}). */
	private final DOMDocTypeFormatter docTypeFormatter;

	/** Formatter for element nodes (start tag, children, end tag). */
	private final DOMElementFormatter elementFormatter;

	/** Formatter for individual attributes within element start tags. */
	private final DOMAttributeFormatter attributeFormatter;

	/** Formatter for text nodes (whitespace normalization, wrapping). */
	private final DOMTextFormatter textFormatter;

	/** Formatter for comment nodes ({@code <!-- ... -->}). */
	private final DOMCommentFormatter commentFormatter;

	/** Formatter for CDATA sections ({@code <![CDATA[...]]>}). */
	private final DOMCDATAFormatter cDATAFormatter;

	/** Extension-point participants that can override formatting behavior. */
	private final Collection<IFormatterParticipant> formatterParticipants;

	/** Cache of grammar documents keyed by namespace, used by formatter participants. */
	private final Map<String, Collection<CMDocument>> formattingContext;

	/** Start offset of the range to format, or -1 for beginning of document. */
	private int startOffset = -1;

	/** End offset of the range to format, or -1 for end of document. */
	private int endOffset = -1;

	/** Optional cancel checker for long-running format operations. */
	private CancelChecker cancelChecker;

	/** Marker text in comments that disables formatting. */
	private static final String FORMATTER_OFF = "@formatter:off";

	/** Marker text in comments that re-enables formatting. */
	private static final String FORMATTER_ON = "@formatter:on";

	/** True when formatting is disabled by a {@code <!-- @formatter:off -->} comment. */
	private boolean formatterOff;

	/**
	 * Returns true if formatting is currently turned off by a
	 * {@code <!-- @formatter:off -->} comment.
	 *
	 * @return true if formatting is off.
	 */
	public boolean isFormatterOff() {
		return formatterOff;
	}

	/**
	 * Creates a formatter for the given XML document.
	 *
	 * @param xmlDocument            the parsed DOM document to format.
	 * @param range                  the range to format, or {@code null} to format
	 *                               the entire document.
	 * @param sharedSettings         formatting options (indent size, max line width, etc.).
	 * @param formatterParticipants  extension-point participants that can override
	 *                               formatting behavior (e.g., XSD/DTD-aware formatting).
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

	/** Returns the line delimiter from the first line of the document, or the system default. */
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

	/**
	 * Formats a portion of the document, returning text edits.
	 *
	 * <p>Steps:</p>
	 * <ol>
	 * <li>Find the DOM node(s) covering [{@code start}, {@code end}].</li>
	 * <li>Initialize {@code availableLineWidth} from the node's column position.</li>
	 * <li>Walk siblings, dispatching each to its node-type formatter.</li>
	 * <li>Apply end-of-document rules (trim trailing whitespace, final newline).</li>
	 * </ol>
	 *
	 * @param document the DOM document.
	 * @param start    start offset of the range to format (-1 for beginning).
	 * @param end      end offset of the range to format (-1 for end).
	 * @return the list of text edits.
	 */
	public List<? extends TextEdit> format(DOMDocument document, int start, int end) {
		int estimatedCapacity = Math.min(textDocument.getTextSequence().length() / 100, 10000);
		List<TextEdit> edits = new ArrayList<>(estimatedCapacity);

		DOMNode currentDOMNode = getDOMNodeToFormat(document, start, end);

		if (currentDOMNode != null) {
			int startOffset = currentDOMNode.getStart();

			XMLFormattingConstraints parentConstraints = getNodeConstraints(currentDOMNode);
			if (isMaxLineWidthSupported()) {
				// Initialize available width = maxLineWidth minus the column
				// the node starts at (handles mid-line range formatting).
				int lineWidth = getMaxLineWidth();

				try {
					int lineOffset = textDocument.lineOffsetAt(startOffset);
					lineWidth = lineWidth - (startOffset - lineOffset);
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
				parentConstraints.setAvailableLineWidth(lineWidth);
			}

			// Format all siblings (and their children) that overlap [start, end]
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

	/** Returns true if {@code startNode} fully covers {@code endNode}, or they are the same node. */
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

	/**
	 * Formats the given DOM node and all its right siblings.
	 *
	 * <p>Use case: when formatting a range that starts mid-document, the first
	 * matching node and all its subsequent siblings are formatted.</p>
	 *
	 * @param edits             the list of text edits to populate.
	 * @param domNode           the first sibling to format.
	 * @param parentConstraints the parent's formatting constraints.
	 * @param start             start offset of the formatting range.
	 * @param end               end offset of the formatting range.
	 */
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

	/**
	 * Dispatches a single DOM node to the appropriate formatter based on its type.
	 *
	 * <p>Handles {@code @formatter:off/on} toggling: when a comment containing
	 * {@code @formatter:off} is encountered, subsequent nodes are skipped (only
	 * {@code availableLineWidth} is updated) until {@code @formatter:on}.</p>
	 *
	 * <p>Node type dispatch:</p>
	 * <ul>
	 * <li>{@code ELEMENT_NODE} → {@link DOMElementFormatter#formatElement}</li>
	 * <li>{@code TEXT_NODE} → {@link DOMTextFormatter#formatText}</li>
	 * <li>{@code COMMENT_NODE} → {@link DOMCommentFormatter#formatComment}</li>
	 * <li>{@code CDATA_SECTION_NODE} → {@link DOMCDATAFormatter#formatCDATASection}</li>
	 * <li>{@code DOCUMENT_TYPE_NODE} → {@link DOMDocTypeFormatter#formatDocType}</li>
	 * <li>{@code PROCESSING_INSTRUCTION_NODE} → {@link DOMProcessingInstructionFormatter}</li>
	 * <li>{@code DOCUMENT_NODE} → recurse into children</li>
	 * </ul>
	 *
	 * @param child             the DOM node to format.
	 * @param parentConstraints the parent's formatting constraints (modified in place).
	 * @param start             start offset of the formatting range.
	 * @param end               end offset of the formatting range.
	 * @param edits             the list of text edits to populate.
	 */
	public void format(DOMNode child, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		// @formatter:off/on toggling (issue #1648)
		if (child.getNodeType() == Node.COMMENT_NODE) {
			DOMComment comment = (DOMComment) child;
			if (!formatterOff && comment.containsText(FORMATTER_OFF)) {
				commentFormatter.formatComment(comment, parentConstraints, start, end, edits);
				formatterOff = true;
				return;
			}
			if (formatterOff && comment.containsText(FORMATTER_ON)) {
				formatterOff = false;
				if (isMaxLineWidthSupported()) {
					parentConstraints.setAvailableLineWidth(
							updateLineWidthWithLastLine(child, parentConstraints.getAvailableLineWidth()));
				}
				return;
			}
		}

		if (formatterOff) {
			// Formatting is off — skip but keep availableLineWidth in sync
			if (isMaxLineWidthSupported()) {
				parentConstraints.setAvailableLineWidth(
						updateLineWidthWithLastLine(child, parentConstraints.getAvailableLineWidth()));
			}
			return;
		}

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
				// Unknown node type — keep availableLineWidth in sync
				if (isMaxLineWidthSupported()) {
					int width = updateLineWidthWithLastLine(child, parentConstraints.getAvailableLineWidth());
					parentConstraints.setAvailableLineWidth(width);
				}
		}
	}

	/**
	 * Formats all child nodes of the given parent node.
	 *
	 * @param currentDOMNode    the parent whose children to format.
	 * @param parentConstraints the parent's formatting constraints.
	 * @param start             start offset of the formatting range.
	 * @param end               end offset of the formatting range.
	 * @param edits             the list of text edits to populate.
	 */
	public void formatChildren(DOMNode currentDOMNode, XMLFormattingConstraints parentConstraints, int start, int end,
			List<TextEdit> edits) {
		for (DOMNode child : currentDOMNode.children()) {
			format(child, parentConstraints, start, end, edits);
		}
	}

	/**
	 * Delegates attribute value formatting to registered
	 * {@link IFormatterParticipant}s.
	 *
	 * <p>Use case: XSD/DTD-aware participants may reformat enumerated attribute
	 * values or enforce quoting conventions specific to the schema.</p>
	 *
	 * @param attr              the attribute whose value to format.
	 * @param parentConstraints the parent element's formatting constraints.
	 * @param edits             the list of text edits to populate.
	 */
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

	/**
	 * Removes all whitespace to the left of {@code to}, bounded by {@code leftLimit}.
	 *
	 * @param leftLimit the leftmost offset to scan for whitespace.
	 * @param to        the offset where whitespace ends.
	 * @param edits     the list of text edits to populate.
	 */
	public void removeLeftSpaces(int leftLimit, int to, List<TextEdit> edits) {
		replaceLeftSpacesWith(leftLimit, to, "", edits);
	}

	/**
	 * Replaces all whitespace to the left of {@code to} with a single space.
	 *
	 * @param leftLimit the leftmost offset to scan for whitespace.
	 * @param to        the offset where whitespace ends.
	 * @param edits     the list of text edits to populate.
	 */
	public void replaceLeftSpacesWithOneSpace(int leftLimit, int to, List<TextEdit> edits) {
		replaceLeftSpacesWith(leftLimit, to, " ", edits);
	}

	/**
	 * Replaces all contiguous whitespace to the left of {@code to} with the given
	 * replacement string. Scans leftward from {@code to} up to {@code leftLimit}.
	 *
	 * @param leftLimit   the leftmost offset to scan.
	 * @param to          the offset where whitespace ends.
	 * @param replacement the string to replace the whitespace with.
	 * @param edits       the list of text edits to populate.
	 */
	void replaceLeftSpacesWith(int leftLimit, int to, String replacement, List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, to);
		if (from >= 0) {
			createTextEditIfNeeded(from, to, replacement, edits);
		}
	}

	/**
	 * Replaces a quote character at [{@code from}, {@code to}) with the user's
	 * preferred quotation character (single or double quote).
	 *
	 * @param from  the start offset of the existing quote.
	 * @param to    the end offset of the existing quote.
	 * @param edits the list of text edits to populate.
	 */
	void replaceQuoteWithPreferred(int from, int to, List<TextEdit> edits) {
		createTextEditIfNeeded(from, to, getQuotationAsString(), edits);
	}

	/**
	 * Scans leftward from {@code to} and returns the offset where contiguous
	 * whitespace begins, bounded by {@code leftLimit}.
	 *
	 * <p>Returns {@code -1} if no whitespace is found immediately before {@code to},
	 * or {@code to} itself if no whitespace exists at that position.</p>
	 *
	 * @param leftLimit the leftmost offset to scan.
	 * @param to        the offset to scan leftward from.
	 * @return the start offset of the whitespace, or -1 if none.
	 */
	public int adjustOffsetWithLeftWhitespaces(int leftLimit, int to) {
		return TextEditUtils.adjustOffsetWithLeftWhitespaces(leftLimit, to, textDocument.getTextSequence());
	}

	/**
	 * Replaces whitespace to the left of {@code to} with proper indentation.
	 *
	 * @param indentLevel      the indent level (each level = tabSize spaces or 1 tab).
	 * @param leftLimit        the leftmost offset to scan for whitespace.
	 * @param to               the offset where the indented content starts.
	 * @param addLineSeparator true to prepend a line separator before the indent.
	 * @param edits            the list of text edits to populate.
	 * @return the length of the replacement string, or 0 if no edit was needed.
	 */
	public int replaceLeftSpacesWithIndentation(int indentLevel, int leftLimit, int to, boolean addLineSeparator,
			List<TextEdit> edits) {
		return replaceLeftSpacesWithIndentation(indentLevel, leftLimit, to, addLineSeparator, false, edits);
	}

	/**
	 * Replaces whitespace to the left of {@code to} with proper indentation,
	 * optionally removing conflicting edits that overlap the same range.
	 *
	 * <p>Use case: in MixedContent overflow handling, when an element is moved
	 * to a new line, a prior edit for the same whitespace region may already
	 * exist and must be replaced.</p>
	 *
	 * @param indentLevel            the indent level.
	 * @param leftLimit              the leftmost offset to scan for whitespace.
	 * @param to                     the offset where the indented content starts.
	 * @param addLineSeparator       true to prepend a line separator.
	 * @param removeConflictingEdits true to remove existing edits covering the same range.
	 * @param edits                  the list of text edits to populate.
	 * @return the length of the replacement string, or 0 if no edit was needed.
	 */
	public int replaceLeftSpacesWithIndentation(int indentLevel, int leftLimit, int to, boolean addLineSeparator,
			boolean removeConflictingEdits, List<TextEdit> edits) {
		int from = adjustOffsetWithLeftWhitespaces(leftLimit, to);
		if (from >= 0) {
			if (removeConflictingEdits) {
				removeEditsForRange(from, to, edits);
			}
			String expectedSpaces = getIndentSpaces(indentLevel, addLineSeparator);
			createTextEditIfNeeded(from, to, expectedSpaces, edits);
			return expectedSpaces.length();
		}
		return 0;
	}

	/**
	 * Removes any existing text edits that cover exactly the range [{@code from}, {@code to}).
	 *
	 * @param from  the start offset of the range.
	 * @param to    the end offset of the range.
	 * @param edits the list of text edits to filter.
	 */
	private void removeEditsForRange(int from, int to, List<TextEdit> edits) {
		try {
			Position startPos = textDocument.positionAt(from);
			Position endPos = textDocument.positionAt(to);
			edits.removeIf(e -> e.getRange().getStart().equals(startPos)
					&& e.getRange().getEnd().equals(endPos));
		} catch (BadLocationException e) {
			// ignore
		}
	}

	/**
	 * Replaces whitespace to the left of {@code offset} with indentation preceded
	 * by the specified number of new lines.
	 *
	 * <p>Use case: PreservedNewlines — when the user has blank lines between
	 * elements, this keeps up to {@code preservedNewlines} blank lines.</p>
	 *
	 * @param indentLevel  the indent level.
	 * @param leftLimit    the leftmost offset to scan for whitespace.
	 * @param offset       the offset where the indented content starts.
	 * @param newLineCount the number of line separators to insert.
	 * @param edits        the list of text edits to populate.
	 * @return the length of the replacement string, or 0 if no edit was needed.
	 */
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

	/**
	 * Replaces whitespace to the left of {@code to} with indentation computed
	 * from a raw space count rather than an indent level.
	 *
	 * <p>Use case: aligning continuation content (e.g., multi-line attribute
	 * values or DOCTYPE internal subsets) to a specific column.</p>
	 *
	 * @param indentSpace      the number of spaces to indent.
	 * @param leftLimit        the leftmost offset to scan for whitespace.
	 * @param to               the offset where the indented content starts.
	 * @param addLineSeparator true to prepend a line separator.
	 * @param edits            the list of text edits to populate.
	 * @return the length of the replacement string, or 0 if no edit was needed.
	 */
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

	/**
	 * Replaces whitespace at [{@code spaceStart}, {@code spaceEnd}) with
	 * indentation while preserving existing blank lines up to the
	 * {@code preservedNewlines} setting.
	 *
	 * <p>Use case: MixedContent indentation normalization — when multi-line
	 * whitespace exists before an element, the line breaks are kept (up to
	 * the limit) but the indentation on the last line is corrected.</p>
	 *
	 * @param spaceStart  the start offset of the whitespace region.
	 * @param spaceEnd    the end offset of the whitespace region.
	 * @param indentLevel the indent level.
	 * @param edits       the list of text edits to populate.
	 */
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

	/**
	 * Returns true if the text between [{@code from}, {@code to}) contains
	 * a line break character ({@code \r} or {@code \n}).
	 *
	 * <p>Use case: in MixedContent overflow handling, distinguishes inline
	 * spacing ({@code </b> <i>}) from multi-line whitespace. Only multi-line
	 * whitespace triggers wrapping to a new line.</p>
	 *
	 * @param from the start offset (inclusive).
	 * @param to   the end offset (exclusive).
	 * @return true if a line break exists in the range.
	 */
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

	/**
	 * Returns the normalized length of text in [{@code from}, {@code to}),
	 * collapsing runs of whitespace to a single space.
	 *
	 * <p>Use case: NormalizeSpace text nodes — computes the effective width
	 * after whitespace normalization to track {@code availableLineWidth}.</p>
	 *
	 * @param from the start offset (inclusive).
	 * @param to   the end offset (exclusive).
	 * @return the normalized character count.
	 */
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

	/**
	 * Adjusts an offset to account for existing line breaks and indentation
	 * when preserving the original line structure.
	 *
	 * <p>Use case: attribute formatting with {@code preserveAttributeLineBreaks}
	 * — computes the effective column position after existing whitespace,
	 * accounting for tab width.</p>
	 *
	 * @param from           the start offset to scan from.
	 * @param to             the current offset to adjust.
	 * @param tabSize        the tab size (number of spaces per tab).
	 * @param isInsertSpaces true if spaces are used instead of tabs.
	 * @return the adjusted offset.
	 */
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

	// ------- Line width tracking utilities -------

	/**
	 * Updates {@code availableLineWidth} by scanning the last line of the given
	 * node's text representation.
	 *
	 * <p>If the node ends with a line break, resets to {@code maxLineWidth}.
	 * Otherwise, decrements the width by the number of characters on the last line.</p>
	 *
	 * <p>Use case: when formatting is off ({@code @formatter:off}) or for unknown
	 * node types, the line width must still be tracked accurately so subsequent
	 * nodes can detect overflow.</p>
	 *
	 * @param child              the DOM node whose text was just processed.
	 * @param availableLineWidth the current available line width.
	 * @return the updated available line width.
	 */
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

	/** Returns true if the character is a line separator ({@code \r} or {@code \n}). */
	private static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

	/**
	 * Returns the offset of the first line break between {@code startAttr} and
	 * {@code start}, or -1 if none exists.
	 *
	 * <p>Use case: attribute formatting — detects whether existing content
	 * already has a line break to decide split strategy.</p>
	 *
	 * @param startAttr the start offset to scan from.
	 * @param start     the end offset to scan to (exclusive).
	 * @return the offset of the first line break, or -1 if none.
	 */
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

	/**
	 * Inserts a line break by replacing content at [{@code start}, {@code end})
	 * with the document's line delimiter.
	 *
	 * @param start the start offset.
	 * @param end   the end offset.
	 * @param edits the list of text edits to populate.
	 */
	void insertLineBreak(int start, int end, List<TextEdit> edits) {
		createTextEditIfNeeded(start, end, lineDelimiter, edits);
	}

	/**
	 * Collapses multiple consecutive spaces into a single space.
	 *
	 * <p>Use case: NormalizeSpace text — {@code <foo>a   b</foo>} becomes
	 * {@code <foo>a b</foo>}.</p>
	 *
	 * @param spaceStart the start offset of the first whitespace character, or -1 if none.
	 * @param spaceEnd   the end offset of the last whitespace character.
	 * @param edits      the list of text edits to populate.
	 */
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
		for (DOMNode child : element.children()) {
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

	/**
	 * Creates a {@link TextEdit} replacing [{@code from}, {@code to}) with
	 * {@code expectedContent}, but only if the existing text differs.
	 *
	 * <p>This is the single point of edit creation — all formatting methods
	 * funnel through here, ensuring idempotency (no edits when already formatted).</p>
	 *
	 * @param from            the start offset of the region to replace.
	 * @param to              the end offset of the region to replace.
	 * @param expectedContent the desired replacement text.
	 * @param edits           the list of text edits to populate.
	 */
	void createTextEditIfNeeded(int from, int to, String expectedContent, List<TextEdit> edits) {
		TextEdit edit = TextEditUtils.createTextEditIfNeeded(from, to, expectedContent, textDocument);
		if (edit != null) {
			edits.add(edit);
		}
	}

	/**
	 * Returns true if the given empty element should be collapsed to
	 * self-closing form ({@code <foo></foo>} → {@code <foo />}).
	 *
	 * <p>Delegates to {@link IFormatterParticipant}s — any participant can
	 * veto collapsing (e.g., HTML-aware formatters for void elements).</p>
	 *
	 * @param element        the empty element to check.
	 * @param sharedSettings the shared settings.
	 * @return true if the element should be collapsed to self-closing.
	 */
	public boolean shouldCollapseEmptyElement(DOMElement element, SharedSettings sharedSettings) {
		for (IFormatterParticipant participant : formatterParticipants) {
			if (!participant.shouldCollapseEmptyElement(element, sharedSettings)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Inserts a line separator followed by indentation at the given offset
	 * (no existing whitespace is replaced — this is a pure insertion).
	 *
	 * <p>Use case: MixedContent adjacent elements — when {@code </a><b>} has
	 * no whitespace between elements and {@code <b>} overflows, indentation
	 * is inserted before {@code <b>}.</p>
	 *
	 * @param indentLevel the indent level.
	 * @param offset      the offset at which to insert.
	 * @param edits       the list of text edits to populate.
	 */
	public void insertIndentation(int indentLevel, int offset, List<TextEdit> edits) {
		String indent = getIndentSpaces(indentLevel, true);
		createTextEditIfNeeded(offset, offset, indent, edits);
	}

	/**
	 * Builds an indentation string for the given indent level.
	 *
	 * @param level            the indent level (each level = tabSize spaces or 1 tab).
	 * @param addLineSeparator true to prepend the document's line delimiter.
	 * @return the indentation string.
	 */
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

	/**
	 * Builds an indentation string from a raw space count, using tabs where
	 * possible and spaces for the remainder.
	 *
	 * @param spaceCount       the total number of spaces to indent.
	 * @param addLineSeparator true to prepend the document's line delimiter.
	 * @return the indentation string.
	 */
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

	/**
	 * Removes trailing blank lines at the end of the document.
	 *
	 * <p>If {@code insertFinalNewline} is true, one trailing newline is preserved.</p>
	 *
	 * @param insertFinalNewline true to preserve one trailing newline.
	 * @param edits              the list of text edits to populate.
	 */
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
			if (!Character.isWhitespace(text.charAt(i - 1))) {
				if (!delimiterHasTwoCharacters) {
					String c = String.valueOf(text.charAt(i));
					if (delimiter.equals(c)) {
						newLineCounter++;
					}
				}
				return newLineCounter;
			}
			if (delimiterHasTwoCharacters) {
				CharSequence c = text.subSequence(i - 2, i);
				if (delimiter.contentEquals(c)) {
					newLineCounter++;
					i--; // skip the second char of the delimiter
				}
			} else {
				String c = String.valueOf(text.charAt(i));
				if (delimiter.equals(c)) {
					newLineCounter++;
				}
			}
		}
		return newLineCounter;
	}

	/** Returns true if {@code maxLineWidth} is set (non-zero). */
	public boolean isMaxLineWidthSupported() {
		return getMaxLineWidth() != 0;
	}

	/**
	 * Returns the available line width at the start of a new line
	 * at the given indent level: {@code maxLineWidth - indentLevel * tabSize}.
	 *
	 * <p>Single source of truth for this calculation, used by
	 * {@link #resetLineWidth} and text formatters that track width locally.</p>
	 *
	 * @param indentLevel the indent level of the new line.
	 * @return the available width after indentation.
	 */
	public int getNewLineAvailableWidth(int indentLevel) {
		return getMaxLineWidth() - indentLevel * getTabSize();
	}

	/**
	 * Resets the available line width in the given constraints for a new line
	 * at the specified indent level.
	 *
	 * <p>Call this after inserting a line break (via
	 * {@link #replaceLeftSpacesWithIndentation} or
	 * {@link #replaceLeftSpacesWithIndentationPreservedNewLines}) instead of
	 * manually calling {@code setAvailableLineWidth(getMaxLineWidth())} and
	 * adding {@code indentLevel * tabSize} to the width accumulator.</p>
	 *
	 * @param constraints the constraints to reset.
	 * @param indentLevel the indent level of the new line.
	 */
	public void resetLineWidth(XMLFormattingConstraints constraints, int indentLevel) {
		constraints.setAvailableLineWidth(getNewLineAvailableWidth(indentLevel));
	}

	/** Returns the configured maximum line width, or 0 if disabled. */
	public int getMaxLineWidth() {
		return getFormattingSettings().getMaxLineWidth();
	}

	/** Returns the tab size (number of spaces per indent level). */
	private int getTabSize() {
		return getFormattingSettings().getTabSize();
	}

	/** Returns true if spaces are used for indentation instead of tabs. */
	private boolean isInsertSpaces() {
		return getFormattingSettings().isInsertSpaces();
	}

	/** Returns true if trailing blank lines at end of document should be removed. */
	private boolean isTrimFinalNewlines() {
		return getFormattingSettings().isTrimFinalNewlines();
	}

	/** Returns true if the document should end with a newline. */
	private boolean isInsertFinalNewline() {
		return getFormattingSettings().isInsertFinalNewline();
	}

	/** Returns true if trailing whitespace on each line should be removed. */
	private boolean isTrimTrailingWhitespace() {
		return getFormattingSettings().isTrimTrailingWhitespace();
	}

	/** Returns the preferred quote character as a string. */
	private String getQuotationAsString() {
		return sharedSettings.getPreferences().getQuotationAsString();
	}

	/** Returns the XML formatting options. */
	private XMLFormattingOptions getFormattingSettings() {
		return getSharedSettings().getFormattingSettings();
	}

	/** Returns the shared settings (formatting + preferences). */
	SharedSettings getSharedSettings() {
		return sharedSettings;
	}

	/** Returns the document's line delimiter ({@code \n}, {@code \r\n}, etc.). */
	String getLineDelimiter() {
		return lineDelimiter;
	}

	/** Returns the document's text as a {@link CharSequence}. */
	CharSequence getTextSequence() {
		return textDocument.getTextSequence();
	}

	/**
	 * Returns the line start offset for the line containing the given offset.
	 *
	 * @param offset the document offset.
	 * @return the line start offset, or -1 on error.
	 */
	public int getLineAtOffset(int offset) {
		try {
			return textDocument.lineOffsetAt(offset);
		} catch (BadLocationException e) {
			return -1;
		}
	}
}