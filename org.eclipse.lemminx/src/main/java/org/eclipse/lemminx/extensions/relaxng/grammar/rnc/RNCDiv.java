/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
 * Represents a {@code div} block in an RNC document.
 *
 * <p>A div block groups grammar definitions without affecting the grammar's
 * semantics. It is useful for organizing large grammars into logical sections.</p>
 *
 * <p>Use case: the parser creates an {@code RNCDiv} node when it encounters
 * the {@code div} keyword followed by braces. Defines inside the div are
 * collected recursively by {@link RNCDocument#getDefines()}.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * div {                        ← RNCDiv
 *   inline = element span { text }
 *   block = element div { text }
 * }
 * </pre>
 */
public class RNCDiv extends RNCNode {

	/** Offset of the opening brace {@code \{} ({@code -1} if missing). */
	private int bodyOpenOffset = -1;

	/** Offset of the closing brace {@code \}} ({@code -1} if missing/unclosed). */
	private int bodyCloseOffset = -1;

	/**
	 * Creates a new div node spanning the given offsets.
	 *
	 * @param start the inclusive start offset (the {@code div} keyword).
	 * @param end   the exclusive end offset.
	 */
	public RNCDiv(int start, int end) {
		super(start, end);
	}

	/**
	 * Returns the offset of the opening brace {@code \{}.
	 *
	 * @return the brace offset, or {@code -1} if no opening brace was found.
	 */
	public int getBodyOpenOffset() {
		return bodyOpenOffset;
	}

	/**
	 * Sets the offset of the opening brace.
	 *
	 * @param offset the offset of {@code \{} in the source text.
	 */
	public void setBodyOpenOffset(int offset) {
		this.bodyOpenOffset = offset;
	}

	/**
	 * Returns the offset of the closing brace {@code \}}.
	 *
	 * @return the brace offset, or {@code -1} if the block is unclosed.
	 */
	public int getBodyCloseOffset() {
		return bodyCloseOffset;
	}

	/**
	 * Sets the offset of the closing brace.
	 *
	 * @param offset the offset of {@code \}} in the source text.
	 */
	public void setBodyCloseOffset(int offset) {
		this.bodyCloseOffset = offset;
	}
}
