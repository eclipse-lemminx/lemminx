/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.hl;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testHighlightsFor;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.junit.jupiter.api.Test;

public class RNCDocumentHighlightTest {

	@Test
	public void testHighlightDefineAndReference() throws BadLocationException {
		String value = "root = element root { text }" + LS +
				"start = |root";
		testHighlightsFor(value,
				hl(r(0, 0, 0, 4), DocumentHighlightKind.Write),
				hl(r(1, 8, 1, 12), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightOnDefinition() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		testHighlightsFor(value,
				hl(r(0, 0, 0, 4), DocumentHighlightKind.Write),
				hl(r(1, 8, 1, 12), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightNoMatch() throws BadLocationException {
		testHighlightsFor("root = |element root { text }");
	}

	@Test
	public void testHighlightMultipleReferences() throws BadLocationException {
		String value = "inline = text" + LS +
				"root = element root { |inline, inline }";
		testHighlightsFor(value,
				hl(r(0, 0, 0, 6), DocumentHighlightKind.Write),
				hl(r(1, 22, 1, 28), DocumentHighlightKind.Read),
				hl(r(1, 30, 1, 36), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightElementName() throws BadLocationException {
		String value = "root = element |foo { text }";
		testHighlightsFor(value,
				hl(r(0, 15, 0, 18), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightAttributeName() throws BadLocationException {
		String value = "root = element foo { attribute |bar { text } }";
		testHighlightsFor(value,
				hl(r(0, 31, 0, 34), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightIdentifierNotElementName() throws BadLocationException {
		String value = "root = element root { |unknown }";
		testHighlightsFor(value);
	}

	@Test
	public void testHighlightChoiceAssign() throws BadLocationException {
		// Place cursor on the define name "tune" at line 3 (not on |= which contains |)
		String value = "start = element songs {" + LS +
				"    tune*" + LS +
				"}" + LS +
				"|tune = element tune {" + LS +
				"    attribute name { text }," + LS +
				"    attribute artist { text }" + LS +
				"}" + LS +
				"tune &= element tune { text }";
		testHighlightsFor(value,
				hl(r(3, 0, 3, 4), DocumentHighlightKind.Write),
				hl(r(7, 0, 7, 4), DocumentHighlightKind.Write),
				hl(r(1, 4, 1, 8), DocumentHighlightKind.Read));
	}

	@Test
	public void testHighlightElementNameSameAsDefine() throws BadLocationException {
		// Use case: "foo = element foo" → clicking on second "foo" (element name)
		// should highlight just that name, not the define
		String value = "foo = element |foo { text }";
		testHighlightsFor(value,
				hl(r(0, 14, 0, 17), DocumentHighlightKind.Read));
	}
}
