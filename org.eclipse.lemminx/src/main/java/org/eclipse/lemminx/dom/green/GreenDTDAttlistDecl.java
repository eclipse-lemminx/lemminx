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
 * Immutable green node for a DTD ATTLIST declaration ({@code <!ATTLIST ...>}).
 */
public final class GreenDTDAttlistDecl extends GreenDTDDeclNode {

	private final GreenDTDParam attributeName;
	private final GreenDTDParam attributeType;
	private final GreenDTDParam attributeValue;
	private final GreenDTDAttlistDecl[] internalDecls;

	public GreenDTDAttlistDecl(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children,
			GreenDTDParam attributeName, GreenDTDParam attributeType,
			GreenDTDParam attributeValue,
			GreenDTDAttlistDecl[] internalDecls) {
		super(width, closed, unrecognized, declType, name, parameters, children);
		this.attributeName = attributeName;
		this.attributeType = attributeType;
		this.attributeValue = attributeValue;
		this.internalDecls = internalDecls;
	}

	@Override
	public short nodeType() {
		return DOMNode.DTD_ATT_LIST_NODE;
	}

	public GreenDTDParam attributeName() {
		return attributeName;
	}

	public GreenDTDParam attributeType() {
		return attributeType;
	}

	public GreenDTDParam attributeValue() {
		return attributeValue;
	}

	public GreenDTDAttlistDecl[] internalDecls() {
		return internalDecls;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDTDAttlistDecl(newWidth, closed(), unrecognized(), declType(),
				name(), parameters(), newChildren,
				attributeName, attributeType, attributeValue, internalDecls);
	}
}
