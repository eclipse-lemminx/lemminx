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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GrammarReferenceLocator}.
 */
public class GrammarReferenceLocatorTest {

	// --- xml-model href ---

	@Test
	public void testLocateXmlModelHref() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schema.xsd", ref.value());
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLocateXmlModelHrefWithPath() {
		String xml = "<?xml-model href=\"schemas/types.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schemas/types.xsd", ref.value());
		assertEquals("schemas/types.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// Fix: xml-model href with whitespace around '='
	@Test
	public void testLocateXmlModelHrefWithSpacesAroundEquals() {
		String xml = "<?xml-model href = \"schema.xsd\"?><root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schema.xsd", ref.value(),
				"href with spaces around '=' must be located");
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLocateXmlModelHrefWithMultipleSpacesAroundEquals() {
		String xml = "<?xml-model href  =  \"schema.xsd\" type=\"application/xml\"?><root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schema.xsd", ref.value());
	}

	// --- noNamespaceSchemaLocation ---

	@Test
	public void testLocateNoNamespaceSchemaLocation() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.NO_NS_SCHEMA_LOCATION);
		assertEquals("schema.xsd", ref.value());
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// --- xsi:schemaLocation ---

	@Test
	public void testLocateSchemaLocation() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://example.com schema.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.SCHEMA_LOCATION);
		assertEquals("schema.xsd", ref.value());
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLocateSchemaLocationMultiplePairs() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://ns1 types.xsd http://ns2 common.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		List<GrammarReference> slRefs = refs.stream()
				.filter(r -> r.kind() == GrammarReference.Kind.SCHEMA_LOCATION)
				.collect(Collectors.toList());
		assertEquals(2, slRefs.size());
		assertEquals("types.xsd", slRefs.get(0).value());
		assertEquals("common.xsd", slRefs.get(1).value());
		// Verify offsets point to exact positions
		assertEquals("types.xsd", xml.substring(slRefs.get(0).offset(), slRefs.get(0).offset() + slRefs.get(0).length()));
		assertEquals("common.xsd", xml.substring(slRefs.get(1).offset(), slRefs.get(1).offset() + slRefs.get(1).length()));
	}

	// Fix: bare schemaLocation on a generic element must NOT be located as INCLUDE_SCHEMA_LOCATION
	@Test
	public void testLocateBareSchemaLocationOnGenericElementNotLocated() {
		String xml = "<root schemaLocation=\"schema.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		List<GrammarReference> includeRefs = refs.stream()
				.filter(r -> r.kind() == GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION)
				.collect(Collectors.toList());
		assertTrue(includeRefs.isEmpty(),
				"Bare schemaLocation on a generic element must not produce INCLUDE_SCHEMA_LOCATION references");
	}

	// Fix: bare schemaLocation on xs:include must be located
	@Test
	public void testLocateBareSchemaLocationOnXsdInclude() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"common.xsd\"/>\n" +
				"</xs:schema>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION);
		assertEquals("common.xsd", ref.value());
		assertEquals("common.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// Fix: bare schemaLocation on xs:import must be located
	@Test
	public void testLocateBareSchemaLocationOnXsdImport() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:import namespace=\"http://example.com\" schemaLocation=\"external.xsd\"/>\n" +
				"</xs:schema>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION);
		assertEquals("external.xsd", ref.value());
	}

	// --- DOCTYPE SYSTEM ---

	@Test
	public void testLocateDoctypeSystem() {
		String xml = "<!DOCTYPE config SYSTEM \"config.dtd\">\n<config/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.DOCTYPE_SYSTEM);
		assertEquals("config.dtd", ref.value());
		assertEquals("config.dtd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// --- xs:include/xs:import schemaLocation ---

	@Test
	public void testLocateIncludeSchemaLocation() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"common.xsd\"/>\n" +
				"</xs:schema>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.INCLUDE_SCHEMA_LOCATION);
		assertEquals("common.xsd", ref.value());
		assertEquals("common.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// --- href (XInclude, XSLT) ---

	@Test
	public void testLocateXIncludeHref() {
		String xml = "<doc xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n" +
				"  <xi:include href=\"chapter1.xml\"/>\n" +
				"</doc>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.HREF);
		assertEquals("chapter1.xml", ref.value());
		assertEquals("chapter1.xml", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// --- locateReferencesTo ---

	@Test
	public void testLocateReferencesToFileName() {
		String xml = "<?xml-model href=\"schemas/order.xsd\"?>\n" +
				"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"order.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locateReferencesTo(xml, "order.xsd");
		assertEquals(2, refs.size());
	}

	@Test
	public void testLocateReferencesToNoMatch() {
		String xml = "<?xml-model href=\"schema.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locateReferencesTo(xml, "other.xsd");
		assertTrue(refs.isEmpty());
	}

	@Test
	public void testLocateReferencesToWithPath() {
		String xml = "<?xml-model href=\"../shared/types.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locateReferencesTo(xml, "types.xsd");
		assertEquals(1, refs.size());
		assertEquals("../shared/types.xsd", refs.get(0).value());
	}

	// --- Realistic document ---

	@Test
	public void testRealisticDocumentAllRefTypes() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"purchase-order.xsd\"?>\n" +
				"<po:purchaseOrder\n" +
				"    xmlns:po=\"http://example.com/po\"\n" +
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"    xsi:schemaLocation=\"http://example.com/po purchase-order.xsd\">\n" +
				"  <po:item/>\n" +
				"</po:purchaseOrder>";
		List<GrammarReference> refs = GrammarReferenceLocator.locateReferencesTo(xml, "purchase-order.xsd");
		assertEquals(2, refs.size());
		// Both should have correct offsets
		for (GrammarReference ref : refs) {
			assertEquals("purchase-order.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
		}
	}

	// --- Malformed XML ---

	@Test
	public void testMalformedXml() {
		String xml = "<?xml-model href=\"schema.xsd\"?>\n<root><unclosed>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		assertEquals(1, refs.size());
		assertEquals("schema.xsd", refs.get(0).value());
	}

	// --- line / startChar / endChar positions ---

	@Test
	public void testLineAndCharPositionsOnLine0() {
		// xml-model PI on the first line: line=0, col == absolute offset
		String xml = "<?xml-model href=\"schema.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals(0, ref.line());
		assertEquals(ref.startChar(), ref.offset()); // col == absolute offset on line 0
		assertEquals(ref.startChar() + ref.length(), ref.endChar());
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLineAndCharPositionsOnLine1() {
		// xml-model PI on the second line: line=1, startChar != absolute offset
		String line0 = "<?xml version=\"1.0\"?>";
		String xml = line0 + "\n<?xml-model href=\"schema.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals(1, ref.line());
		// startChar is the column on line 1, NOT the absolute offset
		int expectedCol = "<?xml-model href=\"".length();
		assertEquals(expectedCol, ref.startChar());
		assertEquals(expectedCol + "schema.xsd".length(), ref.endChar());
		// absolute offset still works for text extraction
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLineAndCharPositionsAttributeOnSecondLine() {
		// noNamespaceSchemaLocation on line 1
		String xml = "<root\n      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.NO_NS_SCHEMA_LOCATION);
		assertEquals(1, ref.line());
		assertEquals("schema.xsd", ref.value());
		assertEquals("schema.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	// --- XML character-reference decoding in values ---

	@Test
	public void testXmlModelHrefWithAmpersandEntity() {
		// &amp; in href must be decoded in value() for path matching, but source
		// offsets must still cover the raw "&amp;" span so text edits are correct.
		String xml = "<?xml-model href=\"a&amp;b.xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("a&b.xsd", ref.value(), "value() must be decoded");
		// Raw source span: "a&amp;b.xsd" = 11 chars
		assertEquals(11, ref.length(), "length() must reflect raw source span including &amp;");
		assertEquals("a&amp;b.xsd", xml.substring(ref.offset(), ref.offset() + ref.length()),
				"offset()+length() must point to raw source text");
	}

	@Test
	public void testNoNamespaceSchemaLocationWithAmpersandEntity() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"a&amp;b.xsd\"/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.NO_NS_SCHEMA_LOCATION);
		assertEquals("a&b.xsd", ref.value(), "value() must be decoded");
		assertEquals(11, ref.length(), "length() must reflect raw source span including &amp;");
	}

	@Test
	public void testNumericCharacterReference() {
		// &#46; = '.' (decimal), so href="schema&#46;xsd" decodes to "schema.xsd"
		String xml = "<?xml-model href=\"schema&#46;xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schema.xsd", ref.value(), "numeric decimal character reference must be decoded");
	}

	@Test
	public void testHexCharacterReference() {
		// &#x2E; = '.' (hex), so href="schema&#x2E;xsd" decodes to "schema.xsd"
		String xml = "<?xml-model href=\"schema&#x2E;xsd\"?>\n<root/>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.XML_MODEL_HREF);
		assertEquals("schema.xsd", ref.value(), "hex character reference must be decoded");
	}

	// --- WSDL import location ---

	@Test
	public void testLocateWsdlImportLocation() {
		String xml =
				"<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
				"  <wsdl:import location=\"types.wsdl\" namespace=\"http://example.com/types\"/>\n" +
				"</wsdl:definitions>";
		List<GrammarReference> refs = GrammarReferenceLocator.locate(xml);
		GrammarReference ref = findByKind(refs, GrammarReference.Kind.HREF);
		assertEquals("types.wsdl", ref.value());
		assertEquals("types.wsdl", xml.substring(ref.offset(), ref.offset() + ref.length()));
	}

	@Test
	public void testLocateWsdlImportLocationRename() {
		String xml =
				"<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
				"  <wsdl:import location=\"types.wsdl\"/>\n" +
				"</wsdl:definitions>";
		List<GrammarReference> refs = GrammarReferenceLocator.locateReferencesTo(xml, "types.wsdl");
		assertEquals(1, refs.size(), "Must find 1 reference to types.wsdl for rename support");
		assertEquals("types.wsdl", refs.get(0).value());
	}

	// --- Empty document ---

	@Test
	public void testEmptyDocument() {
		List<GrammarReference> refs = GrammarReferenceLocator.locate("<root/>");
		assertTrue(refs.isEmpty());
	}

	// --- Helpers ---

	private static GrammarReference findByKind(List<GrammarReference> refs, GrammarReference.Kind kind) {
		return refs.stream()
				.filter(r -> r.kind() == kind)
				.findFirst()
				.orElseThrow(() -> new AssertionError("No reference of kind " + kind));
	}
}
