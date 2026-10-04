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

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.MixedContent;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter tests for {@code xml.format.mixedContent} and
 * {@code xml.format.blockElements} settings.
 *
 * <p>
 * {@code mixedContent} controls how mixed content (text + child elements) is
 * formatted:
 * </p>
 * <ul>
 * <li>{@code preserve} — don't reformat mixed content at all.</li>
 * <li>{@code normalize} (default) — collapse inline whitespace to a single
 * space. Newlines in mixed content are collapsed to spaces (backward
 * compatible). {@code blockElements} is ignored.</li>
 * <li>{@code reflow} — preserve existing newlines, collapse inline whitespace.
 * When {@code maxLineWidth} is set, content soft-wraps at word and element
 * boundaries. Block elements (listed in {@code blockElements}) get own lines.</li>
 * <li>{@code expand} — like {@code reflow}, but always puts each
 * mixed content child on its own line, regardless of
 * {@code maxLineWidth}.</li>
 * </ul>
 *
 * <p>
 * {@code blockElements} is a list of element names treated as block in mixed
 * content. When {@code null} or {@code []} (default), no elements are block —
 * all elements are inline (backward compatible). Only effective in
 * {@code reflow} mode.
 * </p>
 */
public class XMLFormatterMixedContentSettingsTest {

	// ==========================================
	// Comparing both mixedContent values
	// Same input, different setting → see the visual difference
	// ==========================================

