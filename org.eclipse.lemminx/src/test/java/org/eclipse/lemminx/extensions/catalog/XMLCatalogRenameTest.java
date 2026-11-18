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
package org.eclipse.lemminx.extensions.catalog;

import static org.eclipse.lemminx.XMLAssert.assertPrepareRename;
import static org.eclipse.lemminx.XMLAssert.pr;

import java.util.List;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

/**
 * Tests for rename support in XML catalog files ({@link XMLCatalogRenameParticipant}).
 *
 * <p>These tests verify that {@code prepareRename} correctly identifies the
 * renameable identifier in each supported entry type. The underlying
 * {@link CatalogUtils#getCatalogIdentifierAttr} fix for {@code <namespace>}
 * is exercised via end-to-end {@code prepareRename} calls.
 */
public class XMLCatalogRenameTest extends AbstractCacheBasedTest {

	private static final String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

	// --- <namespace name="..."> entries ---

	@Test
	public void testPrepareRenameOnNamespaceEntry() throws BadLocationException {
		// Before fix: getCatalogIdentifierAttr returned null for <namespace>, so
		// prepareRename returned null. After fix: it returns the name attribute range.
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace name=\"http://example.org/ns|\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(new XMLLanguageService(), catalog,
				identifierPrepareRename(catalog, "http://example.org/ns"));
	}

	@Test
	public void testPrepareRenameOnNamespaceEntry_cursorAtStart() throws BadLocationException {
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace name=\"|http://example.org/ns\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(new XMLLanguageService(), catalog,
				identifierPrepareRename(catalog, "http://example.org/ns"));
	}

	// --- <uri name="..."> entries (pre-existing support, regression guard) ---

	@Test
	public void testPrepareRenameOnUriEntry() throws BadLocationException {
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://example.org/ns|\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(new XMLLanguageService(), catalog,
				identifierPrepareRename(catalog, "http://example.org/ns"));
	}

	// --- <system systemId="..."> entries ---

	@Test
	public void testPrepareRenameOnSystemEntry() throws BadLocationException {
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/dtd|\" uri=\"s.dtd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(new XMLLanguageService(), catalog,
				identifierPrepareRename(catalog, "http://example.org/dtd"));
	}

	// --- <public publicId="..."> entries ---

	@Test
	public void testPrepareRenameOnPublicEntry() throws BadLocationException {
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <public publicId=\"-//Ex//DTD//EN|\" uri=\"s.dtd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(new XMLLanguageService(), catalog,
				identifierPrepareRename(catalog, "-//Ex//DTD//EN"));
	}

	// --- no rename outside identifier value ---

	@Test
	public void testNoPrepareRenameOnAttributeName() throws BadLocationException {
		// Cursor is on "name" attribute name, not its value — not renameable
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace na|me=\"http://example.org/ns\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(catalog, null);
	}

	@Test
	public void testNoPrepareRenameOnNonCatalogDocument() throws BadLocationException {
		String xml = "<root>|</root>";
		assertPrepareRename(xml, null);
	}

	@Test
	public void testNoPrepareRenameOnUriAttributeOfEntry() throws BadLocationException {
		// Cursor is on the uri= attribute (grammar file path), not the identifier
		String catalog = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace name=\"http://example.org/ns\" uri=\"s.x|sd\"/>\n"
				+ "</catalog>";
		assertPrepareRename(catalog, null);
	}

	// --- helpers ---

	/**
	 * Builds the expected {@link PrepareRenameResult} by parsing the catalog XML
	 * and using {@link XMLPositionUtility#selectAttributeValue} — the same method
	 * the rename participant uses — so the range is computed rather than manually
	 * counted.
	 */
	private static PrepareRenameResult identifierPrepareRename(String catalogWithCursor, String expectedValue) {
		String catalog = catalogWithCursor.replace("|", "");
		DOMDocument doc = DOMParser.getInstance().parse(catalog, "file:///catalog.xml", null);
		List<CatalogEntry> entries = CatalogUtils.getCatalogEntries(doc);
		DOMElement entryElement = entries.get(0).getEntryElement();
		DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(entryElement);
		Range range = XMLPositionUtility.selectAttributeValue(attr, true);
		return pr(range, expectedValue);
	}
}
