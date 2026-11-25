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
 * Handles the {@code <pre>} tag, emitting fenced code blocks and tracking
 * pre-mode state so the converter skips inline formatting tags inside pre blocks.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <pre>code here</pre>} → {@code "```\ncode here\n```"}</li>
 *   <li>{@code <pre><b>bold</b></pre>} → {@code "```\nbold\n```"}
 *       (formatting tags stripped inside pre)</li>
 *   <li>{@code <pre>line1<br>line2</pre>} → {@code "```\nline1\nline2\n```"}
 *       ({@code <br>} still produces newlines inside pre)</li>
 * </ul>
 */
public class PreHandler implements TagHandler {

	// When true, the converter skips inline formatting (bold, italic, etc.)
	// and preserves raw text content. Only <br> still produces a newline.
	private boolean active;

	/**
	 * Returns whether we are currently inside a {@code <pre>} block.
	 * The converter checks this to decide whether to apply formatting handlers.
	 */
	public boolean isActive() {
		return active;
	}

	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		active = true;
		HtmlConverter.trimTrailingSpaces(sb);
		HtmlConverter.ensureNewline(sb);
		sb.append("```").append(HtmlConverter.LINE_SEP);
	}

	@Override
	public void close(StringBuilder sb) {
		if (!HtmlConverter.endsWithNewline(sb)) {
			sb.append(HtmlConverter.LINE_SEP);
		}
		sb.append("```").append(HtmlConverter.LINE_SEP);
		active = false;
	}
}
