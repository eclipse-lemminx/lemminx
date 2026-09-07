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
 * Immutable green node for a generic DTD declaration.
 *
 * <p>Serves as base data for {@link GreenDocumentType},
 * {@link GreenDTDAttlistDecl}, {@link GreenDTDElementDecl},
 * {@link GreenDTDEntityDecl}, and {@link GreenDTDNotationDecl}.</p>
 */
public class GreenDTDDeclNode extends GreenNode {

	public static final GreenDTDParam[] EMPTY_PARAMS = new GreenDTDParam[0];

	private final GreenDTDParam unrecognized;
	private final GreenDTDParam declType;
	private final GreenDTDParam name;
	private final GreenDTDParam[] parameters;
	private final GreenNode[] children;

	public GreenDTDDeclNode(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children) {
		super(width, closed ? CLOSED_FLAG : 0);
		this.unrecognized = unrecognized;
		this.declType = declType;
		this.name = name;
		this.parameters = parameters != null ? parameters : EMPTY_PARAMS;
		this.children = children != null ? children : EMPTY_CHILDREN;
	}

	@Override
	public short nodeType() {
		return DOMNode.DTD_DECL_NODE;
	}

	public GreenDTDParam unrecognized() {
		return unrecognized;
	}

	public GreenDTDParam declType() {
		return declType;
	}

	public GreenDTDParam name() {
		return name;
	}

	public GreenDTDParam[] parameters() {
		return parameters;
	}

	@Override
	public int childrenStartRel() {
		if (childCount() == 0) {
			return 0;
		}
		int childrenWidth = 0;
		for (GreenNode child : children()) {
			childrenWidth += child.width();
		}
		return width() - childrenWidth;
	}

	@Override
	public GreenNode[] children() {
		return children;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDTDDeclNode(newWidth, closed(), unrecognized, declType,
				name, parameters, newChildren);
	}
}
