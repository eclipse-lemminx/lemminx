/**
 *  Copyright (c) 2018 Angelo ZERR
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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.dom.DOMText;
import org.eclipse.lemminx.dom.LineIndentInfo;
import org.eclipse.lemminx.extensions.contentmodel.participants.ContentModelHoverParticipant;
import org.eclipse.lemminx.extensions.xsi.XSISchemaModel;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.extensions.hover.IHoverRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

/**
 * XML hover tests with XML Schema.
 *
 */
public class XMLSchemaHoverExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void testTagHover() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("spring-beans-3.0.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ "	<bea|n>";
		assertHover(xml,
				"Defines a single (usually named) bean. A bean definition may contain nested tags for constructor arguments, property values, lookup methods, and replaced methods. Mixing constructor injection and setter injection on the same bean is explicitly supported."
						+ //
						System.lineSeparator() + //
						System.lineSeparator() + "Source: [spring-beans-3.0.xsd](" + schemaURI + ")",
				r(2, 2, 2, 6));
	}

	@Test
	public void testAttributeNameHover() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("spring-beans-3.0.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ "	<bean clas|s=>";
		assertHover(xml,
				"The fully qualified name of the bean's class, except if it serves only as a parent definition for child bean definitions."
						+ //
						System.lineSeparator() + //
						System.lineSeparator() + "Source: [spring-beans-3.0.xsd](" + schemaURI + ")",
				r(2, 7, 2, 12));
	}

	@Test
	public void testTagHoverFromXSType() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("invoice.xsd");
		// web.xml servlet, servlet-name declares their xs:annotation not in the element
		// declaration but in type (servletType),
		// this test checks that
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoi|ce xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:schemaLocation=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				"An invoice type..." + //
						System.lineSeparator() + //
						System.lineSeparator() + "Source: [invoice.xsd](" + schemaURI + ")",
				r(1, 1, 1, 8));
	}

	@Test
	public void testTagHoverForSchemaLocation() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:schema|Location=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.SCHEMA_LOCATION_DOC, r(2, 1, 2, 19));
	}

	@Test
	public void testTagHoverForNoNamespaceSchemaLocation() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespace|SchemaLocation=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.NO_NAMESPACE_SCHEMA_LOCATION_DOC, r(2, 1, 2, 30));
	}

	@Test
	public void testTagHoverForXSInil() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:n|il=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.NIL_DOC, r(2, 1, 2, 8));
	}

	@Test
	public void testTagHoverForXSIType() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:ty|pe=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.TYPE_DOC, r(2, 1, 2, 9));
	}

	@Test
	public void testTagHoverForXSIType2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" x|si:type=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.TYPE_DOC, r(2, 1, 2, 9));
	}

	@Test
	public void testTagHoverForXSIBadPrefix() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" BAD:t|ype=\"http://invoice xsd/invoice.xsd \">\r\n";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml", null,
				r(3, 2, 3, 6));
	}

	@Test
	public void testTagHoverForXSITypeNotRoot() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				">\r\n" + //
				"<a xsi:ty|pe=\"\"></a>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.TYPE_DOC, r(3, 3, 3, 11));
	}

	@Test
	public void testTagHoverForXSINILNotRoot() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				">\r\n" + //
				"<a xsi:n|il=\"\"></a>";

		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml",
				XSISchemaModel.NIL_DOC, r(3, 3, 3, 10));
	}

	@Test
	public void testTagHoverForXSISchemaNotRoot() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<invoice xmlns=\"http://invoice\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				">\r\n" + //
				"<a xsi:noNamespa|ceSchemaLocation\"\"></a>";

		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/invoice.xml", null,
				r(3, 2, 3, 6));
	}

	@Test
	public void testHoverAttributeValueEuro() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("money.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<money xmlns=\"http://money\" currency=\"eu|ros\"\r\n" + // <- Hover
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://money xsd/money.xsd\"></money>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/money.xml", "Euro Hover" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [money.xsd](" + schemaURI + ")", r(1, 37, 1, 44));
	}

	@Test
	public void testHoverAttributeValuePound() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("money.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<money xmlns=\"http://money\" currency=\"pou|nds\"\r\n" + // <- Hover
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://money xsd/money.xsd\"></money>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/money.xml", "Pound Hover" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [money.xsd](" + schemaURI + ")", r(1, 37, 1, 45));
	}

	@Test
	public void testHoverAttributeValueNonExistent() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("money.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<money xmlns=\"http://money\" curr|ency=\"pounds\"\r\n" + // <- Hover
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://money xsd/money.xsd\"></money>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/money.xml",
				"Currency name Hover" + //
						System.lineSeparator() + //
						System.lineSeparator() + "Source: [money.xsd](" + schemaURI + ")",
				r(1, 28, 1, 36));
	}

	/**
	 * See https://github.com/redhat-developer/vscode-xml/issues/233
	 *
	 * @throws BadLocationException
	 * @throws MalformedURIException
	 */
	@Test
	public void hoverCacheBug() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("money.xsd");

		XMLLanguageService ls = new XMLLanguageService();

		String xmlAttNameHover = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<money xmlns=\"http://money\" curr|ency=\"pounds\"\r\n" + // <- Hover
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://money xsd/money.xsd\"></money>";
		XMLAssert.assertHover(ls, xmlAttNameHover, null, "src/test/resources/money.xml", "Currency name Hover" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [money.xsd](" + schemaURI + ")", r(1, 28, 1, 36));

		String xmlAttValueHover = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<money xmlns=\"http://money\" currency=\"po|unds\"\r\n" + // <- Hover
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://money xsd/money.xsd\"></money>";
		XMLAssert.assertHover(ls, xmlAttValueHover, null, "src/test/resources/money.xml", "Pound Hover" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [money.xsd](" + schemaURI + ")", r(1, 37, 1, 45));

	}

	@Test
	public void hoverAttributeValueWithUnion() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("dressSize.xsd");
		String xml = "<dress\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\"\r\n" + //
				"	size=\"sma|ll\" />";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/dress.xml", //
				"Small documentation" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [dressSize.xsd](" + schemaURI + ")",
				r(3, 6, 3, 13));
	}

	@Test
	public void hoverAttributeValueWithUnion2() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("dressSize.xsd");
		String xml = "<dress\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\"\r\n" + //
				"	size=\"lar|ge\" />";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/dress.xml", //
				"Size Type documentation" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [dressSize.xsd](" + schemaURI + ")",
				r(3, 6, 3, 13));
	}

	@Test
	public void hoverTextWithUnion() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("dressSize.xsd");
		String xml = "<dresssize\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\" >\r\n" + //
				"	sma|ll " + //
				"</dresssize>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/dress.xml", //
				"Small documentation" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [dressSize.xsd](" + schemaURI + ")",
				r(2, 52, 3, 7));
	}

	@Test
	public void hoverTextWithUnion2() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("dressSize.xsd");
		String xml = "<dresssize\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\" >\r\n" + //
				"	lar|ge " + //
				"</dresssize>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/dress.xml", //
				"Size Type documentation" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [dressSize.xsd](" + schemaURI + ")",
				r(2, 52, 3, 7));
	}

	@Test
	public void hoverTextWithUnion3() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("dressSize.xsd");
		String xml = "<dresssize\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"xsd/dressSize.xsd\" >\r\n" + //
				"	x-lar|ge " + //
				"</dresssize>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/dress.xml", //
				"Size Type documentation" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [dressSize.xsd](" + schemaURI + ")",
				r(2, 52, 3, 9));
	}

	@Test
	public void attrValueEnumWithDoc() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("attr-enum.xsd");
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"      xsi:noNamespaceSchemaLocation=\"xsd/attr-enum.xsd\"\r\n" + //
				"      attribute=\"va|lue1\" />";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/attr-enum.xml", //
				"Documentation for attribute value1" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [attr-enum.xsd](" + schemaURI + ")",
				r(2, 16, 2, 24));
	}

	@Test
	public void attrValueEnumWithNoDoc() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURIconvert("attr-enum.xsd");
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"      xsi:noNamespaceSchemaLocation=\"xsd/attr-enum.xsd\"\r\n" + //
				"      attribute=\"va|lue2\" />";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/attr-enum.xml", //
				"Documentation for attribute" + //
						System.lineSeparator() + //
						System.lineSeparator() + //
						"Source: [attr-enum.xsd](" + schemaURI + ")",
				r(2, 16, 2, 24));
	}

	@Test
	public void hoverWithNullParentNode() throws Exception {
		ContentModelHoverParticipant hoverParticipant = new ContentModelHoverParticipant();
		IHoverRequest hoverRequest = new IHoverRequest() {

			@Override
			public DOMNode getNode() {
				return new DOMText(0, 0);
			}

			@Override
			public int getOffset() {
				return 0;
			}

			@Override
			public Position getPosition() {
				return null;
			}

			@Override
			public DOMElement getParentElement() {
				return null;
			}

			@Override
			public DOMDocument getXMLDocument() {
				return null;
			}

			@Override
			public String getCurrentTag() {
				return null;
			}

			@Override
			public String getCurrentAttributeName() {
				return null;
			}

			@Override
			public DOMAttr getCurrentAttribute() {
				return null;
			}

			@Override
			public LineIndentInfo getLineIndentInfo() throws BadLocationException {
				return null;
			}

			@Override
			public <T> T getComponent(Class clazz) {
				return null;
			}

			@Override
			public boolean canSupportMarkupKind(String kind) {
				return false;
			}

			@Override
			public SharedSettings getSharedSettings() {
				return null;
			}

			@Override
			public Range getHoverRange() {
				return null;
			}

			@Override
			public boolean isOpen() {
				return false;
			}

		};
		assertNull(hoverParticipant.onText(hoverRequest, () -> {
		}));
	}

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		XMLAssert.assertHover(new XMLLanguageService(), value, "src/test/resources/catalogs/catalog.xml", null,
				expectedHoverLabel, expectedHoverRange);
	}
	
	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true).replace("///",
				"/");
	}
	
	private static String getXMLSchemaFileURIconvert(String schemaURI) throws MalformedURIException {
		String res = XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true);
		return res.replaceAll("^file:/(?!/)", "file:///");
		
	}
}
