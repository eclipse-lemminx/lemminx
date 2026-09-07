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
 * Immutable green node for a text content node.
 */
public final class GreenText extends GreenNode {

	private static final int CACHE_SIZE = 128;
	private static final GreenText[] WHITESPACE_CACHE = new GreenText[CACHE_SIZE];
	static {
		for (int i = 0; i < CACHE_SIZE; i++) {
			WHITESPACE_CACHE[i] = new GreenText(i, true);
		}
	}

	public GreenText(int width, boolean whitespace) {
		super(width, CLOSED_FLAG | (whitespace ? SUBCLASS_FLAG : 0));
	}

	public static GreenText whitespace(int width) {
		if (width >= 0 && width < CACHE_SIZE) {
			return WHITESPACE_CACHE[width];
		}
		return new GreenText(width, true);
	}

	@Override
	public short nodeType() {
		return Node.TEXT_NODE;
	}

	public boolean whitespace() {
		return subclassFlag();
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return whitespace() ? whitespace(newWidth) : new GreenText(newWidth, false);
	}
}
