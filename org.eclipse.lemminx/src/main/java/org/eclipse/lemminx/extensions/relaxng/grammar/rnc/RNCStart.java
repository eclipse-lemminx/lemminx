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
 * Represents a {@code start} declaration in an RNC document.
 *
 * <p>The start declaration defines the entry point of the grammar — the pattern
 * that the root element of a valid XML document must match.</p>
 *
 * <p>Use case: the parser creates an {@code RNCStart} node when it encounters the
 * {@code start} keyword followed by an assignment operator. The validator checks
 * that every grammar has exactly one start declaration.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * start = root           ← RNCStart(assignType=ASSIGN), references define "root"
 * start |= alt-root      ← RNCStart(assignType=CHOICE), extends start with alternative
 * </pre>
 */
import java.util.List;

public class RNCStart extends RNCNode {

	/** The assignment operator type ({@code =}, {@code |=}, or {@code &=}). */
	private RNCAssignType assignType = RNCAssignType.ASSIGN;

	/**
	 * Creates a new start node spanning the given offsets.
	 *
	 * @param start the inclusive start offset (the {@code start} keyword).
	 * @param end   the exclusive end offset.
	 */
	public RNCStart(int start, int end) {
		super(start, end);
	}

	/**
	 * Returns the assignment operator type used in this start declaration.
	 *
	 * @return the {@link RNCAssignType}.
	 */
	public RNCAssignType getAssignType() {
		return assignType;
	}

	/**
	 * Sets the assignment operator type for this start declaration.
	 *
	 * @param assignType the assignment type parsed from the source.
	 */
	public void setAssignType(RNCAssignType assignType) {
		this.assignType = assignType;
	}

	@Override
	public String getText() {
		RNCDocument doc = getOwnerDocument();
		if (doc == null) {
			return null;
		}
		RNCNode parent = getParent();
		if (parent == null || !(parent instanceof RNCDocument || parent instanceof RNCDiv)) {
			return doc.getText(getStart(), getEnd());
		}
		List<RNCNode> siblings = parent.getChildren();
		int myIndex = siblings.indexOf(this);
		CharSequence source = doc.getSource();
		int textEnd = parent instanceof RNCDiv ? parent.getEnd() : source.length();
		for (int i = myIndex + 1; i < siblings.size(); i++) {
			RNCNode sibling = siblings.get(i);
			if (sibling instanceof RNCStart || sibling instanceof RNCDefine
					|| sibling instanceof RNCNamespaceDecl || sibling instanceof RNCDatatypesDecl
					|| sibling instanceof RNCInclude || sibling instanceof RNCDiv) {
				textEnd = sibling.getStart();
				break;
			}
		}
		while (textEnd > getStart() && Character.isWhitespace(source.charAt(textEnd - 1))) {
			textEnd--;
		}
		return doc.getText(getStart(), textEnd);
	}
}
