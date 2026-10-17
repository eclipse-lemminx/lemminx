/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.generators.xml2xsd;

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.assertGrammarGenerator;

import java.io.IOException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.junit.jupiter.api.Test;

/**
 * Tests for generating XSD (XML Schema) from XML source.
 *
 */
public class XML2XMLSchemaGeneratorTest extends AbstractCacheBasedTest {

	@Test
	public void schemaWithLegacyFormatter() throws IOException {
		String xml = "<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"from\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"heading\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"body\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setLegacy(true);;
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), settings, xsd);
	}

	@Test
	public void schemaWithNewFormatter() throws IOException {
		String xml = "<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"from\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"heading\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"body\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setGrammarAwareFormatting(false);
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), settings, xsd);
	}

	@Test
	public void schemaWithJoinContentLines() throws IOException {
		String xml = "<note>\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"from\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setJoinContentLines(true);
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), settings, xsd);
	}

	@Test
	public void schemaWithNS() throws IOException {
		String xml = "<note xmlns=\"https://www.w3schools.com\">\r\n" + //
				"	<to>Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"https://www.w3schools.com\""
				+ lineSeparator() + //
				"  elementFormDefault=\"qualified\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"from\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"heading\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"body\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void schemaWithPrefixedNS() throws IOException {
		String xml = "<p:note xmlns:p=\"https://www.w3schools.com\">\r\n" + //
				"	<p:to>Tove</p:to>\r\n" + //
				"</p:note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"https://www.w3schools.com\""
				+ lineSeparator() + //
				"  elementFormDefault=\"qualified\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void schemaWithDecimalAttr() throws IOException {
		String xml = "<note version=\"1.2\" />";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:attribute name=\"version\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void schemaWithDecimalAttrAndContent() throws IOException {
		String xml = "<note version=\"1.2\" >ABCD</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:simpleContent>" + lineSeparator() + //
				"        <xs:extension base=\"xs:string\">" + lineSeparator() + //
				"          <xs:attribute name=\"version\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"        </xs:extension>" + lineSeparator() + //
				"      </xs:simpleContent>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void schemaWithDecimalAttrAndMixedContent() throws IOException {
		String xml = "<note version=\"1.2\" >AB<C/>D</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType mixed=\"true\">" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"C\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"      <xs:attribute name=\"version\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void schemaWithAttrs() throws IOException {
		String xml = "<note version=\"1.2\" >\r\n" + //
				"	<to attr1=\"abcd\" attr2=\"efgh\">Tove</to>\r\n" + //
				"	<from>Jani</from>\r\n" + //
				"	<heading>Reminder</heading>\r\n" + //
				"	<body>Don't forget me this weekend!</body>\r\n" + //
				"</note>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"note\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"to\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:simpleContent>" + lineSeparator() + //
				"              <xs:extension base=\"xs:string\">" + lineSeparator() + //
				"                <xs:attribute name=\"attr1\" use=\"required\" />" + lineSeparator() + //
				"                <xs:attribute name=\"attr2\" use=\"required\" />" + lineSeparator() + //
				"              </xs:extension>" + lineSeparator() + //
				"            </xs:simpleContent>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"        <xs:element name=\"from\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"heading\" type=\"xs:string\" />" + lineSeparator() + //
				"        <xs:element name=\"body\" type=\"xs:string\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"      <xs:attribute name=\"version\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void mixedContent() {
		String xml = "<a><b/>text</a>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"a\">" + lineSeparator() + //
				"    <xs:complexType mixed=\"true\">" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"b\" />" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void threeLevel() {
		String xml = "<a>\r\n" + //
				"	<b>\r\n" + //
				"		<c/>\r\n" + //
				"	</b>\r\n" + //
				"</a>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"a\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"b\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:sequence>" + lineSeparator() + //
				"              <xs:element name=\"c\" />" + lineSeparator() + //
				"            </xs:sequence>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void threeLevelAndText() {
		String xml = "<a>\r\n" + //
				"	<b>\r\n" + //
				"		<c />\r\n" + //
				"		<d>X</d>\r\n" + //
				"	</b>\r\n" + //
				"</a>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"a\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"b\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:sequence>" + lineSeparator() + //
				"              <xs:element name=\"c\" />" + lineSeparator() + //
				"              <xs:element name=\"d\" type=\"xs:string\" />" + lineSeparator() + //
				"            </xs:sequence>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void occurrences() {
		String xml = "<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"  xsi:noNamespaceSchemaLocation=\"grocery-invoice.xsd\">\r\n" + //
				"  <item name=\"Rice\" price=\"4.99\" />\r\n" + //
				"  <item name=\"Baked Beans\" price=\"1.99\" />\r\n" + //
				"  <item name=\"Salad\" price=\"5.99\" />\r\n" + //
				"</invoice>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"invoice\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"name\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"price\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void oneZeroOccurrences() {
		String xml = "<root>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"    </a>\r\n" + //
				"    <a></a>\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"a\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:sequence minOccurs=\"0\">" + lineSeparator() + //
				"              <xs:element name=\"b\" minOccurs=\"0\" />" + lineSeparator() + //
				"            </xs:sequence>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void oneZeroOccurrences2() {
		String xml = "<root>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"    </a>\r\n" + //
				"    <a>\r\n" + //
				"        <c />\r\n" + //
				"    </a>\r\n" + //
				"    <a>\r\n" + //
				"        <b />\r\n" + //
				"        <c />\r\n" + //
				"    </a>\r\n" + //
				"    <a></a>\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"a\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:sequence minOccurs=\"0\">" + lineSeparator() + //
				"              <xs:element name=\"b\" minOccurs=\"0\" />" + lineSeparator() + //
				"              <xs:element name=\"c\" minOccurs=\"0\" />" + lineSeparator() + //
				"            </xs:sequence>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void choice() {
		String xml = "<root>\r\n" + //
				"	<a>\r\n" + //
				"		<c />\r\n" + //
				"		<b />\r\n" + //
				"	</a>\r\n" + //
				"	<a>\r\n" + //
				"		<b />\r\n" + //
				"		<c />\r\n" + //
				"	</a>\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"a\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:sequence>" + lineSeparator() + //
				"              <xs:choice>" + lineSeparator() + //
				"                <xs:element name=\"c\" />" + lineSeparator() + //
				"                <xs:element name=\"b\" />" + lineSeparator() + //
				"              </xs:choice>" + lineSeparator() + //
				"            </xs:sequence>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void optionalAttribute() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"\" attr2=\"\"/>\r\n" + //
				"	<item attr1=\"\" />\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"attr1\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"attr2\" />" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void attrIDsAndFixed() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"id1\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id2\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id3\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id4\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id5\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id6\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id7\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id8\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id9\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id10\" attr2=\"A\" />\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"attr1\" type=\"xs:ID\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"attr2\" use=\"required\" fixed=\"A\" />" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void attrIDsAndEnums() {
		String xml = "<root>\r\n" + //
				"	<item attr1=\"id1\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id2\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id3\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id4\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id5\" attr2=\"A\" />\r\n" + //
				"	<item attr1=\"id6\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id7\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id8\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id9\" attr2=\"B\" />\r\n" + //
				"	<item attr1=\"id10\" attr2=\"B\" />\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"attr1\" type=\"xs:ID\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"attr2\" use=\"required\">" + lineSeparator() + //
				"              <xs:simpleType>" + lineSeparator() + //
				"                <xs:restriction base=\"xs:string\">" + lineSeparator() + //
				"                  <xs:enumeration value=\"A\" />" + lineSeparator() + //
				"                  <xs:enumeration value=\"B\" />" + lineSeparator() + //
				"                </xs:restriction>" + lineSeparator() + //
				"              </xs:simpleType>" + lineSeparator() + //
				"            </xs:attribute>" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void attrTypes() {
		String xml = "<root>\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"1.2\"  />\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"dateTime\" type=\"xs:dateTime\" use=\"required\" />" + lineSeparator()
				+ //
				"            <xs:attribute name=\"date\" type=\"xs:date\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"boolean\" type=\"xs:boolean\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"integer\" type=\"xs:integer\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"decimal\" type=\"xs:decimal\" use=\"required\" />" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}

	@Test
	public void attrTypesWith2Occurs() {
		String xml = "<root>\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"1.2\"  />\r\n" + //
				"	<item dateTime=\"2001-10-26T21:32:52+02:00\"\r\n" + //
				"		  date=\"2001-10-26\"\r\n" + //
				"         boolean=\"true\"\r\n" + //
				"         integer=\"1\"\r\n" + //
				"         decimal=\"XXXXXXXXXXXXXXXXXXXXXX\"  />\r\n" + //
				"</root>";
		String xsd = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
				"  <xs:element name=\"root\">" + lineSeparator() + //
				"    <xs:complexType>" + lineSeparator() + //
				"      <xs:sequence>" + lineSeparator() + //
				"        <xs:element name=\"item\" maxOccurs=\"unbounded\">" + lineSeparator() + //
				"          <xs:complexType>" + lineSeparator() + //
				"            <xs:attribute name=\"dateTime\" type=\"xs:dateTime\" use=\"required\" />" + lineSeparator()
				+ //
				"            <xs:attribute name=\"date\" type=\"xs:date\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"boolean\" type=\"xs:boolean\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"integer\" type=\"xs:integer\" use=\"required\" />" + lineSeparator() + //
				"            <xs:attribute name=\"decimal\" use=\"required\" />" + lineSeparator() + //
				"          </xs:complexType>" + lineSeparator() + //
				"        </xs:element>" + lineSeparator() + //
				"      </xs:sequence>" + lineSeparator() + //
				"    </xs:complexType>" + lineSeparator() + //
				"  </xs:element>" + lineSeparator() + //
				"</xs:schema>";
		assertGrammarGenerator(xml, new XMLSchemaGeneratorSettings(), xsd);
	}
}
