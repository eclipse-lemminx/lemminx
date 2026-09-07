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
 * Immutable representation of a DTD declaration parameter's position.
 *
 * <p>Offsets are relative to the owning DTD declaration node's start.</p>
 */
public final class GreenDTDParam {

	private final int startRel;
	private final int endRel;

	public GreenDTDParam(int startRel, int endRel) {
		this.startRel = startRel;
		this.endRel = endRel;
	}

	public int startRel() {
		return startRel;
	}

	public int endRel() {
		return endRel;
	}

	public int width() {
		return endRel - startRel;
	}
}
