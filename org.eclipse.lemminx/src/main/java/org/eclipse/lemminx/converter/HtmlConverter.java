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

import java.util.Locale;

import org.eclipse.lemminx.dom.parser.Scanner;
import org.eclipse.lemminx.dom.parser.TokenType;
import org.eclipse.lemminx.dom.parser.XMLScanner;

/**
 * Base class for HTML-to-text converters using lemminx's fault-tolerant
 * {@link XMLScanner}. Walks an HTML string token by token, accumulates
 * attributes for the current tag, and dispatches events to subclass callbacks.
 *
 * <p>This class also implements {@link HtmlAttributes} so that the converter
 * instance itself can be passed as the attribute source to
 * {@link TagHandler#open(HtmlAttributes, StringBuilder)}, avoiding a separate
 * allocation per tag.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@link org.eclipse.lemminx.converter.markdown.MarkdownConverter MarkdownConverter}
 *       — converts HTML documentation from XSD/DTD into Markdown for hover tooltips:
 *       {@code "<b>bold</b>"} → {@code "**bold**"}</li>
 *   <li>{@link org.eclipse.lemminx.converter.plaintext.PlainTextConverter PlainTextConverter}
 *       — converts HTML into plain text for clients that do not support Markdown:
 *       {@code "<b>bold</b>"} → {@code "bold"}</li>
 * </ul>
 *
 * @see TagHandler
 * @see HtmlAttributes
 */
public abstract class HtmlConverter implements HtmlAttributes {

	public static final String LINE_SEP = System.lineSeparator();

	private String pendingTag;
	private String pendingAttr;
	private String pendingHref;
	private String pendingSrc;
	private String pendingAlt;
	private int pendingColspan;
	private int pendingRowspan;
	private String inputHtml;
	private int pendingTagStartOffset;
	private int pendingTagEndOffset;
	// Tracks the offset of a StartTagOpen or EndTagOpen that has not yet
	// been confirmed as a real tag (via StartTag/EndTag). When the scanner
	// encounters bare < or </ not followed by a valid tag name (e.g. "x < 100"),
	// this allows recovery: the consumed region is emitted as text content.
	private int pendingStartTagOpenOffset = -1;

	public String getHref() {
		return pendingHref;
	}

	public String getSrc() {
		return pendingSrc;
	}

	public String getAlt() {
		return pendingAlt;
	}

	public int getColspan() {
		return pendingColspan;
	}

	public int getRowspan() {
		return pendingRowspan;
	}

	/**
	 * Scans the given HTML string and builds the converted output by dispatching
	 * tag events to {@link #onOpenTag}, {@link #onCloseTag}, and
	 * {@link #onContent}.
	 *
	 * @param html the HTML content to convert
	 * @return the converted text
	 */
	protected String process(String html) {
		this.inputHtml = html;
		Scanner scanner = XMLScanner.createScanner(html);
		StringBuilder sb = new StringBuilder();

		TokenType token = scanner.scan();
		while (token != TokenType.EOS) {
			switch (token) {
				case StartTagOpen:
					// Ex: scanning "<" in "<a href="url">text</a>"
					// Save offset in case this isn't a real tag (e.g. "x < 100")
					if (pendingStartTagOpenOffset >= 0) {
						onContent(inputHtml.substring(pendingStartTagOpenOffset, scanner.getTokenOffset()), sb);
					}
					pendingStartTagOpenOffset = scanner.getTokenOffset();
					break;

				case StartTag:
					// Ex: scanning "a" in "<a href="url">text</a>"
					// Confirmed this is a real tag, clear the pending offset
					pendingStartTagOpenOffset = -1;
					pendingTag = toLowerCase(scanner.getTokenText());
					pendingTagStartOffset = scanner.getTokenOffset() - 1;
					pendingHref = null;
					pendingSrc = null;
					pendingAlt = null;
					pendingAttr = null;
					pendingColspan = 1;
					pendingRowspan = 1;
					break;

				case AttributeName:
					pendingAttr = toLowerCase(scanner.getTokenText());
					break;

				case DelimiterAssign:
					if (pendingAttr != null) {
						String unquoted = extractUnquotedValue(html, scanner.getTokenEnd());
						if (unquoted != null) {
							applyAttributeValue(unescapeEntities(unquoted));
						}
					}
					break;

				case AttributeValue:
					if (pendingAttr != null) {
						applyAttributeValue(unescapeEntities(unquote(scanner.getTokenText())));
					}
					break;

				case StartTagClose:
				case StartTagSelfClose:
					pendingTagEndOffset = scanner.getTokenEnd();
					if (pendingTag != null) {
						onOpenTag(pendingTag, sb);
						pendingTag = null;
					} else if (pendingStartTagOpenOffset >= 0) {
						// No valid tag name after < (e.g. "x < 100 and y > 0")
						// Emit the consumed region as text content
						onContent(inputHtml.substring(pendingStartTagOpenOffset, scanner.getTokenEnd()), sb);
						pendingStartTagOpenOffset = -1;
					}
					break;

				case EndTagOpen:
					if (pendingStartTagOpenOffset >= 0) {
						onContent(inputHtml.substring(pendingStartTagOpenOffset, scanner.getTokenOffset()), sb);
					}
					pendingStartTagOpenOffset = scanner.getTokenOffset();
					break;

				case EndTag:
					pendingStartTagOpenOffset = -1;
					onCloseTag(toLowerCase(scanner.getTokenText()), sb);
					break;

				case EndTagClose:
					if (pendingStartTagOpenOffset >= 0) {
						onContent(inputHtml.substring(pendingStartTagOpenOffset, scanner.getTokenEnd()), sb);
						pendingStartTagOpenOffset = -1;
					}
					break;

				case Content:
					processContent(scanner, html, sb);
					break;

				default:
					break;
			}
			token = scanner.scan();
		}
		if (pendingStartTagOpenOffset >= 0) {
			onContent(inputHtml.substring(pendingStartTagOpenOffset), sb);
		}
		return postProcess(sb.toString());
	}

