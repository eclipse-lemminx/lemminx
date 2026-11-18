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

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.fi;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.l;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testReferencesFor;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.FileInfo;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RNCReferencesTest {

	@TempDir
	Path tempDir;

	@Test
	public void testReferencesFromDefineDeclaration() throws BadLocationException {
		String value = "|root = element root { text }" + LS + //
				"start = root";
		testReferencesFor(value, l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesFromReference() throws BadLocationException {
		String value = "root = element root { text }" + LS + //
				"start = |root";
		testReferencesFor(value, l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesMultiple() throws BadLocationException {
		String value = "|inline = text" + LS + "root = element root { inline, inline }";
		testReferencesFor(value, l(r(1, 22, 1, 28)), l(r(1, 30, 1, 36)));
	}

	@Test
	public void testReferencesNoMatch() throws BadLocationException {
		String value = "root = |element root { text }";
		testReferencesFor(value);
	}

	@Test
	public void testReferencesDefineAfterCursor() throws BadLocationException {
		String value = "start = |content" + LS + "content = element content { text }";
		testReferencesFor(value, l(r(0, 8, 0, 15)));
	}

	@Test
	public void testReferencesWithDatatypes() throws BadLocationException {
		String value = "datatypes xsd = \"http://www.w3.org/2001/XMLSchema-datatypes\"" + LS + //
				LS + //
				"start = fooContent" + LS + //
				LS + //
				"|fooContent = element foo {" + LS + //
				"  barContent," + LS + //
				"  attribute attr { xsd:integer }" + LS + //
				"}" + LS + //
				"barContent = element bar { empty }";
		testReferencesFor(value, l(r(2, 8, 2, 18)));
	}

	@Test
	public void testReferencesExcludeElementName() throws BadLocationException {
		String value = "|root = element root { text }" + LS + //
				"start = root";
		// "root" after "element" is an XML name, not a reference
		testReferencesFor(value, l(r(1, 8, 1, 12)));
	}

	@Test
	public void testReferencesExcludeOtherDefineDeclarations() throws BadLocationException {
		String value = "|root = text" + LS + //
				"root |= element bar { empty }";
		// "root |=" is an extend declaration, not a pattern usage
		testReferencesFor(value);
	}

	@Test
	public void testReferencesOnKeyword() throws BadLocationException {
		String value = "root = |text";
		testReferencesFor(value);
	}

	@Test
	public void testReferencesExtendPlusUsage() throws BadLocationException {
		String value = "|root = text" + LS + //
				"root |= element bar { empty }" + LS + //
				"start = root";
		// only pattern usages are returned — the extend (|=) is a declaration, not a ref
		testReferencesFor(value, l(r(2, 8, 2, 12)));
	}

	@Test
	public void testReferencesInterleavePlusUsage() throws BadLocationException {
		String value = "|root = text" + LS + //
				"root &= element bar { empty }" + LS + //
				"start = root";
		// &= is also a declaration, excluded from pattern usage refs
		testReferencesFor(value, l(r(2, 8, 2, 12)));
	}

	@Test
	public void testReferencesOnStartKeyword() throws BadLocationException {
		// 'start' is a grammar keyword, not a renamable define
		String value = "|start = root";
		testReferencesFor(value);
	}

	@Test
	public void testReferencesOnStartChoice() throws BadLocationException {
		// 'start |=' is still a grammar keyword
		String value = "|start |= root";
		testReferencesFor(value);
	}

	// --- Workspace cross-file references (code lens click) ---

	/**
	 * Regression: clicking the "N references" workspace code lens on an RNC file
	 * triggered textDocument/references at position (0,0). RNCReferences only
	 * handled RNC-internal define references, so it returned empty when the cursor
	 * was not on a define name. This test verifies that workspace cross-file
	 * references are returned when no define is found at the cursor position.
	 */
	@Test
	public void testWorkspaceReferencesFromCodeLensPosition() throws IOException, BadLocationException {
		// RNC grammar file — position (0,0) lands on "element", not a define name
		FileInfo rncFile = fi(tempDir.resolve("addressbook.rnc"),
				"element addressBook { element card { text } }");
		// <?xml-model href="addressbook.rnc"...
		//              0         1         2         3
		//              0123456789012345678901234567890123
		// 'a' of "addressbook.rnc" starts at col 18, ends at col 33
		FileInfo xmlFile = fi(tempDir.resolve("instance.xml"),
				"<?xml-model href=\"addressbook.rnc\" type=\"application/relax-ng-compact-syntax\"?>\n"
						+ "<addressBook/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xmlFile.path);

		testReferencesFor(rncFile, new Position(0, 0), workspaceIndex,
				new Location(xmlFile.uri, r(0, 18, 0, 33)));
	}

	/**
	 * Regression: a user-defined define at position (0,0) with no intra-file
	 * references caused RNCReferences to return empty without trying the workspace
	 * index, so the workspace "N references" code lens click showed no results.
	 */
	@Test
	public void testWorkspaceReferencesWhenDefineAtPosition0HasNoIntraFileRefs()
			throws IOException, BadLocationException {
		// 'inline' define at (0,0) has no intra-file usages; workspace index has one
		// XML file referencing this RNC grammar.
		FileInfo rncFile = fi(tempDir.resolve("inline.rnc"),
				"inline = element span { text }");
		// <?xml-model href="inline.rnc"...
		//              0         1         2
		//              012345678901234567890123456789
		// 'i' of "inline.rnc" starts at col 18, ends at col 28
		FileInfo xmlFile = fi(tempDir.resolve("instance.xml"),
				"<?xml-model href=\"inline.rnc\" type=\"application/relax-ng-compact-syntax\"?>\n<span/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xmlFile.path);

		testReferencesFor(rncFile, new Position(0, 0), workspaceIndex,
				new Location(xmlFile.uri, r(0, 18, 0, 28)));
	}

	@Test
	public void testWorkspaceReferencesNoIndexReturnsEmpty() throws IOException, BadLocationException {
		FileInfo rncFile = fi(tempDir.resolve("addressbook.rnc"),
				"element addressBook { element card { text } }");

		testReferencesFor(rncFile, new Position(0, 0), null /* no index */);
	}

}
