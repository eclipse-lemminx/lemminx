/**
 *  Copyright (c) 2026 Angelo ZERR.
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

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testAutoInsert;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.customservice.AutoInsertResponse;
import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.junit.jupiter.api.Test;

public class XMLAutoInsertTest {

	// --------------- autoClose tests (kind = "autoClose") ---------------

	@Test
	public void autoCloseBasic() throws BadLocationException {
		testAutoInsert("<div>|", "autoClose", "$0</div>");
	}

	@Test
	public void autoCloseAlreadyClosed() throws BadLocationException {
		testAutoInsert("<div>|</div>", "autoClose", null);
	}

	@Test
	public void autoCloseWithAttributes() throws BadLocationException {
		testAutoInsert("<div class=\"\">|", "autoClose", "$0</div>");
	}

	@Test
	public void autoCloseSelfClosed() throws BadLocationException {
		testAutoInsert("<img />|", "autoClose", null);
	}

	@Test
	public void autoCloseEndTag() throws BadLocationException {
		testAutoInsert("<div><br /></|", "autoClose", "div>$0");
	}

	@Test
	public void autoCloseEndTagWithClosedSiblings() throws BadLocationException {
		testAutoInsert("<div><br /><span></span></|", "autoClose", "div>$0");
	}

	@Test
	public void autoCloseNestedTag() throws BadLocationException {
		testAutoInsert("<a><b>|</a>", "autoClose", "$0</b>");
	}

	@Test
	public void autoCloseNestedTagWithSpaces() throws BadLocationException {
		testAutoInsert("<a>   <b>|</a>", "autoClose", "$0</b>");
	}

	@Test
	public void autoCloseNestedTagNoParentEnd() throws BadLocationException {
		testAutoInsert("<a><b>|", "autoClose", "$0</b>");
	}

	@Test
	public void autoCloseSelfCloseSlash() throws BadLocationException {
		testAutoInsert("<a/|", "autoClose", ">$0");
	}

	@Test
	public void autoCloseSelfCloseSlashBeforeEndTag() throws BadLocationException {
		testAutoInsert("<a/|</b>", "autoClose", ">$0");
	}

	@Test
	public void autoCloseDuplicateTag() throws BadLocationException {
		testAutoInsert("<a><a>|</a>", "autoClose", "$0</a>");
	}

	@Test
	public void autoCloseSelfCloseWithRange() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a/|></a>", "autoClose",
				new AutoInsertResponse(">$0", r(0, 3, 0, 8)), settings);
	}

	@Test
	public void autoCloseSelfCloseWithRangeAndSpaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a/| </a>", "autoClose",
				new AutoInsertResponse(">$0", r(0, 3, 0, 8)), settings);
	}

	@Test
	public void autoCloseSelfCloseWithRangeNested() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a> <a/|> </a> </a>", "autoClose",
				new AutoInsertResponse(">$0", r(0, 7, 0, 13)), settings);
	}

	@Test
	public void autoCloseSelfCloseWithRangeAndAttributes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a var=\"asd\"/|></a>", "autoClose",
				new AutoInsertResponse(">$0", r(0, 13, 0, 18)), settings);
	}

	@Test
	public void autoCloseSlashInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a zz=\"a/|\"></a>", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseSlashBeforeAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a zz=/|\"aa\"> </a>", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseSlashWithSpaceBeforeClose() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a  /|  > </a>", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseSlashInEndTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);
		testAutoInsert("<a> </a/|>", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseRemovesContentMultiLine() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);

		String value = //
				"<a/|\n" + //
				"  <b />\n" + //
				"</a>";
		testAutoInsert(value, "autoClose",
				new AutoInsertResponse(">$0", r(0, 3, 2, 4)), settings);
	}

	@Test
	public void autoCloseDoesntRemoveContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(false);

		String value = //
				"<a/|\n" + //
				"  <b />\n" + //
				"</a>";
		testAutoInsert(value, "autoClose", ">$0", settings);
	}

	@Test
	public void autoCloseWithLeadingTextContent() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		settings.getCompletionSettings().setAutoCloseRemovesContent(true);

		String value = //
				"<a/|\n" + //
				"  content\n" + //
				"</a>";
		testAutoInsert(value, "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseNoEndStartTagInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		testAutoInsert("<aaa attr=\"value>|", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseNoEndStartTagInAttributeValue2() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		testAutoInsert("<aaa attr=\">|value", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseAfterEqualsInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		testAutoInsert("<aaa attr=>|\"", "autoClose", "$0</aaa>", settings);
	}

	@Test
	public void autoCloseNoSelfCloseInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		testAutoInsert("<aaa attr=\"value/|", "autoClose", (String) null, settings);
	}

	@Test
	public void autoCloseNoEndTagInAttributeValue() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(true);
		testAutoInsert("<aaa attr=\"value</|", "autoClose", (String) null, settings);
	}

	// Issue #1505: removing '/' from <tag/> should auto-close to <tag></tag>
	// After deletion, document is <tag> with cursor after '>'
	@Test
	public void autoCloseAfterSlashRemoval() throws BadLocationException {
		testAutoInsert("<tag>|", "autoClose", "$0</tag>");
	}

	@Test
	public void autoCloseAfterSlashRemovalWithAttributes() throws BadLocationException {
		testAutoInsert("<tag attr=\"value\">|", "autoClose", "$0</tag>");
	}

	@Test
	public void autoCloseAfterSlashRemovalAlreadyClosed() throws BadLocationException {
		testAutoInsert("<tag>|</tag>", "autoClose", null);
	}

	@Test
	public void autoCloseNotTriggeredByEquals() throws BadLocationException {
		testAutoInsert("<a foo=|", "autoClose", null);
	}

	// --------------- autoQuote tests (kind = "autoQuote") ---------------

	@Test
	public void autoQuoteBasicDoubleQuotes() throws BadLocationException {
		testAutoInsert("<a foo=|", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteSingleQuotes() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		testAutoInsert("<a foo=|", "autoQuote", "'$1'", settings);
	}

	@Test
	public void autoQuoteDoubleQuotesExplicit() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getPreferences().setQuoteStyle(QuoteStyle.doubleQuotes);
		testAutoInsert("<a foo=|", "autoQuote", "\"$1\"", settings);
	}

	@Test
	public void autoQuoteValueAlreadyExists() throws BadLocationException {
		testAutoInsert("<a foo=|\"bar\"", "autoQuote", null);
	}

	@Test
	public void autoQuoteBeforeClosingTag() throws BadLocationException {
		testAutoInsert("<a foo=|></a>", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteInsideAttributeValue() throws BadLocationException {
		testAutoInsert("<a foo=\"bar=|\"", "autoQuote", null);
	}

	@Test
	public void autoQuoteFollowedByAnotherAttribute() throws BadLocationException {
		testAutoInsert("<a baz=| foo=\"bar\">", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteMultilineAttribute() throws BadLocationException {
		testAutoInsert("<a foo=\"bar\" \n baz=| ></a>", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteNotTriggeredByGreaterThan() throws BadLocationException {
		testAutoInsert("<a>|", "autoQuote", null);
	}

	@Test
	public void autoQuoteNotTriggeredBySlash() throws BadLocationException {
		testAutoInsert("<a/|", "autoQuote", null);
	}

	@Test
	public void autoQuoteInEndTag() throws BadLocationException {
		testAutoInsert("<a></a=|", "autoQuote", null);
	}

	@Test
	public void autoQuoteNoElementContext() throws BadLocationException {
		testAutoInsert("=|", "autoQuote", null);
	}

	@Test
	public void autoQuoteWithNamespace() throws BadLocationException {
		testAutoInsert("<ns:root xmlns:ns=|", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteMultipleAttributes() throws BadLocationException {
		testAutoInsert("<a foo=\"bar\" baz=| qux=\"quux\">", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteFirstAttribute() throws BadLocationException {
		testAutoInsert("<root attr=|", "autoQuote", "\"$1\"");
	}

	@Test
	public void autoQuoteAfterSelfClosingSlash() throws BadLocationException {
		testAutoInsert("<a foo=| />", "autoQuote", "\"$1\"");
	}
}
