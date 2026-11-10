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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.pr;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testPrepareRenameFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testRenameFor;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

public class RNCRenameTest {

	// --- Prepare rename ---

	@Test
	public void testPrepareRenameOnDefineName() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		testPrepareRenameFor(value, pr(r(0, 0, 0, 4), "root"));
	}

	@Test
	public void testPrepareRenameOnReference() throws BadLocationException {
		String value = "root = element root { text }" + LS +
				"start = |root";
		testPrepareRenameFor(value, pr(r(1, 8, 1, 12), "root"));
	}

	@Test
	public void testPrepareRenameOnKeyword() throws BadLocationException {
		testPrepareRenameFor("|start = root", null);
	}

	@Test
	public void testPrepareRenameOnWhitespace() throws BadLocationException {
		testPrepareRenameFor("start = |  root", null);
	}

	@Test
	public void testPrepareRenameOnElementName() throws BadLocationException {
		testPrepareRenameFor("start = element |root { text }", null);
	}

	@Test
	public void testPrepareRenameOnUndefinedName() throws BadLocationException {
		testPrepareRenameFor("start = |unknown", null);
	}

	@Test
	public void testPrepareRenameInMiddleOfWord() throws BadLocationException {
		String value = "fooContent = text" + LS +
				"start = fooCo|ntent";
		testPrepareRenameFor(value, pr(r(1, 8, 1, 18), "fooContent"));
	}

	// --- Rename ---

	@Test
	public void testRenameDefine() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		testRenameFor(value, "newName",
				te(0, 0, 0, 4, "newName"),
				te(1, 8, 1, 12, "newName"));
	}

	@Test
	public void testRenameFromReference() throws BadLocationException {
		String value = "root = element root { text }" + LS +
				"start = |root";
		testRenameFor(value, "newName",
				te(0, 0, 0, 4, "newName"),
				te(1, 8, 1, 12, "newName"));
	}

	@Test
	public void testRenameMultipleReferences() throws BadLocationException {
		String value = "|inline = text" + LS +
				"root = element root { inline, inline }";
		testRenameFor(value, "content",
				te(0, 0, 0, 6, "content"),
				te(1, 22, 1, 28, "content"),
				te(1, 30, 1, 36, "content"));
	}

	@Test
	public void testRenameDefineAfterCursor() throws BadLocationException {
		String value = "start = |content" + LS +
				"content = element content { text }";
		testRenameFor(value, "body",
				te(1, 0, 1, 7, "body"),
				te(0, 8, 0, 15, "body"));
	}

	@Test
	public void testRenameWithDatatypes() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS +
				LS +
				"start = fooContent" + LS +
				LS +
				"|fooContent = element foo {" + LS +
				"  barContent," + LS +
				"  attribute attr { xsd:integer }" + LS +
				"}" + LS +
				"barContent = element bar { empty }";
		testRenameFor(value, "fooBody",
				te(4, 0, 4, 10, "fooBody"),
				te(2, 8, 2, 18, "fooBody"));
	}

	@Test
	public void testRenameExcludesElementName() throws BadLocationException {
		String value = "|root = element root { text }" + LS +
				"start = root";
		testRenameFor(value, "newRoot",
				te(0, 0, 0, 4, "newRoot"),
				te(1, 8, 1, 12, "newRoot"));
	}
}
