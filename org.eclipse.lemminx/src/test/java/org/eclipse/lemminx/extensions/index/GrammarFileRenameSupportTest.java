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
package org.eclipse.lemminx.extensions.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link GrammarFileRenameSupport} — end-to-end rename scenarios.
 */
public class GrammarFileRenameSupportTest {

	@TempDir
	Path tempDir;

	private WorkspaceIndex index;

	@BeforeEach
	public void setUp() {
		index = new WorkspaceIndex();
	}

	// --- UC14: Rename grammar file referenced via xml-model ---

	@Test
	public void testRenameXmlModelHref() throws IOException {
		Path xml = writeFile("order.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>");
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);
		index.indexFile(schema);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"));

		assertNotNull(edit);
		Map<String, List<TextEdit>> changes = edit.getChanges();
		List<TextEdit> xmlEdits = changes.get(xml.toUri().toString());
		assertNotNull(xmlEdits);
		assertEquals(1, xmlEdits.size());
		assertEquals("types.xsd", xmlEdits.get(0).getNewText());
	}

	// --- UC15: Rename grammar referenced via noNamespaceSchemaLocation ---

	@Test
	public void testRenameNoNamespaceSchemaLocation() throws IOException {
		Path xml = writeFile("data.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("types.xsd", edits.get(0).getNewText());
	}

	// --- UC16: Rename grammar referenced via schemaLocation ---

	@Test
	public void testRenameSchemaLocation() throws IOException {
		Path xml = writeFile("invoice.xml",
				"<inv xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"     xsi:schemaLocation=\"http://example.com schema.xsd\"/>");
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("types.xsd", edits.get(0).getNewText());
	}

	// --- UC17: Rename DTD referenced via DOCTYPE ---

	@Test
	public void testRenameDoctypeSystem() throws IOException {
		Path xml = writeFile("config.xml",
				"<!DOCTYPE config SYSTEM \"config.dtd\">\n<config/>");
		Path dtd = writeFile("config.dtd", "<!ELEMENT config EMPTY>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, dtd, tempDir.resolve("settings.dtd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("settings.dtd", edits.get(0).getNewText());
	}

	// --- Multiple files referencing the same grammar ---

	@Test
	public void testRenameAcrossMultipleFiles() throws IOException {
		Path xml1 = writeFile("a.xml",
				"<?xml-model href=\"shared.xsd\"?>\n<a/>");
		Path xml2 = writeFile("b.xml",
				"<b xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"   xsi:noNamespaceSchemaLocation=\"shared.xsd\"/>");
		Path xml3 = writeFile("c.xml",
				"<?xml-model href=\"other.xsd\"?>\n<c/>");
		Path schema = writeFile("shared.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml1);
		index.indexFile(xml2);
		index.indexFile(xml3);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("common.xsd"));

		assertNotNull(edit);
		Map<String, List<TextEdit>> changes = edit.getChanges();
		assertTrue(changes.containsKey(xml1.toUri().toString()));
		assertTrue(changes.containsKey(xml2.toUri().toString()));
		// xml3 references other.xsd, not shared.xsd
		assertTrue(!changes.containsKey(xml3.toUri().toString()));
	}

	// --- Rename with relative path preserved ---

	@Test
	public void testRenamePreservesRelativePath() throws IOException {
		Path xml = writeFile("app.xml",
				"<?xml-model href=\"schemas/order-schema.xsd\"?>\n<app/>");
		Path schema = tempDir.resolve("schemas").resolve("order-schema.xsd");
		Files.createDirectories(schema.getParent());
		Files.writeString(schema,
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("schemas").resolve("purchase-order.xsd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("schemas/purchase-order.xsd", edits.get(0).getNewText());
	}

	// --- Same file referenced multiple times ---

	@Test
	public void testRenameMultipleRefsInSameFile() throws IOException {
		Path xml = writeFile("multi.xml",
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertEquals(2, edits.size());
		for (TextEdit e : edits) {
			assertEquals("types.xsd", e.getNewText());
		}
	}

	// --- XSD include schemaLocation rename ---

	@Test
	public void testRenameXsdInclude() throws IOException {
		Path mainXsd = writeFile("main.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"common.xsd\"/>\n" +
				"</xs:schema>");
		Path commonXsd = writeFile("common.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(mainXsd);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, commonXsd, tempDir.resolve("shared.xsd"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(mainXsd.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("shared.xsd", edits.get(0).getNewText());
	}

	// --- No references found ---

	@Test
	public void testRenameNoReferences() throws IOException {
		Path xml = writeFile("data.xml", "<root/>");
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"));

		assertNull(edit);
	}

	// --- XSLT import rename ---

	@Test
	public void testRenameXsltImport() throws IOException {
		Path xsl = writeFile("main.xsl",
				"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
				"  <xsl:import href=\"utils.xsl\"/>\n" +
				"</xsl:stylesheet>");
		Path utils = writeFile("utils.xsl",
				"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"/>");

		index.indexFile(xsl);

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, utils, tempDir.resolve("helpers.xsl"));

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xsl.toUri().toString());
		assertEquals(1, edits.size());
		assertEquals("helpers.xsl", edits.get(0).getNewText());
	}

	// --- Cross-directory moves ---

	@Test
	public void testMoveGrammarToAnotherDirectory() throws IOException {
		// xml references "../schemas/schema.xsd" from "app/order.xml"
		Path appDir = tempDir.resolve("app");
		Files.createDirectories(appDir);
		Path xml = appDir.resolve("order.xml");
		Files.writeString(xml,
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"../schemas/schema.xsd\"?>\n" +
				"<order/>");

		Path schemasDir = tempDir.resolve("schemas");
		Files.createDirectories(schemasDir);
		Path schema = schemasDir.resolve("schema.xsd");
		Files.writeString(schema, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);
		index.indexFile(schema);

		// Move schema from schemas/ to shared/
		Path sharedDir = tempDir.resolve("shared");
		Path newSchema = sharedDir.resolve("schema.xsd");

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(index, schema, newSchema);

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertNotNull(edits);
		assertEquals(1, edits.size());
		assertEquals("../shared/schema.xsd", edits.get(0).getNewText());
	}

	@Test
	public void testMoveGrammarToAnotherDirectoryWithRename() throws IOException {
		// xml references "../schemas/schema.xsd" from "app/order.xml"
		Path appDir = tempDir.resolve("app");
		Files.createDirectories(appDir);
		Path xml = appDir.resolve("order.xml");
		Files.writeString(xml,
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"../schemas/schema.xsd\"?>\n" +
				"<order/>");

		Path schemasDir = tempDir.resolve("schemas");
		Files.createDirectories(schemasDir);
		Path schema = schemasDir.resolve("schema.xsd");
		Files.writeString(schema, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);
		index.indexFile(schema);

		// Move and rename: schemas/schema.xsd → shared/types.xsd
		Path sharedDir = tempDir.resolve("shared");
		Path newSchema = sharedDir.resolve("types.xsd");

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(index, schema, newSchema);

		assertNotNull(edit);
		List<TextEdit> edits = edit.getChanges().get(xml.toUri().toString());
		assertNotNull(edits);
		assertEquals(1, edits.size());
		assertEquals("../shared/types.xsd", edits.get(0).getNewText());
	}

	// Fix: two grammars with the same name in different directories — only update refs to the correct one
	@Test
	public void testRenameSameNameGrammarInDifferentDirs() throws IOException {
		Path schemasDir = tempDir.resolve("schemas");
		Path otherDir = tempDir.resolve("other");
		Files.createDirectories(schemasDir);
		Files.createDirectories(otherDir);

		Path schema1 = schemasDir.resolve("order.xsd");
		Path schema2 = otherDir.resolve("order.xsd");
		Files.writeString(schema1, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");
		Files.writeString(schema2, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		// XML file references schemas/order.xsd, not other/order.xsd
		Path xml = writeFile("data.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schemas/order.xsd\"/>");

		index.indexFile(xml);
		index.indexFile(schema1);
		index.indexFile(schema2);

		// Rename other/order.xsd → other/renamed.xsd
		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema2, otherDir.resolve("renamed.xsd"));

		// The XML references schemas/order.xsd, not other/order.xsd → no edit for data.xml
		if (edit != null && edit.getChanges() != null) {
			assertFalse(edit.getChanges().containsKey(xml.toUri().toString()),
					"XML referencing schemas/order.xsd must not be updated when renaming other/order.xsd");
		}
	}

	// --- Rename with open (unsaved) document ---

	@Test
	public void testRenameUsesLiveContentWhenDocumentIsOpen() throws IOException {
		// On disk: xml-model PI is on line 1 (after the xml declaration)
		String savedContent = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		Path xml = writeFile("order.xml", savedContent);
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);
		index.indexFile(schema);

		// Simulate user adding two lines before the xml-model PI (without saving):
		// The ref now lives at line 3 instead of line 1.
		String liveContent = "<?xml version=\"1.0\"?>\n" +
				"<!-- added line 1 -->\n" +
				"<!-- added line 2 -->\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		String xmlUri = xml.toUri().toString();

		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"),
				uri -> uri.equals(xmlUri) ? liveContent : null);

		assertNotNull(edit);
		Map<String, List<TextEdit>> changes = edit.getChanges();
		List<TextEdit> xmlEdits = changes.get(xmlUri);
		assertNotNull(xmlEdits);
		assertEquals(1, xmlEdits.size());
		TextEdit textEdit = xmlEdits.get(0);
		assertEquals("types.xsd", textEdit.getNewText());
		// The edit must target line 3 (from live content), NOT line 1 (from cached index)
		assertEquals(3, textEdit.getRange().getStart().getLine(),
				"Rename must use live content positions, not cached positions");
	}

	@Test
	public void testRenameUsesCachedPositionsWhenDocumentIsClosed() throws IOException {
		// xml-model PI on line 1
		String content = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		Path xml = writeFile("order.xml", content);
		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>");

		index.indexFile(xml);
		index.indexFile(schema);

		// openDocumentContent returns null → file is closed → use cached positions
		WorkspaceEdit edit = GrammarFileRenameSupport.computeRenameEdits(
				index, schema, tempDir.resolve("types.xsd"),
				uri -> null);

		assertNotNull(edit);
		List<TextEdit> xmlEdits = edit.getChanges().get(xml.toUri().toString());
		assertNotNull(xmlEdits);
		assertEquals(1, xmlEdits.size());
		assertEquals("types.xsd", xmlEdits.get(0).getNewText());
		assertEquals(1, xmlEdits.get(0).getRange().getStart().getLine(),
				"Closed file must use cached line from index");
	}

	// --- Helpers ---

	private Path writeFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}
}
