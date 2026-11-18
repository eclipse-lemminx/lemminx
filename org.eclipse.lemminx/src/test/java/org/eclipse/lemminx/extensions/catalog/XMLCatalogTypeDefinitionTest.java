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

import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.testTypeDefinitionFor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link org.eclipse.lemminx.extensions.catalog.participants.XMLCatalogTypeDefinitionParticipant}.
 *
 * <p>When the user triggers "Go to Type Definition" on an XML element, the
 * participant navigates to the catalog entry that binds the element's namespace
 * URI. The selection range highlights the identifier attribute value (e.g. the
 * {@code name} attribute value).
 */
public class XMLCatalogTypeDefinitionTest extends AbstractCacheBasedTest {

	private static final String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

	@TempDir
	Path tempDir;

	// --- <uri name="..."> entries ---

	@Test
	public void testTypeDefinitionNavigatesToUriEntry() throws BadLocationException, IOException {
		String catalogContent = "<?xml version=\"1.0\"?>\n"
				+ "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://example.org/ns\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		Path catalogFile = writeFile("catalog.xml", catalogContent);
		String catalogUri = catalogFile.toUri().toString();
		String xmlFileUri = tempDir.resolve("data.xml").toUri().toString();

		testTypeDefinitionFor(new XMLLanguageService(), catalogUri,
				"<da|ta xmlns=\"http://example.org/ns\"/>", xmlFileUri,
				ll(catalogUri,
						originRange("<data xmlns=\"http://example.org/ns\"/>", xmlFileUri),
						entryRange(catalogContent, catalogUri),
						identifierValueRange(catalogContent, catalogUri)));
	}

	// --- <namespace name="..."> entries ---

	@Test
	public void testTypeDefinitionNavigatesToNamespaceEntry() throws BadLocationException, IOException {
		String catalogContent = "<?xml version=\"1.0\"?>\n"
				+ "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace name=\"http://example.org/ns\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		Path catalogFile = writeFile("catalog.xml", catalogContent);
		String catalogUri = catalogFile.toUri().toString();
		String xmlFileUri = tempDir.resolve("data.xml").toUri().toString();

		testTypeDefinitionFor(new XMLLanguageService(), catalogUri,
				"<da|ta xmlns=\"http://example.org/ns\"/>", xmlFileUri,
				ll(catalogUri,
						originRange("<data xmlns=\"http://example.org/ns\"/>", xmlFileUri),
						entryRange(catalogContent, catalogUri),
						identifierValueRange(catalogContent, catalogUri)));
	}

	// --- no result when namespace does not match ---

	@Test
	public void testTypeDefinitionNoResultWhenNamespaceNotInCatalog() throws BadLocationException, IOException {
		String catalogContent = "<?xml version=\"1.0\"?>\n"
				+ "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://other.org/ns\" uri=\"other.xsd\"/>\n"
				+ "</catalog>";
		Path catalogFile = writeFile("catalog.xml", catalogContent);
		String catalogUri = catalogFile.toUri().toString();
		String xmlFileUri = tempDir.resolve("data.xml").toUri().toString();

		testTypeDefinitionFor(new XMLLanguageService(), catalogUri,
				"<da|ta xmlns=\"http://example.org/ns\"/>", xmlFileUri
				/* no expected LocationLinks */);
	}

	// --- no result when element has no namespace ---

	@Test
	public void testTypeDefinitionNoResultForElementWithoutNamespace() throws BadLocationException, IOException {
		String catalogContent = "<?xml version=\"1.0\"?>\n"
				+ "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://example.org/ns\" uri=\"s.xsd\"/>\n"
				+ "</catalog>";
		Path catalogFile = writeFile("catalog.xml", catalogContent);
		String catalogUri = catalogFile.toUri().toString();
		String xmlFileUri = tempDir.resolve("data.xml").toUri().toString();

		testTypeDefinitionFor(new XMLLanguageService(), catalogUri,
				"<da|ta/>", xmlFileUri
				/* no expected LocationLinks */);
	}

	// --- helpers ---

	private Path writeFile(String filename, String content) throws IOException {
		Path path = tempDir.resolve(filename);
		Files.writeString(path, content);
		return path;
	}

	/** Full element range of the first catalog entry, via {@link XMLPositionUtility#createRange}. */
	private static Range entryRange(String catalogContent, String catalogUri) {
		DOMElement entry = firstEntry(catalogContent, catalogUri);
		return XMLPositionUtility.createRange(entry);
	}

	/** Identifier attribute value range of the first catalog entry, via {@link XMLPositionUtility#selectAttributeValue}. */
	private static Range identifierValueRange(String catalogContent, String catalogUri) {
		DOMElement entry = firstEntry(catalogContent, catalogUri);
		DOMAttr identifierAttr = CatalogUtils.getCatalogIdentifierAttr(entry);
		return XMLPositionUtility.selectAttributeValue(identifierAttr, true);
	}

	/** Origin selection range of the root element, via {@link XMLPositionUtility#createSelectionRange}. */
	private static Range originRange(String xmlContent, String xmlFileUri) {
		DOMDocument xmlDoc = DOMParser.getInstance().parse(xmlContent, xmlFileUri, null);
		return XMLPositionUtility.createSelectionRange(xmlDoc.getDocumentElement());
	}

	private static DOMElement firstEntry(String catalogContent, String catalogUri) {
		DOMDocument catalogDoc = DOMParser.getInstance().parse(catalogContent, catalogUri, null);
		List<CatalogEntry> entries = CatalogUtils.getCatalogEntries(catalogDoc);
		return entries.get(0).getEntryElement();
	}
}
