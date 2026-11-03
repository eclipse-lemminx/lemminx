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

public class MarkdownConverterAdvancedTest {

	private static final String NL = System.lineSeparator();

	@Nested
	class PreformattedCodeBlocks {

		@Test
		public void testSimplePreBlock() {
			String html = "<pre>int x = 1;</pre>";
			String expected = "```" + NL + "int x = 1;" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockWithMultipleLines() {
			String html = "<pre>line one\nline two\nline three</pre>";
			String expected = "```" + NL + "line one\nline two\nline three" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockPreservesWhitespace() {
			String html = "<pre>  indented\n    more indented\n  back</pre>";
			String expected = "```" + NL + "  indented\n    more indented\n  back" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockWithCodeChild() {
			String html = "<pre><code>public class Foo {\n    void bar() {}\n}</code></pre>";
			String expected = "```" + NL + "public class Foo {\n    void bar() {}\n}" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockAfterParagraph() {
			String html = "<p>Example:</p><pre>int x = 42;</pre>";
			String expected = "Example:" + NL + NL +
					"```" + NL + "int x = 42;" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockWithSpanChildren() {
			String html = "<pre><span>public</span> <span>class</span> <span>Foo</span> {}</pre>";
			String expected = "```" + NL + "public class Foo {}" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockFollowedByParagraph() {
			String html = "<pre>code here</pre><p>After the code block.</p>";
			String expected = "```" + NL + "code here" + NL + "```" + NL + NL +
					"After the code block.";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class OrderedLists {

		@Test
		public void testSimpleOrderedList() {
			String html = "<ol><li>First</li><li>Second</li><li>Third</li></ol>";
			String expected = "1. First" + NL +
					"2. Second" + NL +
					"3. Third";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testOrderedListWithFormatting() {
			String html = "<ol><li><b>Step one</b>: do this</li><li><code>Step two</code>: do that</li></ol>";
			String expected = "1. **Step one**: do this" + NL +
					"2. `Step two`: do that";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testOrderedListAfterParagraph() {
			String html = "<p>Steps to follow:</p><ol><li>Clone the repo</li><li>Run build</li></ol>";
			String expected = "Steps to follow:" + NL + NL +
					"1. Clone the repo" + NL +
					"2. Run build";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testOrderedListWithLinks() {
			String html = "<ol><li>See <a href=\"https://example.com\">docs</a></li><li>Done</li></ol>";
			String expected = "1. See [docs](https://example.com)" + NL +
					"2. Done";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class Blockquotes {

		@Test
		public void testSimpleBlockquote() {
			String html = "<blockquote>Quoted text</blockquote>";
			String expected = "> Quoted text";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBlockquoteWithFormatting() {
			String html = "<blockquote>This is <b>important</b> and <code>code</code></blockquote>";
			String expected = "> This is **important** and `code`";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBlockquoteAfterParagraph() {
			String html = "<p>Note:</p><blockquote>A quoted remark.</blockquote>";
			String expected = "Note:" + NL + NL +
					"> A quoted remark.";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class HorizontalRules {

		@Test
		public void testSimpleHr() {
			String html = "<p>Above</p><hr><p>Below</p>";
			String expected = "Above" + NL + NL +
					"---" + NL +
					NL + "Below";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testSelfClosingHr() {
			String html = "<p>Above</p><hr/><p>Below</p>";
			String expected = "Above" + NL + NL +
					"---" + NL +
					NL + "Below";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class Strikethrough {

		@Test
		public void testDelTag() {
			String html = "<del>deleted</del>";
			String expected = "~~deleted~~";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testSTag() {
			String html = "<s>struck out</s>";
			String expected = "~~struck out~~";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testStrikethroughWithFormatting() {
			String html = "<del><b>bold deleted</b></del>";
			String expected = "~~**bold deleted**~~";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class DefinitionLists {

		@Test
		public void testSimpleDefinitionList() {
			String html = "<dl><dt>Term</dt><dd>Definition of the term</dd></dl>";
			String expected = "**Term**" + NL +
					"Definition of the term";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMultipleDefinitions() {
			String html = "<dl>" +
					"<dt>Parameters:</dt>" +
					"<dd><code>name</code> - the name</dd>" +
					"<dt>Returns:</dt>" +
					"<dd>the value, or <code>null</code></dd>" +
					"</dl>";
			String expected = "**Parameters:**" + NL +
					"`name` - the name" + NL +
					"**Returns:**" + NL +
					"the value, or `null`";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testJavadocStyleDl() {
			String html = "<p>Returns the value.</p>" +
					"<dl>" +
					"<dt><b>Parameters:</b></dt>" +
					"<dd><code>name</code> - the name</dd>" +
					"<dt><b>Returns:</b></dt>" +
					"<dd>the value, or <code>null</code> if absent</dd>" +
					"</dl>";
			String expected = "Returns the value." + NL + NL +
					"****Parameters:****" + NL +
					"`name` - the name" + NL +
					"****Returns:****" + NL +
					"the value, or `null` if absent";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class AdvancedComplexScenarios {

		@Test
		public void testTableWithBrInCell() {
			String html = "<table>" +
					"<tr><th>Name</th><th>Description</th></tr>" +
					"<tr><td><code>timeout</code></td><td>Delay in ms.<br>Default: 5000</td></tr>" +
					"</table>";
			String expected = "| Name | Description |" + NL +
					"| --- | --- |" + NL +
					"| `timeout` | Delay in ms. Default: 5000 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithLinksInCells() {
			String html = "<table>" +
					"<tr><th>Feature</th><th>Link</th></tr>" +
					"<tr><td>Docs</td><td><a href=\"https://example.com\">See here</a></td></tr>" +
					"</table>";
			String expected = "| Feature | Link |" + NL +
					"| --- | --- |" + NL +
					"| Docs | [See here](https://example.com) |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testHeaderWithAnchorLink() {
			String html = "<h2><a href=\"#install\">Installation</a></h2>";
			String expected = "## [Installation](#install) ##";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testImageInsideLink() {
			String html = "<a href=\"https://example.com\"><img src=\"logo.png\" alt=\"Logo\"></a>";
			String expected = "[![Logo](logo.png)](https://example.com)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testParagraphWithStrikethroughAndBold() {
			String html = "<p><del>Old value</del> replaced by <b>new value</b>.</p>";
			String expected = "~~Old value~~ replaced by **new value**.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMixedListsAndParagraphs() {
			String html = "<p>Overview:</p>" +
					"<ul><li>Point A</li><li>Point B</li></ul>" +
					"<p>Details:</p>" +
					"<ol><li>Step one</li><li>Step two</li></ol>";
			String expected = "Overview:" + NL + NL +
					" *  Point A" + NL +
					" *  Point B" + NL + NL +
					"Details:" + NL + NL +
					"1. Step one" + NL +
					"2. Step two";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testCodeBlockFollowedByList() {
			String html = "<p>Example usage:</p>" +
					"<pre>Foo foo = new Foo();\nfoo.bar();</pre>" +
					"<p>Supported methods:</p>" +
					"<ul><li><code>bar()</code></li><li><code>baz()</code></li></ul>";
			String expected = "Example usage:" + NL + NL +
					"```" + NL + "Foo foo = new Foo();\nfoo.bar();" + NL + "```" + NL + NL +
					"Supported methods:" + NL + NL +
					" *  `bar()`" + NL +
					" *  `baz()`";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testJavadocFullDocumentation() {
			String html = "<p>Returns the value.</p>" +
					"<dl>" +
					"<dt>Parameters:</dt>" +
					"<dd><code>name</code> - the entry name</dd>" +
					"<dt>Returns:</dt>" +
					"<dd>the value, or <code>null</code> if &lt;absent&gt;</dd>" +
					"</dl>" +
					"<hr>" +
					"<p><b>Since:</b> 1.2</p>";
			String expected = "Returns the value." + NL + NL +
					"**Parameters:**" + NL +
					"`name` - the entry name" + NL +
					"**Returns:**" + NL +
					"the value, or `null` if <absent>" + NL + NL +
					"---" + NL + NL +
					"**Since:** 1.2";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBlockquoteWithListInside() {
			String html = "<blockquote>Important notes:<ul><li>First</li><li>Second</li></ul></blockquote>";
			String expected = "> Important notes:" + NL +
					" *  First" + NL +
					" *  Second";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testDeeplyNestedFormatting() {
			String html = "<p><b>Bold with <i>italic and <code>code</code> inside</i> it</b></p>";
			String expected = "**Bold with *italic and `code` inside* it**";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMultipleParagraphsWithDiverseContent() {
			String html = "<p>First paragraph with <a href=\"https://example.com\">link</a>.</p>" +
					"<p>Second paragraph with <code>code</code> and <b>bold</b>.</p>" +
					"<p>Third paragraph with <del>deleted</del> text.</p>";
			String expected = "First paragraph with [link](https://example.com)." + NL + NL +
					"Second paragraph with `code` and **bold**." + NL + NL +
					"Third paragraph with ~~deleted~~ text.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testHrBetweenSections() {
			String html = "<h2>Section 1</h2>" +
					"<p>Content one.</p>" +
					"<hr>" +
					"<h2>Section 2</h2>" +
					"<p>Content two.</p>";
			String expected = "## Section 1 ##" + NL + NL +
					"Content one." + NL + NL +
					"---" + NL + NL +
					"## Section 2 ##" + NL + NL +
					"Content two.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testOrderedListWithCodeAndLinks() {
			String html = "<ol>" +
					"<li>Run <code>mvn install</code></li>" +
					"<li>Open <a href=\"https://example.com\">dashboard</a></li>" +
					"<li>Check the <b>status</b> column</li>" +
					"</ol>";
			String expected = "1. Run `mvn install`" + NL +
					"2. Open [dashboard](https://example.com)" + NL +
					"3. Check the **status** column";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockWithSyntaxHighlightingSpans() {
			String html = "<pre>" +
					"<span>public</span> <span>class</span> <span>Foo</span> {\n" +
					"    <span>// comment</span>\n" +
					"    <span>void</span> <span>bar</span>() {}\n" +
					"}</pre>";
			String expected = "```" + NL +
					"public class Foo {\n" +
					"    // comment\n" +
					"    void bar() {}\n" +
					"}" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testWhitespaceNormalizationVsBr() {
			String html = "<p>\n   Line with    multiple   spaces\n" +
					"   and source newlines\n" +
					"   that are not breaks.\n</p>" +
					"<p>Real break<br>on new line.</p>";
			String expected = "Line with multiple spaces and source newlines that are not breaks." + NL + NL +
					"Real break" + NL +
					"on new line.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testCompleteDocumentationPage() {
			String html = "<h1>API Reference</h1>" +
					"<p>The <code>Foo</code> class provides utility methods.</p>" +
					"<h2>Methods</h2>" +
					"<dl>" +
					"<dt>bar()</dt>" +
					"<dd>Performs the bar operation.</dd>" +
					"<dt>baz(String name)</dt>" +
					"<dd>Sets the <code>name</code> property.</dd>" +
					"</dl>" +
					"<h2>Example</h2>" +
					"<pre>Foo f = new Foo();\nf.bar();</pre>" +
					"<p>See <a href=\"https://example.com/api\">full docs</a> for more.</p>";
			String expected = "# API Reference #" + NL + NL +
					"The `Foo` class provides utility methods." + NL + NL +
					"## Methods ##" + NL + NL +
					"**bar()**" + NL +
					"Performs the bar operation." + NL +
					"**baz(String name)**" + NL +
					"Sets the `name` property." + NL + NL +
					"## Example ##" + NL +
					"```" + NL + "Foo f = new Foo();\nf.bar();" + NL + "```" + NL + NL +
					"See [full docs](https://example.com/api) for more.";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class UnknownTagCodeBlocks {

		@Test
		public void testUnknownTagInlineWithText() {
			// Unknown tag surrounded by text → inline code
			assertEquals("this `<beans>` element", convert("this <beans> element"));
		}

		@Test
		public void testUnknownTagStandaloneOnLine() {
			// Unknown tag alone on its own line → fenced code block
			String input = "e.g.\n\n<beans profile=\"p1,!p2\">\n\nindicates";
			String expected = "e.g." + NL + NL +
					"```" + NL + "<beans profile=\"p1,!p2\">" + NL + "```" + NL + NL +
					"indicates";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testUnknownTagAtStartOfInput() {
			// Unknown tag at the very start → fenced code block
			String input = "<beans profile=\"p1\">\n\ntext";
			String expected = "```" + NL + "<beans profile=\"p1\">" + NL + "```" + NL + NL + "text";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testUnknownTagAfterNewlineWithNbsp() {
			// Unknown tag after NBSP on a new line → fenced code block
			// (simulates joinProseLines NBSP indentation)
			String input = "example:\n\n  <config value=\"x\">\n\ntext";
			String expected = "example:" + NL + NL +
					"```" + NL + "<config value=\"x\">" + NL + "```" + NL + NL +
					"text";
			assertEquals(expected, convert(input));
		}

		@Test
		public void testMultipleUnknownTagsInline() {
			// Multiple unknown tags in text → all inline code
			assertEquals("use `<beans>` and `<bean>` elements",
					convert("use <beans> and <bean> elements"));
		}

		@Test
		public void testUnknownClosingTagStandalone() {
			// Unknown closing tag alone on its line → fenced code block
			String input = "text\n\n</beans>\n\nmore";
			String expected = "text" + NL + NL +
					"```" + NL + "</beans>" + NL + "```" + NL + NL +
					"more";
			assertEquals(expected, convert(input));
		}
	}
}
