/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.services;

import static org.eclipse.lemminx.XMLAssert.CDATA_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.COMMENT_SNIPPETS;
import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCompletionFor;
import static org.eclipse.lemminx.XMLAssert.testTagCompletion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.AutoCloseTagResponse;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XML completion services tests
 *
 */
public class XMLCompletionTest {

	private XMLLanguageService languageService;

	@BeforeEach
	public void initializeLanguageService() {
		languageService = new XMLLanguageService();
	}

	@Test
	public void successfulEndTagCompletion() throws BadLocationException {
		testCompletionFor("<a>|", 1 + 2 /* CDATA and Comments */, c("End with '</a>'", "</a>", r(0, 3, 0, 3), "</a>"));
		testCompletionFor("<a>a|", 1 + 2 /* CDATA and Comments */, c("End with '</a>'", "</a>", r(0, 3, 0, 4), "</a>"));
		testCompletionFor("<a><|", 1 + 2 /* CDATA and Comments */, c("End with '</a>'", "/a>", r(0, 4, 0, 4), "/a>"));
		testCompletionFor("<a></|", 1, c("End with '</a>'", "/a>", r(0, 4, 0, 5), "/a>"));

		testCompletionFor("<a><b>|</a>", 1 + 2 /* CDATA and Comments */,
				c("End with '</b>'", "</b>", r(0, 6, 0, 6), "</b>"));
		testCompletionFor("<a><b><|</a>", 1 + 2 /* CDATA and Comments */,
				c("End with '</b>'", "/b>", r(0, 7, 0, 7), "/b>"));
		testCompletionFor("<a><b></|</a>", 1, c("End with '</b>'", "/b>", r(0, 7, 0, 8), "/b>"));

		testCompletionFor("<a>   <b>|</a>", 1 + 2 /* CDATA and Comments */,
				c("End with '</b>'", "</b>", r(0, 9, 0, 9), "</b>"));
		testCompletionFor("<a>   <b><|</a>", 1 + 2 /* CDATA and Comments */,
				c("End with '</b>'", "/b>", r(0, 10, 0, 10), "/b>"));
		testCompletionFor("<a>   <b></|</a>", 1, c("End with '</b>'", "/b>", r(0, 10, 0, 11), "/b>"));

		testCompletionFor("<a><b>|", 2 + 2 /* CDATA and Comments */,
				c("End with '</b>'", "</b>", r(0, 6, 0, 6), "</b>"),
				c("End with '</a>'", "</a>", r(0, 6, 0, 6), "</a>"));
		testCompletionFor("<a><b><|", 2 + 2 /* CDATA and Comments */, c("End with '</b>'", "/b>", r(0, 7, 0, 7), "/b>"),
				c("End with '</a>'", "/a>", r(0, 7, 0, 7), "/a>"));
		testCompletionFor("<a><b></|", 2, c("End with '</b>'", "/b>", r(0, 7, 0, 8), "/b>"),
				c("End with '</a>'", "/a>", r(0, 7, 0, 8), "/a>"));
	}

	@Test
	public void successfulEndTagCompletionWithIndent() throws BadLocationException {

		testCompletionFor("<a></|", 1, c("End with '</a>'", "/a>", r(0, 4, 0, 5), "/a>"));

		testCompletionFor("  <a>\r\n" + //
				"     <b>\r\n" + //
				"<|", 2 + 2 /* CDATA and Comments */, //
				c("End with '</b>'", "     </b>", r(2, 0, 2, 1), "</b>"), //
				c("End with '</a>'", "  </a>", r(2, 0, 2, 1), "</a>"));
	}

	@Test
	public void unneededEndTagCompletion() throws BadLocationException {
		testCompletionFor("<a>|</a>", 0 + 2 /* CDATA and Comments */);
		testCompletionFor("<a><|</a>", 0 + 2 /* CDATA and Comments */);
		testCompletionFor("<a></|</a>", 0);

		testCompletionFor("<a><b>|</b></a>", 0 + 2 /* CDATA and Comments */);
		testCompletionFor("<a><b><|</b></a>", 0 + 2 /* CDATA and Comments */);
		testCompletionFor("<a><b></|</b></a>", 0);
	}

	@Test
	public void startTagOpenBracket() throws BadLocationException {
		testCompletionFor("<hello><h|</hello>", 1 + //
				CDATA_SNIPPETS /* CDATA snippets */ + //
				COMMENT_SNIPPETS /* Comment snippets */ , //
				c("h", "<h></h>", "<h"));
		testCompletionFor("<hello><h1/><h2></h2><h|</hello>", 3 + //
				CDATA_SNIPPETS /* CDATA snippets */ + //
				COMMENT_SNIPPETS /* Comment snippets */ , //
				c("h", "<h></h>", "<h"), c("h1", "<h1 />", "<h1"), c("h2", "<h2></h2>", "<h2"));
	}

