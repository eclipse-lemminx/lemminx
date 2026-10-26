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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.junit.jupiter.api.Test;

/**
 * Test for listing bindable catalog entries via the xml.catalog.entries.bindable
 * command.
 *
 * <p>
 * These tests exercise the entry filtering logic directly by parsing catalog XML
 * and calling the package-private collectEntries method, avoiding platform-dependent
 * file URI resolution.
 * </p>
 */
public class ListBindableCatalogEntriesCommandTest {

	// Catalog with grammar entries (.xsd, .dtd) and non-grammar entries (.xml, .html)
	// Only grammar entries should be returned; non-grammar entries are skipped
	// <catalog>
	//   <uri name="..." uri="tag.xsd" />        -> included (xsd)
	//   <uri name="..." uri="note.xml" />        -> skipped (not a grammar)
	//   <system systemId="..." uri="note.dtd" /> -> included (dtd)
	//   <uri name="..." uri="page.html" />       -> skipped (not a grammar)
	// </catalog>
	@Test
	public void nonGrammarEntriesAreFiltered() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"  <uri name=\"http://example.org/schema/tag\" uri=\"tag.xsd\" />\n" +
				"  <uri name=\"http://example.org/other\" uri=\"note.xml\" />\n" +
				"  <system systemId=\"http://example.org/note.dtd\" uri=\"note.dtd\" />\n" +
				"  <uri name=\"http://example.org/page\" uri=\"page.html\" />\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(2, result.size());
		assertEquals("xsd", result.get(0).getGrammarType());
		assertEquals("http://example.org/schema/tag", result.get(0).getIdentifier());
		assertEquals("dtd", result.get(1).getGrammarType());
		assertEquals("http://example.org/note.dtd", result.get(1).getIdentifier());
	}

	// All grammar types: .xsd, .dtd, .ent, .mod, .rng, .rnc
	// <catalog>
	//   <uri name="a" uri="a.xsd" />  -> xsd
	//   <uri name="b" uri="b.dtd" />  -> dtd
	//   <uri name="c" uri="c.ent" />  -> dtd
	//   <uri name="d" uri="d.mod" />  -> dtd
	//   <uri name="e" uri="e.rng" />  -> rng
	//   <uri name="f" uri="f.rnc" />  -> rnc
	// </catalog>
	@Test
	public void allGrammarTypesRecognized() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"  <uri name=\"a\" uri=\"a.xsd\" />\n" +
				"  <uri name=\"b\" uri=\"b.dtd\" />\n" +
				"  <uri name=\"c\" uri=\"c.ent\" />\n" +
				"  <uri name=\"d\" uri=\"d.mod\" />\n" +
				"  <uri name=\"e\" uri=\"e.rng\" />\n" +
				"  <uri name=\"f\" uri=\"f.rnc\" />\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(6, result.size());
		assertEquals("xsd", result.get(0).getGrammarType());
		assertEquals("dtd", result.get(1).getGrammarType());
		assertEquals("dtd", result.get(2).getGrammarType());
		assertEquals("dtd", result.get(3).getGrammarType());
		assertEquals("rng", result.get(4).getGrammarType());
		assertEquals("rnc", result.get(5).getGrammarType());
	}

	// HTTP URI should not be corrupted
	// <uri name="..." uri="http://host/book.xsd" /> -> targetURI = "http://host/book.xsd"
	@Test
	public void httpUriPreserved() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"  <uri name=\"http://example.org/schema\" uri=\"http://host/book.xsd\" />\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(1, result.size());
		assertEquals("http://host/book.xsd", result.get(0).getTargetURI());
	}

	// All three entry types: public, system, uri
	// <public publicId="..." uri="a.dtd" />  -> identifier = publicId
	// <system systemId="..." uri="b.xsd" />  -> identifier = systemId
	// <uri name="..." uri="c.rng" />         -> identifier = name
	@Test
	public void allEntryTypes() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"  <public publicId=\"-//Example//DTD//EN\" uri=\"a.dtd\" />\n" +
				"  <system systemId=\"http://example.org/b.xsd\" uri=\"b.xsd\" />\n" +
				"  <uri name=\"http://example.org/c\" uri=\"c.rng\" />\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(3, result.size());
		assertEquals("public", result.get(0).getEntryType());
		assertEquals("-//Example//DTD//EN", result.get(0).getIdentifier());
		assertEquals("system", result.get(1).getEntryType());
		assertEquals("http://example.org/b.xsd", result.get(1).getIdentifier());
		assertEquals("uri", result.get(2).getEntryType());
		assertEquals("http://example.org/c", result.get(2).getIdentifier());
	}

	// Empty catalog: returns empty result
	// <catalog></catalog>
	@Test
	public void emptyCatalogReturnsEmpty() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(0, result.size());
	}

	// Entry with empty identifier: skipped
	// <uri name="" uri="a.xsd" /> -> skipped (empty identifier)
	@Test
	public void emptyIdentifierSkipped() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\n" +
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" +
				"  <uri name=\"\" uri=\"a.xsd\" />\n" +
				"</catalog>";
		String catalogURI = "file:///test/catalog.xml";

		List<BindableCatalogEntry> result = collectEntriesFromCatalog(catalog, catalogURI);

		assertEquals(0, result.size());
	}

	/**
	 * Parse a catalog XML string and collect bindable entries using the same
	 * logic as ListBindableCatalogEntriesCommand.collectEntries.
	 */
	private static List<BindableCatalogEntry> collectEntriesFromCatalog(String catalogXml, String catalogURI) {
		DOMDocument document = DOMParser.getInstance().parse(catalogXml, catalogURI, null);
		ListBindableCatalogEntriesCommand command = new ListBindableCatalogEntriesCommand(null);
		List<BindableCatalogEntry> result = new ArrayList<>();
		command.collectEntries(catalogURI, document, result, new HashSet<>());
		return result;
	}
}
