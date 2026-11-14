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
 * Represents an {@code element} pattern in an RNC document.
 *
 * <p>An element pattern declares an XML element with a name class and a
 * content pattern enclosed in braces.</p>
 *
 * <p>Use case: the parser creates an {@code RNCElement} node when it encounters
 * the {@code element} keyword. This node is typically a child of an
 * {@link RNCDefine} and tracks the element name and brace offsets for
 * diagnostics (unclosed blocks, missing names) and folding.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * root = element root {      ← RNCElement(name="root")
 *   attribute id { text },
 *   content
 * }
 * </pre>
 */
public class RNCElement extends RNCNode {

	/** Inclusive start offset of the element name in the source text. */
	private int nameStart = -1;

	/** Exclusive end offset of the element name in the source text. */
	private int nameEnd = -1;

	/** Offset of the opening brace {@code \{} ({@code -1} if missing). */
	private int bodyOpenOffset = -1;

	/** Offset of the closing brace {@code \}} ({@code -1} if missing/unclosed). */
	private int bodyCloseOffset = -1;

	/**
	 * Creates a new element node spanning the given offsets.
	 *
	 * @param start the inclusive start offset (the {@code element} keyword).
	 * @param end   the exclusive end offset (adjusted when parsing completes).
	 */
	public RNCElement(int start, int end) {
		super(start, end);
	}

	/**
	 * Sets the source offsets of the element name.
	 *
	 * @param start the inclusive start offset of the name.
	 * @param end   the exclusive end offset of the name.
	 */
	public void setName(int start, int end) {
		this.nameStart = start;
		this.nameEnd = end;
	}

	/**
	 * Returns the element name extracted from the source text.
	 *
	 * <p>Example: for {@code element root { text }}, returns {@code "root"}.</p>
	 *
	 * @return the name string, or {@code null} if not set.
	 */
	public String getName() {
		if (nameStart == -1) {
			return null;
		}
		RNCDocument doc = getOwnerDocument();
		return doc != null ? doc.getText(nameStart, nameEnd) : null;
	}

	/**
	 * Returns the inclusive start offset of the element name.
	 *
	 * @return the name start offset, or {@code -1} if not set.
	 */
	public int getNameStart() {
		return nameStart;
	}

	/**
	 * Returns the exclusive end offset of the element name.
	 *
	 * @return the name end offset, or {@code -1} if not set.
	 */
	public int getNameEnd() {
		return nameEnd;
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
