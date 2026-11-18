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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link IndexFileScanner} — combined single-pass streaming scan that
 * populates both {@link GrammarReference} refs and bloom-filter names.
 */
public class IndexFileScannerTest {

	@TempDir
	Path tempDir;

	// ---- Helpers ----

	private Path writeFile(String name, String content) throws IOException {
		Path file = tempDir.resolve(name);
		Files.writeString(file, content);
		return file;
	}

	private static boolean scan(Path path, List<GrammarReference> refs, Set<String> names) throws IOException {
		return IndexFileScanner.scan(path, Integer.MAX_VALUE, Collections.emptyList(), Collections.emptyList(), refs, names);
	}

	private static List<GrammarReference> refs(Path path) throws IOException {
		List<GrammarReference> refs = new ArrayList<>();
		scan(path, refs, new LinkedHashSet<>());
		return refs;
	}

	private static Set<String> names(Path path) throws IOException {
		Set<String> names = new LinkedHashSet<>();
		scan(path, new ArrayList<>(), names);
		return names;
	}

	private static GrammarReference refOfKind(List<GrammarReference> refs, GrammarReference.Kind kind) {
		return refs.stream().filter(r -> r.kind() == kind).findFirst()
				.orElseThrow(() -> new AssertionError("No ref of kind " + kind));
	}

	/**
	 * Asserts all fields of a {@link GrammarReference} in a single call.
	 * Uses {@code assertAll} so every mismatch is reported, not just the first.
	 */
	private static void assertRef(GrammarReference ref,
			String value, GrammarReference.Kind kind,
			int line, int startChar, int endChar) {
		assertAll(
				() -> assertEquals(value,     ref.value(),     "value"),
				() -> assertEquals(kind,      ref.kind(),      "kind"),
				() -> assertEquals(line,      ref.line(),      "line"),
				() -> assertEquals(startChar, ref.startChar(), "startChar"),
				() -> assertEquals(endChar,   ref.endChar(),   "endChar")
		);
	}

	/**
	 * Asserts the bloom-filter name set matches exactly the given expected names.
	 * Uses {@code assertAll} to report both missing and unexpected names.
	 */
	private static void assertNames(Set<String> names, String... expected) {
		Set<String> expectedSet = Set.of(expected);
		assertAll(
				() -> {
					Set<String> missing = new java.util.LinkedHashSet<>(expectedSet);
					missing.removeAll(names);
					assertTrue(missing.isEmpty(), "Missing names: " + missing);
				},
				() -> {
					Set<String> extra = new java.util.LinkedHashSet<>(names);
					extra.removeAll(expectedSet);
					assertTrue(extra.isEmpty(), "Unexpected names: " + extra);
				}
		);
	}

	// ---- UC14: xml-model href ----

