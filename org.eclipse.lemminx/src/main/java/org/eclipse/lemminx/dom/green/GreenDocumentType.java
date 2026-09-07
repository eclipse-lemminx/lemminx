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
 * Immutable green node for a DOCTYPE declaration ({@code <!DOCTYPE ...>}).
 */
public final class GreenDocumentType extends GreenDTDDeclNode {

	private final GreenDTDParam kind;
	private final GreenDTDParam publicId;
	private final GreenDTDParam systemId;
	private final GreenDTDParam internalSubset;

	public GreenDocumentType(int width, boolean closed,
			GreenDTDParam unrecognized, GreenDTDParam declType,
			GreenDTDParam name, GreenDTDParam[] parameters,
			GreenNode[] children,
			GreenDTDParam kind, GreenDTDParam publicId,
			GreenDTDParam systemId, GreenDTDParam internalSubset) {
		super(width, closed, unrecognized, declType, name, parameters, children);
		this.kind = kind;
		this.publicId = publicId;
		this.systemId = systemId;
		this.internalSubset = internalSubset;
	}

	@Override
	public short nodeType() {
		return Node.DOCUMENT_TYPE_NODE;
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

	public GreenDTDParam internalSubset() {
		return internalSubset;
	}

	@Override
	public int childrenStartRel() {
		if (internalSubset != null) {
			return internalSubset.startRel() + 1;
		}
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
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenDocumentType(newWidth, closed(), unrecognized(), declType(),
				name(), parameters(), newChildren,
				kind, publicId, systemId, internalSubset);
	}
}
