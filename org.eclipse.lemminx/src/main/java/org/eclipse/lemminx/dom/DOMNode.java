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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

	DOMNode nextSibling;

	DOMNode parent;

	static final NodeList EMPTY_CHILDREN = new NodeList() {

		@Override
		public Node item(int index) {
			return null;
		}

		@Override
		public int getLength() {
			return 0;
		}
	};

	static class XMLNamedNodeMap<T extends DOMNode> extends ArrayList<T> implements NamedNodeMap {

		private static final long serialVersionUID = 1L;

		@Override
		public int getLength() {
			return super.size();
		}

		@Override
		public T getNamedItem(String name) {
			for (T node : this) {
				if (name.equals(node.getNodeName())) {
					return node;
				}
			}
			return null;
		}

		@Override
		public T getNamedItemNS(String name, String arg1) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public T item(int index) {
			return super.get(index);
		}

		@Override
		public T removeNamedItem(String arg0) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public T removeNamedItemNS(String arg0, String arg1) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public T setNamedItem(Node arg0) throws DOMException {
			throw new UnsupportedOperationException();
		}

		@Override
		public T setNamedItemNS(Node arg0) throws DOMException {
			throw new UnsupportedOperationException();
		}

	}

	DOMNode() {
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
		result.append(getStart());
		result.append(", end: ");
		result.append(getEnd());
		result.append(", name: ");
		result.append(getNodeName());
		result.append(", closed: ");
		result.append(isClosed());
		DOMNode fc = getFirstChild();
		if (fc != null) {
			result.append(", \n");
			for (int i = 0; i < indent + 1; i++) {
				result.append("\t");
			}
			result.append("children:[");
			boolean first = true;
			for (DOMNode node = fc; node != null; node = node.nextSibling) {
				if (!first) {
					result.append(",");
				}
				result.append("\n");
				result.append(node.toString(indent + 2));
				first = false;
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
		return this;
	}

	public DOMNode findNodeAt(int offset) {
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
			for (DOMAttr attr : node.attributes()) {
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
			for (DOMNode child = node.getFirstChild(); child != null; child = child.nextSibling) {
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
		for (DOMAttr attr : attributes()) {
			String attrName = attr.getName();
			if (prefix == null) {
				if (suffix.equals(attrName)) {
					return attr;
				}
			} else {
				int prefixLen = prefix.length();
				int suffixLen = suffix.length();
				if (attrName.length() == prefixLen + 1 + suffixLen
						&& attrName.startsWith(prefix)
						&& attrName.charAt(prefixLen) == ':'
						&& attrName.regionMatches(prefixLen + 1, suffix, 0, suffixLen)) {
					return attr;
				}
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
		int i = 0;
		for (DOMAttr attr : attributes()) {
			if (i == index) {
				return attr;
			}
			i++;
		}
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
		return getFirstAttr() != null;
	}

	/**
	 * Returns true if this node has exactly one attribute.
	 *
	 * @return true if this node has exactly one attribute and false otherwise.
	 */
	public boolean hasSingleAttribute() {
		DOMAttr first = getFirstAttr();
		return first != null && first.nextAttr() == null;
	}

	/**
	 * Returns the last attribute of this node, or null if there are no
	 * attributes.
	 *
	 * @return the last attribute of this node, or null.
	 */
	public DOMAttr getLastAttr() {
		DOMAttr first = getFirstAttr();
		if (first == null) {
			return null;
		}
		DOMAttr last = first;
		for (DOMAttr next = first.nextAttr(); next != null; next = next.nextAttr()) {
			last = next;
		}
		return last;
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

	public Iterable<DOMAttr> attributes() {
		DOMAttr first = getFirstAttr();
		if (first == null) {
			return Collections.emptyList();
		}
		return () -> new Iterator<DOMAttr>() {
			DOMAttr current = first;

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public DOMAttr next() {
				DOMAttr attr = current;
				current = current.nextAttr();
				return attr;
			}
		};
	}

	/**
	 * @deprecated Use {@link #attributes()} to traverse attributes without
	 *             allocating a list.
	 */
	@Deprecated
	public List<DOMAttr> getAttributeNodes() {
		return createAttributeList(getFirstAttr());
	}

	static void addAttribute(DOMAttr attr, DOMNode owner) {
		attr.nextSibling = null;
		DOMAttr first = owner.getFirstAttr();
		if (first == null) {
			owner.setFirstAttr(attr);
		} else {
			DOMAttr last = first;
			while (last.nextAttr() != null) {
				last = last.nextAttr();
			}
			last.nextSibling = attr;
		}
	}

	DOMAttr getFirstAttr() {
		return null;
	}

	void setFirstAttr(DOMAttr attr) {
	}

	static List<DOMAttr> createAttributeList(DOMAttr firstAttr) {
		if (firstAttr == null) {
			return Collections.emptyList();
		}
		return new AbstractList<DOMAttr>() {
			@Override
			public DOMAttr get(int index) {
				int i = 0;
				for (DOMAttr attr = firstAttr; attr != null; attr = attr.nextAttr()) {
					if (i == index) {
						return attr;
					}
					i++;
				}
				throw new IndexOutOfBoundsException(index);
			}

			@Override
			public int size() {
				int count = 0;
				for (DOMAttr attr = firstAttr; attr != null; attr = attr.nextAttr()) {
					count++;
				}
				return count;
			}

			@Override
			public Iterator<DOMAttr> iterator() {
				return new Iterator<DOMAttr>() {
					DOMAttr current = firstAttr;

					@Override
					public boolean hasNext() {
						return current != null;
					}

					@Override
					public DOMAttr next() {
						DOMAttr node = current;
						current = current.nextAttr();
						return node;
					}
				};
			}
		};
	}

	static NamedNodeMap createAttributeNamedNodeMap(DOMAttr firstAttr) {
		if (firstAttr == null) {
			return null;
		}
		return new NamedNodeMap() {
			@Override
			public int getLength() {
				int count = 0;
				for (DOMAttr attr = firstAttr; attr != null; attr = attr.nextAttr()) {
					count++;
				}
				return count;
			}

			@Override
			public Node getNamedItem(String name) {
				for (DOMAttr attr = firstAttr; attr != null; attr = attr.nextAttr()) {
					if (name.equals(attr.getNodeName())) {
						return attr;
					}
				}
				return null;
			}

			@Override
			public Node getNamedItemNS(String ns, String local) throws DOMException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Node item(int index) {
				int i = 0;
				for (DOMAttr attr = firstAttr; attr != null; attr = attr.nextAttr()) {
					if (i == index) {
						return attr;
					}
					i++;
				}
				return null;
			}

			@Override
			public Node removeNamedItem(String name) throws DOMException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Node removeNamedItemNS(String ns, String local) throws DOMException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Node setNamedItem(Node arg) throws DOMException {
				throw new UnsupportedOperationException();
			}

			@Override
			public Node setNamedItemNS(Node arg) throws DOMException {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Returns the first child having an attribute with the given name and value,
	 * or null if none found.
	 *
	 * @param name  name of attribute
	 * @param value value of attribute
	 * @return the first matching child, or null
	 */
	public DOMNode findChildWithAttributeValue(String name, String value) {
		return null;
	}

	/**
	 * Returns an iterable over this node's children. Does not allocate a list.
	 *
	 * @return an iterable over the children.
	 */
	public Iterable<DOMNode> children() {
		return Collections.emptyList();
	}

	/**
	 * Returns the node children as a list.
	 *
	 * @return the node children.
	 * @deprecated Use {@link #children()} to traverse children without allocating a
	 *             list.
	 */
	@Deprecated
	public List<DOMNode> getChildren() {
		return Collections.emptyList();
	}

	/**
	 * Add node child and set child.parent to {@code this}
	 *
	 * @param child the node child to add.
	 */
	public void addChild(DOMNode child) {
		child.parent = this;
		child.nextSibling = null;
	}

	/**
	 * Returns node child at the given index.
	 *
	 * @param index
	 * @return node child at the given index.
	 */
	public DOMNode getChild(int index) {
		return null;
	}

	public boolean isClosed() {
		return false;
	}

	void setClosed(boolean closed) {
	}

	void setEnd(int end) {
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
		return NULL_VALUE;
	}

	@Override
	public int getEnd() {
		return NULL_VALUE;
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
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getLastChild()
	 */
	@Override
	public DOMNode getLastChild() {
		return null;
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
		return EMPTY_CHILDREN;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#appendChild(org.w3c.dom.Node)
	 */
	@Override
	public Node appendChild(Node newChild) throws DOMException {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#cloneNode(boolean)
	 */
	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.w3c.dom.Node#compareDocumentPosition(org.w3c.dom.Node)
	 */
	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
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
		return nextSibling;
	}

	public boolean hasSiblings() {
		return parent != null && parent.getFirstChild() != parent.getLastChild();
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
		if (parent == null) {
			return null;
		}
		DOMNode prev = null;
		for (DOMNode child = parent.getFirstChild(); child != null; child = child.nextSibling) {
			if (child == this) {
				return prev;
			}
			prev = child;
		}
		return null;
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
			DOMNode fc = getFirstChild();
			if (fc != null) {
				final StringBuilder builder = new StringBuilder();
				for (DOMNode child = fc; child != null; child = child.nextSibling) {
					short nodeType = child.getNodeType();
					if (nodeType == Node.COMMENT_NODE || nodeType == Node.PROCESSING_INSTRUCTION_NODE) {
						continue;
					}
					String text = child.getTextContent();
					if (text != null && !text.isEmpty()) {
						builder.append(text);
					}
				}
				return builder.toString();
			}
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
		return false;
	}

	@Override
	public Node insertBefore(Node arg0, Node arg1) throws DOMException {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String arg0) {
		return false;
	}

	@Override
	public boolean isEqualNode(Node arg0) {
		return false;
	}

	@Override
	public boolean isSameNode(Node arg0) {
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
	public Node removeChild(Node arg0) throws DOMException {
		return null;
	}

	@Override
	public Node replaceChild(Node arg0, Node arg1) throws DOMException {
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