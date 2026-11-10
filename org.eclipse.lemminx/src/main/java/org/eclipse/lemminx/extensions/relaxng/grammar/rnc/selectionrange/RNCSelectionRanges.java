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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.selectionrange;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAttribute;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCElement;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNodeType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Computes {@code textDocument/selectionRange} responses for RNC documents.
 *
 * <p>For each cursor position, finds the deepest AST node and walks up the
 * parent chain, adding a {@link SelectionRange} for each node. The innermost
 * range is the word under the cursor, then intermediate sub-node ranges
 * (define name, element/attribute body content), then the full node range,
 * up to the document root.</p>
 *
 * <p>Special handling for defines and starts: the parser only assigns these
 * nodes a range that covers their {@code name =} portion. The actual pattern
 * body (element/attribute alternatives or simple patterns) is represented by
 * sibling nodes or is not in the AST at all. This class computes the
 * <em>logical</em> range of a declaration by scanning siblings, mirroring the
 * logic in {@link RNCDefine#getText()}.</p>
 */
public class RNCSelectionRanges {

	private static final Logger LOGGER = Logger.getLogger(RNCSelectionRanges.class.getName());

	public List<SelectionRange> getSelectionRanges(RNCDocument rncDocument, List<Position> positions,
			CancelChecker cancelChecker) {
		List<SelectionRange> result = new ArrayList<>();
		TextDocument textDocument = rncDocument.getTextDocument();
		for (Position position : positions) {
			try {
				int offset = textDocument.offsetAt(position);
				RNCNode node = rncDocument.findNodeAt(offset);
				result.add(buildChain(node, offset, textDocument));
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, "Unable to compute selection range for position " + position, e);
				result.add(null);
			}
			cancelChecker.checkCanceled();
		}
		return result;
	}

	private SelectionRange buildChain(RNCNode node, int offset, TextDocument textDocument) {
		SelectionRange leaf = new SelectionRange();
		leaf.setRange(new Range());
		SelectionRange current = leaf;

		Range wordRange = RNCPositionUtils.getWordRange(textDocument, offset, null);
		if (wordRange != null) {
			current = addRange(current, wordRange);
		}

		RNCNode curr = node;
		while (curr != null) {
			current = addNodeRanges(current, curr, offset, textDocument);

			// Top-level elements/attributes are AST siblings of their enclosing
			// define/start, not children of it. Inject the enclosing declaration's
			// logical range before ascending to the document.
			RNCNode parent = curr.getParent();
			if (parent instanceof RNCDocument && isTopLevelPattern(curr)) {
				RNCNode enclosing = findEnclosingDeclaration((RNCDocument) parent, curr);
				if (enclosing != null) {
					int logicalEnd = computeLogicalEnd(enclosing);
					current = addRange(current,
							RNCPositionUtils.createRange(textDocument, enclosing.getStart(), logicalEnd));
				}
			}

			curr = parent;
		}

		return leaf.getParent();
	}

	private SelectionRange addNodeRanges(SelectionRange current, RNCNode node, int offset,
			TextDocument textDocument) {
		switch (node.getNodeType()) {
			case Define: {
				RNCDefine define = (RNCDefine) node;
				int nameStart = define.getNameStart();
				int nameEnd = define.getNameEnd();
				if (nameStart != -1 && offset >= nameStart && offset <= nameEnd) {
					current = addRange(current, RNCPositionUtils.createRange(textDocument, nameStart, nameEnd));
				}
				break;
			}
			case Element: {
				RNCElement element = (RNCElement) node;
				current = addBodyContentRange(current, element.getBodyOpenOffset(), element.getBodyCloseOffset(),
						offset, textDocument);
				break;
			}
			case Attribute: {
				RNCAttribute attr = (RNCAttribute) node;
				current = addBodyContentRange(current, attr.getBodyOpenOffset(), attr.getBodyCloseOffset(),
						offset, textDocument);
				break;
			}
			default:
				break;
		}
		// Define and Start nodes' parser-assigned end only reaches after "=".
		// Compute the logical end (spanning the full pattern body) instead.
		int end = isDeclaration(node) ? computeLogicalEnd(node) : node.getEnd();
		return addRange(current, RNCPositionUtils.createRange(textDocument, node.getStart(), end));
	}

	private SelectionRange addBodyContentRange(SelectionRange current, int bodyOpen, int bodyClose,
			int offset, TextDocument textDocument) {
		if (bodyOpen != -1 && bodyClose != -1 && offset > bodyOpen && offset < bodyClose) {
			current = addRange(current, RNCPositionUtils.createRange(textDocument, bodyOpen + 1, bodyClose));
		}
		return current;
	}

	private SelectionRange addRange(SelectionRange current, Range range) {
		if (range == null || range.equals(current.getRange())) {
			return current;
		}
		SelectionRange parent = new SelectionRange();
		parent.setRange(range);
		current.setParent(parent);
		return parent;
	}

	private static boolean isDeclaration(RNCNode node) {
		RNCNodeType t = node.getNodeType();
		return t == RNCNodeType.Define || t == RNCNodeType.Start;
	}

	private static boolean isTopLevelPattern(RNCNode node) {
		RNCNodeType t = node.getNodeType();
		return t == RNCNodeType.Element || t == RNCNodeType.Attribute;
	}

	/**
	 * Finds the define or start declaration that logically owns {@code node},
	 * scanning backwards through {@code doc}'s children.
	 *
	 * <p>Element and attribute siblings between the declaration and {@code node}
	 * are skipped because they are part of the same declaration's body. Any other
	 * sibling type (namespace, datatypes, include, div) breaks the chain.</p>
	 */
	private static RNCNode findEnclosingDeclaration(RNCDocument doc, RNCNode node) {
		List<RNCNode> children = doc.getChildren();
		int idx = children.indexOf(node);
		if (idx < 0) {
			return null;
		}
		for (int i = idx - 1; i >= 0; i--) {
			RNCNode sibling = children.get(i);
			RNCNodeType t = sibling.getNodeType();
			if (t == RNCNodeType.Define || t == RNCNodeType.Start) {
				return sibling;
			}
			if (t == RNCNodeType.Element || t == RNCNodeType.Attribute) {
				continue;
			}
			break;
		}
		return null;
	}

	/**
	 * Computes the logical end offset of a define or start declaration.
	 *
	 * <p>The parser sets a declaration's end only to after its {@code =} token.
	 * The true logical end is the start of the next sibling declaration, or the
	 * end of the source if this is the last declaration. Element and attribute
	 * siblings that follow the declaration are part of its body and are skipped.
	 * Trailing whitespace is trimmed.</p>
	 *
	 * <p>This mirrors the logic in {@link RNCDefine#getText()}.</p>
	 */
	private static int computeLogicalEnd(RNCNode node) {
		RNCNode parent = node.getParent();
		if (!(parent instanceof RNCDocument)) {
			return node.getEnd();
		}
		RNCDocument doc = (RNCDocument) parent;
		CharSequence source = doc.getSource();
		List<RNCNode> siblings = doc.getChildren();
		int myIndex = siblings.indexOf(node);
		if (myIndex < 0) {
			return node.getEnd();
		}

		int logicalEnd = source.length();
		for (int i = myIndex + 1; i < siblings.size(); i++) {
			RNCNode sibling = siblings.get(i);
			RNCNodeType t = sibling.getNodeType();
			if (t == RNCNodeType.Element || t == RNCNodeType.Attribute) {
				continue;
			}
			logicalEnd = sibling.getStart();
			break;
		}

		while (logicalEnd > node.getStart() && logicalEnd > 0
				&& Character.isWhitespace(source.charAt(logicalEnd - 1))) {
			logicalEnd--;
		}
		return logicalEnd;
	}
}
