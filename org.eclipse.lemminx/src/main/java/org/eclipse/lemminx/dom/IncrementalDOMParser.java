package org.eclipse.lemminx.dom;

import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.commons.TextDocumentChange;

/**
 * Incremental DOM parser that reuses as much of the old DOM as possible when
 * processing small text changes.
 */
public class IncrementalDOMParser {

	private static final IncrementalDOMParser INSTANCE = new IncrementalDOMParser();

	public static IncrementalDOMParser getInstance() {
		return INSTANCE;
	}

	public enum UpdateStrategy {
		NONE, //
		FULL, //
		TEXT, //
		ELEMENT, //
		ATTR, //
		SUBTREE; // Re-parse a subtree
	}

	private IncrementalDOMParser() {
	}

	/**
	 * Parse a list of changes incrementally
	 *
	 * @param document the DOM document
	 * @param changes  list of text changes with pre-calculated offsets
	 * @return the update strategy used
	 */
	public UpdateStrategy parseIncremental(DOMDocument document, List<TextDocumentChange> changes) {
		if (changes == null || changes.isEmpty()) {
			return UpdateStrategy.NONE;
		}
		UpdateStrategy strategy = UpdateStrategy.NONE;
		for (TextDocumentChange change : changes) {
			strategy = parseIncremental(document, change);
		}
		return strategy;
	}

	/**
	 * Parse a single change incrementally
	 *
	 * @param document the DOM document
	 * @param change   the text change with pre-calculated offsets
	 * @return the update strategy used
	 */
	private UpdateStrategy parseIncremental(DOMDocument document, TextDocumentChange change) {
		try {
			// Handle full content replacement
			if (change.getEvent().getRange() == null) {
				return parseFull(document, document.getTextDocument());
			}

			// Use pre-calculated offsets (calculated before text was updated)
			int changeOffset = change.getStartOffset();
			int oldLength = change.getOldLength();
			int newLength = change.getNewLength();
			String insertedText = change.getText();

			TextDocument textDoc = document.getTextDocument();
			return parseIncremental(document, textDoc, changeOffset, oldLength, newLength, insertedText);

		} catch (Exception e) {
			return parseFull(document, document.getTextDocument());
		}
	}

	/**
	 * Core incremental parsing logic
	 *
	 * @param oldDocument     the previous DOM document
	 * @param newTextDocument the updated text document
	 * @param changeOffset    offset where the change occurred
	 * @param oldLength       length of old text that was replaced
	 * @param newLength       length of new text
	 * @param insertedText    the text that was inserted/modified
	 * @return updated DOM document
	 */
	public UpdateStrategy parseIncremental(DOMDocument oldDocument, TextDocument newTextDocument, int changeOffset,
	 	int oldLength, int newLength, String insertedText) {

	 if (oldDocument == null) {
	 	return parseFull(oldDocument, newTextDocument);
	 }

	 // Find the affected node BEFORE any changes
	 DOMNode affectedNode = resolveAffectedNode(oldDocument, changeOffset);
	 if (affectedNode == null) {
	 	return parseFull(oldDocument, newTextDocument);
	 }

	 int delta = newLength - oldLength;
	 boolean isSimple = isSimpleChange(insertedText);
	 boolean isSimpleAttr = isSimpleAttributeChange(insertedText);

	 // Strategy 1: Text node modification (simple text change)
	 if (affectedNode.isText() && isSimple) {
	 	return reparseTextNode(oldDocument, affectedNode, delta, newTextDocument);
	 }

	 // Strategy 2: Attribute modification
	 if (affectedNode.isAttribute()) {
	 	DOMAttr attr = (DOMAttr) affectedNode;
	 	DOMElement element = attr.getOwnerElement();
	 	if (element != null && isSimpleAttr) {
	 		return reparseAttribute(oldDocument, element, attr, delta, newTextDocument);
	 	}
	 }

	 // Strategy 3: Element modification
	 if (affectedNode.isElement()) {
	 	UpdateStrategy elementStrategy = tryElementStrategy((DOMElement) affectedNode, changeOffset, oldLength,
	 			delta, isSimple, isSimpleAttr, oldDocument, newTextDocument);
	 	if (elementStrategy != null) {
	 		return elementStrategy;
	 	}
	 }

	 // Strategy 4: Subtree re-parsing
	 // Try to find a stable subtree root that can be re-parsed
	 DOMElement subtreeRoot = findSubtreeRoot(affectedNode, changeOffset, oldLength, newLength);
	 if (subtreeRoot != null) {
	 	return reparseSubtree(oldDocument, subtreeRoot, delta, newTextDocument);
	 }

	 // Default: change too complex, do full parse
	 return parseFull(oldDocument, newTextDocument);
	}

