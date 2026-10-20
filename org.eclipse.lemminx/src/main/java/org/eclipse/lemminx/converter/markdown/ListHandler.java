/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.converter.markdown;

import java.util.ArrayDeque;
import java.util.Deque;

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.HtmlConverter;
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Handles list tags ({@code <ul>}, {@code <ol>}, {@code <li>}) with shared
 * stack state for tracking nesting depth and ordered-list counters.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <ul><li>A</li><li>B</li></ul>} → {@code " *  A\n *  B"}</li>
 *   <li>{@code <ol><li>A</li><li>B</li></ol>} → {@code "1. A\n2. B"}</li>
 *   <li>Nested lists indent by 4 spaces per level</li>
 * </ul>
 */
public class ListHandler {

	// Stack of [isOrdered, itemCounter] pairs. Pushed on <ul>/<ol>, popped on close.
	// Ex: <ul><li>A<ol><li>B</li></ol></li></ul> → stack depth 2
	private final Deque<int[]> listStack = new ArrayDeque<>();

	private static final String INDENT = "    ";

	public boolean isInsideList() {
		return !listStack.isEmpty();
	}

	/**
	 * Handler for {@code <ul>}: pushes an unordered-list frame onto the stack.
	 *
	 * <p>Use case: {@code <ul><li>A</li></ul>} → {@code " *  A"}</p>
	 */
	public final TagHandler unorderedList = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			listStack.push(new int[] { 0, 0 });
		}

		@Override
		public void close(StringBuilder sb) {
			if (!listStack.isEmpty()) {
				listStack.pop();
			}
			if (listStack.isEmpty()) {
				HtmlConverter.ensureBlankLine(sb);
			}
		}
	};

	/**
	 * Handler for {@code <ol>}: pushes an ordered-list frame onto the stack.
	 *
	 * <p>Use case: {@code <ol><li>A</li><li>B</li></ol>} → {@code "1. A\n2. B"}</p>
	 */
	public final TagHandler orderedList = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			listStack.push(new int[] { 1, 0 });
		}

		@Override
		public void close(StringBuilder sb) {
			if (!listStack.isEmpty()) {
				listStack.pop();
			}
			if (listStack.isEmpty()) {
				HtmlConverter.ensureBlankLine(sb);
			}
		}
	};

	/**
	 * Handler for {@code <li>}: emits a bullet or number depending on the
	 * enclosing list type, with indentation based on nesting depth.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code <li>} inside {@code <ul>} → {@code " *  item"}</li>
	 *   <li>{@code <li>} inside {@code <ol>} → {@code "1. item"}</li>
	 *   <li>Nested {@code <li>} inside {@code <ul><ul>} → {@code "     *  item"} (indented)</li>
	 * </ul>
	 */
	public final TagHandler listItem = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureNewline(sb);
			int depth = listStack.size() - 1;
			for (int d = 0; d < depth; d++) {
				sb.append(INDENT);
			}
			int[] top = listStack.peek();
			boolean inOrderedList = top != null && top[0] == 1;
			if (inOrderedList) {
				sb.append(top[1] + 1).append(". ");
				top[1]++;
			} else {
				sb.append(" *  ");
			}
		}

		@Override
		public void close(StringBuilder sb) {
			HtmlConverter.ensureNewline(sb);
		}
	};
}
