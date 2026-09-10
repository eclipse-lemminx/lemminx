/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format.settings;

import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.EmptyElements;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with preserve attribute line
 * breaks.
 *
 */
public class XMLFormatterPreserveAttributeLineBreaksTest {

	@Test
	public void preserveAttributeLineBreaksFormatProlog() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<?xml \n" + //
				"version=\"1.0\"\n" + //
				"encoding=\"UTF-8\"?>\n" + //
				"<a><b attr=\"value\" attr=\"value\"\n" + //
				" attr\n" + //
				" =\n" + //
				" \"value\"></b>\n" + //
				"</a>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<a>\n" + //
				"  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\"></b>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 5, 1, 0, " "), //
				te(1, 13, 2, 0, " "), //
				te(3, 3, 3, 3, "\n  "), //
				te(3, 31, 4, 1, "\n    "), //
				te(4, 5, 5, 1, ""), //
				te(5, 2, 6, 1, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: element with multi-line attributes and no content.
	// The closing tag </b> must be indented to match <b>, even when the
	// only content between > and </b> is a single newline.
	@Test
	public void preserveAttributeLineBreaks() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);

		String content = "<a>\n" + //
				"<b attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\">\n" + //
				"</b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\">\n" + //
				"  </b>\n" + //
				"</a>";

		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\n  "), //
				te(1, 28, 2, 0, "\n    "), //
				te(2, 25, 3, 0, "\n    "), //
				te(3, 26, 4, 0, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a>\n" + //
				"  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\"\n" + //
				"    attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\"></b>\n" + //
				"</a\n" + //
				"\n" + //
				">";
		String expected = "<a>\n" + //
				"  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\"\n" + //
				"    attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\"></b>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(5, 3, 7, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a\n" + //
				"  attr=\"value\"\n" + //
				"  attr=\"value\"\n" + //
				"\n" + //
				"></a>";
		String expected = "<a\n" + //
				"  attr=\"value\"\n" + //
				"  attr=\"value\"\n" + //
				"></a>";
		assertFormat(content, expected, settings, //
				te(2, 14, 4, 0, "\n"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks4() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a\n" + //
				"attr\n" + //
				"=\n" + //
				"\"value\"\n" + //
				"attr\n" + //
				"=\n" + //
				"\"value\"\n" + //
				"/>";
		String expected = "<a\n" + //
				"  attr=\"value\"\n" + //
				"  attr=\"value\"\n" + //
				"/>";
		assertFormat(content, expected, settings, //
				te(0, 2, 1, 0, "\n  "), //
				te(1, 4, 2, 0, ""), //
				te(2, 1, 3, 0, ""), //
				te(3, 7, 4, 0, "\n  "), //
				te(4, 4, 5, 0, ""), //
				te(5, 1, 6, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks5() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a attr=\"value\" attr=\"value\"\n" + //
				"  attr=\n" + //
				"  \"value\" attr=\"value\"></a>";
		String expected = "<a attr=\"value\" attr=\"value\"\n" + //
				"  attr=\"value\" attr=\"value\"></a>";
		assertFormat(content, expected, settings, //
				te(1, 7, 2, 2, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks6() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a attr=\"value\"\n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaks7() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a attr=\"value\"\n" + //
				"/>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksMissingValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a attr= attr=\"value\"\n" + //
				"  attr=\n" + //
				"   attr=\"value\"></a>";
		String expected = "<a attr= attr=\"value\"\n" + //
				"  attr=\n" + //
				"  attr=\"value\"></a>";
		assertFormat(content, expected, settings, //
				te(1, 7, 2, 3, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksCollapseEmptyElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);
		settings.getFormattingSettings().setGrammarAwareFormatting(false);

		String content = "<a>\n" + //
				"<b attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\">\n" + //
				"</b>\n" + "</a>";
		String expected = "<a>\n" + "  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\" />\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\n  "), //
				te(1, 28, 2, 0, "\n    "), //
				te(2, 25, 3, 0, "\n    "), //
				te(3, 25, 4, 4, " />"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksCollapseEmptyElement2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);
		settings.getFormattingSettings().setGrammarAwareFormatting(false);

		String content = "<a>\n" + //
				"<b attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\"\n" + //
				"attr=\"value\" attr=\"value\"\n" + //
				">\n" + //
				"</b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\"\n" + //
				"    attr=\"value\" attr=\"value\" />\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\n  "), //
				te(1, 28, 2, 0, "\n    "), //
				te(2, 25, 3, 0, "\n    "), //
				te(3, 25, 5, 4, " />"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksCollapseEmptyElement3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);
		settings.getFormattingSettings().setGrammarAwareFormatting(false);

		String content = "<a>\n" + //
				"</a>";
		String expected = "<a />";
		assertFormat(content, expected, settings, //
				te(0, 2, 1, 4, " />"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksRangeFormatting() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);

		String content = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  |a4=\"123456789\" a5=\"123456789\" a6=\"123456789|\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				"/>";
		String expected = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				"/>";
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksRangeFormattingWithEndTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);

		String content = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  |a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				"/>|";
		String expected = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				"/>";
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksRangeFormattingWithEndTag2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setEmptyElement(EmptyElements.collapse);

		String content = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  |a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\" />|";
		String expected = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\" />";
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksRangeFormattingWithEndTag3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);

		String content = "<a a1=\"1234|56789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				">|</a>";
		String expected = "<a a1=\"123456789\" a2=\"123456789\" a3=\"123456789\"\n" + //
				"  a4=\"123456789\" a5=\"123456789\" a6=\"123456789\"\n" + //
				"  a7=\"123456789\" a8=\"123456789\" a9=\"123456789\"\n" + //
				"></a>";
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksIndentCloseTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);
		String content = "<a>\r\n" + //
				"  <b attr=\"\"                \r\n" + //
				"/>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b attr=\"\"\r\n" + //
				"  />\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 12, 2, 0, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void preserveAttributeLineBreaksIndentCloseTagAndSpaceBeforeEmptyCloseTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(true);
		String content = "<a>\r\n" + //
				"  <b attr=\"\"                \r\n" + //
				"/>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b attr=\"\"\r\n" + //
				"  />\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 12, 2, 0, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void noPreserveAttributeLineBreaksIndentCloseTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);
		String content = "<a>\r\n" + //
				"  <b attr=\"\"                \r\n" + //
				"/>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b attr=\"\"/>\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 12, 2, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void noPreserveAttributeLineBreaksIndentCloseTagAndSpaceBeforeEmptyCloseTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(true);
		String content = "<a>\r\n" + //
				"  <b attr=\"\"                \r\n" + //
				"/>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b attr=\"\" />\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 12, 2, 0, " "));
		assertFormat(expected, expected, settings);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "test://test.html", expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, uri, true, expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			Boolean considerRangeFormat, TextEdit... expectedEdits) throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, uri, considerRangeFormat, expectedEdits);
	}

}
