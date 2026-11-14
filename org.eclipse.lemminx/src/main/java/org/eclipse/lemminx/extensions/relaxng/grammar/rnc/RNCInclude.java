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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;

/**
 * Represents an {@code include} declaration in an RNC document.
 *
 * <p>An include declaration imports another RNC grammar file. Optionally, it can
 * have a body with braces to override definitions from the included grammar.</p>
 *
 * <p>Example RNC — simple include:</p>
 * <pre>
 * include "base.rnc"
 * </pre>
 *
 * <p>Example RNC — include with overrides:</p>
 * <pre>
 * include "base.rnc" {
 *   start = element doc { text }
 * }
 * </pre>
 */
public class RNCInclude extends RNCFileReferenceNode {

	/** Offset of the opening brace {@code \{} ({@code -1} if no override body). */
	private int bodyOpenOffset = -1;

	/** Offset of the closing brace {@code \}} ({@code -1} if no override body or unclosed). */
	private int bodyCloseOffset = -1;

	/**
	 * Creates a new include node spanning the given offsets.
	 *
	 * @param start the inclusive start offset (the {@code include} keyword).
	 * @param end   the exclusive end offset.
	 */
	public RNCInclude(int start, int end) {
		super(start, end);
	}

	/**
	 * Returns the offset of the opening brace {@code \{} of the override body.
	 *
	 * @return the brace offset, or {@code -1} if no override body.
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
	 * Returns the offset of the closing brace {@code \}} of the override body.
	 *
	 * @return the brace offset, or {@code -1} if no override body or unclosed.
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

	/**
	 * Returns whether this include has an override body (braces with definitions).
	 *
	 * @return {@code true} if the include has a {@code \{ ... \}} body.
	 */
	public boolean hasBody() {
		return bodyOpenOffset != -1;
	}

	/**
	 * Returns the set of define names (and "start") that are overridden
	 * inside this include's body.
	 */
	public Set<String> getOverriddenNames() {
		Set<String> names = new HashSet<>();
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCDefine) {
				String name = ((RNCDefine) child).getName();
				if (name != null) {
					names.add(name);
				}
			} else if (child instanceof RNCStart) {
				names.add(RNCKeywords.START);
			}
		}
		return names;
	}
}
