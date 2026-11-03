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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests adapted from eclipse.jdt.ls JavaDoc2MarkdownConverterTest.
 *
 * Validates that our HTML-to-Markdown conversion handles the same real-world
 * Javadoc HTML patterns that JDT LS encounters.
 *
 * @see <a href=
 *      "https://github.com/eclipse-jdtls/eclipse.jdt.ls/blob/main/org.eclipse.jdt.ls.tests/src/org/eclipse/jdt/ls/core/internal/javadoc/JavaDoc2MarkdownConverterTest.java">JDT
 *      LS source</a>
 */
public class MarkdownConverterJdtTest {

	private static final String NL = System.lineSeparator();

	@Nested
	class Tables {

		@Test
		public void testTableWithHeaders() {
			String html = "<table>\n" +
					"    <tr>\n" +
					"        <th>Header 1</th>\n" +
					"        <th>Header 2</th>\n" +
					"    </tr>\n" +
					"    <tr>\n" +
					"        <td>Row 1A</td>\n" +
					"        <td>Row 1B</td>\n" +
					"    </tr>\n" +
					"    <tr>\n" +
					"        <td>Row 2A</td>\n" +
					"        <td>Row 2B</td>\n" +
					"    </tr>\n" +
					"</table>";
			String expected = "| Header 1 | Header 2 |" + NL +
					"| --- | --- |" + NL +
					"| Row 1A | Row 1B |" + NL +
					"| Row 2A | Row 2B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithoutHeaders() {
			String html = "<table>\n" +
					"    <tr>\n" +
					"        <td>Row 0A</td>\n" +
					"        <td>Row 0B</td>\n" +
					"    </tr>\n" +
					"    <tr>\n" +
					"        <td>Row 1A</td>\n" +
					"        <td>Row 1B</td>\n" +
					"    </tr>\n" +
					"    <tr>\n" +
					"        <td>Row 2A</td>\n" +
					"        <td>Row 2B</td>\n" +
					"    </tr>\n" +
					"</table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| Row 0A | Row 0B |" + NL +
					"| Row 1A | Row 1B |" + NL +
					"| Row 2A | Row 2B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithTheadAndTbody() {
			String html = "<table>\n" +
					"  <thead><tr><th>header 1</th><th>header 2</th></tr></thead>\n" +
					"  <tbody><tr><td>data 1</td><td>data 2</td></tr></tbody>\n" +
					"</table>";
			String expected = "| header 1 | header 2 |" + NL +
					"| --- | --- |" + NL +
					"| data 1 | data 2 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testStandardCharsetsTable() {
			String html = "<table>" +
					"<tr><th>Charset</th><th>Description</th></tr>" +
					"<tr><td><tt>US-ASCII</tt></td>" +
					"    <td>Seven-bit ASCII</td></tr>" +
					"<tr><td><tt>ISO-8859-1</tt></td>" +
					"    <td>ISO Latin Alphabet No. 1</td></tr>" +
					"<tr><td><tt>UTF-8</tt></td>" +
					"    <td>Eight-bit UCS Transformation Format</td></tr>" +
					"<tr><td><tt>UTF-16</tt></td>" +
					"    <td>Sixteen-bit UCS Transformation Format</td></tr>" +
					"</table>";
			String expected = "| Charset | Description |" + NL +
					"| --- | --- |" + NL +
					"| `US-ASCII` | Seven-bit ASCII |" + NL +
					"| `ISO-8859-1` | ISO Latin Alphabet No. 1 |" + NL +
					"| `UTF-8` | Eight-bit UCS Transformation Format |" + NL +
					"| `UTF-16` | Sixteen-bit UCS Transformation Format |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableAfterParagraphContent() {
			String html = "<p>The following charsets are supported:</p>" +
					"<table>" +
					"<tr><th>Name</th><th>Description</th></tr>" +
					"<tr><td>UTF-8</td><td>Eight-bit UCS</td></tr>" +
					"</table>";
			String expected = "The following charsets are supported:" + NL + NL +
					"| Name | Description |" + NL +
					"| --- | --- |" + NL +
					"| UTF-8 | Eight-bit UCS |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testTableWithFormattedCellsFromJavadoc() {
			String html = "<table>" +
					"<tr><th>Method</th><th>Description</th></tr>" +
					"<tr><td><code>getName()</code></td>" +
					"<td>Returns the <a href=\"#name\">name</a> of the charset</td></tr>" +
					"<tr><td><code>aliases()</code></td>" +
					"<td>Returns the <b>aliases</b> of the charset</td></tr>" +
					"</table>";
			String expected = "| Method | Description |" + NL +
					"| --- | --- |" + NL +
					"| `getName()` | Returns the [name](#name) of the charset |" + NL +
					"| `aliases()` | Returns the **aliases** of the charset |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testWeirdTableWithSectionHeaders() {
			String html = "<table>" +
					"<thead><tr><th>Construct</th><th>Matches</th></tr></thead>" +
					"<tbody>" +
					"<tr><th colspan=\"2\">Characters</th></tr>" +
					"<tr><td><code>[abc]</code></td>" +
					"    <td><code>a</code>, <code>b</code>, or <code>c</code></td></tr>" +
					"<tr><td><code>[^abc]</code></td>" +
					"    <td>Any character except <code>a</code>, <code>b</code>, or <code>c</code></td></tr>" +
					"</tbody></table>";
			String result = convert(html);
			assertTrue(result.contains("| Construct | Matches |"));
			assertTrue(result.contains("| `[abc]` |"));
			assertTrue(result.contains("| `[^abc]` |"));
		}
	}

