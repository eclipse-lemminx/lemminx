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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WorkspaceIndexCatalogReferenceParticipant}.
 *
 * <p>Verifies that clicking a catalog code lens returns the XML instance files that
 * use the catalog identifier (namespace URI, system ID, public ID), NOT grammar files
 * such as XSD files that may reference the grammar via xs:include/xs:import.
 */
public class WorkspaceIndexCatalogReferenceParticipantTest {

	@TempDir
	Path tempDir;

	private WorkspaceIndex index;
	private WorkspaceIndexCatalogReferenceParticipant participant;
	private static final CancelChecker NO_CANCEL = () -> {};
	private static final ReferenceContext INCLUDE_DECL = new ReferenceContext(true);

	@BeforeEach
	public void setUp() {
		index = new WorkspaceIndex();
		participant = new WorkspaceIndexCatalogReferenceParticipant(index);
	}

	// --- <uri name="http://foo"> entries ---

	@Test
	public void testFindReferenceOnUriEntry_viaXmlns() throws IOException {
		// XML binds via xmlns="http://foo" — should be returned.
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml, "<data xmlns=\"http://foo\"/>");
		index.indexFile(xml);

		// Also index an XSD that uses xs:import — it should NOT appear.
		Path xsd = tempDir.resolve("schema.xsd");
		Files.writeString(xsd, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
				+ "<xs:import namespace=\"http://foo\" schemaLocation=\"data2.xsd\"/></xs:schema>");
		index.indexFile(xsd);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("data.xml"),
				"Expected data.xml, got: " + locations.get(0).getUri());
	}

	@Test
	public void testFindReferenceOnUriEntry_viaPrefixedXmlns() throws IOException {
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml, "<ns:data xmlns:ns=\"http://foo\"/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("data.xml"));
	}

	@Test
	public void testFindReferenceOnUriEntry_viaSchemaLocation_namespace() throws IOException {
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml,
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:schemaLocation=\"http://foo schema.xsd\"/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("data.xml"));
	}

	@Test
	public void testFindReferenceOnUriEntry_viaXmlModel() throws IOException {
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml, "<?xml-model href=\"http://foo\"?><data/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("data.xml"));
	}

	@Test
	public void testFindReferenceOnUriEntry_viaNoNsSchemaLocation() throws IOException {
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml,
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:noNamespaceSchemaLocation=\"http://foo\"/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("data.xml"));
	}

	// --- <system systemId="..."> entries ---

	@Test
	public void testFindReferenceOnSystemEntry() throws IOException {
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>", "system");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("order.xml"));
	}

	@Test
	public void testFindReferenceOnSystemEntry_multipleFiles() throws IOException {
		Path xml1 = tempDir.resolve("order.xml");
		Path xml2 = tempDir.resolve("invoice.xml");
		Files.writeString(xml1, "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		Files.writeString(xml2, "<!DOCTYPE invoice SYSTEM \"http://example.org/schema\"><invoice/>");
		index.indexFile(xml1);
		index.indexFile(xml2);

		List<Location> locations = findReferencesOnEntry(
				"<system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>", "system");

		assertEquals(2, locations.size());
	}

	// --- <public publicId="..."> entries ---

	@Test
	public void testFindReferenceOnPublicEntry() throws IOException {
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml,
				"<!DOCTYPE order PUBLIC \"-//Example//DTD//EN\" \"schema.dtd\"><order/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<public publicId=\"-//Example//DTD//EN\" uri=\"schema.dtd\"/>", "public");

		assertEquals(1, locations.size());
		assertTrue(locations.get(0).getUri().endsWith("order.xml"));
	}

	// --- no matches ---

	@Test
	public void testNoReferenceWhenDirectFilenameOnly() throws IOException {
		// File uses noNamespaceSchemaLocation="data2.xsd" — does NOT use the catalog identifier.
		Path xml = tempDir.resolve("data.xml");
		Files.writeString(xml,
				"<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:noNamespaceSchemaLocation=\"data2.xsd\"/>");
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<uri name=\"http://foo\" uri=\"data2.xsd\"/>", "uri");

		assertTrue(locations.isEmpty(),
				"Direct filename reference must not appear — it doesn't use the catalog identifier");
	}

	@Test
	public void testNoReferenceOnNonCatalogDocument() {
		DOMDocument doc = DOMParser.getInstance().parse("<root/>", "file:///root.xml", null);
		List<Location> locations = new ArrayList<>();
		participant.findReference(doc, new Position(0, 0), INCLUDE_DECL, locations, NO_CANCEL);
		assertTrue(locations.isEmpty());
	}

	@Test
	public void testNoReferenceWhenCursorOutsideAnyEntry() {
		String src = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n"
				+ "</catalog>";
		DOMDocument catalog = parseCatalog(src);
		// Position (0,0) is at '<' of <catalog> — not inside any entry element.
		List<Location> locations = new ArrayList<>();
		participant.findReference(catalog, new Position(0, 0), INCLUDE_DECL, locations, NO_CANCEL);
		assertTrue(locations.isEmpty());
	}

	// --- open modified document uses live positions ---

	@Test
	public void testFindReference_usesLiveContentWhenDocumentIsOpen() throws IOException {
		// On disk: DOCTYPE system ID is on line 0
		String savedContent = "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>";
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, savedContent);
		index.indexFile(xml);

		// Simulate user adding a comment line before the DOCTYPE (without saving):
		// The ref is now at line 1 instead of line 0.
		String liveContent = "<!-- comment -->\n" +
				"<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>";
		String xmlUri = xml.toUri().toString();

		// Use a participant wired with an open-doc provider that returns live content
		WorkspaceIndexCatalogReferenceParticipant liveParticipant =
				new WorkspaceIndexCatalogReferenceParticipant(index,
						() -> new MockDocumentProvider(xmlUri, liveContent));

		List<Location> locations = findReferencesOnEntryWithParticipant(
				liveParticipant,
				"<system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>", "system");

		assertEquals(1, locations.size());
		// Must use live position (line 1), not cached position (line 0)
		assertEquals(1, locations.get(0).getRange().getStart().getLine(),
				"References must use live content positions, not cached positions");
	}

	@Test
	public void testFindReference_usesCachedPositionsWhenDocumentIsClosed() throws IOException {
		// DOCTYPE on line 0
		String content = "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>";
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, content);
		index.indexFile(xml);

		List<Location> locations = findReferencesOnEntry(
				"<system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>", "system");

		assertEquals(1, locations.size());
		assertEquals(0, locations.get(0).getRange().getStart().getLine(),
				"Closed file must use cached line from index");
	}

	// --- helpers ---

	/**
	 * Builds a catalog with a single entry, positions the cursor inside that entry
	 * element, and runs findReference.
	 *
	 * @param entryXml   the entry element XML snippet (e.g. {@code <uri name="http://foo" uri="data2.xsd"/>})
	 * @param entryTag   the local tag name (e.g. {@code "uri"}, {@code "system"}, {@code "public"})
	 */
	private List<Location> findReferencesOnEntry(String entryXml, String entryTag) {
		return findReferencesOnEntryWithParticipant(participant, entryXml, entryTag);
	}

	private List<Location> findReferencesOnEntryWithParticipant(
			WorkspaceIndexCatalogReferenceParticipant p, String entryXml, String entryTag) {
		String src = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  " + entryXml + "\n"
				+ "</catalog>";
		DOMDocument catalog = parseCatalog(src);

		// Find the cursor position: 1 char after the '<' of the entry tag.
		int tagOffset = src.indexOf("<" + entryTag + " ");
		int offset = tagOffset + 1; // inside the tag name, definitely within the element range

		// Convert offset to line/column
		int line = 0;
		int col = 0;
		for (int i = 0; i < offset; i++) {
			if (src.charAt(i) == '\n') {
				line++;
				col = 0;
			} else {
				col++;
			}
		}

		List<Location> locations = new ArrayList<>();
		p.findReference(catalog, new Position(line, col), INCLUDE_DECL, locations, NO_CANCEL);
		return locations;
	}

	private DOMDocument parseCatalog(String xml) {
		String uri = tempDir.resolve("catalog.xml").toUri().toString();
		return DOMParser.getInstance().parse(xml, uri, null);
	}

	/** Minimal document provider that serves a single pre-parsed document by URI. */
	private static class MockDocumentProvider implements IXMLDocumentProvider {
		private final String uri;
		private final String content;

		MockDocumentProvider(String uri, String content) {
			this.uri = uri;
			this.content = content;
		}

		@Override
		public DOMDocument getDocument(String queryUri) {
			if (uri.equals(queryUri)) {
				return DOMParser.getInstance().parse(content, uri, null);
			}
			return null;
		}
	}
}
