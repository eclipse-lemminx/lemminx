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
 * Immutable green node for a DTD NOTATION declaration ({@code <!NOTATION ...>}).
 */
public final class GreenDTDNotationDecl extends GreenDTDDeclNode {

	private final GreenDTDParam kind;
	private final GreenDTDParam publicId;
	private final GreenDTDParam systemId;

	public GreenDTDNotationDecl(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children,
			GreenDTDParam kind, GreenDTDParam publicId,
			GreenDTDParam systemId) {
		super(width, closed, unrecognized, declType, name, parameters, children);
		this.kind = kind;
		this.publicId = publicId;
		this.systemId = systemId;
	}

	@Override
	public short nodeType() {
		return DOMNode.DTD_NOTATION_DECL;
	}

	public GreenDTDParam kind() {
		return kind;
	}

	public GreenDTDParam publicId() {
		return publicId;
	}

	public GreenDTDParam systemId() {
		return systemId;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDTDNotationDecl(newWidth, closed(), unrecognized(), declType(),
				name(), parameters(), newChildren,
				kind, publicId, systemId);
	}
}
