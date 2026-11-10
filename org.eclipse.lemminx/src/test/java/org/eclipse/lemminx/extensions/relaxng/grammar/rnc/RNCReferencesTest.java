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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testReferencesFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.l;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

public class RNCReferencesTest {

	@Test
	public void testReferencesFromDefineDeclaration() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		testReferencesFor(value,
				l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesFromReference() throws BadLocationException {
		String value = "root = element root { text }" + LS +
				"start = |root";
		testReferencesFor(value,
				l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesMultiple() throws BadLocationException {
		String value = "|inline = text" + LS +
				"root = element root { inline, inline }";
		testReferencesFor(value,
				l(r(1, 22, 1, 28)),
				l(r(1, 30, 1, 36)));
	}

	@Test
	public void testReferencesNoMatch() throws BadLocationException {
		String value = "root = |element root { text }";
		testReferencesFor(value);
	}

	@Test
	public void testReferencesDefineAfterCursor() throws BadLocationException {
		String value = "start = |content" + LS +
				"content = element content { text }";
		testReferencesFor(value,
				l(r(0, 8, 0, 15)));
	}

	@Test
	public void testReferencesWithDatatypes() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = fooContent" + LS +
				LS +
				"|fooContent = element foo {" + LS +
				"  barContent," + LS +
				"  attribute attr { xsd:integer }" + LS +
				"}" + LS +
				"barContent = element bar { empty }";
		testReferencesFor(value,
				l(r(2, 8, 2, 18)));
	}

	@Test
	public void testReferencesExcludeElementName() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		// "root" after "element" is an XML name, not a reference
		testReferencesFor(value,
				l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesExcludeOtherDefineDeclarations() throws BadLocationException {
		String value = "|root = text" + LS +
				"root |= element bar { empty }";
		// "root |=" is an extend define, not a reference; the only ref is excluded
		testReferencesFor(value);
	}

	@Test
	public void testReferencesOnKeyword() throws BadLocationException {
		String value = "root = |text";
		testReferencesFor(value);
	}
}