	private void applyAttributeValue(String val) {
		if ("href".equals(pendingAttr)) {
			pendingHref = val;
		} else if ("src".equals(pendingAttr)) {
			pendingSrc = val;
		} else if ("alt".equals(pendingAttr)) {
			pendingAlt = val;
		} else if ("colspan".equals(pendingAttr)) {
			pendingColspan = parseColspan(val);
		} else if ("rowspan".equals(pendingAttr)) {
			pendingRowspan = parseColspan(val);
		}
		pendingAttr = null;
	}

	/**
	 * Called when an open tag (or self-closing tag) has been fully scanned,
	 * including all its attributes.
	 *
	 * @param tag the lowercase tag name (e.g. {@code "a"}, {@code "div"})
	 * @param sb  the output buffer
	 */
	protected abstract void onOpenTag(String tag, StringBuilder sb);

	/**
	 * Called when a closing tag is encountered.
	 *
	 * @param tag the lowercase tag name
	 * @param sb  the output buffer
	 */
	protected abstract void onCloseTag(String tag, StringBuilder sb);

	/**
	 * Called for text content between tags.
	 *
	 * @param rawText the raw text content (may contain entities, whitespace)
	 * @param sb      the output buffer
	 */
	protected abstract void onContent(String rawText, StringBuilder sb);

	/**
	 * Called when a {@link TokenType#Content} token is scanned. The default
	 * implementation delegates to {@link #onContent}. Subclasses may override
	 * to handle regions that should not be scanned as HTML (e.g., Markdown
	 * fenced code blocks containing XML-like tags) and optionally reposition
	 * the scanner with {@link Scanner#goTo(int)}.
	 *
	 * <p>Use case: {@code MarkdownConverter} detects a {@code ```} fence opener
	 * in the content, finds the closing fence in the raw HTML, emits the fenced
	 * block as-is, and skips the scanner past it.</p>
	 *
	 * @param scanner the scanner (use {@link Scanner#goTo(int)} to skip ahead)
	 * @param html    the full raw HTML input
	 * @param sb      the output buffer
	 */
	protected void processContent(Scanner scanner, String html, StringBuilder sb) {
		onContent(scanner.getTokenText(), sb);
	}

	/**
	 * Hook for subclasses to post-process the final output string
	 * (e.g. stripping boundary newlines).
	 *
	 * @param result the raw converted output
	 * @return the post-processed result
	 */
	protected String postProcess(String result) {
		return result;
	}

	/**
	 * Returns the original HTML source text for the current open tag, including
	 * the {@code <} delimiter, tag name, attributes, and closing {@code >}.
	 *
	 * <p>Use case: when a tag has no registered handler, the plain-text converter
	 * preserves the original markup as-is (e.g. {@code <custom-tag attr="val">}).</p>
	 *
	 * @return the original open tag source text
	 */
	protected String getOriginalOpenTag() {
		return inputHtml.substring(pendingTagStartOffset, pendingTagEndOffset);
	}

	// --- Shared utility methods ---