	@Test
	public void replaceRangeOUnusedfCloseTag() throws BadLocationException {
		testCompletionFor("<hello><h|></hello>", 1, c("h", "<h></h>", "<h"));
		testCompletionFor("<hello><h1/><h2></h2><h|</hello>", 3 + //
				CDATA_SNIPPETS /* CDATA snippets */ + //
				COMMENT_SNIPPETS /* Comment snippets */ , //
				c("h", "<h></h>", "<h"), //
				c("h1", "<h1 />", "<h1"), //
				c("h2", "<h2></h2>", "<h2"));
	}

	@Test
	public void completionBasedOnParent() throws BadLocationException {
		testCompletionFor("<a><b />|</a>", 1 + 2 /* CDATA and Comments */, c("b", "<b />", r(0, 8, 0, 8), "b"));
		testCompletionFor("<a><b /><|</a>", 1 + 2 /* CDATA and Comments */, c("b", "<b />", r(0, 8, 0, 9), "<b"));
		testCompletionFor("<a>|</b></a>", 1 + 2 /* CDATA and Comments */, c("b", "<b>", r(0, 3, 0, 3), "b"));
		testCompletionFor("<a><|b</b></a>", c("b", "<b>", r(0, 3, 0, 5), "<b"));
	}

	@Test
	public void doTagComplete() throws BadLocationException {
		testTagCompletion("<div>|", "$0</div>");
		testTagCompletion("<div>|</div>", null);
		testTagCompletion("<div class=\"\">|", "$0</div>");
		testTagCompletion("<img />|", null);
		testTagCompletion("<div><br /></|", "div>$0");
		testTagCompletion("<div><br /><span></span></|", "div>$0");
		// testTagCompletion("<div><h1><br /><span></span><img /></| </h1></div>",
		// "h1>");
	}

	@Test
	public void testAutoCloseTagCompletion() {
		assertAutoCloseEndTagCompletion("<a>|", "$0</a>");
		assertAutoCloseEndTagCompletion("<a><b>|</a>", "$0</b>");
		assertAutoCloseEndTagCompletion("<a>   <b>|</a>", "$0</b>");
		assertAutoCloseEndTagCompletion("<a><b>|", "$0</b>");
		assertAutoCloseEndTagCompletion("<a></|", "a>$0");
		assertAutoCloseEndTagCompletion("<a/|", ">$0");
		assertAutoCloseEndTagCompletion("<a/|</b>", ">$0");
		assertAutoCloseEndTagCompletion("<a><a>|</a>", "$0</a>");
	}

	@Test
	public void testAutoCloseTagCompletionWithRange() {
		assertAutoCloseEndTagCompletionWithRange("<a/|></a>", ">$0", r(0, 3, 0, 8));
		assertAutoCloseEndTagCompletionWithRange("<a/| </a>", ">$0", r(0, 3, 0, 8));
		assertAutoCloseEndTagCompletionWithRange("<a> <a/|> </a> </a>", ">$0", r(0, 7, 0, 13));
		assertAutoCloseEndTagCompletionWithRange("<a var=\"asd\"/|></a>", ">$0", r(0, 13, 0, 18));
		assertAutoCloseEndTagCompletionWithRange("<a  var=\"asd\"  /| </a>", ">$0", r(0, 16, 0, 21));
		assertAutoCloseEndTagCompletionWithRange("<aB/|></aB>", ">$0", r(0, 4, 0, 10));
	}

	@Test
	public void testAutoCloseTagCompletionWithSlashAtBadLocations() {
		assertAutoCloseEndTagCompletionWithRange("<a zz=\"a/|\"></a>", null, null);
		assertAutoCloseEndTagCompletionWithRange("<a zz=/|\"aa\"> </a>", null, null);
		assertAutoCloseEndTagCompletionWithRange("<a  /|  > </a>", null, null);
		assertAutoCloseEndTagCompletionWithRange("<a> </a/|>", null, null);
		assertAutoCloseEndTagCompletionWithRange("<a> </|a>", null, null);

	}

	@Test
	public void testAutoCloseTagCompletionNoAttributes() {
		assertAutoCloseEndTagCompletionWithRange("<a/|></a>", ">$0", r(0, 3, 0, 8));
		assertAutoCloseEndTagCompletion("<a/|", ">$0");
	}

	@Test
	public void testAutoCloseEnabledDisabled() throws BadLocationException {
		testCompletionFor("<a><div|<a>", false, c("div", "<div>"));
		testCompletionFor("<a><div|<a>", true, c("div", "<div></div>"));
		testCompletionFor("<a>  <div|    <a>", false, c("div", "<div>"));
		testCompletionFor("<a>   <div|    <a>", true, c("div", "<div></div>"));
		assertAutoCloseEndTagCompletionWithRange("<a/|>Text</a>", null, null);
	}

