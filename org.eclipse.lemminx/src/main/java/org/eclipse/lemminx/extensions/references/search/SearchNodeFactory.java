/*******************************************************************************
* Copyright (c) 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.references.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.extensions.references.search.SearchNode.Direction;
import org.eclipse.lemminx.utils.StringUtils;

/**
 * Search node factory.
 * 
 * @author Angelo ZERR
 *
 */
public class SearchNodeFactory {

	private static final Predicate<Character> NAME_PREDICATE = ch -> {
		return !Character.isWhitespace(ch);
	};

	/**
	 * Returns all search node of the given DOM <code>node</code>.
	 * 
	 * <p>
	 * 
	 * <code>
	 * 	<link target="#abc #def" >
	 * </code>
	 * 
	 * will return the search nodes #abc and #def.
	 * 
	 * </p>
	 * 
	 * @param node      the DOM node.
	 * @param prefix    the prefix.
	 * @param multiple  true if multiple is supported.
	 * @param direction the search node direction.
	 * 
	 * @return all search node of the given DOM <code>node</code>.
	 */
	public static List<SearchNode> findSearchNodes(DOMNode node, String prefix, boolean multiple, Direction direction) {
		int startNode = getStartNode(node);
		if (startNode == -1) {
			return Collections.emptyList();
		}
		int endNode = getEndNode(node);
		if (endNode == -1) {
			return Collections.emptyList();
		}

		if (multiple) {
			CharSequence text = node.getOwnerDocument().getTextSequence();
			List<SearchNode> searchNodes = new ArrayList<>();
			int itemStart = -1;
			for (int j = startNode; j < endNode; j++) {
				char c = text.charAt(j);
				if (itemStart == -1) {
					if (!Character.isWhitespace(c)) {
						itemStart = j;
					}
				} else if (Character.isWhitespace(c)) {
					searchNodes.add(new SearchNode(itemStart, j, node, prefix, direction));
					itemStart = -1;
				}
			}
			if (itemStart != -1) {
				searchNodes.add(new SearchNode(itemStart, endNode, node, prefix, direction));
			}
			return searchNodes;

		}
		return Arrays.asList(new SearchNode(startNode, endNode, node, prefix, direction));
	}

	/**
	 * Returns the search node in the given DOM <code>node</code> at the given
	 * <code>offset</code> and null otherwise.
	 * 
	 * <p>
	 * 
	 * <code>
	 * 	<link target="#abc #de|f" >
	 * </code>
	 * 
	 * will return the search node #def.
	 * 
	 * </p>
	 * 
	 * @param node      the DOM node.
	 * @param offset    the offset.
	 * @param prefix    the prefix.
	 * @param multiple  true if multiple is supported.
	 * @param direction the search node direction.
	 * 
	 * @return the search node in the given DOM <code>node</code> at the given
	 *         <code>offset</code> and null otherwise.
	 */
	public static SearchNode getSearchNodeAt(DOMNode node, int offset, String prefix, boolean multiple,
			Direction direction) {
		int startNode = getStartNode(node);
		if (startNode == -1) {
			return null;
		}
		int endNode = getEndNode(node);
		if (endNode == -1) {
			return null;
		}
		if (multiple) {
			CharSequence text = node.getOwnerDocument().getTextSequence();
			if (offset != startNode) {
				int left = StringUtils.findStartWord(text, offset, startNode, NAME_PREDICATE);
				if (left != -1) {
					startNode = left;
				} else {
					left = StringUtils.findStartWord(text, offset - 1, startNode, NAME_PREDICATE);
					if (left != -1) {
						startNode = left;
					} else {
						startNode = offset;
					}
				}
			}
			if (offset != endNode - 1) {
				int right = StringUtils.findEndWord(text, offset, endNode, NAME_PREDICATE);
				if (right != -1) {
					endNode = right;
				} else {
					endNode = offset;
				}
			}
		}
		return new SearchNode(startNode, endNode, node, prefix, direction);
	}

	private static int getStartNode(DOMNode node) {
		DOMRange range = getDOMRange(node);
		if (range == null) {
			return -1;
		}
		return range.getStart() + (node.isAttribute() ? 1 : 0);
	}

	private static int getEndNode(DOMNode node) {
		DOMRange range = getDOMRange(node);
		if (range == null) {
			return -1;
		}
		return range.getEnd() - (node.isAttribute() ? 1 : 0);
	}

	private static DOMRange getDOMRange(DOMNode node) {
		if (node.isAttribute()) {
			DOMAttr attr = (DOMAttr) node;
			return attr.getNodeAttrValue();
		}
		return node;
	}

}
