/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.core;

import static org.eclipse.lemminx.XMLAssert.testInlineCompletionFor;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * XML inline completion tests for closing tags.
 *
 * @see org.eclipse.lemminx.extensions.core.participants.inlinecompletion.XMLCloseTagInlineCompletionParticipant
 */
public class XMLCloseTagInlineCompletionTest {

	@Test
	public void testCloseRootElement() throws BadLocationException {
		String xml = "<root>|";
		testInlineCompletionFor(xml, "</root>");
	}

	@Test
	public void testCloseNestedElement() throws BadLocationException {
		String xml = "<root><child>|</root>";
		testInlineCompletionFor(xml, "</child>");
	}

	@Test
	public void testCloseElementWithAttributes() throws BadLocationException {
		String xml = "<root attr=\"value\">|";
		testInlineCompletionFor(xml, "</root>");
	}

	@Test
	public void testNoSuggestionForClosedElement() throws BadLocationException {
		String xml = "<root>|</root>";
		testInlineCompletionFor(xml, 0);
	}

	@Test
	public void testNoSuggestionForSelfClosedElement() throws BadLocationException {
		String xml = "<root/>|";
		testInlineCompletionFor(xml, 0);
	}

	@Test
	public void testCloseMultipleNestedElements() throws BadLocationException {
		String xml = "<root><parent><child>|</root>";
		testInlineCompletionFor(xml, "</child>");
	}

	@Test
	public void testCloseAfterText() throws BadLocationException {
		String xml = "<root>text|";
		testInlineCompletionFor(xml, "</root>");
	}

	@Test
	public void testCloseAfterWhitespace() throws BadLocationException {
		String xml = "<root>  |";
		testInlineCompletionFor(xml, "</root>");
	}
}