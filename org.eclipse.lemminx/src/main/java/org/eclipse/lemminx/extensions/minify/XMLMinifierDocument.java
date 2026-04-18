/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.minify;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMComment;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMProcessingInstruction;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.w3c.dom.Node;

/**
 * XML minifier which generates several text edits to remove unnecessary spaces.
 *
 * @author Angelo ZERR
 *
 */
public class XMLMinifierDocument {

	private static final Logger LOGGER = Logger.getLogger(XMLMinifierDocument.class.getName());

	private static final String XML_SPACE_ATTR = "xml:space";
	private static final String XML_SPACE_ATTR_PRESERVE = "preserve";

	private final DOMDocument xmlDocument;
	private final TextDocument textDocument;
	private final SharedSettings sharedSettings;

	private int startOffset = -1;
	private int endOffset = -1;

	/**
	 * XML minifier document.
	 */
	public XMLMinifierDocument(DOMDocument xmlDocument, Range range, SharedSettings sharedSettings) {
		this.xmlDocument = xmlDocument;
		this.textDocument = xmlDocument.getTextDocument();
		this.sharedSettings = sharedSettings;

		if (range != null) {
			try {
				this.startOffset = textDocument.offsetAt(range.getStart());
				this.endOffset = textDocument.offsetAt(range.getEnd());
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	/**
	 * Returns the list of TextEdits to minify the XML document.
	 *
	 * @return the list of TextEdits to minify the XML document.
	 * @throws BadLocationException
	 */
	public List<? extends TextEdit> minify() throws BadLocationException {
		return minify(xmlDocument, startOffset, endOffset);
	}

	public List<? extends TextEdit> minify(DOMDocument document, int start, int end) {
		// Pre-allocate list capacity based on document size
		int estimatedCapacity = Math.min(textDocument.getText().length() / 100, 10000);
		List<TextEdit> edits = new ArrayList<>(estimatedCapacity);

		// Get initial document region
		DOMNode currentDOMNode = getDOMNodeToMinify(document, start, end);

		if (currentDOMNode != null) {
			// Minify all siblings (and their children) as long as they overlap with
			// start/end offset
			minifySiblings(edits, currentDOMNode, start, end);
		}

		return edits;
	}

	/**
	 * Returns the DOM node to minify.
	 *
	 * @param document the DOM document
	 * @param start    the start offset
	 * @param end      the end offset
	 * @return the DOM node to minify
	 */
	private DOMNode getDOMNodeToMinify(DOMDocument document, int start, int end) {
		if (start == -1) {
			// No range specified, minify entire document
			// Start from the first child of the document (could be PI, comment, or element)
			return document.getFirstChild();
		}

		// Find the node at the start offset
		DOMNode node = document.findNodeAt(start);
		if (node == null) {
			return document.getDocumentElement();
		}

		// Navigate up to find the first element or significant node
		while (node != null && node.getParentNode() != null) {
			if (node.isElement() || node.getParentNode() == document) {
				return node;
			}
			node = node.getParentNode();
		}

		return node;
	}

	/**
	 * Minify siblings of the given node.
	 *
	 * @param edits the list of text edits
	 * @param node  the starting node
	 * @param start the start offset
	 * @param end   the end offset
	 */
	private void minifySiblings(List<TextEdit> edits, DOMNode node, int start, int end) {
		DOMNode current = node;

		while (current != null) {
			// Check if we're still within the range
			if (end != -1 && current.getStart() > end) {
				break;
			}

			// Minify the current node
			minifyNode(edits, current, start, end);

			// Remove whitespace between current node and next sibling
			DOMNode nextSibling = current.getNextSibling();
			if (nextSibling != null) {
				int afterCurrent = current.getEnd();
				int beforeNext = nextSibling.getStart();
				if (afterCurrent < beforeNext) {
					removeWhitespace(edits, afterCurrent, beforeNext);
				}
			}

			// Move to next sibling
			current = nextSibling;
		}
	}

	/**
	 * Minify a single DOM node.
	 *
	 * @param edits the list of text edits
	 * @param node  the node to minify
	 * @param start the start offset
	 * @param end   the end offset
	 */
	private void minifyNode(List<TextEdit> edits, DOMNode node, int start, int end) {
		if (node == null) {
			return;
		}

		short nodeType = node.getNodeType();

		switch (nodeType) {
		case Node.ELEMENT_NODE:
			minifyElement(edits, (DOMElement) node, start, end);
			break;
		case Node.TEXT_NODE:
			minifyText(edits, (DOMText) node);
			break;
		case Node.COMMENT_NODE:
			// Remove comments during minification
			minifyComment(edits, (DOMComment) node);
			break;
		case Node.CDATA_SECTION_NODE:
			// Keep CDATA as-is (content must be preserved)
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			minifyProcessingInstruction(edits, (DOMProcessingInstruction) node);
			break;
		default:
			break;
		}
	}

	/**
	 * Minify a comment node by removing it.
	 *
	 * @param edits   the list of text edits
	 * @param comment the comment node
	 */
	private void minifyComment(List<TextEdit> edits, DOMComment comment) {
		// Remove the entire comment
		removeContent(edits, comment.getStart(), comment.getEnd());
	}

	/**
	 * Minify a processing instruction node.
	 *
	 * @param edits the list of text edits
	 * @param pi    the processing instruction
	 */
	private void minifyProcessingInstruction(List<TextEdit> edits, DOMProcessingInstruction pi) {
		// Processing instruction content is preserved
		// Whitespace after it is handled in minifySiblings
	}

	/**
	 * Minify an element node.
	 *
	 * @param edits   the list of text edits
	 * @param element the element to minify
	 * @param start   the start offset
	 * @param end     the end offset
	 */
	private void minifyElement(List<TextEdit> edits, DOMElement element, int start, int end) {
		// Check if xml:space="preserve" is set
		if (shouldPreserveSpace(element)) {
			return;
		}

		// Minify attributes (remove spaces between attributes if needed)
		minifyAttributes(edits, element);

		// Minify whitespace between start tag and content
		if (element.hasChildNodes()) {
			DOMNode firstChild = element.getFirstChild();
			if (firstChild != null) {
				// Remove whitespace between opening tag and first child
				int afterStartTag = element.getStartTagCloseOffset() + 1;
				int beforeFirstChild = firstChild.getStart();
				if (afterStartTag < beforeFirstChild) {
					removeWhitespace(edits, afterStartTag, beforeFirstChild);
				}
			}

			// Minify children
			DOMNode child = element.getFirstChild();
			while (child != null) {
				minifyNode(edits, child, start, end);

				// Remove whitespace between siblings
				DOMNode nextSibling = child.getNextSibling();
				if (nextSibling != null) {
					int afterChild = child.getEnd();
					int beforeNext = nextSibling.getStart();
					if (afterChild < beforeNext) {
						removeWhitespace(edits, afterChild, beforeNext);
					}
				}

				child = nextSibling;
			}

			// Remove whitespace between last child and closing tag
			DOMNode lastChild = element.getLastChild();
			if (lastChild != null && element.getEndTagOpenOffset() != DOMNode.NULL_VALUE) {
				int afterLastChild = lastChild.getEnd();
				int beforeEndTag = element.getEndTagOpenOffset();
				if (afterLastChild < beforeEndTag) {
					removeWhitespace(edits, afterLastChild, beforeEndTag);
				}
			}
		}
	}

	/**
	 * Minify attributes by removing unnecessary spaces.
	 *
	 * @param edits   the list of text edits
	 * @param element the element
	 */
	private void minifyAttributes(List<TextEdit> edits, DOMElement element) {
		if (!element.hasAttributes()) {
			return;
		}

		List<DOMAttr> attributes = element.getAttributeNodes();

		// Remove extra spaces between element name and first attribute
		if (!attributes.isEmpty()) {
			DOMAttr firstAttr = attributes.get(0);
			int afterElementName = element.getStartTagOpenOffset() + element.getTagName().length() + 1;
			int beforeFirstAttr = firstAttr.getStart();

			if (afterElementName < beforeFirstAttr) {
				int length = beforeFirstAttr - afterElementName;
				if (StringUtils.isWhitespace(textDocument.getText(), afterElementName, beforeFirstAttr) && length > 1) {
					// Replace multiple spaces with single space
					try {
						Range range = new Range(textDocument.positionAt(afterElementName),
								textDocument.positionAt(beforeFirstAttr));
						edits.add(new TextEdit(range, " "));
					} catch (BadLocationException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}

		// Remove extra spaces between attributes, keep only one space
		for (int i = 0; i < attributes.size() - 1; i++) {
			DOMAttr attr = attributes.get(i);
			DOMAttr nextAttr = attributes.get(i + 1);

			int afterAttr = attr.getEnd();
			int beforeNextAttr = nextAttr.getStart();

			if (afterAttr < beforeNextAttr) {
				int length = beforeNextAttr - afterAttr;
				if (StringUtils.isWhitespace(textDocument.getText(), afterAttr, beforeNextAttr) && length > 1) {
					// Replace multiple spaces with single space
					try {
						Range range = new Range(textDocument.positionAt(afterAttr),
								textDocument.positionAt(beforeNextAttr));
						edits.add(new TextEdit(range, " "));
					} catch (BadLocationException e) {
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Minify text node by trimming unnecessary whitespace.
	 *
	 * @param edits the list of text edits
	 * @param text  the text node
	 */
	private void minifyText(List<TextEdit> edits, DOMText text) {
		DOMNode parent = text.getParentNode();
		if (parent != null && shouldPreserveSpace((DOMElement) parent)) {
			return;
		}

		String content = text.getData();
		if (content == null || content.trim().isEmpty()) {
			// Remove entirely if only whitespace
			removeWhitespace(edits, text.getStart(), text.getEnd());
		} else {
			// Trim leading and trailing whitespace, and normalize internal whitespace
			// Replace any sequence of whitespace characters (spaces, tabs, newlines) with a
			// single space
			String normalized = content.trim().replaceAll("\\s+", " ");
			if (!normalized.equals(content)) {
				try {
					Range range = new Range(textDocument.positionAt(text.getStart()),
							textDocument.positionAt(text.getEnd()));
					edits.add(new TextEdit(range, normalized));
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Remove whitespace in the given range.
	 *
	 * @param edits the list of text edits
	 * @param start the start offset
	 * @param end   the end offset
	 */
	private void removeWhitespace(List<TextEdit> edits, int start, int end) {
		if (start >= end) {
			return;
		}
		if (StringUtils.isWhitespace(textDocument.getText(), start, end)) {
			// Only whitespace, remove it
			removeContent(edits, start, end);
		}
	}

	private void removeContent(List<TextEdit> edits, int start, int end) {
		try {
			Range range = new Range(textDocument.positionAt(start), textDocument.positionAt(end));
			edits.add(new TextEdit(range, ""));
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Check if whitespace should be preserved for this element.
	 *
	 * @param element the element
	 * @return true if whitespace should be preserved
	 */
	private boolean shouldPreserveSpace(DOMElement element) {
		if (element == null) {
			return false;
		}

		// Check xml:space attribute
		String xmlSpace = element.getAttribute(XML_SPACE_ATTR);
		if (XML_SPACE_ATTR_PRESERVE.equals(xmlSpace)) {
			return true;
		}

		// Check parent elements
		DOMNode parent = element.getParentNode();
		if (parent != null && parent.isElement()) {
			return shouldPreserveSpace((DOMElement) parent);
		}

		return false;
	}
}
