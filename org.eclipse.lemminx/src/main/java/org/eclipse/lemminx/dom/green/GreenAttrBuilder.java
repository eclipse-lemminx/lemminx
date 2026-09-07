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

/**
 * Mutable builder for {@link GreenAttr}.
 *
 * <p>Offsets are collected as absolute positions during scanning; the builder
 * converts them to element-relative offsets at {@link #build(int)} time.</p>
 */
public final class GreenAttrBuilder {

	private int nameStart = GreenAttr.NULL_VALUE;
	private int nameEnd = GreenAttr.NULL_VALUE;
	private int delimiter = GreenAttr.NULL_VALUE;
	private int valueStart = GreenAttr.NULL_VALUE;
	private int valueEnd = GreenAttr.NULL_VALUE;

	public void setName(int start, int end) {
		this.nameStart = start;
		this.nameEnd = end;
	}

	public void setDelimiter(int offset) {
		this.delimiter = offset;
	}

	public void setValue(int start, int end) {
		this.valueStart = start;
		this.valueEnd = end;
	}

	/**
	 * Builds an immutable {@link GreenAttr} with offsets relative to the given
	 * element start position.
	 *
	 * @param elementStart the absolute start offset of the owning element
	 * @return immutable attribute
	 */
	public GreenAttr build(int elementStart) {
		return new GreenAttr(
				nameStart - elementStart,
				nameEnd - elementStart,
				delimiter != GreenAttr.NULL_VALUE ? delimiter - elementStart : GreenAttr.NULL_VALUE,
				valueStart != GreenAttr.NULL_VALUE ? valueStart - elementStart : GreenAttr.NULL_VALUE,
				valueEnd != GreenAttr.NULL_VALUE ? valueEnd - elementStart : GreenAttr.NULL_VALUE);
	}
}
