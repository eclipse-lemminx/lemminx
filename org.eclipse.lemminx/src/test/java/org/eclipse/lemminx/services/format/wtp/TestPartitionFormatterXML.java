/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.lemminx.services.format.wtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Test from
 * https://git.eclipse.org/c/sourceediting/webtools.sourceediting.git/tree/xml/tests/org.eclipse.wst.xml.core.tests/src/org/eclipse/wst/xml/core/tests/format/TestPartitionFormatterXML.java
 * 
 * @author Angelo ZERR
 *
 */
public class TestPartitionFormatterXML {

	private static final String BASE_ROOT = "src/test/resources/wtp/";

	protected void formatAndAssertEquals(String beforePath, String afterPath) throws Exception {
		formatAndAssertEquals(beforePath, afterPath, new XMLFormattingPreferences());
	}

	private void formatAndAssertEquals(String beforePath, String afterPath, XMLFormattingPreferences prefs)
			throws Exception {
		String xml = readString(new File(BASE_ROOT, beforePath));
		String expected = readString(new File(BASE_ROOT, afterPath));

		XMLLanguageService ls = new XMLLanguageService();
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().merge(prefs);

		String actual = ls.formatFull(xml, "test.xml", settings, () -> {
		});
		Assertions.assertEquals(expected, actual);
	}

