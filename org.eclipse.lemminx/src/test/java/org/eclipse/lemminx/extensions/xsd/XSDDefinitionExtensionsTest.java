/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.xsd;

import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;

import java.nio.file.Paths;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.LocationLink;
import org.junit.jupiter.api.Test;

/**
 * XSD definition tests.
 *
 * @author Angelo ZERR
 */
public class XSDDefinitionExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void noXSDefinition() throws BadLocationException {
		// definition on |
		String xml = "<?xml version='1.0'?>\r\n" + //
				"<xs:sche|ma xmlns:xs='http://www.w3.org/2001/XMLSchema'>\r\n" + //
				"</xs:schema>"; //
		testDefinitionFor(xml, ll("test.xsd", r(1, 1, 1, 10), r(2, 2, 2, 11)));
	}

	@Test
	public void definitionOnElementType() throws BadLocationException {
		// definition on |
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:element name=\"employee\" type=\"|fullpersoninfo\" />\r\n" + // here the definition is declared
																						// in complexType/@name =
																						// 'fullpersoninfo'
				"	\r\n" + //
				"	<xs:complexType name=\"fullpersoninfo\">	\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	\r\n" + //
				"</xs:schema>";
		testDefinitionFor(xml, ll("test.xsd", r(3, 34, 3, 50), r(5, 22, 5, 38)));
	}

	@Test
	public void definitionOnExtensionBase() throws BadLocationException {
		// definition on |
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:complexType name=\"personinfo\">\r\n" + //
				"		\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	\r\n" + //
				"	<xs:complexType name=\"fullpersoninfo\">\r\n" + //
				"		<xs:complexContent>\r\n" + //
				"			<xs:extension base=\"|personinfo\">\r\n" + // // here the definition is declared in
																		// complexType/@name ='personinfo'
				"		\r\n" + //
				"			</xs:extension>\r\n" + //
				"		</xs:complexContent>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	\r\n" + //
				"</xs:schema>";
		testDefinitionFor(xml, ll("test.xsd", r(9, 22, 9, 34), r(3, 22, 3, 34)));
	}

	@Test
	public void definitionWithTargetNamespace() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n"
				+ "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"http://camel.apache.org/schema/spring\" elementFormDefault=\"qualified\" targetNamespace=\"http://camel.apache.org/schema/spring\" version=\"1.0\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:element name=\"aggregate\" type=\"|tns:aggregateDefinition\">\r\n" + // here the definition is
																								// declared in
																								// complexType/@name
																								// ='aggregateDefinition'
				"	</xs:element>\r\n" + //
				"	\r\n" + //
				"	<xs:complexType name=\"aggregateDefinition\">\r\n" + //
				"	</xs:complexType>\r\n" + //
				"	\r\n" + //
				"</xs:schema>";
		testDefinitionFor(xml, ll("test.xsd", r(3, 35, 3, 60), r(6, 22, 6, 43)));
	}

	@Test
	public void definitionOnElementRef() throws BadLocationException {
		// definition on | xs:element/@ref -> xs:element/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:group name=\"schemaTop\">\r\n" + //
				"		<xs:choice>\r\n" + //
				"			<xs:group ref=\"xs:redefinable\" />\r\n" + //
				"			<xs:element ref=\"xs:element\" />\r\n" + //
				"			<xs:element ref=\"xs:attribute\" />\r\n" + //
				"			<xs:element ref=\"|xs:notation\" />\r\n" + // here definition go to the
																		// elmeent/@name='notation'
				"		</xs:choice>\r\n" + //
				"	</xs:group>\r\n" + //
				"	\r\n" + //
				"	<xs:group name=\"redefinable\">\r\n" + //
				"		<xs:choice></xs:choice>\r\n" + //
				"	</xs:group>\r\n" + //
				"	\r\n" + //
				"	<xs:element name=\"element\" />\r\n" + //
				"	<xs:element name=\"attribute\" />\r\n" + //
				"	<xs:element name=\"notation\" />\r\n" + //
				"</xs:schema>";
		testDefinitionFor(xml, ll("test.xsd", r(8, 19, 8, 32), r(18, 18, 18, 28)));
	}

	@Test
	public void definitionOnGroupRef() throws BadLocationException {
		// definition on | xs:groupt/@ref -> xs:group/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:group name=\"schemaTop\">\r\n" + //
				"		<xs:choice>\r\n" + //
				"			<xs:group ref=\"|xs:redefinable\" />\r\n" + // here definition go to the
																		// group/@name='redefinable'
				"			<xs:element ref=\"xs:element\" />\r\n" + //
				"			<xs:element ref=\"xs:attribute\" />\r\n" + //
				"			<xs:element ref=\"xs:notation\" />\r\n" + //
				"		</xs:choice>\r\n" + //
				"	</xs:group>\r\n" + //
				"	\r\n" + //
				"	<xs:group name=\"redefinable\">\r\n" + //
				"		<xs:choice></xs:choice>\r\n" + //
				"	</xs:group>\r\n" + //
				"	\r\n" + //
				"	<xs:element name=\"element\" />\r\n" + //
				"	<xs:element name=\"attribute\" />\r\n" + //
				"	<xs:element name=\"notation\" />\r\n" + //
				"</xs:schema>";
		testDefinitionFor(xml, ll("test.xsd", r(5, 17, 5, 33), r(12, 16, 12, 29)));
	}

	@Test
	public void definitionWithXSInclude() throws BadLocationException {
		// - SchemaA includes SchemaB (which defines 'TypeFromB' xs:element)
		// - SchemaB includes SchemaC (which defines 'TypeFromC' xs:element)

		// defintion from Schema A -> Schema B
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema id=\"tns\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	<xs:include schemaLocation=\"src/test/resources/xsd/SchemaB.xsd\" />\r\n" + //
				"  \r\n" + //
				"	<xs:complexType name=\"Bar\">\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"TypeFr|omB\" />\r\n" + //
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>";
		String schemaBPath = getSchemaURI("src/test/resources/xsd/SchemaB.xsd");
		testDefinitionFor(xml, ll(schemaBPath, r(6, 19, 6, 30), r(4, 18, 4, 29)));

		// defintion from Schema A -> Schema C
		xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema id=\"tns\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	<xs:include schemaLocation=\"src/test/resources/xsd/SchemaB.xsd\" />\r\n" + //
				"  \r\n" + //
				"	<xs:complexType name=\"Bar\">\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"TypeFr|omC\" />\r\n" + //
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>";
		String schemaCPath = getSchemaURI("src/test/resources/xsd/SchemaC.xsd");
		testDefinitionFor(xml, ll(schemaCPath, r(6, 19, 6, 30), r(3, 18, 3, 29)));

	}

	@Test
	public void definitionWithXSImport() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<xs:schema targetNamespace=\"SomeNamespace\"\r\n" + //
				"           xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"           xmlns:ct=\"ChildTypes\"\r\n" + //
				"           xmlns=\"SomeNamespace\"\r\n" + //
				"           elementFormDefault=\"unqualified\">\r\n" + //
				"  <xs:import schemaLocation=\"src/test/resources/xsd/Child.xsd\" namespace=\"ChildTypes\"/>\r\n"
				+ " \r\n" + //
				"  <xs:complexType name=\"SpecialType\">\r\n" + "    <xs:complexContent>\r\n" + //
				"      <xs:extension base=\"ct:Som|eGenericType\">\r\n" + "        <xs:sequence>\r\n" + //
				"          <xs:element name=\"AdditionalField\" type=\"xs:string\" />\r\n" + //
				"        </xs:sequence>\r\n" + "      </xs:extension>\r\n" + "    </xs:complexContent>\r\n" + //
				"  </xs:complexType>\r\n" + "</xs:schema>";
		String childPath = getSchemaURI("src/test/resources/xsd/Child.xsd");
		testDefinitionFor(xml, ll(childPath, r(10, 25, 10, 45), r(5, 23, 5, 40)));
	}

	@Test
	public void definitionWithXSImportOnType() throws BadLocationException {
		// definition on type="imp:Impor|tedType" should navigate to ImportedSchema.xsd
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"           xmlns:imp=\"http://import\"\r\n" + //
				"           targetNamespace=\"http://test\"\r\n" + //
				"           xmlns:tns=\"http://test\">\r\n" + //
				"	<xs:import schemaLocation=\"src/test/resources/xsd/ImportedSchema.xsd\" namespace=\"http://import\" />\r\n"
				+ //
				"	<xs:element name=\"elt\" type=\"imp:Impor|tedType\" />\r\n" + //
				"</xs:schema>";
		String importedPath = getSchemaURI("src/test/resources/xsd/ImportedSchema.xsd");
		testDefinitionFor(xml, ll(importedPath, r(6, 29, 6, 47), r(5, 22, 5, 36)));
	}

	@Test
	public void definitionWithXSImportOnRef() throws BadLocationException {
		// definition on ref="imp:Imported|Element" should navigate to ImportedSchema.xsd
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"           xmlns:imp=\"http://import\"\r\n" + //
				"           targetNamespace=\"http://test\"\r\n" + //
				"           xmlns:tns=\"http://test\">\r\n" + //
				"	<xs:import schemaLocation=\"src/test/resources/xsd/ImportedSchema.xsd\" namespace=\"http://import\" />\r\n"
				+ //
				"	<xs:complexType name=\"MyType\">\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"imp:Imported|Element\" />\r\n" + //
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		String importedPath = getSchemaURI("src/test/resources/xsd/ImportedSchema.xsd");
		testDefinitionFor(xml, ll(importedPath, r(8, 19, 8, 40), r(11, 18, 11, 35)));
	}

	private static String getSchemaURI(String path) {
		return Paths.get(path).toUri().toString();
	}

	private static void testDefinitionFor(String xml, LocationLink... expectedItems) throws BadLocationException {
		XMLAssert.testDefinitionFor(xml, "test.xsd", expectedItems);
	}
}
