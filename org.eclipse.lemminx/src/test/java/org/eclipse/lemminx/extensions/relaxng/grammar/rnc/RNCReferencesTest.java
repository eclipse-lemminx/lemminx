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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RNCReferencesTest {

	@TempDir
	Path tempDir;

	private static final CancelChecker NO_CANCEL = () -> {};

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
		String value = "|root = text" + LS +
				"root |= element bar { empty }" + LS +
				"start = root";
		// only pattern usages are returned — the extend (|=) is a declaration, not a ref
		testReferencesFor(value,
				l(r(2, 8, 2, 12)));
	}

	@Test
	public void testReferencesInterleavePlusUsage() throws BadLocationException {
		String value = "|root = text" + LS +
				"root &= element bar { empty }" + LS +
				"start = root";
		// &= is also a declaration, excluded from pattern usage refs
		testReferencesFor(value,
				l(r(2, 8, 2, 12)));
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
	 * triggered textDocument/references at position (0,0). RNCReferences only handled
	 * RNC-internal define references, so it returned empty when the cursor was not on a
	 * define name. This test verifies that workspace cross-file references are returned
	 * when no define is found at the cursor position.
	 */
	@Test
	public void testWorkspaceReferencesFromCodeLensPosition() throws IOException, BadLocationException {
		// RNC grammar file content — position (0,0) lands on "element", not a define name
		String rncContent = "element addressBook { element card { text } }";

		// Write the RNC file to disk so WorkspaceIndex can resolve it
		Path rncFile = tempDir.resolve("addressbook.rnc");
		Files.writeString(rncFile, rncContent);

		// XML file that references the RNC grammar via <?xml-model?>
		Path xmlFile = tempDir.resolve("instance.xml");
		Files.writeString(xmlFile,
				"<?xml-model href=\"addressbook.rnc\" type=\"application/relax-ng-compact-syntax\"?>\n"
				+ "<addressBook/>");

		// Index the XML file
		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xmlFile);

		// Parse the RNC document with the real on-disk URI
		String rncUri = rncFile.toUri().toString();
		TextDocument textDocument = new TextDocument(rncContent, rncUri);
		RNCDocument rncDocument = RNCParser.getInstance().parse(rncContent, rncUri);
		rncDocument.setTextDocument(textDocument);

		// Simulate the code lens click: position (0,0) is "element" — not a define name
		RNCLanguageService service = new RNCLanguageService(uri -> null);
		ReferenceContext context = new ReferenceContext(false);
		List<? extends Location> locations = service.findReferences(
				rncDocument, new Position(0, 0), context, workspaceIndex, NO_CANCEL);

		assertEquals(1, locations.size(), "Expected exactly 1 cross-file reference from instance.xml");
		assertTrue(locations.get(0).getUri().contains("instance.xml"),
				"Expected reference URI to contain 'instance.xml', got: " + locations.get(0).getUri());
	}

	/**
	 * Regression: a user-defined define at position (0,0) with no intra-file references
	 * caused RNCReferences to return empty without trying the workspace index, so the
	 * workspace "N references" code lens click showed no results.
	 */
	@Test
	public void testWorkspaceReferencesWhenDefineAtPosition0HasNoIntraFileRefs()
			throws IOException, BadLocationException {
		// 'inline' is a user-defined define starting at (0,0), but nothing in the file
		// references it internally (it's the only pattern). The workspace index has one
		// XML file that references this RNC grammar.
		String rncContent = "inline = element span { text }";

		Path rncFile = tempDir.resolve("inline.rnc");
		Files.writeString(rncFile, rncContent);

		Path xmlFile = tempDir.resolve("instance.xml");
		Files.writeString(xmlFile,
				"<?xml-model href=\"inline.rnc\" type=\"application/relax-ng-compact-syntax\"?>\n"
				+ "<span/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xmlFile);

		String rncUri = rncFile.toUri().toString();
		TextDocument textDocument = new TextDocument(rncContent, rncUri);
		RNCDocument rncDocument = RNCParser.getInstance().parse(rncContent, rncUri);
		rncDocument.setTextDocument(textDocument);

		// Workspace code lens click at (0,0) — on the 'inline' define, which has no
		// intra-file refs. Must fall through to workspace index and return cross-file ref.
		RNCLanguageService service = new RNCLanguageService(uri -> null);
		ReferenceContext context = new ReferenceContext(false);
		List<? extends Location> locations = service.findReferences(
				rncDocument, new Position(0, 0), context, workspaceIndex, NO_CANCEL);

		assertEquals(1, locations.size(),
				"Expected workspace backlink when user-defined define at (0,0) has no intra-file refs");
		assertTrue(locations.get(0).getUri().contains("instance.xml"),
				"Expected reference URI to contain 'instance.xml', got: " + locations.get(0).getUri());
	}

	@Test
	public void testWorkspaceReferencesNoIndexReturnsEmpty() throws IOException, BadLocationException {
		String rncContent = "element addressBook { element card { text } }";

		Path rncFile = tempDir.resolve("addressbook.rnc");
		Files.writeString(rncFile, rncContent);

		String rncUri = rncFile.toUri().toString();
		TextDocument textDocument = new TextDocument(rncContent, rncUri);
		RNCDocument rncDocument = RNCParser.getInstance().parse(rncContent, rncUri);
		rncDocument.setTextDocument(textDocument);

		// No workspace index → should return empty, not throw
		RNCLanguageService service = new RNCLanguageService(uri -> null);
		ReferenceContext context = new ReferenceContext(false);
		List<? extends Location> locations = service.findReferences(
				rncDocument, new Position(0, 0), context, null /* no index */, NO_CANCEL);

		assertTrue(locations.isEmpty(), "Expected empty when workspaceIndex is null");
	}

}
