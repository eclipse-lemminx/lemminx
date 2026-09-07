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

/**
 * Immutable green node — the backbone of the red-green tree.
 *
 * <p>Green nodes store <em>width</em> (character span), not absolute offsets.
 * They carry no parent pointer and are safe to share across document versions.
 * The mutable red wrapper (the existing {@code DOMNode} hierarchy) computes
 * absolute positions lazily from the green tree.</p>
 *
 * <p>All subclasses must be immutable: every field is {@code final} and every
 * collection is an unmodifiable snapshot taken at construction time.</p>
 */
public abstract class GreenNode {

	public static final GreenNode[] EMPTY_CHILDREN = new GreenNode[0];
	public static final GreenAttr[] EMPTY_ATTRS = new GreenAttr[0];

	static final int CLOSED_FLAG = 1 << 31;
	static final int SUBCLASS_FLAG = 1 << 30;
	static final int EXTRA_FLAG = 1 << 29;
	private static final int WIDTH_MASK = 0x1FFFFFFF;

	private final int widthAndFlags;

	protected GreenNode(int width, int flags) {
		this.widthAndFlags = width | flags;
	}

	public abstract short nodeType();

	public final int width() {
		return widthAndFlags & WIDTH_MASK;
	}

	public final boolean closed() {
		return (widthAndFlags & CLOSED_FLAG) != 0;
	}

	protected final boolean subclassFlag() {
		return (widthAndFlags & SUBCLASS_FLAG) != 0;
	}

	protected final boolean extraFlag() {
		return (widthAndFlags & EXTRA_FLAG) != 0;
	}

	public GreenNode[] children() {
		return EMPTY_CHILDREN;
	}

	public int childCount() {
		return children().length;
	}

	public GreenNode child(int index) {
		return children()[index];
	}

	public GreenNode withReplacedChild(int index, GreenNode newChild) {
		GreenNode[] old = children();
		GreenNode[] copy = new GreenNode[old.length];
		System.arraycopy(old, 0, copy, 0, old.length);
		int deltaWidth = newChild.width() - old[index].width();
		copy[index] = newChild;
		return replaceChildren(copy, width() + deltaWidth);
	}

	public int childrenStartRel() {
		return 0;
	}

	protected abstract GreenNode replaceChildren(GreenNode[] newChildren, int newWidth);
}
