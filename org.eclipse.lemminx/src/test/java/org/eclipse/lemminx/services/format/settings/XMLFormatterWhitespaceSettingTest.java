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

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

/**
 * XML formatter services tests for general whitespace behavior.
 *
 */
public class XMLFormatterWhitespaceSettingTest {

	// https://github.com/redhat-developer/vscode-xml/issues/1026
	// Use case: multiple spaces between words in text content must be preserved.
	// <Inner>Test     Spaces</Inner> => <Inner>Test     Spaces</Inner>
	@Test
	public void testPreserveInternalWhitespaces() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		String content = "<Tag><Inner>Test     Spaces</Inner></Tag>";
		String expected = "<Tag>" + lineSeparator() + //
				"  <Inner>Test     Spaces</Inner>" + lineSeparator() + //
				"</Tag>";
		assertFormat(content, expected, settings, //
				te(0, 5, 0, 5, lineSeparator() + "  "), //
				te(0, 35, 0, 35, lineSeparator()));
		assertFormat(expected, expected, settings);
	}

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
