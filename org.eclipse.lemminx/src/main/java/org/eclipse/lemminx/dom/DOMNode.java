/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.lemminx.dom.green.GreenNode;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * DOM node.
 *
 */
public abstract class DOMNode implements Node, DOMRange {

	/**
	 * Null value used for offset.
	 */
	public static final int NULL_VALUE = -1;

	/**
	 * The node is a <code>DTD Element Declaration</code>.
	 */
	public static final short DTD_ELEMENT_DECL_NODE = 101;

	/**
	 * The node is a <code>DTD Attribute List</code>.
	 */
	public static final short DTD_ATT_LIST_NODE = 102;

	/**
	 * The node is a <code>DTD Entity Declaraction</code>.
	 */
	public static final short DTD_ENTITY_DECL_NODE = 103;

	/**
	 * The node is a <code>DTD Notation Declaraction</code>.
	 */
	public static final short DTD_NOTATION_DECL = 104;

	/**
	 * The node is a generic <code>DTD Decl Node</code>.
	 */
	public static final short DTD_DECL_NODE = 105;

	private byte flags = 0;
	private static final byte FLAG_CLOSED = 0x01;
	static final byte FLAG_SELF_CLOSED = 0x02;
	static final byte FLAG_WHITESPACE = 0x04;

	final int start; // |<root> </root>
	int end; // <root> </root>|

	DOMNode parent;

	int cachedIndexInParent = -1;

	private static final NodeList EMPTY_CHILDREN = new NodeList() {

		@Override
		public Node item(int index) {
			return null;
		}

		@Override
		public int getLength() {
			return 0;
		}
	};

	private static final class ArrayNodeList implements NodeList {
		private final DOMNode[] nodes;
		ArrayNodeList(DOMNode[] nodes) { this.nodes = nodes; }
		@Override
		public int getLength() { return nodes.length; }
		@Override
		public Node item(int index) {
			return index >= 0 && index < nodes.length ? nodes[index] : null;
		}
	}

	static final class AttrNamedNodeMap implements NamedNodeMap {
		private final DOMAttr[] attrs;
		AttrNamedNodeMap(DOMAttr[] attrs) { this.attrs = attrs; }
		@Override public int getLength() { return attrs.length; }
		@Override public Node item(int index) { return index >= 0 && index < attrs.length ? attrs[index] : null; }
		@Override public Node getNamedItem(String name) {
			for (DOMAttr a : attrs) { if (name.equals(a.getNodeName())) return a; }
			return null;
		}
		@Override public Node getNamedItemNS(String ns, String local) throws DOMException { throw new UnsupportedOperationException(); }
		@Override public Node removeNamedItem(String n) throws DOMException { throw new UnsupportedOperationException(); }
		@Override public Node removeNamedItemNS(String ns, String local) throws DOMException { throw new UnsupportedOperationException(); }
		@Override public Node setNamedItem(Node n) throws DOMException { throw new UnsupportedOperationException(); }
		@Override public Node setNamedItemNS(Node n) throws DOMException { throw new UnsupportedOperationException(); }
	}

	public DOMNode(int start, int end) {
		this.start = start;
		this.end = end;
	}

	protected final boolean hasFlag(byte flag) {
		return (flags & flag) != 0;
	}