	// Input: <p>text   <b>bold</b>   more</p>
	// preserve:  unchanged (spaces and layout kept as-is)
	// reflow: inline whitespace collapsed, no newlines to preserve
	@Test
	public void testMixedContentPreserve_basic() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<p>text   <b>bold</b>   more</p>";
		assertFormat(content, content, settings);
	}

	@Test
	public void testMixedContentReflow_basic() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings,
				te(0, 7, 0, 10, " "),
				te(0, 21, 0, 24, " "));
		assertFormat(expected, expected, settings);
	}

	// Input with newlines: <p>\n  text\n  <b>bold</b>\n</p>
	// preserve:  unchanged
	// reflow: inline whitespace collapsed, existing newlines preserved
	@Test
	public void testMixedContentPreserve_withNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<p>\n  text\n  <b>bold</b>\n</p>";
		assertFormat(content, content, settings);
	}

	@Test
	public void testMixedContentReflow_withNewlines() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>\n  text\n  <b>bold</b>\n</p>";
		// reflow: newlines preserved, indentation normalized
		assertFormat(content, content, settings);
	}

	// Input with multiple children: <p>text <b>bold</b> <i>italic</i> more</p>
	// preserve:  unchanged
	// reflow: all inline (default blockElements=null), stays on same line
	@Test
	public void testMixedContentPreserve_multipleChildren() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<p>text <b>bold</b> <i>italic</i> more</p>";
		assertFormat(content, content, settings);
	}

	@Test
	public void testMixedContentReflow_multipleChildren() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>text <b>bold</b> <i>italic</i> more</p>";
		assertFormat(content, content, settings);
	}

	// ==========================================
	// mixedContent does NOT affect other categories
	// ==========================================

	// IgnoreSpace (element-only content): still indented even with preserve
	@Test
	public void testMixedContentPreserve_doesNotAffectIgnoreSpace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<root><child/></root>";
		String expected = "<root>" + lineSeparator() +
				"  <child />" + lineSeparator() +
				"</root>";
		assertFormat(content, expected, settings,
				te(0, 6, 0, 6, lineSeparator() + "  "),
				te(0, 12, 0, 12, " "),
				te(0, 14, 0, 14, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// NormalizeSpace (text-only content): still formatted even with preserve
	@Test
	public void testMixedContentPreserve_doesNotAffectNormalizeSpace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<p>hello world</p>";
		assertFormat(content, content, settings);
	}

	// ==========================================
	// blockElements tests
	// ==========================================

	// blockElements=null (default): no block elements, all inline (backward compat)
	@Test
	public void testAllInlineWhenNullDefault() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		// blockElements is null by default (no block elements = all inline)
		String content = "<p>text <div>block</div></p>";
		assertFormat(content, content, settings);
	}

	// Inline element stays on same line as text (reflow mode).
	// Use case: <b> is NOT in blockElements → stays inline with surrounding text.
	@Test
	public void testInlineElementStaysInline() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>text <b>bold</b></p>";
		assertFormat(content, content, settings);
	}

	// Block element gets its own line with indent (reflow mode).
	// Use case: <div> is in blockElements → block → own line.
	@Test
	public void testBlockElementGetsOwnLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>text <div>block</div></p>";
		String expected = "<p>text" + lineSeparator() +
				"  <div>block</div>" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings,
				te(0, 7, 0, 8, lineSeparator() + "  "),
				te(0, 24, 0, 24, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Mix of inline and block elements in same parent (reflow mode).
	// Use case: <div> is in blockElements → block → own line. <b> is not → inline.
	// Text "more" after block element → starts on own line.
	@Test
	public void testMixOfInlineAndBlockElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>text <b>bold</b> <div>block</div> more</p>";
		String expected = "<p>text <b>bold</b>" + lineSeparator() +
				"  <div>block</div>" + lineSeparator() +
				"  more" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Empty blockElements list [] is equivalent to null: no block elements, all inline.
	// Use case: user clears the blockElements setting → same as not setting it.
	@Test
	public void testEmptyBlockElementsSameAsNull() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Collections.emptyList());
		String content = "<p>text <b>bold</b></p>";
		// [] = null = no block elements = all inline → <b> stays on same line
		assertFormat(content, content, settings);
	}

	// Multiple block elements get proper indentation (reflow mode).
	// Use case: <div> and <section> are in blockElements → block → own lines.
	@Test
	public void testMultipleBlockElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div", "section"));
		String content = "<article>text <div>a</div><section>b</section></article>";
		String expected = "<article>text" + lineSeparator() +
				"  <div>a</div>" + lineSeparator() +
				"  <section>b</section>" + lineSeparator() +
				"</article>";
		assertFormat(content, expected, settings,
				te(0, 13, 0, 14, lineSeparator() + "  "),
				te(0, 26, 0, 26, lineSeparator() + "  "),
				te(0, 46, 0, 46, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Self-closing block element in mixed content (reflow mode).
	// Use case: <hr/> is in blockElements → block → own line.
	@Test
	public void testSelfClosingBlockElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("hr"));
		String content = "<p>text <hr/></p>";
		String expected = "<p>text" + lineSeparator() +
				"  <hr />" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings,
				te(0, 7, 0, 8, lineSeparator() + "  "),
				te(0, 11, 0, 11, " "),
				te(0, 13, 0, 13, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Text before and after block element (reflow mode).
	// Use case: <div> is in blockElements → own line. Text "after" follows block → own line.
	// End tag on own line because softWrapped.
	@Test
	public void testTextBeforeAndAfterBlockElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>before <div>block</div> after</p>";
		String expected = "<p>before" + lineSeparator() +
				"  <div>block</div>" + lineSeparator() +
				"  after" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Interaction tests
	// ==========================================

	// preserveSpace list takes priority over mixedContent setting
	@Test
	public void testPreserveSpacePriority() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setPreserveSpace(Arrays.asList("pre"));
		String content = "<pre>text  <b>bold</b></pre>";
		assertFormat(content, content, settings);
	}

	// mixedContent=preserve + blockElements → preserve wins (no reformatting)
	@Test
	public void testPreserveWinsOverBlockElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>text <div>block</div></p>";
		assertFormat(content, content, settings);
	}

	// closingBracketNewLine with block elements (reflow mode).
	// Use case: <b> is not in blockElements → stays on same line even with closingBracketNewLine.
	@Test
	public void testBlockElementsWithClosingBracketNewLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<p>text <b>bold</b></p>";
		assertFormat(content, content, settings);
	}

	// Block element with URI "test.xml" (reflow mode, no newlines in input).
	// Use case: <div> is in blockElements → own line, using System.lineSeparator.
	@Test
	public void testBlockElementCRLF() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>text <div>block</div></p>";
		String expected = "<p>text" + lineSeparator() +
				"  <div>block</div>" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings, "test.xml",
				te(0, 7, 0, 8, lineSeparator() + "  "),
				te(0, 24, 0, 24, lineSeparator()));
		assertFormat(expected, expected, settings, "test.xml");
	}

	// ==========================================
	// Issue-specific regression tests
	// ==========================================

	// #1012 (vscode-xml): Text content inlined with closing tag
	// Use case: user wants <line>Foo (ref <b>bar</b>)</line> to stay as-is
	// with preserve mode
	@Test
	public void testIssue1012_textContentInlined_preserve() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<line>Foo (ref <b>bar</b>)</line>";
		assertFormat(content, content, settings);
	}

	// #1012: with reflow mode, content stays inline (all inline by default)
	@Test
	public void testIssue1012_textContentInlined_reflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<line>Foo (ref <b>bar</b>)</line>";
		assertFormat(content, content, settings);
	}

	// #1026 (vscode-xml): Multiple whitespaces collapsed
	// Use case: user has <inner>Test     Spaces</inner> inside a structure
	// with preserve, text-only elements (NormalizeSpace) are still formatted
	@Test
	public void testIssue1026_whitespacePreserve() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<tag>" + lineSeparator() +
				"  <inner>Test     Spaces</inner>" + lineSeparator() +
				"</tag>";
		assertFormat(content, content, settings);
	}

	// #1684 (lemminx): All elements inline by default (no blockElements set).
	// Use case (reflow): <p>Click <b>here</b> to continue</p>
	// <b> is not in blockElements → inline → stays on same line.
	@Test
	public void testIssue1684_allInline() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>Click <b>here</b> to continue</p>";
		assertFormat(content, content, settings);
	}

	// #1684: block element gets own line (reflow mode).
	// Use case: <div> is in blockElements → block → own line.
	@Test
	public void testIssue1684_blockElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		String content = "<p>Click <b>here</b> to <div>see more</div></p>";
		String expected = "<p>Click <b>here</b> to" + lineSeparator() +
				"  <div>see more</div>" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings,
				te(0, 23, 0, 24, lineSeparator() + "  "),
				te(0, 43, 0, 43, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// #1649 (lemminx): When start tag and end tag are on different lines,
	// end tag goes on its own line indented with start tag.
	// Use case: <text x="50%"\n      y="50%"> SVG </text>
	//   → </text> on own line because <text and </text> are on different lines.
	@Test
	public void testIssue1649_endTagOnDifferentLine_normalizeSpace() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<text x=\"50%\"\n" +
				"      y=\"50%\"\n" +
				"      font-size=\"60\" > SVG </text>";
		String expected = "<text x=\"50%\"\n" +
				"  y=\"50%\"\n" +
				"  font-size=\"60\"> SVG\n" +
				"</text>";
		assertFormat(content, expected, settings);
	}

	// #1649: MixedContent (text + child element) — end tag on own line
	// because start and end tags are on different lines.
	@Test
	public void testIssue1649_endTagOnDifferentLine_mixedContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<text x=\"50%\"\n" +
				"      y=\"50%\"> Click <b>here</b> </text>";
		String expected = "<text x=\"50%\"\n" +
				"  y=\"50%\"> Click <b>here</b>\n" +
				"</text>";
		assertFormat(content, expected, settings);
	}

	// #1649: single line element — start and end tag on same line, no change.
	@Test
	public void testIssue1649_singleLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setClosingBracketNewLine(true);
		String content = "<p>text <b>bold</b></p>";
		assertFormat(content, content, settings);
	}

	// #1797 (lemminx) + #1131 (vscode-xml): Mixed content with maxLineWidth
	// Use case: long mixed content with maxLineWidth
	@Test
	public void testIssue1797_mixedContentMaxLineWidth_preserve() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		settings.getFormattingSettings().setMaxLineWidth(40);
		String content = "<p>some long text <b>bold</b> and more <i>italic content here</i></p>";
		// preserve: unchanged regardless of maxLineWidth
		assertFormat(content, content, settings);
	}

	// ==========================================
	// Soft-wrap tests
	// reflow + maxLineWidth: content fits → stays flat
	// reflow + maxLineWidth: content overflows → soft-wrap at boundaries
	// ==========================================

	// Use case: <p>text <b>bold</b> more</p> with maxLineWidth=80
	// Content fits on one line → stays flat, no wrapping.
	@Test
	public void testReflow_fitsOnOneLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<p>text <b>bold</b> more</p>";
		assertFormat(content, content, settings);
	}

	// Use case: <p>text <b>bold</b> and more <i>italic</i></p> with maxLineWidth=30.
	// blockElements=["b","i"] → <b> and <i> are block → each on own line.
	// Text after block element → own line.
	@Test
	public void testReflow_overflowSoftWrap() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("b", "i"));
		String content = "<p>text <b>bold</b> and more <i>italic</i></p>";
		String expected = "<p>text" + lineSeparator() +
				"  <b>bold</b>" + lineSeparator() +
				"  and more" + lineSeparator() +
				"  <i>italic</i>" + lineSeparator() +
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Soft-wrap with nested block elements. Text after block → own line.
	// Use case: blockElements=["div","b"] → <div> and <b> are block.
	// Verifies idempotency at indent level > 0.
	@Test
	public void testReflow_softWrapNestedBlocks() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(30);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div", "b"));
		String content = "<root>before <div>text <b>bold</b> trailing</div> after</root>";
		String expected = "<root>before" + lineSeparator() +
				"  <div>text" + lineSeparator() +
				"    <b>bold</b>" + lineSeparator() +
				"  trailing" + lineSeparator() +
				"  </div>" + lineSeparator() +
				"  after" + lineSeparator() +
				"</root>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Use case: MyBatis <update> with SQL + child elements, maxLineWidth=80.
	// blockElements=["set"] → <set> is block → own line, <if> is inline.
	// "update emp" stays inline with start tag (fits).
	// Text "where id=..." after block </set> → own line.
	@Test
	public void testReflow_mybatisUpdate() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setBlockElements(Arrays.asList("set"));
		String content = "<update id=\"updateEmp\"> update emp <set><if test=\"name != null\">name=#{name},</if><if test=\"gender != null\">gender=#{gender},</if></set> where id=#{id} </update>";
		String expected = "<update id=\"updateEmp\"> update emp" + lineSeparator() +
				"  <set>" + lineSeparator() +
				"    <if test=\"name != null\">name=#{name},</if>" + lineSeparator() +
				"    <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator() +
				"  </set>" + lineSeparator() +
				"  where id=#{id}" + lineSeparator() +
				"</update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// #1131 (vscode-xml): MyBatis <update> with many <if> children, tabSize=4
	// Use case: real-world MyBatis mapper with messy indentation and some <if>
	// content split across lines. maxLineWidth=0 → no wrapping.
	// Just mixedContent=reflow (no blockElements needed):
	//   - all elements are inline by default (blockElements=null)
	//   - isNormalizeInsideMixedContent joins multi-line <if> content
	//   - newlines in mixed content are preserved (mixedContentJoins=false)
	@Test
	public void testIssue1131_mybatisFullUpdate() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<update id=\"updateEmp\"> update emp <set>" + lineSeparator()
				+ "      <if test=\"username != null and username != ''\">username=#{username},</if>" + lineSeparator()
				+ "      <if test=\"password != null and password != ''\">" + lineSeparator()
				+ "    password=#{password},</if>" + lineSeparator()
				+ "      <if test=\"name != null and name != ''\">name=#{name},</if>" + lineSeparator()
				+ "      <if test=\"gender != null\">" + lineSeparator()
				+ "    gender=#{gender},</if>" + lineSeparator()
				+ "      <if test=\"phone != null and phone != ''\">phone=#{phone},</if>" + lineSeparator()
				+ "      <if test=\"job != null\">" + lineSeparator()
				+ "    job=#{job},</if>" + lineSeparator()
				+ "      <if test=\"salary != null\">salary=#{salary},</if>" + lineSeparator()
				+ "      <if test=\"image != null and image != ''\">" + lineSeparator()
				+ "    image=#{image},</if>" + lineSeparator()
				+ "      <if test=\"entryDate != null\">entry_date=#{entryDate},</if>" + lineSeparator()
				+ "      <if test=\"deptId != null\">" + lineSeparator()
				+ "    dept_id=#{deptId},</if> update_time=#{updateTime} </set> where id=#{id} </update>";
		String expected = "<update id=\"updateEmp\"> update emp <set>" + lineSeparator()
				+ "        <if test=\"username != null and username != ''\">username=#{username},</if>" + lineSeparator()
				+ "        <if test=\"password != null and password != ''\">password=#{password},</if>" + lineSeparator()
				+ "        <if test=\"name != null and name != ''\">name=#{name},</if>" + lineSeparator()
				+ "        <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator()
				+ "        <if test=\"phone != null and phone != ''\">phone=#{phone},</if>" + lineSeparator()
				+ "        <if test=\"job != null\">job=#{job},</if>" + lineSeparator()
				+ "        <if test=\"salary != null\">salary=#{salary},</if>" + lineSeparator()
				+ "        <if test=\"image != null and image != ''\">image=#{image},</if>" + lineSeparator()
				+ "        <if test=\"entryDate != null\">entry_date=#{entryDate},</if>" + lineSeparator()
				+ "        <if test=\"deptId != null\">dept_id=#{deptId},</if> update_time=#{updateTime} </set> where id=#{id} </update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Use case: <p>text   <b>bold</b>   more</p> with no maxLineWidth
	// No maxLineWidth → no wrapping, just collapse inline spaces to single space.
	@Test
	public void testReflow_noMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings,
				te(0, 7, 0, 10, " "),
				te(0, 21, 0, 24, " "));
		assertFormat(expected, expected, settings);
	}

	// #1301 (lemminx): End tag with preserve spaces
	// Use case: preserve mode keeps end tag position
	@Test
	public void testIssue1301_endTagPreserveSpaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.preserve);
		String content = "<doc>text <b>bold</b>\n</doc>";
		assertFormat(content, content, settings);
	}

	// #1301: with reflow mode, trailing newline before end tag is preserved
	@Test
	public void testIssue1301_endTagReflow() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<doc>text <b>bold</b>\n</doc>";
		assertFormat(content, content, settings);
	}

	// ==========================================
	// Newline preservation tests
	// reflow preserves existing newlines in mixed content
	// ==========================================

	// Use case: <line>Foo (ref <b>bar</b>)\n</line>
	// User has a newline before </line> → reflow preserves it.
	// Unlike Prettier (which would collapse to one line if it fits),
	// reflow respects existing structure.
	@Test
	public void testReflow_preservesNewlineBeforeEndTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<line>Foo (ref <b>bar</b>)\n</line>";
		assertFormat(content, content, settings);
	}

	// Use case: <line>Foo (ref <b>bar</b>)</line> (no newline)
	// No existing newline → everything stays on one line.
	@Test
	public void testReflow_noNewlineStaysInline() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<line>Foo (ref <b>bar</b>)</line>";
		assertFormat(content, content, settings);
	}

	// Use case: <line> Foo (ref <b>bar</b>)</line> with maxLineWidth=200.
	// Content (~38 chars) fits well within maxLineWidth → no wrapping.
	@Test
	public void testReflow_lineWithMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(200);
		String content = "<line> Foo (ref <b>bar</b>)</line>";
		// Content fits within 200 chars → stays on one line.
		assertFormat(content, content, settings);
	}

	// expand: <line> Foo (ref <b>bar</b>)</line> with expand.
	// expand always expand MixedContent.
	@Test
	public void testExpand_lineWithInlineElement() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<line> Foo (ref <b>bar</b>)</line>";
		// expand expand: all children on own lines.
		// <b> is NormalizeSpace (text-only) → "bar" stays inline with <b>.
		String expected = "<line>" + lineSeparator() + //
				"  Foo (ref" + lineSeparator() + //
				"  <b>bar</b>" + lineSeparator() + //
				"  )" + lineSeparator() + //
				"</line>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// Deeply nested structure
	// ==========================================

	// Nested mixed content with inline and block elements (reflow mode).
	// Use case: <article> is IgnoreSpace → children indented.
	// <p> has <b> (inline) → text and <b> stay on same line.
	@Test
	public void testDeeplyNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<article><p>text <b>bold</b></p><section>content</section></article>";
		// <article> is IgnoreSpace (element-only), children get indented
		String expected = "<article>" + lineSeparator() +
				"  <p>text <b>bold</b></p>" + lineSeparator() +
				"  <section>content</section>" + lineSeparator() +
				"</article>";
		assertFormat(content, expected, settings,
				te(0, 9, 0, 9, lineSeparator() + "  "),
				te(0, 32, 0, 32, lineSeparator() + "  "),
				te(0, 58, 0, 58, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

	// Comment in mixed content — stays inline (reflow mode).
	// Use case: comments are always inline in mixed content, never block.
	@Test
	public void testCommentInMixedContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>text <!-- comment --></p>";
		assertFormat(content, content, settings);
	}

	// ==========================================
	// Default settings backward compatibility
	// ==========================================

	// Use case: no mixedContent, no blockElements → main behavior.
	// Inline whitespace is collapsed to a single space.
	@Test
	public void testDefaultSettings_mixedContentBackwardCompat() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		// No mixedContent, no blockElements → mixedContentJoins=true → spaces collapsed
		String content = "<p>text   <b>bold</b>   more</p>";
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings,
				te(0, 7, 0, 10, " "), //
				te(0, 21, 0, 24, " ")); //
		assertFormat(expected, expected, settings);
	}

	// normalize + blockElements: blockElements is ignored in normalize mode.
	// Use case: user sets blockElements=["div"] but keeps default normalize mode.
	// Newlines are still collapsed to spaces (normalize behavior).
	// Block/inline distinction does NOT apply.
	@Test
	public void testNormalize_blockElementsIgnored() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		// mixedContent defaults to normalize
		settings.getFormattingSettings().setBlockElements(Arrays.asList("div"));
		// Whitespace inside text nodes (before <b>, after </div>) is collapsed.
		// Whitespace in DOM gaps (between </b> and <div>) is preserved with
		// normalized indentation — same behavior as default (no blockElements).
		String content = "<p>text\n  <b>bold</b>\n  <div>block</div>\n  more</p>";
		String expected = "<p>text <b>bold</b>\n  <div>block</div> more</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// normalize + blockElements: same behavior as without blockElements.
	// Use case: verifies that setting blockElements=["b"] with normalize
	// produces the same result as not setting it at all.
	@Test
	public void testNormalize_blockElementsSameAsDefault() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setBlockElements(Arrays.asList("b"));
		String content = "<p>text   <b>bold</b>   more</p>";
		// Same as testDefaultSettings_mixedContentBackwardCompat
		String expected = "<p>text <b>bold</b> more</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// reflow: newlines preserved in mixed content.
	// Use case: user wants newlines NOT collapsed, <b> stays inline.
	// End tag goes on own line because content spans multiple lines (softWrapped).
	@Test
	public void testReflow_preservesNewlinesInMixedContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		String content = "<p>text <b>bold</b> replace\n  the content in <b>file.xml</b></p>";
		// reflow: newline after "replace" preserved, <b> stays inline.
		// End tag on own line: content spans multiple lines → end tag aligns with start tag.
		String expected = "<p>text <b>bold</b> replace\n  the content in <b>file.xml</b>\n</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// mixedContent=reflow without blockElements
	// Use case: MyBatis — just one setting activates all reflow behaviors
	// ==========================================

	// Use case: MyBatis <update> with reflow-only + maxLineWidth.
	// All elements inline by default. <set> overflows → moved to own line.
	// Text " where id=#{id}" stays inline after </set> (inline element).
	@Test
	public void testReflow_mybatisNoBlockElements() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<update id=\"updateEmp\"> update emp " + //
				"<set><if test=\"name != null\">name=#{name},</if>" + //
				"<if test=\"gender != null\">gender=#{gender},</if></set>" + //
				" where id=#{id} </update>";
		String expected = "<update id=\"updateEmp\"> update emp" + lineSeparator() + //
				"  <set>" + lineSeparator() + //
				"    <if test=\"name != null\">name=#{name},</if>" + lineSeparator() + //
				"    <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator() + //
				"  </set> where id=#{id}" + lineSeparator() + //
				"</update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// NormalizeSpace expand
	// Use case: text-only element where text + end tag exceeds available width
	// ==========================================

	// Use case: <p>long text...</p> with narrow maxLineWidth.
	// Text soft-wraps at word boundaries, end tag goes on own line
	// when soft-wrap occurred (softWrapped=true).
	@Test
	public void testNormalizeSpace_softWrap() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(20);
		// <p>(3) + content(47) + </p>(4) = 54 > 20 → soft-wrap.
		// Word-wrapping splits text at word boundaries.
		// End tag on own line (softWrapped=true).
		String content = "<p>this is a very long text that exceeds the width</p>";
		String expected = "<p>this is a very" + lineSeparator() + //
				"  long text that" + lineSeparator() + //
				"  exceeds the width" + lineSeparator() + //
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Use case: NormalizeSpace soft-wrap idempotency —
	// formatting twice produces same result.
	@Test
	public void testNormalizeSpace_softWrapIdempotent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(15);
		// <tag>(5) + content(15) + </tag>(6) = 26 > 15 → soft-wrap.
		// Word-wraps at "here" (line too long).
		// End tag on own line.
		String content = "<tag>short text here</tag>";
		String expected = "<tag>short text" + lineSeparator() + //
				"  here" + lineSeparator() + //
				"</tag>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ==========================================
	// mixedContent=expand
	// Forces expand (all children on own lines) without maxLineWidth.
	// ==========================================

	// Use case: user's example — <root><bbbbbb>c<g>hhh...h</g><g>kkk</g></bbbbbb></root>
	// expand always expands MixedContent, regardless of maxLineWidth.
	// <bbbbbb> is MixedContent (text + elements) → all children on own lines.
	// <g> is NormalizeSpace (text-only) → content stays inline with <g>.
	@Test
	public void testExpand_userExample() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<root><bbbbbb>c<g>hhhhhhhhhhhh</g><g>kkkkkkkkk</g></bbbbbb></root>";
		// <root> is IgnoreSpace → children indented
		// <bbbbbb> is MixedContent → expand expand
		// <g> is NormalizeSpace → text stays inline
		String expected = "<root>" + lineSeparator() + //
				"    <bbbbbb>" + lineSeparator() + //
				"        c" + lineSeparator() + //
				"        <g>hhhhhhhhhhhh</g>" + lineSeparator() + //
				"        <g>kkkkkkkkk</g>" + lineSeparator() + //
				"    </bbbbbb>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// #1797: reflow with maxLineWidth=100 — first <g> fits on same line as "c"
	@Test
	public void testReflow_userExample_maxLineWidth100() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(100);
		String content = "<root><bbbbbb>c<g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></bbbbbb></root>";
		String expected = "<root>" + lineSeparator() + //
				"  <bbbbbb>c<g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g>" + lineSeparator() + //
				"    <g>kkkkkkkkk</g>" + lineSeparator() + //
				"  </bbbbbb>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// #1797: reflow with maxLineWidth=80 — first <g> exceeds width, wraps to new line
	@Test
	public void testReflow_userExample_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String content = "<root><bbbbbb>c<g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g><g>kkkkkkkkk</g></bbbbbb></root>";
		String expected = "<root>" + lineSeparator() + //
				"  <bbbbbb>c" + lineSeparator() + //
				"    <g>hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh</g>" + lineSeparator() + //
				"    <g>kkkkkkkkk</g>" + lineSeparator() + //
				"  </bbbbbb>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand with tabSize=2 (default)
	@Test
	public void testExpand_tabSize2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<p>text <b>bold</b> more</p>";
		// <p> is MixedContent → expand expand
		String expected = "<p>" + lineSeparator() + //
				"  text" + lineSeparator() + //
				"  <b>bold</b>" + lineSeparator() + //
				"  more" + lineSeparator() + //
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: NormalizeSpace elements are NOT expanded (no forced expand).
	// Only MixedContent elements get the expand treatment.
	@Test
	public void testExpand_normalizeSpaceNotExpanded() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		// <p> is NormalizeSpace (text-only) → no forced expand
		String content = "<p>short text</p>";
		assertFormat(content, content, settings);
	}

	// expand with nested MixedContent: both levels get expand.
	@Test
	public void testExpand_nested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<div>text <p>inner <b>bold</b></p></div>";
		// <div> is MixedContent → expand
		// <p> is MixedContent → expand
		String expected = "<div>" + lineSeparator() + //
				"  text" + lineSeparator() + //
				"  <p>" + lineSeparator() + //
				"    inner" + lineSeparator() + //
				"    <b>bold</b>" + lineSeparator() + //
				"  </p>" + lineSeparator() + //
				"</div>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: single child element in MixedContent still gets expand.
	@Test
	public void testExpand_singleChild() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<p>text <b>bold</b></p>";
		String expected = "<p>" + lineSeparator() + //
				"  text" + lineSeparator() + //
				"  <b>bold</b>" + lineSeparator() + //
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand with MyBatis: all children expanded regardless of width.
	@Test
	public void testExpand_mybatis() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<update id=\"updateEmp\"> update emp " + //
				"<set><if test=\"name != null\">name=#{name},</if>" + //
				"<if test=\"gender != null\">gender=#{gender},</if></set>" + //
				" where id=#{id} </update>";
		// <update> is MixedContent → expand
		// <set> is MixedContent → expand
		String expected = "<update id=\"updateEmp\">" + lineSeparator() + //
				"    update emp" + lineSeparator() + //
				"    <set>" + lineSeparator() + //
				"        <if test=\"name != null\">name=#{name},</if>" + lineSeparator() + //
				"        <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator() + //
				"    </set>" + lineSeparator() + //
				"    where id=#{id}" + lineSeparator() + //
				"</update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: IgnoreSpace parent (element-only) is not affected.
	@Test
	public void testExpand_ignoreSpaceUnaffected() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		// <root> is IgnoreSpace (children are elements only) → normal indentation
		String content = "<root><a>text</a><b>more</b></root>";
		String expected = "<root>" + lineSeparator() + //
				"  <a>text</a>" + lineSeparator() + //
				"  <b>more</b>" + lineSeparator() + //
				"</root>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: whitespace between text and elements is normalized.
	@Test
	public void testExpand_whitespaceNormalized() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<p>text   <b>bold</b>   more</p>";
		// expand expand + whitespace normalized
		String expected = "<p>" + lineSeparator() + //
				"  text" + lineSeparator() + //
				"  <b>bold</b>" + lineSeparator() + //
				"  more" + lineSeparator() + //
				"</p>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: MyBatis <select> with SQL text + <where> + <if>.
	// <select> is MixedContent (text + <where>) → expand expand.
	// <where> is IgnoreSpace (only <if> children) → normal indentation.
	// <if> is NormalizeSpace (text-only) → not expanded.
	@Test
	public void testExpand_mybatisSelectWhere() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<select id=\"findAll\"> select * from emp " + //
				"<where>" + //
				"<if test=\"name != null\"> and name = #{name} </if>" + //
				"<if test=\"gender != null\">and gender = #{gender}</if>" + //
				"</where> </select>";
		String expected = "<select id=\"findAll\">" + lineSeparator() + //
				"  select * from emp" + lineSeparator() + //
				"  <where>" + lineSeparator() + //
				"    <if test=\"name != null\"> and name = #{name} </if>" + lineSeparator() + //
				"    <if test=\"gender != null\">and gender = #{gender}</if>" + lineSeparator() + //
				"  </where>" + lineSeparator() + //
				"</select>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: MyBatis <delete> with text + <foreach>.
	// <delete> is MixedContent → expand expand.
	// <foreach> is NormalizeSpace (text-only) → not expanded.
	@Test
	public void testExpand_mybatisDeleteForeach() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<delete id=\"deleteEmp\"> delete from emp where id in " + //
				"<foreach collection=\"id\" item=\"item\" open=\"(\" close=\")\" separator=\",\">" + //
				" #{item} </foreach> </delete>";
		String expected = "<delete id=\"deleteEmp\">" + lineSeparator() + //
				"  delete from emp where id in" + lineSeparator() + //
				"  <foreach collection=\"id\" item=\"item\" open=\"(\" close=\")\" separator=\",\"> #{item} </foreach>" + lineSeparator() + //
				"</delete>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: MyBatis <delete> with <foreach> split attributes.
	// <delete> is MixedContent → expand expand.
	// <foreach> is NormalizeSpace with multi-line start tag →
	// end tag goes on own line (start tag and end tag not on same line).
	@Test
	public void testExpand_mybatisDeleteForeachSplitAttrs() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<delete id=\"deleteEmp\"> delete from emp where id in " + //
				"<foreach collection=\"id\"" + lineSeparator() + //
				"           item=\"item\"" + lineSeparator() + //
				"           open=\"(\"" + lineSeparator() + //
				"           close=\")\"" + lineSeparator() + //
				"           separator=\",\">" + lineSeparator() + //
				"    #{item} </foreach> </delete>";
		// expand expand on <delete> puts <foreach> on own line.
		// <foreach> has multi-line start tag → </foreach> goes on own line.
		// Content #{item} stays inline after >, isNormalizeInsideMixedContent joins.
		String expected = "<delete id=\"deleteEmp\">" + lineSeparator() + //
				"  delete from emp where id in" + lineSeparator() + //
				"  <foreach collection=\"id\"" + lineSeparator() + //
				"    item=\"item\"" + lineSeparator() + //
				"    open=\"(\"" + lineSeparator() + //
				"    close=\")\"" + lineSeparator() + //
				"    separator=\",\">#{item}" + lineSeparator() + //
				"  </foreach>" + lineSeparator() + //
				"</delete>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: MyBatis <update> with <set> containing <if> elements + standalone text.
	// <update> is MixedContent → expand.
	// <set> is MixedContent (text "update_time=..." + <if> elements) → expand.
	// <if> is NormalizeSpace inside MixedContent → content joined.
	@Test
	public void testExpand_mybatisFullUpdate() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<update id=\"updateEmp\"> update emp " + //
				"<set>" + //
				"<if test=\"username != null and username != ''\">username=#{username},</if>" + //
				"<if test=\"password != null and password != ''\">" + lineSeparator() + //
				"    password=#{password},</if>" + //
				"<if test=\"name != null and name != ''\">name=#{name},</if>" + //
				" update_time=#{updateTime} </set> where id=#{id} </update>";
		String expected = "<update id=\"updateEmp\">" + lineSeparator() + //
				"  update emp" + lineSeparator() + //
				"  <set>" + lineSeparator() + //
				"    <if test=\"username != null and username != ''\">username=#{username},</if>" + lineSeparator() + //
				"    <if test=\"password != null and password != ''\">password=#{password},</if>" + lineSeparator() + //
				"    <if test=\"name != null and name != ''\">name=#{name},</if>" + lineSeparator() + //
				"    update_time=#{updateTime}" + lineSeparator() + //
				"  </set>" + lineSeparator() + //
				"  where id=#{id}" + lineSeparator() + //
				"</update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// expand: already formatted MyBatis mapper stays unchanged (idempotency).
	@Test
	public void testExpand_mybatisIdempotent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setMaxLineWidth(0);
		String content = "<update id=\"updateEmp\">" + lineSeparator() + //
				"  update emp" + lineSeparator() + //
				"  <set>" + lineSeparator() + //
				"    <if test=\"name != null\">name=#{name},</if>" + lineSeparator() + //
				"    <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator() + //
				"    update_time=#{updateTime}" + lineSeparator() + //
				"  </set>" + lineSeparator() + //
				"  where id=#{id}" + lineSeparator() + //
				"</update>";
		assertFormat(content, content, settings);
	}

	// expand: isNormalizeInsideMixedContent still joins content in NormalizeSpace
	// children of MixedContent parents.
	// <set> must have text + elements to be MixedContent (not just <if> alone).
	@Test
	public void testExpand_normalizeInsideMixedContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.expand);
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setMaxLineWidth(0);
		// <set> has <if> + text "done" → MixedContent → expand expand.
		// <if> is NormalizeSpace inside MixedContent parent → content joined.
		String content = "<set><if test=\"name != null\">" + lineSeparator() + //
				"    name=#{name},</if> done </set>";
		String expected = "<set>" + lineSeparator() + //
				"    <if test=\"name != null\">name=#{name},</if>" + lineSeparator() + //
				"    done" + lineSeparator() + //
				"</set>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// ---- Paragraph tests: <b> inline vs block with different maxLineWidth ----

	private static final String PARAGRAPH_CONTENT = "<paragraph> Dearest Bob, <b>please</b>"
			+ " replace the text content in <b>mixedContentFormatting.xml</b>"
			+ " with something more <b>work appropriate</b>"
			+ " that still includes <b>bolded</b> words. </paragraph>";

	// Paragraph with inline (<b>) and block (<div>) elements for inline/block distinction.
	private static final String PARAGRAPH_MIXED_ELEMENTS = "<p> Click <b>here</b> to see the"
			+ " <div>important details</div> and then <b>submit</b> the final <div>report</div> for review. </p>";

	// blockElements=null (all inline), maxLineWidth=0: no wrapping, stays inline.
	@Test
	public void testReflow_paragraphAllInline_noMaxLineWidth() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(0);
		assertFormat(PARAGRAPH_CONTENT, PARAGRAPH_CONTENT, settings);
	}

	// blockElements=null (all inline), maxLineWidth=200: content fits, stays inline.
	@Test
	public void testReflow_paragraphAllInline_maxLineWidth200() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(200);
		assertFormat(PARAGRAPH_CONTENT, PARAGRAPH_CONTENT, settings);
	}

	// blockElements=null (all inline), maxLineWidth=80: soft-wrap at 80 chars.
	// <b> elements stay inline (not in blockElements). Lines wrap at word/element boundaries.
	@Test
	public void testReflow_paragraphAllInline_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String expected = "<paragraph> Dearest Bob, <b>please</b> replace the text content in" + lineSeparator() + //
				"  <b>mixedContentFormatting.xml</b> with something more <b>work appropriate</b>" + lineSeparator() + //
				"  that still includes <b>bolded</b> words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// blockElements=null (b is inline by default), maxLineWidth=200: content fits, stays inline.
	@Test
	public void testReflow_paragraphBInline_maxLineWidth200() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(200);
		assertFormat(PARAGRAPH_CONTENT, PARAGRAPH_CONTENT, settings);
	}

	// blockElements=null (b is inline by default), maxLineWidth=80: same as all-inline
	// since all child elements in the example ARE <b> (not in blockElements).
	@Test
	public void testReflow_paragraphBInline_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String expected = "<paragraph> Dearest Bob, <b>please</b> replace the text content in" + lineSeparator() + //
				"  <b>mixedContentFormatting.xml</b> with something more <b>work appropriate</b>" + lineSeparator() + //
				"  that still includes <b>bolded</b> words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// blockElements=null (all inline), maxLineWidth=120: wraps at ~120 chars.
	@Test
	public void testReflow_paragraphAllInline_maxLineWidth120() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(120);
		String expected = "<paragraph> Dearest Bob, <b>please</b> replace the text content in <b>mixedContentFormatting.xml</b> with something more" + lineSeparator() + //
				"  <b>work appropriate</b> that still includes <b>bolded</b> words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// blockElements=null (all inline), maxLineWidth=40: narrow wrapping.
	@Test
	public void testReflow_paragraphAllInline_maxLineWidth40() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(40);
		String expected = "<paragraph> Dearest Bob, <b>please</b>" + lineSeparator() + //
				"  replace the text content in" + lineSeparator() + //
				"  <b>mixedContentFormatting.xml</b> with" + lineSeparator() + //
				"  something more <b>work appropriate</b>" + lineSeparator() + //
				"  that still includes <b>bolded</b>" + lineSeparator() + //
				"  words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// blockElements=["b"] (every <b> is block), maxLineWidth=200.
	// Use case: every <b> is block → own line.
	// Text after block → own line. maxLineWidth irrelevant since blocks force breaks.
	@Test
	public void testReflow_paragraphAllBlock_maxLineWidth200() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(200);
		settings.getFormattingSettings().setBlockElements(java.util.Arrays.asList("b"));
		String expected = "<paragraph> Dearest Bob," + lineSeparator() + //
				"  <b>please</b>" + lineSeparator() + //
				"  replace the text content in" + lineSeparator() + //
				"  <b>mixedContentFormatting.xml</b>" + lineSeparator() + //
				"  with something more" + lineSeparator() + //
				"  <b>work appropriate</b>" + lineSeparator() + //
				"  that still includes" + lineSeparator() + //
				"  <b>bolded</b>" + lineSeparator() + //
				"  words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// blockElements=["b"] (all block), maxLineWidth=80: same output as 200,
	// because block elements already force breaks before maxLineWidth matters.
	@Test
	public void testReflow_paragraphAllBlock_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setBlockElements(java.util.Arrays.asList("b"));
		String expected = "<paragraph> Dearest Bob," + lineSeparator() + //
				"  <b>please</b>" + lineSeparator() + //
				"  replace the text content in" + lineSeparator() + //
				"  <b>mixedContentFormatting.xml</b>" + lineSeparator() + //
				"  with something more" + lineSeparator() + //
				"  <b>work appropriate</b>" + lineSeparator() + //
				"  that still includes" + lineSeparator() + //
				"  <b>bolded</b>" + lineSeparator() + //
				"  words." + lineSeparator() + //
				"</paragraph>";
		assertFormat(PARAGRAPH_CONTENT, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Mixed elements (<b> + <div>), blockElements=null (all inline), maxLineWidth=80.
	@Test
	public void testReflow_mixedElements_allInline_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		String expected = "<p> Click <b>here</b> to see the <div>important details</div> and then" + lineSeparator() + //
				"  <b>submit</b> the final <div>report</div> for review." + lineSeparator() + //
				"</p>";
		assertFormat(PARAGRAPH_MIXED_ELEMENTS, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Mixed elements (<b> + <div>), blockElements=["div"] (div is block), maxLineWidth=80.
	@Test
	public void testReflow_mixedElements_bInline_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setBlockElements(java.util.Arrays.asList("div"));
		String expected = "<p> Click <b>here</b> to see the" + lineSeparator() + //
				"  <div>important details</div>" + lineSeparator() + //
				"  and then <b>submit</b> the final" + lineSeparator() + //
				"  <div>report</div>" + lineSeparator() + //
				"  for review." + lineSeparator() + //
				"</p>";
		assertFormat(PARAGRAPH_MIXED_ELEMENTS, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Mixed elements (<b> + <div>), blockElements=["b","div"] (all block), maxLineWidth=80.
	// Use case: all elements in blockElements → all are block → each on own line.
	@Test
	public void testReflow_mixedElements_allBlock_maxLineWidth80() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(80);
		settings.getFormattingSettings().setBlockElements(java.util.Arrays.asList("b", "div"));
		String expected = "<p> Click" + lineSeparator() + //
				"  <b>here</b>" + lineSeparator() + //
				"  to see the" + lineSeparator() + //
				"  <div>important details</div>" + lineSeparator() + //
				"  and then" + lineSeparator() + //
				"  <b>submit</b>" + lineSeparator() + //
				"  the final" + lineSeparator() + //
				"  <div>report</div>" + lineSeparator() + //
				"  for review." + lineSeparator() + //
				"</p>";
		assertFormat(PARAGRAPH_MIXED_ELEMENTS, expected, settings);
		assertFormat(expected, expected, settings);
	}

	// Bug: reflow + joinContentLines=true causes idempotency oscillation on end tag.
	// Pass 1: </update> on its own line. Pass 2: </update> on same line as text.
	// Pass 3: back to pass 1.
	@Test
	public void testReflow_mybatisJoinContentLinesIdempotent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMixedContent(MixedContent.reflow);
		settings.getFormattingSettings().setMaxLineWidth(100);
		settings.getFormattingSettings().setJoinContentLines(true);
		String content = "<update id=\"updateEmp\"> update emp "
				+ "<set>"
				+ "<if test=\"username != null and username != ''\">username=#{username},</if>"
				+ "<if test=\"password != null and password != ''\">password=#{password},</if>"
				+ "<if test=\"name != null and name != ''\">name=#{name},</if>"
				+ "<if test=\"gender != null\">gender=#{gender},</if>"
				+ "<if test=\"job != null\">job=#{job},</if>"
				+ "<if test=\"salary != null\">salary=#{salary},</if>"
				+ "</set> where id=#{id} </update>";
		String expected = "<update id=\"updateEmp\"> update emp" + lineSeparator() +
				"  <set>" + lineSeparator() +
				"    <if test=\"username != null and username != ''\">username=#{username},</if>" + lineSeparator() +
				"    <if test=\"password != null and password != ''\">password=#{password},</if>" + lineSeparator() +
				"    <if test=\"name != null and name != ''\">name=#{name},</if>" + lineSeparator() +
				"    <if test=\"gender != null\">gender=#{gender},</if>" + lineSeparator() +
				"    <if test=\"job != null\">job=#{job},</if>" + lineSeparator() +
				"    <if test=\"salary != null\">salary=#{salary},</if>" + lineSeparator() +
				"  </set> where id=#{id}" + lineSeparator() +
				"</update>";
		assertFormat(content, expected, settings);
		assertFormat(expected, expected, settings);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "test://test.html", expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			TextEdit... expectedEdits) throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, uri, true,
				expectedEdits.length == 0 ? null : expectedEdits);
	}
}
