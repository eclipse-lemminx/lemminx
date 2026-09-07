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
 * Immutable green node for a processing instruction ({@code <?target ...?>})
 * or XML prolog ({@code <?xml ...?>}).
 */
public final class GreenProcessingInstruction extends GreenNode {

	private final boolean startTagClose;
	private final String target;
	private final boolean prolog;
	private final boolean processingInstruction;
	private final int startContentRel;
	private final int endContentRel;
	private final int endTagOpenRel;
	private final GreenAttr[] attributes;

	public GreenProcessingInstruction(int width, boolean closed, boolean startTagClose,
			String target, boolean prolog, boolean processingInstruction,
			int startContentRel, int endContentRel, int endTagOpenRel,
			GreenAttr[] attributes) {
		super(width, closed ? CLOSED_FLAG : 0);
		this.startTagClose = startTagClose;
		this.target = target;
		this.prolog = prolog;
		this.processingInstruction = processingInstruction;
		this.startContentRel = startContentRel;
		this.endContentRel = endContentRel;
		this.endTagOpenRel = endTagOpenRel;
		this.attributes = attributes != null ? attributes : EMPTY_ATTRS;
	}

	@Override
	public short nodeType() {
		return Node.PROCESSING_INSTRUCTION_NODE;
	}

	public boolean startTagClose() {
		return startTagClose;
	}

	public String target() {
		return target;
	}

	public boolean prolog() {
		return prolog;
	}

	public boolean processingInstruction() {
		return processingInstruction;
	}

	public int startContentRel() {
		return startContentRel;
	}

	public int endContentRel() {
		return endContentRel;
	}

	public int endTagOpenRel() {
		return endTagOpenRel;
	}

	public GreenAttr[] attributes() {
		return attributes;
	}

	@Override
	protected GreenNode replaceChildren(GreenNode[] newChildren, int newWidth) {
		return new GreenProcessingInstruction(newWidth, closed(), startTagClose,
				target, prolog, processingInstruction,
				startContentRel, endContentRel, endTagOpenRel, attributes);
	}
}
