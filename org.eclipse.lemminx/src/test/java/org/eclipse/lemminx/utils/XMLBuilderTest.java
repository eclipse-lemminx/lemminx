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
}
