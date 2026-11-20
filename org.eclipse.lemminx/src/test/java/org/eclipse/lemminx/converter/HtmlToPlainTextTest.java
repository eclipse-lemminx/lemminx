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
package org.eclipse.lemminx.converter;

import static org.eclipse.lemminx.converter.plaintext.PlainTextConverter.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class HtmlToPlainTextTest {

	@Test
	public void testSafeLink() {
		assertEquals("Click here <https://example.com>",
				convert("<a href=\"https://example.com\">Click here</a>"));
	}

	@Test
	public void testJavascriptLinkBlocked() {
		String result = convert("<a href=\"javascript:alert('xss')\">Click</a>");
		assertEquals("Click", result);
		assertFalse(result.contains("javascript:"));
	}

	@Test
	public void testVbscriptLinkBlocked() {
		String result = convert("<a href=\"vbscript:payload\">Click</a>");
		assertEquals("Click", result);
		assertFalse(result.contains("vbscript:"));
	}

	@Test
	public void testDataLinkBlocked() {
		String result = convert("<a href=\"data:text/html,payload\">Click</a>");
		assertEquals("Click", result);
		assertFalse(result.contains("data:"));
	}

	@Test
	public void testMailtoLinkAllowed() {
		assertEquals("Contact <mailto:user@example.com>",
				convert("<a href=\"mailto:user@example.com\">Contact</a>"));
	}

	@Test
	public void testFileLinkAllowed() {
		assertEquals("File <file:///C:/path/file.txt>",
				convert("<a href=\"file:///C:/path/file.txt\">File</a>"));
	}

	@Test
	public void testRelativeLinkAllowed() {
		assertEquals("Link <../path/file.html>",
				convert("<a href=\"../path/file.html\">Link</a>"));
	}

	@Test
	public void testJavascriptUppercaseBlocked() {
		String result = convert("<a href=\"JAVASCRIPT:void(0)\">Click</a>");
		assertEquals("Click", result);
	}

	@Test
	public void testBrTag() {
		assertEquals("Line 1\nLine 2",
				convert("Line 1<br>Line 2"));
	}

	@Test
	public void testBoldAndCode() {
		assertEquals("This is bold code",
				convert("This is <b>bold</b> <code>code</code>"));
	}
}