	@Test
	public void testXmlModelHref() throws IOException {
		// <?xml-model href="schema.xsd"?>
		//  0         1
		//  0123456789012345678
		// piStartChar=12, valStart=6 → startChar=18, endChar=28
		Path file = writeFile("test.xml",
				"<?xml-model href=\"schema.xsd\"?>\n<root/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		boolean partial = scan(file, refs, names);

		assertFalse(partial);
		assertEquals(1, refs.size());
		assertRef(refs.get(0), "schema.xsd", GrammarReference.Kind.XML_MODEL_HREF, 0, 18, 28);
		assertNames(names, "schema.xsd", "root");
	}

	@Test
	public void testXmlModelHrefWithType() throws IOException {
		// <?xml-model href="order.xsd" type="application/xml"?>
		// piStartChar=12, valStart=6 → startChar=18; "order.xsd"=9 → endChar=27
		Path file = writeFile("test.xml",
				"<?xml-model href=\"order.xsd\" type=\"application/xml\"?>\n<order/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(1, refs.size());
		assertRef(refs.get(0), "order.xsd", GrammarReference.Kind.XML_MODEL_HREF, 0, 18, 27);
		assertNames(names, "order.xsd", "order");
	}

	@Test
	public void testXmlModelHrefRelativePath() throws IOException {
		// <?xml-model href="../schemas/base.xsd"?>
		// piStartChar=12, valStart=6 → startChar=18; "../schemas/base.xsd"=19 → endChar=37
		Path file = writeFile("test.xml",
				"<?xml-model href=\"../schemas/base.xsd\"?><root/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(1, refs.size());
		assertRef(refs.get(0), "../schemas/base.xsd", GrammarReference.Kind.XML_MODEL_HREF, 0, 18, 37);
		assertNames(names, "../schemas/base.xsd", "base.xsd", "root");
	}

	// ---- UC15: noNamespaceSchemaLocation ----

	@Test
	public void testNoNamespaceSchemaLocation() throws IOException {
		// line 0: <root xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		//          0         1
		//          01234567890123456789...
		//         '<root xmlns:xsi="' → startChar=17, len=41 → endChar=58
		// line 1:       xsi:noNamespaceSchemaLocation="schema.xsd"/>
		//               '      xsi:noNamespaceSchemaLocation="' → startChar=37, len=10 → endChar=47
		Path file = writeFile("test.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema-instance",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 17, 58);
		assertRef(refs.get(1), "schema.xsd",
				GrammarReference.Kind.NO_NS_SCHEMA_LOCATION, 1, 37, 47);
		assertNames(names, "root", "http://www.w3.org/2001/XMLSchema-instance", "schema.xsd");
	}

	// ---- UC16: xsi:schemaLocation pairs ----

	@Test
	public void testSchemaLocationPairs() throws IOException {
		// line 0: <root xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		//         startChar=17, endChar=58
		// line 1:       xsi:schemaLocation="http://example.com/ns invoice.xsd"/>
		//         baseStartChar=26
		//         "http://example.com/ns" at [0,21) → startChar=26, endChar=47
		//         "invoice.xsd"           at [22,33) → startChar=48, endChar=59
		Path file = writeFile("test.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://example.com/ns invoice.xsd\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(3, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema-instance",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 17, 58);
		assertRef(refs.get(1), "http://example.com/ns",
				GrammarReference.Kind.NAMESPACE_DECL, 1, 26, 47);
		assertRef(refs.get(2), "invoice.xsd",
				GrammarReference.Kind.SCHEMA_LOCATION, 1, 48, 59);
		assertNames(names, "root", "http://www.w3.org/2001/XMLSchema-instance",
				"http://example.com/ns", "invoice.xsd");
	}

	@Test
	public void testSchemaLocationMultiplePairs() throws IOException {
		// line 1:       xsi:schemaLocation="http://ns1 a.xsd  http://ns2 b.xsd"/>
		//         baseStartChar=26
		//         "http://ns1" at [0,10)  → startChar=26, endChar=36
		//         "a.xsd"      at [11,16) → startChar=37, endChar=42
		//         "http://ns2" at [18,28) → startChar=44, endChar=54
		//         "b.xsd"      at [29,34) → startChar=55, endChar=60
		Path file = writeFile("test.xml",
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://ns1 a.xsd  http://ns2 b.xsd\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(5, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema-instance",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 17, 58);
		assertRef(refs.get(1), "http://ns1", GrammarReference.Kind.NAMESPACE_DECL, 1, 26, 36);
		assertRef(refs.get(2), "a.xsd",      GrammarReference.Kind.SCHEMA_LOCATION,  1, 37, 42);
		assertRef(refs.get(3), "http://ns2", GrammarReference.Kind.NAMESPACE_DECL, 1, 44, 54);
		assertRef(refs.get(4), "b.xsd",      GrammarReference.Kind.SCHEMA_LOCATION,  1, 55, 60);
		assertNames(names, "root", "http://www.w3.org/2001/XMLSchema-instance",
				"http://ns1", "a.xsd", "http://ns2", "b.xsd");
	}

	// ---- xs:include / xs:import schemaLocation ----

	@Test
	public void testXsIncludeSchemaLocation() throws IOException {
		// line 0: <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
		//         '<xs:schema xmlns:xs="' → startChar=21, len=32 → endChar=53
		// line 1:   <xs:include schemaLocation="common.xsd"/>
		//           '  <xs:include schemaLocation="' → startChar=30, len=10 → endChar=40
		Path file = writeFile("types.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"common.xsd\"/>\n" +
				"</xs:schema>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 21, 53);
		assertRef(refs.get(1), "common.xsd",
				GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION, 1, 30, 40);
		assertNames(names, "schema", "http://www.w3.org/2001/XMLSchema", "include", "common.xsd");
	}

	@Test
	public void testXsImportSchemaLocation() throws IOException {
		// line 1:   <xs:import namespace="http://example.com" schemaLocation="types.xsd"/>
		//           '  <xs:import namespace="http://example.com" schemaLocation="' → startChar=60, len=9 → endChar=69
		Path file = writeFile("service.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:import namespace=\"http://example.com\" schemaLocation=\"types.xsd\"/>\n" +
				"</xs:schema>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 21, 53);
		assertRef(refs.get(1), "types.xsd",
				GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION, 1, 60, 69);
		assertNames(names, "schema", "http://www.w3.org/2001/XMLSchema", "import", "types.xsd");
	}

	// ---- href attribute ----

	@Test
	public void testHrefAttribute() throws IOException {
		// line 0: <xi:include xmlns:xi="http://www.w3.org/2001/XInclude"
		//         '<xi:include xmlns:xi="' → startChar=22, len=31 → endChar=53
		// line 1:             href="fragment.xml"/>
		//         '            href="' → startChar=18, len=12 → endChar=30
		Path file = writeFile("doc.xml",
				"<xi:include xmlns:xi=\"http://www.w3.org/2001/XInclude\"\n" +
				"            href=\"fragment.xml\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XInclude",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 22, 53);
		assertRef(refs.get(1), "fragment.xml",
				GrammarReference.Kind.HREF, 1, 18, 30);
		assertNames(names, "include", "http://www.w3.org/2001/XInclude", "fragment.xml");
	}

	@Test
	public void testWsdlImportLocation() throws IOException {
		// line 0: <wsdl:definitions xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/">
		//         '<wsdl:definitions xmlns:wsdl="' → startChar=30, len=32 → endChar=62
		// line 1:   <wsdl:import location="types.wsdl" .../>
		//           '  <wsdl:import location="' → startChar=25, len=10 → endChar=35
		Path file = writeFile("service.wsdl",
				"<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
				"  <wsdl:import location=\"types.wsdl\" namespace=\"http://example.com/types\"/>\n" +
				"</wsdl:definitions>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "http://schemas.xmlsoap.org/wsdl/",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 30, 62);
		assertRef(refs.get(1), "types.wsdl",
				GrammarReference.Kind.HREF, 1, 25, 35);
		assertNames(names, "definitions", "http://schemas.xmlsoap.org/wsdl/", "import", "types.wsdl");
	}

	// ---- DOCTYPE ----

	@Test
	public void testDoctypeSystem() throws IOException {
		// <!DOCTYPE config SYSTEM "config.dtd">
		//  0         1         2         3
		//  0123456789012345678901234567890123456
		// '<!DOCTYPE config SYSTEM "' → startChar=25, len=10 → endChar=35
		Path file = writeFile("config.xml",
				"<!DOCTYPE config SYSTEM \"config.dtd\">\n<config/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(1, refs.size());
		assertRef(refs.get(0), "config.dtd", GrammarReference.Kind.DOCTYPE_SYSTEM, 0, 25, 35);
		assertNames(names, "config.dtd", "config");
	}

	@Test
	public void testDoctypePublicAndSystem() throws IOException {
		// <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0//EN" "xhtml1.dtd">
		// PUBLIC id: '<!DOCTYPE html PUBLIC "' → startChar=23, len=25 → endChar=48
		// SYSTEM id: after closing '"'(48) + ' "(50)   → startChar=51, len=10 → endChar=61
		Path file = writeFile("page.xml",
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0//EN\" \"xhtml1.dtd\">\n<html/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "-//W3C//DTD XHTML 1.0//EN",
				GrammarReference.Kind.DOCTYPE_PUBLIC, 0, 23, 48);
		assertRef(refs.get(1), "xhtml1.dtd",
				GrammarReference.Kind.DOCTYPE_SYSTEM, 0, 51, 61);
		assertNames(names, "-//W3C//DTD XHTML 1.0//EN", "xhtml1.dtd", "html");
	}

	// ---- xmlns namespace declarations ----

	@Test
	public void testXmlnsDeclaration() throws IOException {
		// <root xmlns="http://example.com/ns"/>
		// '<root xmlns="' → startChar=13, len=21 → endChar=34
		Path file = writeFile("test.xml",
				"<root xmlns=\"http://example.com/ns\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(1, refs.size());
		assertRef(refs.get(0), "http://example.com/ns",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 13, 34);
		assertNames(names, "root", "http://example.com/ns");
	}

	// ---- Bloom-filter name collection ----

	@Test
	public void testElementNamesInBloomFilter() throws IOException {
		Path file = writeFile("types.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"person\"/>\n" +
				"  <xs:complexType name=\"PersonType\"/>\n" +
				"</xs:schema>");
		assertNames(names(file), "schema", "http://www.w3.org/2001/XMLSchema",
				"element", "person", "complexType", "PersonType");
	}

	@Test
	public void testSymbolNamesForXsdAttributes() throws IOException {
		Path file = writeFile("service.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"person\" type=\"PersonType\"\n" +
				"              substitutionGroup=\"entity\"/>\n" +
				"  <xs:complexType name=\"employee\">\n" +
				"    <xs:extension base=\"PersonType\"/>\n" +
				"  </xs:complexType>\n" +
				"</xs:schema>");
		assertNames(names(file), "schema", "http://www.w3.org/2001/XMLSchema",
				"element", "person", "PersonType", "entity",
				"complexType", "employee", "extension");
	}

	@Test
	public void testSymbolNamesStripPrefixToo() throws IOException {
		Path file = writeFile("service.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element type=\"tns:PersonType\"/>\n" +
				"</xs:schema>");
		assertNames(names(file), "schema", "http://www.w3.org/2001/XMLSchema",
				"element", "tns:PersonType", "PersonType");
	}

	@Test
	public void testFilePathAndBasenameInNames() throws IOException {
		Path file = writeFile("test.xml",
				"<root xsi:noNamespaceSchemaLocation=\"schemas/types.xsd\"\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		assertNames(names(file), "root", "schemas/types.xsd", "types.xsd",
				"http://www.w3.org/2001/XMLSchema-instance");
	}

	// ---- Multiple refs in one file ----

	@Test
	public void testMultipleGrammarRefs() throws IOException {
		// line 0: <?xml version="1.0"?>
		// line 1: <?xml-model href="model.rng"?>   piStartChar=12, valStart=6 → start=18, "model.rng"=9 → end=27
		// line 2: <root xmlns:xsi="..."            startChar=17, end=58
		// line 3:       xsi:noNamespaceSchemaLocation="schema.xsd"/>  startChar=37, end=47
		Path file = writeFile("complex.xml",
				"<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"model.rng\"?>\n" +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(3, refs.size());
		assertRef(refs.get(0), "model.rng",  GrammarReference.Kind.XML_MODEL_HREF,         1, 18, 27);
		assertRef(refs.get(1), "http://www.w3.org/2001/XMLSchema-instance",
				GrammarReference.Kind.NAMESPACE_DECL, 2, 17, 58);
		assertRef(refs.get(2), "schema.xsd", GrammarReference.Kind.NO_NS_SCHEMA_LOCATION,  3, 37, 47);
		assertNames(names, "model.rng", "root",
				"http://www.w3.org/2001/XMLSchema-instance", "schema.xsd");
	}

	// ---- GrammarReference positions ----

	@Test
	public void testXmlModelHrefPositions() throws IOException {
		// <?xml-model href="schema.xsd"?>
		//  0         1         2
		//  0123456789012345678901234567890
		// piStartChar=12, href value at [18,28)
		Path file = writeFile("test.xml", "<?xml-model href=\"schema.xsd\"?><root/>");
		GrammarReference ref = refOfKind(refs(file), GrammarReference.Kind.XML_MODEL_HREF);

		assertRef(ref, "schema.xsd", GrammarReference.Kind.XML_MODEL_HREF, 0, 18, 28);
		assertEquals(10, ref.length());
	}

	@Test
	public void testNoNamespaceSchemaLocationPositions() throws IOException {
		// <root xsi:noNamespaceSchemaLocation="schema.xsd"/>
		//  0         1         2         3         4
		//  01234567890123456789012345678901234567890123456789
		// 'x' at col 6, '=' at 35, '"' at 36, value at [37,47)
		Path file = writeFile("test.xml",
				"<root xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>");
		GrammarReference ref = refOfKind(refs(file), GrammarReference.Kind.NO_NS_SCHEMA_LOCATION);

		assertRef(ref, "schema.xsd", GrammarReference.Kind.NO_NS_SCHEMA_LOCATION, 0, 37, 47);
	}

	@Test
	public void testDoctypeSystemPositions() throws IOException {
		// <!DOCTYPE root SYSTEM "file.dtd">
		//  0         1         2         3
		//  01234567890123456789012345678901234
		// '"' at col 22, value at [23,31)
		Path file = writeFile("test.xml",
				"<!DOCTYPE root SYSTEM \"file.dtd\"><root/>");
		GrammarReference ref = refOfKind(refs(file), GrammarReference.Kind.DOCTYPE_SYSTEM);

		assertRef(ref, "file.dtd", GrammarReference.Kind.DOCTYPE_SYSTEM, 0, 23, 31);
	}

	// ---- Entity decoding ----

	@Test
	public void testEntityDecodingInHref() throws IOException {
		// href="a&amp;b.xsd" → decoded value = "a&b.xsd"; raw length = 11
		// piStartChar=12, valStart=6, valEnd=17 → startChar=18, endChar=29
		Path file = writeFile("test.xml",
				"<?xml-model href=\"a&amp;b.xsd\"?><root/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(1, refs.size());
		assertRef(refs.get(0), "a&b.xsd", GrammarReference.Kind.XML_MODEL_HREF, 0, 18, 29);
		assertEquals(11, refs.get(0).length()); // raw "a&amp;b.xsd" vs decoded "a&b.xsd"
		assertNames(names, "a&b.xsd", "root");
	}

	@Test
	public void testEntityDecodingInSchemaLocation() throws IOException {
		// line 0: <root xsi:noNamespaceSchemaLocation="a&amp;b.xsd"
		//         startChar=37; raw "a&amp;b.xsd"=11 chars → endChar=48
		// line 1:       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
		//         '      xmlns:xsi="' → startChar=17, len=41 → endChar=58
		Path file = writeFile("test.xml",
				"<root xsi:noNamespaceSchemaLocation=\"a&amp;b.xsd\"\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertEquals(2, refs.size());
		assertRef(refs.get(0), "a&b.xsd",
				GrammarReference.Kind.NO_NS_SCHEMA_LOCATION, 0, 37, 48);
		assertRef(refs.get(1), "http://www.w3.org/2001/XMLSchema-instance",
				GrammarReference.Kind.NAMESPACE_DECL, 1, 17, 58);
		assertNames(names, "root", "a&b.xsd", "http://www.w3.org/2001/XMLSchema-instance");
	}

	// ---- Partial scan (maxChars limit) ----

	@Test
	public void testPartialScanStopsEarly() throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml-model href=\"schema.xsd\"?>\n");
		sb.append("<root>\n");
		while (sb.length() < 500) {
			sb.append("  <item attr=\"value\"/>\n");
		}
		sb.append("</root>");
		Path file = writeFile("test.xml", sb.toString());

		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		boolean partial = IndexFileScanner.scan(file, 60, Collections.emptyList(), Collections.emptyList(), refs, names);

		assertTrue(partial, "Scan must be partial when limit is hit");
		assertTrue(refs.stream().anyMatch(r -> "schema.xsd".equals(r.value())),
				"Ref before the limit must still be collected");
	}

	@Test
	public void testFullScanIsNotPartial() throws IOException {
		Path file = writeFile("test.xml",
				"<?xml-model href=\"schema.xsd\"?><root/>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		boolean partial = scan(file, refs, names);

		assertFalse(partial);
	}

	// ---- Empty / trivial files ----

	@Test
	public void testEmptyFile() throws IOException {
		Path file = writeFile("empty.xml", "");
		assertTrue(refs(file).isEmpty());
		assertTrue(names(file).isEmpty());
	}

	@Test
	public void testFileWithNoGrammarRefs() throws IOException {
		// "value" is a plain attr value (attr="value"), not a symbol attr → not in names
		Path file = writeFile("plain.xml",
				"<root>\n  <child attr=\"value\"/>\n</root>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertTrue(refs.isEmpty(), "No grammar refs expected");
		assertNames(names, "root", "child");
	}

	@Test
	public void testXsdWithTypeRefs() throws IOException {
		// Symbol attrs (name/type/ref/base/substitutionGroup) → bloom only, no GrammarReferences
		// Only the xmlns:xs declaration produces a ref
		// line 0: <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
		//         startChar=21, len=32 → endChar=53
		Path file = writeFile("schema.xsd",
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"person\" type=\"xs:string\"\n" +
				"              ref=\"address\" substitutionGroup=\"entity\"/>\n" +
				"  <xs:complexType>\n" +
				"    <xs:extension base=\"BaseType\"/>\n" +
				"  </xs:complexType>\n" +
				"</xs:schema>");
		List<GrammarReference> refs = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		scan(file, refs, names);

		assertNames(names, "schema", "http://www.w3.org/2001/XMLSchema",
				"element", "person", "xs:string", "string", "address", "entity",
				"complexType", "extension", "BaseType");
		assertEquals(1, refs.size());
		assertRef(refs.get(0), "http://www.w3.org/2001/XMLSchema",
				GrammarReference.Kind.NAMESPACE_DECL, 0, 21, 53);
	}
}
