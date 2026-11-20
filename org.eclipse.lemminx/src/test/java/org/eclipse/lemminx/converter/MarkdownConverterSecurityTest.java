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
package org.eclipse.lemminx.converter;

import static org.eclipse.lemminx.converter.markdown.MarkdownConverter.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MarkdownConverterSecurityTest {

	private static final String NL = System.lineSeparator();

	@Nested
	class UnsafeLinkUrls {

		@Test
		public void testJavascriptSchemeInLink() {
			assertEquals("[Click me]()",
					convert("<a href=\"javascript:alert('xss')\">Click me</a>"));
		}

		@Test
		public void testJavascriptSchemeUppercase() {
			assertEquals("[Click me]()",
					convert("<a href=\"JAVASCRIPT:alert('xss')\">Click me</a>"));
		}

		@Test
		public void testJavascriptSchemeMixedCase() {
			assertEquals("[Click me]()",
					convert("<a href=\"JaVaScRiPt:alert('xss')\">Click me</a>"));
		}

		@Test
		public void testVbscriptSchemeInLink() {
			assertEquals("[Click]()",
					convert("<a href=\"vbscript:MsgBox('xss')\">Click</a>"));
		}

		@Test
		public void testVbscriptSchemeUppercase() {
			assertEquals("[Click]()",
					convert("<a href=\"VBSCRIPT:MsgBox('xss')\">Click</a>"));
		}

		@Test
		public void testDataSchemeInLink() {
			assertEquals("[Click]()",
					convert("<a href=\"data:text/html,<script>alert('xss')</script>\">Click</a>"));
		}

		@Test
		public void testDataSchemeUppercase() {
			assertEquals("[Click]()",
					convert("<a href=\"DATA:text/html,payload\">Click</a>"));
		}

		@Test
		public void testJavascriptWithLeadingWhitespace() {
			assertEquals("[Click]()",
					convert("<a href=\"  javascript:alert('xss')\">Click</a>"));
		}

		@Test
		public void testSafeHttpLink() {
			assertEquals("[Safe](https://example.com)",
					convert("<a href=\"https://example.com\">Safe</a>"));
		}

		@Test
		public void testSafeHttpLinkPreserved() {
			assertEquals("[Link](http://example.com)",
					convert("<a href=\"http://example.com\">Link</a>"));
		}

		@Test
		public void testSafeMailtoLink() {
			assertEquals("[Email](mailto:user@example.com)",
					convert("<a href=\"mailto:user@example.com\">Email</a>"));
		}

		@Test
		public void testSafeFileLink() {
			assertEquals("[File](file:/C:/path/file.txt)",
					convert("<a href=\"file:///C:/path/file.txt\">File</a>"));
		}

		@Test
		public void testSafeRelativeLink() {
			assertEquals("[Relative](../path/file.html)",
					convert("<a href=\"../path/file.html\">Relative</a>"));
		}

		@Test
		public void testSafeAnchorLink() {
			assertEquals("[Anchor](#section)",
					convert("<a href=\"#section\">Anchor</a>"));
		}

		@Test
		public void testSafeEmptyHref() {
			assertEquals("[text]()", convert("<a href=\"\">text</a>"));
		}

		@Test
		public void testNoHrefLink() {
			assertEquals("text", convert("<a>text</a>"));
		}
	}

	@Nested
	class UnsafeImageUrls {

		@Test
		public void testJavascriptSchemeInImage() {
			assertEquals("alt text",
					convert("<img src=\"javascript:alert('xss')\" alt=\"alt text\" />"));
		}

		@Test
		public void testJavascriptSchemeInImageUppercase() {
			assertEquals("alt",
					convert("<img src=\"JAVASCRIPT:void(0)\" alt=\"alt\" />"));
		}

		@Test
		public void testVbscriptSchemeInImage() {
			assertEquals("alt",
					convert("<img src=\"vbscript:payload\" alt=\"alt\" />"));
		}

		@Test
		public void testDataSchemeInImage() {
			assertEquals("alt",
					convert("<img src=\"data:image/svg+xml,<script>alert(1)</script>\" alt=\"alt\" />"));
		}

		@Test
		public void testUnsafeImageNoAlt() {
			assertEquals("",
					convert("<img src=\"javascript:alert(1)\" />"));
		}

		@Test
		public void testUnsafeImageInlineWithText() {
			assertEquals("Before alt After",
					convert("Before <img src=\"javascript:void(0)\" alt=\"alt\" /> After"));
		}

		@Test
		public void testSafeImagePreserved() {
			assertEquals("![logo](https://example.com/logo.png)",
					convert("<img src=\"https://example.com/logo.png\" alt=\"logo\" />"));
		}

		@Test
		public void testSafeImageRelative() {
			assertEquals("![icon](icons/edit.png)",
					convert("<img src=\"icons/edit.png\" alt=\"icon\" />"));
		}
	}

	@Nested
	class ColspanSupport {

		@Test
		public void testColspan2InHeader() {
			String html = "<table>" +
					"<tr><th colspan=\"2\">Merged Header</th><th>C</th></tr>" +
					"<tr><td>1</td><td>2</td><td>3</td></tr>" +
					"</table>";
			String expected = "| Merged Header |  | C |" + NL +
					"| --- | --- | --- |" + NL +
					"| 1 | 2 | 3 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspan2InBody() {
			String html = "<table>" +
					"<tr><th>A</th><th>B</th><th>C</th></tr>" +
					"<tr><td colspan=\"2\">Merged</td><td>3</td></tr>" +
					"</table>";
			String expected = "| A | B | C |" + NL +
					"| --- | --- | --- |" + NL +
					"| Merged |  | 3 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspan3() {
			String html = "<table>" +
					"<tr><th>A</th><th>B</th><th>C</th></tr>" +
					"<tr><td colspan=\"3\">Full width content</td></tr>" +
					"</table>";
			String expected = "| A | B | C |" + NL +
					"| --- | --- | --- |" + NL +
					"| Full width content |  |  |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspanSectionHeader() {
			String html = "<table>" +
					"<tr><th>Construct</th><th>Matches</th></tr>" +
					"<tr><th colspan=\"2\">Characters</th></tr>" +
					"<tr><td><code>[abc]</code></td><td>a, b, or c</td></tr>" +
					"</table>";
			String expected = "| Construct | Matches |" + NL +
					"| --- | --- |" + NL +
					"| Characters |  |" + NL +
					"| `[abc]` | a, b, or c |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspanWithImplicitCellClose() {
			String html = "<table><tr><td colspan=\"2\">A<td>B</tr></table>";
			String expected = "|  |  |  |" + NL +
					"| --- | --- | --- |" + NL +
					"| A |  | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspanMixedWithNormalCells() {
			String html = "<table>" +
					"<tr><th>Name</th><th>Description</th><th>Default</th></tr>" +
					"<tr><td><code>timeout</code></td><td>Delay in ms</td><td><code>5000</code></td></tr>" +
					"<tr><td colspan=\"2\">Merged cell</td><td>-</td></tr>" +
					"</table>";
			String expected = "| Name | Description | Default |" + NL +
					"| --- | --- | --- |" + NL +
					"| `timeout` | Delay in ms | `5000` |" + NL +
					"| Merged cell |  | - |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspanInvalidValue() {
			String html = "<table><tr><th colspan=\"abc\">H</th><th>H2</th></tr>" +
					"<tr><td>A</td><td>B</td></tr></table>";
			String expected = "| H | H2 |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testColspanZero() {
			String html = "<table><tr><th colspan=\"0\">H</th><th>H2</th></tr>" +
					"<tr><td>A</td><td>B</td></tr></table>";
			String expected = "| H | H2 |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class NestedTablesAdvanced {

		@Test
		public void testNestedTableContentInlined() {
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
		public void testNestedTableWithMultipleRows() {
			String html = "<table>" +
					"<tr><td>Category</td><td>" +
					"<table><tr><td>A</td><td>1</td></tr>" +
					"<tr><td>B</td><td>2</td></tr></table>" +
					"</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| Category | A 1 B 2 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testNestedTableWithHeadersAndData() {
			String html = "<table>" +
					"<tr><th>Section</th><th>Details</th></tr>" +
					"<tr><td>Info</td><td>" +
					"<table><tr><th>Key</th><th>Val</th></tr>" +
					"<tr><td>x</td><td>10</td></tr></table>" +
					"</td></tr></table>";
			String expected = "| Section | Details |" + NL +
					"| --- | --- |" + NL +
					"| Info | Key Val x 10 |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testDeeplyNestedTables() {
			String html = "<table><tr><td>" +
					"<table><tr><td>" +
					"<table><tr><td>Deep</td></tr></table>" +
					"</td></tr></table>" +
					"</td></tr></table>";
			String expected = "|  |" + NL +
					"| --- |" + NL +
					"| Deep |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testNestedTableWithFormattedContent() {
			String html = "<table>" +
					"<tr><td>Main</td><td>" +
					"<table><tr><td><b>Bold</b></td><td><code>code</code></td></tr></table>" +
					"</td></tr></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| Main | **Bold** `code` |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class UrlEdgeCases {

		@Test
		public void testJavascriptWithEncodedColon() {
			assertEquals("[Click me]()",
					convert("<a href=\"javascript&#58;alert(1)\">Click me</a>"));
		}

		@Test
		public void testSchemeWithNumbers() {
			assertEquals("[Link](h323:call)",
					convert("<a href=\"h323:call\">Link</a>"));
		}

		@Test
		public void testFtpScheme() {
			assertEquals("[FTP](ftp://files.example.com)",
					convert("<a href=\"ftp://files.example.com\">FTP</a>"));
		}

		@Test
		public void testRelativePathWithColon() {
			assertEquals("[File](path/to:file.html)",
					convert("<a href=\"path/to:file.html\">File</a>"));
		}

		@Test
		public void testUrlWithQueryParamOnly() {
			assertEquals("[Search](?q=test)",
					convert("<a href=\"?q=test\">Search</a>"));
		}

		@Test
		public void testUrlWithFragmentOnly() {
			assertEquals("[Section](#id)",
					convert("<a href=\"#id\">Section</a>"));
		}

		@Test
		public void testUnsafeLinkWithBoldText() {
			assertEquals("[**Click**]()",
					convert("<a href=\"javascript:void(0)\"><b>Click</b></a>"));
		}

		@Test
		public void testUnsafeImageInsideLink() {
			assertEquals("[![alt](safe.png)]()",
					convert("<a href=\"javascript:void(0)\"><img src=\"safe.png\" alt=\"alt\"/></a>"));
		}

		@Test
		public void testSafeLinkUnsafeImage() {
			assertEquals("[evil img](https://example.com)",
					convert("<a href=\"https://example.com\"><img src=\"javascript:void(0)\" alt=\"evil img\"/></a>"));
		}

		@Test
		public void testSecurityInTableCell() {
			String html = "<table><tr><td><a href=\"javascript:alert(1)\">XSS</a></td></tr></table>";
			String result = convert(html);
			assertFalse(result.contains("javascript:"), "Table cell should not contain javascript: URL");
		}

		@Test
		public void testJavascriptWithEmbeddedTab() {
			assertEquals("[Click]()",
					convert("<a href=\"java&#9;script:alert(1)\">Click</a>"));
		}

		@Test
		public void testJavascriptWithEmbeddedNewline() {
			assertEquals("[Click]()",
					convert("<a href=\"java&#10;script:alert(1)\">Click</a>"));
		}

		@Test
		public void testJavascriptWithLeadingC0Control() {
			assertEquals("[Click]()",
					convert("<a href=\"&#1;javascript:alert(1)\">Click</a>"));
		}

		@Test
		public void testJavascriptWithMultipleLeadingControls() {
			assertEquals("[Click]()",
					convert("<a href=\"&#1;&#4;&#8;javascript:alert(1)\">Click</a>"));
		}

		@Test
		public void testJavascriptWithBackslashBeforeColon() {
			assertEquals("[Click]()",
					convert("<a href=\"javascript\\:alert(1)\">Click</a>"));
		}

		@Test
		public void testVbscriptWithBackslashBeforeColon() {
			assertEquals("[Click]()",
					convert("<a href=\"vbscript\\:payload\">Click</a>"));
		}

		@Test
		public void testJavascriptWithDoubleEncodedColon() {
			assertEquals("[Click]()",
					convert("<a href=\"javascript&amp;#58;alert(1)\">Click</a>"));
		}

		@Test
		public void testJavascriptWithTripleEncodedColon() {
			assertEquals("[Click]()",
					convert("<a href=\"javascript&amp;amp;#58;alert(1)\">Click</a>"));
		}

		@Test
		public void testDataSchemeWithBackslash() {
			assertEquals("[Click]()",
					convert("<a href=\"da\\ta:text/html,payload\">Click</a>"));
		}

		@Test
		public void testImageWithBackslashBypass() {
			assertEquals("alt",
					convert("<img src=\"javascript\\:alert(1)\" alt=\"alt\" />"));
		}

		@Test
		public void testImageWithDoubleEncodedBypass() {
			assertEquals("alt",
					convert("<img src=\"javascript&amp;#58;alert(1)\" alt=\"alt\" />"));
		}
	}

	@Nested
	class AnchorWithoutHref {

		@Test
		public void testAnchorWithNameAttribute() {
			assertEquals("text", convert("<a name=\"x\">text</a>"));
		}

		@Test
		public void testAnchorWithEmptyHref() {
			assertEquals("[text]()", convert("<a href=\"\">text</a>"));
		}

		@Test
		public void testAnchorWithIdOnly() {
			assertEquals("anchor point", convert("<a id=\"top\">anchor point</a>"));
		}

		@Test
		public void testAnchorWithoutHrefBold() {
			assertEquals("**bold text**", convert("<a name=\"x\"><b>bold text</b></a>"));
		}
	}

	@Nested
	class PreFormattingTags {

		@Test
		public void testPreSkipsBold() {
			assertEquals("```" + NL + "bold text" + NL + "```",
					convert("<pre><b>bold text</b></pre>"));
		}

		@Test
		public void testPreSkipsItalic() {
			assertEquals("```" + NL + "italic text" + NL + "```",
					convert("<pre><i>italic text</i></pre>"));
		}

		@Test
		public void testPreSkipsStrikethrough() {
			assertEquals("```" + NL + "deleted text" + NL + "```",
					convert("<pre><del>deleted text</del></pre>"));
		}

		@Test
		public void testPreSkipsLink() {
			assertEquals("```" + NL + "link text" + NL + "```",
					convert("<pre><a href=\"http://x.com\">link text</a></pre>"));
		}

		@Test
		public void testPreSkipsCode() {
			assertEquals("```" + NL + "code text" + NL + "```",
					convert("<pre><code>code text</code></pre>"));
		}

		@Test
		public void testPreKeepsBr() {
			assertEquals("```" + NL + "line1" + NL + "line2" + NL + "```",
					convert("<pre>line1<br>line2</pre>"));
		}

		@Test
		public void testPreSkipsAllFormattingMixed() {
			assertEquals("```" + NL + "bold italic code" + NL + "```",
					convert("<pre><b>bold</b> <i>italic</i> <code>code</code></pre>"));
		}
	}

	@Nested
	class SeparatorFixes {

		@Test
		public void testPreFollowedByText() {
			assertEquals("```" + NL + "code" + NL + "```" + NL + NL + "text",
					convert("<pre>code</pre><p>text</p>"));
		}

		@Test
		public void testHeadingFollowedByText() {
			assertEquals("### Title ###" + NL + "text",
					convert("<h3>Title</h3>text"));
		}

		@Test
		public void testHrSetextPrevention() {
			assertEquals("text" + NL + NL + "---" + NL + NL + "more",
					convert("text<hr><p>more</p>"));
		}

		@Test
		public void testHrAfterInlineText() {
			assertEquals("text" + NL + NL + "---",
					convert("text<hr>"));
		}
	}

	@Nested
	class TableWithoutTr {

		@Test
		public void testTdWithoutTr() {
			String html = "<table><td>A</td><td>B</td></table>";
			String expected = "|  |  |" + NL +
					"| --- | --- |" + NL +
					"| A | B |";
			assertEquals(expected, convert(html));
		}

		@Test
		public void testThWithoutTr() {
			String html = "<table><th>H1</th><th>H2</th></table>";
			String expected = "| H1 | H2 |" + NL +
					"| --- | --- |";
			assertEquals(expected, convert(html));
		}
	}

	@Nested
	class FormatDestinationEncoding {

		@Test
		public void testUrlWithAngleBracketsAndSpaces() {
			assertEquals("[link](<http://x.com/a %3Cb%3E>)",
					convert("<a href=\"http://x.com/a &lt;b&gt;\">link</a>"));
		}

		@Test
		public void testUrlWithAmpersandEscaped() {
			assertEquals("[link](http://x.com?a=1&b=2)",
					convert("<a href=\"http://x.com?a=1&amp;b=2\">link</a>"));
		}

		@Test
		public void testUrlWithBackslashEscaped() {
			assertEquals("[link](file:/C:%5Cpath%5Cfile.txt)",
					convert("<a href=\"file:///C:\\path\\file.txt\">link</a>"));
		}

		@Test
		public void testSafeUrlPreservedAfterEscaping() {
			assertEquals("[link](https://example.com/path)",
					convert("<a href=\"https://example.com/path\">link</a>"));
		}

		@Test
		public void testImageUrlWithAmpersandEscaped() {
			assertEquals("![img](http://x.com/img?w=100&h=50)",
					convert("<img src=\"http://x.com/img?w=100&amp;h=50\" alt=\"img\" />"));
		}
	}
}
