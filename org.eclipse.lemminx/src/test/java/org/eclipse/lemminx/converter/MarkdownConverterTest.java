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

public class MarkdownConverterTest {

	private static final String NL = System.lineSeparator();

	@Nested
	class BasicConversion {

		@Test
		public void testHTMLConversion() {
			assertEquals("This is `my code`", convert("This is <code>my code</code>"));
			assertEquals("This is" + NL + "**bold**", convert("This is<br><b>bold</b>"));
			assertEquals("The `<project>` element is the root of the descriptor.",
					convert("The <code>&lt;project&gt;</code> element is the root of the descriptor."));
			assertEquals("# Hey Man #", convert("<h1>Hey Man</h1>"));
			assertEquals("[Placeholder](https://www.xml.com)",
					convert("<a href=\"https://www.xml.com\">Placeholder</a>"));

			String htmlList = "<ul>" + NL +
					"  <li>Coffee</li>" + NL +
					"  <li>Tea</li>" + NL +
					"  <li>Milk</li>" + NL +
					"</ul>";
			String expectedList = " *  Coffee" + NL +
					" *  Tea" + NL +
					" *  Milk";
			assertEquals(expectedList, convert(htmlList));
			assertEquals("ONLY_THIS_TEXT", convert("<p>ONLY_THIS_TEXT</p>"));

			String multilineHTML = "multi" + NL +
					"line" + NL +
					"<code>HTML</code>" + NL +
					"stuff";
			assertEquals("multi" + NL + "line" + NL + "`HTML`" + NL + "stuff", convert(multilineHTML));

			String multilineHTML2 = "<p>multi<p>" + NL +
					"line" + NL +
					"<code>HTML</code>" + NL +
					"stuff";
			String multilineHTML2Expected = "multi" + NL +
					NL +
					"line `HTML` stuff";
			assertEquals(multilineHTML2Expected, convert(multilineHTML2));
		}

		@Test
		public void testMarkdownPassthrough() {
			assertEquals("This is `my code`", convert("This is `my code`"));
			assertEquals("The `<thing>` element is the root of the descriptor.",
					convert("The `<thing>` element is the root of the descriptor."));
			assertEquals("The `<project>` element is the root of the descriptor.",
					convert("The `&lt;project&gt;` element is the root of the descriptor."));
		}

		@Test
		public void testEmptyContent() {
			assertEquals("", convert(""));
		}

		@Test
		public void testPlainTextPreserved() {
			assertEquals("plain text", convert("plain text"));
		}
	}

	@Nested
	class Links {

		@Test
		public void testSimpleLink() {
			assertEquals("[Click here](https://example.com)",
					convert("<a href=\"https://example.com\">Click here</a>"));
		}

		@Test
		public void testLinkWithSingleQuotedHref() {
			assertEquals("[Link](https://example.com)",
					convert("<a href='https://example.com'>Link</a>"));
		}

		@Test
		public void testLinkWithoutHref() {
			assertEquals("text", convert("<a>text</a>"));
		}

		@Test
		public void testLinkWithEntitiesInText() {
			assertEquals("[A & B <> C](https://example.com)",
					convert("<a href=\"https://example.com\">A &amp; B &lt;&gt; C</a>"));
		}

		@Test
		public void testLinkWithEntitiesInUrl() {
			assertEquals("[Search](https://example.com/search?q=a&b=c)",
					convert("<a href=\"https://example.com/search?q=a&amp;b=c\">Search</a>"));
		}

		@Test
		public void testMultipleLinks() {
			assertEquals("[A](http://a.com) and [B](http://b.com)",
					convert("<a href=\"http://a.com\">A</a> and <a href=\"http://b.com\">B</a>"));
		}

		@Test
		public void testLinkWithBoldText() {
			assertEquals("[**bold link**](http://example.com)",
					convert("<a href=\"http://example.com\"><b>bold link</b></a>"));
		}

		@Test
		public void testLinkWithCodeText() {
			assertEquals("[`code link`](http://example.com)",
					convert("<a href=\"http://example.com\"><code>code link</code></a>"));
		}

		@Test
		public void testLinkWithFileUri() {
			assertEquals("[file.xsd](file:/C:/path/to/file.xsd)",
					convert("<a href=\"file:///C:/path/to/file.xsd\">file.xsd</a>"));
		}

		@Test
		public void testLinkWithImage() {
			assertEquals("[![img](icon.png)](http://example.com)",
					convert("<a href=\"http://example.com\"><img src=\"icon.png\" alt=\"img\"/></a>"));
		}
	}

	@Nested
	class Images {

		@Test
		public void testSimpleImage() {
			assertEquals("![alt text](https://example.com/image.png)",
					convert("<img src=\"https://example.com/image.png\" alt=\"alt text\" />"));
		}

		@Test
		public void testImageSelfClosingNoSlash() {
			assertEquals("![logo](logo.png)",
					convert("<img src=\"logo.png\" alt=\"logo\">"));
		}

		@Test
		public void testImageWithoutAlt() {
			assertEquals("![](https://example.com/image.png)",
					convert("<img src=\"https://example.com/image.png\" />"));
		}

		@Test
		public void testImageWithoutSrc() {
			assertEquals("![alt text]()",
					convert("<img alt=\"alt text\" />"));
		}

