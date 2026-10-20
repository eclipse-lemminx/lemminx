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

import java.util.HashMap;
import java.util.Map;

/**
 * Enumerates known HTML tags with their block/inline/void classification.
 *
 * <p>
 * Used by {@link org.eclipse.lemminx.converter.markdown.MarkdownConverter MarkdownConverter}
 * to detect block transitions, and by
 * {@link org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager XSDAnnotationModel}
 * to decide whether XSD documentation prose needs line-break handling.
 * </p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>{@code HtmlTag.isBlockElement("div")} → {@code true} (block element)</li>
 *   <li>{@code HtmlTag.isBlockElement("span")} → {@code false} (inline element)</li>
 *   <li>{@code HtmlTag.isBlockElement(text, start, end)} → case-insensitive range check
 *       without substring allocation, used by the SAX documentation handler</li>
 *   <li>{@code HtmlTag.of("img").getCategory()} → {@code Category.VOID}</li>
 * </ul>
 */
public enum HtmlTag {

	// Inline formatting
	B("b", Category.INLINE),
	STRONG("strong", Category.INLINE),
	I("i", Category.INLINE),
	EM("em", Category.INLINE),
	DEL("del", Category.INLINE),
	S("s", Category.INLINE),
	CODE("code", Category.INLINE),
	TT("tt", Category.INLINE),

	// Inline passthrough
	SPAN("span", Category.INLINE),
	FONT("font", Category.INLINE),
	U("u", Category.INLINE),
	SUB("sub", Category.INLINE),
	SUP("sup", Category.INLINE),

	// Inline special
	A("a", Category.INLINE),

	// Void elements
	BR("br", Category.VOID),
	HR("hr", Category.VOID),
	IMG("img", Category.VOID),

	// Block elements with specific markdown equivalents
	P("p", Category.BLOCK),
	DIV("div", Category.BLOCK),
	BLOCKQUOTE("blockquote", Category.BLOCK),
	PRE("pre", Category.BLOCK),

	// Headings
	H1("h1", Category.BLOCK),
	H2("h2", Category.BLOCK),
	H3("h3", Category.BLOCK),
	H4("h4", Category.BLOCK),
	H5("h5", Category.BLOCK),
	H6("h6", Category.BLOCK),

	// Lists
	UL("ul", Category.BLOCK),
	OL("ol", Category.BLOCK),
	LI("li", Category.BLOCK),

	// Definition lists
	DL("dl", Category.BLOCK),
	DT("dt", Category.BLOCK),
	DD("dd", Category.BLOCK),

	// Tables
	TABLE("table", Category.BLOCK),
	THEAD("thead", Category.BLOCK),
	TBODY("tbody", Category.BLOCK),
	TFOOT("tfoot", Category.BLOCK),
	CAPTION("caption", Category.BLOCK),
	TR("tr", Category.BLOCK),
	TH("th", Category.BLOCK),
	TD("td", Category.BLOCK),

	// Semantic block elements
	HEADER("header", Category.BLOCK),
	FOOTER("footer", Category.BLOCK),
	SECTION("section", Category.BLOCK),
	ARTICLE("article", Category.BLOCK),
	ASIDE("aside", Category.BLOCK),
	NAV("nav", Category.BLOCK),
	MAIN("main", Category.BLOCK),
	FIGURE("figure", Category.BLOCK),
	FIGCAPTION("figcaption", Category.BLOCK),
	DETAILS("details", Category.BLOCK),
	SUMMARY("summary", Category.BLOCK),
	FIELDSET("fieldset", Category.BLOCK),
	LEGEND("legend", Category.BLOCK),
	ADDRESS("address", Category.BLOCK),
	FORM("form", Category.BLOCK),

	// Less common block elements (CommonMark HTML blocks)
	BODY("body", Category.BLOCK),
	CENTER("center", Category.BLOCK),
	COLGROUP("colgroup", Category.BLOCK),
	DIALOG("dialog", Category.BLOCK),
	DIR("dir", Category.BLOCK),
	FRAMESET("frameset", Category.BLOCK),
	HEAD("head", Category.BLOCK),
	HTML("html", Category.BLOCK),
	IFRAME("iframe", Category.BLOCK),
	MENU("menu", Category.BLOCK),
	MENUITEM("menuitem", Category.BLOCK),
	NOFRAMES("noframes", Category.BLOCK),
	OPTGROUP("optgroup", Category.BLOCK),
	OPTION("option", Category.BLOCK),
	SEARCH("search", Category.BLOCK),
	TITLE("title", Category.BLOCK);

	/**
	 * Classification of HTML elements.
	 * <ul>
	 *   <li>{@code BLOCK} — block-level elements (p, div, table, h1–h6, …)</li>
	 *   <li>{@code INLINE} — inline elements (b, em, span, a, …)</li>
	 *   <li>{@code VOID} — self-closing elements with no content (br, hr, img)</li>
	 * </ul>
	 */
	public enum Category {
		BLOCK, INLINE, VOID
	}

	private final String tagName;
	private final Category category;

	private static final Map<String, HtmlTag> BY_NAME;
	static {
		HtmlTag[] values = values();
		BY_NAME = new HashMap<>(values.length * 2);
		for (HtmlTag tag : values) {
			BY_NAME.put(tag.tagName, tag);
		}
	}

	HtmlTag(String tagName, Category category) {
		this.tagName = tagName;
		this.category = category;
	}

	public String getTagName() {
		return tagName;
	}

	public Category getCategory() {
		return category;
	}

	public boolean isBlock() {
		return category == Category.BLOCK;
	}

	/**
	 * Returns the {@link HtmlTag} for the given lowercase tag name, or
	 * {@code null} if not recognized.
	 */
	public static HtmlTag of(String name) {
		return BY_NAME.get(name);
	}

	/**
	 * Returns {@code true} if the given lowercase tag name is a known block
	 * element.
	 */
	public static boolean isBlockElement(String name) {
		HtmlTag tag = BY_NAME.get(name);
		return tag != null && tag.isBlock();
	}

	/**
	 * Returns {@code true} if the tag name at the given range (case-insensitive)
	 * is a known block element. Avoids substring allocation by lowercasing
	 * in-place.
	 */
	public static boolean isBlockElement(String text, int start, int end) {
		int len = end - start;
		if (len <= 0 || len > 10) {
			return false;
		}
		char[] buf = new char[len];
		for (int i = 0; i < len; i++) {
			char c = text.charAt(start + i);
			buf[i] = (c >= 'A' && c <= 'Z') ? (char) (c + 32) : c;
		}
		return isBlockElement(new String(buf));
	}
}
