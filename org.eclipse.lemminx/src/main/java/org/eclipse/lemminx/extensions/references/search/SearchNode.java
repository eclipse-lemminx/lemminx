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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMRange;
import org.eclipse.lemminx.extensions.references.settings.XMLReferenceExpression;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Search node wraps a DOM node which matches a from/to path of a given
 * {@link XMLReferenceExpression}.
 * 
 * <p>
 * If the expression doesn't define multiple
 * {@link XMLReferenceExpression#isMultiple()} the search node contains the same
 * ranges (start / end offsets) and the same value than the DOM node.
 * Ex :
 * <code>
 * 	<link ref="#A" />
 * </code>
 * 
 * In this sample the DOM node will generate one search node with the following
 * range:
 * 
 * <code>
 * 	<link ref="|#A|" />
 * </code>
 * 
 * </p>
 *
 * <p>
 * If the expression defines multiple
 * {@link XMLReferenceExpression#isMultiple()} the DOM node generates several
 * search node with proper ranges and value.
 * Ex :
 * <code>
 * 	<link ref="#A #B" />
 * </code>
 * 
 * In this sample the DOM node will generate two search nodes with the following
 * ranges:
 * 
 * <code>
 * 	<link ref="|#A| |#B|" />
 * </code>
 * 
 * </p>
 * 
 * @author Angelo ZERR
 *
 */
public class SearchNode implements DOMRange {

	public static enum Direction {
		FROM, TO;
	}

	public enum ValidationStatus {
		INVALID_PREFIX,
		EMPTY_VALUE,
		OK;
	}

	private final int start;

	private final int end;

	private final DOMNode node;

	private final String prefix;

	private final Direction direction;

	private ValidationStatus validationStatus;

	public SearchNode(int start, int end, DOMNode node, String prefix, Direction direction) {
		this.start = start;
		this.end = end;
		this.node = node;
		this.prefix = prefix;
		this.direction = direction;
	}

	public String getValue(String forcedPrefix) {
		StringBuilder value = new StringBuilder();
		if (forcedPrefix != null) {
			value.append(forcedPrefix);
		}
		CharSequence text = getOwnerDocument().getTextSequence();
		for (int i = getStart(); i < getEnd(); i++) {
			value.append(text.charAt(i));
		}
		return value.toString();
	}

	public String getPrefix() {
		return prefix;
	}

	/**
	 * Returns true if the given search node <code>searchNode</code> matches the
	 * value of this search node and false otherwise.
	 * 
	 * @param searchNode the search node.
	 * 
	 * @return true if the given search node <code>searchNode</code> matches the
	 *         value of this search node and false otherwise.
	 */
	public boolean matchesValue(SearchNode searchNode) {
		int fromStart = getStart();
		int fromEnd = getEnd();
		CharSequence fromText = getOwnerDocument().getTextSequence();
		if (direction == Direction.FROM) {
			int adjust = adjustWithPrefix(this);
			if (adjust == -1) {
				return false;
			}
			fromStart = fromStart + adjust;
		}
		int toStart = searchNode.getStart();
		int toEnd = searchNode.getEnd();
		CharSequence toText = searchNode.getOwnerDocument().getTextSequence();
		if (direction == Direction.TO) {
			int adjust = adjustWithPrefix(searchNode);
			if (adjust == -1) {
				return false;
			}
			toStart = toStart + adjust;
		}

		int length = fromEnd - fromStart;

		if (length != toEnd - toStart) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (fromText.charAt(i + fromStart) != toText.charAt(i + toStart)) {
				return false;
			}
		}
		return true;
	}

	private static int adjustWithPrefix(SearchNode node) {
		String prefix = node.getPrefix();
		if (prefix == null) {
			return 0;
		}
		if (!node.isValidPrefix()) {
			return -1;
		}
		return prefix.length();
	}

	public DOMNode getNode() {
		return node;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getEnd() {
		return end;
	}

	@Override
	public DOMDocument getOwnerDocument() {
		return node.getOwnerDocument();
	}

	public Direction getDirection() {
		return direction;
	}

	public Range createRange() {
		return createRange(false);
	}

	public Range createRange(boolean checkPrefix) {
		Range range = XMLPositionUtility.createRange(this);
		if (checkPrefix && isNeedToAjdustWithPrefix()) {
			// Adjust position start with prefix length.
			Position start = range.getStart();
			start.setCharacter(start.getCharacter() + getPrefix().length());
		}
		return range;

	}

	public boolean isNeedToAjdustWithPrefix() {
		return (getDirection() == Direction.FROM && getPrefix() != null);
	}

	/**
	 * Returns true if the search node is valid and false otherwise.
	 * 
	 * The node is valid when:
	 * 
	 * <ul>
	 * <li>the search node is a 'to' node.</li>
	 * <li>the 'from' search node have none prefix.</li>
	 * <li>the 'from' search node have the expected prefix.</li>
	 * </ul>
	 * 
	 * @return true if the search node has valid prefix and false otherwise
	 */
	public boolean isValid() {
		return getValidationStatus() == ValidationStatus.OK;
	}

	/**
	 * Returns the validation status of the search node.
	 * 
	 * @return the validation status of the search node.
	 */
	public ValidationStatus getValidationStatus() {
		if (validationStatus == null) {
			validationStatus = validate();
		}
		return validationStatus;
	}

	/**
	 * Validate the search node.
	 * 
	 * @return the validation status.
	 */
	private ValidationStatus validate() {
		if (!isValidPrefix()) {
			return ValidationStatus.INVALID_PREFIX;
		}
		// Validate empty value.
		if (start == (end - (prefix != null ? prefix.length() : 0))) {
			return ValidationStatus.EMPTY_VALUE;
		}
		return ValidationStatus.OK;
	}

	/**
	 * Returns true if prefix is valid and false otherwise.
	 * 
	 * @return true if prefix is valid and false otherwise.
	 */
	private boolean isValidPrefix() {
		if (direction == Direction.TO) {
			// the search node is a 'to' node.
			return true;
		}
		String prefix = getPrefix();
		if (prefix == null) {
			// the 'from' search node have none prefix.
			return true;
		}
		if (prefix.length() > (end - start)) {
			return false;
		}
		CharSequence text = node.getOwnerDocument().getTextSequence();
		for (int i = 0; i < prefix.length(); i++) {
			if (text.charAt(start + i) != prefix.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		CharSequence text = node.getOwnerDocument().getTextSequence();
		result.append(text.subSequence(start, end));
		result.append(direction == Direction.FROM ? " -->" : " <--");
		return result.toString();
	}
}