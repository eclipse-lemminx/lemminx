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

import org.eclipse.lemminx.dom.DOMNode;

/**
 * Immutable green node for a DTD ELEMENT declaration ({@code <!ELEMENT ...>}).
 */
public final class GreenDTDElementDecl extends GreenDTDDeclNode {

	private final GreenDTDParam category;
	private final GreenDTDParam content;

	public GreenDTDElementDecl(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children,
			GreenDTDParam category, GreenDTDParam content) {
		super(width, closed, unrecognized, declType, name, parameters, children);
		this.category = category;
		this.content = content;
	}

	@Override
	public short nodeType() {
		return DOMNode.DTD_ELEMENT_DECL_NODE;
	}

	public GreenDTDParam category() {
		return category;
	}

	public GreenDTDParam content() {
		return content;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDTDElementDecl(newWidth, closed(), unrecognized(), declType(),
				name(), parameters(), newChildren,
				category, content);
	}
}