		@Test
		public void testImageWithSingleQuotedAttributes() {
			assertEquals("![photo](photo.jpg)",
					convert("<img src='photo.jpg' alt='photo' />"));
		}

		@Test
		public void testImageInlineWithText() {
			assertEquals("See ![icon](icon.png) for details",
					convert("See <img src=\"icon.png\" alt=\"icon\" /> for details"));
		}

		@Test
		public void testMultipleImagesInRow() {
			assertEquals("![a](1.png) ![b](2.png) ![c](3.png)",
					convert("<img src=\"1.png\" alt=\"a\"/> <img src=\"2.png\" alt=\"b\"/> <img src=\"3.png\" alt=\"c\"/>"));
		}

		@Test
		public void testImageFollowedByLink() {
			assertEquals("![Logo](logo.png) [Home](http://home.com)",
					convert("<img src=\"logo.png\" alt=\"Logo\" /> <a href=\"http://home.com\">Home</a>"));
		}
	}

	@Nested
	class Formatting {

		@Test
		public void testBold() {
			assertEquals("**bold text**", convert("<b>bold text</b>"));
		}

		@Test
		public void testStrong() {
			assertEquals("**strong text**", convert("<strong>strong text</strong>"));
		}

		@Test
		public void testBoldInsideParagraph() {
			assertEquals("This is **important** text",
					convert("This is <b>important</b> text"));
		}

		@Test
		public void testItalic() {
			assertEquals("*italic text*", convert("<i>italic text</i>"));
		}

		@Test
		public void testEmphasis() {
			assertEquals("*emphasized text*", convert("<em>emphasized text</em>"));
		}

		@Test
		public void testItalicInsideParagraph() {
			assertEquals("This is *subtle* text",
					convert("This is <i>subtle</i> text"));
		}

		@Test
		public void testBoldInsideItalic() {
			assertEquals("***bold and italic***",
					convert("<i><b>bold and italic</b></i>"));
		}

		@Test
		public void testItalicInsideBold() {
			assertEquals("***italic and bold***",
					convert("<b><i>italic and bold</i></b>"));
		}

		@Test
		public void testCodeInsideBold() {
			assertEquals("**`code in bold`**",
					convert("<b><code>code in bold</code></b>"));
		}

		@Test
		public void testBoldAndItalicSeparate() {
			assertEquals("**bold** and *italic*",
					convert("<b>bold</b> and <i>italic</i>"));
		}

		@Test
		public void testDeeplyNestedFormatting() {
			assertEquals("***`deep`***",
					convert("<b><i><code>deep</code></i></b>"));
		}

		@Test
		public void testInlineCode() {
			assertEquals("`some code`", convert("<code>some code</code>"));
		}

		@Test
		public void testCodeWithEntities() {
			assertEquals("`List<String>`",
					convert("<code>List&lt;String&gt;</code>"));
		}

		@Test
		public void testMultipleCodeSpans() {
			assertEquals("Use `foo()` or `bar()`",
					convert("Use <code>foo()</code> or <code>bar()</code>"));
		}
	}

	@Nested
	class Headers {

		@Test
		public void testAllHeaderLevels() {
			assertEquals("# H1 #", convert("<h1>H1</h1>"));
			assertEquals("## H2 ##", convert("<h2>H2</h2>"));
			assertEquals("### H3 ###", convert("<h3>H3</h3>"));
			assertEquals("#### H4 ####", convert("<h4>H4</h4>"));
			assertEquals("##### H5 #####", convert("<h5>H5</h5>"));
			assertEquals("###### H6 ######", convert("<h6>H6</h6>"));
		}

		@Test
		public void testHeaderWithFormattedContent() {
			assertEquals("## **Bold Header** ##",
					convert("<h2><b>Bold Header</b></h2>"));
		}

		@Test
		public void testHeaderWithLink() {
			assertEquals("### [Link](http://example.com) ###",
					convert("<h3><a href=\"http://example.com\">Link</a></h3>"));
		}

