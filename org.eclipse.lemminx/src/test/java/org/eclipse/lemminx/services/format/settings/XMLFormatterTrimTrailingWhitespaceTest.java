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

import java.util.Arrays;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter tests for trimTrailingWhitespace setting.
 *
 * <p>
 * Use case: trailing spaces at the end of each line are removed,
 * while internal whitespace between words is preserved.
 * </p>
 *
 * <pre>
 * &lt;a&gt;   [LF]         &lt;a&gt;[LF]
 * text     [LF]   →     text[LF]
 * &lt;/a&gt;   [LF]         &lt;/a&gt;
 * </pre>
 */
public class XMLFormatterTrimTrailingWhitespaceTest {

	// Use case: trailing spaces removed from each line of text content
	// <a>   \n text     \n ... </a>    → <a>\n  text\n  ... </a>
	@Test
	public void testTrimTrailingWhitespaceText() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);

		String content = "<a>   \n" + //
				"text     \n" + //
				"    text text text    \n" + //
				"    text\n" + //
				"</a>   ";
		String expected = "<a>\n" + //
				"  text\n" + //
				"  text text text\n" + //
				"  text\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\n  "), //
				te(1, 4, 2, 4, "\n  "), //
				te(2, 18, 3, 4, "\n  "), //
				te(4, 4, 4, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: trailing spaces removed + joinContentLines joins text into one line
	// <a>   \n text     \n ... </a> → <a> text text ... </a>
	@Test
	public void testTrimTrailingWhitespaceTextJoinContentLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>   \n" + //
				"text     \n" + //
				"    text text text    \n" + //
				"    text\n" + //
				"</a>   ";
		String expected = "<a> text text text text text </a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, " "), //
				te(1, 4, 2, 4, " "), //
				te(2, 18, 3, 4, " "), //
				te(3, 8, 4, 0, " "), //
				te(4, 4, 4, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: trailing spaces trimmed but preserveSpace keeps internal whitespace
	// <a>   \n text     \n → <a>\n text\n (spaces at line end removed)
	@Test
	public void testTrimTrailingWhitespaceTextPreserveEmptyContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));

		String content = "<a>   \n" + //
				"text     \n" + //
				"    text text text    \n" + //
				"    text\n" + //
				"</a>   ";
		String expected = "<a>\n" + //
				"text\n" + //
				"    text text text\n" + //
				"    text\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 0, 6, ""), //
				te(1, 4, 1, 9, ""), //
				te(2, 18, 2, 22, ""), //
				te(4, 4, 4, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: blank lines with only spaces are trimmed
	// <a>   \n   \n </a> → <a>\n\n</a>
	@Test
	public void testTrimTrailingWhitespaceNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<a>   \n" + //
				"   \n" + //
				"</a>   ";
		String expected = "<a>\n" + //
				"\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 2, 0, "\n\n"), //
				te(2, 4, 2, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: trailing whitespace at end of document is removed
	// </a>   \n   \n       → </a>
	@Test
	public void testTrimTrailingWhitespaceAtEnd() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<a>   \n" + //
				"</a>   " + //
				"   \n" + //
				"   \n       ";
		String expected = "<a>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\n"), //
				te(1, 4, 3, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: CRLF line endings, trimFinalNewlines=false — trailing spaces
	// removed but newlines preserved
	@Test
	public void testTrimTrailingWhitespaceAtEndTwoCharLineSeparator() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a>   \r\n" + //
				"</a>\r\n   " + //
				"   \r\n" + //
				"   \r\n ";
		String expected = "<a>\r\n" + //
				"</a>\r\n" + //
				"\r\n" + //
				"\r\n";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\r\n"), //
				te(2, 0, 2, 6, ""), //
				te(3, 0, 3, 3, ""), //
				te(4, 0, 4, 1, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: CRLF line endings, trimFinalNewlines=true — trailing spaces
	// AND final newlines removed
	@Test
	public void testTrimTrailingWhitespaceAtEndTwoCharLineSeparatorTrimFinalNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		settings.getFormattingSettings().setTrimFinalNewlines(true);
		String content = "<a>   \r\n" + //
				"</a>\r\n   " + //
				"   \r\n" + //
				"   \r\n  ";
		String expected = "<a>\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, "\r\n"), //
				te(1, 4, 4, 2, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: mixed text and blank lines — trailing spaces trimmed,
	// blank lines preserved
	@Test
	public void testTrimTrailingWhitespaceTextAndNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<a>   \n" + //
				"    \n" + //
				"text     \n" + //
				"    text text text    \n" + //
				"   \n" + //
				"    text\n" + //
				"        \n" + //
				"</a>   ";
		String expected = "<a>\n" + //
				"\n" + //
				"  text\n" + //
				"  text text text\n" + //
				"\n" + //
				"  text\n" + //
				"\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 2, 0, "\n\n  "), //
				te(2, 4, 3, 4, "\n  "), //
				te(3, 18, 5, 4, "\n\n  "), //
				te(5, 8, 7, 0, "\n\n"), //
				te(7, 4, 7, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: mixed text and blank lines with preserveSpace — trailing
	// spaces trimmed but indentation preserved
	@Test
	public void testTrimTrailingWhitespaceTextAndNewlinesPreserveEmptyContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));
		String content = "<a>   \n" + //
				"    \n" + //
				"text     \n" + //
				"    text text text    \n" + //
				"   \n" + //
				"    text\n" + //
				"        \n" + //
				"</a>   ";
		String expected = "<a>\n" + //
				"\n" + //
				"text\n" + //
				"    text text text\n" + //
				"\n" + //
				"    text\n" + //
				"\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 0, 6, ""), //
				te(1, 0, 1, 4, ""), //
				te(2, 4, 2, 9, ""), //
				te(3, 18, 3, 22, ""), //
				te(4, 0, 4, 3, ""), //
				te(6, 0, 6, 8, ""), //
				te(7, 4, 7, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case: CRLF line endings — same as LF but with \r\n
	@Test
	public void testTrimTrailingWhitespaceTextAndNewlinesTwoCharLineSeparator() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<a>   \r\n" + //
				"    \r\n" + //
				"text     \r\n" + //
				"    text text text    \r\n" + //
				"   \r\n" + //
				"    text\r\n" + //
				"        \r\n" + //
				"</a>   ";
		String expected = "<a>\r\n" + //
				"\r\n" + //
				"  text\r\n" + //
				"  text text text\r\n" + //
				"\r\n" + //
				"  text\r\n" + //
				"\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 2, 0, "\r\n\r\n  "), //
				te(2, 4, 3, 4, "\r\n  "), //
				te(3, 18, 5, 4, "\r\n\r\n  "), //
				te(5, 8, 7, 0, "\r\n\r\n"), //
				te(7, 4, 7, 7, ""));
		assertFormat(expected, expected, settings);
	}

	// Use case (#1026): internal whitespace "f as  as" is preserved (not collapsed
	// to "f as as") while trailing whitespace on other lines is still trimmed.
	@Test
	public void testTrimTrailingWhitespaceWithRange() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<aaa>\r\n" + //
				"  <bbb>\r\n" + //
				"    |asdf               \r\n" + //
				"    asd a;s jlkaj k lk ;alkdsj alskdj a;lskdj a\r\n" + //
				"    a jssa j\r\n" + //
				"    sd asd\r\n" + //
				"    fsdf\r\n" + //
				"    fsd a\r\n" + //
				"    sd f\r\n" + //
				"    asd     \r\n" + //
				"    f as  as\r\n" + //
				"    hjkl    |\r\n" + //
				"  </bbb>\r\n" + //
				"  <ccc>\r\n" + //
				"  </ccc>\r\n" + //
				"</aaa>\r\n";
		String expected = "<aaa>\r\n" + //
				"  <bbb>\r\n" + //
				"    asdf\r\n" + //
				"    asd a;s jlkaj k lk ;alkdsj alskdj a;lskdj a\r\n" + //
				"    a jssa j\r\n" + //
				"    sd asd\r\n" + //
				"    fsdf\r\n" + //
				"    fsd a\r\n" + //
				"    sd f\r\n" + //
				"    asd\r\n" + //
				"    f as  as\r\n" + //
				"    hjkl\r\n" + //
				"  </bbb>\r\n" + //
				"  <ccc>\r\n" + //
				"  </ccc>\r\n" + //
				"</aaa>\r\n";
		assertFormat(content, expected, settings, //
				te(2, 8, 3, 4, "\r\n    "), //
				te(9, 7, 10, 4, "\r\n    "), //
				te(11, 8, 11, 12, ""));
	}

	// Use case (#1026): same as testTrimTrailingWhitespaceWithRange but with LF
	// line endings. Internal whitespace "f as  as" is preserved.
	@Test
	public void testTrimTrailingWhitespaceWithRangeSingleCharLineSeparator() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<aaa>\n" + //
				"  <bbb>\n" + //
				"    |asdf               \n" + //
				"    asd a;s jlkaj k lk ;alkdsj alskdj a;lskdj a\n" + //
				"    a jssa j\n" + //
				"    sd asd\n" + //
				"    fsdf\n" + //
				"    fsd a\n" + //
				"    sd f\n" + //
				"    asd     \n" + //
				"    f as  as\n" + //
				"    hjkl    |\n" + //
				"  </bbb>\n" + //
				"  <ccc>\n" + //
				"  </ccc>\n" + //
				"</aaa>\n";
		String expected = "<aaa>\n" + //
				"  <bbb>\n" + //
				"    asdf\n" + //
				"    asd a;s jlkaj k lk ;alkdsj alskdj a;lskdj a\n" + //
				"    a jssa j\n" + //
				"    sd asd\n" + //
				"    fsdf\n" + //
				"    fsd a\n" + //
				"    sd f\n" + //
				"    asd\n" + //
				"    f as  as\n" + //
				"    hjkl\n" + //
				"  </bbb>\n" + //
				"  <ccc>\n" + //
				"  </ccc>\n" + //
				"</aaa>\n";
		assertFormat(content, expected, settings, //
				te(2, 8, 3, 4, "\n    "), //
				te(9, 7, 10, 4, "\n    "), //
				te(11, 8, 11, 12, ""));
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
