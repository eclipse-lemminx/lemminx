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
import org.eclipse.lemminx.settings.XMLFormattingOptions.MixedContent;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with join content lines
 * setting.
 *
 */
public class XMLFormatterJoinContentLinesTest {
	@Test
	public void testPreserveEmptyContentWithJoinContentLines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("a"));
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>\n" + //
				"   xx  \n" + //
				"   yy  \n" + //
				"   <b>  </b>  \n" + //
				"</a>";
		String expected = content;
		assertFormat(content, expected, settings);
	}

	@Test
	public void testJoinContentLinesTrue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>\n" + //
				"   zz  \n" + //
				"   zz  " + //
				"</a>";
		String expected = "<a> zz zz </a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 3, " "),
				te(1, 5, 2, 3, " "),
				te(2, 5, 2, 7, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesTrue2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>zz zz zz</a>";
		String expected = "<a>zz zz zz</a>";
		assertFormat(content, expected, settings);
	}

	@Test
	public void testJoinContentLinesFalse() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(false);

		String content = "<a>\n" + //
				"   zz  \n" + //
				"   zz  </a>";
		String expected = "<a>\n" + //
				"  zz\n" + //
				"  zz </a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 3, "\n  "),
				te(1, 5, 2, 3, "\n  "),
				te(2, 5, 2, 7, " "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesFalseEmptyContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(false);

		String content = "<a>\n" + //
				"     \n" + //
				"     " + //
				"</a>";
		String expected = "<a>\n" + //
				"\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 2, 5, "\n\n"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithSiblingElementTrue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(true);

		String content = "<a>\n" + //
				"   zz  \n" + //
				"   zz  \n" + //
				"   <b>  </b>  \n" + //
				"</a>";
		String expected = "<a> zz zz <b> </b>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 3, " "),
				te(1, 5, 2, 3, " "),
				te(2, 5, 3, 3, " "),
				te(3, 6, 3, 8, " "),
				te(3, 12, 4, 0, "\n"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithSiblingElementFalse() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setPreserveEmptyContent(false);
		settings.getFormattingSettings().setJoinContentLines(false);

		String content = "<a>\n" + //
				"   zz  \n" + //
				"   zz  \n" + //
				"   <a>  </a>  \n" + //
				"</a>";
		String expected = "<a> zz zz <a> </a>\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 3, " "),
				te(1, 5, 2, 3, " "),
				te(2, 5, 3, 3, " "),
				te(3, 6, 3, 8, " "),
				te(3, 12, 4, 0, "\n"));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithSplitAttributes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);

		String content = "<text x=\"50%\" y=\"50%\" font-size=\"60\"> SVG </text>\n";
		String expected = "<text\n" + //
				"    x=\"50%\"\n" + //
				"    y=\"50%\"\n" + //
				"    font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithSplitAttributesMultilineContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);

		String content = "<text\n" + //
				"  x=\"50%\"\n" + //
				"  y=\"50%\"\n" + //
				"  font-size=\"60\"> SVG\n" + //
				"</text>";
		String expected = "<text\n" + //
				"    x=\"50%\"\n" + //
				"    y=\"50%\"\n" + //
				"    font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesFalseWithSplitAttributes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(false);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);

		String content = "<text x=\"50%\" y=\"50%\" font-size=\"60\"> SVG </text>\n";
		String expected = "<text\n" + //
				"    x=\"50%\"\n" + //
				"    y=\"50%\"\n" + //
				"    font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithAlignWithFirstAttrReflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);

		String content = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG </text>\n";
		String expected = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesFalseWithAlignWithFirstAttrReflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(false);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);

		String content = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG </text>\n";
		String expected = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesWithAlignWithFirstAttrNormalize() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		String content = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG </text>\n";
		String expected = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testJoinContentLinesFalseWithAlignWithFirstAttrNormalize() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(false);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setMixedContent(MixedContent.normalize);

		String content = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG </text>\n";
		String expected = "<text x=\"50%\"\n" + //
				"      y=\"50%\"\n" + //
				"      font-size=\"60\"> SVG\n" + //
				"</text>";
		XMLAssert.assertFormat(content, expected, settings);
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
