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
package org.eclipse.lemminx.dom.green;

import org.w3c.dom.Node;

/**
 * Immutable green node for an XML element.
 *
 * <p>All offsets are <em>relative to the node's own start</em> (i.e., relative
 * offset 0 is the first character of the element). The red wrapper adds the
 * absolute document offset when queried.</p>
 */
public final class GreenElement extends GreenNode {

	public static final int NULL_VALUE = -1;

	private final String tag;
	private final int startTagCloseRel;
	private final int endTagOpenRel;
	private final GreenAttr[] attributes;
	private final Object childrenData;

	public GreenElement(int width, boolean closed, String tag, boolean selfClosed,
			int startTagCloseRel, int endTagOpenRel, boolean endTagHasClose,
			GreenAttr[] attributes, GreenNode[] children) {
		super(width, (closed ? CLOSED_FLAG : 0) | (selfClosed ? SUBCLASS_FLAG : 0)
				| (endTagHasClose ? EXTRA_FLAG : 0));
		this.tag = tag;
		this.startTagCloseRel = startTagCloseRel;
		this.endTagOpenRel = endTagOpenRel;
		this.attributes = attributes != null ? attributes : EMPTY_ATTRS;
		if (children == null || children.length == 0) {
			this.childrenData = null;
		} else if (children.length == 1) {
			this.childrenData = children[0];
		} else {
			this.childrenData = children;
		}
	}

	@Override
	public short nodeType() {
		return Node.ELEMENT_NODE;
	}

	public String tag() {
		return tag;
	}

	public boolean selfClosed() {
		return subclassFlag();
	}

	public int startTagCloseRel() {
		return startTagCloseRel;
	}

	public int endTagOpenRel() {
		return endTagOpenRel;
	}

	public int endTagCloseRel() {
		if (!extraFlag()) {
			return NULL_VALUE;
		}
		return width() - 1;
	}

	public GreenAttr[] attributes() {
		return attributes;
	}

	public int attributeCount() {
		return attributes.length;
	}

	@Override
	public int childrenStartRel() {
		if (startTagCloseRel != NULL_VALUE) {
			return startTagCloseRel + 1;
		}
		int cc = childCount();
		if (cc == 0) {
			return 0;
		}
		int childrenWidth = 0;
		for (int i = 0; i < cc; i++) {
			childrenWidth += child(i).width();
		}
		if (endTagOpenRel != NULL_VALUE) {
			return endTagOpenRel - childrenWidth;
		}
		return width() - childrenWidth;
	}

	@Override
	public GreenNode[] children() {
		if (childrenData == null) return EMPTY_CHILDREN;
		if (childrenData instanceof GreenNode[]) return (GreenNode[]) childrenData;
		return new GreenNode[] { (GreenNode) childrenData };
	}

	@Override
	public int childCount() {
		if (childrenData == null) return 0;
		if (childrenData instanceof GreenNode[]) return ((GreenNode[]) childrenData).length;
		return 1;
	}

	@Override
	public GreenNode child(int index) {
		if (childrenData instanceof GreenNode[]) return ((GreenNode[]) childrenData)[index];
		if (index == 0 && childrenData != null) return (GreenNode) childrenData;
		throw new IndexOutOfBoundsException(index);
	}

	public GreenElement withNewChildren(GreenNode[] newChildren, int widthDelta) {
		return new GreenElement(
				width() + widthDelta, closed(), tag, selfClosed(),
				startTagCloseRel,
				endTagOpenRel != NULL_VALUE ? endTagOpenRel + widthDelta : NULL_VALUE,
				extraFlag(),
				attributes, newChildren);
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenElement(newWidth, closed(), tag, selfClosed(),
				startTagCloseRel, endTagOpenRel, extraFlag(),
				attributes, newChildren);
	}
}
