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

import static org.eclipse.lemminx.XMLAssert.te;

import java.util.Arrays;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with preserve spaces.
 *
 */
public class XMLFormatterPreserveSpacesTest {

	// Use case (#1026): internal whitespace preserved even without xml:space="preserve"
	// <a>b  c</a> => <a>b  c</a> (not <a>b c</a>)
	@Test
	public void noPreserveSpaces() throws BadLocationException {
		String content = "<a>b  c</a>";
		String expected = "<a>b  c</a>";
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	@Test
	public void preserveSpacesWithXmlSpace() throws BadLocationException {
		String content = "<a xml:space=\"preserve\">b  c</a>";
		String expected = content;
		assertFormat(content, expected);
	}

	@Test
	public void preserveSpacesWithXmlSpace2() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    c  <d></d>  e\r\n" + //
				"  </b>\r\n" + //
				"  <b xml:space=\"preserve\">\r\n" + //
				"    c  <d></d>  e\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b> c <d></d> e </b>\r\n" + //
				"  <b xml:space=\"preserve\">\r\n" + //
				"    c  <d></d>  e\r\n" + //
				"  </b>\r\n" + //
				"</a>";

		assertFormat(content, expected, //
				te(1, 5, 2, 4, " "), //
				te(2, 5, 2, 7, " "), //
				te(2, 14, 2, 16, " "), //
				te(2, 17, 3, 2, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void preserveSpacesWithSettings() throws BadLocationException {
		String content = "<a>b  c</a>";
		String expected = content;
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));
		assertFormat(content, expected, settings);
	}

	@Test
	public void preserveSpacesWithDefaultSettings() throws BadLocationException {
		String content = "<xsl:text>b \r\n" + //
				" c</xsl:text>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Tests from depreciated preserveEmptyContent setting
	@Test
	public void testPreserveEmptyContentTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));

		String content = "<a>\n" + //
				"     " + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testDontPreserveEmptyContentTag() throws BadLocationException {
		String content = "<a>\n" + //
				"     " + //
				"</a>";
		String expected = "<a>\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 1, 5, "\n"));
		assertFormat(expected, expected);
	}

