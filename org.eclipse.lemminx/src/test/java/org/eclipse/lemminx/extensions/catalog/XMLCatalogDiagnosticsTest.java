package org.eclipse.lemminx.extensions.catalog;

import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.DiagnosticsTagSupport;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class XMLCatalogDiagnosticsTest extends AbstractCacheBasedTest {

	@TempDir
	Path tempDir;

	/**
	 * Creates common grammar files so that catalog entries with local URIs never
	 * trigger catalog_uri.
	 */
	@BeforeEach
	public void createGrammarFiles() throws IOException {
		Files.writeString(tempDir.resolve("catalog.xml"),
				"<?xml version=\"1.0\"?><catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\"/>");
		Files.createFile(tempDir.resolve("schema.xsd"));
		Files.createFile(tempDir.resolve("other.xsd"));
		Files.createFile(tempDir.resolve("schema.dtd"));
		Files.createFile(tempDir.resolve("data2.xsd"));
	}

	/**
	 * catalog_not_registered warning on {@code <catalog>} with no leading indent
	 * (line 1, cols 1-8).
	 */
	private static Diagnostic unregistered() {
		return d(1, 1, 1, 8, XMLCatalogErrorCode.catalog_not_registered);
	}

	/**
	 * catalog_not_registered warning on {@code  <catalog>} with 2-space indent
	 * (line 1, cols 3-10).
	 */
	private static Diagnostic unregisteredIndented() {
		return d(1, 3, 1, 10, XMLCatalogErrorCode.catalog_not_registered);
	}

	@Test
	public void testCatalogWithValidFile() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"<system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"uri=\"src/test/resources/xsd/spring-beans-3.0.xsd\" />" + //
				"</catalog>";
		testDiagnosticsFor(xml, unregistered());
	}

	@Test
	public void testCatalogWithInvalidFile() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"<system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"uri=\"src/test/resources/xsd/spring-beans-3.0ABCDE.xsd\" />" + //
				"</catalog>";
		testDiagnosticsFor(xml, d(3, 5, 3, 53, XMLCatalogErrorCode.catalog_uri), unregistered());
	}

	@Test
	public void testCatalogWithHttpLink() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"<system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"uri=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" />" + //
				"</catalog>";
		testDiagnosticsFor(xml, unregistered());
	}

	@Test
	public void testCatalogEntryWithXMLBaseGroupValidFile() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"  <catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base= \"src\">\n" + //
				"    <group xml:base=\"test/resources/xsd/\">\n" + //
				"      <system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"      uri=\"spring-beans-3.0.xsd\" />" + //
				"    </group>" + //
				"  </catalog>";
		testDiagnosticsFor(xml, unregisteredIndented());
	}

	@Test
	public void testCatalogEntryWithXMLBaseGroupInvalidFile() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"  <catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base= \"src\">\n" + //
				"    <group xml:base=\"test/resources/xsd/\">\n" + //
				"      <system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"      uri=\"spring-beans-3.0ABCDE.xsd\" />" + //
				"    </group>" + //
				"  </catalog>";
		testDiagnosticsFor(xml, d(4, 11, 4, 36, XMLCatalogErrorCode.catalog_uri), unregisteredIndented());
	}

	@Test
	public void testCatalogEntryWithXMLBaseGroupInvalidBase() {
		String xml = "<?xml version=\"1.0\"?>\n" + //
				"  <catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\" xml:base= \"srcABCDE\">\n" + //
				"    <group xml:base=\"test/resources/xsd/\">\n" + //
				"      <system systemId=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\n" + //
				"      uri=\"spring-beans-3.0.xsd\" />" + //
				"    </group>" + //
				"  </catalog>";
		testDiagnosticsFor(xml, d(4, 11, 4, 31, XMLCatalogErrorCode.catalog_uri), unregisteredIndented());
	}

	// --- Workspace index: registration
	// -----------------------------------------------

	@Test
	public void testNoDiagnosticsWhenCatalogIsRegistered() {
		// Catalog IS registered and scan is not complete → no unused check, no
		// unregistered warning.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, service -> registerCatalog(service, catalogUri), catalogUri);
	}

	@Test
	public void testUnregisteredCatalogHintWithUnnecessaryTag() {
		// Catalog NOT registered + client supports Unnecessary tag → Hint on <catalog>.
		// uri="http://..." avoids catalog_uri (remote URIs are skipped by the checker).
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"http://example.org/schema.xsd\"/>\n"
				+ "</catalog>";
		ContentModelSettings settings = settingsWithUnnecessaryTagSupport(null);
		testDiagnosticsFor(xml, null, XMLCatalogDiagnosticsTest::scanOnly, null, true, settings,
				d(0, 1, 0, 8, XMLCatalogErrorCode.catalog_not_registered, DiagnosticTag.Unnecessary));
	}

	// --- Workspace index: unused entries
	// ---------------------------------------------

	@Test
	public void testWarningForUnusedSystemEntry() {
		// Registered catalog, scan done, no XML files reference the systemId.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri,
				d(1, 2, 1, 65, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testWarningForUnusedUriEntry() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri,
				d(1, 2, 1, 42, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testWarningForUnusedPublicEntry() {
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <public publicId=\"-//Example//DTD//EN\" uri=\"schema.dtd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri,
				d(1, 2, 1, 59, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testHintWithUnnecessaryTagWhenClientSupports() {
		// Registered catalog, scan done, unused entry, client supports Unnecessary →
		// Hint.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n" + "</catalog>";
		ContentModelSettings settings = settingsWithUnnecessaryTagSupport(catalogUri);
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri, true, settings,
				d(1, 2, 1, 65, XMLCatalogErrorCode.catalog_entry_unused, DiagnosticTag.Unnecessary));
	}

	@Test
	public void testSkipsRewriteSystemEntry() {
		// <rewriteSystem> is not in the relevant entry types → no unused diagnostic.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <rewriteSystem systemIdStartString=\"http://example.org/\" rewritePrefix=\"./\"/>\n"
				+ "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri);
	}

	@Test
	public void testSkipsRewriteUriEntry() {
		// <rewriteURI> is not in the relevant entry types → no unused diagnostic.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <rewriteURI uriStartString=\"http://example.org/\" rewritePrefix=\"./\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri);
	}

	@Test
	public void testUnusedWhenUriIsEmpty() {
		// uri="" → resolvedLocation is null, but the identifier is still never used.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"ccc\" uri=\"\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri,
				d(1, 2, 1, 26, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testUnusedRemoteUri() {
		// uri="http://..." → remote target, but the identifier "myschema" is still
		// checked for usage since the bloom index works on identifiers, not targets.
		String catalogUri = resolveUri("catalog.xml");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"myschema\" uri=\"http://remote.example.org/schema.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, scanOnly(catalogUri), catalogUri,
				d(1, 2, 1, 67, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testNoDiagnosticsWhenRemoteUriEntryIsReferenced() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path modelXml = writeFile("model.xml", "<?xml-model href=\"myschema\"?><root/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"myschema\" uri=\"http://remote.example.org/schema.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, modelXml), catalogUri);
	}

	// --- Workspace index: entries referenced via catalog identifier
	// ------------------

	@Test
	public void testNoDiagnosticsWhenSystemEntryIsReferencedViaCatalogId() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml", "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n" + //
				"  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n" + //
				"</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, orderXml), catalogUri);
	}

	@Test
	public void testNoDiagnosticsWhenUriEntryIsReferencedViaNamespace() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml", "<data xmlns=\"http://foo\"/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, dataXml), catalogUri);
	}

	@Test
	public void testDiagnosticWhenXmlUsesFilenameDirectly() throws IOException {
		// XML references the grammar by filename (not by catalog identifier) → still
		// unused.
		String catalogUri = resolveUri("catalog.xml");
		Path dataXml = writeFile("data.xml", "<data xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
				+ " xsi:noNamespaceSchemaLocation=\"data2.xsd\"/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <uri name=\"http://foo\" uri=\"data2.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, dataXml), catalogUri,
				d(1, 2, 1, 42, XMLCatalogErrorCode.catalog_entry_unused));
	}

	@Test
	public void testNoDiagnosticsWhenPublicEntryIsReferenced() throws IOException {
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml",
				"<!DOCTYPE order PUBLIC \"-//Example//DTD//EN\" \"schema.dtd\"><order/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <public publicId=\"-//Example//DTD//EN\" uri=\"schema.dtd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, orderXml), catalogUri);
	}

	@Test
	public void testMultipleEntries_partiallyUnused() throws IOException {
		// One entry referenced via its catalog identifier, the other not.
		String catalogUri = resolveUri("catalog.xml");
		Path orderXml = writeFile("order.xml", "<!DOCTYPE order SYSTEM \"http://example.org/schema\"><order/>");
		String xml = "<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">\n"
				+ "  <system systemId=\"http://example.org/schema\" uri=\"schema.xsd\"/>\n"
				+ "  <system systemId=\"http://example.org/other\" uri=\"other.xsd\"/>\n" + "</catalog>";
		testDiagnosticsFor(xml, catalogUri, withIndexedFiles(catalogUri, orderXml), catalogUri,
				d(2, 2, 2, 63, XMLCatalogErrorCode.catalog_entry_unused));
	}

	// --- Helpers
	// ---------------------------------------------------------------------

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

	/**
	 * Returns a consumer that registers the catalog and marks the workspace scan as
	 * completed. Use for tests where the catalog IS registered.
	 */
	private static Consumer<XMLLanguageService> scanOnly(String catalogUri) {
		return service -> {
			registerCatalog(service, catalogUri);
			scanOnly(service);
		};
	}

	/**
	 * Marks the workspace scan as completed WITHOUT registering a catalog. Use as a
	 * method reference for unregistered-catalog tests.
	 */
	private static void scanOnly(XMLLanguageService service) {
		((WorkspaceIndex) service.getComponent(WorkspaceIndex.class)).markScanCompleted();
	}

	/**
	 * Returns a consumer that registers the catalog, indexes the given files, then
	 * marks the scan as completed.
	 */
	private static Consumer<XMLLanguageService> withIndexedFiles(String catalogUri, Path... files) {
		return service -> {
			registerCatalog(service, catalogUri);
			WorkspaceIndex index = service.getComponent(WorkspaceIndex.class);
			for (Path file : files) {
				try {
					index.indexFile(file);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			index.markScanCompleted();
		};
	}

	/**
	 * Returns ContentModelSettings with Unnecessary tag support and an optional
	 * catalog.
	 */
	private static ContentModelSettings settingsWithUnnecessaryTagSupport(String catalogUri) {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		if (catalogUri != null) {
			settings.setCatalogs(new String[] { catalogUri });
		}
		XMLValidationRootSettings problems = new XMLValidationRootSettings();
		problems.setNoGrammar("ignore");
		PublishDiagnosticsCapabilities caps = new PublishDiagnosticsCapabilities();
		DiagnosticsTagSupport tagSupport = new DiagnosticsTagSupport();
		tagSupport.setValueSet(List.of(DiagnosticTag.Unnecessary));
		caps.setTagSupport(tagSupport);
		problems.setCapabilities(caps);
		settings.setValidation(problems);
		return settings;
	}
}
