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
 * Represents a named pattern definition in an RNC document.
 *
 * <p>A define binds a name to a pattern using one of the assignment operators
 * ({@code =}, {@code |=}, {@code &=}). The name can then be referenced elsewhere
 * in the grammar.</p>
 *
 * <p>Use case: used by multiple language features:</p>
 * <ul>
 *   <li><b>Completion</b> — suggests define names as references</li>
 *   <li><b>Hover</b> — shows the full define text on hover</li>
 *   <li><b>Definition</b> — navigates from a reference to this declaration</li>
 *   <li><b>Code lens</b> — counts references to this define</li>
 *   <li><b>Highlighting</b> — highlights the declaration (Write) and references (Read)</li>
 * </ul>
 *
 * <p>Example RNC:</p>
 * <pre>
 * root = element root {    ← RNCDefine(name="root", assignType=ASSIGN)
 *   attribute id { text },
 *   content
 * }
 * content |= text          ← RNCDefine(name="content", assignType=CHOICE)
 * </pre>
 */
import java.util.List;

public class RNCDefine extends RNCNode {

	/** Inclusive start offset of the define name in the source text ({@code -1} if not set). */
	private int nameStart = -1;

	/** Exclusive end offset of the define name in the source text ({@code -1} if not set). */
	private int nameEnd = -1;

	/** The assignment operator type ({@code =}, {@code |=}, or {@code &=}). */
	private RNCAssignType assignType = RNCAssignType.ASSIGN;

	/**
	 * Creates a new define node spanning the given offsets.
	 *
	 * @param start the inclusive start offset of the entire define declaration.
	 * @param end   the exclusive end offset (adjusted as the define body is parsed).
	 */
	public RNCDefine(int start, int end) {
		super(start, end);
	}

	/**
	 * Sets the source offsets of the define name.
	 *
	 * <p>Called by the parser when the name identifier token is encountered.</p>
	 *
	 * @param start the inclusive start offset of the name.
	 * @param end   the exclusive end offset of the name.
	 */
	public void setName(int start, int end) {
		this.nameStart = start;
		this.nameEnd = end;
	}

	/**
	 * Returns the define name extracted from the source text.
	 *
	 * <p>Example: for {@code root = element root { text }}, returns {@code "root"}.</p>
	 *
	 * @return the name string, or {@code null} if the name offsets have not been set.
	 */
	public String getName() {
		if (nameStart == -1) {
			return null;
		}
		RNCDocument doc = getOwnerDocument();
		return doc != null ? doc.getText(nameStart, nameEnd) : null;
	}

	/**
	 * Returns the inclusive start offset of the define name.
	 *
	 * @return the name start offset, or {@code -1} if not set.
	 */
	public int getNameStart() {
		return nameStart;
	}

	/**
	 * Returns the exclusive end offset of the define name.
	 *
	 * @return the name end offset, or {@code -1} if not set.
	 */
	public int getNameEnd() {
		return nameEnd;
	}

	/**
	 * Returns the assignment operator type used in this define.
	 *
	 * @return the {@link RNCAssignType} ({@code ASSIGN}, {@code CHOICE}, or {@code INTERLEAVE}).
	 */
	public RNCAssignType getAssignType() {
		return assignType;
	}

	/**
	 * Sets the assignment operator type for this define.
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
		if (parent == null
				|| !(parent instanceof RNCDocument || parent instanceof RNCDiv || parent instanceof RNCInclude)) {
			return doc.getText(getStart(), getEnd());
		}
		List<RNCNode> siblings = parent.getChildren();
		int myIndex = siblings.indexOf(this);
		CharSequence source = doc.getSource();
		int textEnd = parent instanceof RNCDocument ? source.length() : parent.getEnd();
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
