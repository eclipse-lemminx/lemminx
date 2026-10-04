/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
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
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter tests for trimFinalNewlines setting.
 *
 * <p>
 * When trimFinalNewlines is enabled, trailing blank lines at the end of the
 * document are removed. If insertFinalNewline is also enabled, one trailing
 * newline is preserved.
 * </p>
 */
public class XMLFormatterTrimFinalNewlinesTest {

	// Use case: default settings — all trailing newlines removed
	// <aaa></aaa>\r\n → <aaa></aaa>
	@Test
	public void testDefaultRemovesAllTrailingNewlines() throws BadLocationException {
		String content = "<aaa></aaa>\r\n";
		String expected = "<aaa></aaa>";
		assertFormat(content, expected, //
				te(0, 11, 1, 0, ""));
		assertFormat(expected, expected);
	}

	// Use case: default settings, multiple trailing newlines
	// <aaa></aaa>\r\n\r\n\r\n → <aaa></aaa> (all removed)
	@Test
	public void testDefaultRemovesMultipleTrailingNewlines() throws BadLocationException {
		String content = "<aaa></aaa>\r\n\r\n\r\n";
		String expected = "<aaa></aaa>";
		assertFormat(content, expected, //
				te(0, 11, 3, 0, ""));
		assertFormat(expected, expected);
	}

	// Use case: no trailing newlines — nothing to trim
	// <aaa></aaa> → <aaa></aaa> (unchanged)
	@Test
	public void testNoTrailingNewlines() throws BadLocationException {
		String content = "<aaa></aaa>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Use case: trimFinalNewlines=false — trailing newlines not removed
	// <aaa></aaa>\r\n\r\n → <aaa></aaa>\r\n\r\n (unchanged)
	@Test
	public void testTrimDisabled() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		settings.getFormattingSettings().setInsertFinalNewline(false);

		String content = "<aaa></aaa>\r\n\r\n";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// Use case: document with nested content + trailing newlines
	// <root>              <root>
	//   <child></child>     <child></child>
	// </root>             </root>
	// [blank line]        → removed
	// [blank line]        → removed
	@Test
	public void testNestedContentWithTrailingNewlines() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <child></child>\r\n" + //
				"</root>\r\n\r\n";
		String expected = "<root>\r\n" + //
				"  <child></child>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(2, 7, 4, 0, ""));
		assertFormat(expected, expected);
	}

	// Use case: trimFinalNewlines=false — trailing newlines at end of document
	// are kept as-is
	@Test
	public void testDontTrimFinalNewLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a  ></a>\r\n\r\n\r\n";
		String expected = "<a></a>\r\n\r\n\r\n";

		assertFormat(content, expected, settings, //
				te(0, 2, 0, 4, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: trimFinalNewlines=false — trailing whitespace-only lines preserved
	@Test
	public void testDontTrimFinalNewLines2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a  ></a>\r\n" + //
				"   \r\n\r\n";
		String expected = "<a></a>\r\n" + //
				"   \r\n\r\n";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 4, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: trimFinalNewlines=false — text content is normalized but
	// trailing whitespace lines at end of document are kept
	@Test
	public void testDontTrimFinalNewLines3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a  ></a>\r\n" + //
				"  text \r\n" + //
				"  more text   \r\n" + //
				"   \r\n";
		String expected = "<a></a>\r\n" + //
				"text\r\n" + //
				"more text   \r\n" + //
				"   \r\n";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 4, ""), //
				te(0, 9, 1, 2, "\r\n"), //
				te(1, 6, 2, 2, "\r\n"));
		assertFormat(expected, expected, settings);
	}

	// Use case: trimFinalNewlines=true + trimTrailingWhitespace=false
	// — final newlines are removed but trailing spaces on lines are kept
	@Test
	public void testFormatRemoveFinalNewlinesWithoutTrimTrailing() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(true);
		settings.getFormattingSettings().setTrimTrailingWhitespace(false);
		settings.getFormattingSettings().setSpaceBeforeEmptyCloseTag(false);

		String content = "<aaa/>    \r\n\r\n\r\n";
		String expected = "<aaa/>    ";
		assertFormat(content, expected, settings, //
				te(0, 10, 3, 0, ""));
		assertFormat(expected, expected, settings);
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
