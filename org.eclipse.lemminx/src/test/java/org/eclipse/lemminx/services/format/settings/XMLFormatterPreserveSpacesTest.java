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
	@Test
	public void preserveSpaceEndTagIndentationSameLine() throws BadLocationException {
		String content = "<root>\r\n" + //
				"  <doc xml:space=\"preserve\">Content</doc>\r\n" + //
				"</root>";
		String expected = content;
		assertFormat(content, expected);
		assertFormat(expected, expected);
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
