package org.eclipse.lemminx.dom;

import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

public class IncrementalDOMParser {

	public DOMDocument parseIncremental(DOMDocument oldDocument, List<TextDocumentContentChangeEvent> changes) {

		DOMDocument document = oldDocument;
		for (TextDocumentContentChangeEvent event : changes) {
			document = parseIncremental(document, event);
		}
		return document;
	}

	private DOMDocument parseIncremental(DOMDocument document, TextDocumentContentChangeEvent event) {
		try {
			if (event.getRange() == null) {
				return parseFull(document.getTextDocument());
			}

			int changeOffset = document.offsetAt(event.getRange().getStart());

			int oldLength;
			if (event.getRangeLength() != null) {
				oldLength = event.getRangeLength();
			} else {
				int endOffset = document.offsetAt(event.getRange().getEnd());
				oldLength = endOffset - changeOffset;
			}

			int newLength = event.getText() != null ? event.getText().length() : 0;

			TextDocument textDoc = document.getTextDocument();

			return parseIncremental(document, textDoc, changeOffset, oldLength, newLength);

		} catch (BadLocationException e) {
			return parseFull(document.getTextDocument());
		}
	}

	public DOMDocument parseIncremental(DOMDocument oldDocument, TextDocument newTextDocument, int changeOffset,
			int oldLength, int newLength) {

		if (oldDocument == null) {
			return parseFull(newTextDocument);
		}

		DOMNode affectedNode = oldDocument.findNodeAt(changeOffset);
		if (affectedNode == null) {
			return parseFull(newTextDocument);
		}

		DOMNode adjusteddNode = DOMNode.findTextAt(affectedNode, changeOffset);
		if (adjusteddNode == null) {
			adjusteddNode = DOMNode.findAttrAt(affectedNode, changeOffset);
		}
		if (adjusteddNode != null) {
			affectedNode = adjusteddNode;
		}

		int delta = newLength - oldLength;

		// Text node
		if (affectedNode.isText()) {
			return reparseTextNode(oldDocument, affectedNode, delta, newTextDocument);
		}

		// Element
		if (affectedNode.isElement()) {
			DOMElement element = (DOMElement) affectedNode;

			// Check if in attribute
			if (element.hasAttributes()) {
				for (DOMAttr attr : element.getAttributeNodes()) {
					if (changeOffset >= attr.getStart() && changeOffset + oldLength <= attr.getEnd()) {
						return reparseAttribute(oldDocument, element, attr, delta, newTextDocument);
					}
				}
			}

			// Check if in element content
			if (element.getStartTagCloseOffset() != DOMNode.NULL_VALUE
					&& element.getEndTagOpenOffset() != DOMNode.NULL_VALUE
					&& changeOffset > element.getStartTagCloseOffset()
					&& changeOffset + oldLength < element.getEndTagOpenOffset()) {

				String newText = newTextDocument.getText();
				int safeEnd = Math.min(changeOffset + newLength, newText.length());
				String changedText = newText.substring(changeOffset, safeEnd);

				if (!changedText.contains("<") && !changedText.contains(">")) {
					return reparseElementContent(oldDocument, element, delta, newTextDocument);
				}
			}
		}

		return parseFull(newTextDocument);
	}

	private DOMDocument reparseTextNode(DOMDocument oldDoc, DOMNode textNode, int delta, TextDocument newTextDoc) {

		// 1. Update the text node
		textNode.end += delta;

		// 2. Adjust PARENT offsets
		adjustParentOffsets(textNode, delta);

		// 3. Adjust following nodes
		shiftOffsetsAfter(textNode, delta);

		return createUpdatedDocument(oldDoc, newTextDoc);
	}

