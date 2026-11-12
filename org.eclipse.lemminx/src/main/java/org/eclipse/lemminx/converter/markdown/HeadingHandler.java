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
 * Handles heading tags ({@code <h1>} through {@code <h6>}) using ATX-style
 * Markdown with closing hashes.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <h1>Title</h1>} → {@code # Title #}</li>
 *   <li>{@code <h3>Section</h3>} → {@code ### Section ###}</li>
 * </ul>
 */
public class HeadingHandler implements TagHandler {

	private final int level;

	/**
	 * @param level the heading level (1 for {@code <h1>}, 6 for {@code <h6>})
	 */
	public HeadingHandler(int level) {
		this.level = level;
	}

	/**
	 * Emits the opening ATX-style heading prefix.
	 *
	 * <p>Use case: {@code <h2>} → ensures blank line, then emits {@code "## "}.</p>
	 */
	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		// Blank line before heading to separate from previous content.
		// Ex: "paragraph\n\n## Heading"
		HtmlConverter.ensureBlankLine(sb);
		for (int i = 0; i < level; i++) {
			sb.append('#');
		}
		sb.append(' ');
	}

	/**
	 * Emits the closing ATX-style heading suffix and a newline.
	 *
	 * <p>Use case: {@code </h2>} → emits {@code " ##\n"} after the heading text.</p>
	 */
	@Override
	public void close(StringBuilder sb) {
		// Closing hashes for ATX-style heading.
		// Ex: "## Title ##\n"
		sb.append(' ');
		for (int i = 0; i < level; i++) {
			sb.append('#');
		}
		sb.append(HtmlConverter.LINE_SEP);
	}
}
