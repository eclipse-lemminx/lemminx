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

import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Incremental parser that reparses only the affected region of a document
 * when an edit is applied, reusing unchanged subtrees via structural sharing.
 *
 * <p>Given an old {@link GreenDocument}, the new text, and edit coordinates,
 * this class identifies children that are entirely before or after the edit
 * (prefix/suffix), and reparses only the affected middle range using
 * {@link GreenTreeBuilder#parseRange}.</p>
 *
 * <p>When exactly one child contains the edit and that child is a
 * {@link GreenElement} with sub-children, the parser recursively descends
 * into that element to find sharing at deeper levels. This is critical for
 * documents with a single root element wrapping many children (the common
 * XML pattern).</p>
 *
 * <p>Falls back to full reparse when no structural sharing is possible or
 * when the reparsed middle ends with an unclosed element.</p>
 */
public final class IncrementalParser {

	private IncrementalParser() {
	}

	/**
	 * Incrementally reparses the document. Returns a new {@link GreenDocument}
	 * that structurally shares unchanged subtrees with the old tree.
	 *
	 * @param oldDoc        the previous immutable green tree
	 * @param newText       the full new document text (after edit applied)
	 * @param editStart     offset in old text where the edit begins
	 * @param deleteLength  number of characters deleted from old text
	 * @param insertLength  number of characters inserted (length of new text at edit point)
	 * @param uri           the document URI
	 * @param monitor       optional cancel checker
	 * @return a new GreenDocument (never null)
	 */
	public static GreenDocument incrementalParse(GreenDocument oldDoc,
			CharSequence newText, int editStart, int deleteLength, int insertLength,
			String uri, CancelChecker monitor) {
		int delta = insertLength - deleteLength;
		int editEnd = editStart + deleteLength;

		GreenNode[] result = tryIncrementalOnChildren(
				oldDoc.children(), 0, childrenWidth(oldDoc.children()),
				editStart, editEnd, delta, newText, uri, monitor);

		if (result != null) {
			return new GreenDocument(newText.length(), result);
		}
		return GreenTreeBuilder.parse(newText, uri, monitor);
	}

	private static GreenNode[] tryIncrementalOnChildren(
			GreenNode[] oldChildren, int childrenAbsStart, int childrenAreaWidth,
			int editStart, int editEnd, int delta,
			CharSequence newText, String uri, CancelChecker monitor) {

		if (oldChildren.length == 0) {
			return null;
		}

		// Find prefix: children entirely before the edit
		int prefixCount = 0;
		int prefixWidth = 0;
		for (int i = 0; i < oldChildren.length; i++) {
			int childEnd = childrenAbsStart + prefixWidth + oldChildren[i].width();
			if (childEnd <= editStart) {
				prefixCount++;
				prefixWidth += oldChildren[i].width();
			} else {
				break;
			}
		}

		// Find suffix: children entirely after the edit in old text
		int suffixCount = 0;
		int suffixWidth = 0;
		int scanPos = childrenAbsStart + childrenAreaWidth;
		for (int i = oldChildren.length - 1; i >= prefixCount; i--) {
			int childStart = scanPos - oldChildren[i].width();
			if (childStart >= editEnd) {
				suffixCount++;
				suffixWidth += oldChildren[i].width();
				scanPos = childStart;
			} else {
				break;
			}
		}

		// Try descent if exactly one child in the middle
		int middleCount = oldChildren.length - prefixCount - suffixCount;
		if (middleCount == 1) {
			GreenNode middleChild = oldChildren[prefixCount];
			if (middleChild instanceof GreenText) {
				int insertEnd = editEnd + delta;
				if (!containsLessThan(newText, editStart, insertEnd)) {
					GreenNode newText2 = new GreenText(middleChild.width() + delta, false);
					return splice(oldChildren, prefixCount, suffixCount,
							new GreenNode[] { newText2 });
				}
			} else if (middleChild instanceof GreenElement && middleChild.childCount() > 0) {
				GreenElement elem = (GreenElement) middleChild;
				int elemAbsStart = childrenAbsStart + prefixWidth;
				GreenElement newElem = tryDescentIntoElement(
						elem, elemAbsStart, editStart, editEnd, delta,
						newText, uri, monitor);
				if (newElem != null) {
					return splice(oldChildren, prefixCount, suffixCount,
							new GreenNode[] { newElem });
				}
			}
		}

		// Need at least some sharing to justify partial reparse
		if (prefixCount + suffixCount == 0) {
			return null;
		}

		// Parse the middle range in new text
		int middleStart = childrenAbsStart + prefixWidth;
		int middleEnd = childrenAbsStart + childrenAreaWidth + delta - suffixWidth;

		if (middleEnd < middleStart || middleEnd > newText.length()) {
			return null;
		}

		GreenDocument middleDoc = GreenTreeBuilder.parseRange(
				newText, uri, middleStart, middleEnd, monitor);
		GreenNode[] middleChildren = middleDoc.children();

		// If last reparsed child is unclosed, invalidate suffix
		if (suffixCount > 0 && middleChildren.length > 0
				&& !middleChildren[middleChildren.length - 1].closed()) {
			int extendedEnd = childrenAbsStart + childrenAreaWidth + delta;
			if (extendedEnd > newText.length()) {
				return null;
			}
			middleDoc = GreenTreeBuilder.parseRange(
					newText, uri, middleStart, extendedEnd, monitor);
			middleChildren = middleDoc.children();
			suffixCount = 0;
		}

		return splice(oldChildren, prefixCount, suffixCount, middleChildren);
	}

	private static GreenElement tryDescentIntoElement(
			GreenElement elem, int elemAbsStart,
			int editStart, int editEnd, int delta,
			CharSequence newText, String uri, CancelChecker monitor) {

		int csr = elem.childrenStartRel();
		int childrenAbsStart = elemAbsStart + csr;
		int childrenAreaWidth = childrenWidth(elem.children());

		// Only descend if edit is entirely within children area
		if (editStart < childrenAbsStart
				|| editEnd > childrenAbsStart + childrenAreaWidth) {
			return null;
		}

		GreenNode[] newChildren = tryIncrementalOnChildren(
				elem.children(), childrenAbsStart, childrenAreaWidth,
				editStart, editEnd, delta, newText, uri, monitor);

		if (newChildren == null) {
			return null;
		}

		return elem.withNewChildren(newChildren, delta);
	}

	private static GreenNode[] splice(GreenNode[] oldChildren,
			int prefixCount, int suffixCount, GreenNode[] middle) {
		int total = prefixCount + middle.length + suffixCount;
		GreenNode[] result = new GreenNode[total];
		System.arraycopy(oldChildren, 0, result, 0, prefixCount);
		System.arraycopy(middle, 0, result, prefixCount, middle.length);
		if (suffixCount > 0) {
			System.arraycopy(oldChildren, oldChildren.length - suffixCount,
					result, prefixCount + middle.length, suffixCount);
		}
		return coalesceAdjacentText(result);
	}

	private static GreenNode[] coalesceAdjacentText(GreenNode[] nodes) {
		boolean needed = false;
		for (int i = 0; i < nodes.length - 1; i++) {
			if (nodes[i] instanceof GreenText && nodes[i + 1] instanceof GreenText) {
				needed = true;
				break;
			}
		}
		if (!needed) {
			return nodes;
		}
		int newLen = 0;
		for (int i = 0; i < nodes.length;) {
			newLen++;
			if (nodes[i] instanceof GreenText) {
				while (++i < nodes.length && nodes[i] instanceof GreenText) {
				}
			} else {
				i++;
			}
		}
		GreenNode[] result = new GreenNode[newLen];
		int j = 0;
		for (int i = 0; i < nodes.length;) {
			if (nodes[i] instanceof GreenText) {
				int w = 0;
				boolean allWhitespace = true;
				int start = i;
				while (i < nodes.length && nodes[i] instanceof GreenText) {
					w += nodes[i].width();
					if (!((GreenText) nodes[i]).whitespace()) {
						allWhitespace = false;
					}
					i++;
				}
				result[j++] = (i - start == 1) ? nodes[start]
						: allWhitespace ? GreenText.whitespace(w) : new GreenText(w, false);
			} else {
				result[j++] = nodes[i++];
			}
		}
		return result;
	}

	private static boolean containsLessThan(CharSequence text, int from, int to) {
		for (int i = from; i < to; i++) {
			if (text.charAt(i) == '<') {
				return true;
			}
		}
		return false;
	}

	private static int childrenWidth(GreenNode[] children) {
		int w = 0;
		for (GreenNode child : children) {
			w += child.width();
		}
		return w;
	}
}
