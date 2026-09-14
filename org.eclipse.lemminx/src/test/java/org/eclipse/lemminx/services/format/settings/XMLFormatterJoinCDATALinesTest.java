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

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with join CDATA lines setting.
 *
 */
public class XMLFormatterJoinCDATALinesTest {

	@Test
	public void testCDATANotClosed() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"<![CDATA[ \r\n" + //
				"</foo>";
		SharedSettings settings = new SharedSettings();
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testCDATAWithRange() throws BadLocationException {
		String content = "<foo>\r\n" + //
				"  <![CDATA[ |<bar>|\r\n" + //
				"  </bar>\r\n" + //
				"  ]]>\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"  <![CDATA[ <bar>\r\n" + //
				"  </bar>\r\n" + //
				"  ]]>\r\n" + //
				"</foo>";
		SharedSettings settings = new SharedSettings();
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinCDATALinesSameLine() throws BadLocationException {
		String content = "<a>   <![CDATA[\r\n" + //
				"  <  \r\n" + //
				"]]>   </a>";
		String expected = "<a>   <![CDATA[<]]>   </a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		assertFormat(content, expected, settings, //
				te(0, 15, 1, 2, ""), //
				te(1, 3, 2, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinCDATALinesEmpty() throws BadLocationException {
		String content = "<a>   <![CDATA[\r\n" + //
				"    \r\n" + //
				"]]>   </a>";
		String expected = "<a>   <![CDATA[]]>   </a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		assertFormat(content, expected, settings, //
				te(0, 15, 2, 0, ""));
		assertFormat(expected, expected, settings);
	}

	// From issue: https://github.com/eclipse/lemminx/issues/1193
	@Test
	public void testJoinCDATALinesWithText() throws BadLocationException {
		String content = "<a>  x  <![CDATA[\r\n" + //
				"<\r\n" + //
				"]]> y  </a>";
		String expected = "<a>  x <![CDATA[<]]> y </a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		assertFormat(content, expected, settings, //
				te(0, 6, 0, 8, " "), //
				te(0, 17, 1, 0, ""), //
				te(1, 1, 2, 0, ""), //
				te(2, 5, 2, 7, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinCDATALines() throws BadLocationException {
		String content = "<a>\r\n" + //
				"<![CDATA[\r\n" + //
				"line 1\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"line 2\r\n" + //
				"line 3\r\n" + //
				"]]>   </a>";
		String expected = "<a>\r\n" + //
				"<![CDATA[line 1 line 2 line 3]]>   </a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		assertFormat(content, expected, settings, //
				te(1, 9, 2, 0, ""), //
				te(2, 6, 5, 0, " "), //
				te(5, 6, 6, 0, " "), //
				te(6, 6, 7, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinCDATALinesMultiLine() throws BadLocationException {
		String content = "<a>\r\n" + //
				"<![CDATA[\r\n" + //
				"line 1\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"line 2\r\n" + //
				"line 3 test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test test\r\n"
				+ //
				"]]>   </a>";
		String expected = "<a>\r\n" + //
				"<![CDATA[line 1 line 2 line 3 test test test test test test test test test test test\r\n" + //
				"  test test test test test test test test test test test test test test test\r\n" + //
				"  test test test test test test test test test]]>   </a>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinCDATALines(true);
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, //
				te(1, 9, 2, 0, ""), //
				te(2, 6, 5, 0, " "), //
				te(5, 6, 6, 0, " "), //
				te(6, 61, 6, 62, "\r\n  "), //
				te(6, 136, 6, 137, "\r\n  "), //
				te(6, 181, 7, 0, ""));
		assertFormat(expected, expected, settings);
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