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
package org.eclipse.lemminx.services.format;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests
 *
 */
public class XMLFormatterTest extends AbstractCacheBasedTest {

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
	}

	@Test
	public void singleEndTag() throws BadLocationException {
		String content = "</a>";
		String expected = content;
		assertFormat(content, expected);
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

	@Test
	public void invalidAttribute() throws BadLocationException {
		String content = "<a bb=\"asd\" \"aaa\"> </a>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void invalidQuoteInTag() throws BadLocationException {
		String content = "<a'> </a>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void invalidEqualsInTag() throws BadLocationException {
		String content = "<a=";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void invalidProcessingInstruction() throws BadLocationException {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?\r\n" + //
				"			<a />";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void invalidTagName() throws BadLocationException {
		String content = "<message>\r\n" + //
				"	  <table>\r\n" + //
				"	    <tbody>\r\n" + //
				"	      <%= for el <- @elements do %>\r\n" + //
				"	        <tr>\r\n" + //
				"	            <td><%= el.id %></td>\r\n" + //
				"	        </tr>\r\n" + //
				"	      <% end %>\r\n" + //
				"	    </tbody>\r\n" + //
				"	  </table>\r\n" + //
				"	</message>";
		String expected = "<message>\r\n" + //
				"  <table>\r\n" + //
				"    <tbody>\r\n" + //
				"      <%= for el\r\n" + //
				"      <- @elements do %>\r\n" + //
				"      <tr>\r\n" + //
				"        <td>\r\n" + //
				"          <%= el.id %>\r\n" + //
				"        </td>\r\n" + //
				"      </tr>\r\n" + //
				"      <% end %>\r\n" + //
				"    </tbody>\r\n" + //
				"  </table>\r\n" + //
				"</message>";
		assertFormat(content, expected, //
				te(0, 9, 1, 3, "\r\n  "), //
				te(1, 10, 2, 5, "\r\n    "), //
				te(2, 12, 3, 7, "\r\n      "), //
				te(3, 17, 3, 18, "\r\n      "), //
				te(3, 36, 4, 9, "\r\n      "), //
				te(4, 13, 5, 13, "\r\n        "), //
				te(5, 17, 5, 17, "\r\n          "), //
				te(5, 29, 5, 29, "\r\n        "), //
				te(5, 34, 6, 9, "\r\n      "), //
				te(6, 14, 7, 7, "\r\n      "), //
				te(7, 16, 8, 5, "\r\n    "), //
				te(8, 13, 9, 3, "\r\n  "), //
				te(9, 11, 10, 1, "\r\n"));
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
				"  <a\r\n" + //
				"    a=\"aa\">\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 3, "\r\n    "), //
				te(2, 4, 2, 17, ""), //
				te(2, 18, 2, 27, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueTwoLinesWithoutPreserveAttrLineBreak() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
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
		assertFormat(content, expected, settings, //
				te(1, 4, 2, 3, " "), //
				te(2, 4, 2, 17, ""), //
				te(2, 18, 2, 27, ""));
		assertFormat(expected, expected, settings);
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
				"  <a\r\n" + //
				"    a=\"aa\"\r\n" + //
				"  >\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 2, "\r\n    "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, "\r\n  "));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLinesWithoutPreserveAttrLineBreak() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
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
		assertFormat(content, expected, settings, //
				te(1, 4, 2, 2, " "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, ""));
		assertFormat(expected, expected, settings);
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
				"  <a\r\n" + //
				"    a=\"aa\">\r\n" + //
				"    <b></b>\r\n" + //
				"  </a>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 3, "\r\n    "), //
				te(2, 4, 2, 14, ""), //
				te(2, 15, 4, 3, ""), //
				te(4, 8, 4, 8, "\r\n    "));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLinesWithChildWithoutPreserveAttrLineBreak() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
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
		assertFormat(content, expected, settings, //
				te(1, 4, 2, 3, " "), //
				te(2, 4, 2, 14, ""), //
				te(2, 15, 4, 3, ""), //
				te(4, 8, 4, 8, "\r\n    "));
		assertFormat(expected, expected, settings);
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
				"  <a\r\n" + //
				"    a=\"aa\"\r\n" + //
				"  >\r\n" + //
				"    <b>\r\n" + //
				"      <c></c>\r\n" + //
				"    </b>\r\n" + //
				"  </a>\r\n" + //
				"  <d></d>\r\n" + //
				"</xml>";
		assertFormat(content, expected, //
				te(1, 4, 2, 2, "\r\n    "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, "\r\n  "), //
				te(6, 3, 7, 8, "\r\n    "), //
				te(7, 11, 8, 10, "\r\n      "), //
				te(10, 6, 11, 8, "\r\n  "));
		assertFormat(expected, expected);
	}

	@Test
	public void testAttributeNameValueMultipleLinesWithChildrenSiblingsWithoutPreserveAttrLineBreak()
			throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(false);
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
		assertFormat(content, expected, settings, //
				te(1, 4, 2, 2, " "), //
				te(2, 3, 3, 2, ""), //
				te(3, 3, 4, 2, ""), //
				te(4, 6, 6, 2, ""), //
				te(6, 3, 7, 8, "\r\n    "), //
				te(7, 11, 8, 10, "\r\n      "), //
				te(10, 6, 11, 8, "\r\n  "));
		assertFormat(expected, expected, settings);
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

	@Test
	public void testPI() throws BadLocationException {
		String content = "<a><?m2e asd as das das ?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?m2e asd as das das?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), //
				te(0, 23, 0, 24, ""), //
				te(0, 26, 0, 26, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testPINoContent() throws BadLocationException {
		String content = "<a><?m2e?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?m2e?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), //
				te(0, 10, 0, 10, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Disabled
	@Test
	public void testDefinedPIWithVariables() throws BadLocationException {
		String content = "<a><?xml-stylesheet   href=\"my-style.css\"     type=   \"text/css\"?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-stylesheet href=\"my-style.css\" type=\"text/css\"?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), //
				te(0, 19, 0, 22, " "), //
				te(0, 41, 0, 46, " "), //
				te(0, 51, 0, 54, ""), //
				te(0, 66, 0, 66, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Disabled
	@Test
	public void testDefinedPIWithJustAttributeNames() throws BadLocationException {
		String content = "<a><?xml-stylesheet    href     type  =       attName?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-stylesheet href type= attName?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), //
				te(0, 19, 0, 23, " "), //
				te(0, 27, 0, 32, " "), //
				te(0, 36, 0, 38, ""), //
				te(0, 39, 0, 46, " "), //
				te(0, 55, 0, 55, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Disabled
	@Test
	public void testPIWithVariables() throws BadLocationException {
		String content = "<a><?xml-styleZZ   href=\"my-style.css\"     type=   \"text/css\"?></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <?xml-styleZZ href=\"my-style.css\" type=\"text/css\"?>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected);
	}

	@Test
	public void testPIWithoutTarget() throws BadLocationException {
		String content = "<a><??></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <??></a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "));
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

	@Test
	public void testComment2() throws BadLocationException {
		String content = "<!-- CommentText --><!-- Comment2 --><a>Val</a>";
		String expected = "<!-- CommentText --><!-- Comment2 -->" + lineSeparator() + //
				"<a>Val</a>";
		assertFormat(content, expected, //
				te(0, 37, 0, 37, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testCommentNested() throws BadLocationException {
		String content = "<a><!-- CommentText --></a>";
		String expected = "<a><!-- CommentText -->" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 23, 0, 23, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testCommentNested2() throws BadLocationException {
		String content = "<a><!-- CommentText --><b><!-- Comment2 --></b></a>";
		String expected = "<a><!-- CommentText -->" + lineSeparator() + //
				"  <b><!-- Comment2 -->" + lineSeparator() + //
				"  </b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 23, 0, 23, lineSeparator() + "  "), //
				te(0, 43, 0, 43, lineSeparator() + "  "), //
				te(0, 47, 0, 47, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testCommentMultiLineContent() throws BadLocationException {
		String content = "<a><!-- CommentText" + lineSeparator() + //
				"2222" + lineSeparator() + //
				"  3333 --></a>";
		String expected = "<a><!-- CommentText" + lineSeparator() + //
				"2222" + lineSeparator() + //
				"  3333 -->" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(2, 10, 2, 10, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void testCommentLongWrap() throws BadLocationException {
		String content = "<a>\n" + //
				"  Content <!-- comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment -->"
				+ //
				"</a>";
		String expected = "<a> Content <!-- comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment --></a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 2, " "), //
				te(1, 78, 1, 79, "\n  "), //
				te(1, 150, 1, 151, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testCommentLongWrapJoinContentLines() throws BadLocationException {
		String content = "<a>\n" + //
				"  Content <!-- comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment -->"
				+ //
				"</a>";
		String expected = "<a> Content <!-- comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment --></a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 2, " "), //
				te(1, 78, 1, 79, "\n  "), //
				te(1, 150, 1, 151, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testCommentLongTextContentWrap() throws BadLocationException {
		String content = "<a> content content content content content content content content content content content content content content content content content content <!-- comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment -->\n"
				+ //
				"</a>";
		String expected = "<a> content content content content content content content content content\n" + //
				"  content content content content content content content content content <!--\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment -->\n" + //
				"</a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, //
				te(0, 75, 0, 76, "\n  "), //
				te(0, 152, 0, 153, "\n  "),
				te(0, 224, 0, 225, "\n  "), //
				te(0, 296, 0, 297, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testCommentLongTextContentWrapNewLine() throws BadLocationException {
		String content = "<a>\n" + //
				"  content content content content content content content content content content content content content content content content content content <!-- comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment comment -->\n"
				+ //
				"</a>";
		String expected = "<a> content content content content content content content content content\n" + //
				"  content content content content content content content content content <!--\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment comment comment\n" + //
				"  comment comment comment comment comment comment comment -->\n" + //
				"</a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 2, " "), //
				te(1, 73, 1, 74, "\n  "), //
				te(1, 150, 1, 151, "\n  "),
				te(1, 222, 1, 223, "\n  "), //
				te(1, 294, 1, 295, "\n  "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testCommentNotClosed() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <!-- \r\n" + //
				"</foo>";
		String expected = content;
		assertFormat(content, expected);
	}

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

	// ---------- Tests for Text formatting

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
				"  Content\r" + //
				"  Content2\r" + //
				"  Content3\r" + //
				"  Content4\r" + //
				"  Content5\r" + //
				"</a>";

		assertFormat(content, expected, //
				te(0, 3, 1, 1, "\r  "), //
				te(1, 8, 2, 5, "\r  "), //
				te(2, 13, 3, 6, "\r  "),
				te(3, 14, 4, 1, "\r  "));
		assertFormat(expected, expected);
	}

	@Test
	public void testContentFormatting2() throws BadLocationException {
		String content = "<a>\r" + //
				" Content\r" + //
				" <b>\r" + //
				"   Content2\r" + //
				"    Content3\r" + //
				" </b>\r" + //
				"</a>";
		String expected = "<a> Content <b>\r" + //
				"    Content2\r" + //
				"    Content3\r" + //
				"  </b>\r" + //
				"</a>";

		assertFormat(content, expected, //
				te(0, 3, 1, 1, " "), //
				te(1, 8, 2, 1, " "), //
				te(2, 4, 3, 3, "\r    "),
				te(4, 12, 5, 1, "\r  "));
		assertFormat(expected, expected);
	}

	@Test
	public void testContentFormattingDontMoveEndTag() throws BadLocationException {
		String content = "<a>\r" + //
				" Content\r" + //
				" <b>\r" + //
				"   Content2\r" + //
				"    Content3     </b>\r" + //
				"</a>";
		String expected = "<a> Content <b>\r" + //
				"    Content2\r" + //
				"    Content3 </b>\r" + //
				"</a>";

		assertFormat(content, expected, //
				te(0, 3, 1, 1, " "), //
				te(1, 8, 2, 1, " "), //
				te(2, 4, 3, 3, "\r    "),
				te(4, 12, 4, 17, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void testContentFormatting3() throws BadLocationException {
		String content = "<a> content </a>";
		String expected = "<a> content </a>";

		assertFormat(content, expected);
	}

	@Test
	public void testContentFormatting6() throws BadLocationException {
		String content = "<a>\r" + //
				"\r" + //
				" Content\r" + //
				"</a>";
		String expected = "<a>\r" + //
				"\r" + //
				"  Content\r" + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 2, 1, "\r\r  "));
		assertFormat(expected, expected);

		content = "<a>\r\n" + //
				"\r\n" + //
				" Content\r\n" + //
				"</a>";
		expected = "<a>\r\n" + //
				"\r\n" + //
				"  Content\r\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 2, 1, "\r\n\r\n  "));
		assertFormat(expected, expected);
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