		@Test
		public void testHeaderFollowedByList() {
			String html = "<h2>Features</h2>" +
					"<ul><li>Fast</li><li>Reliable</li></ul>";
			String expected = "## Features ##" + NL +
					" *  Fast" + NL +
					" *  Reliable";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class LineBreaks {

		@Test
		public void testBrTag() {
			assertEquals("Line 1" + NL + "Line 2",
					convert("Line 1<br>Line 2"));
		}

		@Test
		public void testBrSelfClosing() {
			assertEquals("Line 1" + NL + "Line 2",
					convert("Line 1<br/>Line 2"));
		}

		@Test
		public void testMultipleBr() {
			assertEquals("A" + NL + "B" + NL + "C",
					convert("A<br>B<br>C"));
		}

		@Test
		public void testBrOnly() {
			assertEquals("", convert("<br>"));
		}

		@Test
		public void testMultipleBrOnly() {
			assertEquals("", convert("<br><br><br>"));
		}

		@Test
		public void testBrBeforeContent() {
			assertEquals("text", convert("<br>text"));
		}

		@Test
		public void testBrAfterContent() {
			assertEquals("text", convert("text<br>"));
		}

		@Test
		public void testBrBetweenFormatting() {
			assertEquals("**bold**" + NL + "*italic*",
					convert("<b>bold</b><br><i>italic</i>"));
		}

		@Test
		public void testBrInListItem() {
			String html = "<ul><li>Line 1<br>Line 2</li></ul>";
			String expected = " *  Line 1" + NL + "Line 2";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBrInTableCell() {
			String html = "<table><tr><td>A<br>B</td><td>C</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| A B | C |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class Paragraphs {

		@Test
		public void testSingleParagraph() {
			assertEquals("Some text", convert("<p>Some text</p>"));
		}

		@Test
		public void testMultipleParagraphs() {
			assertEquals("First" + NL + NL + "Second",
					convert("<p>First</p><p>Second</p>"));
		}

		@Test
		public void testParagraphWithFormatting() {
			assertEquals("This is **bold** and *italic*",
					convert("<p>This is <b>bold</b> and <i>italic</i></p>"));
		}

		@Test
		public void testParagraphWithBreak() {
			assertEquals("Line 1" + NL + "Line 2" + NL + NL + "New para",
					convert("<p>Line 1<br>Line 2</p><p>New para</p>"));
		}

		@Test
		public void testUnclosedParagraphs() {
			assertEquals("First" + NL + NL + "Second",
					convert("<p>First<p>Second"));
		}
	}

	@Nested
	class Lists {

		@Test
		public void testUnorderedList() {
			String html = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>";
			String expected = " *  Item 1" + NL + " *  Item 2" + NL + " *  Item 3";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testListWithFormattedItems() {
			String html = "<ul><li><b>Bold</b> item</li><li><code>code</code> item</li></ul>";
			String expected = " *  **Bold** item" + NL + " *  `code` item";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testListWithLinks() {
			String html = "<ul><li><a href=\"http://a.com\">Link A</a></li><li><a href=\"http://b.com\">Link B</a></li></ul>";
			String expected = " *  [Link A](http://a.com)" + NL + " *  [Link B](http://b.com)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testListAfterParagraph() {
			String html = "<p>Items:</p><ul><li>A</li><li>B</li></ul>";
			String expected = "Items:" + NL + NL +
					" *  A" + NL +
					" *  B";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class Tables {

		@Test
		public void testSimpleTable() {
			String html = "<table><tr><th>Name</th><th>Value</th></tr><tr><td>foo</td><td>bar</td></tr></table>";
			String expected = "| Name | Value |" + NL +
					"| --- | --- |" + NL +
					"| foo | bar |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithMultipleRows() {
			String html = "<table>" +
					"<tr><th>A</th><th>B</th><th>C</th></tr>" +
					"<tr><td>1</td><td>2</td><td>3</td></tr>" +
					"<tr><td>4</td><td>5</td><td>6</td></tr>" +
					"</table>";
			String expected = "| A | B | C |" + NL +
					"| --- | --- | --- |" + NL +
					"| 1 | 2 | 3 |" + NL +
					"| 4 | 5 | 6 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithFormattedCells() {
			String html = "<table><tr><td><b>Bold</b></td><td><code>code</code></td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| **Bold** | `code` |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithLinks() {
			String html = "<table><tr><td><a href=\"http://example.com\">Link</a></td><td>Text</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| [Link](http://example.com) | Text |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testNestedTable() {
			String html = "<table>" +
					"<tr><td>Outer</td><td>" +
					"<table><tr><td>Inner1</td><td>Inner2</td></tr></table>" +
					"</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| Outer | Inner1 Inner2 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithNestedTableAndHeaders() {
			String html = "<table>" +
					"<tr><th>Category</th><th>Details</th></tr>" +
					"<tr><td>Info</td><td>" +
					"<table><tr><th>Key</th><th>Val</th></tr>" +
					"<tr><td>a</td><td>1</td></tr></table>" +
					"</td></tr></table>";
			String expected = "| Category | Details |" + NL +
					"| --- | --- |" + NL +
					"| Info | Key Val a 1 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithImplicitCellClose() {
			String html = "<table><tr><td>A<td>B</tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithImplicitRowClose() {
			String html = "<table><tr><td>A</td><td>B</td><tr><td>C</td><td>D</td></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| A | B |" + NL +
					"| C | D |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithUnclosedLastRow() {
			String html = "<table><tr><th>H1</th><th>H2</th></tr><tr><td>A<td>B</table>";
			String expected = "| H1 | H2 |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableFollowedByParagraph() {
			String html = "<table><tr><th>Attr</th><th>Type</th></tr>" +
					"<tr><td>name</td><td>string</td></tr></table>" +
					"<p>Description text here.</p>";
			String expected = "| Attr | Type |" + NL +
					"| --- | --- |" + NL +
					"| name | string |" + NL + NL +
					"Description text here.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testComplexTableWithAllFormatting() {
			String html = "<table>" +
					"<tr><th><b>Header</b></th><th>Type</th></tr>" +
					"<tr><td><code>name</code></td><td><a href=\"http://ex.com\">link</a></td></tr>" +
					"<tr><td><i>note</i></td><td><img src=\"i.png\" alt=\"img\"/></td></tr>" +
					"</table>";
			String expected = "| **Header** | Type |" + NL +
					"| --- | --- |" + NL +
					"| `name` | [link](http://ex.com) |" + NL +
					"| *note* | ![img](i.png) |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class Entities {

		@Test
		public void testBasicEntities() {
			assertEquals("<", convert("&lt;"));
			assertEquals(">", convert("&gt;"));
			assertEquals("&", convert("&amp;"));
			assertEquals("\"", convert("&quot;"));
			assertEquals("'", convert("&apos;"));
		}

		@Test
		public void testNbspEntity() {
			String result = HtmlConverter.unescapeEntities("&nbsp;");
			assertEquals('\u00A0', result.charAt(0));
			assertEquals(1, result.length());
		}

		@Test
		public void testNumericEntities() {
			assertEquals("A", convert("&#65;"));
			assertEquals("<", convert("&#60;"));
			assertEquals(">", convert("&#62;"));
		}

		@Test
		public void testHexEntities() {
			assertEquals("A", convert("&#x41;"));
			assertEquals("A", convert("&#X41;"));
			assertEquals("\u00E9", convert("&#xe9;"));
		}

		@Test
		public void testUnknownEntity() {
			assertEquals("&unknown;", convert("&unknown;"));
		}

		@Test
		public void testMixedEntities() {
			assertEquals("A < B & C > D",
					convert("A &lt; B &amp; C &gt; D"));
		}

		@Test
		public void testEntitiesInCode() {
			assertEquals("`List<String>`",
					convert("<code>List&lt;String&gt;</code>"));
		}
	}

	@Nested
	class Whitespace {

		@Test
		public void testCollapseWhitespaceInHtml() {
			assertEquals("a b c", convert("<p>a   b   c</p>"));
		}

		@Test
		public void testCollapseNewlinesInBlockHtml() {
			assertEquals("a b c", convert("<p>a\n\t\n  b\n\tc</p>"));
		}

		@Test
		public void testLeadingWhitespaceAfterNewline() {
			assertEquals("Title" + NL + "text",
					convert("Title<br>   text"));
		}

		@Test
		public void testWhitespaceOnlyInTags() {
			assertEquals("", convert("<p>   </p>"));
		}

		@Test
		public void testSelfClosingBr() {
			assertEquals("A" + NL + "B", convert("A<br/>B"));
		}

		@Test
		public void testSelfClosingImg() {
			assertEquals("![alt](src.png)", convert("<img src=\"src.png\" alt=\"alt\"/>"));
		}
	}

	@Nested
	class CaseInsensitivity {

		@Test
		public void testUppercaseBold() {
			assertEquals("**bold text**", convert("<B>bold text</B>"));
		}

		@Test
		public void testUppercaseItalic() {
			assertEquals("*italic*", convert("<I>italic</I>"));
		}

		@Test
		public void testUppercaseCode() {
			assertEquals("`code`", convert("<CODE>code</CODE>"));
		}

		@Test
		public void testUppercaseBr() {
			assertEquals("A" + NL + "B", convert("A<BR>B"));
		}

		@Test
		public void testUppercaseBrSelfClosing() {
			assertEquals("A" + NL + "B", convert("A<BR/>B"));
		}

		@Test
		public void testUppercaseParagraph() {
			assertEquals("First" + NL + NL + "Second",
					convert("<P>First</P><P>Second</P>"));
		}

		@Test
		public void testUppercaseHeaders() {
			assertEquals("# Title #", convert("<H1>Title</H1>"));
			assertEquals("## Subtitle ##", convert("<H2>Subtitle</H2>"));
		}

		@Test
		public void testUppercaseLink() {
			assertEquals("[Link](http://example.com)",
					convert("<A HREF=\"http://example.com\">Link</A>"));
		}

		@Test
		public void testUppercaseImage() {
			assertEquals("![alt](img.png)",
					convert("<IMG SRC=\"img.png\" ALT=\"alt\" />"));
		}

		@Test
		public void testMixedCaseTags() {
			assertEquals("**bold** and *italic*",
					convert("<B>bold</b> and <Em>italic</eM>"));
		}

		@Test
		public void testUppercaseList() {
			String html = "<UL><LI>A</LI><LI>B</LI></UL>";
			String expected = " *  A" + NL + " *  B";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testUppercaseTable() {
			String html = "<TABLE><TR><TH>Key</TH><TH>Val</TH></TR><TR><TD>a</TD><TD>1</TD></TR></TABLE>";
			String expected = "| Key | Val |" + NL +
					"| --- | --- |" + NL +
					"| a | 1 |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class MixedHtmlAndMarkdown {

		@Test
		public void testMarkdownBoldWithHtmlCode() {
			assertEquals("**bold** and `code`",
					convert("**bold** and <code>code</code>"));
		}

		@Test
		public void testMarkdownBackticksPreserved() {
			assertEquals("This is `my code`", convert("This is `my code`"));
		}

		@Test
		public void testMarkdownStarsWithHtmlItalic() {
			assertEquals("**stars** and *html italic*",
					convert("**stars** and <i>html italic</i>"));
		}

		@Test
		public void testMarkdownLinksWithHtmlBold() {
			assertEquals("[link](url) and **bold**",
					convert("[link](url) and <b>bold</b>"));
		}

		@Test
		public void testPlainTextWithHtmlFormatting() {
			assertEquals("plain **bold** plain",
					convert("plain <b>bold</b> plain"));
		}

		@Test
		public void testHtmlInsideMarkdownBackticks() {
			assertEquals("The `<element>` tag", convert("The `<element>` tag"));
		}

		@Test
		public void testEntitiesInMarkdownContext() {
			assertEquals("The `<project>` element is the root.",
					convert("The `&lt;project&gt;` element is the root."));
		}
	}

	@Nested
	class ComplexScenarios {

		@Test
		public void testXsdDocumentationStyle() {
			String html = "Defines a single (usually named) bean." +
					"<p>Source: <a href=\"file:/C:/schemas/spring-beans.xsd\">spring-beans.xsd</a></p>";
			String expected = "Defines a single (usually named) bean." + NL + NL +
					"Source: [spring-beans.xsd](file:/C:/schemas/spring-beans.xsd)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testDocumentationWithListAfterParagraph() {
			String html = "<p>The <code>&lt;bean&gt;</code> element supports:</p>" +
					"<ul>" +
					"<li><b>constructor-arg</b> - for constructor injection</li>" +
					"<li><b>property</b> - for setter injection</li>" +
					"</ul>" +
					"<p>See <a href=\"https://docs.spring.io\">Spring Docs</a> for details.</p>";
			String expected = "The `<bean>` element supports:" + NL + NL +
					" *  **constructor-arg** - for constructor injection" + NL +
					" *  **property** - for setter injection" + NL + NL +
					"See [Spring Docs](https://docs.spring.io) for details.";
			assertEquals(expected, convert(html));
		}
	}

	/**
	 * Tests from real-world issues:
	 * https://github.com/eclipse-lemminx/lemminx/issues/962
	 * https://github.com/redhat-developer/vscode-xml/issues/310
	 */
	@Nested
	class RealWorldIssues {

		// --- Issue #962: Maven XSD documentation with <br> and <b> ---

		@Test
		public void testMavenAttributeDocumentation() {
			String html = "When children inherit from project&apos;s url, append path or not? " +
					"Note: While the type of this field is <code>String</code> for technical reasons, " +
					"the semantic type is actually <code>Boolean</code>" +
					"<br><b>Default value is</b>: <code>true</code>" +
					"<br><b>Since</b>: Maven 3.6.1";
			String expected = "When children inherit from project's url, append path or not? " +
					"Note: While the type of this field is `String` for technical reasons, " +
					"the semantic type is actually `Boolean`" + NL +
					"**Default value is**: `true`" + NL +
					"**Since**: Maven 3.6.1";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testDocumentationWithParagraphsAndList() {
			String html = "<p>The Property List key with the main tmPreferences keys.</p>" +
					"<p>Valid values are:</p>" +
					"<ul>" +
					"<li>name</li>" +
					"<li>scope</li>" +
					"<li>settings</li>" +
					"<li>uuid</li>" +
					"</ul>";
			String expected = "The Property List key with the main tmPreferences keys." + NL + NL +
					"Valid values are:" + NL + NL +
					" *  name" + NL +
					" *  scope" + NL +
					" *  settings" + NL +
					" *  uuid";
			assertEquals(expected, convert(html));
		}

		// --- Issue #310: Single <br/> for line breaks ---

		@Test
		public void testSingleBrLineBreak() {
			assertEquals("The name" + NL + "of the element",
					convert("The name<br/>of the element"));
		}

		@Test
		public void testDoubleBrLineBreak() {
			assertEquals("Text" + NL + NL + "more text",
					convert("Text<br/><br/>more text"));
		}

		@Test
		public void testDocumentationWithBrAndDescription() {
			String html = "Array of checkbox which needs to be displayed as part of the checkbox group." +
					"<br/>Value shape: blablaba";
			String expected = "Array of checkbox which needs to be displayed as part of the checkbox group." +
					NL + "Value shape: blablaba";
			assertEquals(expected, convert(html));
		}

		// --- Issue #962: Mixed formatting in XSD documentation ---

		@Test
		public void testXsdAnnotationWithMultipleFormats() {
			String html = "The <code>groupId</code> of the parent project." +
					"<br><b>Required</b>: Yes" +
					"<br>See <a href=\"https://maven.apache.org/pom.html#Inheritance\">Inheritance</a>";
			String expected = "The `groupId` of the parent project." + NL +
					"**Required**: Yes" + NL +
					"See [Inheritance](https://maven.apache.org/pom.html#Inheritance)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testXsdWithEscapedHtmlEntities() {
			String html = "The type is <code>String</code> and the default is <code>true</code>.";
			String expected = "The type is `String` and the default is `true`.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMavenModelDocumentation() {
			String html = "Defines a single (usually named) bean. A bean definition may contain " +
					"nested tags for constructor arguments, property values, lookup methods, " +
					"and replaced methods. Mixing constructor injection and setter injection " +
					"on the same bean is explicitly supported." +
					"<p>Source: <a href=\"file:/C:/schemas/spring-beans-3.0.xsd\">spring-beans-3.0.xsd</a></p>";
			String expected = "Defines a single (usually named) bean. A bean definition may contain " +
					"nested tags for constructor arguments, property values, lookup methods, " +
					"and replaced methods. Mixing constructor injection and setter injection " +
					"on the same bean is explicitly supported." + NL + NL +
					"Source: [spring-beans-3.0.xsd](file:/C:/schemas/spring-beans-3.0.xsd)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMultipleBrWithBoldLabels() {
			String html = "<b>Type</b>: String" +
					"<br><b>Default</b>: none" +
					"<br><b>Required</b>: No" +
					"<br><b>Since</b>: 2.0";
			String expected = "**Type**: String" + NL +
					"**Default**: none" + NL +
					"**Required**: No" + NL +
					"**Since**: 2.0";
			assertEquals(expected, convert(html));
		}

		// --- Issue #951: CDATA with HTML documentation ---

		@Test
		public void testCdataDocumentationWithParagraphsAndList() {
			String html = "<p>Il Tag è utilizzato per creare dei contesti manipolati.</p>" +
					"<p>E' possibile utilizzare le seguenti tag:</p>" +
					"<ul>" +
					"<li>Where: Applica un filtro al contesto.</li>" +
					"<li>Take: Seleziona un certo numero di elementi dal contesto.</li>" +
					"</ul>";
			String expected = "Il Tag è utilizzato per creare dei contesti manipolati." + NL + NL +
					"E' possibile utilizzare le seguenti tag:" + NL + NL +
					" *  Where: Applica un filtro al contesto." + NL +
					" *  Take: Seleziona un certo numero di elementi dal contesto.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testDocumentationWithCodeBlockAndList() {
			String html = "Use the <code>key</code> element to define entries." +
					"<p>Valid values are:</p>" +
					"<ul>" +
					"<li><code>name</code> - The entry name</li>" +
					"<li><code>scope</code> - The visibility scope</li>" +
					"<li><code>settings</code> - Configuration map</li>" +
					"</ul>";
			String expected = "Use the `key` element to define entries." + NL + NL +
					"Valid values are:" + NL + NL +
					" *  `name` - The entry name" + NL +
					" *  `scope` - The visibility scope" + NL +
					" *  `settings` - Configuration map";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class PreFormattedMarkdown {

		@Test
		public void testPlainTextPassthrough() {
			assertEquals("Simple documentation text", convert("Simple documentation text"));
		}

		@Test
		public void testMarkdownBoldPreserved() {
			assertEquals("This is **bold** text", convert("This is **bold** text"));
		}

		@Test
		public void testMarkdownTablePreserved() {
			String markdown = "A table:" + NL + NL +
					"| header 1 | header 2 |" + NL +
					"|----------|----------|" + NL +
					"| data 1 | data 2 |";
			assertEquals(markdown, convert(markdown));
		}

		@Test
		public void testMarkdownNestedListIndentationPreserved() {
			String markdown = "* **Coffee**" + NL +
					"  * Mocha" + NL +
					"  * Latte" + NL +
					"* Tea" + NL +
					"  * Darjeeling" + NL +
					"  * Early Grey";
			assertEquals(markdown, convert(markdown));
		}

		@Test
		public void testMarkdownLinksPreserved() {
			String markdown = "See [Ralf](mailto:foo@bar.com) and [Andrew](mailto:bar@foo.com)";
			assertEquals(markdown, convert(markdown));
		}

		@Test
		public void testMarkdownCodePreserved() {
			assertEquals("Use the `key` element", convert("Use the `key` element"));
		}

		@Test
		public void testEntitiesUnescaped() {
			assertEquals("A & B", convert("A &amp; B"));
			assertEquals("1 < 2 > 0", convert("1 &lt; 2 &gt; 0"));
		}

		@Test
		public void testComplexJavadocStyleMarkdown() {
			String markdown = "This Javadoc contains some ` code ` and a table" + NL + NL +
					"| header 1 | header 2 |" + NL +
					"|----------|----------|" + NL +
					"| data 1 | data 2 |" + NL + NL +
					"* **Coffee**" + NL +
					"  * Mocha" + NL +
					"  * Latte" + NL +
					"* Tea" + NL +
					"  * Darjeeling" + NL +
					"  * Early Grey" + NL + NL +
					"* **Parameters:**" + NL +
					"  * **param1** the first parameter" + NL +
					"  * **param2** the 2nd parameter" + NL +
					"* **Returns:**" + NL +
					"  * some kind of result" + NL +
					"* **Author:**" + NL +
					"  * [Ralf](mailto:foo@bar.com)";
			assertEquals(markdown, convert(markdown));
		}

		@Test
		public void testHtmlStillConverted() {
			assertEquals("This is **bold**", convert("This is <b>bold</b>"));
		}

		@Test
		public void testUnknownXmlTagWrappedInBackticks() {
			assertEquals("Use the `<bean>` element", convert("Use the <bean> element"));
		}

		@Test
		public void testMarkdownWithEntityDoesNotTriggerConverter() {
			String markdown = "Value must be &gt; 0 and &lt; 100";
			assertEquals("Value must be > 0 and < 100", convert(markdown));
		}

		@Test
		public void testMixedTextWithInlineHtml() {
			String input = "Sub-item with <code>code</code> text";
			String expected = "Sub-item with `code` text";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testMixedTextWithBr() {
			String input = "Line 1<br/>Item 2";
			String expected = "Line 1" + NL + "Item 2";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testMarkdownWithIndentedContent() {
			String input = "Previous versions:" + NL + NL +
					"  http://example.com/v1" + NL +
					"    (Version 1)" + NL + NL +
					"  http://example.com/v2" + NL +
					"    (Version 2)";
			assertEquals(input, convert(input));
		}

		@Test
		public void testXsdDocumentationMarkdownPreserved() {
			String markdown = "This Javadoc contains some ` code `, a link to `IOException` and a table" + NL + NL +
					"| header 1 | header 2 |" + NL +
					"|----------|----------|" + NL +
					"| data 1 | data 2 |" + NL + NL + NL +
					"* **Coffee**" + NL +
					"  * Mocha" + NL +
					"  * Latte" + NL +
					"* Tea" + NL +
					"  * Darjeeling" + NL +
					"  * Early Grey" + NL + NL +
					"* **Parameters:**" + NL +
					"  * **param1** the first parameter" + NL +
					"  * **param2** the 2nd parameter" + NL +
					"  * **param3**" + NL +
					"* **Returns:**" + NL +
					"  * some kind of result" + NL +
					"* **Throws:**" + NL +
					"  * NastyException a nasty exception" + NL +
					"  * IOException another nasty exception" + NL +
					"* **Author:**" + NL +
					"  * [Ralf](mailto:foo@bar.com)" + NL +
					"  * [Andrew](mailto:bar@foo.com)" + NL +
					"* **Since:**" + NL +
					"  * 1.0" + NL +
					"  * 0" + NL +
					"* @unknown" + NL +
					"  * unknown tag" + NL +
					"* @unknown" + NL +
					"  * another unknown tag";
			assertEquals(markdown, convert(markdown));
		}
	}

	/**
	 * Tests for block vs inline HTML element handling.
	 *
	 * Block elements (p, div, table, ul, ol, li, h1-h6, blockquote, pre, etc.)
	 * switch to HTML mode where whitespace is collapsed.
	 *
	 * Inline elements (b, code, br, a, span, del, em, etc.) stay in markdown
	 * mode where content is preserved as-is.
	 *
	 * @see <a href="https://spec.commonmark.org/0.31.2/#html-blocks">CommonMark
	 *      HTML Blocks</a>
	 */
	@Nested
	class BlockVsInlineHtml {

		@Test
		public void testBlockDivCollapsesWhitespace() {
			assertEquals("hello world", convert("<div>hello     world</div>"));
		}

		@Test
		public void testBlockParagraphCollapsesWhitespace() {
			assertEquals("hello world", convert("<p>hello     world</p>"));
		}

		@Test
		public void testInlineSpanPreservesContent() {
			assertEquals("hello     world", convert("<span>hello     world</span>"));
		}

		@Test
		public void testInlineDelPreservesMarkdown() {
			assertEquals("~~*foo*~~", convert("<del>*foo*</del>"));
		}

		@Test
		public void testInlineBoldPreservesMarkdown() {
			assertEquals("**some *nested* text**", convert("<b>some *nested* text</b>"));
		}

		@Test
		public void testInlineElementsDoNotAffectDepth() {
			assertEquals("**bold** and `code`" + NL + "next",
					convert("<b>bold</b> and <code>code</code><br>next"));
		}

		@Test
		public void testBlockElementThenMarkdownContent() {
			assertEquals("Title" + NL + NL + "* item 1" + NL + "* item 2",
					convert("<p>Title</p>* item 1\n* item 2"));
		}

		@Test
		public void testMarkdownThenBlockElement() {
			assertEquals("* item 1" + NL + "* item 2" + NL + NL + "Conclusion",
					convert("* item 1\n* item 2<p>Conclusion</p>"));
		}

		@Test
		public void testBlockInsideBlockCollapsesWhitespace() {
			assertEquals("> hello world",
					convert("<blockquote><div>hello     world</div></blockquote>"));
		}

		@Test
		public void testInlineInsideBlockCollapsesWhitespace() {
			assertEquals("hello **bold** world",
					convert("<p>hello     <b>bold</b>     world</p>"));
		}

		@Test
		public void testMarkdownPreservedBetweenBlockElements() {
			assertEquals("First" + NL + NL +
					"* markdown list" + NL +
					"* second item" + NL + NL +
					"Last",
					convert("<p>First</p>* markdown list\n* second item<p>Last</p>"));
		}

		@Test
		public void testInlineHtmlWithMarkdownFormatting() {
			assertEquals("**Type**: String" + NL +
					"**Default**: none" + NL +
					"**Required**: No",
					convert("<b>Type</b>: String<br>" +
							"<b>Default</b>: none<br>" +
							"<b>Required</b>: No"));
		}

		@Test
		public void testBlockSectionCollapsesWhitespace() {
			assertEquals("hello world", convert("<section>hello     world</section>"));
		}

		@Test
		public void testBlockArticleCollapsesWhitespace() {
			assertEquals("hello world", convert("<article>hello     world</article>"));
		}

		@Test
		public void testBlockDetailsCollapsesWhitespace() {
			assertEquals("title content",
					convert("<details><summary>title</summary> content</details>"));
		}

		@Test
		public void testBlockNavCollapsesWhitespace() {
			assertEquals("link1 link2", convert("<nav>link1     link2</nav>"));
		}

		@Test
		public void testBlockHeaderFooterCollapsesWhitespace() {
			assertEquals("head" + NL + NL + "foot",
					convert("<header>head</header><footer>foot</footer>"));
		}

		@Test
		public void testBlockFigureCollapsesWhitespace() {
			assertEquals("![img](i.png) caption",
					convert("<figure><img src=\"i.png\" alt=\"img\"/><figcaption>caption</figcaption></figure>"));
		}

		@Test
		public void testBlockFieldsetCollapsesWhitespace() {
			assertEquals("Title field",
					convert("<fieldset><legend>Title</legend> field</fieldset>"));
		}

		@Test
		public void testBlockMainCollapsesWhitespace() {
			assertEquals("main content", convert("<main>main     content</main>"));
		}

		@Test
		public void testBlockAsideCollapsesWhitespace() {
			assertEquals("side note", convert("<aside>side     note</aside>"));
		}

		@Test
		public void testVoidHrDoesNotAffectDepth() {
			// <hr> is void — should NOT increment blockDepth
			assertEquals("before" + NL + NL + "---" + NL + "after",
					convert("before<hr>after"));
		}

		@Test
		public void testVoidHrSelfClosingDoesNotAffectDepth() {
			assertEquals("before" + NL + NL + "---" + NL + "after",
					convert("before<hr/>after"));
		}
	}

	@Nested
	class BugFixes {

		// Bug 1: Bare < not followed by a valid tag name should be emitted as text
		// (XMLScanner consumes the region between < and > as a malformed tag)

		@Test
		public void testBareAngleBracketInComparison() {
			assertEquals("x < 100 and y > 0", convert("x < 100 and y > 0"));
		}

		@Test
		public void testBareAngleBracketMultiple() {
			assertEquals("3 < 5 > 2 and 7 < 9 > 1", convert("3 < 5 > 2 and 7 < 9 > 1"));
		}

		// Bug 2: Raw & in link destinations should not be entity-escaped

		@Test
		public void testLinkWithRawAmpersandInHref() {
			assertEquals("[Search](https://example.com/search?a=1&b=2)",
					convert("<a href=\"https://example.com/search?a=1&b=2\">Search</a>"));
		}

		@Test
		public void testLinkWithMultipleAmpersands() {
			assertEquals("[Go](http://x.com?a=1&b=2&c=3)",
					convert("<a href=\"http://x.com?a=1&amp;b=2&amp;c=3\">Go</a>"));
		}

		// Bug 3: Newlines in block content should be collapsed to spaces (HTML whitespace rules)

		@Test
		public void testBlockquoteNewlineCollapsed() {
			assertEquals(">  text here",
					convert("<blockquote>\n  text here\n</blockquote>"));
		}

		@Test
		public void testNewlinesCollapsedInDiv() {
			assertEquals("first second third",
					convert("<div>first\nsecond\nthird</div>"));
		}

		// Bug 4: <p> inside <li> should use ensureNewline (not ensureBlankLine)
		// to avoid separating the bullet marker from its text with a blank line

		@Test
		public void testParagraphInsideListItem() {
			String html = "<ul><li><p>Item text</p></li></ul>";
			String expected = " *  Item text";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMultipleParagraphsInsideListItem() {
			String html = "<ul><li><p>First</p><p>Second</p></li><li><p>Third</p></li></ul>";
			String expected = " *  First" + NL +
					"Second" + NL +
					" *  Third";
			assertEquals(expected, convert(html));
		}

		// Bug 6: Table rowspan support — cells spanning multiple rows
		// should insert empty placeholder cells in subsequent rows

		@Test
		public void testTableWithRowspan() {
			String html = "<table>" +
					"<tr><th>Name</th><th>Value</th></tr>" +
					"<tr><td rowspan=\"2\">A</td><td>1</td></tr>" +
					"<tr><td>2</td></tr>" +
					"</table>";
			String expected = "| Name | Value |" + NL +
					"| --- | --- |" + NL +
					"| A | 1 |" + NL +
					"|  | 2 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithRowspanFirstColumn() {
			String html = "<table>" +
					"<tr><th>Cat</th><th>Sub</th><th>Val</th></tr>" +
					"<tr><td rowspan=\"3\">X</td><td>a</td><td>1</td></tr>" +
					"<tr><td>b</td><td>2</td></tr>" +
					"<tr><td>c</td><td>3</td></tr>" +
					"</table>";
			String expected = "| Cat | Sub | Val |" + NL +
					"| --- | --- | --- |" + NL +
					"| X | a | 1 |" + NL +
					"|  | b | 2 |" + NL +
					"|  | c | 3 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithCaption() {
			String html = "<table>" +
					"<caption>My Title</caption>" +
					"<tr><th>Name</th><th>Age</th></tr>" +
					"<tr><td>Anna</td><td>30</td></tr>" +
					"</table>";
			String expected = "My Title" + NL +
					"| Name | Age |" + NL +
					"| --- | --- |" + NL +
					"| Anna | 30 |";
			assertEquals(expected, convert(html));
		}
	}
}
