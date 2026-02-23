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

import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;

/**
 * An attribute node.
 *
 */
public class DOMAttr extends DOMNode implements org.w3c.dom.Attr {

	public static final String XMLNS_ATTR = "xmlns";
	public static final String XMLNS_NO_DEFAULT_ATTR = "xmlns:";

	// Memory optimization Phase 6: Store offsets directly instead of creating inner
	// class objects
	// For 546K attributes, this eliminates 1,092,576 objects (AttrName + AttrValue)
	// Saves ~35 MB of heap memory

	private String name; // Only cached if set programmatically (rare)

	// Attribute name offsets (instead of AttrName object)
	private int nameStart = NULL_VALUE;
	private int nameEnd = NULL_VALUE;

	private int delimiter;

	// Attribute value offsets (instead of AttrValue object)
	private String value; // Only cached if set programmatically (rare)
	private int valueStart = NULL_VALUE;
	private int valueEnd = NULL_VALUE;

	private final DOMNode ownerElement;

	public DOMAttr(String name, DOMNode ownerElement) {
		this(name, NULL_VALUE, NULL_VALUE, ownerElement);
	}

	public DOMAttr(String name, int start, int end, DOMNode ownerElement) {
		super(NULL_VALUE, NULL_VALUE);
		this.name = name;
		this.delimiter = NULL_VALUE;
		this.nameStart = start;
		this.nameEnd = end;
		this.ownerElement = ownerElement;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getNodeType()
	 */
	@Override
	public short getNodeType() {
		return DOMNode.ATTRIBUTE_NODE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getNodeName()
	 */
	@Override
	public String getNodeName() {
		return getName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#getName()
	 */
	@Override
	public String getName() {
		// Memory optimization: Extract name from document instead of caching
		if (nameStart != NULL_VALUE && nameEnd != NULL_VALUE) {
			// Name is in the document, extract it
			return getOwnerDocument().getTextSequence().subSequence(nameStart, nameEnd).toString();
		}
		// Name was set programmatically or doesn't exist
		return name;
	}

	@Override
	public String getNodeValue() throws DOMException {
		return getValue();
	}

	@Override
	public String getLocalName() {
		String name = getName();
		int colonIndex = name.indexOf(":");
		if (colonIndex > 0) {
			return name.substring(colonIndex + 1);
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#getOwnerElement()
	 */
	public DOMElement getOwnerElement() {
		return ownerElement.isElement() ? (DOMElement) ownerElement : null;
	}

	@Override
	public DOMDocument getOwnerDocument() {
		return ownerElement != null ? ownerElement.getOwnerDocument() : null;
	}

	/*
	 *
	 * Returns the attribute's value without quotes. Memory optimization: Extract
	 * value from document instead of caching
	 */
	@Override
	public String getValue() {
		String originalValue = getOriginalValue();
		if (originalValue == null) {
			return null;
		}
		// Remove quotes if present
		if (originalValue.length() >= 2) {
			char first = originalValue.charAt(0);
			char last = originalValue.charAt(originalValue.length() - 1);
			if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
				return originalValue.substring(1, originalValue.length() - 1);
			}
		}
		return originalValue;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#getSchemaTypeInfo()
	 */
	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#getSpecified()
	 */
	@Override
	public boolean getSpecified() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#isId()
	 */
	@Override
	public boolean isId() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Attr#setValue(java.lang.String)
	 */
	@Override
	public void setValue(String value) throws DOMException {
		setValue(value, NULL_VALUE, NULL_VALUE);
	}

	public DOMRange getNodeAttrName() {
		// Return a DOMRange implementation for the name
		if (nameStart == NULL_VALUE) {
			return null;
		}
		return new DOMRange() {
			@Override
			public int getStart() {
				return nameStart;
			}

			@Override
			public int getEnd() {
				return nameEnd;
			}

			@Override
			public DOMDocument getOwnerDocument() {
				return DOMAttr.this.getOwnerDocument();
			}
		};
	}

	public void setDelimiter(int delimiter) {
		this.delimiter = delimiter;
	}

	public boolean hasDelimiter() {
		return delimiter != NULL_VALUE;
	}

	/**
	 * Get original attribute value from the document.
	 *
	 * This will include quotations (", ').
	 *
	 * @return attribute value with quotations if it had them.
	 */
	public String getOriginalValue() {
		// Memory optimization: Extract from document instead of caching
		if (valueStart != NULL_VALUE && delimiter < valueStart) {
			return getOwnerDocument().getTextSequence().subSequence(valueStart, valueEnd).toString();
		}
		return value;
	}

	public void setValue(String value, int start, int end) {
		// When value is set programmatically, we need to cache it
		// This is rare compared to parsing from document
		this.value = value;
		this.valueStart = start;
		this.valueEnd = end;
	}

	public DOMRange getNodeAttrValue() {
		// Return a DOMRange implementation for the value
		if (valueStart == NULL_VALUE) {
			return null;
		}
		return new DOMRange() {
			@Override
			public int getStart() {
				return valueStart;
			}

			@Override
			public int getEnd() {
				return valueEnd;
			}

			@Override
			public DOMDocument getOwnerDocument() {
				return DOMAttr.this.getOwnerDocument();
			}
		};
	}

	public boolean valueContainsOffset(int offset) {
		return valueStart != NULL_VALUE && offset >= valueStart && offset < valueEnd;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getPrefix()
	 */
	@Override
	public String getPrefix() {
		String name = getName();
		if (name == null) {
			return null;
		}
		String prefix = null;
		int index = name.indexOf(":"); //$NON-NLS-1$
		if (index != -1) {
			prefix = name.substring(0, index);
		}
		return prefix;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getNamespaceURI()
	 */
	@Override
	public String getNamespaceURI() {
		if (ownerElement == null || ownerElement.getNodeType() != Node.ELEMENT_NODE) {
			return null;
		}
		String prefix = getPrefix();
		// Try to get xmlns attribute from the element
		return ((DOMElement) ownerElement).getNamespaceURI(prefix);
	}

	/**
	 * Returns true if attribute name is a xmlns attribute and false otherwise.
	 *
	 * @param attributeName
	 * @return true if attribute name is a xmlns attribute and false otherwise.
	 */
	public boolean isXmlns() {
		return isXmlns(getName());
	}

	public static boolean isXmlns(String attributeName) {
		return attributeName.startsWith(XMLNS_ATTR);
	}

	/**
	 * Returns true if attribute name is the default xmlns attribute and false
	 * otherwise.
	 *
	 * @param attributeName
	 * @return true if attribute name is the default xmlns attribute and false
	 *         otherwise.
	 */
	public boolean isDefaultXmlns() {
		return isDefaultXmlns(getName());
	}

	public static boolean isDefaultXmlns(String attributeName) {
		return attributeName.equals(XMLNS_ATTR);
	}

	public String extractPrefixFromXmlns() {
		String name = getName();
		if (isDefaultXmlns()) {
			return name.substring(XMLNS_ATTR.length(), name.length());
		}
		return name.substring(XMLNS_NO_DEFAULT_ATTR.length(), name.length());
	}

	/**
	 * Returns the prefix if the given URI matches this attributes value.
	 *
	 * If the URI doesnt match, null is returned.
	 *
	 * @param uri
	 * @return
	 */
	public String getPrefixIfMatchesURI(String uri) {
		if (isXmlns()) {
			String value = getValue();
			if (value != null && value.equals(uri)) {
				if (isDefaultXmlns()) {
					// xmlns="http://"
					return null;
				}
				// xmlns:xxx="http://"
				return extractPrefixFromXmlns();
			}
		}
		return null;
	}

	/**
	 * Returns true if attribute name is the no default xmlns attribute and false
	 * otherwise.
	 *
	 * @param attributeName
	 * @return true if attribute name is the no default xmlns attribute and false
	 *         otherwise.
	 */
	public boolean isNoDefaultXmlns() {
		return isNoDefaultXmlns(getName());
	}

	public static boolean isNoDefaultXmlns(String attributeName) {
		return attributeName.startsWith(XMLNS_NO_DEFAULT_ATTR);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.w3c.dom.Node#getNextSibling()
	 */
	@Override
	public DOMNode getNextSibling() {
		DOMNode parentNode = getOwnerElement();
		if (parentNode == null) {
			return null;
		}
		List<DOMAttr> children = parentNode.getAttributeNodes();
		int nextIndex = children.indexOf(this) + 1;
		return nextIndex < children.size() ? children.get(nextIndex) : null;
	}

	public boolean isIncluded(int offset) {
		return DOMNode.isIncluded(getStart(), getEnd(), offset);
	}

	@Override
	public int getStart() {
		return nameStart;
	}

	@Override
	public int getEnd() {
		if (valueStart != NULL_VALUE) {
			// <foo attr="value"| >
			return valueEnd;
		}
		if (hasDelimiter()) {
			// <foo attr=| >
			return delimiter + 1;
		}
		// <foo attr| >
		return nameEnd;
	}

	@Override
	public int hashCode() {
		String name = getName();
		String value = getValue(); // Extract on demand
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DOMAttr other = (DOMAttr) obj;
		String name = getName();
		if (name == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!name.equals(other.getName())) {
			return false;
		}
		String value = getValue();
		if (value == null) {
			if (other.getValue() != null) {
				return false;
			}
		} else if (!value.equals(other.getValue())) {
			return false;
		}
		return true;
	}

	public int getDelimiterOffset() {
		return delimiter;
	}

}
