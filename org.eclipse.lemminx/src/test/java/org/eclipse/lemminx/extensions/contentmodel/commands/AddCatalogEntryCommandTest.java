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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Arrays;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.Test;

/**
 * Test for adding catalog entries via the xml.catalog.entry.add command.
 */
public class AddCatalogEntryCommandTest {

	private static final CancelChecker NULL_CHECKER = () -> {
	};

	// --- Basic entry type tests ---

	// Add <public> entry to empty catalog
	// <catalog>
	// </catalog>
	// ->
	// <catalog>
	//   <public publicId="-//Sun Microsystems...//EN" uri="../dtd/web-app_2_3.dtd" />
	// </catalog>
	@Test
	public void addPublicEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/dtd/web-app_2_3.dtd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"public", "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../dtd/web-app_2_3.dtd";
		assertTextEdit(actual, 2, 0, 2, 0,
				"\r\n  <public publicId=\"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\" uri=\""
						+ expectedUri + "\" />");
	}

	// Add <system> entry to empty catalog
	// <catalog>
	// </catalog>
	// ->
	// <catalog>
	//   <system systemId="http://www.springframework.org/..." uri="../xsd/spring-beans-3.0.xsd" />
	// </catalog>
	@Test
	public void addSystemEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/spring-beans-3.0.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"system", "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/spring-beans-3.0.xsd";
		assertTextEdit(actual, 2, 0, 2, 0,
				"\r\n  <system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" uri=\""
						+ expectedUri + "\" />");
	}

	// Add <uri> entry to empty catalog
	// <catalog>
	// </catalog>
	// ->
	// <catalog>
	//   <uri name="http://example.org/schema/tag" uri="../xsd/tag.xsd" />
	// </catalog>
	@Test
	public void addUriEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		assertTextEdit(actual, 2, 0, 2, 0,
				"\r\n  <uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />");
	}

	// --- Non-empty catalog ---

	// Append new entry after existing entries
	// <catalog>
	//   <system systemId="http://existing.xsd" uri="existing.xsd" />
	// </catalog>
	// ->
	// <catalog>
	//   <system systemId="http://existing.xsd" uri="existing.xsd" />
	//   <uri name="http://example.org/schema/tag" uri="../xsd/tag.xsd" />
	// </catalog>
	@Test
	public void addEntryToNonEmptyCatalog() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"  <system systemId=\"http://existing.xsd\" uri=\"existing.xsd\" />\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		assertTextEdit(actual, 2, 62, 2, 62,
				"\r\n  <uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />");
	}

	// --- Self-closed catalog ---

	// Self-closed <catalog .../> is expanded to <catalog ...>entry</catalog>
	// <catalog xmlns="..." /> -> <catalog xmlns="...">
	//   <uri name="..." uri="..." />
	// </catalog>
	@Test
	public void addEntryToSelfClosedCatalog() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" />";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		// Self-closed <catalog .../> should be replaced with <catalog ...>\n  <entry />\n</catalog>
		assertTextEdit(actual, 1, 61, 1, 63,
				">\r\n  <uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />\r\n</catalog>");
	}

	// --- xml:base ---

	// xml:base="../" changes the effective base directory for relative grammar URIs
	// With xml:base="../", the relative URI is computed from the parent of catalogs/
	// so xsd/tag.xsd becomes "xsd/tag.xsd" instead of "../xsd/tag.xsd"
	@Test
	public void addEntryWithXmlBase() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base=\"../\">\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "xsd/tag.xsd";
		assertTextEdit(actual, 2, 0, 2, 0,
				"\r\n  <uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />");
	}

	// --- Duplicate entry replacement ---

	// Duplicate <uri> with same name: replaces existing entry in-place
	// (XML Catalog spec: first match wins, so a duplicate would be ineffective)
	// <catalog>
	//   <uri name="http://example.org/schema/tag" uri="old/path.xsd" />
	// </catalog>
	// ->
	// <catalog>
	//   <uri name="http://example.org/schema/tag" uri="../xsd/tag.xsd" />
	// </catalog>
	@Test
	public void replaceDuplicateUriEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"  <uri name=\"http://example.org/schema/tag\" uri=\"old/path.xsd\" />\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		assertTextEdit(actual, 2, 2, 2, 65,
				"<uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />");
	}

	// Duplicate <system> with same systemId: replaces existing entry in-place
	// <catalog>
	//   <system systemId="http://example.org/old.xsd" uri="old.xsd" />
	// </catalog>
	// ->
	// <catalog>
	//   <system systemId="http://example.org/old.xsd" uri="../xsd/tag.xsd" />
	// </catalog>
	@Test
	public void replaceDuplicateSystemEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"  <system systemId=\"http://example.org/old.xsd\" uri=\"old.xsd\" />\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"system", "http://example.org/old.xsd", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		assertTextEdit(actual, 2, 2, 2, 64,
				"<system systemId=\"http://example.org/old.xsd\" uri=\"" + expectedUri + "\" />");
	}

	// Duplicate <public> with same publicId: replaces existing entry in-place
	// <catalog>
	//   <public publicId="-//Example//DTD Test//EN" uri="old.dtd" />
	// </catalog>
	// ->
	// <catalog>
	//   <public publicId="-//Example//DTD Test//EN" uri="../dtd/web-app_2_3.dtd" />
	// </catalog>
	@Test
	public void replaceDuplicatePublicEntry() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"  <public publicId=\"-//Example//DTD Test//EN\" uri=\"old.dtd\" />\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/dtd/web-app_2_3.dtd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"public", "-//Example//DTD Test//EN", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../dtd/web-app_2_3.dtd";
		assertTextEdit(actual, 2, 2, 2, 62,
				"<public publicId=\"-//Example//DTD Test//EN\" uri=\"" + expectedUri + "\" />");
	}

	// Different identifier: no replacement, new entry is appended
	// <catalog>
	//   <uri name="http://example.org/schema/other" uri="other.xsd" />
	// </catalog>
	// ->
	// <catalog>
	//   <uri name="http://example.org/schema/other" uri="other.xsd" />
	//   <uri name="http://example.org/schema/tag" uri="../xsd/tag.xsd" />
	// </catalog>
	@Test
	public void noReplacementForDifferentIdentifier() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"  <uri name=\"http://example.org/schema/other\" uri=\"other.xsd\" />\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeAddCatalogEntry(catalog, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		String expectedUri = "../xsd/tag.xsd";
		assertTextEdit(actual, 2, 64, 2, 64,
				"\r\n  <uri name=\"http://example.org/schema/tag\" uri=\"" + expectedUri + "\" />");
	}

	// --- Version ---

	// Verify that the TextDocumentEdit uses the document's version
	@Test
	public void addEntryUsesDocumentVersion() throws Exception {
		String catalog = "<?xml version=\"1.0\"?>\r\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\r\n" + //
				"</catalog>";
		String catalogURI = getFileURI("src/test/resources/catalogs/test-catalog.xml");
		String grammarURI = getFileURI("src/test/resources/xsd/tag.xsd");

		DOMDocument document = DOMParser.getInstance().parse(catalog, catalogURI, null);
		document.getTextDocument().setVersion(42);

		TextDocumentEdit actual = executeAddCatalogEntryWithDocument(document, catalogURI,
				"uri", "http://example.org/schema/tag", grammarURI);

		assertNotNull(actual);
		assertEquals(42, actual.getTextDocument().getVersion());
	}

	// --- Helper methods ---

	private static TextDocumentEdit executeAddCatalogEntry(String catalogContent, String catalogURI,
			String entryType, String identifier, String grammarURI) throws Exception {
		DOMDocument document = DOMParser.getInstance().parse(catalogContent, catalogURI, null);
		return executeAddCatalogEntryWithDocument(document, catalogURI, entryType, identifier, grammarURI);
	}

	private static TextDocumentEdit executeAddCatalogEntryWithDocument(DOMDocument document, String catalogURI,
			String entryType, String identifier, String grammarURI) throws Exception {
		IXMLDocumentProvider documentProvider = uri -> {
			if (catalogURI.equals(uri)) {
				return document;
			}
			return null;
		};
		AddCatalogEntryCommand command = new AddCatalogEntryCommand(documentProvider);
		ExecuteCommandParams params = new ExecuteCommandParams(
				AddCatalogEntryCommand.COMMAND_ID,
				Arrays.asList(catalogURI, entryType, identifier, grammarURI));
		SharedSettings sharedSettings = new SharedSettings();
		return (TextDocumentEdit) command.executeCommand(params, sharedSettings, NULL_CHECKER);
	}

	private static void assertTextEdit(TextDocumentEdit actual, int startLine, int startChar,
			int endLine, int endChar, String expectedText) {
		assertEquals(1, actual.getEdits().size());
		TextEdit edit = actual.getEdits().get(0).getLeft();
		assertEquals(new Range(new Position(startLine, startChar), new Position(endLine, endChar)),
				edit.getRange());
		assertEquals(expectedText, edit.getNewText());
	}

	private static String getFileURI(String fileName) {
		String uri = new File(fileName).toURI().toString();
		boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
		if (isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
