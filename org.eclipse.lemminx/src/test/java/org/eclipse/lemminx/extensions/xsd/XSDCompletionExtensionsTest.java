/**
 *  Copyright (c) 2018 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.extensions.xsd;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.te;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.MarkupKind;
import org.junit.jupiter.api.Test;

/**
 * XSD completion tests which test the {@link XDLURIResolverExtension}.
 *
 */
public class XSDCompletionExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void completion() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.1\"?>\r\n"
				+ "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">   \r\n"
				+ //
				"|";
		testCompletionFor(xml, c("xs:annotation", te(2, 0, 2, 0, "<xs:annotation></xs:annotation>"), "xs:annotation"),
				c("xs:attribute", te(2, 0, 2, 0, "<xs:attribute name=\"\"></xs:attribute>"), "xs:attribute"));
	}

	@Test
	public void completionWithSourceDocumentation() throws BadLocationException {
		// completion on |
		String xml = "<?xml version=\"1.1\"?>\r\n"
				+ "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">   \r\n"
				+ //
				"|";
		testCompletionFor(xml,
				c("xs:annotation", te(2, 0, 2, 0, "<xs:annotation></xs:annotation>"), "xs:annotation",
						null, null),
				c("xs:attribute", te(2, 0, 2, 0, "<xs:attribute name=\"\"></xs:attribute>"), "xs:attribute",
						null, null));
	}

	@Test
	public void completionWithSourceDescriptionAndDocumentation() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:schemaLocation=\"http://invoice xsd/invoice.xsd \">\r\n" + //
				"  <|";
		String lineSeparator = System.getProperty("line.separator");
		XMLAssert.testCompletionFor(xml, null, "src/test/resources/invoice.xml", null,
				c("date", te(3, 2, 3, 3, "<date></date>"), "<date",
						"Date Description" + lineSeparator + lineSeparator + "Source: invoice.xsd", MarkupKind.PLAINTEXT));
	}

	@Test
	public void completionOnElementType() throws BadLocationException {
		// completion on | xs:element/@type -> xs:complexType/@name, xs:simpleType/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:element name=\"elt\" type=\"|\" />\r\n" + //
				"	<xs:complexType name=\"aComplexType\" />\r\n" + //
				"	<xs:simpleType name=\"aSimpleType\" />\r\n" + //
				"</xs:schema>";
		testCompletionFor(xml, c("xs:aComplexType", te(2, 30, 2, 30, "xs:aComplexType"), "xs:aComplexType"),
				c("xs:aSimpleType", te(2, 30, 2, 30, "xs:aSimpleType"), "xs:aSimpleType"),
				c("xs:string", te(2, 30, 2, 30, "xs:string"), "xs:string"));
	}

	@Test
	public void completionOnElementTypeItemDefaults() throws BadLocationException {
		// completion on | xs:element/@type -> xs:complexType/@name, xs:simpleType/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:element name=\"elt\" type=\"|\" />\r\n" + //
				"	<xs:complexType name=\"aComplexType\" />\r\n" + //
				"	<xs:simpleType name=\"aSimpleType\" />\r\n" + //
				"</xs:schema>";
		testCompletionFor(xml, true, c("xs:aComplexType", te(2, 30, 2, 30, "xs:aComplexType"), "xs:aComplexType"),
				c("xs:aSimpleType", te(2, 30, 2, 30, "xs:aSimpleType"), "xs:aSimpleType"),
				c("xs:string", te(2, 30, 2, 30, "xs:string"), "xs:string"));
	}

	@Test
	public void completionOnAttributeType() throws BadLocationException {
		// completion on | xs:attribute/@type -> xs:simpleType/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\">\r\n"
				+ //
				"	<xs:attribute name=\"attr\" type=\"|\" />\r\n" + //
				"	<xs:complexType name=\"aComplexType\" />\r\n" + //
				"	<xs:simpleType name=\"aSimpleType\" />\r\n" + //
				"</xs:schema>";
		testCompletionFor(xml, c("xs:aSimpleType", te(2, 33, 2, 33, "xs:aSimpleType"), "xs:aSimpleType"),
				c("xs:string", te(2, 33, 2, 33, "xs:string"), "xs:string"));
	}

	@Test
	public void completionOnElementRef() throws BadLocationException {
		// completion on | xs:element/@ref -> xs:element/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:group name=\"schemaTop\">\r\n" + //
				"		<xs:choice>\r\n" + //
				"			<xs:group ref=\"xs:redefinable\" />\r\n" + //
				"			<xs:element ref=\"xs:element\" />\r\n" + //
				"			<xs:element ref=\"xs:attribute\" />\r\n" + //
				"			<xs:element ref=\"|\" />\r\n" + // here completion shows xs:element, xs:attribute,
															// xs:notation
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
		XMLAssert.testCompletionFor(xml, 3, c("xs:element", te(8, 20, 8, 20, "xs:element"), "xs:element"),
				c("xs:attribute", te(8, 20, 8, 20, "xs:attribute"), "xs:attribute"),
				c("xs:notation", te(8, 20, 8, 20, "xs:notation"), "xs:notation"));
	}

	@Test
	public void completionOnGroupRef() throws BadLocationException {
		// completion on | xs:groupt/@ref -> xs:group/@name
		String xml = "<?xml version=\"1.1\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" targetNamespace=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\r\n"
				+ //
				"	\r\n" + //
				"	<xs:group name=\"schemaTop\">\r\n" + //
				"		<xs:choice>\r\n" + //
				"			<xs:group ref=\"|\" />\r\n" + // here completion shows xs:schemaTop, xs:redefinable
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
		XMLAssert.testCompletionFor(xml, 2, c("xs:redefinable", te(5, 18, 5, 18, "xs:redefinable"), "xs:redefinable"),
				c("xs:schemaTop", te(5, 18, 5, 18, "xs:schemaTop"), "xs:schemaTop"));
	}

	@Test
	public void complectionWithXSInclude() throws BadLocationException {
		// - SchemaA includes SchemaB (which defines 'TypeFromB' xs:element)
		// - SchemaB includes SchemaC (which defines 'TypeFromC' xs:element)
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema id=\"tns\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\r\n"
				+ //
				"	<xs:include schemaLocation=\"src/test/resources/xsd/SchemaB.xsd\" />\r\n" + //
				"  \r\n" + //
				"	<xs:complexType name=\"Bar\">\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"|\" />\r\n" + // here completion shows TypeFromB, TypeFromC
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>";
		XMLAssert.testCompletionFor(xml, null, "test.xml", 2,
				c("TypeFromB", te(6, 20, 6, 20, "TypeFromB"), "TypeFromB"),
				c("TypeFromC", te(6, 20, 6, 20, "TypeFromC"), "TypeFromC"));
	}

	@Test
	public void completionWithXSImportOnType() throws BadLocationException {
		// SchemaA imports ImportedSchema (which defines 'ImportedType' complexType and
		// 'ImportedSimpleType' simpleType in namespace "http://import")
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"           xmlns:imp=\"http://import\"\r\n" + //
				"           targetNamespace=\"http://test\"\r\n" + //
				"           xmlns:tns=\"http://test\">\r\n" + //
				"	<xs:import schemaLocation=\"src/test/resources/xsd/ImportedSchema.xsd\" namespace=\"http://import\" />\r\n"
				+ //
				"	<xs:complexType name=\"LocalType\" />\r\n" + //
				"	<xs:element name=\"elt\" type=\"|\" />\r\n" + //
				"</xs:schema>";
		XMLAssert.testCompletionFor(xml, null, "test.xml", null,
				c("tns:LocalType", te(7, 30, 7, 30, "tns:LocalType"), "tns:LocalType"),
				c("imp:ImportedType", te(7, 30, 7, 30, "imp:ImportedType"), "imp:ImportedType"),
				c("imp:ImportedSimpleType", te(7, 30, 7, 30, "imp:ImportedSimpleType"), "imp:ImportedSimpleType"));
	}

	@Test
	public void completionWithXSImportOnRef() throws BadLocationException {
		// completion on xs:element/@ref should show elements from imported schema with
		// correct namespace prefix
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
				"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"           xmlns:imp=\"http://import\"\r\n" + //
				"           targetNamespace=\"http://test\"\r\n" + //
				"           xmlns:tns=\"http://test\">\r\n" + //
				"	<xs:import schemaLocation=\"src/test/resources/xsd/ImportedSchema.xsd\" namespace=\"http://import\" />\r\n"
				+ //
				"	<xs:element name=\"LocalElement\" />\r\n" + //
				"	<xs:complexType name=\"MyType\">\r\n" + //
				"		<xs:sequence>\r\n" + //
				"			<xs:element ref=\"|\" />\r\n" + //
				"		</xs:sequence>\r\n" + //
				"	</xs:complexType>\r\n" + //
				"</xs:schema>";
		XMLAssert.testCompletionFor(xml, null, "test.xml", null,
				c("tns:LocalElement", te(9, 20, 9, 20, "tns:LocalElement"), "tns:LocalElement"),
				c("imp:ImportedElement", te(9, 20, 9, 20, "imp:ImportedElement"), "imp:ImportedElement"));
	}

	private void testCompletionFor(String xml, boolean enableItemDefaults, CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(xml, null, enableItemDefaults, expectedItems);
	}

	private void testCompletionFor(String xml, CompletionItem... expectedItems) throws BadLocationException {
		XMLAssert.testCompletionFor(xml, null, expectedItems);
	}
}