	protected final void setFlag(byte flag, boolean value) {
		if (value) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}
	}

	/**
	 * Returns the owner document and null otherwise.
	 * 
	 * @return the owner document and null otherwise.
	 */
	@Override
	public DOMDocument getOwnerDocument() {
		Node node = parent;
		while (node != null) {
			if (node.getNodeType() == Node.DOCUMENT_NODE) {
				return (DOMDocument) node;
			}
			node = node.getParentNode();
		}
		return null;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private String toString(int indent) {
		StringBuilder result = new StringBuilder("");
		for (int i = 0; i < indent; i++) {
			result.append("\t");
		}
		result.append("{start: ");
		result.append(start);
		result.append(", end: ");
		result.append(end);
		result.append(", name: ");
		result.append(getNodeName());
		result.append(", closed: ");
		result.append(isClosed());
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		if (arr != null && arr.length > 0) {
			result.append(", \n");
			for (int i = 0; i < indent + 1; i++) {
				result.append("\t");
			}
			result.append("children:[");
			for (int i = 0; i < arr.length; i++) {
				DOMNode node = arr[i];
				result.append("\n");
				result.append(node.toString(indent + 2));
				if (i < arr.length - 1) {
					result.append(",");
				}
			}
			result.append("\n");
			for (int i = 0; i < indent + 1; i++) {
				result.append("\t");
			}
			result.append("]");
			result.append("\n");
			for (int i = 0; i < indent; i++) {
				result.append("\t");
			}
			result.append("}");
		} else {
			result.append("}");
		}
		return result.toString();
	}

	/**
	 * Returns the node before
	 */
	public DOMNode findNodeBefore(int offset) {
		List<DOMNode> children = getChildren();
		int idx = findFirst(children, c -> offset <= c.start) - 1;
		if (idx >= 0) {
			DOMNode child = children.get(idx);
			if (offset > child.start) {
				if (offset < child.end) {
					return child.findNodeBefore(offset);
				}
				DOMNode lastChild = child.getLastChild();
				if (lastChild != null && lastChild.end == child.end) {
					return child.findNodeBefore(offset);
				}
				return child;
			}
		}
		return this;
	}

	public DOMNode findNodeAt(int offset) {
		List<DOMNode> children = getChildren();
		int idx = findFirst(children, c -> offset <= c.start) - 1;
		if (idx >= 0) {
			DOMNode child = children.get(idx);
			if (isIncluded(child, offset)) {
				return child.findNodeAt(offset);
			}
		}
		return this;
	}

	/**
	 * Returns true if the node included the given offset and false otherwise.
	 * 
	 * @param node
	 * @param offset
	 * @return true if the node included the given offset and false otherwise.
	 */
	public static boolean isIncluded(DOMRange node, int offset) {
		if (node == null) {
			return false;
		}
		return isIncluded(node.getStart(), node.getEnd(), offset);
	}

	public static boolean isIncluded(int start, int end, int offset) {
		return offset >= start && offset <= end;
	}

	public DOMAttr findAttrAt(int offset) {
		DOMNode node = findNodeAt(offset);
		return findAttrAt(node, offset);
	}

	public static DOMAttr findAttrAt(DOMNode node, int offset) {
		if (node != null && node.hasAttributes()) {
			for (DOMAttr attr : node.getAttributeNodes()) {
				if (attr.isIncluded(offset)) {
					return attr;
				}
			}
		}
		return null;
	}

	public DTDDeclParameter findDTDDeclParameterAt(int offset) {
		DOMNode node = findNodeAt(offset);
		return findDTDDeclParameterAt(node, offset);
	}

	public static DTDDeclParameter findDTDDeclParameterAt(DOMNode node, int offset) {
		if (node != null && (node.isDTDAttListDecl() || node.isDTDElementDecl() || node.isDTDEntityDecl()
				|| node.isDTDNotationDecl())) {
			for (DTDDeclParameter parameter : ((DTDDeclNode) node).getParameters()) {
				if (isIncluded(parameter, offset)) {
					return parameter;
				}
			}
		}
		return null;
	}

	public static DOMText findTextAt(DOMNode node, int offset) {
		if (node != null && node.hasChildNodes()) {
			for (DOMNode child : node.getChildren()) {
				if (child.isText() && isIncluded(child, offset)) {
					return (DOMText) child;
				}
			}
		}
		return null;
	}

	public static DOMNode findNodeOrAttrAt(DOMDocument document, int offset) {
		DOMNode node = document.findNodeAt(offset);
		if (node != null) {
			DOMAttr attr = findAttrAt(node, offset);
			if (attr != null) {
				return attr;
			}
		}
		return node;
	}

	/**
	 * Takes a sorted array and a function p. The array is sorted in such a way that
	 * all elements where p(x) is false are located before all elements where p(x)
	 * is true.
	 * 
	 * @returns the least x for which p(x) is true or array.length if no element
	 *          full fills the given function.
	 */
	private static <T> int findFirst(List<T> array, Predicate<T> p) {
		int low = 0, high = array.size();
		if (high == 0) {
			return 0;
		}
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (p.test(array.get(mid))) {
				high = mid;
			} else {
				low = mid + 1;
			}
		}
		return low;
	}

	public DOMAttr getAttributeNode(String name) {
		return getAttributeNode(null, name);
	}

	/**
	 * Returns the attribute that matches the given name.
	 * 
	 * If there is no namespace, set prefix to null.
	 */
	public DOMAttr getAttributeNode(String prefix, String suffix) {
		if (!hasAttributes()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
			sb.append(":");
		}
		sb.append(suffix);
		String name = sb.toString();
		for (DOMAttr attr : getAttributeNodes()) {
			if (name.equals(attr.getName())) {
				return attr;
			}
		}
		return null;
	}

	public String getAttribute(String name) {
		DOMAttr attr = getAttributeNode(name);
		String value = attr != null ? attr.getValue() : null;
		if (value == null) {
			return null;
		}
		if (value.isEmpty()) {
			return value;
		}
		// remove quote
		char c = value.charAt(0);
		if (c == '"' || c == '\'') {
			if (value.length() == 1) {
				return value;
			} else if (value.charAt(value.length() - 1) == c) {
				return value.substring(1, value.length() - 1);
			}
			return value.substring(1, value.length());
		}
		return value;
	}

	/**
	 * Returns the attribute at the given index, the order is how the attributes
	 * appear in the document.
	 * 
	 * @param index Starting at 0, index of attribute you want
	 * @return
	 */
	public DOMAttr getAttributeAtIndex(int index) {
		return null;
	}

	public boolean hasAttribute(String name) {
		return hasAttributes() && getAttributeNode(name) != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#hasAttributes()
	 */
	@Override
	public boolean hasAttributes() {
		return false;
	}

	public void setAttribute(String name, String value) {
		DOMAttr attr = getAttributeNode(name);
		if (attr == null) {
			attr = new DOMAttr(name, this);
			setAttributeNode(attr);
		}
		attr.setValue(value);
	}

	public void setAttributeNode(DOMAttr attr) {
	}

	public List<DOMAttr> getAttributeNodes() {
		return null;
	}

	/**
	 * Returns a list of children, each having an attribute called name, with a
	 * value of value
	 * 
	 * @param name  name of attribute
	 * @param value value of attribute
	 * @return list of children, each having a specified attribute name and value
	 */
	public List<DOMNode> getChildrenWithAttributeValue(String name, String value) {
		List<DOMNode> result = new ArrayList<>();
		for (DOMNode child : getChildren()) {
			if (child.hasAttribute(name)) {
				String attrValue = child.getAttribute(name);
				if (Objects.equals(attrValue, value)) {
					result.add(child);
				}
			}
		}
		return result;
	}

	DOMNode[] getChildrenArray() {
		return null;
	}

	void setChildrenArray(DOMNode[] c) {
	}

	void ensureChildren() {
	}

	void setLazy(GreenNode green, int absStart) {
	}

	/**
	 * Returns the node children.
	 *
	 * @return the node children.
	 */
	public List<DOMNode> getChildren() {
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		if (arr == null || arr.length == 0) {
			return Collections.emptyList();
		}
		return Arrays.asList(arr);
	}

	/**
	 * Add node child and set child.parent to {@code this}
	 *
	 * @param child the node child to add.
	 */
	public void addChild(DOMNode child) {
		child.parent = this;
		DOMNode[] arr = getChildrenArray();
		if (arr == null) {
			arr = new DOMNode[] { child };
		} else {
			arr = Arrays.copyOf(arr, arr.length + 1);
			arr[arr.length - 1] = child;
		}
		setChildrenArray(arr);
		child.cachedIndexInParent = arr.length - 1;
	}

	void compactChildren() {
	}

	/**
	 * Returns node child at the given index.
	 * 
	 * @param index
	 * @return node child at the given index.
	 */
	public DOMNode getChild(int index) {
		return getChildren().get(index);
	}

	public boolean isClosed() {
		return hasFlag(FLAG_CLOSED);
	}

	void setClosed(boolean closed) {
		setFlag(FLAG_CLOSED, closed);
	}

	public DOMElement getParentElement() {
		DOMNode parent = getParentNode();
		DOMDocument ownerDocument = getOwnerDocument();
		while (parent != null && parent != ownerDocument) {
			if (parent.isElement()) {
				return (DOMElement) parent;
			}
			parent = parent.getParentNode();
		}
		return null;
	}

	public boolean isComment() {
		return getNodeType() == DOMNode.COMMENT_NODE;
	}

	public boolean isProcessingInstruction() {
		return (getNodeType() == DOMNode.PROCESSING_INSTRUCTION_NODE)
				&& ((DOMProcessingInstruction) this).isProcessingInstruction();
	}

	public boolean isProlog() {
		return (getNodeType() == DOMNode.PROCESSING_INSTRUCTION_NODE) && ((DOMProcessingInstruction) this).isProlog();
	}

	public boolean isCDATA() {
		return getNodeType() == DOMNode.CDATA_SECTION_NODE;
	}

	public boolean isDoctype() {
		return getNodeType() == DOMNode.DOCUMENT_TYPE_NODE;
	}

	public boolean isGenericDTDDecl() {
		return getNodeType() == DOMNode.DTD_DECL_NODE;
	}

	public boolean isElement() {
		return getNodeType() == DOMNode.ELEMENT_NODE;
	}

	public boolean isAttribute() {
		return getNodeType() == DOMNode.ATTRIBUTE_NODE;
	}

	public boolean isText() {
		return getNodeType() == DOMNode.TEXT_NODE;
	}

	public boolean isCharacterData() {
		return isCDATA() || isText() || isProcessingInstruction() || isComment();
	}

	public boolean isDTDElementDecl() {
		return getNodeType() == DOMNode.DTD_ELEMENT_DECL_NODE;
	}

	public boolean isDTDAttListDecl() {
		return getNodeType() == DOMNode.DTD_ATT_LIST_NODE;
	}

	public boolean isDTDEntityDecl() {
		return getNodeType() == Node.ENTITY_NODE;
	}

	public boolean isDTDNotationDecl() {
		return getNodeType() == DOMNode.DTD_NOTATION_DECL;
	}

	public boolean isOwnerDocument() {
		return getNodeType() == Node.DOCUMENT_NODE;
	}

	public boolean isChildOfOwnerDocument() {
		if (parent == null) {
			return false;
		}
		return parent.getNodeType() == Node.DOCUMENT_NODE;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getEnd() {
		return end;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getLocalName()
	 */
	@Override
	public String getLocalName() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getParentNode()
	 */
	@Override
	public DOMNode getParentNode() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getFirstChild()
	 */
	@Override
	public DOMNode getFirstChild() {
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		return arr != null && arr.length > 0 ? arr[0] : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	@Override
	public DOMNode getLastChild() {
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		return arr != null && arr.length > 0 ? arr[arr.length - 1] : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getAttributes()
	 */
	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getChildNodes()
	 */
	@Override
	public NodeList getChildNodes() {
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		return arr != null && arr.length > 0 ? new ArrayNodeList(arr) : EMPTY_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	@Override
	public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	@Override
	public org.w3c.dom.Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#compareDocumentPosition(org.w3c.dom.Node)
	 */
	@Override
	public short compareDocumentPosition(org.w3c.dom.Node other) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getBaseURI()
	 */
	@Override
	public String getBaseURI() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getFeature(java.lang.String, java.lang.String)
	 */
	@Override
	public Object getFeature(String arg0, String arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNamespaceURI() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	@Override
	public DOMNode getNextSibling() {
		DOMNode parentNode = getParentNode();
		if (parentNode == null) {
			return null;
		}
		List<DOMNode> children = parentNode.getChildren();
		int currentIndex = cachedIndexInParent;
		if (currentIndex == -1) {
			currentIndex = children.indexOf(this);
			cachedIndexInParent = currentIndex;
		}
		int nextIndex = currentIndex + 1;
		return nextIndex < children.size() ? children.get(nextIndex) : null;
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public String getPrefix() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getPreviousSibling()
	 */
	@Override
	public DOMNode getPreviousSibling() {
		DOMNode parentNode = getParentNode();
		if (parentNode == null) {
			return null;
		}
		List<DOMNode> children = parentNode.getChildren();
		int currentIndex = cachedIndexInParent;
		if (currentIndex == -1) {
			currentIndex = children.indexOf(this);
			cachedIndexInParent = currentIndex;
		}
		int previousIndex = currentIndex - 1;
		return previousIndex >= 0 ? children.get(previousIndex) : null;
	}

	public DOMNode getPreviousNonTextSibling() {
		DOMNode prev = getPreviousSibling();
		while (prev != null && prev.isText()) {
			prev = prev.getPreviousSibling();
		}
		return prev;
	}

	/**
	 * Returns the orphan end element after the given offset which matches the given
	 * tagName and null otherwise.
	 * 
	 * The following sample sample with tagName=foo will returns the <\foo> orphan
	 * end element:
	 * <p>
	 * |
	 * <\foo>
	 * </p>
	 * 
	 * @param offset  the offset.
	 * @param tagName the tag name.
	 * 
	 * @return the orphan end element after the given offset which matches the given
	 *         tagName and null otherwise.
	 */
	public DOMElement getOrphanEndElement(int offset, String tagName) {
		return getOrphanEndElement(offset, tagName, false);
	}

	/**
	 * Returns the orphan end element after the given offset which matches the given
	 * tagName and the first orphan end element otherwise and null otherwise.
	 * 
	 * The following sample sample with tagName=bar will returns the <\foo> orphan
	 * end element:
	 * <p>
	 * |
	 * <\foo>
	 * </p>
	 * 
	 * @param offset    the offset.
	 * @param tagName   the tag name.
	 * @param anyOrphan true if any orphan should be returned and false otherwise.
	 * 
	 * @return the orphan end element after the given offset which matches the given
	 *         tagName and the first orphan end element otherwise and null
	 *         otherwise.
	 */
	public DOMElement getOrphanEndElement(int offset, String tagName, boolean anyOrphan) {
		DOMNode next = getNextSibling();
		if (next == null || !next.isElement()) {
			return null;
		}
		// emp| </employe>
		DOMElement nextElement = (DOMElement) next;
		if ((anyOrphan && nextElement.isOrphanEndTag()) || nextElement.isOrphanEndTagOf(tagName)) {
			return nextElement;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#getTextContent()
	 */
	@Override
	public String getTextContent() throws DOMException {

		switch (getNodeType()) {
		// Text like nodes simply return their node value
		case Node.TEXT_NODE:
		case Node.CDATA_SECTION_NODE:
		case Node.COMMENT_NODE:
		case Node.PROCESSING_INSTRUCTION_NODE:
			return getNodeValue();
		// These special types has to return null
		case Node.DOCUMENT_NODE:
		case Node.DOCUMENT_TYPE_NODE:
		case Node.NOTATION_NODE:
			return null;
		// concatenation of the textContent attribute value of every child node
		default:
			ensureChildren();
			DOMNode[] arr = getChildrenArray();
			if (arr != null && arr.length > 0) {
				final StringBuilder builder = new StringBuilder();
				for (DOMNode child : arr) {
					short nodeType = child.getNodeType();
					if (nodeType == Node.COMMENT_NODE || nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
						// excluding COMMENT_NODE and PROCESSING_INSTRUCTION_NODE nodes.
						continue;
					}
					String text = child.getTextContent();
					if (text != null && !text.isEmpty()) {
						builder.append(text);
					}
				}
				return builder.toString();
			}
			// empty string if the node has no children
			return "";
		}
	}

	@Override
	public Object getUserData(String arg0) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#hasChildNodes()
	 */
	@Override
	public boolean hasChildNodes() {
		ensureChildren();
		DOMNode[] arr = getChildrenArray();
		return arr != null && arr.length > 0;
	}

	@Override
	public org.w3c.dom.Node insertBefore(org.w3c.dom.Node arg0, org.w3c.dom.Node arg1) throws DOMException {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String arg0) {
		return false;
	}

	@Override
	public boolean isEqualNode(org.w3c.dom.Node arg0) {
		return false;
	}

	@Override
	public boolean isSameNode(org.w3c.dom.Node arg0) {
		return false;
	}

	@Override
	public boolean isSupported(String arg0, String arg1) {
		return false;
	}

	@Override
	public String lookupNamespaceURI(String arg0) {
		return null;
	}

	@Override
	public String lookupPrefix(String arg0) {
		return null;
	}

	@Override
	public void normalize() {
	}

	@Override
	public org.w3c.dom.Node removeChild(org.w3c.dom.Node arg0) throws DOMException {
		return null;
	}

	@Override
	public org.w3c.dom.Node replaceChild(org.w3c.dom.Node arg0, org.w3c.dom.Node arg1) throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String arg0) throws DOMException {
	}

	@Override
	public void setPrefix(String arg0) throws DOMException {
	}

	@Override
	public void setTextContent(String arg0) throws DOMException {
	}

	@Override
	public Object setUserData(String arg0, Object arg1, UserDataHandler arg2) {
		return null;
	}

}