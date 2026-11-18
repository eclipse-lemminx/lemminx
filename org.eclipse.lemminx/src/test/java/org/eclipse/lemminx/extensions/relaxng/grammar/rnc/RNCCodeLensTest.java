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
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.cl;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCodeLensFor;
import static org.eclipse.lemminx.XMLAssert.r;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RNCCodeLensTest {

	@Test
	public void testCodeLensReferences() {
		String value = "start = root" + LS +
				"root = element root { content }" + LS +
				"content = text";
		testCodeLensFor(value,
				cl(r(1, 0, 1, 4), "1 reference"),
				cl(r(2, 0, 2, 7), "1 reference"));
	}

	@Test
	public void testCodeLensMultipleReferences() {
		String value = "start = root" + LS +
				"a = root" + LS +
				"root = element root { text }";
		testCodeLensFor(value,
				cl(r(2, 0, 2, 4), "2 references"));
	}

	@Test
	public void testCodeLensNoReferences() {
		testCodeLensFor("unused = text");
	}

	@Test
	public void testCodeLensEmptyDocument() {
		testCodeLensFor("");
	}

	@Test
	public void testCodeLensNoDefines() {
		testCodeLensFor("start = element root { text }");
	}

	@Test
	public void testCodeLensDoesNotCountDefinition() {
		String value = "a = text" + LS +
				"start = a";
		testCodeLensFor(value,
				cl(r(0, 0, 0, 1), "1 reference"));
	}

	@Test
	public void testCodeLensWithExtendDefine() {
		// tune |= extends the define — the main define (tune =) counts it as a reference
		String value = "start = tune" + LS +
				"tune = element tune { text }" + LS +
				"tune |= element tune { attribute name { text } }";
		testCodeLensFor(value,
				cl(r(1, 0, 1, 4), "2 references"),
				cl(r(2, 0, 2, 4), "2 references"));
	}

	@Test
	public void testCodeLensStartNotCounted() {
		// 'start' is a grammar keyword — it has no code lens even when referenced
		String value = "start = root" + LS +
				"root = element root { text }";
		testCodeLensFor(value,
				cl(r(1, 0, 1, 4), "1 reference"));
	}

	@Test
	public void testCodeLensStartChoiceNotCounted() {
		// 'start |=' is still a grammar keyword
		String value = "start |= root" + LS +
				"root = element root { text }";
		testCodeLensFor(value,
				cl(r(1, 0, 1, 4), "1 reference"));
	}

	@Test
	public void testCodeLensFullSchema() {
		String value = "default namespace = \"http://example.com\"" + LS
				+ "start = root" + LS
				+ "root = element root {" + LS
				+ "  attribute id { text }," + LS
				+ "  name+" + LS
				+ "}" + LS
				+ "name = element name { text }";
		testCodeLensFor(value,
				cl(r(2, 0, 2, 4), "1 reference"),
				cl(r(6, 0, 6, 4), "1 reference"));
	}

	// --- Workspace "N references" lens ---

	@TempDir
	Path tempDir;

	@Test
	public void testWorkspaceReferenceLens_oneXmlFile() throws IOException {
		// XML file references schema.rnc via xml-model href
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<?xml-model href=\"schema.rnc\"?><order/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xml);

		// Use a fake URI that ends with schema.rnc so the index can match by filename
		String rncUri = xml.getParent().toUri() + "schema.rnc";
		String rncContent = "start = element root { text }";
		testCodeLensFor(rncContent, rncUri, workspaceIndex,
				cl(r(0, 0, 0, 0), "1 reference"));
	}

	@Test
	public void testWorkspaceReferenceLens_twoXmlFiles() throws IOException {
		Path xml1 = tempDir.resolve("a.xml");
		Path xml2 = tempDir.resolve("b.xml");
		Files.writeString(xml1, "<?xml-model href=\"schema.rnc\"?><root/>");
		Files.writeString(xml2, "<?xml-model href=\"schema.rnc\"?><root/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xml1);
		workspaceIndex.indexFile(xml2);

		String rncUri = xml1.getParent().toUri() + "schema.rnc";
		String rncContent = "start = element root { text }";
		testCodeLensFor(rncContent, rncUri, workspaceIndex,
				cl(r(0, 0, 0, 0), "2 references"));
	}

	@Test
	public void testWorkspaceReferenceLens_noReferences() throws IOException {
		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		// No files indexed — workspace lens must be absent

		String rncUri = tempDir.toUri() + "orphan.rnc";
		String rncContent = "start = element root { text }";
		// No workspace lens; no define lenses either
		testCodeLensFor(rncContent, rncUri, workspaceIndex);
	}

	@Test
	public void testWorkspaceReferenceLens_combinedWithDefineLenses() throws IOException {
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<?xml-model href=\"schema.rnc\"?><order/>");

		WorkspaceIndex workspaceIndex = new WorkspaceIndex();
		workspaceIndex.indexFile(xml);

		String rncUri = xml.getParent().toUri() + "schema.rnc";
		String rncContent = "start = root" + LS + "root = element root { text }";
		// Workspace lens at (0,0) + define lens for 'root'
		testCodeLensFor(rncContent, rncUri, workspaceIndex,
				cl(r(0, 0, 0, 0), "1 reference"),
				cl(r(1, 0, 1, 4), "1 reference"));
	}
}
