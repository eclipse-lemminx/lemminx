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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * Tests for the new {@code splitAttributes} values that follow VS Code HTML's
 * {@code html.format.wrapAttributes} naming convention.
 *
 * <p>
 * These values were added for consistency with VS Code HTML, so users
 * familiar with {@code html.format.wrapAttributes} can use the same
 * names: {@code auto}, {@code force}, {@code force-aligned},
 * {@code force-expand-multiline}, {@code aligned-multiple}, and
 * {@code preserve-aligned}.
 * </p>
 *
 * <p>
 * The legacy values {@code splitNewLine} and {@code alignWithFirstAttr}
 * remain as deprecated aliases for {@code force-expand-multiline} and
 * {@code force-aligned} respectively.
 * </p>
 */
public class XMLFormatterWrapAttributesTest extends AbstractCacheBasedTest {

	// ==========================================
	// force: wrap all except first, indent
	// ==========================================

	@Test
	public void testForce() throws BadLocationException {
		// force: first attribute stays on the tag line, rest on new lines with indent
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();
		String expected = "<root a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 12, 0, 13, lineSeparator() + "    "), //
				te(0, 18, 0, 19, lineSeparator() + "    "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceSingleAttribute() throws BadLocationException {
		// force with a single attribute: stays on the same line (no wrapping)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root  a='a' />" + lineSeparator();
		String expected = "<root a='a' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 15, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceNested() throws BadLocationException {
		// force with nested elements
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root a='a' b='b'>" + lineSeparator() + //
				"  <child c='c' d='d' e='e' />" + lineSeparator() + //
				"</root>";
		String expected = "<root a='a'" + lineSeparator() + //
				"    b='b'>" + lineSeparator() + //
				"  <child c='c'" + lineSeparator() + //
				"      d='d'" + lineSeparator() + //
				"      e='e' />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(0, 11, 0, 12, lineSeparator() + "    "), //
				te(1, 14, 1, 15, lineSeparator() + "      "), //
				te(1, 20, 1, 21, lineSeparator() + "      "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceTwoAttributes() throws BadLocationException {
		// force with 2 attributes: second wraps, first stays on tag line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root a='a' b='b' />" + lineSeparator();
		String expected = "<root a='a'" + lineSeparator() + //
				"    b='b' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceNoAttribute() throws BadLocationException {
		// force with 0 attributes: no change
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root />";
		String expected = "<root />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
	}

	@Test
	public void testForceWithTextContent() throws BadLocationException {
		// force with text content: attributes wrap, text stays
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<root a='a' b='b'>text</root>";
		String expected = "<root a='a'" + lineSeparator() + //
				"    b='b'>text" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceWithIndentSize1() throws BadLocationException {
		// force with splitAttributesIndentSize=1
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);
		settings.getFormattingSettings().setSplitAttributesIndentSize(1);

		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();
		String expected = "<root a='a'" + lineSeparator() + //
				"  b='b'" + lineSeparator() + //
				"  c='c' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 12, 0, 13, lineSeparator() + "  "), //
				te(0, 18, 0, 19, lineSeparator() + "  "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// force-expand-multiline: wrap ALL including first
	// (equivalent to legacy splitNewLine)
	// ==========================================

	@Test
	public void testForceExpandMultiline() throws BadLocationException {
		// force-expand-multiline: ALL attributes including the first go on new lines
		// This is equivalent to the legacy splitNewLine behavior.
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();
		String expected = "<root" + lineSeparator() + //
				"    a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, lineSeparator() + "    "), //
				te(0, 12, 0, 13, lineSeparator() + "    "), //
				te(0, 18, 0, 19, lineSeparator() + "    "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceExpandMultilineSingleAttribute() throws BadLocationException {
		// force-expand-multiline with a single attribute: stays on same line
		// (matches splitNewLine behavior)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String content = "<root  a='a' />" + lineSeparator();
		String expected = "<root a='a' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 15, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceExpandMultilineNested() throws BadLocationException {
		// force-expand-multiline with nested: all attributes (including first) on new lines
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String content = "<root a='a' b='b'>" + lineSeparator() + //
				"  <child c='c' d='d' />" + lineSeparator() + //
				"</root>";
		String expected = "<root" + lineSeparator() + //
				"    a='a'" + lineSeparator() + //
				"    b='b'>" + lineSeparator() + //
				"  <child" + lineSeparator() + //
				"      c='c'" + lineSeparator() + //
				"      d='d' />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceExpandMultilineMatchesSplitNewLine() throws BadLocationException {
		// Verify force-expand-multiline produces identical output to splitNewLine
		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();

		SharedSettings legacy = new SharedSettings();
		legacy.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);

		SharedSettings newStyle = new SharedSettings();
		newStyle.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String expected = "<root" + lineSeparator() + //
				"    a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c' />";

		assertFormat(content, expected, legacy, //
				te(0, 5, 0, 7, lineSeparator() + "    "), //
				te(0, 12, 0, 13, lineSeparator() + "    "), //
				te(0, 18, 0, 19, lineSeparator() + "    "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(content, expected, newStyle, //
				te(0, 5, 0, 7, lineSeparator() + "    "), //
				te(0, 12, 0, 13, lineSeparator() + "    "), //
				te(0, 18, 0, 19, lineSeparator() + "    "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
	}

	// ==========================================
	// force-aligned: wrap all except first, align with first attr
	// (equivalent to legacy alignWithFirstAttr)
	// ==========================================

	@Test
	public void testForceAligned() throws BadLocationException {
		// force-aligned: first attr stays on tag line, rest align with it.
		// This is equivalent to the legacy alignWithFirstAttr behavior.
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();
		String expected = "<root a='a'" + lineSeparator() + //
				"      b='b'" + lineSeparator() + //
				"      c='c' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 12, 0, 13, lineSeparator() + "      "), //
				te(0, 18, 0, 19, lineSeparator() + "      "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceAlignedNested() throws BadLocationException {
		// force-aligned with nested: attributes align with first attr at each level
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<root a='a' b='b'>" + lineSeparator() + //
				"  <child c='c' d='d' />" + lineSeparator() + //
				"</root>";
		String expected = "<root a='a'" + lineSeparator() + //
				"      b='b'>" + lineSeparator() + //
				"  <child c='c'" + lineSeparator() + //
				"         d='d' />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceAlignedSingleAttribute() throws BadLocationException {
		// force-aligned with a single attribute: stays on same line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<root  a='a' />" + lineSeparator();
		String expected = "<root a='a' />";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 7, " "), //
				te(0, 15, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceAlignedMatchesAlignWithFirstAttr() throws BadLocationException {
		// Verify force-aligned produces identical output to alignWithFirstAttr
		String content = "<root  a='a' b='b' c='c'/>" + lineSeparator();

		SharedSettings legacy = new SharedSettings();
		legacy.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);

		SharedSettings newStyle = new SharedSettings();
		newStyle.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String expected = "<root a='a'" + lineSeparator() + //
				"      b='b'" + lineSeparator() + //
				"      c='c' />";

		assertFormat(content, expected, legacy, //
				te(0, 5, 0, 7, " "), //
				te(0, 12, 0, 13, lineSeparator() + "      "), //
				te(0, 18, 0, 19, lineSeparator() + "      "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
		assertFormat(content, expected, newStyle, //
				te(0, 5, 0, 7, " "), //
				te(0, 12, 0, 13, lineSeparator() + "      "), //
				te(0, 18, 0, 19, lineSeparator() + "      "), //
				te(0, 24, 0, 24, " "), //
				te(0, 26, 1, 0, ""));
	}

	// ==========================================
	// auto: wrap when exceeds maxLineWidth, indent
	// ==========================================

	@Test
	public void testAutoFitsOnOneLine() throws BadLocationException {
		// auto: all attributes fit on one line -> no wrapping
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<root a='a' b='b' />" + lineSeparator();
		String expected = "<root a='a' b='b' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAutoExceedsWidth() throws BadLocationException {
		// auto: attributes exceed maxLineWidth -> wrap
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(30);

		String content = "<root attr1='value1' attr2='value2' attr3='value3' />" + lineSeparator();
		String expected = "<root attr1='value1'" + lineSeparator() + //
				"    attr2='value2'" + lineSeparator() + //
				"    attr3='value3' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAutoExceedsWidthNested() throws BadLocationException {
		// auto with nested elements: only wraps where needed
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(40);

		String content = "<root a='a'>" + lineSeparator() + //
				"  <child attr1='value1' attr2='value2' attr3='value3' />" + lineSeparator() + //
				"</root>";
		String expected = "<root a='a'>" + lineSeparator() + //
				"  <child attr1='value1' attr2='value2'" + lineSeparator() + //
				"      attr3='value3' />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAutoNoMaxLineWidth() throws BadLocationException {
		// auto without maxLineWidth set -> no wrapping (like preserve)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);

		String content = "<root attr1='value1' attr2='value2' attr3='value3' />" + lineSeparator();
		String expected = "<root attr1='value1' attr2='value2' attr3='value3' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// aligned-multiple: wrap when exceeds width, align with first
	// ==========================================

	@Test
	public void testAlignedMultipleFitsOnOneLine() throws BadLocationException {
		// aligned-multiple: all attributes fit -> no wrapping
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignedMultiple);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<root a='a' b='b' />" + lineSeparator();
		String expected = "<root a='a' b='b' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAlignedMultipleExceedsWidth() throws BadLocationException {
		// aligned-multiple: exceeds maxLineWidth -> wrap and align with first attribute
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignedMultiple);
		settings.getFormattingSettings().setMaxLineWidth(30);

		String content = "<root attr1='value1' attr2='value2' attr3='value3' />" + lineSeparator();
		String expected = "<root attr1='value1'" + lineSeparator() + //
				"      attr2='value2'" + lineSeparator() + //
				"      attr3='value3' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// preserve-aligned: preserve breaks, align with first
	// ==========================================

	@Test
	public void testPreserveAligned() throws BadLocationException {
		// preserve-aligned: preserve existing line breaks, align with first attribute
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserveAligned);
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);

		String content = "<root attr1='a'" + lineSeparator() + //
				"  attr2='b'" + lineSeparator() + //
				"  attr3='c' />" + lineSeparator();
		String expected = "<root attr1='a'" + lineSeparator() + //
				"      attr2='b'" + lineSeparator() + //
				"      attr3='c' />";
		assertFormat(content, expected, settings, //
				te(0, 15, 1, 2, lineSeparator() + "      "), //
				te(1, 11, 2, 2, lineSeparator() + "      "), //
				te(2, 14, 3, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testPreserveAlignedNoLineBreaks() throws BadLocationException {
		// preserve-aligned without existing line breaks -> stays on one line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserveAligned);
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);

		String content = "<root attr1='a' attr2='b' />" + lineSeparator();
		String expected = "<root attr1='a' attr2='b' />";
		assertFormat(content, expected, settings, //
				te(0, 28, 1, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testPreserveAlignedNested() throws BadLocationException {
		// preserve-aligned with nested elements
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserveAligned);
		settings.getFormattingSettings().setPreserveAttributeLineBreaks(true);

		String content = "<root a='a'>" + lineSeparator() + //
				"  <child attr1='v1'" + lineSeparator() + //
				"  attr2='v2'" + lineSeparator() + //
				"  attr3='v3' />" + lineSeparator() + //
				"</root>";
		String expected = "<root a='a'>" + lineSeparator() + //
				"  <child attr1='v1'" + lineSeparator() + //
				"         attr2='v2'" + lineSeparator() + //
				"         attr3='v3' />" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(1, 19, 2, 2, lineSeparator() + "         "), //
				te(2, 12, 3, 2, lineSeparator() + "         "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testPreserveAlignedOverflow() throws BadLocationException {
		// preserve-aligned + maxLineWidth: wraps on overflow and aligns with first attr
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserveAligned);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<root>" + lineSeparator() + //
				"  <element val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6' val7='val7' val8='val8' val9='val9'>" + lineSeparator() + //
				"  </element>" + lineSeparator() + //
				"</root>";
		String expected = "<root>" + lineSeparator() + //
				"  <element val1='val1' val2='val2' val3='val3' val4='val4' val5='val5'" + lineSeparator() + //
				"           val6='val6' val7='val7' val8='val8' val9='val9'>" + lineSeparator() + //
				"  </element>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testPreserveOverflow() throws BadLocationException {
		// preserve + maxLineWidth: wraps on overflow with indent (not aligned)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<root>" + lineSeparator() + //
				"  <element val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6' val7='val7' val8='val8' val9='val9'>" + lineSeparator() + //
				"  </element>" + lineSeparator() + //
				"</root>";
		String expected = "<root>" + lineSeparator() + //
				"  <element val1='val1' val2='val2' val3='val3' val4='val4' val5='val5'" + lineSeparator() + //
				"    val6='val6' val7='val7' val8='val8' val9='val9'>" + lineSeparator() + //
				"  </element>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// force vs force-expand-multiline: first attr behavior
	// ==========================================

	@Test
	public void testForceVsForceExpandMultiline() throws BadLocationException {
		// force keeps first attr on tag line, force-expand-multiline moves it
		String content = "<root a='a' b='b' c='c'/>" + lineSeparator();

		SharedSettings forceSettings = new SharedSettings();
		forceSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		SharedSettings expandSettings = new SharedSettings();
		expandSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		// force: first attr stays
		String expectedForce = "<root a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c' />";

		// force-expand-multiline: first attr on new line
		String expectedExpand = "<root" + lineSeparator() + //
				"    a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c' />";

		assertFormat(content, expectedForce, forceSettings, //
				te(0, 11, 0, 12, lineSeparator() + "    "), //
				te(0, 17, 0, 18, lineSeparator() + "    "), //
				te(0, 23, 0, 23, " "), //
				te(0, 25, 1, 0, ""));
		assertFormat(content, expectedExpand, expandSettings, //
				te(0, 5, 0, 6, lineSeparator() + "    "), //
				te(0, 11, 0, 12, lineSeparator() + "    "), //
				te(0, 17, 0, 18, lineSeparator() + "    "), //
				te(0, 23, 0, 23, " "), //
				te(0, 25, 1, 0, ""));
	}

	// ==========================================
	// Enum fromString: hyphenated JSON values
	// ==========================================

	@Test
	public void testFromStringHyphenated() {
		// Verify that hyphenated names from JSON are parsed correctly
		assertEquals(SplitAttributes.forceAligned, SplitAttributes.fromString("force-aligned"));
		assertEquals(SplitAttributes.forceExpandMultiline, SplitAttributes.fromString("force-expand-multiline"));
		assertEquals(SplitAttributes.alignedMultiple, SplitAttributes.fromString("aligned-multiple"));
		assertEquals(SplitAttributes.preserveAligned, SplitAttributes.fromString("preserve-aligned"));
	}

	@Test
	public void testFromStringCamelCase() {
		// Verify that camelCase names also work
		assertEquals(SplitAttributes.forceAligned, SplitAttributes.fromString("forceAligned"));
		assertEquals(SplitAttributes.forceExpandMultiline, SplitAttributes.fromString("forceExpandMultiline"));
		assertEquals(SplitAttributes.alignedMultiple, SplitAttributes.fromString("alignedMultiple"));
		assertEquals(SplitAttributes.preserveAligned, SplitAttributes.fromString("preserveAligned"));
	}

	@Test
	public void testFromStringLegacy() {
		// Legacy values still work
		assertEquals(SplitAttributes.preserve, SplitAttributes.fromString("preserve"));
		assertEquals(SplitAttributes.splitNewLine, SplitAttributes.fromString("splitNewLine"));
		assertEquals(SplitAttributes.alignWithFirstAttr, SplitAttributes.fromString("alignWithFirstAttr"));
	}

	@Test
	public void testFromStringUnknown() {
		// Unknown values fall back to preserve
		assertEquals(SplitAttributes.preserve, SplitAttributes.fromString("unknown"));
		assertEquals(SplitAttributes.preserve, SplitAttributes.fromString(null));
	}

	// ==========================================
	// closingBracketNewLine interactions
	// ==========================================

	@Test
	public void testForceWithClosingBracketNewLine() throws BadLocationException {
		// force + closingBracketNewLine: closing bracket on its own line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);
		settings.getFormattingSettings().setClosingBracketNewLine(true);

		String content = "<root a='a' b='b' c='c'>" + lineSeparator() + "</root>";
		String expected = "<root a='a'" + lineSeparator() + //
				"    b='b'" + lineSeparator() + //
				"    c='c'" + lineSeparator() + //
				"    >" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(0, 11, 0, 12, lineSeparator() + "    "), //
				te(0, 17, 0, 18, lineSeparator() + "    "), //
				te(0, 23, 0, 23, lineSeparator() + "    "));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testForceAlignedWithClosingBracketNewLine() throws BadLocationException {
		// force-aligned + closingBracketNewLine: closing bracket aligned
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);
		settings.getFormattingSettings().setClosingBracketNewLine(true);

		String content = "<root a='a' b='b' c='c'>" + lineSeparator() + "</root>";
		String expected = "<root a='a'" + lineSeparator() + //
				"      b='b'" + lineSeparator() + //
				"      c='c'" + lineSeparator() + //
				"      >" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings, //
				te(0, 11, 0, 12, lineSeparator() + "      "), //
				te(0, 17, 0, 18, lineSeparator() + "      "), //
				te(0, 23, 0, 23, lineSeparator() + "      "));
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Issue #273: Improve attribute formatting preferences
	// https://github.com/eclipse/lemminx/issues/273
	// Request: same wrapAttributes enum as VS Code HTML extension
	// ==========================================

	@Test
	public void testIssue273_allModesAvailable() {
		// #273 requested all wrapAttributes modes from VS Code HTML.
		// Verify all enum values can be parsed from their JSON names.
		assertEquals(SplitAttributes.auto, SplitAttributes.fromString("auto"));
		assertEquals(SplitAttributes.force, SplitAttributes.fromString("force"));
		assertEquals(SplitAttributes.forceAligned, SplitAttributes.fromString("force-aligned"));
		assertEquals(SplitAttributes.forceExpandMultiline, SplitAttributes.fromString("force-expand-multiline"));
		assertEquals(SplitAttributes.alignedMultiple, SplitAttributes.fromString("aligned-multiple"));
		assertEquals(SplitAttributes.preserveAligned, SplitAttributes.fromString("preserve-aligned"));
		assertEquals(SplitAttributes.preserve, SplitAttributes.fromString("preserve"));
	}

	@Test
	public void testIssue273_forceExample() throws BadLocationException {
		// #273: "force" — each attribute except first on its own line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<div id='main' class='container' style='color:red'></div>";
		String expected = "<div id='main'" + lineSeparator() + //
				"    class='container'" + lineSeparator() + //
				"    style='color:red'></div>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue273_forceExpandMultilineExample() throws BadLocationException {
		// #273: "force-expand-multiline" — ALL attributes on new lines (legacy splitNewLine)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String content = "<div id='main' class='container' style='color:red'></div>";
		String expected = "<div" + lineSeparator() + //
				"    id='main'" + lineSeparator() + //
				"    class='container'" + lineSeparator() + //
				"    style='color:red'></div>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue273_autoWrapsOnlyWhenNeeded() throws BadLocationException {
		// #273: "auto" — wrap only when maxLineWidth exceeded
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(40);

		// Fits on one line (< 40 chars)
		String shortContent = "<div id='main'></div>";
		assertFormat(shortContent, shortContent, settings);

		// Exceeds maxLineWidth
		String longContent = "<div id='main' class='container' style='color:red'></div>";
		String expected = "<div id='main' class='container'" + lineSeparator() + //
				"    style='color:red'></div>";
		assertFormat(longContent, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Issue #631: Improve splitAttributes
	// https://github.com/eclipse/lemminx/issues/631
	// Request: first attr on same line as tag, rest aligned with it
	// ==========================================

	@Test
	public void testIssue631_firstAttrOnTagLine() throws BadLocationException {
		// #631: keep first attribute on the tag line, align rest with it.
		// This is exactly what force-aligned does.
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<configuration attr1='value1' attr2='value2' attr3='value3' />";
		String expected = "<configuration attr1='value1'" + lineSeparator() + //
				"               attr2='value2'" + lineSeparator() + //
				"               attr3='value3' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue631_vsCurrentSplitNewLine() throws BadLocationException {
		// #631: the issue contrasts current splitNewLine (ALL on new line)
		// with desired behavior (first stays). Verify the difference.
		String content = "<elem attr1='value1' attr2='value2' />";

		// splitNewLine: ALL on new lines
		SharedSettings expandSettings = new SharedSettings();
		expandSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String expectedExpand = "<elem" + lineSeparator() + //
				"    attr1='value1'" + lineSeparator() + //
				"    attr2='value2' />";

		// force-aligned (#631 request): first stays, rest align
		SharedSettings alignedSettings = new SharedSettings();
		alignedSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String expectedAligned = "<elem attr1='value1'" + lineSeparator() + //
				"      attr2='value2' />";

		assertFormat(content, expectedExpand, expandSettings, (TextEdit[]) null);
		assertFormat(content, expectedAligned, alignedSettings, (TextEdit[]) null);
	}

	@Test
	public void testIssue631_nestedElements() throws BadLocationException {
		// #631: force-aligned with nested elements — each level aligns independently
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<project version='4'>" + lineSeparator() + //
				"  <component name='comp' type='main' scope='global' />" + lineSeparator() + //
				"</project>";
		String expected = "<project version='4'>" + lineSeparator() + //
				"  <component name='comp'" + lineSeparator() + //
				"             type='main'" + lineSeparator() + //
				"             scope='global' />" + lineSeparator() + //
				"</project>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Issue #965: Conditional splitAttributes
	// https://github.com/eclipse/lemminx/issues/965
	// Request: split only when line exceeds maxLineWidth
	// ==========================================

	@Test
	public void testIssue965_conditionalWrapShortLine() throws BadLocationException {
		// #965: short lines should NOT be wrapped (auto mode)
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<bean id='myBean' class='com.example.MyClass' />";
		String expected = "<bean id='myBean' class='com.example.MyClass' />";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue965_conditionalWrapLongLine() throws BadLocationException {
		// #965: long lines should be wrapped when they exceed maxLineWidth
		// maxLineWidth=40: "id" fits (17 < 40), "class" overflows (45 > 40) → wraps
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(40);

		String content = "<bean id='myBean' class='com.example.MyClass' scope='singleton' lazy-init='true' />";
		String expected = "<bean id='myBean'" + lineSeparator() + //
				"    class='com.example.MyClass'" + lineSeparator() + //
				"    scope='singleton' lazy-init='true' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue965_conditionalAlignedMultiple() throws BadLocationException {
		// #965: aligned-multiple — conditional wrap + alignment with first attr
		// maxLineWidth=40: "id" fits (17 < 40), "class" overflows (45 > 40) → wraps aligned
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignedMultiple);
		settings.getFormattingSettings().setMaxLineWidth(40);

		String content = "<bean id='myBean' class='com.example.MyClass' scope='singleton' lazy-init='true' />";
		String expected = "<bean id='myBean'" + lineSeparator() + //
				"      class='com.example.MyClass'" + lineSeparator() + //
				"      scope='singleton' lazy-init='true' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Issue #984: splitAttributes mixes orthogonal configurations
	// https://github.com/eclipse/lemminx/issues/984
	// Request: independent control of when/how/first-attr
	// ==========================================

	@Test
	public void testIssue984_alwaysSplitWithIndent() throws BadLocationException {
		// #984 aspect 1: "when=always" + "how=indent" = force
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		String content = "<mapper namespace='com.example.UserMapper' resultType='User' />";
		String expected = "<mapper namespace='com.example.UserMapper'" + lineSeparator() + //
				"    resultType='User' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue984_alwaysSplitWithAlign() throws BadLocationException {
		// #984 aspect 2: "when=always" + "how=align" = force-aligned
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		String content = "<mapper namespace='com.example.UserMapper' resultType='User' />";
		String expected = "<mapper namespace='com.example.UserMapper'" + lineSeparator() + //
				"        resultType='User' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue984_alwaysSplitExpandFirst() throws BadLocationException {
		// #984 aspect 3: "when=always" + "first-attr=new-line" = force-expand-multiline
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		String content = "<mapper namespace='com.example.UserMapper' resultType='User' />";
		String expected = "<mapper" + lineSeparator() + //
				"    namespace='com.example.UserMapper'" + lineSeparator() + //
				"    resultType='User' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue984_conditionalSplitWithIndent() throws BadLocationException {
		// #984 aspect 4: "when=overflow" + "how=indent" = auto
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(50);

		// Short line: no wrapping
		String shortContent = "<bean id='a' />";
		assertFormat(shortContent, shortContent, settings);

		// Long line: namespace fits (42 < 50), resultType overflows → wraps
		String longContent = "<mapper namespace='com.example.UserMapper' resultType='User' />";
		String expected = "<mapper namespace='com.example.UserMapper'" + lineSeparator() + //
				"    resultType='User' />";
		assertFormat(longContent, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testIssue984_conditionalSplitWithAlign() throws BadLocationException {
		// #984 aspect 5: "when=overflow" + "how=align" = aligned-multiple
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignedMultiple);
		settings.getFormattingSettings().setMaxLineWidth(50);

		// Short line: no wrapping
		String shortContent = "<bean id='a' />";
		assertFormat(shortContent, shortContent, settings);

		// Long line: namespace fits (42 < 50), resultType overflows → wraps aligned
		String longContent = "<mapper namespace='com.example.UserMapper' resultType='User' />";
		String expected = "<mapper namespace='com.example.UserMapper'" + lineSeparator() + //
				"        resultType='User' />";
		assertFormat(longContent, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Auto mode: HTML-like multi-attribute continuation lines
	// https://github.com/eclipse-lemminx/lemminx/pull/1870
	// After wrap, attributes that fit on the continuation line stay there
	// ==========================================

	@Test
	public void testAutoMultipleAttrsPerLine() throws BadLocationException {
		// Like HTML auto: after first wrap, short attrs continue on the same line
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(80);

		// 8 short attributes: first line fills to ~80, then wraps and continues
		String content = "<beans val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6' val7='val7' val8='val8'>" + lineSeparator() + "</beans>";
		String expected = "<beans val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6'" + lineSeparator() + //
				"    val7='val7' val8='val8'>" + lineSeparator() + //
				"</beans>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAlignedMultipleMultipleAttrsPerLine() throws BadLocationException {
		// aligned-multiple: after wrap, short attrs continue on the same line, aligned
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignedMultiple);
		settings.getFormattingSettings().setMaxLineWidth(80);

		String content = "<beans val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6' val7='val7' val8='val8'>" + lineSeparator() + "</beans>";
		String expected = "<beans val1='val1' val2='val2' val3='val3' val4='val4' val5='val5' val6='val6'" + lineSeparator() + //
				"       val7='val7' val8='val8'>" + lineSeparator() + //
				"</beans>";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testAutoMultipleWraps() throws BadLocationException {
		// auto: multiple continuation lines, each filling up to maxLineWidth
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.auto);
		settings.getFormattingSettings().setMaxLineWidth(40);

		String content = "<el a1='v1' a2='v2' a3='v3' a4='v4' a5='v5' a6='v6' />";
		String expected = "<el a1='v1' a2='v2' a3='v3' a4='v4'" + lineSeparator() + //
				"    a5='v5' a6='v6' />";
		assertFormat(content, expected, settings, (TextEdit[]) null);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Helper methods
	// ==========================================

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
