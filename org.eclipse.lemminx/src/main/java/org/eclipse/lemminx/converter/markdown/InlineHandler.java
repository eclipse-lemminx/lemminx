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
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Handles symmetric inline formatting tags by wrapping content with Markdown
 * markers on both open and close.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <b>text</b>} or {@code <strong>text</strong>} → {@code **text**}</li>
 *   <li>{@code <i>text</i>} or {@code <em>text</em>} → {@code *text*}</li>
 *   <li>{@code <del>text</del>} or {@code <s>text</s>} → {@code ~~text~~}</li>
 *   <li>{@code <code>text</code>} or {@code <tt>text</tt>} → {@code `text`}</li>
 * </ul>
 */
public class InlineHandler implements TagHandler {

	/** Handler for {@code <b>} and {@code <strong>}: wraps with {@code **}. */
	public static final InlineHandler BOLD = new InlineHandler("**");

	/** Handler for {@code <i>} and {@code <em>}: wraps with {@code *}. */
	public static final InlineHandler ITALIC = new InlineHandler("*");

	/** Handler for {@code <del>} and {@code <s>}: wraps with {@code ~~}. */
	public static final InlineHandler STRIKETHROUGH = new InlineHandler("~~");

	/** Handler for {@code <code>} and {@code <tt>}: wraps with {@code `}. */
	public static final InlineHandler CODE = new InlineHandler("`");

	private final String marker;

	private InlineHandler(String marker) {
		this.marker = marker;
	}

	/**
	 * Emits the opening marker.
	 *
	 * <p>Use case: {@code <b>} → emits {@code "**"} before the content.</p>
	 */
	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		sb.append(marker);
	}

	/**
	 * Emits the closing marker (same as the opening marker).
	 *
	 * <p>Use case: {@code </b>} → emits {@code "**"} after the content.</p>
	 */
	@Override
	public void close(StringBuilder sb) {
		sb.append(marker);
	}
}