	@Nested
	class Links {

		@Test
		public void testFileLink() {
			assertEquals("[File](file://some_location)",
					convert("<a href=\"file://some_location\">File</a>"));
		}

		@Test
		public void testJdtLink() {
			assertEquals("[JDT](jdt://some_location)",
					convert("<a href=\"jdt://some_location\">JDT</a>"));
		}

		@Test
		public void testLinkWithParenthesesInUrl() {
			String html = "<a href=\"jdt://contents/foo.jar/pkg/Clazz.class?=x/%3Cpkg(Clazz.class#42\">waitForExit</a>";
			String expected = "[waitForExit](<jdt://contents/foo.jar/pkg/Clazz.class?=x/%3Cpkg(Clazz.class#42>)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testMailtoLink() {
			assertEquals("[Ralf](mailto:foo@bar.com)",
					convert("<a href=\"mailto:foo@bar.com\">Ralf</a>"));
		}

		@Test
		public void testLinkWithTitleAttribute() {
			String html = "<a href=\"../spi/CharsetProvider.html\" title=\"class in java.nio.charset.spi\"><code>CharsetProvider</code></a>";
			assertEquals("[`CharsetProvider`](../spi/CharsetProvider.html)",
					convert(html));
		}
	}

	@Nested
	class PreBlocks {

