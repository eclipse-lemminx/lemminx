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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.lemminx.extensions.index.HeadLimitRules;
import org.eclipse.lemminx.settings.XMLWorkspaceIndexSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WorkspaceIndex} — end-to-end use cases.
 */
public class WorkspaceIndexTest {

	@TempDir
	Path tempDir;

	private WorkspaceIndex index;

	@BeforeEach
	public void setUp() {
		index = new WorkspaceIndex();
	}

	// --- UC14: Find XML files referencing a grammar via xml-model ---

	@Test
	public void testFindFilesReferencingGrammarViaXmlModel() throws IOException {
		Path xml1 = writeFile("order.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"order.xsd\"?>\n" +
				"<order/>");

		Path xml2 = writeFile("catalog.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"catalog.xsd\"?>\n" +
				"<catalog/>");

		index.indexFile(xml1);
		index.indexFile(xml2);

		List<String> refs = index.findReferencingFiles("order.xsd");
		assertTrue(refs.contains(xml1.toUri().toString()));
		assertFalse(refs.contains(xml2.toUri().toString()));
	}

	// --- UC15: Find XML files via noNamespaceSchemaLocation ---

	@Test
	public void testFindFilesViaNoNamespaceSchemaLocation() throws IOException {
		Path xml = writeFile("data.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"data-schema.xsd\">\n" +
				"  <item/>\n" +
				"</root>");

		index.indexFile(xml);

		List<String> refs = index.findReferencingFiles("data-schema.xsd");
		assertTrue(refs.contains(xml.toUri().toString()));
	}

	// --- UC16: Find XML files via schemaLocation ---

	@Test
	public void testFindFilesViaSchemaLocation() throws IOException {
		Path xml = writeFile("invoice.xml",
				"<inv:invoice xmlns:inv=\"http://example.com/inv\"\n" +
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"    xsi:schemaLocation=\"http://example.com/inv invoice.xsd\">\n" +
				"  <inv:total>100</inv:total>\n" +
				"</inv:invoice>");

		index.indexFile(xml);

		List<String> refs = index.findReferencingFiles("invoice.xsd");
		assertTrue(refs.contains(xml.toUri().toString()));
	}

	// --- UC17: Find XML files via DOCTYPE ---

	@Test
	public void testFindFilesViaDoctypeSystem() throws IOException {
		Path xml = writeFile("config.xml",
				"<!DOCTYPE config SYSTEM \"config.dtd\">\n" +
				"<config>\n" +
				"  <setting name=\"debug\" value=\"true\"/>\n" +
				"</config>");

		index.indexFile(xml);

		List<String> refs = index.findReferencingFiles("config.dtd");
		assertTrue(refs.contains(xml.toUri().toString()));
	}

	// --- UC19: Find all references to a grammar file ---

	@Test
	public void testFindAllReferencesToGrammar() throws IOException {
		Path xml1 = writeFile("a.xml",
				"<?xml-model href=\"shared.xsd\"?>\n<a/>");
		Path xml2 = writeFile("b.xml",
				"<b xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"   xsi:noNamespaceSchemaLocation=\"shared.xsd\"/>");
		Path xml3 = writeFile("c.xml",
				"<?xml-model href=\"other.xsd\"?>\n<c/>");

		index.indexFile(xml1);
		index.indexFile(xml2);
		index.indexFile(xml3);

		List<String> refs = index.findReferencingFiles("shared.xsd");
		assertTrue(refs.contains(xml1.toUri().toString()));
		assertTrue(refs.contains(xml2.toUri().toString()));
		assertFalse(refs.contains(xml3.toUri().toString()));
	}

	// --- UC1: XSD cross-file type reference ---

	@Test
	public void testFindXsdTypeUsage() throws IOException {
		Path types = writeFile("types.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:complexType name=\"PersonType\">\n" +
				"    <xs:sequence>\n" +
				"      <xs:element name=\"firstName\" type=\"xs:string\"/>\n" +
				"    </xs:sequence>\n" +
				"  </xs:complexType>\n" +
				"</xs:schema>");

		Path schema = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"types.xsd\"/>\n" +
				"  <xs:element name=\"person\" type=\"PersonType\"/>\n" +
				"</xs:schema>");

