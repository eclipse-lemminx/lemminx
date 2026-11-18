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

import static org.eclipse.lemminx.XMLAssert.cl;
import static org.eclipse.lemminx.XMLAssert.r;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.client.ClientCommands;
import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.CodeLens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class XMLCatalogCodeLensTest extends AbstractCacheBasedTest {

	private static final List<String> ALL_LENS_KINDS = Arrays.asList(CodeLensKind.References);

	private static final String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";

	@TempDir
	Path tempDir;

	@BeforeEach
	public void createCatalogFile() throws IOException {
		writeFile("catalog.xml",
				"<?xml version=\"1.0\"?><catalog xmlns=\"" + CATALOG_NS + "\"/>");
	}

	@Test
	public void testRegisterCatalogLens() {
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, null, (XMLLanguageService) null, ALL_LENS_KINDS,
				(Consumer<XMLLanguageService>) null,
				gLens(),
				cl(r(0, 1, 0, 8), "Register Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testUnregisterCatalogLens() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testCodeLensOnSystemEntry_withOneReference() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml", "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, orderXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 9), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnSystemEntry_withTwoReferences() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path xml1 = writeFile("order.xml", "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		Path xml2 = writeFile("invoice.xml", "<!DOCTYPE invoice SYSTEM \"http://example.org/schema\"><invoice/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, xml1);
			indexFile(service, xml2);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 9), "2 references", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testNoCodeLensWhenXmlUsesFilenameDirectly() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml",
				"<order xsi:noNamespaceSchemaLocation=\"schema.xsd\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, orderXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testCodeLensOnUriEntry() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml", "<?xml-model href=\"myschema\"?><data/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"myschema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, dataXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnPublicEntry_viaPublicId() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml",
				"<!DOCTYPE order PUBLIC \"-//Example//DTD//EN\" \"schema.dtd\"><order/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <public publicId=\"-//Example//DTD//EN\" uri=\"schema.dtd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, orderXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 9), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnUriEntry_viaFullUri() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml", "<?xml-model href=\"http://example.org/schema\"?><order/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, orderXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnUriEntry_viaXmlns() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml", "<data xmlns=\"http://foo\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, dataXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnUriEntry_viaPrefixedXmlns() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml", "<ns:data xmlns:ns=\"http://foo\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, dataXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensOnUriEntry_viaSchemaLocation_namespace() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml",
				"<data xmlns=\"http://foo\""
				+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:schemaLocation=\"http://foo schema.xsd\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, dataXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "1 reference", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testCodeLensCountsAllBindingMechanisms() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path xml1 = writeFile("a.xml", "<a xmlns=\"http://foo\"/>");
		Path xml2 = writeFile("b.xml", "<?xml-model href=\"http://foo\"?><b/>");
		Path xml3 = writeFile("c.xml",
				"<c xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:noNamespaceSchemaLocation=\"http://foo\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
			indexFile(service, xml1);
			indexFile(service, xml2);
			indexFile(service, xml3);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION),
				cl(r(1, 3, 1, 6), "3 references", ClientCommands.SHOW_REFERENCES));
	}

	@Test
	public void testNoCodeLensWhenNoReferences() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testNoCodeLensWhenCatalogNotRegistered() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml",
				"<order xsi:noNamespaceSchemaLocation=\"schema.xsd\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			indexFile(service, orderXml);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Register Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testNoCodeLensOnNamespaceEntry() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <namespace name=\"http://example.org/ns\" uri=\"schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testNoCodeLensOnRewriteUriEntry() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <rewriteURI uriStartString=\"http://example.org/\" rewritePrefix=\"./\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	@Test
	public void testNoCodeLensOnNonCatalogDocument() {
		XMLAssert.testCodeLensFor("<root/>", null, (XMLLanguageService) null, ALL_LENS_KINDS,
				(Consumer<XMLLanguageService>) null);
	}

	@Test
	public void testNoCodeLensWhenUriIsHttpUrl() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"" + CATALOG_NS + "\">\n"
				+ "  <uri name=\"myschema\" uri=\"http://remote.example.org/schema.xsd\"/>\n"
				+ "</catalog>";
		XMLAssert.testCodeLensFor(xml, catalogUri, (XMLLanguageService) null, ALL_LENS_KINDS, service -> {
			registerCatalog(service, catalogUri);
		},
				gLens(),
				cl(r(0, 1, 0, 8), "Unregister Catalog", ClientCommands.UPDATE_CONFIGURATION));
	}

	// --- Helpers ---------------------------------------------------------------------

	/**
	 * Grammar reference lens always present for catalog documents: the OASIS
	 * catalog XSD is always embedded. With ALL_LENS_KINDS limited to References
	 * (no OpenUri), the lens has an empty command.
	 */
	private static CodeLens gLens() {
		return cl(r(0, 0, 0, 0), CATALOG_NS + " (embedded catalog.xsd)", "");
	}

	private Path writeFile(String filename, String content) throws IOException {
		Path path = resolvePath(filename);
		Files.writeString(path, content);
		return path;
	}

	private Path resolvePath(String filename) {
		return tempDir.resolve(filename);
	}

	private String resolveUri(String filename) {
		return resolvePath(filename).toUri().toString();
	}

	private static void registerCatalog(XMLLanguageService service, String catalogUri) {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		settings.setCatalogs(new String[] { catalogUri });
		service.doSave(new XMLAssert.SettingsSaveContext(settings));
	}

	private static WorkspaceIndex getIndex(XMLLanguageService service) {
		return service.getComponent(WorkspaceIndex.class);
	}

	private static void indexFile(XMLLanguageService service, Path file) {
		try {
			getIndex(service).indexFile(file);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