	/**
	 * Resolve the most specific affected node at the change offset.
	 * Progressively refines from element → text node → attribute node.
	 *
	 * @param document     the document
	 * @param changeOffset the offset where the change occurred
	 * @return the affected node, or null if not found
	 */
	private DOMNode resolveAffectedNode(DOMDocument document, int changeOffset) {
	 DOMNode node = document.findNodeAt(changeOffset);
	 if (node == null) {
	 	return null;
	 }

	 if (!node.isText()) {
	 	DOMNode textNode = DOMNode.findTextAt(node, changeOffset);
	 	if (textNode != null) {
	 		return textNode;
	 	}
	 }

	 if (!node.isText()) {
	 	DOMNode attrNode = DOMNode.findAttrAt(node, changeOffset);
	 	if (attrNode != null) {
	 		return attrNode;
	 	}
	 }

	 return node;
	}

	/**
	 * Try to apply element-specific incremental parsing strategies.
	 *
	 * @param element        the element node
	 * @param changeOffset   the offset where the change occurred
	 * @param oldLength      length of old text that was replaced
	 * @param delta          the change in length
	 * @param isSimple       whether the change is simple (no structural chars)
	 * @param isSimpleAttr   whether the change is simple for attributes
	 * @param oldDocument    the old document
	 * @param newTextDocument the new text document
	 * @return the update strategy if successful, null otherwise
	 */
	private UpdateStrategy tryElementStrategy(DOMElement element, int changeOffset, int oldLength, int delta,
	 	boolean isSimple, boolean isSimpleAttr, DOMDocument oldDocument, TextDocument newTextDocument) {

	 // Check if change is inside an attribute
	 DOMAttr attr = findAttributeAtOffset(element, changeOffset, oldLength);
	 if (attr != null && isSimpleAttr) {
	 	return reparseAttribute(oldDocument, element, attr, delta, newTextDocument);
	 }

	 // Check if change is in the start tag (adding/removing attributes)
	 if (element.startTagCloseOffset != DOMNode.NULL_VALUE &&
	     changeOffset <= element.startTagCloseOffset) {
	 	// Change is in start tag - reparse attributes
	 	return reparseStartTag(oldDocument, element, delta, newTextDocument);
	 }
	 
	 // Special case: element has no closing ">" yet (startTagCloseOffset == NULL_VALUE)
	 // This means we're likely adding the ">" to close the start tag
	 // We need to use SUBTREE strategy because closing the tag may create new text nodes
	 if (element.startTagCloseOffset == DOMNode.NULL_VALUE) {
	 	// Element start tag is not closed - use subtree reparse
	 	return null; // Will fall through to subtree strategy
	 }

	 // Check if change is inside element content
	 if (isChangeInElementContent(element, changeOffset, oldLength) && isSimple) {
	 	return reparseElementContent(oldDocument, element, delta, newTextDocument);
	 }

	 return null;
	}

	/**
	 * Find an attribute that contains the change range.
	 *
	 * @param element      the element to search
	 * @param changeOffset the offset where the change occurred
	 * @param oldLength    length of old text that was replaced
	 * @return the attribute if found, null otherwise
	 */
	private DOMAttr findAttributeAtOffset(DOMElement element, int changeOffset, int oldLength) {
	 if (!element.hasAttributes()) {
	 	return null;
	 }

	 for (DOMAttr attr : element.getAttributeNodes()) {
	 	if (changeOffset >= attr.getStart() && changeOffset + oldLength <= attr.getEnd()) {
	 		return attr;
	 	}
	 }
	 return null;
	}

