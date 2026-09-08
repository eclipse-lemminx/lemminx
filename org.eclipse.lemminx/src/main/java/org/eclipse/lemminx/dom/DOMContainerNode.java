/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for DOM nodes that can have children (elements, documents, DTD
 * declarations). Leaf nodes (text, comment, CDATA, processing instruction,
 * attribute) extend {@link DOMNode} directly.
 */
public abstract class DOMContainerNode extends DOMTreeNode {

	DOMNode firstChild;
	DOMNode lastChild;

	public DOMContainerNode(int start, int end) {
		super(start, end);
	}

	@Override
	public void addChild(DOMNode child) {
		child.parent = this;
		child.nextSibling = null;
		if (firstChild == null) {
			firstChild = child;
		} else {
			lastChild.nextSibling = child;
		}
		lastChild = child;
	}

	@Override
	public Iterable<DOMNode> children() {
		if (firstChild == null) {
			return Collections.emptyList();
		}
		return () -> new Iterator<DOMNode>() {
			DOMNode current = firstChild;

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public DOMNode next() {
				DOMNode node = current;
				current = current.nextSibling;
				return node;
			}
		};
	}

	@Deprecated
	@Override
	public List<DOMNode> getChildren() {
		if (firstChild == null) {
			return Collections.emptyList();
		}
		return new AbstractList<DOMNode>() {
			@Override
			public DOMNode get(int index) {
				int i = 0;
				for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
					if (i == index) {
						return child;
					}
					i++;
				}
				throw new IndexOutOfBoundsException(index);
			}

			@Override
			public int size() {
				int count = 0;
				for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
					count++;
				}
				return count;
			}

			@Override
			public Iterator<DOMNode> iterator() {
				return children().iterator();
			}
		};
	}

	@Override
	public DOMNode getFirstChild() {
		return firstChild;
	}

	@Override
	public DOMNode getLastChild() {
		return lastChild;
	}

	@Override
	public boolean hasChildNodes() {
		return firstChild != null;
	}

	@Override
	public NodeList getChildNodes() {
		if (firstChild == null) {
			return EMPTY_CHILDREN;
		}
		return new NodeList() {
			@Override
			public Node item(int index) {
				int i = 0;
				for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
					if (i == index) {
						return child;
					}
					i++;
				}
				return null;
			}

			@Override
			public int getLength() {
				int count = 0;
				for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
					count++;
				}
				return count;
			}
		};
	}

	@Override
	public DOMNode getChild(int index) {
		int i = 0;
		for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
			if (i == index) {
				return child;
			}
			i++;
		}
		return null;
	}

	@Override
	public DOMNode findNodeBefore(int offset) {
		DOMNode found = null;
		for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
			if (offset <= child.getStart()) {
				break;
			}
			found = child;
		}
		if (found != null) {
			if (offset > found.getStart()) {
				if (offset < found.getEnd()) {
					return found.findNodeBefore(offset);
				}
				DOMNode last = found.getLastChild();
				if (last != null && last.getEnd() == found.getEnd()) {
					return found.findNodeBefore(offset);
				}
				return found;
			}
		}
		return this;
	}

	@Override
	public DOMNode findNodeAt(int offset) {
		DOMNode found = null;
		for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
			if (offset <= child.getStart()) {
				break;
			}
			found = child;
		}
		if (found != null && isIncluded(found, offset)) {
			return found.findNodeAt(offset);
		}
		return this;
	}

	@Override
	public DOMNode findChildWithAttributeValue(String name, String value) {
		for (DOMNode child = firstChild; child != null; child = child.nextSibling) {
			if (child.hasAttribute(name)) {
				String attrValue = child.getAttribute(name);
				if (Objects.equals(attrValue, value)) {
					return child;
				}
			}
		}
		return null;
	}
}
