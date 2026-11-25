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
 * Handles the {@code <img>} tag, emitting Markdown image syntax with URL
 * safety checks.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <img src="logo.png" alt="Logo"/>} → {@code ![Logo](logo.png)}</li>
 *   <li>{@code <img src="javascript:…" alt="x"/>} → {@code x}
 *       (unsafe URL stripped, alt text preserved)</li>
 *   <li>{@code <img src="javascript:…"/>} → {@code ""}
 *       (unsafe URL, no alt text)</li>
 * </ul>
 */
public class ImageHandler implements TagHandler {

	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		String src = attrs.getSrc();
		String alt = attrs.getAlt();
		if (src != null && !HtmlConverter.isSafeUrl(src)) {
			// Unsafe image URL: emit alt text only (no image link).
			// Ex: '<img src="javascript:…" alt="photo"/>' → "photo"
			sb.append(alt != null ? alt : "");
		} else {
			// Safe image URL: emit markdown image syntax.
			// Ex: '<img src="logo.png" alt="Logo"/>' → "![Logo](logo.png)"
			sb.append("![");
			sb.append(alt != null ? alt : "");
			sb.append("](");
			sb.append(src != null ? LinkHandler.formatDestination(src) : "");
			sb.append(")");
		}
	}
}
