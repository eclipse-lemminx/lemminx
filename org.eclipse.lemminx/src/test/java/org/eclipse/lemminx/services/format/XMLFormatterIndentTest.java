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