	/**
	 * Check if the change is within the element's content (between start and end tags).
	 * Only returns true if the element contains simple text content (no child elements).
	 *
	 * @param element      the element
	 * @param changeOffset the offset where the change occurred
	 * @param oldLength    length of old text that was replaced
	 * @return true if change is in element content and element has no child elements
	 */
	private boolean isChangeInElementContent(DOMElement element, int changeOffset, int oldLength) {
		// Check basic position constraints
		if (element.getStartTagCloseOffset() == DOMNode.NULL_VALUE
				|| element.getEndTagOpenOffset() == DOMNode.NULL_VALUE
				|| changeOffset <= element.getStartTagCloseOffset()
				|| changeOffset + oldLength > element.getEndTagOpenOffset()) {
			return false;
		}
		
		// Only allow element content strategy if element has no child elements
		// (only text nodes are allowed)
		if (element.hasChildNodes()) {
			for (DOMNode child : element.getChildren()) {
				if (child.isElement()) {
					// Element has child elements - cannot use simple element content strategy
					return false;
				}
			}
		}
		
		return true;
	}

	/**
	 * Incrementally update when a text node is modified
	 */
	private UpdateStrategy reparseTextNode(DOMDocument oldDoc, DOMNode textNode, int delta, TextDocument newTextDoc) {

		// IMPORTANT: The TEXT strategy only works correctly when we're just adding/removing characters
		// without changing the document structure. However, when delta == 0 (same length replacement),
		// we still need to ensure the DOM structure remains valid.
		//
		// The issue is that even with delta == 0, the document content has changed, and all nodes
		// that read from the document will read incorrect content. The TEXT strategy assumes only
		// the affected text node needs updating, but this is not safe when content changes.
		//
		// For now, we'll keep the TEXT strategy but ensure siblings are always processed,
		// even when delta == 0, by removing the early return in shiftOffsetsAfter().

		// 1. Update the text node's end offset
		// Simply add delta to the current end position since delta represents the change in length
		textNode.end = textNode.end + delta;

		// 2. Clear cached data to force reload from updated document
		if (textNode instanceof DOMCharacterData) {
			((DOMCharacterData) textNode).clearCache();
		}

		// 3. Adjust all parent offsets
		adjustParentOffsets(textNode, delta);

		// 4. Adjust all following sibling nodes
		// NOTE: This must be called even when delta == 0 to ensure cache is cleared
		shiftOffsetsAfter(textNode, delta);

		return UpdateStrategy.TEXT;
	}

