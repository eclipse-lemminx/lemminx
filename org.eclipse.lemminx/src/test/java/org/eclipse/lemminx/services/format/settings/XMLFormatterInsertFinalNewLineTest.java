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

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with insert final new lines.
 *
 */
public class XMLFormatterInsertFinalNewLineTest extends AbstractCacheBasedTest {

	@Test
	public void testTrimFinalNewlinesDefault() throws BadLocationException {
		String content = "<a  ></a>\r\n";
		String expected = "<a></a>";
		assertFormat(content, expected, //
				te(0, 2, 0, 4, ""), //
				te(0, 9, 1, 0, ""));
		assertFormat(expected, expected);

	}

	@Test
	public void testDontInsertFinalNewLine1() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "";
		assertFormat(content, content, settings);
	}

	@Test
	public void testDontInsertFinalNewLine2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<a  ></a>\r\n";
		String expected = "<a></a>\r\n";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 4, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testDontInsertFinalNewLine3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		settings.getFormattingSettings().setInsertFinalNewline(false);
		String content = "<a  ></a>\r\n" + "   ";
		String expected = "<a></a>\r\n" + "   ";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 4, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testInsertFinalNewLine1() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<a></a>";
		String expected = "<a></a>" + lineSeparator();
		assertFormat(content, expected, settings, //
				te(0, 7, 0, 7, lineSeparator()));
	}

	@Test
	public void testInsertFinalNewLine2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(true);
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<a></a>\r\n\r\n";
		String expected = "<a></a>\r\n";
		assertFormat(content, expected, settings, //
				te(1, 0, 2, 0, ""));
	}

	@Test
	public void testInsertFinalNewLine3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(true);
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<a></a>\n\n";
		String expected = "<a></a>\n";
		assertFormat(content, expected, settings, //
				te(1, 0, 2, 0, ""));
	}

	// Use case: insertFinalNewline=true with range format — final newline not
	// inserted when range doesn't include end of document
	@Test
	public void testDontInsertFinalNewLineWithRange() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<div  class = \"foo\">\r\n" + //
				"  |<img  src = \"foo\"|/>\r\n" + //
				" </div>";
		String expected = "<div  class = \"foo\">\r\n" + //
				"  <img src=\"foo\" />\r\n" + //
				" </div>";
		assertFormat(content, expected, settings, //
				te(1, 6, 1, 8, " "), //
				te(1, 11, 1, 12, ""), //
				te(1, 13, 1, 14, ""), //
				te(1, 19, 1, 19, " "));
	}

	// Use case: insertFinalNewline=true with range including end of document
	// — final newline IS inserted
	@Test
	public void testInsertFinalNewLineWithRange2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<div  class = \"foo\">\r\n" + //
				"  |<img  src = \"foo\"/>\r\n" + //
				" </div>|";
		String expected = "<div  class = \"foo\">\r\n" + //
				"  <img src=\"foo\" />\r\n" + //
				"</div>\r\n";
		assertFormat(content, expected, settings, //
				te(1, 6, 1, 8, " "), //
				te(1, 11, 1, 12, ""), //
				te(1, 13, 1, 14, ""), //
				te(1, 19, 1, 19, " "), //
				te(1, 21, 2, 1, "\r\n"), //
				te(2, 7, 2, 7, "\r\n"));
	}

	// Use case: insertFinalNewline=true with range including blank lines
	// — blank lines inside range preserved, final newline not inserted (range
	// doesn't reach end of document)
	@Test
	public void testInsertFinalNewLineWithRange3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setInsertFinalNewline(true);
		String content = "<div  class = \"foo\">\r\n" + //
				"  |<img  src = \"foo\"/>\r\n" + //
				"\r\n" + "|" + "\r\n" + //
				"<h1></h1>\r\n" + //
				" </div>";
		String expected = "<div  class = \"foo\">\r\n" + //
				"  <img src=\"foo\" />\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"<h1></h1>" + "\r\n" + //
				" </div>";
		assertFormat(content, expected, settings, //
				te(1, 6, 1, 8, " "), //
				te(1, 11, 1, 12, ""), //
				te(1, 13, 1, 14, ""), //
				te(1, 19, 1, 19, " "));
	}

	private static void assertFormat(String unformatted, String actual, TextEdit... expectedEdits)
			throws BadLocationException {
		assertFormat(unformatted, actual, new SharedSettings(), expectedEdits);
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
