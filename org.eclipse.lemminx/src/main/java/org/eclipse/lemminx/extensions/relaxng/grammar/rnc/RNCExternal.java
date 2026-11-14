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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

/**
 * Represents an {@code external} reference in an RNC document.
 *
 * <p>An external reference uses another RNC grammar's start pattern as
 * a pattern expression. Unlike {@code include}, it has no override body.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * start = external "other.rnc"
 * </pre>
 */
public class RNCExternal extends RNCFileReferenceNode {

	/**
	 * Creates a new external reference node spanning the given offsets.
	 *
	 * @param start the inclusive start offset (the {@code external} keyword).
	 * @param end   the exclusive end offset.
	 */
	public RNCExternal(int start, int end) {
		super(start, end);
	}
}