	@Test
	public void testAutoCloseTagCompletionRemovesContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);


		String value = //
				"<a/|\n" + //
				"  <b />\n" + //
				"</a>";
		AutoCloseTagResponse closeTagResponse = new AutoCloseTagResponse(">$0", r(0, 3, 2, 4));

		XMLAssert.testTagCompletion(value, closeTagResponse, settings);
	}

	@Test
	public void testAutoCloseTagCompletionDoesntRemoveContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(false);

		String value = //
				"<a/|\n" + //
				"  <b />\n" + //
				"</a>";
		AutoCloseTagResponse closeTagResponse = new AutoCloseTagResponse(">$0");

		XMLAssert.testTagCompletion(value, closeTagResponse, settings);
	}

	@Test
	public void testAutoCloseTagCompletionWithLeadingTextContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);

		String value = //
				"<a/|\n" + //
				"  content\n" + //
				"</a>";

		XMLAssert.testTagCompletion(value, (AutoCloseTagResponse) null, settings);
	}

	@Test
	public void testnoCDATANPE() {
		try {
			testCompletionFor("<a> <![CDATA[<b>foo</b>]]| </a>", 1);
		} catch (BadLocationException e) {
			fail();
		}
	}

	@Test
	public void testNoEndStartTagInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		String xml = "<aaa attr=\"value>|";
		XMLAssert.testTagCompletion(xml, (AutoCloseTagResponse) null, settings);
		xml = "<aaa attr=\">|value";
		XMLAssert.testTagCompletion(xml, (AutoCloseTagResponse) null, settings);
		xml = "<aaa attr=>|\"";
		XMLAssert.testTagCompletion(xml, new AutoCloseTagResponse("$0</aaa>"), settings);
	}

	@Test
	public void testNoSelfCloseTagInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		String xml = "<aaa attr=\"value/|";
		XMLAssert.testTagCompletion(xml, (AutoCloseTagResponse) null, settings);
	}

	@Test
	public void testNoEndTagInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		String xml = "<aaa attr=\"value</|";
		XMLAssert.testTagCompletion(xml, (AutoCloseTagResponse) null, settings);
	}

	// -------------------Tools----------------------------------------------------------

	public void assertOpenStartTagCompletion(String xmlText, int expectedStartTagOffset, boolean startWithTagOpen,
			String... expectedTag) {

		List<String> expectedTags = Arrays.asList(expectedTag);
		int offset = getOffset(xmlText);
		DOMDocument xmlDocument = initializeXMLDocument(xmlText, offset);
		CompletionList completionList = initializeCompletion(xmlText, xmlDocument, offset);
		String currentTag, currentTextEdit;
		CompletionItem completionItem;

		assertEquals(expectedTag.length, completionList.getItems().size());
		for (int i = 0; i < expectedTag.length; i++) {
			currentTag = expectedTags.get(i);
			currentTextEdit = createTextEditElement(currentTag);
			completionItem = completionList.getItems().get(i);

			assertEquals(currentTag, completionItem.getLabel());
			assertEquals(startWithTagOpen ? "<" + currentTag : currentTag, completionItem.getFilterText());
			try {
				Range range = completionItem.getTextEdit().getLeft().getRange();
				assertEquals(expectedStartTagOffset, xmlDocument.offsetAt(range.getStart()));
			} catch (Exception e) {
				fail("Couldn't get offset at position");
			}
			assertEquals(currentTextEdit, completionItem.getTextEdit().getLeft().getNewText());
		}
	}

	public void assertAutoCloseEndTagCompletion(String xmlText, String expectedTextEdit) {
		assertAutoCloseEndTagCompletionWithRange(xmlText, expectedTextEdit, null);
	}

	public void assertAutoCloseEndTagCompletionWithRange(String xmlText, String expectedTextEdit, Range range) {
		assertAutoCloseEndTagCompletionWithRange(xmlText, expectedTextEdit, range, new SharedSettings());
	}

	public void assertAutoCloseEndTagCompletionWithRange(String xmlText, String expectedTextEdit, Range range, SharedSettings settings) {
		int offset = getOffset(xmlText);
		DOMDocument xmlDocument = initializeXMLDocument(xmlText, offset);
		Position position = null;
		try {
			position = xmlDocument.positionAt(offset);
		} catch (Exception e) {
			fail("Couldn't get position at offset");
		}
		AutoCloseTagResponse response = languageService.doTagComplete(xmlDocument, settings.getCompletionSettings(), position);
		if (response == null) {
			assertNull(expectedTextEdit);
			assertNull(range);
			return;
		}
		String completionList = response.snippet;
		assertEquals(expectedTextEdit, completionList);
		assertEquals(range, response.range);
	}

	public int getOffset(String xmlText) {
		return xmlText.indexOf("|");
	}

	public DOMDocument initializeXMLDocument(String xmlText, int offset) {
		xmlText = xmlText.substring(0, offset) + xmlText.substring(offset + 1);
		return DOMParser.getInstance().parse(xmlText, "test:uri", null);
	}

	public CompletionList initializeCompletion(String xmlText, DOMDocument xmlDocument, int offset) {
		Position position = null;
		try {
			position = xmlDocument.positionAt(offset);
		} catch (Exception e) {
			fail("Couldn't get position at offset");
		}

		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getFormattingSettings().setTabSize(4);
		sharedSettings.getFormattingSettings().setInsertSpaces(false);

		CompletionList completionList = languageService.doComplete(xmlDocument, position, sharedSettings);
		return completionList;
	}

	// TextEdits are created with the "<" symbol already existing and offset set to
	// the first character
	// of the tag name
	public String createTextEditElement(String tag) {
		return tag + ">$0</" + tag + ">";
	}
}