		Path unrelated = writeFile("other.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"item\" type=\"xs:string\"/>\n" +
				"</xs:schema>");

		index.indexFile(types);
		index.indexFile(schema);
		index.indexFile(unrelated);

		List<String> refs = index.findReferencingFiles("PersonType");
		assertTrue(refs.contains(types.toUri().toString()));  // definition
		assertTrue(refs.contains(schema.toUri().toString())); // usage
		assertFalse(refs.contains(unrelated.toUri().toString()));
	}

	// --- Directory scanning ---

	@Test
	public void testScanDirectory() throws IOException {
		writeFile("a.xml", "<?xml-model href=\"schema.xsd\"?>\n<a/>");
		writeFile("b.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"test\"/>\n" +
				"</xs:schema>");
		writeFile("c.txt", "This is not an XML file");

		index.scanDirectory(tempDir);

		assertEquals(2, index.size()); // only .xml and .xsd
	}

	@Test
	public void testScanDirectoryRecursive() throws IOException {
		Path subDir = tempDir.resolve("schemas");
		Files.createDirectories(subDir);

		writeFile("root.xml", "<?xml-model href=\"types.xsd\"?>\n<root/>");
		Files.writeString(subDir.resolve("types.xsd"),
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:complexType name=\"TestType\"/>\n" +
				"</xs:schema>");

		index.scanDirectory(tempDir);

		assertEquals(2, index.size());
		List<String> refs = index.findReferencingFiles("types.xsd");
		assertFalse(refs.isEmpty());
	}

	// --- Re-indexing ---

	@Test
	public void testReindexAfterFileChange() throws IOException {
		Path xml = writeFile("data.xml",
				"<?xml-model href=\"old.xsd\"?>\n<root/>");

		index.indexFile(xml);
		assertTrue(index.findReferencingFiles("old.xsd").contains(xml.toUri().toString()));

		Files.writeString(xml, "<?xml-model href=\"new.xsd\"?>\n<root/>");
		index.indexFile(xml);

		assertTrue(index.findReferencingFiles("new.xsd").contains(xml.toUri().toString()));
		assertFalse(index.findReferencingFiles("old.xsd").contains(xml.toUri().toString()));
	}

	// --- Remove file ---

	@Test
	public void testRemoveFile() throws IOException {
		Path xml = writeFile("data.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<root/>");

		index.indexFile(xml);
		assertEquals(1, index.size());

		index.removeFile(xml);
		assertEquals(0, index.size());
		assertTrue(index.findReferencingFiles("schema.xsd").isEmpty());
	}

	// --- Clear ---

	@Test
	public void testClear() throws IOException {
		writeFile("a.xml", "<a/>");
		writeFile("b.xml", "<b/>");
		index.scanDirectory(tempDir);
		assertTrue(index.size() > 0);

		index.clear();
		assertEquals(0, index.size());
	}

	// --- Use case: rename grammar file → find impacted files ---

	@Test
	public void testRenameGrammarFindImpactedFiles() throws IOException {
		Path xml1 = writeFile("order.xml",
				"<?xml-model href=\"schemas/order-schema.xsd\"?>\n<order/>");
		Path xml2 = writeFile("invoice.xml",
				"<inv xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"     xsi:noNamespaceSchemaLocation=\"schemas/order-schema.xsd\"/>");
		Path xml3 = writeFile("catalog.xml",
				"<?xml-model href=\"catalog.xsd\"?>\n<catalog/>");

		index.indexFile(xml1);
		index.indexFile(xml2);
		index.indexFile(xml3);

		// When user renames "order-schema.xsd", find all files that reference it
		List<String> impacted = index.findReferencingFiles("order-schema.xsd");
		assertTrue(impacted.contains(xml1.toUri().toString()));
		assertTrue(impacted.contains(xml2.toUri().toString()));
		assertFalse(impacted.contains(xml3.toUri().toString()));
	}

	// --- Use case: find where a DTD is used ---

	@Test
	public void testFindDtdUsage() throws IOException {
		Path html1 = writeFile("page1.xml",
				"<!DOCTYPE html SYSTEM \"strict.dtd\">\n<html/>");
		Path html2 = writeFile("page2.xml",
				"<!DOCTYPE html SYSTEM \"transitional.dtd\">\n<html/>");

		index.indexFile(html1);
		index.indexFile(html2);

		List<String> refs = index.findReferencingFiles("strict.dtd");
		assertEquals(1, refs.size());
		assertTrue(refs.contains(html1.toUri().toString()));
	}

	// --- Use case: multiple grammar types in one file ---

	@Test
	public void testMultipleGrammarTypesInOneFile() throws IOException {
		Path xml = writeFile("complex.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"model.rng\"?>\n" +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");

		index.indexFile(xml);

		assertTrue(index.findReferencingFiles("model.rng").contains(xml.toUri().toString()));
		assertTrue(index.findReferencingFiles("schema.xsd").contains(xml.toUri().toString()));
	}

	// --- Empty index ---

	@Test
	public void testEmptyIndex() {
		assertEquals(0, index.size());
		assertTrue(index.findReferencingFiles("anything").isEmpty());
	}

	// --- Large XML files (only head is scanned) ---

	@Test
	public void testLargeXmlFileOnlyHeadScanned() throws IOException {
		// Grammar ref at the top, then 200 KB of data
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml-model href=\"schema.xsd\"?>\n");
		sb.append("<root>\n");
		while (sb.length() < 200 * 1024) {
			sb.append("  <item>data</item>\n");
		}
		sb.append("</root>");
		Path xml = writeFile("large.xml", sb.toString());

		index.indexFile(xml);

		List<String> refs = index.findReferencingFiles("schema.xsd");
		assertTrue(refs.contains(xml.toUri().toString()));
	}

	@Test
	public void testSmallGrammarFileIndexedInHead() throws IOException {
		// Small XSD (< 64 KB): type definitions are captured by the head scan.
		StringBuilder sb = new StringBuilder();
		sb.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
		for (int i = 0; i < 100; i++) {
			sb.append("  <xs:element name=\"e").append(i).append("\" type=\"xs:string\"/>\n");
		}
		sb.append("  <xs:complexType name=\"DeepType\"/>\n");
		sb.append("</xs:schema>");
		Path xsd = writeFile("deep.xsd", sb.toString());

		index.indexFile(xsd);

		List<String> refs = index.findReferencingFiles("DeepType");
		assertTrue(refs.contains(xsd.toUri().toString()));
	}

	// Fix: files exceeding their headLimits entry are partiallyIndexed (always candidates).
	// This prevents OOM from large XSD files whose full read would allocate hundreds of MB.
	@Test
	public void testLargeGrammarFilePartiallyIndexed() throws IOException {
		// XSD exceeding the configured headLimits["xsd"] limit: type definitions beyond
		// the limit are not in the Bloom filter, but the file is always returned as a
		// candidate (partiallyIndexed).
		int smallLimit = 1024; // 1 KB for test speed
		index.setHeadLimits(Map.of("**/*.xsd", String.valueOf(smallLimit)));
		StringBuilder sb = new StringBuilder();
		sb.append("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
		while (sb.length() < smallLimit + 512) {
			sb.append("  <xs:element name=\"padding\" type=\"xs:string\"/>\n");
		}
		sb.append("  <xs:complexType name=\"LateType\"/>\n");
		sb.append("</xs:schema>");
		Path xsd = writeFile("large.xsd", sb.toString());

		index.indexFile(xsd);

		// File is always a candidate because it is partiallyIndexed
		List<String> candidates = index.findReferencingFiles("LateType");
		assertTrue(candidates.contains(xsd.toUri().toString()),
				"Files exceeding their headLimits entry must always be candidates (partiallyIndexed)");
	}

	// Fix: files exceeding their headLimits pattern limit are always candidates (conservative)
	@Test
	public void testPartiallyIndexedFileAlwaysCandidate() throws IOException {
		// Set a 64KB limit for xml files, then create a 70KB XML with no grammar ref
		// in the first 64KB. The file must still be a candidate (partiallyIndexed).
		int limit = 64 * 1024;
		index.setHeadLimits(Map.of("**/*.xml", String.valueOf(limit)));
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		while (sb.length() < 70 * 1024) {
			sb.append("  <item>data-without-any-grammar-reference</item>\n");
		}
		sb.append("</root>");
		Path bigXml = writeFile("big.xml", sb.toString());
		index.indexFile(bigXml);

		// The file must appear as a candidate because it is partially indexed
		List<String> candidates = index.findReferencingFiles("schema.xsd");
		assertTrue(candidates.contains(bigXml.toUri().toString()),
				"Partially indexed large files must always be candidates to avoid false negatives");
	}

	// Fix: removeFile also removes from partiallyIndexed
	@Test
	public void testRemovePartiallyIndexedFile() throws IOException {
		int limit = 64 * 1024;
		index.setHeadLimits(Map.of("**/*.xml", String.valueOf(limit)));
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		while (sb.length() < 70 * 1024) {
			sb.append("  <item>data</item>\n");
		}
		sb.append("</root>");
		Path bigXml = writeFile("big.xml", sb.toString());
		index.indexFile(bigXml);
		assertTrue(index.findReferencingFiles("anything").contains(bigXml.toUri().toString()));

		index.removeFile(bigXml);
		assertTrue(index.findReferencingFiles("anything").isEmpty(),
				"Removed file must no longer be a candidate");
	}

	// --- Scan completion ---

	@Test
	public void testScanCompletedFalseByDefault() {
		assertFalse(index.isScanCompleted());
	}

	@Test
	public void testMarkScanCompleted() {
		index.markScanCompleted();
		assertTrue(index.isScanCompleted());
	}

	@Test
	public void testClearResetsScanCompleted() {
		index.markScanCompleted();
		assertTrue(index.isScanCompleted());
		index.clear();
		assertFalse(index.isScanCompleted());
	}

	// --- Directory exclusion ---

	@Test
	public void testScanDirectoryExcludesDirectories() throws IOException {
		Path nodeModules = tempDir.resolve("node_modules");
		Files.createDirectories(nodeModules);
		Files.writeString(nodeModules.resolve("lib.xml"), "<lib/>");
		writeFile("root.xml", "<root/>");

		index.scanDirectory(tempDir, null, Set.of("node_modules"));
		assertEquals(1, index.size()); // only root.xml, not node_modules/lib.xml
	}

	@Test
	public void testScanDirectoryExcludesMultipleDirectories() throws IOException {
		Path target = tempDir.resolve("target");
		Path git = tempDir.resolve(".git");
		Files.createDirectories(target);
		Files.createDirectories(git);
		Files.writeString(target.resolve("compiled.xml"), "<compiled/>");
		Files.writeString(git.resolve("history.xml"), "<history/>");
		writeFile("src.xml", "<src/>");

		index.scanDirectory(tempDir, null, Set.of("target", ".git"));
		assertEquals(1, index.size()); // only src.xml
	}

	// --- WSDL import location ---

	@Test
	public void testFindFilesViaWsdlImportLocation() throws IOException {
		Path wsdl = writeFile("service.wsdl",
				"<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
				"  <wsdl:import location=\"types.wsdl\" namespace=\"http://example.com/types\"/>\n" +
				"</wsdl:definitions>");

		index.indexFile(wsdl);

		List<String> refs = index.findReferencingFiles("types.wsdl");
		assertTrue(refs.contains(wsdl.toUri().toString()),
				"WSDL file with wsdl:import location must be indexed as referencing the imported WSDL");
	}

	// --- Discovery progress (total=0 signal) ---

	@Test
	public void testScanDirectoryReportsDiscoveryProgress() throws IOException {
		// Create exactly 100 XML files so the every-100-files trigger fires once
		for (int i = 0; i < 100; i++) {
			writeFile(String.format("file%03d.xml", i), "<root/>");
		}

		List<int[]> calls = new ArrayList<>();
		index.scanDirectory(tempDir, (indexed, total, file) -> calls.add(new int[]{indexed, total}));

		// At least one discovery call (total=0) must have been emitted at file #100
		boolean hasDiscoveryCall = calls.stream().anyMatch(c -> c[1] == 0 && c[0] == 100);
		assertTrue(hasDiscoveryCall,
				"scanDirectory must call listener with total=0 during discovery phase every 100 files");

		// All indexing calls (total>0) must have total=100
		calls.stream().filter(c -> c[1] > 0).forEach(c ->
				assertEquals(100, c[1], "indexing calls must carry correct total"));
	}

	// --- parseSizeLimit ---

	@Test
	public void testParseSizeLimitNull() {
		assertEquals(Integer.MAX_VALUE, XMLWorkspaceIndexSettings.parseSizeLimit(null));
	}

	@Test
	public void testParseSizeLimitBlank() {
		assertEquals(Integer.MAX_VALUE, XMLWorkspaceIndexSettings.parseSizeLimit(""));
		assertEquals(Integer.MAX_VALUE, XMLWorkspaceIndexSettings.parseSizeLimit("  "));
	}

	@Test
	public void testParseSizeLimitZero() {
		assertEquals(Integer.MAX_VALUE, XMLWorkspaceIndexSettings.parseSizeLimit("0"));
	}

	@Test
	public void testParseSizeLimitRawBytes() {
		assertEquals(1024, XMLWorkspaceIndexSettings.parseSizeLimit("1024"));
	}

	@Test
	public void testParseSizeLimitKB() {
		assertEquals(512 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("512KB"));
		assertEquals(512 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("512kb"));
		assertEquals(512 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("512Ko"));
		assertEquals(512 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("512ko"));
	}

	@Test
	public void testParseSizeLimitMB() {
		assertEquals(1 * 1024 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("1MB"));
		assertEquals(1 * 1024 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("1Mo"));
	}

	@Test
	public void testParseSizeLimitGB() {
		assertEquals(1 * 1024 * 1024 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("1GB"));
		assertEquals(1 * 1024 * 1024 * 1024, XMLWorkspaceIndexSettings.parseSizeLimit("1Go"));
	}

	@Test
	public void testParseSizeLimitInvalid() {
		assertEquals(Integer.MAX_VALUE, XMLWorkspaceIndexSettings.parseSizeLimit("not-a-size"));
	}

	@Test
	public void testHeadLimitRulesExtensionPattern() throws IOException {
		// "**/*.xsd" matches XSD files but not XML files
		index.setHeadLimits(Map.of("**/*.xsd", "1024"));
		Path xml = writeFile("data.xml", "<root/>");
		index.indexFile(xml);
		// xml has no limit → fully indexed → not partiallyIndexed → not always a candidate
		// (file is small so it won't be partiallyIndexed regardless; just verify it indexed)
		assertEquals(1, index.size());
	}

	@Test
	public void testHeadLimitRulesLongestPatternWins() throws IOException {
		// vendor XSD gets no limit (null), other XSDs get 1024 chars
		// "**/*.xsd" (8 chars) < "**/vendor/**/*.xsd" (18 chars) → vendor pattern wins
		int smallLimit = 1024;
		Map<String, String> limits = new java.util.HashMap<>();
		limits.put("**/*.xsd", String.valueOf(smallLimit));
		limits.put("**/vendor/**/*.xsd", null); // null = no limit
		index.setHeadLimits(limits);
		// Create a large XSD under a "vendor" subdirectory
		Path vendor = tempDir.resolve("vendor");
		Files.createDirectories(vendor);
		StringBuilder sb = new StringBuilder("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n");
		while (sb.length() < smallLimit + 512) {
			sb.append("  <xs:element name=\"padding\" type=\"xs:string\"/>\n");
		}
		sb.append("  <xs:complexType name=\"VendorType\"/>\n</xs:schema>");
		Path vendorXsd = vendor.resolve("types.xsd");
		Files.writeString(vendorXsd, sb.toString());

		index.indexFile(vendorXsd);

		// vendor/types.xsd matched the no-limit pattern → fully indexed → VendorType is found
		List<String> refs = index.findReferencingFiles("VendorType");
		assertTrue(refs.contains(vendorXsd.toUri().toString()),
				"File matched by longer (no-limit) pattern must be fully indexed");
	}

	@Test
	public void testHeadLimitRulesMatchAllStar() throws IOException {
		// "*" is a user-friendly catch-all alias — must apply to any file, including
		// files in subdirectories, even though bare "*" does not cross path separators
		// in strict glob semantics.
		index.setHeadLimits(Map.of("*", "1024"));
		Path subDir = tempDir.resolve("deep").resolve("nested");
		Files.createDirectories(subDir);
		StringBuilder sb = new StringBuilder("<root>\n");
		while (sb.length() < 2048) {
			sb.append("  <item>data</item>\n");
		}
		sb.append("</root>");
		Path xml = subDir.resolve("data.xml");
		Files.writeString(xml, sb.toString());
		index.indexFile(xml);
		assertTrue(index.findReferencingFiles("anything").contains(xml.toUri().toString()),
				"\"*\" catch-all must apply to any file, including in subdirectories");
	}

	@Test
	public void testHeadLimitRulesIsMatchAll() {
		assertTrue(HeadLimitRules.isMatchAll("*"));
		assertTrue(HeadLimitRules.isMatchAll("**"));
		assertTrue(HeadLimitRules.isMatchAll("**/*"));
		assertFalse(HeadLimitRules.isMatchAll("**/*.xsd"));
		assertFalse(HeadLimitRules.isMatchAll("**/vendor/**"));
	}

	@Test
	public void testHeadLimitRulesExtractGlobExtFilter() {
		// Path-based patterns ending with *.ext get an extension pre-filter
		assertEquals("xsd", HeadLimitRules.extractGlobExtFilter("**/schemas/*.xsd"));
		assertEquals("xml", HeadLimitRules.extractGlobExtFilter("src/**/*.xml"));
		// No fixed extension → no pre-filter
		assertNull(HeadLimitRules.extractGlobExtFilter("**/vendor/**"));
		// No '*' before the dot → not a wildcard extension
		assertNull(HeadLimitRules.extractGlobExtFilter("**/schemas/types.xsd"));
	}

	@Test
	public void testSetHeadLimitsNullRestoresDefaultEmpty() {
		XMLWorkspaceIndexSettings settings = new XMLWorkspaceIndexSettings();
		settings.setHeadLimits(null);
		assertTrue(settings.getHeadLimits().isEmpty(),
				"setHeadLimits(null) must restore the default empty map (no limits)");
	}

	// --- headLimits integration ---

	@Test
	public void testHeadLimitsExtensionGlobPattern() throws IOException {
		// "**/*.xml" is an extension-only pattern (fast path) that caps XML files.
		// A file exceeding the limit is partiallyIndexed → always a candidate.
		index.setHeadLimits(Map.of("**/*.xml", "1024"));
		StringBuilder sb = new StringBuilder();
		sb.append("<root>\n");
		while (sb.length() < 2048) {
			sb.append("  <item>data</item>\n");
		}
		sb.append("</root>");
		Path xml = writeFile("data.xml", sb.toString());
		index.indexFile(xml);
		assertTrue(index.findReferencingFiles("anything").contains(xml.toUri().toString()),
				"File exceeding path-based glob limit must be a candidate (partiallyIndexed)");
	}

	// --- Helpers ---

	private Path writeFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}
}