	/**
	 * Incrementally update when an attribute value is modified
	 */
	private UpdateStrategy reparseAttribute(DOMDocument oldDoc, DOMElement element, DOMAttr attr, int delta,
			TextDocument newTextDoc) {

		// Update modified attribute offsets
		attr.end += delta;
		
		// Update nodeAttrValue offsets if it exists
		DOMRange oldAttrValue = attr.getNodeAttrValue();
		if (oldAttrValue != null) {
			// Create new AttrValue with updated end offset
			attr.setValue(null, oldAttrValue.getStart(), oldAttrValue.getEnd() + delta);
		}

		// Adjust offsets of all following attributes in the same element
		if (element.hasAttributes()) {
			boolean foundModifiedAttr = false;
			for (DOMAttr otherAttr : element.getAttributeNodes()) {
				if (otherAttr == attr) {
					foundModifiedAttr = true;
					continue;
				}
				if (foundModifiedAttr) {
					// This attribute comes after the modified one, adjust its offsets
					adjustAttrOffsets(otherAttr, delta);
				}
			}
		}

		// Update element tag offsets
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

		// Adjust all children (they come after the attribute)
		if (element.hasChildNodes()) {
			for (DOMNode child : element.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}

		// Adjust parent offsets
		adjustParentOffsets(element, delta);

		// Adjust following nodes
		shiftOffsetsAfter(element, delta);
		return UpdateStrategy.ATTR;
	}

	/**
	 * Incrementally update when element content is modified
	 */
	private UpdateStrategy reparseElementContent(DOMDocument oldDoc, DOMElement element, int delta,
			TextDocument newTextDoc) {

		boolean createdTextNode = false;
		
		// Special case: if element has no children and we're adding text, create a text node
		if (!element.hasChildNodes() && delta > 0) {
			// Start after the '>' of the start tag
			int textStart = element.getStartTagCloseOffset() + 1;
			int textEnd = textStart + delta;
			DOMText textNode = new DOMText(textStart, textEnd);
			textNode.parent = element;
			textNode.closed = true; // Text nodes are always closed
			element.addChild(textNode);
			createdTextNode = true;
		}

		// Update element end tag offsets
		if (element.endTagOpenOffset != DOMNode.NULL_VALUE) {
			element.endTagOpenOffset += delta;
		}
		if (element.endTagCloseOffset != DOMNode.NULL_VALUE) {
			element.endTagCloseOffset += delta;
		}
		element.end += delta;

		// Adjust all children offsets (but not the text node we just created with correct offsets)
		if (element.hasChildNodes() && !createdTextNode) {
			for (DOMNode child : element.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}

		// Adjust parent offsets
		adjustParentOffsets(element, delta);

		// Adjust following nodes
		shiftOffsetsAfter(element, delta);

		// Return TEXT strategy if we created a text node, otherwise ELEMENT
		return createdTextNode ? UpdateStrategy.TEXT : UpdateStrategy.ELEMENT;
	}

	/**
		* Re-parse the start tag to update attributes when they are added/removed/modified.
		* Uses the scanner to parse attributes from the start tag.
		*/
	private UpdateStrategy reparseStartTag(DOMDocument oldDoc, DOMElement element, int delta,
			TextDocument newTextDoc) {
		
		String newText = newTextDoc.getText();
		int startTagStart = element.getStart();
		int startTagEnd = element.getStartTagCloseOffset() + delta;
		
		if (startTagEnd > newText.length()) {
			return parseFull(oldDoc, newTextDoc);
		}
		
		// Clear existing attributes
		element.getAttributeNodes().clear();
		
		// Use scanner to parse the start tag and extract attributes
		org.eclipse.lemminx.dom.parser.Scanner scanner =
			org.eclipse.lemminx.dom.parser.XMLScanner.createScanner(newText, startTagStart, false);
		
		org.eclipse.lemminx.dom.parser.TokenType token = scanner.scan();
		DOMAttr currentAttr = null;
		
		while (token != org.eclipse.lemminx.dom.parser.TokenType.EOS &&
		       scanner.getTokenOffset() < startTagEnd) {
			
			switch (token) {
				case AttributeName:
					currentAttr = new DOMAttr(scanner.getTokenText(),
						scanner.getTokenOffset(), scanner.getTokenEnd(), oldDoc);
					element.setAttributeNode(currentAttr);
					break;
					
				case DelimiterAssign:
					if (currentAttr != null) {
						currentAttr.setDelimiter(scanner.getTokenOffset());
					}
					break;
					
				case AttributeValue:
					if (currentAttr != null) {
						// Pass null as value - setValue will extract it from offsets
						currentAttr.setValue(null,
							scanner.getTokenOffset(), scanner.getTokenEnd());
						currentAttr = null;
					}
					break;
					
				case StartTagClose:
				case StartTagSelfClose:
					// End of start tag
					break;
					
				default:
					break;
			}
			
			token = scanner.scan();
		}
		
		// Update element offsets
		element.startTagCloseOffset += delta;
		if (element.endTagOpenOffset != DOMNode.NULL_VALUE) {
			element.endTagOpenOffset += delta;
		}
		if (element.endTagCloseOffset != DOMNode.NULL_VALUE) {
			element.endTagCloseOffset += delta;
		}
		element.end += delta;
		
		// Adjust children offsets
		if (element.hasChildNodes()) {
			for (DOMNode child : element.getChildren()) {
				adjustNodeOffsets(child, delta);
			}
		}
		
		// Adjust parent offsets
		adjustParentOffsets(element, delta);
		
		// Adjust following nodes
		shiftOffsetsAfter(element, delta);
		
		return UpdateStrategy.ELEMENT;
	}

	/**
		* Adjust offsets of all parent nodes up the tree
		*/
	private void adjustParentOffsets(DOMNode node, int delta) {
		DOMNode parent = node.getParentNode();

		while (parent != null) {
			// Adjust parent end offset
			parent.end += delta;

			// If it's an element, adjust tag offsets ONLY if they exist and come after the change
			if (parent instanceof DOMElement) {
				DOMElement element = (DOMElement) parent;

				// Only adjust end tag offsets if the element actually has an end tag
				// For elements without end tags (unclosed), these should remain NULL_VALUE
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

	private void shiftOffsetsAfter(DOMNode changedNode, int delta) {
		// REMOVED: Early return when delta == 0
		// Even when delta == 0, we need to clear caches of all following nodes
		// because the document content has changed and cached data may be stale
		// if (delta == 0)
		// 	return;

		DOMNode current = changedNode;

		// Go up the tree and adjust all nodes that come after
		while (current != null && !(current instanceof DOMDocument)) {
			// Collect all siblings after current first to avoid issues with getNextSibling()
			// during offset adjustment
			java.util.List<DOMNode> siblings = new java.util.ArrayList<>();
			DOMNode sibling = current.getNextSibling();
			while (sibling != null) {
				siblings.add(sibling);
				sibling = sibling.getNextSibling();
			}
			
			// Now adjust offsets of collected siblings
			for (DOMNode s : siblings) {
				adjustNodeOffsets(s, delta);
			}

			// Move up to parent
			current = current.getParentNode();
		}
	}

	/**
	 * Recursively adjust offsets of a node and all its descendants.
	 *
	 * IMPORTANT: This method must clear caches even when delta=0, because:
	 * - When text content changes with same length (delta=0), the document content
	 *   has changed even though positions haven't
	 * - Cached data (like normalized text, whitespace flags) becomes stale
	 * - Sibling nodes after the change need their caches cleared to reflect the
	 *   new document state
	 */
	private void adjustNodeOffsets(DOMNode node, int delta) {
		node.start += delta;
		node.end += delta;

		// Clear cached data for character data nodes (text, comments, CDATA, etc.)
		// This is critical even when delta=0 because the document content has changed
		if (node instanceof DOMCharacterData) {
			((DOMCharacterData) node).clearCache();
		}

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

	/**
	 * Adjust all offsets in an attribute node
	 */
	private void adjustAttrOffsets(DOMAttr attr, int delta) {
		attr.start += delta;
		attr.end += delta;

		// Adjust delimiter offset
		if (attr.hasDelimiter()) {
			attr.setDelimiter(attr.getDelimiterOffset() + delta);
		}
		
		// Update nodeAttrName offsets if it exists
		DOMRange oldAttrName = attr.getNodeAttrName();
		if (oldAttrName != null) {
			attr.updateAttrNameOffsets(oldAttrName.getStart() + delta, oldAttrName.getEnd() + delta);
		}
		
		// Update nodeAttrValue offsets if it exists
		DOMRange oldAttrValue = attr.getNodeAttrValue();
		if (oldAttrValue != null) {
			// Create new AttrValue with adjusted offsets and clear cached values
			attr.setValue(null, oldAttrValue.getStart() + delta, oldAttrValue.getEnd() + delta);
		}
	}

	/**
	 * Check if a text change is simple (no structural characters).
	 */
	private boolean isSimpleChange(String text) {
		if (text == null || text.isEmpty()) {
			return true; // Empty change is always simple
		}
		
		// Reject if contains structural characters
		return !text.contains("<") && !text.contains(">");
	}

	/**
	 * Check if an attribute change is simple (no quotes or structural characters).
	 */
	private boolean isSimpleAttributeChange(String text) {
		if (text == null || text.isEmpty()) {
			return true; // Empty change is always simple
		}
		
		// Reject if contains problematic characters
		return !text.contains("<") &&
		       !text.contains(">") &&
		       !text.contains("\"") &&
		       !text.contains("'");
	}

	/**
	 * Find a stable subtree root that can be re-parsed.
	 *
	 * A subtree is stable if:
	 * - It's a complete element (has both start and end tags)
	 * - The change is contained within the element's content
	 * - The change doesn't affect the element's tags
	 *
	 * @param affectedNode the node affected by the change
	 * @param changeOffset the offset where the change occurred
	 * @param oldLength    the length of the old text
	 * @param newLength    the length of the new text
	 * @return the subtree root element, or null if no stable subtree found
	 */
	private DOMElement findSubtreeRoot(DOMNode affectedNode, int changeOffset, int oldLength, int newLength) {
		// Start from the affected node and go up the tree
		DOMNode current = affectedNode;
		
		while (current != null && !(current instanceof DOMDocument)) {
			if (current instanceof DOMElement) {
				DOMElement element = (DOMElement) current;
				
				// For subtree reparse, we need at least a closed start tag
				// The element doesn't need to have an end tag (can be unclosed)
				if (element.getStartTagCloseOffset() == DOMNode.NULL_VALUE) {
					// Start tag is not closed, try parent
					current = current.parent;
					continue;
				}
				
				// Check if change is within element's content (not in tags)
				int contentStart = element.getStartTagCloseOffset() + 1;
				// For unclosed elements, use element.end as content end
				int contentEnd = element.endTagOpenOffset != DOMNode.NULL_VALUE
					? element.endTagOpenOffset
					: element.end;
				
				if (changeOffset >= contentStart && changeOffset < contentEnd) {
					// Change is in content area - this element is a good candidate
					// For insertions (oldLength=0), only check start position
					// For replacements, check if the replaced range is within content
					if (oldLength == 0) {
						// Pure insertion - only check insertion point
						return element;
					} else {
						// Replacement - check if replaced range is within content
						int changeEnd = changeOffset + oldLength;
						if (changeEnd <= contentEnd) {
							return element;
						}
					}
				}
			}
			
			// Try parent
			current = current.parent;
		}
		
		// No stable subtree found
		return null;
	}

	/**
	 * Re-parse a subtree and replace it in the document.
	 *
	 * This method uses DOMParser.parseFragment() which:
	 * - Does NOT use substring() - uses scanner with offsets directly
	 * - Creates nodes with correct ownerDocument references
	 * - Parses with absolute offsets (no adjustment needed)
	 *
	 * @param document    the document
	 * @param subtreeRoot the root of the subtree to re-parse
	 * @param delta       the change in length
	 * @param newTextDoc  the updated text document
	 * @return SUBTREE strategy
	 */
	private UpdateStrategy reparseSubtree(DOMDocument document, DOMElement subtreeRoot, int delta, TextDocument newTextDoc) {
		try {
			// 1. Calculate the fragment boundaries (with delta applied)
			int startOffset = subtreeRoot.getStart();
			int oldEndOffset = subtreeRoot.getEnd();
			int newEndOffset = oldEndOffset + delta;
			
			String newText = newTextDoc.getText();
			if (newEndOffset > newText.length()) {
				return parseFull(document, newTextDoc);
			}
			
			// 2. Parse the fragment using parseFragment() - NO SUBSTRING!
			// The scanner will parse directly from the full text using offsets
			DOMElement newSubtreeRoot = DOMParser.getInstance().parseFragment(
					newText, startOffset, newEndOffset, document);
			
			if (newSubtreeRoot == null) {
				// Parsing failed, fall back to full parse
				return parseFull(document, newTextDoc);
			}
			
			// 3. Replace the old subtree with the new one
			// No offset adjustment needed - parseFragment() uses absolute offsets!
			replaceSubtree(document, subtreeRoot, newSubtreeRoot);
			
			// 4. Adjust parent offsets
			adjustParentOffsets(newSubtreeRoot, delta);
			
			// 5. Adjust offsets of all nodes after the subtree
			shiftOffsetsAfter(newSubtreeRoot, delta);
			
			return UpdateStrategy.SUBTREE;
			
		} catch (Exception e) {
			// If anything goes wrong, fall back to full parse
			return parseFull(document, newTextDoc);
		}
	}

	/**
	 * Replace an old subtree with a new one in the document.
	 */
	private void replaceSubtree(DOMDocument document, DOMElement oldRoot, DOMElement newRoot) {
		DOMNode parent = oldRoot.getParentNode();
		
		if (parent == null) {
			// The subtree is the document root
			document.getChildren().clear();
			document.getChildren().add(newRoot);
			newRoot.parent = document;
		} else {
			// Replace in parent's children list
			List<DOMNode> siblings = parent.getChildren();
			int index = siblings.indexOf(oldRoot);
			
			if (index >= 0) {
				siblings.set(index, newRoot);
				newRoot.parent = parent;
			}
		}
	}

	/**
	 * Fallback to full parse when incremental parsing is not possible
	 *
	 * @param document
	 * @return
	 */
	private UpdateStrategy parseFull(DOMDocument document, TextDocument doc) {
		DOMDocument newDoc = DOMParser.getInstance().parse(doc, null);
		document.getChildren().clear();
		document.getChildren().addAll(newDoc.getChildren());
		
		// Update parent references for all children
		for (DOMNode child : document.getChildren()) {
			child.parent = document;
		}
		
		// Update document end offset to match new content length
		document.end = doc.getText().length();
		
		return UpdateStrategy.FULL;
	}
}