	private DOMDocument reparseAttribute(DOMDocument oldDoc, DOMElement element, DOMAttr attr, int delta,
			TextDocument newTextDoc) {

		attr.end += delta;
		/*
		 * if (attr.nodeAttrValueEnd != null) { attr.nodeAttrValueEnd += delta; }
		 */

		if (element.startTagCloseOffset != DOMNode.NULL_VALUE) {
			element.startTagCloseOffset += delta;
		}
		if (element.endTagOpenOffset != DOMNode.NULL_VALUE) {
			element.endTagOpenOffset += delta;
		}
		if (element.endTagCloseOffset != DOMNode.NULL_VALUE) {
			element.endTagCloseOffset += delta;
		}
		element.end += delta;

		// Adjust children offsets (all children are after the attribute)
		if (element.hasChildNodes()) {
			for (DOMNode child : element.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}

		// Adjust parent offsets
		adjustParentOffsets(element, delta);

		// Adjust following nodes
		shiftOffsetsAfter(element, delta);

		return createUpdatedDocument(oldDoc, newTextDoc);
	}

	private DOMDocument reparseElementContent(DOMDocument oldDoc, DOMElement element, int delta,
			TextDocument newTextDoc) {

		// Update element end offsets
		if (element.endTagOpenOffset != DOMNode.NULL_VALUE) {
			element.endTagOpenOffset += delta;
		}
		if (element.endTagCloseOffset != DOMNode.NULL_VALUE) {
			element.endTagCloseOffset += delta;
		}
		element.end += delta;

		// Adjust children offsets (they are inside the element content)
		if (element.hasChildNodes()) {
			for (DOMNode child : element.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}

		// Adjust parent offsets
		adjustParentOffsets(element, delta);

		// Adjust following nodes
		shiftOffsetsAfter(element, delta);

		return createUpdatedDocument(oldDoc, newTextDoc);
	}

	/**
	 * Ajuste les offsets de tous les parents du nœud
	 */
	private void adjustParentOffsets(DOMNode node, int delta) {
		DOMNode parent = node.getParentNode();

		while (parent != null && !(parent instanceof DOMDocument)) {
			// Adjust parent end offset
			parent.end += delta;

			// If element, adjust tag offsets
			if (parent instanceof DOMElement) {
				DOMElement element = (DOMElement) parent;

				if (element.endTagOpenOffset != DOMNode.NULL_VALUE) {
					element.endTagOpenOffset += delta;
				}
				if (element.endTagCloseOffset != DOMNode.NULL_VALUE) {
					element.endTagCloseOffset += delta;
				}
			}

			parent = parent.getParentNode();
		}
	}

	private DOMDocument createUpdatedDocument(DOMDocument oldDoc, TextDocument newTextDoc) {
		DOMDocument newDoc = new DOMDocument(newTextDoc, oldDoc.getResolverExtensionManager());
		newDoc.setCancelChecker(oldDoc.getCancelChecker());

		for (DOMNode child : oldDoc.getChildren()) {
			newDoc.addChild(child);
		}

		return newDoc;
	}

	private void shiftOffsetsAfter(DOMNode node, int delta) {
		if (delta == 0)
			return;

		DOMNode next = getNextNodeInDocumentOrder(node);
		while (next != null) {
			adjustNodeOffsets(next, delta);
			next = getNextNodeInDocumentOrder(next);
		}
	}

	private void adjustNodeOffsets(DOMNode node, int delta) {
		node.start += delta;
		node.end += delta;

		if (node instanceof DOMElement) {
			DOMElement e = (DOMElement) node;
			if (e.startTagOpenOffset != DOMNode.NULL_VALUE)
				e.startTagOpenOffset += delta;
			if (e.startTagCloseOffset != DOMNode.NULL_VALUE)
				e.startTagCloseOffset += delta;
			if (e.endTagOpenOffset != DOMNode.NULL_VALUE)
				e.endTagOpenOffset += delta;
			if (e.endTagCloseOffset != DOMNode.NULL_VALUE)
				e.endTagCloseOffset += delta;

			if (e.hasAttributes()) {
				for (DOMAttr attr : e.getAttributeNodes()) {
					adjustAttrOffsets(attr, delta);
				}
			}
		}

		// Recursively adjust children
		if (node.hasChildNodes()) {
			for (DOMNode child : node.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}
	}

	private void adjustAttrOffsets(DOMAttr attr, int delta) {
		attr.start += delta;
		attr.end += delta;

		/*
		 * if (attr.nodeAttrNameStart != null) attr.nodeAttrNameStart += delta; if
		 * (attr.nodeAttrNameEnd != null) attr.nodeAttrNameEnd += delta; if
		 * (attr.nodeAttrValueStart != null) attr.nodeAttrValueStart += delta; if
		 * (attr.nodeAttrValueEnd != null) attr.nodeAttrValueEnd += delta; if
		 * (attr.delimiterAssign != null) attr.delimiterAssign += delta;
		 */
	}

	private DOMNode getNextNodeInDocumentOrder(DOMNode node) {
		DOMNode next = node.getNextSibling();
		if (next != null)
			return next;

		DOMNode parent = node.getParentNode();
		while (parent != null) {
			next = parent.getNextSibling();
			if (next != null)
				return next;
			parent = parent.getParentNode();
		}
		return null;
	}

	private DOMDocument parseFull(TextDocument doc) {
		return DOMParser.getInstance().parse(doc, null);
	}
}