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
 * Immutable green node for an XML attribute.
 *
 * <p>Offsets are relative to the <em>owning element's</em> start position.
 * The red wrapper adds the element's absolute offset when converting.</p>
 */
public final class GreenAttr {

	public static final int NULL_VALUE = -1;

	private final int nameStartRel;
	private final int nameEndRel;
	private final int delimiterRel;
	private final int valueStartRel;
	private final int valueEndRel;

	public GreenAttr(int nameStartRel, int nameEndRel, int delimiterRel,
			int valueStartRel, int valueEndRel) {
		this.nameStartRel = nameStartRel;
		this.nameEndRel = nameEndRel;
		this.delimiterRel = delimiterRel;
		this.valueStartRel = valueStartRel;
		this.valueEndRel = valueEndRel;
	}

	public int nameStartRel() {
		return nameStartRel;
	}

	public int nameEndRel() {
		return nameEndRel;
	}

	public int delimiterRel() {
		return delimiterRel;
	}

	public int valueStartRel() {
		return valueStartRel;
	}

	public int valueEndRel() {
		return valueEndRel;
	}

	public int width() {
		if (valueEndRel != NULL_VALUE) {
			return valueEndRel - nameStartRel;
		}
		if (delimiterRel != NULL_VALUE) {
			return delimiterRel + 1 - nameStartRel;
		}
		return nameEndRel - nameStartRel;
	}
}
