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
 * Immutable green node for a CDATA section ({@code <![CDATA[ ... ]]>}).
 */
public final class GreenCDATA extends GreenNode {

	private final int startContentRel;
	private final int endContentRel;

	public GreenCDATA(int width, boolean closed, int startContentRel, int endContentRel) {
		super(width, closed ? CLOSED_FLAG : 0);
		this.startContentRel = startContentRel;
		this.endContentRel = endContentRel;
	}

	@Override
	public short nodeType() {
		return Node.CDATA_SECTION_NODE;
	}

	public int startContentRel() {
		return startContentRel;
	}

	public int endContentRel() {
		return endContentRel;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenCDATA(newWidth, closed(), startContentRel, endContentRel);
	}
}
