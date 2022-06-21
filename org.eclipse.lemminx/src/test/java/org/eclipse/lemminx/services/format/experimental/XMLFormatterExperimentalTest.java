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
package org.eclipse.lemminx.services.format.experimental;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * XML experimental formatter services tests
 *
 */
public class XMLFormatterExperimentalTest {

	@Test
	public void emptyXML() throws BadLocationException {
		String content = "";
		String expected = content;
		assertFormat(expected, expected);
	}

	@Test
	public void bracket() throws BadLocationException {
		String content = "<";
		String expected = content;
		assertFormat(expected, expected);
	}

	// ---------- Tests for tag elements formatting

	@Test
	public void closeStartTagMissing() throws BadLocationException {
		// Don't close tag with bad XML
		String content = "<a";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void closeTagMissing() throws BadLocationException {
		// Don't close tag with bad XML
		String content = "<a>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void autoCloseTag() throws BadLocationException {
		String content = "<a/>";
		String expected = "<a />";
		assertFormat(content, expected, //
				te(0, 2, 0, 2, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void selfClosingTag() throws BadLocationException {
		String content = "<a></a>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void singleEndTag() throws BadLocationException {
		String content = "</a>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void invalidEndTag() throws BadLocationException {
		String content = "</";
		String expected = content;
		assertFormat(content, expected);

		content = "</a";
		expected = content;
		assertFormat(content, expected);

		content = "<a></";
		expected = "<a>" + //
				System.lineSeparator() + //
				"</";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, System.lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void invalidEndTagInsideRoot() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    </\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void endTagMissing() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <bar>\r\n" + //
				"    <toto></toto>\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"  <bar>\r\n" + //
				"  <toto></toto>\r\n" + //
				"</foo>";
		assertFormat(content, expected, //
				te(1, 7, 2, 4, "\r\n  "));
		assertFormat(expected, expected);
	}

	@Test
	public void testUnclosedEndTagBracketTrailingElement() throws BadLocationException {
		String content = "<root>\r\n" + //
				"         <a> content </a\r\n" + //
				"      <b></b>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <a> content </a\r\n" + //
				"  <b></b>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(0, 6, 1, 9, "\r\n  "), //
				te(1, 24, 2, 6, "\r\n  "));
		assertFormat(expected, expected);
	}

	@Test
	public void endTagWithSpace() throws BadLocationException {
		String content = "<a></a        >";
		String expected = "<a></a>";
		assertFormat(content, expected, //
				te(0, 6, 0, 14, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void endTagWithLineBreak() throws BadLocationException {
		String content = "<a></a  \n      >";
		String expected = "<a></a>";
		assertFormat(content, expected, //
				te(0, 6, 1, 6, ""));
		assertFormat(expected, expected);
	}

	// ---------- Tests for attributes formatting

	@Test
	public void attrWithEqualsSpace() throws BadLocationException {
		String content = "<div  class = \"foo\">\n" + //
				"<br/>\n" + //
				" </div>";
		String expected = "<div class=\"foo\">\n" + //
				"  <br />\n" + //
				"</div>";
		assertFormat(content, expected, //
				te(0, 4, 0, 6, " "), //
				te(0, 11, 0, 12, ""), //
				te(0, 13, 0, 14, ""), //
				te(0, 20, 1, 0, "\n  "), //
				te(1, 3, 1, 3, " "), //
				te(1, 5, 2, 1, "\n"));
		assertFormat(expected, expected);
	}

	@Test
	public void attrValueWithLineBreakSpace() throws BadLocationException {
		String content = "<div  class = \n" + //
				"\"foo\">\n" + //
				"<br/>\n" + //
				" </div>";
		String expected = "<div class=\"foo\">\n" + //
				"  <br />\n" + //
				"</div>";
		assertFormat(content, expected, //
				te(0, 4, 0, 6, " "), //
				te(0, 11, 0, 12, ""), //
				te(0, 13, 1, 0, ""), //
				te(1, 6, 2, 0, "\n  "), //
				te(2, 3, 2, 3, " "), //
				te(2, 5, 3, 1, "\n"));
		assertFormat(expected, expected);
	}

	@Test
	public void testInvalidAttr() throws BadLocationException {
		String content = "<asdf \"\"`=asdf />";
		String expected = "<asdf \"\" `= asdf />";
		assertFormat(content, expected, //
				te(0, 8, 0, 8, " "), //
				te(0, 10, 0, 10, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueTwoLines() throws BadLocationException {
		String content = "<xml>\r\n" + //
				"  <a \r\n" + //
				"   |a             =         \"aa\"|>\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		String expected = "<xml>\r\n" + //
				"  <a a=\"aa\">\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 3, " "), //
				te(2, 4, 2, 17, ""), //
				te(2, 18, 2, 27, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLines() throws BadLocationException {
		String content = "<xml>\r\n" + //
				"  <a \r\n" + //
				"  |a\r\n" + //
				"  =\r\n" + //
				"  \"aa\"\r\n" + //
				"  \r\n" + //
				"  >|\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		String expected = "<xml>\r\n" + //
				"  <a a=\"aa\">\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 2, " "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLinesWithChild() throws BadLocationException {
		String content = "<xml>\r\n" + //
				"  <a \r\n" + //
				"   |a          =        \r\n" + //
				"   \r\n" + //
				"   \"aa\">|<b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		String expected = "<xml>\r\n" + //
				"  <a a=\"aa\">\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 3, " "), //
				te(2, 4, 2, 14, ""), //
				te(2, 15, 4, 3, ""), //
				te(4, 8, 4, 8, "\r\n    "));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLinesWithChildrenSiblings() throws BadLocationException {
		String content = "<xml>\r\n" + //
				"  <a \r\n" + //
				"  |a\r\n" + //
				"  =\r\n" + //
				"  \"aa\"\r\n" + //
				"  \r\n" + //
				"  >\r\n" + //
				"        <b>\r\n" + //
				"          <c></c>\r\n" + //
				"    </b>\r\n" + //
				"  </a>\r\n" + //
				"        <d></d>|\r\n" + //
				"</xml>";
		String expected = "<xml>\r\n" + //
				"  <a a=\"aa\">\r\n" + //
				"    <b>\r\n" + //
				"      <c></c>\r\n" + //
				"    </b>\r\n" + //
				"  </a>\r\n" + //
				"  <d></d>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 2, " "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, ""), //
				te(6, 3, 7, 8, "\r\n    "), //
				te(7, 11, 8, 10, "\r\n      "), //
				te(10, 6, 11, 8, "\r\n  "));
		assertFormat(expected, expected);
	}

	// ---------- Tests for processing instruction formatting

	@Test
	public void testProlog() throws BadLocationException {
		String content = "<?xml version=   \"1.0\"       encoding=\"UTF-8\"  ?>\r\n";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		assertFormat(content, expected, //
				te(0, 14, 0, 17, ""), //
				te(0, 22, 0, 29, " "), //
				te(0, 45, 0, 47, ""), //
				te(0, 49, 1, 0, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testProlog2() throws BadLocationException {
		String content = "<?xml version=   \"1.0\"       encoding=\"UTF-8\"  ?><a>bb</a>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<a>bb</a>";
		assertFormat(content, expected, //
				te(0, 14, 0, 17, ""), //
				te(0, 22, 0, 29, " "), //
				te(0, 45, 0, 47, ""), //
				te(0, 49, 0, 49, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testProlog3() throws BadLocationException {
		String content = "<?xml version=   \"1.0\"       encoding=\"UTF-8\"  ?><a><b>c</b></a>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<a>" + lineSeparator() + //
				"  <b>c</b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 14, 0, 17, ""), //
				te(0, 22, 0, 29, " "), //
				te(0, 45, 0, 47, ""), //
				te(0, 49, 0, 49, lineSeparator()), //
				te(0, 52, 0, 52, lineSeparator() + "  "), //
				te(0, 60, 0, 60, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testProlog4WithUnknownVariable() throws BadLocationException {
		String content = "<?xml version=   \"1.0\"       encoding=\"UTF-8\"  unknown=\"unknownValue\" ?><a><b>c</b></a>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" unknown=\"unknownValue\"?>" + lineSeparator() + //
				"<a>" + lineSeparator() + //
				"  <b>c</b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 14, 0, 17, ""), //
				te(0, 22, 0, 29, " "), //
				te(0, 45, 0, 47, " "), //
				te(0, 69, 0, 70, ""), //
				te(0, 72, 0, 72, lineSeparator()), //
				te(0, 75, 0, 75, lineSeparator() + "  "), //
				te(0, 83, 0, 83, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Disabled
	@Test
	public void testPI() throws BadLocationException {
		String content = "<a><?m2e asd as das das ?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?m2e asd as das das?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testPINoContent() throws BadLocationException {
		String content = "<a><?m2e?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?m2e ?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testDefinedPIWithVariables() throws BadLocationException {
		String content = "<a><?xml-stylesheet   href=\"my-style.css\"     type=   \"text/css\"?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-stylesheet href=\"my-style.css\" type=\"text/css\" ?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testDefinedPIWithJustAttributeNames() throws BadLocationException {
		String content = "<a><?xml-stylesheet    href     type  =       attName?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-stylesheet href type= attName ?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testPIWithVariables() throws BadLocationException {
		String content = "<a><?xml-styleZZ   href=\"my-style.css\"     type=   \"text/css\"?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-styleZZ href=\"my-style.css\"     type=   \"text/css\"?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	// ---------- Tests for comment formatting

	@Test
	public void testComment() throws BadLocationException {
		String content = "<!-- CommentText --><a>Val</a>";
		String expected = "<!-- CommentText -->" + lineSeparator() + //
				"<a>Val</a>";
		assertFormat(content, expected, //
				te(0, 20, 0, 20, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Disabled
	@Test
	public void testComment2() throws BadLocationException {
		String content = "<!-- CommentText --><!-- Comment2 --><a>Val</a>";
		String expected = "<!-- CommentText -->" + lineSeparator() + //
				"<!-- Comment2 -->" + lineSeparator() + //
				"<a>Val</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testCommentNested() throws BadLocationException {
		String content = "<a><!-- CommentText --></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <!-- CommentText -->" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testCommentNested2() throws BadLocationException {
		String content = "<a><!-- CommentText --><b><!-- Comment2 --></b></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <!-- CommentText -->" + lineSeparator() + //
				"  <b>" + lineSeparator() + //
				"    <!-- Comment2 -->" + lineSeparator() + //
				"  </b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testCommentMultiLineContent() throws BadLocationException {
		String content = "<a><!-- CommentText" + lineSeparator() + //
				"2222" + lineSeparator() + //
				"  3333 --></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <!-- CommentText" + lineSeparator() + //
				"2222" + lineSeparator() + //
				"  3333 -->" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Test
	public void testCommentNotClosed() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <!-- \r\n" + //
				"</foo>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testCommentWithRange() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <!-- |<bar>|\r\n" + //
				"  </bar>\r\n" + //
				"	-->\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"  <!-- <bar>\r\n" + //
				"  </bar>\r\n" + //
				"	-->\r\n" + //
				"</foo>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testJoinCommentLines() throws BadLocationException {
		String content = "<!--" + lineSeparator() + //
				" line 1" + lineSeparator() + //
				" " + lineSeparator() + //
				" " + lineSeparator() + //
				"   line 2" + lineSeparator() + //
				" -->";
		String expected = "<!-- line 1 line 2 -->";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCommentLines(true);
		assertFormat(content, expected, settings);
	}

	@Disabled
	@Test
	public void testUnclosedEndTagTrailingComment() throws BadLocationException {
		String content = "<root>" + lineSeparator() + //
				"    <a> content </a" + lineSeparator() + //
				"        <!-- comment -->" + lineSeparator() + //
				" </root>";
		String expected = "<root>" + lineSeparator() + //
				"  <a> content </a" + lineSeparator() + //
				"  <!-- comment -->" + lineSeparator() + //
				"</root>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCommentLines(true);
		assertFormat(content, expected, settings);
	}

	@Disabled
	@Test
	public void testJoinCommentLinesNested() throws BadLocationException {
		String content = "<a>" + lineSeparator() + //
				"  <!--" + lineSeparator() + //
				"   line 1" + lineSeparator() + //
				"   " + lineSeparator() + //
				"   " + lineSeparator() + //
				"     line 2" + lineSeparator() + //
				"   -->" + lineSeparator() + //
				"</a>";
		String expected = "<a>" + lineSeparator() + //
				"  <!-- line 1 line 2 -->" + lineSeparator() + //
				"</a>";

		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCommentLines(true);
		assertFormat(content, expected, settings);
	}

	@Disabled
	@Test
	public void testCommentFormatSameLine() throws BadLocationException {
		String content = "<a>" + lineSeparator() + //
				" Content" + lineSeparator() + //
				"</a> <!-- My   Comment   -->";
		String expected = "<a>" + lineSeparator() + //
				" Content" + lineSeparator() + //
				"</a> <!-- My Comment -->";

		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCommentLines(true);
		assertFormat(content, expected, settings);
	}

	// ---------- Tests for CDATA formatting

	@Test
	public void testCDATANotClosed() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <![CDATA[ \r\n" + //
				"</foo>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testCDATAWithRange() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <![CDATA[ |<bar>|\r\n" + //
				"  </bar>\r\n" + //
				"  ]]>\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"  <![CDATA[ <bar>\r\n" + //
				"  </bar>\r\n" + //
				"  ]]>\r\n" + //
				"</foo>";
		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testJoinCDATALines() throws BadLocationException {
		String content = "<a>" + lineSeparator() + //
				"<![CDATA[" + lineSeparator() + //
				"line 1" + lineSeparator() + //
				"" + lineSeparator() + //
				"" + lineSeparator() + //
				"line 2" + lineSeparator() + //
				"line 3" + lineSeparator() + //
				"]]> </a>";
		String expected = "<a>" + lineSeparator() + //
				"  <![CDATA[line 1 line 2 line 3]]>" + lineSeparator() + //
				"</a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		assertFormat(content, expected, settings);
	}

	// ---------- Tests for Text formatting

	@Disabled
	@Test
	public void testElementContentNotNormalized() throws BadLocationException {
		String content = "<a>\r" + //
				" Content\r" + //
				"     Content2\r" + //
				"      Content3\r" + //
				" Content4\r" + //
				"  Content5\r" + //
				"</a>";
		String expected = "<a>\r" + //
				" Content\r" + //
				"     Content2\r" + //
				"      Content3\r" + //
				" Content4\r" + //
				"  Content5\r" + //
				"</a>";

		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testContentFormatting2() throws BadLocationException {
		String content = "<a>\r" + //
				" Content\r" + //
				" <b>\r" + //
				"   Content2\r" + //
				"    Content3\r" + //
				" </b>\r" + //
				"</a>";
		String expected = "<a>\r" + //
				"  Content\r" + //
				"  <b>\r" + //
				"   Content2\r" + //
				"    Content3\r" + //
				" </b>\r" + //
				"</a>";

		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testContentFormattingDontMoveEndTag() throws BadLocationException {
		String content = "<a>\r" + //
				" Content\r" + //
				" <b>\r" + //
				"   Content2\r" + //
				"    Content3 </b>\r" + //
				"</a>";
		String expected = "<a>\r" + //
				"  Content\r" + //
				"  <b>\r" + //
				"   Content2\r" + //
				"    Content3 </b>\r" + //
				"</a>";

		assertFormat(content, expected);
	}

	@Test
	public void testContentFormatting3() throws BadLocationException {
		String content = "<a> content </a>";
		String expected = "<a> content </a>";

		assertFormat(content, expected);
	}

	@Disabled
	@Test
	public void testContentFormatting6() throws BadLocationException {
		String content = "<a>\r" + //
				"\r" + //
				" Content\r" + //
				"</a>";
		String expected = "<a>\r" + //
				"\r" + //
				" Content\r" + //
				"</a>";
		assertFormat(content, expected);

		content = "<a>\r\n" + //
				"\r\n" + //
				" Content\r\n" + //
				"</a>";
		expected = "<a>\r\n" + //
				"\r\n" + //
				" Content\r\n" + //
				"</a>";
		assertFormat(content, expected);
	}

	@Disabled
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
				"text\n" + //
				"    text text text\n" + //
				"    text\n" + //
				"</a>";
		assertFormat(content, expected, settings);
	}

	@Disabled
	@Test
	public void testTrimTrailingWhitespaceNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimTrailingWhitespace(true);
		String content = "<a>   \n" + //
				"   \n" + //
				"</a>   ";
		String expected = "<a></a>";
		assertFormat(content, expected, settings);
	}

	@Disabled
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
				"text\n" + //
				"    text text text\n" + //
				"\n" + //
				"    text\n" + //
				"\n" + //
				"</a>";
		assertFormat(content, expected, settings);
	}

	@Disabled
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
		assertFormat(content, expected, settings);
	}

	@Disabled
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
		assertFormat(content, expected, settings);
	}

	@Disabled
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
				"<h1></h1>" + "\r\n" + //
				" </div>";
		assertFormat(content, expected, settings);
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

	@Disabled
	@Test
	public void testDontTrimFinalNewLines2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a  ></a>\r\n" + //
				"   \r\n\r\n";
		String expected = "<a></a>\r\n" + //
				"   \r\n\r\n";
		assertFormat(content, expected, settings);
	}

	@Disabled
	@Test
	public void testDontTrimFinalNewLines3() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTrimFinalNewlines(false);
		String content = "<a  ></a>\r\n" + //
				"  text \r\n" + //
				"  more text   \r\n" + //
				"   \r\n";
		String expected = "<a></a>\r\n" + //
				"  text \r\n" + //
				"  more text   \r\n" + //
				"   \r\n";
		assertFormat(content, expected, settings);
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
		// Force to "experimental" formatter
		sharedSettings.getFormattingSettings().setExperimental(true);
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, uri, considerRangeFormat, expectedEdits);
	}

}