	@Test
	public void testPreserveTextContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));

		String content = "<a>\n" + //
				"   aaa  " + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testPreserveTextContent2() throws BadLocationException {
		String content = "<a>\n" + //
				"   aaa   </a>";
		String expected = "<a>\n" + //
				"  aaa </a>";
		assertFormat(content, expected, //
				te(0, 3, 1, 3, "\n  "), //
				te(1, 6, 1, 9, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void testPreserveEmptyContentTagWithSiblings() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("b"));

		String content = "<a>\n" + //
				"     " + //
				"  <b>  </b>" + //
				"     " + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>  </b>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 7, "\n  "), //
				te(1, 16, 1, 21, "\n"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testPreserveEmptyContentTagWithSiblingContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a", "b"));

		String content = "<a>\n" + //
				"   zz     <b>  </b>tt     </a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testDontPreserveEmptyContentTagWithSiblingContent() throws BadLocationException {
		String content = "<a>\n" + //
				"   zz     <b>  </b>tt     </a>";
		String expected = "<a> zz <b> </b>tt </a>";
		assertFormat(content, expected, //
				te(0, 3, 1, 3, " "), //
				te(1, 5, 1, 10, " "), //
				te(1, 13, 1, 15, " "), //
				te(1, 21, 1, 26, " "));
		assertFormat(expected, expected);
	}

	@Test
	public void testPreserveEmptyContentTagWithSiblingWithComment() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a", "b"));

		String content = "<a>\n" + //
				"   zz    <b>  </b>tt <!-- Comment -->     </a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testPreserveEmptyContentTagWithSiblingWithNewLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a", "b"));

		String content = "<a>\n" + //
				"   zz    \n" + //
				"<b>\n" + //
				"  </b>\n" + //
				"tt <!-- Comment -->     </a>\n" + //
				"\n" + //
				"<c></c>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testDontPreserveEmptyContentTagWithSiblingWithComment() throws BadLocationException {
		String content = "<a>\n" + //
				"   zz    <b>  </b>tt <!-- Comment -->     </a>";
		String expected = "<a> zz <b> </b>tt <!-- Comment -->\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(0, 3, 1, 3, " "), //
				te(1, 5, 1, 9, " "), //
				te(1, 12, 1, 14, " "), //
				te(1, 37, 1, 42, "\n"));
		assertFormat(expected, expected);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case: xml:space="preserve" with closing tag on its own line.
	// Content is untouched but </doc> is indented to match <doc>.
	// <doc xml:space="preserve">\nContent\n</doc> => ...\n  </doc>
	@Test
	public void preserveSpaceEndTagIndentation() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">\r\n" + //
				"Content\r\n" + //
				"multiple lines.\r\n" + //
				"</doc>\r\n" + //
				"</root>";
		String expected = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">\r\n" + //
				"Content\r\n" + //
				"multiple lines.\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(4, 0, 4, 0, "  "));
		assertFormat(expected, expected);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case: xml:space="preserve" with closing tag on the same line as content.
	// <doc xml:space="preserve">Content</doc> => no change (tag stays inline)
	@Test
	public void preserveSpaceEndTagIndentationSameLine() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">Content</doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	// Use case (#1301): preserve space end tag indentation with LF line endings.
	// Same as preserveSpaceEndTagIndentation but with '\n' only.
	@Test
	public void preserveSpaceEndTagIndentationLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc xml:space=\"preserve\">\n" + //
				"Content\n" + //
				"multiple lines.\n" + //
				"</doc>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <doc xml:space=\"preserve\">\n" + //
				"Content\n" + //
				"multiple lines.\n" + //
				"  </doc>\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(4, 0, 4, 0, "  "));
		assertFormat(expected, expected);
	}

	// Use case (#1301): preserve space with single line content and LF.
	// Content on same line as start tag — end tag stays inline.
	@Test
	public void preserveSpaceEndTagIndentationSameLineLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc xml:space=\"preserve\">Content</doc>\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
	}

	// Use case (#1301): deeply nested preserve space — end tag indented
	// to match its opening tag at indent level 2.
	@Test
	public void preserveSpaceEndTagDeeplyNested() throws BadLocationException {
		String content = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    <c xml:space=\"preserve\">\r\n" + //
				"preserved content\r\n" + //
				"</c>\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		String expected = "<a>\r\n" + //
				"  <b>\r\n" + //
				"    <c xml:space=\"preserve\">\r\n" + //
				"preserved content\r\n" + //
				"    </c>\r\n" + //
				"  </b>\r\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(4, 0, 4, 0, "    "));
		assertFormat(expected, expected);
	}

	// Use case (#1301): deeply nested preserve space with LF.
	@Test
	public void preserveSpaceEndTagDeeplyNestedLF() throws BadLocationException {
		String content = "<a>\n" + //
				"  <b>\n" + //
				"    <c xml:space=\"preserve\">\n" + //
				"preserved content\n" + //
				"</c>\n" + //
				"  </b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"    <c xml:space=\"preserve\">\n" + //
				"preserved content\n" + //
				"    </c>\n" + //
				"  </b>\n" + //
				"</a>";
		assertFormat(content, expected, //
				te(4, 0, 4, 0, "    "));
		assertFormat(expected, expected);
	}

	// Use case (#1301): preserve space inherited from parent element.
	// Child element inherits xml:space="preserve" and end tag must be indented.
	@Test
	public void preserveSpaceInheritedEndTagIndentation() throws BadLocationException {
		String content = "<root xml:space=\"preserve\">\r\n" + //
				"  <child>\r\n" + //
				"content\r\n" + //
				"</child>\r\n" + //
				"</root>";
		String expected = "<root xml:space=\"preserve\">\r\n" + //
				"  <child>\r\n" + //
				"content\r\n" + //
				"  </child>\r\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(3, 0, 3, 0, "  "));
		assertFormat(expected, expected);
	}

	// Use case (#1301): preserve space inherited from parent with LF.
	@Test
	public void preserveSpaceInheritedEndTagIndentationLF() throws BadLocationException {
		String content = "<root xml:space=\"preserve\">\n" + //
				"  <child>\n" + //
				"content\n" + //
				"</child>\n" + //
				"</root>";
		String expected = "<root xml:space=\"preserve\">\n" + //
				"  <child>\n" + //
				"content\n" + //
				"  </child>\n" + //
				"</root>";
		assertFormat(content, expected, //
				te(3, 0, 3, 0, "  "));
		assertFormat(expected, expected);
	}

	// Use case (#1301): preserve space with end tag already correctly indented
	// — idempotent, no edits.
	@Test
	public void preserveSpaceEndTagAlreadyIndented() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">\r\n" + //
				"Content\r\n" + //
				"  </doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Use case (#1301): preserve space with end tag already indented, LF.
	@Test
	public void preserveSpaceEndTagAlreadyIndentedLF() throws BadLocationException {
		String content = "<root>\n" + //
				"  <doc xml:space=\"preserve\">\n" + //
				"Content\n" + //
				"  </doc>\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
	}

	// Use case (#1301): preserve space with multiple lines and end tag
	// with existing whitespace before it (not directly after newline).
	// The existing indentation should be left as-is.
	@Test
	public void preserveSpaceEndTagWithExistingIndentation() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">\r\n" + //
				"Content\r\n" + //
				"     </doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case (from issue): xs:documentation with preserveSpace setting.
	// Content is preserved but end tag </xs:documentation> on its own line
	// must be indented to match the opening tag. (CRLF)
	@Test
	public void issue1301EndTagIndentation() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("xs:documentation"));
		String content = "<xs:schema>\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"      <xs:documentation>\r\n" + //
				"        Content that spans\r\n" + //
				"        multiple lines.\r\n" + //
				"</xs:documentation>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"  </xs:complexType>\r\n" + //
				"</xs:schema>";
		String expected = "<xs:schema>\r\n" + //
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
				te(6, 0, 6, 0, "      "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case (from issue): same as above with LF line endings.
	@Test
	public void issue1301EndTagIndentationLF() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("xs:documentation"));
		String content = "<xs:schema>\n" + //
				"  <xs:complexType name=\"myType\">\n" + //
				"    <xs:annotation>\n" + //
				"      <xs:documentation>\n" + //
				"        Content that spans\n" + //
				"        multiple lines.\n" + //
				"</xs:documentation>\n" + //
				"    </xs:annotation>\n" + //
				"  </xs:complexType>\n" + //
				"</xs:schema>";
		String expected = "<xs:schema>\n" + //
				"  <xs:complexType name=\"myType\">\n" + //
				"    <xs:annotation>\n" + //
				"      <xs:documentation>\n" + //
				"        Content that spans\n" + //
				"        multiple lines.\n" + //
				"      </xs:documentation>\n" + //
				"    </xs:annotation>\n" + //
				"  </xs:complexType>\n" + //
				"</xs:schema>";
		assertFormat(content, expected, settings, //
				te(6, 0, 6, 0, "      "));
		assertFormat(expected, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case (from issue): end tag on same line as content — no change. (CRLF)
	@Test
	public void issue1301EndTagSameLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("xs:documentation"));
		String content = "<xs:schema>\r\n" + //
				"  <xs:complexType name=\"myType\">\r\n" + //
				"    <xs:annotation>\r\n" + //
				"      <xs:documentation>\r\n" + //
				"        Content that spans\r\n" + //
				"        multiple lines. </xs:documentation>\r\n" + //
				"    </xs:annotation>\r\n" + //
				"  </xs:complexType>\r\n" + //
				"</xs:schema>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1301
	// Use case (from issue): end tag on same line as content with LF.
	@Test
	public void issue1301EndTagSameLineLF() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("xs:documentation"));
		String content = "<xs:schema>\n" + //
				"  <xs:complexType name=\"myType\">\n" + //
				"    <xs:annotation>\n" + //
				"      <xs:documentation>\n" + //
				"        Content that spans\n" + //
				"        multiple lines. </xs:documentation>\n" + //
				"    </xs:annotation>\n" + //
				"  </xs:complexType>\n" + //
				"</xs:schema>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	private static void assertFormat(String unformatted, String actual, TextEdit... expectedEdits)
			throws BadLocationException {
		assertFormat(unformatted, actual, new SharedSettings(), expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "test.xml", expectedEdits);
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
