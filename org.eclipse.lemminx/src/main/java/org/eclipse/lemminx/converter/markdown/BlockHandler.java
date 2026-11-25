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

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.HtmlConverter;
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Handlers for block-level HTML elements that map to Markdown block constructs.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <p>text</p>} → blank lines around {@code text} (paragraph separation)</li>
 *   <li>{@code <blockquote>text</blockquote>} → {@code > text}</li>
 *   <li>{@code <hr>} → {@code ---}</li>
 *   <li>{@code <dl><dt>term</dt><dd>desc</dd></dl>} → {@code **term**\ndesc}</li>
 * </ul>
 */
public final class BlockHandler {

	private BlockHandler() {
	}

	/** {@code <p>text</p>} → blank line, text, blank line. */
	public static final TagHandler PARAGRAPH = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureBlankLine(sb);
		}

		@Override
		public void close(StringBuilder sb) {
			HtmlConverter.ensureBlankLine(sb);
		}
	};

	/** {@code <blockquote>text</blockquote>} → {@code "> text"}. */
	public static final TagHandler BLOCKQUOTE = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			// Trim trailing spaces and start on new line with "> " prefix.
			// Ex: "paragraph\n> quoted text"
			HtmlConverter.trimTrailingSpaces(sb);
			HtmlConverter.ensureNewline(sb);
			sb.append("> ");
		}

		@Override
		public void close(StringBuilder sb) {
			// Blank line after blockquote to separate from following content.
			// Ex: "> quoted text\n\nnext paragraph"
			HtmlConverter.ensureBlankLine(sb);
		}
	};

	/** {@code <hr>} or {@code <hr/>} → {@code "---"} on its own line. */
	public static final TagHandler HORIZONTAL_RULE = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureBlankLine(sb);
			sb.append("---").append(HtmlConverter.LINE_SEP);
		}
	};

	/** {@code <dl>…</dl>} → blank lines around the definition list. */
	public static final TagHandler DEFINITION_LIST = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureBlankLine(sb);
		}

		@Override
		public void close(StringBuilder sb) {
			HtmlConverter.ensureBlankLine(sb);
		}
	};

	/** {@code <dt>term</dt>} → {@code "**term**"} (bold term). */
	public static final TagHandler DEFINITION_TERM = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureNewline(sb);
			sb.append("**");
		}

		@Override
		public void close(StringBuilder sb) {
			sb.append("**");
		}
	};

	/** {@code <dd>description</dd>} → description on a new line. */
	public static final TagHandler DEFINITION_DESC = new TagHandler() {
		@Override
		public void open(HtmlAttributes attrs, StringBuilder sb) {
			HtmlConverter.ensureNewline(sb);
		}
	};
}
