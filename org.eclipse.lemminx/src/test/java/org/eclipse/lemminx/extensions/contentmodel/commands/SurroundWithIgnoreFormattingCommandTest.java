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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import static org.eclipse.lemminx.XMLAssert.assertSurroundWith;

import org.eclipse.lemminx.extensions.contentmodel.BaseFileTempTest;
import org.eclipse.lemminx.extensions.contentmodel.commands.SurroundWithCommand.SurroundWithKind;
import org.junit.jupiter.api.Test;

/**
 * Tests with {@link SurroundWithCommand} command with Ignore Formatting.
 *
 */
public class SurroundWithIgnoreFormattingCommandTest extends BaseFileTempTest {

	// --------------- Surround with Ignore Formatting

	@Test
	public void surroundInText() throws Exception {
		String xml = "<foo>\r\n" + //
				"	s|ome te|xt\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"	s<!-- @formatter:off -->ome te<!-- @formatter:on -->$0xt\r\n" + //
				"</foo>";
		assertSurroundWith(xml, SurroundWithKind.ignoreFormatting, true, expected);
	}

	@Test
	public void surroundInStartTag() throws Exception {
		String xml = "<fo|o>\r\n" + //
				"	som|e text\r\n" + //
				"</foo>";
		String expected = "<fo<!-- @formatter:off -->o>\r\n" + //
				"	som<!-- @formatter:on -->$0e text\r\n" + //
				"</foo>";
		assertSurroundWith(xml, SurroundWithKind.ignoreFormatting, true, expected);
	}

	@Test
	public void surroundEmptySelectionInText() throws Exception {
		String xml = "<foo>\r\n" + //
				"	s|ome text\r\n" + //
				"</foo>";
		String expected = "<foo>\r\n" + //
				"	s<!-- @formatter:off -->$1<!-- @formatter:on -->$0ome text\r\n" + //
				"</foo>";
		assertSurroundWith(xml, SurroundWithKind.ignoreFormatting, true, expected);
	}

	@Test
	public void surroundEmptySelectionInStartTag() throws Exception {
		String xml = "<f|oo>\r\n" + //
				"	some text\r\n" + //
				"</foo>";
		String expected = "<!-- @formatter:off -->$1<foo>\r\n" + //
				"	some text\r\n" + //
				"</foo><!-- @formatter:on -->$0";
		assertSurroundWith(xml, SurroundWithKind.ignoreFormatting, true, expected);
	}

	@Test
	public void surroundEmptySelectionInEndTag() throws Exception {
		String xml = "<foo>\r\n" + //
				"	some text\r\n" + //
				"</fo|o>";
		String expected = "<!-- @formatter:off -->$1<foo>\r\n" + //
				"	some text\r\n" + //
				"</foo><!-- @formatter:on -->$0";
		assertSurroundWith(xml, SurroundWithKind.ignoreFormatting, true, expected);
	}

}
