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
 * Immutable green node for the document root.
 *
 * <p>The document root's width equals the full text length. Its children are
 * the top-level nodes (prolog, doctype, root element, comments, PIs).</p>
 */
public final class GreenDocument extends GreenNode {

	private final GreenNode[] children;

	public GreenDocument(int width, GreenNode[] children) {
		super(width, CLOSED_FLAG);
		this.children = children != null ? children : EMPTY_CHILDREN;
	}

	@Override
	public short nodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	public GreenNode[] children() {
		return children;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDocument(newWidth, newChildren);
	}
}
