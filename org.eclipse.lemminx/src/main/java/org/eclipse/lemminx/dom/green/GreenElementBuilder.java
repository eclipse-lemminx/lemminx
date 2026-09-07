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

import java.util.Arrays;

/**
 * Mutable builder for {@link GreenElement}.
 *
 * <p>Used during parsing: the scanner populates fields across multiple
 * tokens, then {@link #build()} freezes the result into an immutable
 * {@link GreenElement}.</p>
 */
public final class GreenElementBuilder {

	private final int nodeStart;
	private int nodeEnd;
	private String tag;
	private boolean selfClosed;
	private boolean closed;
	private int startTagCloseOffset = GreenElement.NULL_VALUE;
	private int endTagOpenOffset = GreenElement.NULL_VALUE;
	private boolean endTagHasClose;
	private GreenAttr[] attributes;
	private int attrCount;
	private GreenNode[] children;
	private int childCount;

	public GreenElementBuilder(int nodeStart) {
		this.nodeStart = nodeStart;
		this.nodeEnd = nodeStart;
	}

	public int nodeStart() {
		return nodeStart;
	}

	public int nodeEnd() {
		return nodeEnd;
	}

	public void setNodeEnd(int nodeEnd) {
		this.nodeEnd = nodeEnd;
	}

	public String tag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setSelfClosed(boolean selfClosed) {
		this.selfClosed = selfClosed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public void setStartTagCloseOffset(int offset) {
		this.startTagCloseOffset = offset;
	}

	public void setEndTagOpenOffset(int offset) {
		this.endTagOpenOffset = offset;
	}

	public void setEndTagHasClose(boolean endTagHasClose) {
		this.endTagHasClose = endTagHasClose;
	}

	public void addAttribute(GreenAttr attr) {
		if (attributes == null) {
			attributes = new GreenAttr[4];
			attrCount = 0;
		}
		if (attrCount == attributes.length) {
			attributes = Arrays.copyOf(attributes, attributes.length * 2);
		}
		attributes[attrCount++] = attr;
	}

	public void addChild(GreenNode child) {
		if (children == null) {
			children = new GreenNode[4];
			childCount = 0;
		}
		if (childCount == children.length) {
			children = Arrays.copyOf(children, children.length * 2);
		}
		children[childCount++] = child;
	}

	public GreenElement build() {
		int width = nodeEnd - nodeStart;
		int stcRel = startTagCloseOffset != GreenElement.NULL_VALUE
				? startTagCloseOffset - nodeStart : GreenElement.NULL_VALUE;
		int etoRel = endTagOpenOffset != GreenElement.NULL_VALUE
				? endTagOpenOffset - nodeStart : GreenElement.NULL_VALUE;

		GreenAttr[] attrs = attributes != null
				? (attrCount == attributes.length ? attributes : Arrays.copyOf(attributes, attrCount))
				: null;
		GreenNode[] kids = children != null
				? (childCount == children.length ? children : Arrays.copyOf(children, childCount))
				: null;

		return new GreenElement(width, closed, tag, selfClosed,
				stcRel, etoRel, endTagHasClose, attrs, kids);
	}
}
