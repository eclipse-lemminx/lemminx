/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XML builder test.
 *
 */
public class XMLBuilderTest {

	SharedSettings settings;

	@BeforeEach
	public void startup() {
		settings = new SharedSettings();
		settings.getFormattingSettings().setInsertSpaces(false);
		settings.getFormattingSettings().setTabSize(4);
	}

	@Test
	public void simple() throws Exception {
		String xml = new XMLBuilder(settings, "", "\n").startElement("html", true).endElement("html").toString();
		assertEquals("<html></html>", xml);
	}

	@Test
	public void simpleLinefeed() throws Exception {
		String xml = new XMLBuilder(settings, "", "\n").startElement("html", true).linefeed().endElement("html")
				.toString();
		assertEquals("<html>\n</html>", xml);
	}

	@Test
	public void elementWithChild() throws Exception {
		String xml = new XMLBuilder(settings, "", "\n").startElement("html", true).linefeed().indent(1)
				.startElement("head", false).selfCloseElement().linefeed().endElement("html").toString();
		assertEquals("<html>\n\t<head />\n</html>", xml);
	}

	// ==========================================
	// addAttribute with splitAttributes modes
	// ==========================================

	@Test
	public void addAttributeForce() throws Exception {
		// force: each attribute on new line with splitAttributesIndentSize indent
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.force);
		s.getFormattingSettings().setSplitAttributesIndentSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("a", "'1'", false)
				.addAttribute("b", "'2'", 0, false)
				.addAttribute("c", "'3'", 0, false)
				.selfCloseElement()
				.toString();
		assertEquals("<root a='1'\n    b='2'\n    c='3' />", xml);
	}

	@Test
	public void addAttributeForceExpandMultiline() throws Exception {
		// force-expand-multiline: all attributes on new lines including the first
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);
		s.getFormattingSettings().setSplitAttributesIndentSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addAttribute("a", "'1'", 0, false)
				.addAttribute("b", "'2'", 0, false)
				.addAttribute("c", "'3'", 0, false)
				.selfCloseElement()
				.toString();
		assertEquals("<root\n    a='1'\n    b='2'\n    c='3' />", xml);
	}

	@Test
	public void addAttributeForceAligned() throws Exception {
		// force-aligned: each attribute on new line, aligned with first attribute
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		// <root  = 6 chars → b and c aligned at column 6
		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("a", "'1'", false)
				.addAttribute("b", "'2'", 0, false)
				.addAttribute("c", "'3'", 0, false)
				.selfCloseElement()
				.toString();
		assertEquals("<root a='1'\n      b='2'\n      c='3' />", xml);
	}

	@Test
	public void addAttributePreserve() throws Exception {
		// preserve: no forced line breaks, all on one line
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("a", "'1'", false)
				.addAttribute("b", "'2'", 0, false)
				.addAttribute("c", "'3'", 0, false)
				.selfCloseElement()
				.toString();
		assertEquals("<root a='1' b='2' c='3' />", xml);
	}

	@Test
	public void addAttributeForceNested() throws Exception {
		// force at indent level 1: whitespacesIndent "  " + indent(1+2=3) * tabSize 2
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.force);
		s.getFormattingSettings().setSplitAttributesIndentSize(2);

		String xml = new XMLBuilder(s, "  ", "\n")
				.startElement("child", false)
				.addSingleAttribute("a", "'1'", false)
				.addAttribute("b", "'2'", 1, false)
				.selfCloseElement()
				.toString();
		assertEquals("<child a='1'\n        b='2' />", xml);
	}

	@Test
	public void addAttributeForceAlignedNested() throws Exception {
		// force-aligned at indent level 1: whitespacesIndent "  " + align with first attr
		// "  <child " = 9 chars → b aligned at column 9
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String xml = new XMLBuilder(s, "  ", "\n")
				.startElement("child", false)
				.addSingleAttribute("a", "'1'", false)
				.addAttribute("b", "'2'", 1, false)
				.selfCloseElement()
				.toString();
		assertEquals("<child a='1'\n         b='2' />", xml);
	}

	// ==========================================
	// Quote escaping in attribute values
	// ==========================================

	@Test
	public void addAttributeEscapesSingleQuoteInValue() throws Exception {
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("href", "http://example.org/o'brien", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root href='http://example.org/o&apos;brien' />", xml);
	}

	@Test
	public void addAttributeEscapesDoubleQuoteInValue() throws Exception {
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("title", "He said \"hello\"", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root title=\"He said &quot;hello&quot;\" />", xml);
	}

	@Test
	public void addAttributeNoEscapeWhenNoConflict() throws Exception {
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("href", "http://example.org/path", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root href=\"http://example.org/path\" />", xml);
	}

	@Test
	public void addAttributeSingleQuoteInValueWithDoubleQuotePreference() throws Exception {
		// Double quote in value, but preferred quote is single → no escaping needed
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("title", "He said \"hello\"", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root title='He said \"hello\"' />", xml);
	}

	@Test
	public void addAttributeDoubleQuoteInValueWithSingleQuotePreference() throws Exception {
		// Single quote in value, but preferred quote is double → no escaping needed
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("href", "o'brien", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root href=\"o'brien\" />", xml);
	}

	@Test
	public void addAttributeMultipleQuotesEscaped() throws Exception {
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("v", "a\"b\"c", true)
				.selfCloseElement()
				.toString();
		assertEquals("<root v=\"a&quot;b&quot;c\" />", xml);
	}

	@Test
	public void addAttributeOnNewLineEscapesQuotes() throws Exception {
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);
		s.getFormattingSettings().setSplitAttributes(SplitAttributes.force);
		s.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.initElementContext("root")
				.addSingleAttribute("a", "it's", true)
				.addAttributeOnNewLine("b", "it's fine", 0, true)
				.selfCloseElement()
				.toString();
		assertEquals("<root a='it&apos;s'\n    b='it&apos;s fine' />", xml);
	}

	@Test
	public void addAttributeNoSurroundQuotesNoEscaping() throws Exception {
		// When surroundWithQuotes is false, no escaping should occur
		SharedSettings s = new SharedSettings();
		s.getFormattingSettings().setInsertSpaces(true);
		s.getFormattingSettings().setTabSize(2);

		String xml = new XMLBuilder(s, "", "\n")
				.startElement("root", false)
				.addSingleAttribute("v", "'raw'", false)
				.selfCloseElement()
				.toString();
		assertEquals("<root v='raw' />", xml);
	}
}
