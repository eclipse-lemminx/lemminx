/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lemminx.converter.plaintext;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.HtmlConverter;
import org.eclipse.lemminx.converter.TagHandler;

/**
 * Converts HTML content into plain text equivalent for clients that do not
 * support Markdown rendering.
 *
 * <p>HTML structure tags are converted to whitespace (newlines, spaces) while
 * inline formatting is stripped. Unknown tags are preserved as-is.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code "<b>bold</b>"} → {@code "bold"} (formatting stripped)</li>
 *   <li>{@code "<p>one</p><p>two</p>"} → {@code "\none\ntwo\n"} (paragraphs as newlines)</li>
 *   <li>{@code "<a href=\"url\">text</a>"} → {@code "text <url>"} (link appended)</li>
 *   <li>{@code "<ul><li>A</li><li>B</li></ul>"} → bulleted list with {@code *} markers</li>
 *   <li>{@code "<custom-tag>text</custom-tag>"} → {@code "<custom-tag>text</custom-tag>"}
 *       (unknown tags preserved)</li>
 * </ul>
 */
public class PlainTextConverter extends HtmlConverter {

	private final Map<String, TagHandler> contentHandlers = new HashMap<>();

	private String lastHref;
	private int listNesting;

	private PlainTextConverter() {
		contentHandlers.put("br", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				append(sb, "\n");
			}
		});

		contentHandlers.put("ul", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				listNesting++;
			}

			@Override
			public void close(StringBuilder sb) {
				listNesting--;
			}
		});

		contentHandlers.put("li", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				append(sb, "\n ");
				for (int i = 1; i < listNesting; i++) {
					append(sb, "  ");
				}
				if (listNesting <= 1) {
					append(sb, "* ");
				} else {
					append(sb, "- ");
				}
			}
		});

		contentHandlers.put("dt", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				append(sb, "  ");
			}

			@Override
			public void close(StringBuilder sb) {
				append(sb, "\n");
			}
		});

		TagHandler newlineOnOpen = new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				append(sb, "\n");
			}
		};

		TagHandler newlineOnClose = new TagHandler() {
			@Override
			public void close(StringBuilder sb) {
				append(sb, "\n");
			}
		};

		TagHandler newlineOnBoth = new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				append(sb, "\n");
			}

			@Override
			public void close(StringBuilder sb) {
				append(sb, "\n");
			}
		};

		contentHandlers.put("p", newlineOnBoth);
		contentHandlers.put("tr", newlineOnOpen);
		for (int i = 1; i <= 6; i++) {
			contentHandlers.put("h" + i, newlineOnBoth);
		}
		contentHandlers.put("dd", newlineOnClose);

		TagHandler spaceOnClose = new TagHandler() {
			@Override
			public void close(StringBuilder sb) {
				append(sb, " ");
			}
		};
		contentHandlers.put("th", spaceOnClose);
		contentHandlers.put("td", spaceOnClose);

		contentHandlers.put("a", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				lastHref = attrs.getHref();
			}

			@Override
			public void close(StringBuilder sb) {
				if (lastHref != null && isSafeUrl(lastHref)) {
					append(sb, String.format(" <%s>", lastHref));
				}
				lastHref = null;
			}
		});

		// Passthrough handlers for HTML tags (content shown, tags dropped)
		TagHandler passthrough = new TagHandler() {
		};
		for (String tag : new String[] { "b", "strong", "i", "em", "code", "tt",
				"del", "s", "pre", "span", "div", "font", "u", "sub", "sup",
				"blockquote", "hr", "img", "ol", "table",
				"thead", "tbody", "tfoot", "caption", "section", "dl" }) {
			contentHandlers.put(tag, passthrough);
		}
	}

	/**
	 * Converts the given HTML string to plain text.
	 *
	 * @param html the HTML content to convert
	 * @return the plain text equivalent
	 */
	public static String convert(String html) {
		return new PlainTextConverter().process(html);
	}

	@Override
	protected void onOpenTag(String tag, StringBuilder sb) {
		TagHandler handler = contentHandlers.get(tag);
		if (handler != null) {
			handler.open(this, sb);
		} else {
			// Unknown tag: preserve original markup as-is in plain text output.
			// Ex: "<custom-tag attr='val'>" → "<custom-tag attr='val'>"
			append(sb, getOriginalOpenTag());
		}
	}

	@Override
	protected void onCloseTag(String tag, StringBuilder sb) {
		TagHandler handler = contentHandlers.get(tag);
		if (handler != null) {
			handler.close(sb);
		} else {
			// Unknown closing tag: preserve as-is.
			// Ex: "</custom-tag>" → "</custom-tag>"
			append(sb, "</" + tag + ">");
		}
	}

	@Override
	protected void onContent(String rawText, StringBuilder sb) {
		// Collapse whitespace runs and strip leading whitespace at line start.
		// Ex: "  hello   world  " after <p> → "hello world "
		String text = collapseWhitespace(rawText);
		if (sb.length() == 0 || endsWith(sb, " ") || endsWith(sb, "\n")) {
			int start = 0;
			while (start < text.length()) {
				char c = text.charAt(start);
				if (c != ' ' && c != '\n' && c != '\r') {
					break;
				}
				start++;
			}
			if (start > 0) {
				text = text.substring(start);
			}
		}
		if (!text.isEmpty()) {
			append(sb, unescapeEntities(text));
		}
	}

	/**
	 * Appends text to the output buffer with whitespace deduplication.
	 * Suppresses whitespace-only text when the buffer is empty or already
	 * ends with whitespace, but allows newlines after a trailing space.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code append(sb, " ")} when sb="" → suppressed (no leading space)</li>
	 *   <li>{@code append(sb, " ")} when sb="word" → appended (space after word)</li>
	 *   <li>{@code append(sb, "\n")} when sb="word " → appended (newline after space is allowed)</li>
	 *   <li>{@code append(sb, " ")} when sb="word\n" → suppressed (no double whitespace)</li>
	 * </ul>
	 */
	private static void append(StringBuilder sb, String text) {
		if (text.trim().isEmpty()
				&& (sb.length() == 0 || endsWith(sb, "\n")
						|| (endsWith(sb, " ") && !"\n".equals(text)))) {
			return;
		}
		sb.append(text);
	}

	private static boolean endsWith(StringBuilder sb, String suffix) {
		int len = sb.length();
		int sLen = suffix.length();
		if (len < sLen) {
			return false;
		}
		for (int i = 0; i < sLen; i++) {
			if (sb.charAt(len - sLen + i) != suffix.charAt(i)) {
				return false;
			}
		}
		return true;
	}
}
