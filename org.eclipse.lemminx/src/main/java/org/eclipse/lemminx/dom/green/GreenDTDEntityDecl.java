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
 * Immutable green node for a DTD ENTITY declaration ({@code <!ENTITY ...>}).
 */
public final class GreenDTDEntityDecl extends GreenDTDDeclNode {

	private final GreenDTDParam percent;
	private final GreenDTDParam value;
	private final GreenDTDParam kind;
	private final GreenDTDParam publicId;
	private final GreenDTDParam systemId;

	public GreenDTDEntityDecl(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children,
			GreenDTDParam percent, GreenDTDParam value,
			GreenDTDParam kind, GreenDTDParam publicId,
			GreenDTDParam systemId) {
		super(width, closed, unrecognized, declType, name, parameters, children);
		this.percent = percent;
		this.value = value;
		this.kind = kind;
		this.publicId = publicId;
		this.systemId = systemId;
	}

	@Override
	public short nodeType() {
		return Node.ENTITY_NODE;
	}

	public GreenDTDParam percent() {
		return percent;
	}

	public GreenDTDParam value() {
		return value;
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
		return new GreenDTDEntityDecl(newWidth, closed(), unrecognized(), declType(),
				name(), parameters(), newChildren,
				percent, value, kind, publicId, systemId);
	}
}
