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
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests with grammar aware formatting with
 * xml bound to DTD.
 *
 */
public class XMLFormatterWithDTDGrammarAwareFormattingTest {
	@Test
	public void testDTDForMixedElement() throws BadLocationException {
		String content = "<?xml-model href=\"dtd/mixed-element.dtd\"?>\r\n" + //
				"<mixedElement> text \r\n" + // <-- mixedElement is defined in DTD as mixed type, should join content
				"   content   </mixedElement>\r\n" + //
				"<notMixed> text\r\n" + // <-- notMixed is NOT defined in DTD as mixed type, should NOT join content
				"  content </notMixed>\r\n";
		String expected = "<?xml-model href=\"dtd/mixed-element.dtd\"?>\r\n" + //
				"<mixedElement> text content </mixedElement>\r\n" + //
				"<notMixed> text\r\n" + //
				"  content </notMixed>";
		assertFormat(content, expected, //
				te(1, 19, 2, 3, " "), //
				te(2, 10, 2, 13, " "), //
				te(4, 21, 5, 0, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testDTDWithManyEntities() throws BadLocationException {
		// See https://github.com/eclipse-lemminx/lemminx/issues/1651
		String content = "<?xml-model href=\"dtd/many-entities.dtd\"?>\r\n" + //
				"<root> text \r\n" + //
				"   content   </root>\r\n";
		String expected = "<?xml-model href=\"dtd/many-entities.dtd\"?>\r\n" + //
				"<root> text content </root>";
		assertFormat(content, expected, //
				te(1, 11, 2, 3, " "), //
				te(2, 10, 2, 13, " "), //
				te(2, 20, 3, 0, ""));
		assertFormat(expected, expected);
	}

	@Test
	public void testDTDForEmptyMixedElement() throws BadLocationException {
		String content = "<?xml-model href=\"dtd/mixed-element.dtd\"?>\r\n" + //
				"<mixedElement></mixedElement>";
		String expected = content;
		// This should not generate any NullPointerException inside formatter
		try {
			assertFormat(expected, expected);
		} catch (Exception ex) {
			fail("Formatter failed to process text", ex);
		}
	}

	private static void assertFormat(String unformatted, String actual, TextEdit... expectedEdits)
			throws BadLocationException {
		assertFormat(unformatted, actual, new SharedSettings(), expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "src/test/resources/test.xml", expectedEdits);
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