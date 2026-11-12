/*******************************************************************************
 * Copyright (c) 2016-2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.converter.markdown;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.lemminx.converter.HtmlAttributes;
import org.eclipse.lemminx.converter.HtmlConverter;
import org.eclipse.lemminx.converter.HtmlTag;
import org.eclipse.lemminx.converter.TagHandler;
import org.eclipse.lemminx.dom.parser.Scanner;

/**
 * Converts HTML content into Markdown equivalent using lemminx's fault-tolerant
 * {@link org.eclipse.lemminx.dom.parser.XMLScanner XMLScanner}.
 *
 * <p>This converter is used by the language server to render XSD/DTD documentation
 * as Markdown in hover tooltips and completion details. It replaces the archived
 * remark-java library and jsoup dependency with a self-contained scanner-based
 * approach.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code "<b>bold</b>"} → {@code "**bold**"}</li>
 *   <li>{@code "<a href=\"url\">text</a>"} → {@code "[text](url)"}</li>
 *   <li>{@code "<pre>code</pre>"} → {@code "```\ncode\n```"}</li>
 *   <li>{@code "<table><tr><th>A</th></tr></table>"} → {@code "| A |\n| --- |"}</li>
 *   <li>Unsafe URLs are sanitized: {@code "<a href=\"javascript:…\">x</a>"}
 *       → {@code "[x]()"}</li>
 * </ul>
 *
 * @author Fred Bricon
 */
public class MarkdownConverter extends HtmlConverter {

	// Structure handlers are always dispatched (even inside <pre>).
	// Ex: <table> and <tr> must be tracked to redirect cell content.
	private final Map<String, TagHandler> structureHandlers = new HashMap<>();

	// Content handlers are skipped when inside a <pre> block.
	// Ex: <b> → "**" in normal mode, but ignored inside <pre>.
	private final Map<String, TagHandler> contentHandlers = new HashMap<>();

	private final PreHandler preHandler = new PreHandler();
	private final TableHandler tableHandler = new TableHandler();
	private final ListHandler listHandler = new ListHandler();
	private int blockDepth;

