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
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with indentation.
 *
 */
public class XMLFormatterIndentTest extends AbstractCacheBasedTest {

	@Test
	public void startWithSpaces() throws BadLocationException {
		String content = "\r\n    " + //
				"   <a></a>";
		String expected = "<a></a>";
		assertFormat(content, expected, //
				te(0, 0, 1, 7, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void oneElementsInSameLine() throws BadLocationException {
		String content = "<a></a>";
		String expected = content;
		assertFormat(content, expected);

		content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<a></a>";
		expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void oneElementsInDifferentLine() throws BadLocationException {
		String content = "<a>\r\n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void oneElementsInDifferentLineWithSpace() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  </a>";
		String expected = "<a>\r\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 1, 2, "\r\n"));
		assertFormat(expected, expected);
	}

	@Test
	public void twoElementsInSameLine() throws BadLocationException {
		String content = "<a><b></b></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <b></b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), //
				te(0, 10, 0, 10, lineSeparator()));
		assertFormat(expected, expected);
	}

	// Use case (#1026): internal whitespace between text must be preserved
	// <a>b  c</a> => <a>b  c</a> (not <a>b c</a>)
	@Test
	public void textSpaces() throws BadLocationException {
		String content = "<a>b  c</a>";
		String expected = "<a>b  c</a>";
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void mixedContent() throws BadLocationException {
		String content = "<a><b>B</b></a>";
		String expected = "<a>" + lineSeparator() + //
				"  <b>B</b>" + lineSeparator() + // indent with 2 spaces
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 0, 3, lineSeparator() + "  "), // indent with 2 spaces
				te(0, 11, 0, 11, lineSeparator()));
		assertFormat(expected, expected);
	}

	@Test
	public void mixedContentWithTabs4Spaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setInsertSpaces(true);
		settings.getFormattingSettings().setTabSize(4);

		String content = "<a><b>B</b></a>";
		String expected = "<a>" + lineSeparator() + //
				"    <b>B</b>" + lineSeparator() + // indent with 4 spaces
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 0, 3, lineSeparator() + "    "), // indent with 4 spaces
				te(0, 11, 0, 11, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void mixedContentWithTabs() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setInsertSpaces(false);

		String content = "<a><b>B</b></a>";
		String expected = "<a>" + lineSeparator() + //
				"	<b>B</b>" + lineSeparator() + // indent with one tab
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 0, 3, lineSeparator() + "	"), // indent with one tab
				te(0, 11, 0, 11, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/634
	@Test
	public void multipleRootNestedIssue634() throws BadLocationException {
		String content = "<parent>\r\n" + //
				"  <child>\r\n" + //
				"    test\r\n" + //
				"  </child>\r\n" + //
				"</parent>" + //
				"<parent>\r\n" + //
				"  <child>\r\n" + //
				"    test\r\n" + //
				"  </child>\r\n" + //
				"</parent>";
		String expected = "<parent>\r\n" + //
				"  <child>\r\n" + //
				"    test\r\n" + //
				"  </child>\r\n" + //
				"</parent>\r\n" + //
				"<parent>\r\n" + //
				"  <child>\r\n" + //
				"    test\r\n" + //
				"  </child>\r\n" + //
				"</parent>";
		assertFormat(content, expected, //
				te(4, 9, 4, 9, "\r\n"));
		assertFormat(expected, expected);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/634
	@Test
	public void multipleRootEmptyIssue634() throws BadLocationException {
		String content = "<foo />\r\n" + //
				"<bar />\r\n" + //
				"<fizz />\r\n" + //
				"<buzz />";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void mixedContent2() throws BadLocationException {
		String content = "<a>A<b>B</b></a>";
		String expected = content;
		assertFormat(content, expected);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/600
	@Test
	public void multipleLineContentIssue600JoinContentLinesTrue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    foo\r\n" + //
				"    bar\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b> foo bar </b>\r\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 5, 2, 4, " "),
				te(2, 7, 3, 4, " "),
				te(3, 7, 4, 2, " "));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/600
	@Test
	public void multipleLineContentIssue600JoinContentLinesFalse() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(false);
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    foo\r\n" + //
				"    bar\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/600
	@Test
	public void multipleLineContentIssue600PreserveSpaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(true);
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    foo\r\n" + //
				"    bar\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/662
	@Test
	public void xsDocumentationTextContentIssue662JoinContentTrue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"           <xs:documentation>\r\n" + //
				"    Content that spans\r\n" + //
				"    multiple lines.\r\n" + //
				"</xs:documentation>\r\n" + //
				"           </xs:annotation>\r\n" + //
				"           </xs:complexType>\r\n" + //
				"</xs:schema>";
		String expected = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"      <xs:documentation> Content that spans multiple lines. </xs:documentation>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"  </xs:complexType>\r\n" + //
				"</xs:schema>";
		assertFormat(content, expected, settings, //
				te(2, 19, 3, 11, "\r\n      "), //
				te(3, 29, 4, 4, " "), //
				te(4, 22, 5, 4, " "), //
				te(5, 19, 6, 0, " "), //
				te(6, 19, 7, 11, "\r\n    "), //
				te(7, 27, 8, 11, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/662
	@Test
	public void xsDocumentationTextContentIssue662JoinContentFalse() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"           <xs:documentation>\r\n" + //
				"    Content that spans\r\n" + //
				"    multiple lines.\r\n" + //
				"</xs:documentation>\r\n" + //
				"           </xs:annotation>\r\n" + //
				"           </xs:complexType>\r\n" + //
				"</xs:schema>";
		String expected = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"      <xs:documentation>\r\n" + //
				"        Content that spans\r\n" + //
				"        multiple lines.\r\n" + //
				"      </xs:documentation>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"  </xs:complexType>\r\n" + //
				"</xs:schema>";
		assertFormat(content, expected, settings, //
				te(2, 19, 3, 11, "\r\n      "), //
				te(3, 29, 4, 4, "\r\n        "), //
				te(4, 22, 5, 4, "\r\n        "), //
				te(5, 19, 6, 0, "\r\n      "), //
				te(6, 19, 7, 11, "\r\n    "), //
				te(7, 27, 8, 11, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/redhat-developer/vscode-xml/issues/662
	// Use case (#1301): xml:space="preserve" content is untouched but the
	// closing tag </xs:documentation> must be indented to match its opening tag.
	@Test
	public void xsDocumentationTextContentIssue662Preserve() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"           <xs:documentation xml:space=\"preserve\">\r\n" + //
				"    Content that spans\r\n" + //
				"    multiple lines.\r\n" + //
				"</xs:documentation>\r\n" + //
				"           </xs:annotation>\r\n" + //
				"           </xs:complexType>\r\n" + //
				"</xs:schema>";
		String expected = "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"      <xs:documentation xml:space=\"preserve\">\r\n" + //
				"    Content that spans\r\n" + //
				"    multiple lines.\r\n" + //
				"      </xs:documentation>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"  </xs:complexType>\r\n" + //
				"</xs:schema>";
		assertFormat(content, expected, settings, //
				te(2, 19, 3, 11, "\r\n      "), //
				te(6, 0, 6, 0, "      "), //
				te(6, 19, 7, 11, "\r\n    "), //
				te(7, 27, 8, 11, "\r\n  "));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/eclipse-lemminx/lemminx/pull/1845#issuecomment-5700561219
	// Use case (CRLF): text-only element with unindented content and closing tag.
	// <doc>\r\nContent\r\nmultiple lines.\r\n</doc>
	// =>
	// <doc>\r\n    Content\r\n    multiple lines.\r\n  </doc>
	@Test
	public void normalizeSpaceEndTagIndentation() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"Content\r\n" + //
				"multiple lines.\r\n" + //
				"</doc>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"    Content\r\n" + //
				"    multiple lines.\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(1, 7, 2, 0, "\r\n    "), //
				te(2, 7, 3, 0, "\r\n    "), //
				te(3, 15, 4, 0, "\r\n  "));
		assertFormat(expected, expected);
	}

	// From issue: https://github.com/eclipse-lemminx/lemminx/pull/1845#issuecomment-5700561219
	// Use case (LF): same as above but with Unix line endings.
	// The trailing '\n' before </doc> is a single char, which previously left
	// spaceEnd=-1, causing the closing tag indentation to be skipped.
	@Test
	public void normalizeSpaceEndTagIndentationLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc>\n" + //
				"Content\n" + //
				"multiple lines.\n" + //
				"</doc>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <doc>\n" + //
				"    Content\n" + //
				"    multiple lines.\n" + //
				"  </doc>\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(1, 7, 2, 0, "\n    "), //
				te(2, 7, 3, 0, "\n    "), //
				te(3, 15, 4, 0, "\n  "));
		assertFormat(expected, expected);
	}

	// Use case (#1026): multiple words with varying internal whitespace.
	// All spaces between words must be preserved.
	@Test
	public void textMultipleInternalSpaces() throws BadLocationException {
		String content = "<a>one  two   three    four</a>";
		String expected = "<a>one  two   three    four</a>";
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	// Use case (#1026): tab characters between text must be preserved.
	@Test
	public void textTabsPreserved() throws BadLocationException {
		String content = "<a>one\ttwo</a>";
		String expected = "<a>one\ttwo</a>";
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	// Use case (#1026): with joinContentLines=true, internal whitespace IS
	// collapsed to a single space.
	@Test
	public void textSpacesCollapsedWithJoinContentLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<a>b  c</a>";
		String expected = "<a>b c</a>";
		assertFormat(content, expected, settings, //
				te(0, 4, 0, 6, " "));
		assertFormat(expected, expected, settings);
	}

	// Use case (#1026): mixed content (text + child elements) collapses whitespace.
	// <a>b  c<b/></a> => <a>b c<b /></a>
	@Test
	public void textSpacesCollapsedInMixedContent() throws BadLocationException {
		String content = "<a>b  c<b/></a>";
		String expected = "<a>b c<b /></a>";
		assertFormat(content, expected, //
				te(0, 4, 0, 6, " "), //
				te(0, 9, 0, 9, " "));
		assertFormat(expected, expected);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1026
	// Use case (from issue): <Inner>   Test     Spaces   </Inner>
	// Internal whitespace between words is preserved; trailing whitespace
	// before the end tag is normalized to a single space.
	@Test
	public void issue1026InternalSpacesPreserved() throws BadLocationException {
		String content = "<Inner>   Test     Spaces   </Inner>";
		String expected = "<Inner>   Test     Spaces </Inner>";
		assertFormat(content, expected, //
				te(0, 25, 0, 28, " "));
		assertFormat(expected, expected);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1026
	// Use case (from issue): nested element with internal whitespace. (CRLF)
	@Test
	public void issue1026NestedInternalSpaces() throws BadLocationException {
		String content = "<Outer>\r\n" + //
				"  <Inner>   Test     Spaces   </Inner>\r\n" + //
				"</Outer>";
		String expected = "<Outer>\r\n" + //
				"  <Inner>   Test     Spaces </Inner>\r\n" + //
				"</Outer>";
		assertFormat(content, expected, //
				te(1, 27, 1, 30, " "));
		assertFormat(expected, expected);
	}

	// https://github.com/redhat-developer/vscode-xml/issues/1026
	// Use case (from issue): nested element with internal whitespace. (LF)
	@Test
	public void issue1026NestedInternalSpacesLF() throws BadLocationException {
		String content = "<Outer>\n" + //
				"  <Inner>   Test     Spaces   </Inner>\n" + //
				"</Outer>";
		String expected = "<Outer>\n" + //
				"  <Inner>   Test     Spaces </Inner>\n" + //
				"</Outer>";
		assertFormat(content, expected, //
				te(1, 27, 1, 30, " "));
		assertFormat(expected, expected);
	}

	// Use case: end tag indented with single line of text content.
	// <doc>\nContent\n</doc> => <doc>\n  Content\n</doc>
	@Test
	public void normalizeSpaceEndTagSingleLineContent() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"Content\r\n" + //
				"</doc>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"    Content\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(1, 7, 2, 0, "\r\n    "), //
				te(2, 7, 3, 0, "\r\n  "));
		assertFormat(expected, expected);
	}

	// Use case: end tag indented with single line of text content (LF).
	@Test
	public void normalizeSpaceEndTagSingleLineContentLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc>\n" + //
				"Content\n" + //
				"</doc>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <doc>\n" + //
				"    Content\n" + //
				"  </doc>\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(1, 7, 2, 0, "\n    "), //
				te(2, 7, 3, 0, "\n  "));
		assertFormat(expected, expected);
	}

	// Use case: text content on the same line as start tag — end tag stays inline.
	// <doc>Content</doc> => <doc>Content</doc> (no change)
	@Test
	public void normalizeSpaceEndTagInlineSameLine() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc>Content</doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Use case: deeply nested text-only elements with end tag indentation.
	@Test
	public void normalizeSpaceEndTagDeeplyNested() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    <c>\r\n" + //
				"Text\r\n" + //
				"</c>\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    <c>\r\n" + //
				"      Text\r\n" + //
				"    </c>\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(2, 7, 3, 0, "\r\n      "), //
				te(3, 4, 4, 0, "\r\n    "));
		assertFormat(expected, expected);
	}

	// Use case: deeply nested text-only elements with end tag indentation (LF).
	@Test
	public void normalizeSpaceEndTagDeeplyNestedLF() throws BadLocationException {
		String content = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"Text\n" + //
				"</c>\n" + //
				"  </b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"      Text\n" + //
				"    </c>\n" + //
				"  </b>\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(2, 7, 3, 0, "\n      "), //
				te(3, 4, 4, 0, "\n    "));
		assertFormat(expected, expected);
	}