	/**
	 * Resolves HTML character entities in the given text.
	 *
	 * <p>Handles named entities ({@code &amp;lt;}, {@code &amp;gt;}, {@code &amp;amp;},
	 * {@code &amp;quot;}, {@code &amp;apos;}, {@code &amp;nbsp;}) and numeric
	 * entities ({@code &amp;#60;}, {@code &amp;#x3C;}).</p>
	 *
	 * <p>Use case: {@code "5 &amp;gt; 3"} → {@code "5 > 3"}</p>
	 *
	 * @param text the text possibly containing HTML entities
	 * @return the text with entities resolved
	 */
	public static String unescapeEntities(String text) {
		if (text.indexOf('&') == -1) {
			return text;
		}
		StringBuilder sb = new StringBuilder(text.length());
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '&') {
				int semiIdx = -1;
				for (int j = i + 1, max = Math.min(text.length(), i + 11); j < max; j++) {
					if (text.charAt(j) == ';') {
						semiIdx = j;
						break;
					}
				}
				if (semiIdx > i + 1) {
					String entity = text.substring(i + 1, semiIdx);
					String resolved = resolveEntity(entity);
					if (resolved != null) {
						sb.append(resolved);
						i = semiIdx + 1;
						continue;
					}
				}
			}
			sb.append(c);
			i++;
		}
		return sb.toString();
	}

	private static String resolveEntity(String entity) {
		switch (entity) {
			case "lt":
				return "<";
			case "gt":
				return ">";
			case "amp":
				return "&";
			case "quot":
				return "\"";
			case "apos":
				return "'";
			case "nbsp":
				return " ";
			default:
				if (entity.length() > 1 && entity.charAt(0) == '#') {
					try {
						int codePoint;
						if (entity.charAt(1) == 'x' || entity.charAt(1) == 'X') {
							codePoint = Integer.parseInt(entity.substring(2), 16);
						} else {
							codePoint = Integer.parseInt(entity.substring(1));
						}
						if (!Character.isValidCodePoint(codePoint)) {
							return null;
						}
						return new String(Character.toChars(codePoint));
					} catch (NumberFormatException e) {
						return null;
					}
				}
				return null;
		}
	}

	/**
	 * Collapses all whitespace (spaces, tabs, newlines) into single spaces,
	 * following HTML whitespace collapsing rules.
	 *
	 * <p>Use case: {@code "  hello \n world  "} → {@code " hello world "}</p>
	 *
	 * @param text the raw text content
	 * @return text with whitespace collapsed
	 */
	public static String collapseWhitespace(String text) {
		StringBuilder result = new StringBuilder(text.length());
		boolean lastWasSpace = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\r') {
				if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				if (!lastWasSpace) {
					result.append(' ');
					lastWasSpace = true;
				}
			} else if (c == '\n' || c == ' ' || c == '\t' || c == '\f') {
				if (!lastWasSpace) {
					result.append(' ');
					lastWasSpace = true;
				}
			} else {
				result.append(c);
				lastWasSpace = false;
			}
		}
		return result.toString();
	}

	/**
	 * Checks whether a URL is safe to include in rendered output. Rejects
	 * {@code javascript:}, {@code vbscript:}, and {@code data:} schemes
	 * to prevent XSS in Markdown/plain-text rendering.
	 *
	 * <p>The check is hardened against common bypass techniques:</p>
	 * <ul>
	 *   <li>Multi-level entity encoding ({@code javascript&amp;amp;#58;alert(1)})
	 *       is fully decoded before checking</li>
	 *   <li>Leading C0 control characters ({@code &#1;javascript:…}) are stripped</li>
	 *   <li>Embedded tabs, newlines, carriage returns, and backslashes
	 *       ({@code java&#9;script:…}, {@code javascript\:…}) are ignored
	 *       during scheme extraction</li>
	 *   <li>Scheme comparison is case-insensitive ({@code JAVASCRIPT:} is blocked)</li>
	 * </ul>
	 *
	 * <p>Use cases:</p>
	 * <ul>
	 *   <li>{@code isSafeUrl("https://example.com")} → {@code true}</li>
	 *   <li>{@code isSafeUrl("javascript:alert(1)")} → {@code false}</li>
	 *   <li>{@code isSafeUrl("\u0001javascript:alert(1)")} → {@code false}</li>
	 *   <li>{@code isSafeUrl("java\tscript:alert(1)")} → {@code false}</li>
	 *   <li>{@code isSafeUrl("#section")} → {@code true} (fragment-only)</li>
	 *   <li>{@code isSafeUrl("../path/file.html")} → {@code true} (relative path)</li>
	 * </ul>
	 *
	 * @param url the URL to check (may still contain entity references)
	 * @return {@code true} if the URL is safe to render, {@code false} otherwise
	 */
	public static boolean isSafeUrl(String url) {
		// Step 0: Fully decode entities to prevent multi-level encoding bypasses.
		// Ex: "javascript&amp;#58;alert(1)" → first pass "javascript&#58;alert(1)"
		//     → second pass "javascript:alert(1)" → blocked
		String decoded = url;
		for (;;) {
			String next = unescapeEntities(decoded);
			if (next.equals(decoded)) {
				break;
			}
			decoded = next;
		}
		url = decoded;

		// Step 1: Skip leading C0 control characters and whitespace.
		// Ex: "\u0001javascript:…" → skip \u0001, then check "javascript:…"
		int i = 0;
		while (i < url.length()
				&& (url.charAt(i) <= 0x20 || Character.isWhitespace(url.charAt(i)))) {
			i++;
		}
		// Step 2: Find the colon that separates the scheme from the rest.
		// Skip embedded tabs/LF/CR/backslash that attackers use to evade detection.
		// Ex: "java\tscript:…" → colon found at index 15, tabs ignored
		// Ex: "javascript\:…" → backslash ignored, scheme = "javascript" → blocked
		// Ex: "../path" → '/' hit before ':', so no scheme detected → safe
		int colonIdx = -1;
		for (int j = i; j < url.length(); j++) {
			char c = url.charAt(j);
			if (c == '\t' || c == '\n' || c == '\r' || c == '\\') {
				continue;
			}
			if (c == ':') {
				colonIdx = j;
				break;
			}
			if (c == '/' || c == '?' || c == '#') {
				break;
			}
		}
		// No colon found → relative URL or fragment, always safe
		// Ex: "#section", "?q=test", "../file.html"
		if (colonIdx <= i) {
			return true;
		}
		// Step 3: Extract the scheme, stripping embedded tabs/LF/CR/backslash.
		// Ex: "java\tscript" → "javascript" after stripping
		// Ex: "java\\script" → "javascript" after stripping
		StringBuilder scheme = new StringBuilder(colonIdx - i);
		for (int j = i; j < colonIdx; j++) {
			char c = url.charAt(j);
			if (c != '\t' && c != '\n' && c != '\r' && c != '\\') {
				scheme.append(c);
			}
		}
		// Step 4: Case-insensitive comparison against blocked schemes.
		// Ex: "JavaScript" → "javascript" → blocked
		// Ex: "https" → safe, "mailto" → safe, "ftp" → safe
		String s = scheme.toString().toLowerCase(Locale.ROOT);
		return !"javascript".equals(s) && !"vbscript".equals(s) && !"data".equals(s);
	}

	public static String toLowerCase(String text) {
		return text.toLowerCase(Locale.ROOT);
	}

	public static boolean endsWithNewline(StringBuilder sb) {
		if (sb.length() == 0) {
			return false;
		}
		char last = sb.charAt(sb.length() - 1);
		return last == '\n' || last == '\r';
	}

	public static void trimTrailingSpaces(StringBuilder sb) {
		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
			sb.deleteCharAt(sb.length() - 1);
		}
	}

	public static void stripTrailingNewlines(StringBuilder sb) {
		while (sb.length() > 0) {
			char c = sb.charAt(sb.length() - 1);
			if (c != '\n' && c != '\r') {
				break;
			}
			sb.deleteCharAt(sb.length() - 1);
		}
	}

	/**
	 * Ensures the output buffer ends with a blank line (two newlines) if it
	 * contains any content. Used before/after block elements like {@code <p>}.
	 *
	 * @param sb the output buffer
	 */
	public static void ensureBlankLine(StringBuilder sb) {
		trimTrailingSpaces(sb);
		stripTrailingNewlines(sb);
		if (sb.length() > 0) {
			sb.append(LINE_SEP).append(LINE_SEP);
		}
	}

	/**
	 * Ensures the output buffer ends with at least one newline if it contains
	 * any content. Used at the start of list items, blockquotes, etc.
	 *
	 * @param sb the output buffer
	 */
	public static void ensureNewline(StringBuilder sb) {
		if (sb.length() > 0 && !endsWithNewline(sb)) {
			sb.append(LINE_SEP);
		}
	}

	private static String extractUnquotedValue(String html, int pos) {
		while (pos < html.length() && Character.isWhitespace(html.charAt(pos))) {
			pos++;
		}
		if (pos >= html.length()) {
			return null;
		}
		char ch = html.charAt(pos);
		if (ch == '"' || ch == '\'') {
			return null;
		}
		int start = pos;
		while (pos < html.length()) {
			ch = html.charAt(pos);
			if (Character.isWhitespace(ch) || ch == '>' || ch == '<') {
				break;
			}
			pos++;
		}
		return start == pos ? null : html.substring(start, pos);
	}

	private static String unquote(String value) {
		if (value.length() >= 2) {
			char first = value.charAt(0);
			if ((first == '"' || first == '\'') && value.charAt(value.length() - 1) == first) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}

	private static int parseColspan(String value) {
		try {
			int n = Integer.parseInt(value);
			return n > 0 ? n : 1;
		} catch (NumberFormatException e) {
			return 1;
		}
	}
}
