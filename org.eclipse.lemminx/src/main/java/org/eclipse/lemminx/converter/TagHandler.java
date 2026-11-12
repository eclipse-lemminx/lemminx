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
package org.eclipse.lemminx.converter;

/**
 * Handler for a specific HTML tag type. Converters register handlers per tag
 * name; the converter dispatches open/close events to the appropriate handler.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <b>text</b>} → a handler wraps {@code text} with {@code **}
 *       markers to produce {@code **text**} in Markdown</li>
 *   <li>{@code <a href="…">text</a>} → a handler captures the href on open
 *       and emits {@code [text](href)} on close</li>
 *   <li>{@code <br>} → a handler appends a newline on open (no close needed)</li>
 * </ul>
 *
 * <p>Both methods have default no-op implementations, allowing handlers to
 * override only the callbacks they need. For example, a passthrough handler
 * (which drops the tag but keeps its content) overrides neither method.</p>
 *
 * @see HtmlAttributes
 * @see HtmlConverter
 */
public interface TagHandler {

	/**
	 * Called when an open tag or self-closing tag is encountered.
	 *
	 * @param attrs provides access to the tag's HTML attributes (href, src, alt, colspan)
	 * @param sb    the output buffer to append to
	 */
	default void open(HtmlAttributes attrs, StringBuilder sb) {
	}

	/**
	 * Called when a closing tag is encountered.
	 *
	 * @param sb the output buffer to append to
	 */
	default void close(StringBuilder sb) {
	}
}