		@Test
		public void testPreBlockFromMethodDoc() {
			String html = "This is a method that does something." +
					"<pre>int x = 10;\nSystem.out.println(x);</pre>";
			String expected = "This is a method that does something." + NL +
					"```" + NL + "int x = 10;\nSystem.out.println(x);" + NL + "```";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testPreBlockWithParagraphBefore() {
			String html = "<p>Returns the canonical name of this charset.</p>" +
					"<pre>Charset cs = Charset.forName(\"UTF-8\");\nString name = cs.name();</pre>";
			String expected = "Returns the canonical name of this charset." + NL + NL +
					"```" + NL + "Charset cs = Charset.forName(\"UTF-8\");\nString name = cs.name();" + NL + "```";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class JavadocPatterns {

		@Test
		public void testDefinitionListFromJavadoc() {
			String html = "<dl>" +
					"<dt>Parameters:</dt>" +
					"<dd><code>name</code> - the charset name</dd>" +
					"<dt>Returns:</dt>" +
					"<dd>the charset object, or <code>null</code></dd>" +
					"</dl>";
			String expected = "**Parameters:**" + NL +
					"`name` - the charset name" + NL +
					"**Returns:**" + NL +
					"the charset object, or `null`";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testHeadingsFromJavadoc() {
			String html = "<p>Some introduction text.</p>" +
					"<h2>Charset names</h2>" +
					"<p>Charsets are named by strings.</p>" +
					"<h2>Standard charsets</h2>" +
					"<p>Every implementation must support these.</p>";
			String expected = "Some introduction text." + NL + NL +
					"## Charset names ##" + NL + NL +
					"Charsets are named by strings." + NL + NL +
					"## Standard charsets ##" + NL + NL +
					"Every implementation must support these.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testListWithCodeElements() {
			String html = "<ul>" +
					"<li>The uppercase letters <tt>'A'</tt> through <tt>'Z'</tt></li>" +
					"<li>The lowercase letters <tt>'a'</tt> through <tt>'z'</tt></li>" +
					"<li>The digits <tt>'0'</tt> through <tt>'9'</tt></li>" +
					"</ul>";
			String expected = " *  The uppercase letters `'A'` through `'Z'`" + NL +
					" *  The lowercase letters `'a'` through `'z'`" + NL +
					" *  The digits `'0'` through `'9'`";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testNestedUnorderedLists() {
			String html = "<ul>" +
					"<li><b>Coffee</b>" +
					"<ul>" +
					"<li>Mocha</li>" +
					"<li>Latte</li>" +
					"</ul>" +
					"</li>" +
					"<li>Tea" +
					"<ul>" +
					"<li>Darjeeling</li>" +
					"<li>Early Grey</li>" +
					"</ul>" +
					"</li>" +
					"</ul>";
			String expected = " *  **Coffee**" + NL +
					"     *  Mocha" + NL +
					"     *  Latte" + NL +
					" *  Tea" + NL +
					"     *  Darjeeling" + NL +
					"     *  Early Grey";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testParagraphsWithLinks() {
			String html = "<p>A named mapping between sequences of sixteen-bit Unicode " +
					"<a href=\"../../lang/Character.html#unicode\">code units</a> and sequences of bytes.</p>" +
					"<p>Support for new charsets can be added via the " +
					"<a href=\"../spi/CharsetProvider.html\"><code>CharsetProvider</code></a> class.</p>";
			String expected = "A named mapping between sequences of sixteen-bit Unicode " +
					"[code units](../../lang/Character.html#unicode) and sequences of bytes." + NL + NL +
					"Support for new charsets can be added via the " +
					"[`CharsetProvider`](../spi/CharsetProvider.html) class.";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testHrSeparator() {
			String html = "<p>Main content.</p><hr><p><b>Since:</b> 1.4</p>";
			String expected = "Main content." + NL + NL +
					"---" + NL + NL +
					"**Since:** 1.4";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testCompleteJavadocPage() {
			String html = "<p>Returns the canonical name of this charset.</p>" +
					"<dl>" +
					"<dt>Returns:</dt>" +
					"<dd>The canonical name of this charset</dd>" +
					"</dl>" +
					"<hr>" +
					"<p><b>Since:</b> 1.4</p>" +
					"<p>See <a href=\"https://docs.oracle.com/javase/8/docs/api/\">Java API docs</a></p>";
			String expected = "Returns the canonical name of this charset." + NL + NL +
					"**Returns:**" + NL +
					"The canonical name of this charset" + NL + NL +
					"---" + NL + NL +
					"**Since:** 1.4" + NL + NL +
					"See [Java API docs](https://docs.oracle.com/javase/8/docs/api/)";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testBlockquoteFromJavadoc() {
			String html = "<p>The precedence of operators:</p>" +
					"<blockquote>Literal escape has the highest precedence.</blockquote>" +
					"<p>Note that metacharacters differ inside character classes.</p>";
			String expected = "The precedence of operators:" + NL + NL +
					"> Literal escape has the highest precedence." + NL + NL +
					"Note that metacharacters differ inside character classes.";
			assertEquals(expected, convert(html));
		}
	}
}