	// Use case: element with only whitespace (newline) between tags.
	// The closing tag must be indented even when there is no real text content.
	// <b>\n</b> => <b>\n  </b>
	@Test
	public void emptyElementWithNewlineIndented() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"</b>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(1, 5, 2, 0, "\r\n  "));
		assertFormat(expected, expected);
	}

	// Use case: same as above with LF.
	@Test
	public void emptyElementWithNewlineIndentedLF() throws BadLocationException {
		String content = "<a>\n" + //
				"  <b>\n" + //
				"</b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"  </b>\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(1, 5, 2, 0, "\n  "));
		assertFormat(expected, expected);
	}

	// Use case (#1026): internal whitespace preserved with multi-line text.
	// Spaces within a line are kept, but newlines are re-indented.
	@Test
	public void multiLineTextWithInternalSpaces() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"hello  world\r\n" + //
				"foo   bar\r\n" + //
				"</doc>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"    hello  world\r\n" + //
				"    foo   bar\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(1, 7, 2, 0, "\r\n    "), //
				te(2, 12, 3, 0, "\r\n    "), //
				te(3, 9, 4, 0, "\r\n  "));
		assertFormat(expected, expected);
	}

	// Use case: already correctly indented — idempotent, no edits.
	@Test
	public void normalizeSpaceEndTagAlreadyIndented() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc>\r\n" + //
				"    Content\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Use case: already correctly indented with LF — idempotent, no edits.
	@Test
	public void normalizeSpaceEndTagAlreadyIndentedLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc>\n" + //
				"    Content\n" + //
				"  </doc>\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
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
