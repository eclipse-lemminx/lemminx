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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;


import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XMLReferenceCollector}.
 */
public class XMLReferenceCollectorTest {

	// --- UC14: xml-model href ---

	@Test
	public void testXmlModelHref() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"));
	}

	@Test
	public void testXmlModelHrefWithPath() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schemas/common/types.xsd\"?>\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schemas/common/types.xsd"));
		assertTrue(names.contains("types.xsd")); // filename extracted
	}

	@Test
	public void testXmlModelHrefWithSingleQuotes() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href='grammar.rng'?>\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("grammar.rng"));
	}

	@Test
	public void testMultipleXmlModelPIs() {
		String xml = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"types.xsd\"?>\n" +
				"<?xml-model href=\"constraints.xsd\"?>\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("types.xsd"));
		assertTrue(names.contains("constraints.xsd"));
	}

	// Fix: xml-model href with whitespace around '='
	@Test
	public void testXmlModelHrefWithSpacesAroundEquals() {
		String xml = "<?xml-model href = \"schema.xsd\"?><root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"),
				"href with spaces around '=' must be collected");
	}

	@Test
	public void testXmlModelHrefWithMultipleSpacesAroundEquals() {
		String xml = "<?xml-model href  =  \"schema.xsd\" type=\"application/xml\"?><root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"),
				"href with multiple spaces around '=' must be collected");
	}

	// --- UC15: noNamespaceSchemaLocation ---

	@Test
	public void testNoNamespaceSchemaLocation() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"schema.xsd\"/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"));
	}

	@Test
	public void testNoNamespaceSchemaLocationWithPath() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:noNamespaceSchemaLocation=\"../schemas/types.xsd\"/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("../schemas/types.xsd"));
		assertTrue(names.contains("types.xsd"));
	}

	// --- UC16: schemaLocation ---

	@Test
	public void testSchemaLocation() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://example.com schema.xsd\"/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"));
	}

	@Test
	public void testSchemaLocationMultiplePairs() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://ns1 types.xsd http://ns2 common.xsd\"/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("types.xsd"));
		assertTrue(names.contains("common.xsd"));
	}

	@Test
	public void testSchemaLocationWithPaths() {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"      xsi:schemaLocation=\"http://ns schemas/order.xsd\"/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schemas/order.xsd"));
		assertTrue(names.contains("order.xsd"));
	}

	// Fix: bare schemaLocation on a generic element must NOT be collected
	@Test
	public void testBareSchemaLocationOnGenericElementNotCollected() {
		String xml = "<root schemaLocation=\"schema.xsd\"/>";
		Set<String> names = collect(xml);
		assertFalse(names.contains("schema.xsd"),
				"Bare schemaLocation on a generic element must not be treated as a grammar reference");
	}

	// Fix: bare schemaLocation on xs:include must be collected
	@Test
	public void testBareSchemaLocationOnXsdIncludeIsCollected() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"types.xsd\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("types.xsd"),
				"schemaLocation on xs:include must be collected");
	}

	// Fix: bare schemaLocation on xs:import must be collected
	@Test
	public void testBareSchemaLocationOnXsdImportIsCollected() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:import namespace=\"http://example.com\" schemaLocation=\"external.xsd\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("external.xsd"),
				"schemaLocation on xs:import must be collected");
	}

	// --- UC17/UC18: DOCTYPE ---

	@Test
	public void testDoctypeSystem() {
		String xml = "<!DOCTYPE root SYSTEM \"grammar.dtd\">\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("grammar.dtd"));
	}

	@Test
	public void testDoctypePublic() {
		String xml = "<!DOCTYPE root PUBLIC \"-//Example//DTD Test//EN\" \"test.dtd\">\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("test.dtd"));
	}

	@Test
	public void testDoctypeWithPath() {
		String xml = "<!DOCTYPE root SYSTEM \"dtds/common/grammar.dtd\">\n" +
				"<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("dtds/common/grammar.dtd"));
		assertTrue(names.contains("grammar.dtd"));
	}

	// --- UC1-UC6: XSD cross-file symbol references ---

	@Test
	public void testXsdComplexType() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"person\" type=\"PersonType\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("PersonType"));
		assertTrue(names.contains("person"));
	}

	@Test
	public void testXsdElementRef() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element ref=\"address\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("address"));
	}

	@Test
	public void testXsdExtensionBase() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:complexType name=\"EmployeeType\">\n" +
				"    <xs:complexContent>\n" +
				"      <xs:extension base=\"PersonType\">\n" +
				"        <xs:sequence>\n" +
				"          <xs:element name=\"department\" type=\"xs:string\"/>\n" +
				"        </xs:sequence>\n" +
				"      </xs:extension>\n" +
				"    </xs:complexContent>\n" +
				"  </xs:complexType>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("EmployeeType"));
		assertTrue(names.contains("PersonType"));
		assertTrue(names.contains("department"));
		assertTrue(names.contains("xs:string"));
		assertTrue(names.contains("string"));
	}

	@Test
	public void testXsdSubstitutionGroup() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:element name=\"shipTo\" substitutionGroup=\"address\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("address"));
		assertTrue(names.contains("shipTo"));
	}

	@Test
	public void testXsdRestrictionBase() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:simpleType name=\"ShortString\">\n" +
				"    <xs:restriction base=\"xs:string\"/>\n" +
				"  </xs:simpleType>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("ShortString"));
		assertTrue(names.contains("xs:string"));
		assertTrue(names.contains("string"));
	}

	@Test
	public void testXsdPrefixedType() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
				"           xmlns:tns=\"http://example.com\">\n" +
				"  <xs:element name=\"order\" type=\"tns:OrderType\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("tns:OrderType"));
		assertTrue(names.contains("OrderType")); // prefix stripped
	}

	// --- XSD include/import schemaLocation (not in XSI namespace) ---

	@Test
	public void testXsdIncludeSchemaLocation() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:include schemaLocation=\"common-types.xsd\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("common-types.xsd"));
	}

	@Test
	public void testXsdImportSchemaLocation() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:import namespace=\"http://example.com\" schemaLocation=\"external.xsd\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("external.xsd"));
	}

	@Test
	public void testXsdImportSchemaLocationWithPath() {
		String xml = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"  <xs:import schemaLocation=\"../shared/common.xsd\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("../shared/common.xsd"));
		assertTrue(names.contains("common.xsd"));
	}

	// --- UC21: XInclude href ---

	@Test
	public void testXIncludeHref() {
		String xml = "<doc xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n" +
				"  <xi:include href=\"chapter1.xml\"/>\n" +
				"  <xi:include href=\"chapter2.xml\"/>\n" +
				"</doc>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("chapter1.xml"));
		assertTrue(names.contains("chapter2.xml"));
	}

	// --- XSLT import/include ---

	@Test
	public void testXsltImportHref() {
		String xml = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">\n" +
				"  <xsl:import href=\"common-templates.xsl\"/>\n" +
				"  <xsl:include href=\"utils.xsl\"/>\n" +
				"</xsl:stylesheet>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("common-templates.xsl"));
		assertTrue(names.contains("utils.xsl"));
	}

	// --- Element and attribute names ---

	@Test
	public void testElementNames() {
		String xml = "<catalog>\n" +
				"  <book>\n" +
				"    <title>Test</title>\n" +
				"    <author>Author</author>\n" +
				"  </book>\n" +
				"</catalog>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("catalog"));
		assertTrue(names.contains("book"));
		assertTrue(names.contains("title"));
		assertTrue(names.contains("author"));
	}

	// --- Realistic full document ---

	@Test
	public void testRealisticXmlDocument() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<?xml-model href=\"../schemas/purchase-order.xsd\"?>\n" +
				"<po:purchaseOrder xmlns:po=\"http://example.com/po\"\n" +
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"    xsi:schemaLocation=\"http://example.com/po po.xsd\">\n" +
				"  <po:shipTo>\n" +
				"    <po:name>Alice</po:name>\n" +
				"    <po:street>123 Main St</po:street>\n" +
				"  </po:shipTo>\n" +
				"  <po:items>\n" +
				"    <po:item partNum=\"926-AA\">\n" +
				"      <po:productName>Widget</po:productName>\n" +
				"      <po:quantity>1</po:quantity>\n" +
				"    </po:item>\n" +
				"  </po:items>\n" +
				"</po:purchaseOrder>";
		Set<String> names = collect(xml);
		// xml-model href
		assertTrue(names.contains("../schemas/purchase-order.xsd"));
		assertTrue(names.contains("purchase-order.xsd"));
		// schemaLocation
		assertTrue(names.contains("po.xsd"));
		// element names
		assertTrue(names.contains("purchaseOrder"));
		assertTrue(names.contains("shipTo"));
		assertTrue(names.contains("items"));
		assertTrue(names.contains("item"));
	}

	@Test
	public void testRealisticXsdDocument() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
				"           xmlns:tns=\"http://example.com/types\">\n" +
				"  <xs:include schemaLocation=\"common-types.xsd\"/>\n" +
				"  <xs:complexType name=\"AddressType\">\n" +
				"    <xs:sequence>\n" +
				"      <xs:element name=\"street\" type=\"xs:string\"/>\n" +
				"      <xs:element name=\"city\" type=\"xs:string\"/>\n" +
				"      <xs:element name=\"country\" type=\"tns:CountryCode\"/>\n" +
				"    </xs:sequence>\n" +
				"  </xs:complexType>\n" +
				"  <xs:element name=\"address\" type=\"tns:AddressType\"/>\n" +
				"</xs:schema>";
		Set<String> names = collect(xml);
		// schemaLocation from xs:include
		assertTrue(names.contains("common-types.xsd"));
		// type names
		assertTrue(names.contains("AddressType"));
		assertTrue(names.contains("tns:CountryCode"));
		assertTrue(names.contains("CountryCode"));
		// element names
		assertTrue(names.contains("street"));
		assertTrue(names.contains("city"));
		assertTrue(names.contains("address"));
	}

	// --- Malformed XML ---

	@Test
	public void testMalformedXml() {
		String xml = "<?xml-model href=\"schema.xsd\"?>\n" +
				"<root>\n" +
				"  <unclosed>\n";
		Set<String> names = collect(xml);
		assertTrue(names.contains("schema.xsd"));
		assertTrue(names.contains("root"));
	}

	// --- Empty document ---

	@Test
	public void testEmptyDocument() {
		String xml = "<?xml version=\"1.0\"?>\n<root/>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("root"));
		assertFalse(names.contains("schema.xsd"));
	}

	// --- No grammar references ---

	@Test
	public void testNoGrammarReferences() {
		String xml = "<note>\n" +
				"  <to>User</to>\n" +
				"  <body>Hello</body>\n" +
				"</note>";
		Set<String> names = collect(xml);
		assertTrue(names.contains("note"));
		assertTrue(names.contains("to"));
		assertTrue(names.contains("body"));
		assertFalse(names.contains("schema.xsd"));
	}

	// --- Helpers ---

	private static Set<String> collect(String xml) {
		return XMLReferenceCollector.collect(xml);
	}
}
