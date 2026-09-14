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

import java.util.Arrays;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with whitespace setting.
 *
 */
public class XMLFormatterWhitespaceSettingTest {

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

	@Test
	public void testClosingBracketNewLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='' c=''/>";
		String expected = "<a" + lineSeparator() + "b=''" + lineSeparator() + "c=''" + lineSeparator() + "/>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, lineSeparator()), //
				te(0, 7, 0, 8, lineSeparator()), //
				te(0, 12, 0, 12, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithDefaultIndentSize() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a b='b' c='c'/>";
		String expected = "<a" + System.lineSeparator() + //
				"    b='b'" + System.lineSeparator() + //
				"    c='c'" + System.lineSeparator() + //
				"    />";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, lineSeparator() + "    "), //
				te(0, 8, 0, 9, lineSeparator() + "    "), //
				te(0, 14, 0, 14, lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithAlignWithFirstAttr() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='b' c='c'/>";
		String expected = "<a b='b'" + System.lineSeparator() + //
				"   c='c'" + System.lineSeparator() + //
				"   />";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, lineSeparator() + "   "), //
				te(0, 14, 0, 14, lineSeparator() + "   "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithAlignWithFirstAttrNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='b' c='c'>\n" + //
				"  <b c='c' d='d'/>\n" + //
				"</a>";
		String expected = "<a b='b'\n" + //
				"   c='c'\n" + //
				"   >\n" + //
				"  <b c='c'\n" + //
				"     d='d'\n" + //
				"     />\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, "\n   "), //
				te(0, 14, 0, 14, "\n   "), //
				te(1, 10, 1, 11, "\n     "), //
				te(1, 16, 1, 16, "\n     "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithoutSplitAttributes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='' c=''/>";
		String expected = "<a b='' c='' />";
		assertFormat(content, expected, settings, //
				te(0, 12, 0, 12, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithSingleAttribute() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b=''/>";
		String expected = "<a b='' />";
		assertFormat(content, expected, settings, te(0, 7, 0, 7, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithChildElementIndent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + "  <b c='' d=''/>" + lineSeparator() + "</a>";
		String expected = "<a>" + lineSeparator() + "  <b" + lineSeparator() + "  c=''" + lineSeparator() + "  d=''"
				+ lineSeparator() + "  />" + lineSeparator() + "</a>";
		assertFormat(content, expected, settings, //
				te(1, 4, 1, 5, lineSeparator() + "  "), //
				te(1, 9, 1, 10, lineSeparator() + "  "), //
				te(1, 14, 1, 14, lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithPreserveEmptyContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setPreserveEmptyContent(true);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + "<b c='' d=''></b>" + lineSeparator() + "</a>";
		String expected = "<a>" + lineSeparator() + "  <b" + lineSeparator() + "  c=''" + lineSeparator() + "  d=''"
				+ lineSeparator() + "  ></b>" + lineSeparator() + "</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, lineSeparator() + "  "), //
				te(1, 2, 1, 3, lineSeparator() + "  "), //
				te(1, 7, 1, 8, lineSeparator() + "  "), //
				te(1, 12, 1, 12, lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testClosingBracketNewLineWithPreserveEmptyContentSingleAttribute() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setPreserveEmptyContent(true);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + "<b></b>" + lineSeparator() + "</a>";
		String expected = "<a>" + lineSeparator() + "  <b></b>" + lineSeparator() + "</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1026
	@Test
	public void testPreserveInternalWhitespaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<Tag><Inner>Test     Spaces</Inner></Tag>";
		String expected = "<Tag>" + lineSeparator() + //
				"  <Inner>Test     Spaces</Inner>" + lineSeparator() + //
				"</Tag>";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 5, lineSeparator() + "  "), //
				te(0, 35, 0, 35, lineSeparator()));
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