	@Test
	@Disabled
	public void testSimpleXml() throws Exception {
		// results are different than old formatter
		// Bug [228495] - Result should have blank lines cleared
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/simple-standalone.xml", "testfiles/xml/simple-standalone-newfmt.xml",
				prefs);
	}

	@Test
	@Disabled
	public void testWhitespaceFormatXSD() throws Exception {
		// Bug 194698
		formatAndAssertEquals("testfiles/xml/xml-whitespace-xsd.xml", "testfiles/xml/xml-whitespace-xsd-actual.xml");
	}

	@Test
	@Disabled
	public void testPreserveFormat() throws Exception {
		// results are different than old formatter
		// Bug [228495] - Result should have blank lines cleared
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/xml-space-preserve-standalone.xml",
				"testfiles/xml/xml-space-preserve-standalone-newfmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testPreserveFormatDTD() throws Exception {
		// results are different than old formatter
		// Bug [228495] - Result should have blank lines cleared
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/xml-space-preserve-dtd.xml",
				"testfiles/xml/xml-space-preserve-dtd-newfmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testOneLineFormat() throws Exception {
		// BUG115716
		formatAndAssertEquals("testfiles/xml/oneline.xml", "testfiles/xml/oneline-fmt.xml");
	}

	@Test
	@Disabled
	public void testOneLineTextNodeFormat() throws Exception {
		// results are different than old formatter
		// BUG166441
		formatAndAssertEquals("testfiles/xml/onelineTextNode.xml", "testfiles/xml/onelineTextNode-newfmt.xml");
	}

	@Test
	@Disabled
	public void testEmptyContentNodeFormat() throws Exception {
		// BUG174243
		// BUG174243
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setIndentMultipleAttributes(true);
		formatAndAssertEquals("testfiles/xml/usetagswithemptycontent.xml",
				"testfiles/xml/usetagswithemptycontent-fmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testXSLFormat() throws Exception {
		// BUG108074
		formatAndAssertEquals("testfiles/xml/xslattributetext.xsl", "testfiles/xml/xslattributetext-fmt.xsl");
	}

	@Test
	@Disabled
	public void testEntityFormat() throws Exception {
		// results are different than old formatter
		// BUG102076
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/entities.xml", "testfiles/xml/entities-newfmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testPreservePCDATAFormat() throws Exception {
		// BUG84688
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setPCDataWhitespaceStrategy(XMLFormattingPreferences.PRESERVE);
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/xml-preservepcdata.xml", "testfiles/xml/xml-preservepcdata-yes-fmt.xml",
				prefs);

		// results are different than old formatter
		prefs.setPCDataWhitespaceStrategy(XMLFormattingPreferences.COLLAPSE);
		formatAndAssertEquals("testfiles/xml/xml-preservepcdata.xml", "testfiles/xml/xml-preservepcdata-no-newfmt.xml",
				prefs);
	}

	@Test
	public void testPreserveCDATAFormat() throws Exception {
		// BUG161330
		formatAndAssertEquals("testfiles/xml/usecdata.xml", "testfiles/xml/usecdata-fmt.xml");
	}

	@Test
	public void testPreserveCDATAFormat2() throws Exception {
		formatAndAssertEquals("testfiles/xml/usecdata2.xml", "testfiles/xml/usecdata2-fmt.xml");
	}

	@Test
	public void testPreserveCDATAFormat3() throws Exception {
		// <script> is treated specially, because it likely contains formatted source code
		formatAndAssertEquals("testfiles/xml/usecdata3.xml", "testfiles/xml/usecdata3-fmt.xml");
	}

	@Test
	@Disabled
	public void testSplitAttributesFormat() throws Exception {
		// BUG113584
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		// the below tests are slighty different from old formatter test
		prefs.setIndentMultipleAttributes(true);
		prefs.setAlignFinalBracket(false);
		formatAndAssertEquals("testfiles/xml/multiattributes2.xml",
				"testfiles/xml/multiattributes2-yessplit-noalign-newfmt.xml", prefs);

		prefs.setIndentMultipleAttributes(false);
		prefs.setAlignFinalBracket(false);
		formatAndAssertEquals("testfiles/xml/multiattributes2.xml",
				"testfiles/xml/multiattributes2-nosplit-noalign-newfmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testAlignEndBracketFormat() throws Exception {
		// results are different than old formatter
		// BUG113584
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setIndentMultipleAttributes(false);
		prefs.setAlignFinalBracket(true);
		formatAndAssertEquals("testfiles/xml/multiattributes.xml",
				"testfiles/xml/multiattributes-nosplit-yesalign-newfmt.xml", prefs);

		// results are different than old formatter
		prefs.setIndentMultipleAttributes(true);
		prefs.setAlignFinalBracket(true);
		formatAndAssertEquals("testfiles/xml/multiattributes.xml",
				"testfiles/xml/multiattributes-yessplit-yesalign-newfmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testSpaceBeforeEmptyCloseTag() throws Exception {
		// Bug 195264
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setSpaceBeforeEmptyCloseTag(false);
		formatAndAssertEquals("testfiles/xml/xml-empty-tag-space.xml",
				"testfiles/xml/xml-empty-tag-space-none-newfmt.xml", prefs);

		prefs.setSpaceBeforeEmptyCloseTag(true);
		formatAndAssertEquals("testfiles/xml/xml-empty-tag-space.xml", "testfiles/xml/xml-empty-tag-space-newfmt.xml",
				prefs);
	}

	@Test
	@Disabled
	public void testProcessingInstruction() throws Exception {
		// BUG198297
		formatAndAssertEquals("testfiles/xml/processinginstruction.xml", "testfiles/xml/processinginstruction-fmt.xml");
	}

	@Test
	@Disabled
	public void testComments() throws Exception {
		// Bug 226821
		formatAndAssertEquals("testfiles/xml/xml-comment.xml", "testfiles/xml/xml-comment-newfmt.xml");
	}

	@Test
	public void testComments_short_NoText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-short-NoText.xml",
				"testfiles/xml/xml-comment-short-NoText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_short_InbetweenText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-short-InbetweenText.xml",
				"testfiles/xml/xml-comment-short-InbetweenText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_short_SameLineText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-short-SameLineText.xml",
				"testfiles/xml/xml-comment-short-SameLineText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_short_EverywhereText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-short-EverywhereText.xml",
				"testfiles/xml/xml-comment-short-EverywhereText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_long_NoText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-long-NoText.xml",
				"testfiles/xml/xml-comment-long-NoText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_long_InbetweenText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-long-InbetweenText.xml",
				"testfiles/xml/xml-comment-long-InbetweenText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_long_SameLineText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-long-SameLineText.xml",
				"testfiles/xml/xml-comment-long-SameLineText-formated.xml");
	}

	@Test
	@Disabled
	public void testComments_long_EverywhereText() throws Exception {
		// Bug 258512
		formatAndAssertEquals("testfiles/xml/xml-comment-long-EverywhereText.xml",
				"testfiles/xml/xml-comment-long-EverywhereText-formated.xml");
	}

	@Test
	@Disabled
	public void testKeepEmptyLines() throws Exception {
		// Bug 228495
		// Test that formatting keeps empty lines
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(false);
		formatAndAssertEquals("testfiles/xml/xml-keep-blank-lines.xml", "testfiles/xml/xml-keep-blank-lines-fmt.xml",
				prefs);
	}

	@Test
	@Disabled
	public void testClearBlankLines() throws Exception {
		// Bug 228495
		// Test that formatting clears empty lines
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setClearAllBlankLines(true);
		formatAndAssertEquals("testfiles/xml/xml-keep-blank-lines.xml", "testfiles/xml/xml-clear-blank-lines-fmt.xml",
				prefs);
	}

	@Test
	@Disabled
	public void testFormatMalformedEndTag() throws Exception {
		// Bug 221279
		// Test that malformed end tags do not cause a NPE and format the document
		formatAndAssertEquals("testfiles/xml/xml-221279.xml", "testfiles/xml/xml-221279-fmt.xml");
	}

	@Test
	@Disabled
	public void testFormatWithFracturedXMLContent() throws Exception {
		// Bug 229135
		// Test that text content that is split into multiple document regions does not
		// stop the formatter
		formatAndAssertEquals("testfiles/xml/xml-229135.xml", "testfiles/xml/xml-229135-fmt.xml");
	}

	@Test
	@Disabled
	public void testFormatCommentsJoinLinesDisabled() throws Exception {
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setJoinCommentLines(false);
		formatAndAssertEquals("testfiles/xml/xml-join-lines-disabled.xml",
				"testfiles/xml/xml-join-lines-disabled-fmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testFormatCommentsDisabled() throws Exception {
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setFormatCommentText(false);
		formatAndAssertEquals("testfiles/xml/xml-format-comments-disabled.xml",
				"testfiles/xml/xml-format-comments-disabled-fmt.xml", prefs);
	}

	@Test
	@Disabled
	public void testFormatDocumentLevelComment() throws Exception {
		XMLFormattingPreferences prefs = new XMLFormattingPreferences();
		prefs.setJoinCommentLines(false);
		formatAndAssertEquals("testfiles/xml/xml-format-document-level-comment.xml",
				"testfiles/xml/xml-format-document-level-comment-fmt.xml", prefs);
	}

	@Test
	public void testFormatDocumentLevelShortComment() throws Exception {
		formatAndAssertEquals("testfiles/xml/xml-format-document-level-short-comment.xml",
				"testfiles/xml/xml-format-document-level-short-comment-fmt.xml");
	}

	@Test
	@Disabled
	public void testNestedEndTag() throws Exception {
		formatAndAssertEquals("testfiles/xml/nested-endtag.xml", "testfiles/xml/nested-endtag-fmt.xml");
	}

	private static String readString(File file) throws FileNotFoundException, IOException {
		try (InputStream in = new FileInputStream(file)) {
			return IOUtils.convertStreamToString(in);
		}
	}

}