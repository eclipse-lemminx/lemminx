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
package org.eclipse.lemminx.services.format;

import static org.eclipse.lemminx.XMLAssert.assertOnTypeFormatting;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.junit.jupiter.api.Test;

/**
 * XML on type formatting tests.
 */
public class XMLOnTypeFormattingTest extends AbstractCacheBasedTest {

	// ---------- Tests for Enter inside start tag with splitAttributes: splitNewLine
	// splitNewLine indent = (indentLevel + splitAttributesIndentSize) * tabSize

	@Test
	public void enterInsideStartTagSplitNewLine() throws BadLocationException {
		// indent = (0 + 2) * 2 = 4 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterInsideStartTagSplitNewLineNested() throws BadLocationException {
		// indent = (1 + 2) * 2 = 6 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"      attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterInsideStartTagSplitNewLineIndentSize1() throws BadLocationException {
		// indent = (0 + 1) * 2 = 2 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(1);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"  attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterInsideStartTagSplitNewLineTabSize4() throws BadLocationException {
		// indent = (0 + 2) * 4 = 8 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"        attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for Enter inside start tag with splitAttributes: alignWithFirstAttr
	// alignWithFirstAttr indent = tabSize * indentLevel + tagName.length() + 2

	@Test
	public void enterInsideStartTagAlignWithFirstAttr() throws BadLocationException {
		// spaceCount = 2*0 + 4 + 2 = 6 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"      attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	@Test
	public void enterInsideStartTagAlignWithFirstAttrNested() throws BadLocationException {
		// spaceCount = 2*1 + 5 + 2 = 9 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"         attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, settings);
	}

	@Test
	public void enterInsideStartTagAlignWithFirstAttrDeeplyNested() throws BadLocationException {
		// spaceCount = 2*2 + 5 + 2 = 11 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root>\n" + //
				"  <parent>\n" + //
				"    <child attr1=\"value1\"\n" + //
				"attr2=\"value2\">\n" + //
				"    </child>\n" + //
				"  </parent>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <parent>\n" + //
				"    <child attr1=\"value1\"\n" + //
				"           attr2=\"value2\">\n" + //
				"    </child>\n" + //
				"  </parent>\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 3, 0, settings);
	}

	@Test
	public void enterAlignWithFirstAttrLongTagName() throws BadLocationException {
		// spaceCount = 2*0 + 18 + 2 = 20 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<myLongElementName attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<myLongElementName attr1=\"value1\"\n" + //
				"                   attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	// ---------- Tests for Enter inside start tag with splitAttributes: preserve
	// preserve indent = (indentLevel + 1) * tabSize

	@Test
	public void enterInsideStartTagPreserve() throws BadLocationException {
		// indent = (0+1) * 2 = 2 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"  attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	@Test
	public void enterInsideStartTagPreserveNested() throws BadLocationException {
		// indent = (1+1) * 2 = 4 spaces
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.preserve);
		String content = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <child attr1=\"value1\"\n" + //
				"    attr2=\"value2\">\n" + //
				"  </child>\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, settings);
	}

	// ---------- Tests for Enter between start and end tags

	@Test
	public void enterBetweenTags() throws BadLocationException {
		// contentIndent = (0+1) * 2 = 2, endTagIndent = 0
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  \n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsNested() throws BadLocationException {
		// contentIndent = (1+1) * 2 = 4, endTagIndent = (1) * 2 = 2
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <child>\n" + //
				"</child>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <child>\n" + //
				"    \n" + //
				"  </child>\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsWithContent() throws BadLocationException {
		// contentIndent = (0+1) * 2 = 2
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"text</root>";
		String expected = "<root>\n" + //
				"  text</root>";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	@Test
	public void enterBetweenTagsTabSize4() throws BadLocationException {
		// contentIndent = (0+1) * 4 = 4, endTagIndent = 0
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setTabSize(4);
		String content = "<root>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"    \n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsUseTabs() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setInsertSpaces(false);
		String content = "<root>\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"\t\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests with existing whitespace on the new line

	@Test
	public void enterWithExistingWhitespace() throws BadLocationException {
		// alignWithFirstAttr, root: spaceCount = 6
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"      attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	@Test
	public void enterWithTooMuchWhitespace() throws BadLocationException {
		// splitNewLine: indent = (0+2)*2 = 4
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				"                    attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests with closing bracket on the new line

	@Test
	public void enterBeforeCloseBracketNoSpace() throws BadLocationException {
		// > on its own line: indented as attribute (preserve: elementColumn + tabSize)
		SharedSettings settings = createSettings();
		String content = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				">\n" + //
				"</foo>";
		String expected = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"  >\n" + //
				"</foo>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterBeforeCloseBracketWithSpace() throws BadLocationException {
		// > with leading space: re-indented as attribute
		SharedSettings settings = createSettings();
		String content = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				" >\n" + //
				"</foo>";
		String expected = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"  >\n" + //
				"</foo>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterAfterLastAttributeWithCloseBracketOnNextLine() throws BadLocationException {
		// New line aligns with previous attribute line (2 spaces)
		SharedSettings settings = createSettings();
		String content = "<foo attr1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"\n" + //
				">\n" + //
				"</foo>";
		String expected = "<foo attr1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"  \n" + //
				">\n" + //
				"</foo>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterBeforeSelfCloseBracket() throws BadLocationException {
		// /> indented as attribute (preserve: elementColumn + tabSize = 0 + 2 = 2)
		SharedSettings settings = createSettings();
		String content = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"  />\n";
		String expected = "<foo att1=\"\"\n" + //
				"  attr2=\"\"\n" + //
				"  />\n";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterBeforeSelfCloseBracketNested() throws BadLocationException {
		// /> indented as attribute (preserve: elementColumn + tabSize = 2 + 2 = 4)
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <foo att1=\"\"\n" + //
				"    attr2=\"\"\n" + //
				"      />\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <foo att1=\"\"\n" + //
				"    attr2=\"\"\n" + //
				"    />\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 3, 0, "\n", settings, false);
	}

	@Test
	public void enterAtEndOfAttributeSplitNewLine() throws BadLocationException {
		// > indented as attribute (splitNewLine: elementColumn + indentSize * tabSize = 0 + 2*2 = 4)
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				">";
		String expected = "<root attr1=\"value1\"\n" + //
				"    >";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterAtEndOfAttributeAlignWithFirstAttr() throws BadLocationException {
		// > indented as attribute (alignWithFirstAttr: elementColumn + tagName + 2 = 0 + 4 + 2 = 6)
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root attr1=\"value1\"\n" + //
				">";
		String expected = "<root attr1=\"value1\"\n" + //
				"      >";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for formatting disabled

	@Test
	public void noFormattingWhenDisabled() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setEnabled(false);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\">";
		assertOnTypeFormatting(content, content, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for self-closing tags

	@Test
	public void enterInsideSelfClosingTagSplitNewLine() throws BadLocationException {
		// indent = (0+2)*2 = 4
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\" />";
		String expected = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\" />";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterInsideSelfClosingTagAlignWithFirstAttr() throws BadLocationException {
		// spaceCount = 6
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root attr1=\"value1\"\n" + //
				"attr2=\"value2\" />";
		String expected = "<root attr1=\"value1\"\n" + //
				"      attr2=\"value2\" />";
		assertOnTypeFormatting(content, expected, 1, 0, settings);
	}

	// ---------- Tests for third attribute on new line

	@Test
	public void enterThirdAttributeSplitNewLine() throws BadLocationException {
		// indent = (0+2)*2 = 4
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\"\n" + //
				"attr3=\"value3\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"    attr2=\"value2\"\n" + //
				"    attr3=\"value3\">";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterThirdAttributeAlignWithFirstAttr() throws BadLocationException {
		// spaceCount = 6
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "<root attr1=\"value1\"\n" + //
				"      attr2=\"value2\"\n" + //
				"attr3=\"value3\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"      attr2=\"value2\"\n" + //
				"      attr3=\"value3\">";
		assertOnTypeFormatting(content, expected, 2, 0, settings);
	}

	// ---------- Tests for content indentation in nested elements

	@Test
	public void enterContentInDeeplyNestedElement() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"text\n" + //
				"    </c>\n" + //
				"  </b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"      text\n" + //
				"    </c>\n" + //
				"  </b>\n" + //
				"</a>";
		assertOnTypeFormatting(content, expected, 3, 0, settings);
	}

	@Test
	public void enterBetweenTagsDeeplyNested() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"</c>\n" + //
				"  </b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"      \n" + //
				"    </c>\n" + //
				"  </b>\n" + //
				"</a>";
		assertOnTypeFormatting(content, expected, 3, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsWithExistingWhitespaceBeforeEndTag() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"      </root>";
		String expected = "<root>\n" + //
				"  \n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsContentWithTabSize4() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setTabSize(4);
		String content = "<root>\n" + //
				"text</root>";
		String expected = "<root>\n" + //
				"    text</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for non-newline character (should be no-op)

	@Test
	public void nonNewlineCharacterNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root attr1=\"value1\">";
		assertOnTypeFormatting(content, content, 0, 19, ">", settings);
	}

	// ---------- Tests for edge cases / no-op scenarios

	@Test
	public void enterOutsideAnyElement() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "some text\n" + //
				"more text";
		assertOnTypeFormatting(content, content, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterAfterProlog() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<?xml version=\"1.0\"?>\n" + //
				"<root>\n" + //
				"</root>";
		String expected = "<?xml version=\"1.0\"?>\n" + //
				"<root>\n" + //
				"  \n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	@Test
	public void enterEmptyDocument() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "\n";
		assertOnTypeFormatting(content, content, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for element with leading whitespace

	@Test
	public void enterInsideStartTagWithLeadingWhitespace() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "          <foo attr=\"\"\n" + //
				"|attr2=\"\">";
		String expected = "          <foo attr=\"\"\n" + //
				"            |attr2=\"\">";
		assertOnTypeFormatting(content, expected, "\n", settings, false);
	}

	@Test
	public void enterInsideStartTagWithLeadingWhitespaceAlignWithFirstAttr() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.alignWithFirstAttr);
		String content = "          <foo attr=\"\"\n" + //
				"|attr2=\"\">";
		String expected = "          <foo attr=\"\"\n" + //
				"               |attr2=\"\">";
		assertOnTypeFormatting(content, expected, "\n", settings, false);
	}

	@Test
	public void enterInsideStartTagWithLeadingWhitespaceSplitNewLine() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.splitNewLine);
		settings.getFormattingSettings().setSplitAttributesIndentSize(2);
		String content = "          <foo attr=\"\"\n" + //
				"|attr2=\"\">";
		String expected = "          <foo attr=\"\"\n" + //
				"              |attr2=\"\">";
		assertOnTypeFormatting(content, expected, "\n", settings, false);
	}

	@Test
	public void enterBeforeCloseBracketWithLeadingWhitespace() throws BadLocationException {
		// > indented as attribute (preserve: elementColumn + tabSize = 5 + 2 = 7)
		SharedSettings settings = createSettings();
		String content = "     <foo attr=\"\"\n" + //
				"  >";
		String expected = "     <foo attr=\"\"\n" + //
				"       >";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for '/' trigger (closing tag indentation)

	@Test
	public void slashAlignsWithStartTag() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"   </|";
		String expected = "<foo>\n" + //
				"</foo>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashAlignsWithNestedStartTag() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <foo>\n" + //
				"    text\n" + //
				"       </|\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <foo>\n" + //
				"    text\n" + //
				"  </foo>|\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashWithLeadingWhitespace() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "          <foo>\n" + //
				"  </|";
		String expected = "          <foo>\n" + //
				"          </foo>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashNotAfterOpenBracketNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo attr=\"a/|b\">";
		assertOnTypeFormatting(content, content, "/", settings, false);
	}

	@Test
	public void slashAfterContentNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo>text</|";
		assertOnTypeFormatting(content, content, "/", settings, false);
	}

	@Test
	public void slashDeeplyNested() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"         </|\n" + //
				"  </b>\n" + //
				"</a>";
		String expected = "<a>\n" + //
				"  <b>\n" + //
				"    <c>\n" + //
				"    </c>|\n" + //
				"  </b>\n" + //
				"</a>";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashAlreadyAligned() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"</|";
		String expected = "<foo>\n" + //
				"</foo>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashGeneratesEndTagAfterEnter() throws BadLocationException {
		// Full flow: <foo> → Enter (indented) → < (no-op) → / (indent + end tag)
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"  </|";
		String expected = "<foo>\n" + //
				"</foo>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashDoesNotGenerateTagWhenContentFollows() throws BadLocationException {
		// Content after '/' on the line → indent only, no end tag generation
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"   </|bar>\n" + //
				"</foo>";
		String expected = "<foo>\n" + //
				"</|bar>\n" + //
				"</foo>";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashAtDocumentStartNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "</|";
		assertOnTypeFormatting(content, content, "/", settings, false);
	}

	// ---------- Tests for content indentation with leading whitespace

	@Test
	public void enterContentWithLeadingWhitespace() throws BadLocationException {
		// contentIndent = elementColumn(10) + tabSize(2) = 12 spaces
		SharedSettings settings = createSettings();
		String content = "          <foo>\n" + //
				"text\n" + //
				"          </foo>";
		String expected = "          <foo>\n" + //
				"            text\n" + //
				"          </foo>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterBetweenTagsWithLeadingWhitespace() throws BadLocationException {
		// contentIndent = elementColumn(10) + tabSize(2) = 12, endTagIndent = 10
		SharedSettings settings = createSettings();
		String content = "          <foo>\n" + //
				"          </foo>";
		String expected = "          <foo>\n" + //
				"            \n" + //
				"          </foo>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for '/' trigger additional cases

	@Test
	public void slashWithTabSize4() throws BadLocationException {
		SharedSettings settings = createSettings();
		settings.getFormattingSettings().setTabSize(4);
		String content = "<foo>\n" + //
				"      </|";
		String expected = "<foo>\n" + //
				"</foo>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashClosesRootElement() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <child></child>\n" + //
				"  </|";
		String expected = "<root>\n" + //
				"  <child></child>\n" + //
				"</root>|";
		assertOnTypeFormatting(content, expected, "/", settings, false);
	}

	@Test
	public void slashBareWithoutOpenBracketNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"  /|";
		assertOnTypeFormatting(content, content, "/", settings, false);
	}

	@Test
	public void slashInSelfClosingTagNoOp() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<br /|>";
		assertOnTypeFormatting(content, content, "/", settings, false);
	}

	// ---------- Tests for Enter after self-closing element

	@Test
	public void enterContentAfterSelfClosingElement() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <br />\n" + //
				"text\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  <br />\n" + //
				"  text\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	// ---------- Tests for Enter idempotent (already correct indent)

	@Test
	public void enterAlreadyCorrectIndentContent() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  text\n" + //
				"</root>";
		String expected = "<root>\n" + //
				"  text\n" + //
				"</root>";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterAlreadyCorrectIndentAttribute() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root attr1=\"value1\"\n" + //
				"  attr2=\"value2\">";
		String expected = "<root attr1=\"value1\"\n" + //
				"  attr2=\"value2\">";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	// ---------- Tests for unclosed elements (no end tag)

	@Test
	public void enterInUnclosedElement() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<foo>\n" + //
				"text";
		String expected = "<foo>\n" + //
				"  text";
		assertOnTypeFormatting(content, expected, 1, 0, "\n", settings, false);
	}

	@Test
	public void enterInUnclosedElementNested() throws BadLocationException {
		SharedSettings settings = createSettings();
		String content = "<root>\n" + //
				"  <foo>\n" + //
				"text";
		String expected = "<root>\n" + //
				"  <foo>\n" + //
				"    text";
		assertOnTypeFormatting(content, expected, 2, 0, "\n", settings, false);
	}

	// ---------- Helper methods

	private static SharedSettings createSettings() {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(2);
		settings.getFormattingSettings().setInsertSpaces(true);
		return settings;
	}
}
