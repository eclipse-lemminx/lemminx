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
 * Handles the {@code <a>} tag, emitting Markdown links with URL safety checks
 * and destination encoding.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code <a href="https://example.com">text</a>}
 *       → {@code [text](https://example.com)}</li>
 *   <li>{@code <a href="javascript:…">text</a>}
 *       → {@code [text]()} (unsafe URL stripped)</li>
 *   <li>{@code <a>text</a>} (no href) → {@code text} (plain text, no link)</li>
 *   <li>{@code <a href="path with spaces">text</a>}
 *       → {@code [text](<path with spaces>)} (angle-bracket wrapping)</li>
 * </ul>
 */
public class LinkHandler implements TagHandler {

	private String lastHref;

	@Override
	public void open(HtmlAttributes attrs, StringBuilder sb) {
		String href = attrs.getHref();
		// Only start a markdown link if href is present.
		// Ex: "<a href='url'>text</a>" → "[" opened here, closed in close()
		// Ex: "<a name='anchor'>text</a>" → no "[", renders as plain "text"
		if (href != null) {
			sb.append("[");
		}
		lastHref = href;
	}

	@Override
	public void close(StringBuilder sb) {
		if (lastHref != null) {
			if (HtmlConverter.isSafeUrl(lastHref)) {
				// Safe URL: emit full link.
				// Ex: "[text](https://example.com)"
				sb.append("](");
				sb.append(formatDestination(normalizeFileUri(lastHref)));
				sb.append(")");
			} else {
				// Unsafe URL (javascript:, vbscript:, data:): emit empty destination.
				// Ex: "[text]()" — link text preserved, dangerous URL removed
				sb.append("](");
				sb.append(")");
			}
		}
		lastHref = null;
	}

	/**
	 * Normalizes {@code file:///} URIs to the two-slash form ({@code file:/})
	 * expected by VS Code's Markdown renderer.
	 *
	 * <p>Use case: {@code "file:///C:/path"} → {@code "file:/C:/path"}</p>
	 */
	static String normalizeFileUri(String uri) {
		if (uri.startsWith("file:///")) {
			return "file:/" + uri.substring(8);
		}
		return uri;
	}

	/**
	 * Formats a URL as a Markdown link destination. Backslashes are
	 * percent-encoded. If the URL contains whitespace or parentheses, it is
	 * wrapped in angle brackets per the CommonMark spec.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code "https://example.com"} → {@code "https://example.com"}</li>
	 *   <li>{@code "path with spaces"} → {@code "<path with spaces>"}</li>
	 * </ul>
	 */
	static String formatDestination(String url) {
		url = url.replace("\\", "%5C");
		boolean needsAngleBrackets = false;
		for (int i = 0; i < url.length(); i++) {
			char c = url.charAt(i);
			if (Character.isWhitespace(c) || c == '(' || c == ')') {
				needsAngleBrackets = true;
				break;
			}
		}
		if (needsAngleBrackets) {
			return "<" + url.replace("<", "%3C").replace(">", "%3E") + ">";
		}
		return url;
	}
}
