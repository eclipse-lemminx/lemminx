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
 * xml bound to XSD.
 *
 */
public class XMLFormatterWithXSDGrammarAwareFormattingTest {
	@Test
	public void testXSDForMixedElement() throws Exception {
		String content = "<?xml-model href=\"xsd/mixed-element.xsd\"?>\r\n" + //
				"<mixedElement> text \r\n" + // <-- mixedElement is defined in DTD as mixed type, should join content
				"   content   </mixedElement>\r\n" + //
				"<notMixed> text\r\n" + // <-- notMixed is NOT defined in DTD as mixed type, should NOT join content
				"  content </notMixed>\r\n";
		String expected = "<?xml-model href=\"xsd/mixed-element.xsd\"?>\r\n" + //
				"<mixedElement> text content </mixedElement>\r\n" + //
				"<notMixed> text\r\n" + //
				"  content </notMixed>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		settings.getFormattingSettings().setGrammarAwareFormatting(true);
		assertFormat(content, expected, settings, //
				te(1, 19, 2, 3, " "), //
				te(2, 10, 2, 13, " "), //
				te(4, 21, 5, 0, ""));
		assertFormat(expected, expected, settings);
	}

	@Test
	public void testXSDForEmptyMixedElement() throws Exception {
		String content = "<?xml-model href=\"xsd/mixed-element.xsd\"?>\r\n"
				+ "<mixedElement></mixedElement>";
		String expected = content;
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		settings.getFormattingSettings().setGrammarAwareFormatting(true);
		try {
			assertFormat(content, expected, settings);
		} catch (Exception ex) {
			fail("Formatter failed to process text", ex);
		}
	}

	@Test
	public void testXSDForPreserveSpaceWithStringContent() throws Exception {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 src/test/resources/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <description>a    b     c</description>\r\n" + //
				"  <description2>a    b     c</description2>\r\n" + //
				"</project>";

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 src/test/resources/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <description>a    b     c</description>\r\n" + // <-- preserve space because description is xs:string
				"  <description2>a    b     c</description2>\r\n" + // <-- preserve space because description2 is not
																	// in schema
				"</project>";

		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		settings.getFormattingSettings().setGrammarAwareFormatting(true);
		assertFormat(content, expected, settings, "test.xml", true);
		assertFormat(expected, expected, settings, "test.xml", true);
	}

	@Test
	public void testXSDForPreserveSpaceWithStringContentWithMaxLineWidth() throws Exception {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 src/test/resources/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <description>a    b     c</description>\r\n" + //
				"  <description2>a    b     c</description2>\r\n" + //
				"</project>";

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"  xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 src/test/resources/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"  <description>a    b     c</description>\r\n" + // <-- preserve space because description is xs:string
				"  <description2>a    b     c</description2>\r\n" + // <-- preserve space because description2 is not
																	// in schema
				"</project>";

		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setGrammarAwareFormatting(true);
		settings.getFormattingSettings().setMaxLineWidth(80);
		assertFormat(content, expected, settings, "test.xml", true, //
				te(1, 50, 1, 51, "\r\n  "), //
				te(1, 104, 1, 105, "\r\n  "));
		assertFormat(expected, expected, settings, "test.xml", true);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "src/test/resources/test.xml", true, expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			Boolean considerRangeFormat, TextEdit... expectedEdits) throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, uri, true, expectedEdits);
	}
}