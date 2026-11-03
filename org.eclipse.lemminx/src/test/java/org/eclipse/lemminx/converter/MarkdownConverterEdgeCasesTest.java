/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.converter;

import static org.eclipse.lemminx.converter.markdown.MarkdownConverter.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MarkdownConverterEdgeCasesTest {

	private static final String NL = System.lineSeparator();

	@Nested
	class EdgeCasesEmptyAndBoundary {

		@Test
		public void testNullLikeEmptyString() {
			assertEquals("", convert(""));
		}

		@Test
		public void testOnlyWhitespace() {
			assertEquals("", convert("   "));
		}

		@Test
		public void testOnlyNewlines() {
			assertEquals("", convert("\n\n\n"));
		}

		@Test
		public void testOnlyTags() {
			assertEquals("", convert("<p></p>"));
		}

		@Test
		public void testEmptyBoldTag() {
			assertEquals("****", convert("<b></b>"));
		}

		@Test
		public void testEmptyItalicTag() {
			assertEquals("**", convert("<i></i>"));
		}

		@Test
		public void testEmptyCodeTag() {
			assertEquals("``", convert("<code></code>"));
		}

		@Test
		public void testEmptyLinkTag() {
			assertEquals("[](http://x.com)", convert("<a href=\"http://x.com\"></a>"));
		}

		@Test
		public void testEmptyPreBlock() {
			assertEquals("```" + NL + "```", convert("<pre></pre>"));
		}

		@Test
		public void testEmptyListItem() {
			assertEquals(" *  ", convert("<ul><li></li></ul>"));
		}

		@Test
		public void testEmptyOrderedListItem() {
			assertEquals("1. ", convert("<ol><li></li></ol>"));
		}

		@Test
		public void testEmptyTableCell() {
			String expected = "|  |" + NL +
					"| --- |" + NL +
					"|  |";
			assertEquals(expected, convert("<table><tr><td></td></tr></table>"));
		}

		@Test
		public void testSingleCharacterContent() {
			assertEquals("x", convert("<p>x</p>"));
		}

		@Test
		public void testOnlyEntity() {
			assertEquals("<", convert("&lt;"));
		}

		@Test
		public void testOnlyBrTag() {
			assertEquals("", convert("<br>"));
		}

		@Test
		public void testOnlyHrTag() {
			assertEquals("---", convert("<hr>"));
		}
	}

	@Nested
	class EdgeCasesEntities {

		@Test
		public void testEntityAtStartOfString() {
			assertEquals("<hello", convert("&lt;hello"));
		}

		@Test
		public void testEntityAtEndOfString() {
			assertEquals("hello>", convert("hello&gt;"));
		}

		@Test
		public void testConsecutiveEntities() {
			assertEquals("<>&\"'", convert("&lt;&gt;&amp;&quot;&apos;"));
		}

		@Test
		public void testAmpersandWithoutSemicolon() {
			assertEquals("a & b", convert("a & b"));
		}

		@Test
		public void testAmpersandAtEndOfString() {
			assertEquals("trailing &", convert("trailing &"));
		}

		@Test
		public void testMalformedNumericEntity() {
			assertEquals("&#abc;", convert("&#abc;"));
		}

		@Test
		public void testMalformedHexEntity() {
			assertEquals("&#xZZ;", convert("&#xZZ;"));
		}

		@Test
		public void testEmptyEntity() {
			assertEquals("&;", convert("&;"));
		}

		@Test
		public void testEntityTooLong() {
			assertEquals("&verylongentity;", convert("&verylongentity;"));
		}

		@Test
		public void testEntityExactly10Chars() {
			assertEquals("&abcdefghi;", convert("&abcdefghi;"));
		}

		@Test
		public void testNumericEntityZero() {
			assertEquals("", convert("&#0;"));
		}

		@Test
		public void testEntityInAttributeValue() {
			assertEquals("[a&b](http://x.com?a=1&b=2)",
					convert("<a href=\"http://x.com?a=1&amp;b=2\">a&amp;b</a>"));
		}

		@Test
		public void testDoubleAmpersand() {
			assertEquals("&&", convert("&amp;&amp;"));
		}

		@Test
		public void testEntityInsideCode() {
			assertEquals("`<div>`", convert("<code>&lt;div&gt;</code>"));
		}
	}

	@Nested
	class EdgeCasesUnclosedTags {

		@Test
		public void testUnclosedBoldTag() {
			assertEquals("**text", convert("<b>text"));
		}

		@Test
		public void testUnclosedItalicTag() {
			assertEquals("*text", convert("<i>text"));
		}

		@Test
		public void testUnclosedCodeTag() {
			assertEquals("`text", convert("<code>text"));
		}

		@Test
		public void testUnclosedLinkTag() {
			assertEquals("[text", convert("<a href=\"http://x.com\">text"));
		}

		@Test
		public void testUnclosedListItem() {
			assertEquals(" *  text", convert("<ul><li>text"));
		}

		@Test
		public void testUnclosedPreTag() {
			assertEquals("```" + NL + "code here", convert("<pre>code here"));
		}

		@Test
		public void testUnclosedBlockquote() {
			assertEquals("> text", convert("<blockquote>text"));
		}

		@Test
		public void testUnclosedDelTag() {
			assertEquals("~~text", convert("<del>text"));
		}

		@Test
		public void testExtraClosingTag() {
			assertEquals("text**", convert("text</b>"));
		}

		@Test
		public void testMismatchedTags() {
			assertEquals("**text*", convert("<b>text</i>"));
		}
	}

	@Nested
	class EdgeCasesNormalizeFileUri {

		@Test
		public void testFileUriAlreadyNormalized() {
			assertEquals("[f](file:/C:/path/file.xsd)",
					convert("<a href=\"file:/C:/path/file.xsd\">f</a>"));
		}

		@Test
		public void testFileUriTwoSlashes() {
			assertEquals("[f](file://server/share)",
					convert("<a href=\"file://server/share\">f</a>"));
		}

		@Test
		public void testNonFileUri() {
			assertEquals("[f](https://example.com)",
					convert("<a href=\"https://example.com\">f</a>"));
		}

		@Test
		public void testEmptyHref() {
			assertEquals("[text]()", convert("<a href=\"\">text</a>"));
		}

		@Test
		public void testHrefWithSpaces() {
			assertEquals("[f](<http://example.com/my path>)",
					convert("<a href=\"http://example.com/my path\">f</a>"));
		}
	}

	@Nested
	class EdgeCasesTagInteractions {

		@Test
		public void testBoldInsideListItem() {
			String html = "<ul><li><b>Important:</b> note</li></ul>";
			assertEquals(" *  **Important:** note", convert(html));
		}

		@Test
		public void testLinkInsideOrderedListItem() {
			String html = "<ol><li><a href=\"http://x.com\">link</a> here</li></ol>";
			assertEquals("1. [link](http://x.com) here", convert(html));
		}

		@Test
		public void testCodeInsideHeader() {
			assertEquals("## `config` option ##", convert("<h2><code>config</code> option</h2>"));
		}

		@Test
		public void testStrikethroughInsideHeader() {
			assertEquals("## ~~old~~ new ##", convert("<h2><del>old</del> new</h2>"));
		}

		@Test
		public void testImageInsideListItem() {
			String html = "<ul><li>![icon](i.png) text</li></ul>";
			assertEquals(" *  ![icon](i.png) text", convert(html));
		}

		@Test
		public void testPreAfterOrderedList() {
			String html = "<ol><li>First</li></ol><pre>code</pre>";
			String expected = "1. First" + NL + NL +
					"```" + NL + "code" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableAfterOrderedList() {
			String html = "<ol><li>Done</li></ol>" +
					"<table><tr><td>A</td><td>B</td></tr></table>";
			String expected = "1. Done" + NL + NL +
					"|  |  |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMultipleConsecutiveHr() {
			String html = "<hr><hr><hr>";
			assertEquals("---" + NL + NL + "---" + NL + NL + "---", convert(html));
		}

		@Test
		public void testDefinitionListInsideBlockquote() {
			String html = "<blockquote><dl><dt>Term</dt><dd>Def</dd></dl></blockquote>";
			assertEquals(">" + NL + NL + "**Term**" + NL + "Def", convert(html));
		}

		@Test
		public void testOrderedListThenUnorderedList() {
			String html = "<ol><li>A</li><li>B</li></ol>" +
					"<ul><li>X</li><li>Y</li></ul>";
			String expected = "1. A" + NL +
					"2. B" + NL + NL +
					" *  X" + NL +
					" *  Y";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBrInsideBlockquote() {
			assertEquals("> line1" + NL + "line2",
					convert("<blockquote>line1<br>line2</blockquote>"));
		}

		@Test
		public void testPreFollowedByPre() {
			String html = "<pre>block1</pre><pre>block2</pre>";
			String expected = "```" + NL + "block1" + NL + "```" + NL +
					"```" + NL + "block2" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testEmptyParagraphBetweenContent() {
			String html = "before<p></p>after";
			assertEquals("before" + NL + NL + "after", convert(html));
		}

		@Test
		public void testLinkWithBoldAndItalic() {
			assertEquals("[***styled***](http://x.com)",
					convert("<a href=\"http://x.com\"><b><i>styled</i></b></a>"));
		}

		@Test
		public void testHeaderFollowedByHeader() {
			String html = "<h1>Title</h1><h2>Subtitle</h2>";
			String expected = "# Title #" + NL + NL + "## Subtitle ##";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreWithEntities() {
			String html = "<pre>if (a &lt; b) {}</pre>";
			String expected = "```" + NL + "if (a < b) {}" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testOrderedListSingleItem() {
			assertEquals("1. Only item", convert("<ol><li>Only item</li></ol>"));
		}

		@Test
		public void testUlFollowedByOlFollowedByUl() {
			String html = "<ul><li>A</li></ul><ol><li>B</li></ol><ul><li>C</li></ul>";
			String expected = " *  A" + NL + NL +
					"1. B" + NL + NL +
					" *  C";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testNestedBoldTags() {
			assertEquals("****nested****", convert("<b><b>nested</b></b>"));
		}

		@Test
		public void testOverlappingTags() {
			assertEquals("**text*", convert("<b>text</i>"));
		}
	}

	@Nested
	class EdgeCasesWhitespace {

		@Test
		public void testTabsInContent() {
			assertEquals("a b", convert("<p>a\tb</p>"));
		}

		@Test
		public void testCrLfInContent() {
			assertEquals("a b", convert("<p>a\r\nb</p>"));
		}

		@Test
		public void testWhitespaceOnlyContent() {
			assertEquals("", convert("<p>  \t\n  </p>"));
		}

		@Test
		public void testWhitespaceBetweenTags() {
			assertEquals("**A** **B**", convert("<b>A</b> <b>B</b>"));
		}

		@Test
		public void testNewlineBetweenTags() {
			assertEquals("**A**" + NL + "**B**", convert("<b>A</b>\n<b>B</b>"));
		}

		@Test
		public void testMultipleSpacesBetweenWords() {
			assertEquals("hello world", convert("<p>hello     world</p>"));
		}

		@Test
		public void testLeadingSpaceInParagraph() {
			assertEquals("text", convert("<p>   text</p>"));
		}

		@Test
		public void testTrailingSpaceInParagraph() {
			assertEquals("text", convert("<p>text   </p>"));
		}

		@Test
		public void testContentBetweenParagraphs() {
			assertEquals("A" + NL + NL + "between" + NL + NL + "B",
					convert("<p>A</p>between<p>B</p>"));
		}

		@Test
		public void testPrePreservesTabsAndSpaces() {
			String html = "<pre>\ttab\t\tmore\n  space</pre>";
			assertEquals("```" + NL + "\ttab\t\tmore\n  space" + NL + "```",
					convert(html));
		}
	}

	@Nested
	class EdgeCasesConvertGateway {

		@Test
		public void testTagInsideBackticksNotConverted() {
			assertEquals("The `<element>` tag", convert("The `<element>` tag"));
		}

		@Test
		public void testTagOutsideBackticksConverted() {
			assertEquals("The `code` and **bold**",
					convert("The `code` and <b>bold</b>"));
		}

		@Test
		public void testBackticksWithNoTags() {
			assertEquals("just `code` here", convert("just `code` here"));
		}

		@Test
		public void testNoBackticksNoTags() {
			assertEquals("plain text", convert("plain text"));
		}

		@Test
		public void testEntitiesUnescapedWithoutTags() {
			assertEquals("a < b & c > d", convert("a &lt; b &amp; c &gt; d"));
		}

		@Test
		public void testEntitiesInsideBackticks() {
			assertEquals("The `<project>` element",
					convert("The `&lt;project&gt;` element"));
		}

		@Test
		public void testAngleBracketsNotTags() {
			assertEquals("a < b > c", convert("a &lt; b &gt; c"));
		}
	}

	@Nested
	class EdgeCasesUnquote {

		@Test
		public void testUnquotedAttribute() {
			assertEquals("[text](http://x.com)",
					convert("<a href=http://x.com>text</a>"));
		}

		@Test
		public void testUnquotedImgSrc() {
			assertEquals("![](logo.png)",
					convert("<img src=logo.png>"));
		}

		@Test
		public void testUnquotedWithOtherAttributes() {
			assertEquals("[text](http://x.com)",
					convert("<a href=http://x.com title=\"link\">text</a>"));
		}

		@Test
		public void testSingleQuotedAttribute() {
			assertEquals("[text](http://x.com)",
					convert("<a href='http://x.com'>text</a>"));
		}

		@Test
		public void testDoubleQuotedAttribute() {
			assertEquals("[text](http://x.com)",
					convert("<a href=\"http://x.com\">text</a>"));
		}

		@Test
		public void testEmptyQuotedAttribute() {
			assertEquals("[text]()", convert("<a href=\"\">text</a>"));
		}
	}

	@Nested
	class EdgeCasesImages {

		@Test
		public void testImageAttributeOrderReversed() {
			assertEquals("![alt text](img.png)",
					convert("<img alt=\"alt text\" src=\"img.png\" />"));
		}

		@Test
		public void testImageWithUnknownAttributes() {
			assertEquals("![logo](logo.png)",
					convert("<img src=\"logo.png\" alt=\"logo\" width=\"100\" height=\"50\" />"));
		}

		@Test
		public void testImageWithEntitiesInAlt() {
			assertEquals("![A & B](img.png)",
					convert("<img src=\"img.png\" alt=\"A &amp; B\" />"));
		}

		@Test
		public void testImageWithFileUri() {
			assertEquals("![](file:///C:/img.png)",
					convert("<img src=\"file:///C:/img.png\" />"));
		}
	}

	@Nested
	class EdgeCasesSourceLink {

		@Test
		public void testSourceLinkPatternXsd() {
			String html = "Documentation text" +
					"<p>Source: <a href=\"file:///C:/schema.xsd\">schema.xsd</a></p>";
			String expected = "Documentation text" + NL + NL +
					"Source: [schema.xsd](file:/C:/schema.xsd)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testSourceLinkPatternDtd() {
			String html = "DTD doc text" +
					"<p>Source: <a href=\"file:///C:/grammar.dtd\">grammar.dtd</a></p>";
			String expected = "DTD doc text" + NL + NL +
					"Source: [grammar.dtd](file:/C:/grammar.dtd)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testSourceLinkPatternHttp() {
			String html = "Remote schema" +
					"<p>Source: <a href=\"https://example.com/schema.xsd\">schema.xsd</a></p>";
			String expected = "Remote schema" + NL + NL +
					"Source: [schema.xsd](https://example.com/schema.xsd)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPlainTextFollowedByHtmlLink() {
			String html = "Simple description without HTML" +
					"<p>Source: <a href=\"file:///C:/test.xsd\">test.xsd</a></p>";
			String expected = "Simple description without HTML" + NL + NL +
					"Source: [test.xsd](file:/C:/test.xsd)";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class EdgeCasesMultipleElements {

		@Test
		public void testMultipleImagesInParagraph() {
			String html = "<p>![a](1.png) ![b](2.png)</p>";
			assertEquals("![a](1.png) ![b](2.png)", convert(html));
		}

		@Test
		public void testMultipleLinksInRow() {
			String html = "<a href=\"a\">A</a><a href=\"b\">B</a><a href=\"c\">C</a>";
			assertEquals("[A](a)[B](b)[C](c)", convert(html));
		}

		@Test
		public void testMultipleBoldSpansInRow() {
			assertEquals("**A** **B** **C**",
					convert("<b>A</b> <b>B</b> <b>C</b>"));
		}

		@Test
		public void testMultipleCodeSpansInRow() {
			assertEquals("`A` `B` `C`",
					convert("<code>A</code> <code>B</code> <code>C</code>"));
		}

		@Test
		public void testMultipleParagraphsWithSingleContent() {
			assertEquals("A" + NL + NL + "B" + NL + NL + "C",
					convert("<p>A</p><p>B</p><p>C</p>"));
		}

		@Test
		public void testLongUnorderedList() {
			String html = "<ul>" +
					"<li>A</li><li>B</li><li>C</li><li>D</li><li>E</li>" +
					"<li>F</li><li>G</li><li>H</li><li>I</li><li>J</li>" +
					"</ul>";
			String expected = " *  A" + NL + " *  B" + NL + " *  C" + NL +
					" *  D" + NL + " *  E" + NL + " *  F" + NL +
					" *  G" + NL + " *  H" + NL + " *  I" + NL + " *  J";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testLongOrderedList() {
			String html = "<ol>" +
					"<li>A</li><li>B</li><li>C</li><li>D</li><li>E</li>" +
					"<li>F</li><li>G</li><li>H</li><li>I</li><li>J</li>" +
					"</ol>";
			String expected = "1. A" + NL + "2. B" + NL + "3. C" + NL +
					"4. D" + NL + "5. E" + NL + "6. F" + NL +
					"7. G" + NL + "8. H" + NL + "9. I" + NL + "10. J";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class EdgeCasesSpecialContent {

		@Test
		public void testContentWithAmpersandInText() {
			assertEquals("AT&T", convert("<p>AT&amp;T</p>"));
		}

		@Test
		public void testContentWithLtGtInCode() {
			assertEquals("`Map<String, List<Integer>>`",
					convert("<code>Map&lt;String, List&lt;Integer&gt;&gt;</code>"));
		}

		@Test
		public void testContentWithQuotesInAttribute() {
			assertEquals("[link](http://x.com?a=\"1\")",
					convert("<a href=\"http://x.com?a=&quot;1&quot;\">link</a>"));
		}

		@Test
		public void testContentWithNbspBetweenWords() {
			String result = convert("<p>word1&nbsp;word2</p>");
			assertEquals("word1\u00A0word2", result);
		}

		@Test
		public void testContentWithUnicodeCharacters() {
			assertEquals("caf\u00E9 na\u00EFve",
					convert("<p>caf&#xe9; na&#xef;ve</p>"));
		}

		@Test
		public void testPreBlockWithHtmlEntities() {
			String html = "<pre>if (x &amp;&amp; y) {\n  return &lt;result&gt;;\n}</pre>";
			String expected = "```" + NL +
					"if (x && y) {\n  return <result>;\n}" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMarkdownSpecialCharsInText() {
			assertEquals("2 * 3 = 6 and a_b_c",
					convert("<p>2 * 3 = 6 and a_b_c</p>"));
		}

		@Test
		public void testPipeInTableCell() {
			String html = "<table><tr><td>a|b</td><td>c</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| a\\|b | c |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBackticksInContent() {
			assertEquals("Use `` ` `` for code",
					convert("Use `` ` `` for code"));
		}
	}

	@Nested
	class FencedCodeBlocks {

		// Fenced code blocks (``` or ~~~) containing XML-like tags should be
		// preserved as-is. The scanner skips past fenced content to avoid
		// tokenizing <key>, <element/>, etc. as HTML.

		@Test
		public void testFencedCodeWithXmlTags() {
			// Ex: XSD documentation with XML example in a fenced block
			String input = "```xml\n<key>value</key>\n```";
			assertEquals(input, convert(input));
		}

		@Test
		public void testFencedCodeWithMultipleXmlTags() {
			// Ex: multi-line XML snippet with attributes and nesting
			String input = "```xml\n<root>\n  <child attr=\"v\"/>\n</root>\n```";
			assertEquals(input, convert(input));
		}

		@Test
		public void testFencedCodeWithTildeFence() {
			// Ex: ~~~ fence (alternative to ```)
			String input = "~~~xml\n<key>value</key>\n~~~";
			assertEquals(input, convert(input));
		}

		@Test
		public void testFencedCodePreservedWithSurroundingText() {
			// Ex: prose before and after a fenced block.
			// "Before\n" goes through appendMarkdownContent → \n becomes LINE_SEP.
			// Fenced block content is emitted as raw HTML (newlines preserved).
			String expected = "Before" + NL + "```\n<tag/>\n```\nAfter";
			assertEquals(expected, convert("Before\n```\n<tag/>\n```\nAfter"));
		}

		@Test
		public void testFencedCodeNoAngleBracketsUnchanged() {
			// Ex: fenced block without any < — fast path, no scanning needed
			String input = "```\nplain code\n```";
			assertEquals(input, convert(input));
		}

		@Test
		public void testTextWithBackticksButNoFence() {
			// Ex: backticks that don't form a fence marker (less than 3)
			assertEquals("Use `code` here", convert("Use `code` here"));
		}

		@Test
		public void testFencedCodeMixedWithHtml() {
			// Ex: HTML tags outside the fence are converted, tags inside preserved
			String input = "<b>bold</b>\n```\n<element/>\n```";
			String expected = "**bold**" + NL + "```\n<element/>\n```";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testMultipleFencedBlocks() {
			// Ex: two fenced blocks separated by plain text.
			// "text\n" between blocks goes through appendMarkdownContent → LINE_SEP.
			// Fenced block content is emitted as raw HTML.
			String input = "```\n<a/>\n```\ntext\n```\n<b/>\n```";
			String expected = "```\n<a/>\n```\n" + "text" + NL + "```\n<b/>\n```";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testMismatchedFenceCharsNotClosed() {
			// Ex: ~~~ inside a ``` fence doesn't close it — only matching chars close
			String input = "```\n<tag/>\n~~~\n<other/>\n```";
			assertEquals(input, convert(input));
		}
	}
}
