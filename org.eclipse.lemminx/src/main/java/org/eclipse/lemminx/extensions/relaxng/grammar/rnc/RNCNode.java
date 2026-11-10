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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCRange;

/**
 * Base class for all nodes in the RNC (RELAX NG Compact Syntax) abstract syntax tree.
 *
 * <p>Each node represents a span of text in the source document, defined by a
 * start offset (inclusive) and an end offset (exclusive). Nodes form a tree via
 * parent-child relationships: the root is always an {@link RNCDocument}, and
 * children are specific constructs like {@link RNCElement}, {@link RNCAttribute},
 * {@link RNCDefine}, etc.</p>
 *
 * <p>Use case: the {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser} builds an AST of {@code RNCNode} subclasses
 * from a {@code .rnc} file. This tree is then used by language features (hover,
 * definition, code lens) to locate defines, elements, and other constructs.</p>
 *
 * <p>Example AST for {@code start = element root \{ text \}}:</p>
 * <pre>
 * RNCDocument
 *   └─ RNCStart (start=0, end=5)
 *   └─ RNCElement (start=8, end=30, name="root")
 * </pre>
 */
public class RNCNode implements RNCRange {

	/** Inclusive start offset of this node in the source text. */
	private int start;

	/** Exclusive end offset of this node in the source text. */
	private int end;

	/** Parent node in the AST, or {@code null} for the root {@link RNCDocument}. */
	private RNCNode parent;

	/** Child nodes, lazily initialized on first {@link #addChild(RNCNode)} call. */
	private List<RNCNode> children;

	/** Whether this node has been properly closed (e.g. matching {@code \}} found). */
	private boolean closed;

	/**
	 * Creates a new node spanning the given offsets.
	 *
	 * @param start the inclusive start offset.
	 * @param end   the exclusive end offset (may be adjusted later as more tokens are parsed).
	 */
	public RNCNode(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * Returns the inclusive start offset of this node in the source text.
	 *
	 * @return the start offset.
	 */
	@Override
	public int getStart() {
		return start;
	}

	/**
	 * Sets the inclusive start offset of this node.
	 *
	 * @param start the new start offset.
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * Returns the exclusive end offset of this node in the source text.
	 *
	 * @return the end offset.
	 */
	@Override
	public int getEnd() {
		return end;
	}

	/**
	 * Sets the exclusive end offset of this node. Called by the parser when the
	 * node's extent is fully determined (e.g. when the closing brace is found).
	 *
	 * @param end the new end offset.
	 */
	public void setEnd(int end) {
		this.end = end;
	}

	/**
	 * Returns the parent node, or {@code null} for the root {@link RNCDocument}.
	 *
	 * @return the parent node.
	 */
	public RNCNode getParent() {
		return parent;
	}

	/**
	 * Adds a child node and sets its parent to this node.
	 *
	 * @param child the child node to add.
	 */
	public void addChild(RNCNode child) {
		if (children == null) {
			children = new ArrayList<>();
		}
		child.parent = this;
		children.add(child);
	}

	/**
	 * Returns the list of child nodes (never {@code null}, returns empty list if no children).
	 *
	 * @return the children.
	 */
	public List<RNCNode> getChildren() {
		if (children == null) {
			return Collections.emptyList();
		}
		return children;
	}

	/**
	 * Returns whether this node has any child nodes.
	 *
	 * @return {@code true} if this node has children.
	 */
	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	/**
	 * Returns whether this node has been properly closed (e.g. the parser found
	 * a matching closing brace {@code \}}).
	 *
	 * <p>Use case: used by diagnostics to detect unclosed blocks like
	 * {@code element root \{} with no matching {@code \}}.</p>
	 *
	 * @return {@code true} if the node is closed.
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Sets whether this node is closed.
	 *
	 * @param closed {@code true} if a matching close token was found.
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	/**
	 * Walks up the parent chain to find the root {@link RNCDocument}.
	 *
	 * <p>Use case: model classes like {@link RNCDefine} use this to access
	 * the source text for extracting their name or body content.</p>
	 *
	 * @return the owning {@link RNCDocument}, or {@code null} if not attached.
	 */
	@Override
	public RNCDocument getOwnerDocument() {
		RNCNode node = this;
		while (node != null) {
			if (node instanceof RNCDocument) {
				return (RNCDocument) node;
			}
			node = node.parent;
		}
		return null;
	}

	/**
	 * Returns the source text spanned by this node.
	 *
	 * @return the text from {@link #getStart()} to {@link #getEnd()}, or {@code null}
	 *         if this node is not attached to a document.
	 */
	public String getText() {
		RNCDocument doc = getOwnerDocument();
		if (doc == null) {
			return null;
		}
		return doc.getText(start, end);
	}

	/**
	 * Finds the deepest node in the tree that contains the given offset.
	 *
	 * <p>Use case: used to determine which AST construct the cursor is inside,
	 * for context-sensitive features like completion.</p>
	 *
	 * @param offset the character offset to look up.
	 * @return the deepest node containing the offset (may be {@code this}).
	 */
	public RNCNode findNodeAt(int offset) {
		if (children != null) {
			for (RNCNode child : children) {
				if (child.start <= offset && offset < child.end) {
					return child.findNodeAt(offset);
				}
			}
		}
		return this;
	}
}