	private MarkdownConverter() {
		// Structure handlers (always active, even in pre mode)
		structureHandlers.put("table", tableHandler.table);
		structureHandlers.put("tr", tableHandler.row);
		structureHandlers.put("th", tableHandler.header);
		structureHandlers.put("td", tableHandler.cell);
		contentHandlers.put("img", new ImageHandler());

		// Content handlers (skipped in pre mode)
		contentHandlers.put("b", InlineHandler.BOLD);
		contentHandlers.put("strong", InlineHandler.BOLD);
		contentHandlers.put("i", InlineHandler.ITALIC);
		contentHandlers.put("em", InlineHandler.ITALIC);
		contentHandlers.put("del", InlineHandler.STRIKETHROUGH);
		contentHandlers.put("s", InlineHandler.STRIKETHROUGH);
		contentHandlers.put("code", InlineHandler.CODE);
		contentHandlers.put("tt", InlineHandler.CODE);
		contentHandlers.put("pre", preHandler);
		contentHandlers.put("br", BreakHandler.INSTANCE);
		contentHandlers.put("hr", BlockHandler.HORIZONTAL_RULE);
		contentHandlers.put("p", new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				if (!listHandler.isInsideList()) {
					HtmlConverter.ensureBlankLine(sb);
				}
			}

			@Override
			public void close(StringBuilder sb) {
				if (listHandler.isInsideList()) {
					HtmlConverter.ensureNewline(sb);
				} else {
					HtmlConverter.ensureBlankLine(sb);
				}
			}
		});
		contentHandlers.put("blockquote", BlockHandler.BLOCKQUOTE);
		contentHandlers.put("dl", BlockHandler.DEFINITION_LIST);
		contentHandlers.put("dt", BlockHandler.DEFINITION_TERM);
		contentHandlers.put("dd", BlockHandler.DEFINITION_DESC);
		contentHandlers.put("a", new LinkHandler());
		for (int i = 1; i <= 6; i++) {
			contentHandlers.put("h" + i, new HeadingHandler(i));
		}
		contentHandlers.put("ul", listHandler.unorderedList);
		contentHandlers.put("ol", listHandler.orderedList);
		contentHandlers.put("li", listHandler.listItem);

		// Passthrough handlers for HTML tags without markdown equivalents
		TagHandler passthrough = new TagHandler() {
		};
		contentHandlers.put("span", passthrough);
		contentHandlers.put("div", passthrough);
		contentHandlers.put("font", passthrough);
		contentHandlers.put("u", passthrough);
		contentHandlers.put("sub", passthrough);
		contentHandlers.put("sup", passthrough);
		contentHandlers.put("section", passthrough);
		contentHandlers.put("article", passthrough);
		contentHandlers.put("aside", passthrough);
		contentHandlers.put("details", passthrough);
		contentHandlers.put("summary", passthrough);
		contentHandlers.put("nav", passthrough);
		contentHandlers.put("header", BlockHandler.PARAGRAPH);
		contentHandlers.put("footer", BlockHandler.PARAGRAPH);
		contentHandlers.put("figure", passthrough);
		TagHandler spaceBefore = new TagHandler() {
			@Override
			public void open(HtmlAttributes attrs, StringBuilder sb) {
				if (sb.length() > 0 && !HtmlConverter.endsWithNewline(sb)
						&& sb.charAt(sb.length() - 1) != ' ') {
					sb.append(' ');
				}
			}
		};
		contentHandlers.put("figcaption", spaceBefore);
		contentHandlers.put("fieldset", passthrough);
		contentHandlers.put("legend", passthrough);
		contentHandlers.put("main", passthrough);
		contentHandlers.put("address", passthrough);
		contentHandlers.put("form", passthrough);
		structureHandlers.put("thead", passthrough);
		structureHandlers.put("tbody", passthrough);
		structureHandlers.put("tfoot", passthrough);
		structureHandlers.put("caption", passthrough);
	}

	/**
	 * Converts the given HTML string to Markdown.
	 *
	 * <p>If the input contains no HTML tags, entities are unescaped and the
	 * result is returned directly. Otherwise a full scan-based conversion is
	 * performed.</p>
	 *
	 * @param html the HTML content to convert (may be {@code null}-free)
	 * @return the Markdown equivalent
	 */
	public static String convert(String html) {
		if (html.indexOf('<') == -1) {
			// Fast path: no HTML tags, just unescape entities.
			// Ex: "5 &gt; 3" → "5 > 3"
			String result = unescapeEntities(html);
			if (result.trim().isEmpty()) {
				return "";
			}
			return stripBoundaryNewlines(result);
		}
		// Scanner path: parse HTML tags and convert to Markdown.
		// Fenced code blocks (``` or ~~~) are detected in processContent()
		// which skips the scanner past the fenced content to avoid
		// tokenizing XML-like tags inside the block.
		return new MarkdownConverter().process(html);
	}

	@Override
	protected void onOpenTag(String tag, StringBuilder sb) {
		if (isBlockElement(tag)) {
			blockDepth++;
		}

		// Structure handlers run first (always active, even inside <pre>).
		// Ex: <table> inside <pre> still needs to be tracked for cell routing.
		TagHandler structural = structureHandlers.get(tag);
		if (structural != null) {
			structural.open(this, sb);
			return;
		}

		// When inside a table, content goes to the cell buffer instead of sb.
		// Ex: "<td>cell text</td>" → text written to cell buffer, flushed on </table>.
		StringBuilder target = tableHandler.getTarget(sb);

		// Inside <pre>, only <br> produces output (as a newline); other tags are ignored.
		// Ex: "<pre><b>bold</b></pre>" → "```\nbold\n```" (no ** markers)
		if (preHandler.isActive()) {
			if ("br".equals(tag)) {
				target.append(LINE_SEP);
			}
			return;
		}

		TagHandler handler = contentHandlers.get(tag);
		if (handler != null) {
			handler.open(this, target);
		} else {
			String original = getOriginalOpenTag();
			if (isAtLineStart(target)) {
				// Unknown tag alone on its own line → fenced code block.
				// Ex: standalone "<beans profile="p1,!p2">" in XSD CDATA
				// → "```\n<beans profile="p1,!p2">\n```"
				trimTrailingWhitespaceOnCurrentLine(target);
				ensureNewline(target);
				target.append("```").append(LINE_SEP);
				target.append(original);
				target.append(LINE_SEP).append("```");
			} else {
				// Unknown tag inline with text → inline code.
				// Ex: "this <beans> element" → "this `<beans>` element"
				// Adjacent backticks are merged: "`<a>`" + "`</a>`" → "`<a></a>`"
				if (target.length() > 0 && target.charAt(target.length() - 1) == '`') {
					target.append(original);
				} else {
					target.append('`').append(original).append('`');
				}
			}
		}
	}

	@Override
	protected void onCloseTag(String tag, StringBuilder sb) {
		if (isBlockElement(tag) && blockDepth > 0) {
			blockDepth--;
		}

		TagHandler structural = structureHandlers.get(tag);
		if (structural != null) {
			structural.close(sb);
			return;
		}

		StringBuilder target = tableHandler.getTarget(sb);

		// Inside <pre>, skip closing tags except </pre> itself.
		// Ex: "</b>" inside pre is ignored; "</pre>" closes the fenced block.
		if (preHandler.isActive() && !"pre".equals(tag)) {
			return;
		}

		TagHandler handler = contentHandlers.get(tag);
		if (handler != null) {
			handler.close(target);
		} else {
			String closeTag = "</" + tag + ">";
			if (isAtLineStart(target)) {
				// Unknown closing tag alone on its own line → fenced code block.
				trimTrailingWhitespaceOnCurrentLine(target);
				ensureNewline(target);
				target.append("```").append(LINE_SEP);
				target.append(closeTag);
				target.append(LINE_SEP).append("```");
			} else {
				// Unknown closing tag inline → inline code, merging with adjacent backticks.
				// Ex: "</custom-tag>" → "`</custom-tag>`"
				if (target.length() > 0 && target.charAt(target.length() - 1) == '`') {
					target.append(closeTag);
				} else {
					target.append("`").append(closeTag).append("`");
				}
			}
		}
	}

	@Override
	protected void onContent(String rawText, StringBuilder sb) {
		StringBuilder target = tableHandler.getTarget(sb);
		if (preHandler.isActive()) {
			// Inside <pre>: preserve raw text (only unescape entities).
			// Ex: "<pre>  spaces  preserved  </pre>" → "  spaces  preserved  "
			target.append(unescapeEntities(rawText));
		} else if (blockDepth > 0) {
			// Inside block HTML: collapse whitespace runs.
			// Ex: "  hello   world  " → " hello world "
			String text = collapseWhitespace(rawText);
			appendContent(text, target);
		} else {
			// Top-level markdown content: preserve newlines from source.
			// Ex: text between paragraphs or outside any block element.
			appendMarkdownContent(rawText, target);
		}
	}

	@Override
	protected String postProcess(String result) {
		return stripBoundaryNewlines(result);
	}

	private static void appendContent(String text, StringBuilder sb) {
		if (text.trim().isEmpty() && (sb.length() == 0 || endsWithNewline(sb))) {
			return;
		}
		if (sb.length() == 0 || endsWithNewline(sb)) {
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
			sb.append(unescapeEntities(text));
		}
	}

	private static void appendMarkdownContent(String rawText, StringBuilder sb) {
		String text = unescapeEntities(rawText);
		int start = 0;
		if (sb.length() == 0 || endsWithNewline(sb)) {
			while (start < text.length()) {
				char c = text.charAt(start);
				if (c != ' ' && c != '\t') {
					break;
				}
				start++;
			}
		}
		for (int i = start; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\r') {
				sb.append(LINE_SEP);
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
			} else if (c == '\n') {
				sb.append(LINE_SEP);
			} else {
				sb.append(c);
			}
		}
	}

	/**
	 * Returns {@code true} if the output buffer has only whitespace (spaces,
	 * tabs, NBSPs) since the last newline, or is empty. This indicates the
	 * current position is at the start of a line, meaning an unknown tag here
	 * is standalone and should be rendered as a code block.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code "text "} → false (text on current line)</li>
	 *   <li>{@code "text\n"} → true (at line start)</li>
	 *   <li>{@code "text\n  "} → true (only NBSPs after newline)</li>
	 *   <li>{@code ""} → true (empty buffer)</li>
	 * </ul>
	 */
	private static boolean isAtLineStart(StringBuilder sb) {
		for (int i = sb.length() - 1; i >= 0; i--) {
			char c = sb.charAt(i);
			if (c == '\n' || c == '\r') {
				return true;
			}
			if (c != ' ' && c != '\t' && c != ' ') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes trailing whitespace (spaces, tabs, NBSPs) on the current line,
	 * stopping at the last newline. Used before inserting a code block to
	 * remove NBSP indentation left by {@code joinProseLines}.
	 *
	 * <p>Ex: {@code "e.g.\n\n  "} → {@code "e.g.\n\n"}</p>
	 */
	private static void trimTrailingWhitespaceOnCurrentLine(StringBuilder sb) {
		int i = sb.length() - 1;
		while (i >= 0) {
			char c = sb.charAt(i);
			if (c == '\n' || c == '\r') {
				break;
			}
			if (c != ' ' && c != '\t' && c != ' ') {
				break;
			}
			i--;
		}
		sb.setLength(i + 1);
	}

	/**
	 * Detects Markdown fenced code blocks ({@code ```} or {@code ~~~}) in the
	 * content and skips the scanner past the closing fence so that XML-like tags
	 * inside the fenced block are not tokenized as HTML.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code "```xml\n<key>value</key>\n```"} → scanner skips past the
	 *       fenced block, {@code <key>} is preserved as-is in the output</li>
	 *   <li>{@code "<b>bold</b>\n```\n<el/>\n```"} → {@code <b>} processed
	 *       normally, fenced content preserved</li>
	 *   <li>Content without fence markers → delegates to
	 *       {@link #onContent(String, StringBuilder)}</li>
	 * </ul>
	 */
	@Override
	protected void processContent(Scanner scanner, String html, StringBuilder sb) {
		String contentText = scanner.getTokenText();
		int tokenEnd = scanner.getTokenEnd();

		// Find the last unclosed fence opener in this content chunk.
		// Ex: "text\n```xml\n" → fenceOpenerLine = start of "```xml" line
		int fenceOpenerLine = findOpenFenceInContent(contentText);
		if (fenceOpenerLine < 0) {
			// No fence opener — process content normally.
			onContent(contentText, sb);
			return;
		}

		// Emit content before the fence opener through normal processing.
		// Ex: "text\n```xml\n" → "text\n" processed as Markdown
		if (fenceOpenerLine > 0) {
			onContent(contentText.substring(0, fenceOpenerLine), sb);
		}

		// Calculate the fence opener position in the raw HTML.
		char fenceChar = contentText.charAt(fenceOpenerLine);
		int fenceStartInHtml = tokenEnd - contentText.length() + fenceOpenerLine;

		// Find the closing fence (same char, e.g. ``` matches ``` but not ~~~).
		// Search starts after the opener line in the raw HTML.
		int closingFenceEnd = findClosingFence(html, tokenEnd, fenceChar);

		// Emit the entire fenced block as-is (fence opener through closing fence).
		// Ex: "```xml\n<key>value</key>\n```\n" emitted verbatim
		StringBuilder target = tableHandler.getTarget(sb);
		target.append(html, fenceStartInHtml, closingFenceEnd);

		// Skip the scanner past the fenced block so it does not tokenize
		// XML-like tags inside the block (e.g., <key> treated as StartTag).
		scanner.goTo(closingFenceEnd);
	}

	/**
	 * Finds the line start of the last unclosed fence opener in a content chunk.
	 * Returns {@code -1} if no fence is open at the end of the chunk.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code "```xml\n"} → returns 0 (fence opens at start)</li>
	 *   <li>{@code "text\n```\n"} → returns 5 (fence opens after "text\n")</li>
	 *   <li>{@code "```\ncode\n```\n"} → returns -1 (fence opened then closed)</li>
	 *   <li>{@code "no fence here"} → returns -1</li>
	 * </ul>
	 */
	private static int findOpenFenceInContent(String text) {
		int len = text.length();
		int lineStart = 0;
		boolean inFence = false;
		char fenceChar = 0;
		int fenceOpenerLine = -1;

		for (int i = 0; i <= len; i++) {
			if (i == len || text.charAt(i) == '\n') {
				if (isFenceMarker(text, lineStart, i)) {
					char c = text.charAt(lineStart);
					if (!inFence) {
						inFence = true;
						fenceChar = c;
						fenceOpenerLine = lineStart;
					} else if (c == fenceChar) {
						inFence = false;
						fenceOpenerLine = -1;
					}
				}
				lineStart = i + 1;
			}
		}
		return fenceOpenerLine;
	}

	/**
	 * Searches the raw HTML starting from {@code from} for a closing fence
	 * marker that matches the given fence character. Returns the offset
	 * immediately after the closing fence line (including its newline),
	 * or the end of the HTML if no closing fence is found.
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>Input after {@code "```xml\n"}: {@code "<key/>\n```\nmore"}
	 *       → returns offset after {@code "```\n"}</li>
	 *   <li>Input after {@code "~~~\n"}: {@code "<el/>\n```\n~~~\n"}
	 *       → skips non-matching {@code ```}, returns after {@code "~~~\n"}</li>
	 *   <li>No closing fence found → returns {@code html.length()}</li>
	 * </ul>
	 */
	private static int findClosingFence(String html, int from, char fenceChar) {
		int len = html.length();
		int lineStart = from;
		for (int i = from; i <= len; i++) {
			if (i == len || html.charAt(i) == '\n') {
				if (isFenceMarker(html, lineStart, i)
						&& html.charAt(lineStart) == fenceChar) {
					// Return offset after the closing fence line (including newline).
					return i < len ? i + 1 : i;
				}
				lineStart = i + 1;
			}
		}
		return len;
	}

	private static boolean isFenceMarker(String text, int start, int end) {
		if (end - start < 3) {
			return false;
		}
		char c = text.charAt(start);
		return (c == '`' || c == '~')
				&& text.charAt(start + 1) == c && text.charAt(start + 2) == c;
	}

	private static boolean isBlockElement(String tag) {
		return HtmlTag.isBlockElement(tag);
	}

	private static String stripBoundaryNewlines(String text) {
		int start = 0;
		while (start < text.length()) {
			char c = text.charAt(start);
			if (c != '\n' && c != '\r') {
				break;
			}
			start++;
		}
		int end = text.length();
		while (end > start) {
			char c = text.charAt(end - 1);
			if (c != '\n' && c != '\r') {
				break;
			}
			end--;
		}
		return text.substring(start, end);
	}
}
