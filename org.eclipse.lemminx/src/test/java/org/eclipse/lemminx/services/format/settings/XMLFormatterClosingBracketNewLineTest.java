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
package org.eclipse.lemminx.services.format.settings;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter tests for closingBracketNewLine setting.
 *
 * <p>
 * Use case: when splitAttributes is enabled and there are multiple attributes,
 * the closing bracket (/&gt; or &gt;) is placed on a new line.
 * </p>
 *
 * <pre>
 * &lt;a b='' c=''/&gt;   →   &lt;a
 *                        b=''
 *                        c=''
 *                        /&gt;
 * </pre>
 */
public class XMLFormatterClosingBracketNewLineTest {

	// Use case: splitNewLine + closingBracketNewLine with indentSize=0
	// <a b='' c=''/> → <a\nb=''\nc=''\n/>
	@Test
	public void testClosingBracketNewLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='' c=''/>";
		String expected = "<a" + lineSeparator() + "b=''" + lineSeparator() + "c=''" + lineSeparator() + "/>";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, lineSeparator()), //
				te(0, 7, 0, 8, lineSeparator()), //
				te(0, 12, 0, 12, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Use case: splitNewLine + closingBracketNewLine with default indent (4)
	// <a b='b' c='c'/> → <a\n    b='b'\n    c='c'\n    />
	@Test
	public void testClosingBracketNewLineWithDefaultIndentSize() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);
		String content = "<a b='b' c='c'/>";
		String expected = "<a" + System.lineSeparator() + //
				"    b='b'" + System.lineSeparator() + //
				"    c='c'" + System.lineSeparator() + //
				"    />";
		assertFormat(content, expected, settings, //
				te(0, 2, 0, 3, lineSeparator() + "    "), //
				te(0, 8, 0, 9, lineSeparator() + "    "), //
				te(0, 14, 0, 14, lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	// Use case: alignWithFirstAttr + closingBracketNewLine
	// <a b='b' c='c'/> → <a b='b'\n   c='c'\n   />
	@Test
	public void testClosingBracketNewLineWithAlignWithFirstAttr() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='b' c='c'/>";
		String expected = "<a b='b'" + System.lineSeparator() + //
				"   c='c'" + System.lineSeparator() + //
				"   />";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, lineSeparator() + "   "), //
				te(0, 14, 0, 14, lineSeparator() + "   "));
		assertFormat(expected, expected, settings);
	}

	// Use case: alignWithFirstAttr + closingBracketNewLine with nested elements
	// Nested element <b> gets its own alignment based on indent level
	@Test
	public void testClosingBracketNewLineWithAlignWithFirstAttrNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='b' c='c'>\n" + //
				"  <b c='c' d='d'/>\n" + //
				"</a>";
		String expected = "<a b='b'\n" + //
				"   c='c'\n" + //
				"   >\n" + //
				"  <b c='c'\n" + //
				"     d='d'\n" + //
				"     />\n" + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 8, 0, 9, "\n   "), //
				te(0, 14, 0, 14, "\n   "), //
				te(1, 10, 1, 11, "\n     "), //
				te(1, 16, 1, 16, "\n     "));
		assertFormat(expected, expected, settings);
	}

	// Use case: closingBracketNewLine without splitAttributes — no effect,
	// attributes stay on same line
	@Test
	public void testClosingBracketNewLineWithoutSplitAttributes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b='' c=''/>";
		String expected = "<a b='' c='' />";
		assertFormat(content, expected, settings, //
				te(0, 12, 0, 12, " "));
		assertFormat(expected, expected, settings);
	}

	// Use case: closingBracketNewLine with single attribute — no split,
	// closing bracket stays on same line
	@Test
	public void testClosingBracketNewLineWithSingleAttribute() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a b=''/>";
		String expected = "<a b='' />";
		assertFormat(content, expected, settings, te(0, 7, 0, 7, " "));
		assertFormat(expected, expected, settings);
	}

	// Use case: closingBracketNewLine with child element — closing bracket
	// aligned with child's indent level
	@Test
	public void testClosingBracketNewLineWithChildElementIndent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + //
				"  <b c='' d=''/>" + lineSeparator() + //
				"</a>";
		String expected = "<a>" + lineSeparator() + //
				"  <b" + lineSeparator() + //
				"  c=''" + lineSeparator() + //
				"  d=''" + lineSeparator() + //
				"  />" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(1, 4, 1, 5, lineSeparator() + "  "), //
				te(1, 9, 1, 10, lineSeparator() + "  "), //
				te(1, 14, 1, 14, lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Use case: closingBracketNewLine + preserveEmptyContent — closing bracket
	// on new line, empty content between start/end tags preserved
	@Test
	public void testClosingBracketNewLineWithPreserveEmptyContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setPreserveEmptyContent(true);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + //
				"<b c='' d=''></b>" + lineSeparator() + //
				"</a>";
		String expected = "<a>" + lineSeparator() + //
				"  <b" + lineSeparator() + //
				"  c=''" + lineSeparator() + //
				"  d=''" + lineSeparator() + //
				"  ></b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, lineSeparator() + "  "), //
				te(1, 2, 1, 3, lineSeparator() + "  "), //
				te(1, 7, 1, 8, lineSeparator() + "  "), //
				te(1, 12, 1, 12, lineSeparator() + "  "));
		assertFormat(expected, expected, settings);
	}

	// Use case: closingBracketNewLine + preserveEmptyContent + single attribute
	// — no split, closing bracket stays on same line
	@Test
	public void testClosingBracketNewLineWithPreserveEmptyContentSingleAttribute() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(0);
		settings.getFormattingSettings().setPreserveEmptyContent(true);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<a>" + lineSeparator() + //
				"<b></b>" + lineSeparator() + //
				"</a>";
		String expected = "<a>" + lineSeparator() + //
				"  <b></b>" + lineSeparator() + //
				"</a>";
		assertFormat(content, expected, settings, //
				te(0, 3, 1, 0, lineSeparator() + "  "));
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
