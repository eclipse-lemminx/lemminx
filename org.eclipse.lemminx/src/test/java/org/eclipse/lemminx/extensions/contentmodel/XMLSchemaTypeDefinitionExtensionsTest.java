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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testTypeDefinitionFor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

/**
 * XSD type definition tests.
 *
 */
public class XMLSchemaTypeDefinitionExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void globalXSElementWithXSChoice() throws BadLocationException {
		String xmlFile = "src/test/resources/choice.xml";
		String xsdFile = "xsd/choice.xsd";

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<pers|on\r\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"        xsi:noNamespaceSchemaLocation=\"" + xsdFile + "\">\r\n" + //
				"    <employee></employee>\r\n" + //
				"    <member></member>\r\n" + //
				"</person>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, ll(targetSchemaURI, r(1, 1, 1, 7), r(5, 21, 5, 29)));
	}

	@Test
	public void localXSElementWithXSChoice() throws BadLocationException {
		String xmlFile = "src/test/resources/choice.xml";
		String xsdFile = "xsd/choice.xsd";

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<person\r\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"        xsi:noNamespaceSchemaLocation=\"" + xsdFile + "\">\r\n" + //
				"    <emp|loyee></employee>\r\n" + //
				"    <member></member>\r\n" + //
				"</person>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, ll(targetSchemaURI, r(4, 5, 4, 13), r(10, 33, 10, 43)));
	}

	@Test
	public void localXSElementWithXSSequence() throws BadLocationException {
		String xmlFile = "src/test/resources/resources.xml";
		String xsdFile = "xsd/resources.xsd";

		String xml = "<resources xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\""
				+ xsdFile + "\" variant=\"\">\r\n" + //
				"	<resour|ce name=\"\">";

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, ll(targetSchemaURI, r(1, 2, 1, 10), r(18, 33, 18, 43)));

		xml = "<resources xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\""
				+ xsdFile + "\" variant=\"\">\r\n" + //
				"	<resource name=\"\">\r\n" + //
				"	  <prop|erty \">";
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, ll(targetSchemaURI, r(2, 4, 2, 12), r(10, 29, 10, 39)));
	}

	@Test
	public void localXSAttribute() throws BadLocationException {
		String xmlFile = "src/test/resources/resources.xml";
		String xsdFile = "xsd/resources.xsd";

		String xml = "<resources xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\""
				+ xsdFile + "\" var|iant=\"\">\r\n";

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(0, 115, 0, 122), r(20, 31, 20, 40)));

		xml = "<resources xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\""
				+ xsdFile + "\" variant=\"\">\r\n" + //
				"	<resource na|me=\"\">";
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(1, 11, 1, 15), r(12, 27, 12, 33)));
	}

	@Test
	public void globalXSElementWithCatalog() throws BadLocationException, MalformedURIException, IOException {
		String xmlFile = "src/test/resources/resources.xml";
		String catalogPath = "src/test/resources/catalogs/catalog.xml";
		String catalogUri = Paths.get(catalogPath).toAbsolutePath().toUri().toString();
		String firstXSDURI = XMLEntityManager.expandSystemId("xsd/edmx.xsd", xmlFile, true);
		String secondXSDURI = XMLEntityManager.expandSystemId("xsd/edm.xsd", xmlFile, true);
		String edmxNs = "http://docs.oasis-open.org/odata/ns/edmx";
		String edmNs = "http://docs.oasis-open.org/odata/ns/edm";

		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Ed|mx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n";

		testTypeDefinitionFor(xmlLanguageService, catalogPath, xml, xmlFile,
				ll(firstXSDURI, r(1, 1, 1, 10), r(68, 19, 68, 25)),
				ll(catalogUri, r(1, 1, 1, 10),
						catalogEntryTargetRange(catalogPath, edmxNs),
						catalogEntrySelectionRange(catalogPath, edmxNs)));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Uri=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n"
				+ //
				"    <edmx:Include Namespace=\"Org.OData.Core.V1\" Alias=\"Core\">\r\n" + //
				"      <Ann|otation Term=\"Core.DefaultNamespace\" />";

		testTypeDefinitionFor(xmlLanguageService, catalogPath, xml, xmlFile,
				ll(secondXSDURI, r(4, 7, 4, 17), r(229, 19, 229, 31)),
				ll(catalogUri, r(4, 7, 4, 17),
						catalogEntryTargetRange(catalogPath, edmNs),
						catalogEntrySelectionRange(catalogPath, edmNs)));

	}

	@Test
	public void localXSAttributeWithCatalog() throws BadLocationException, MalformedURIException, IOException {
		String xmlFile = "src/test/resources/resources.xml";
		String catalogPath = "src/test/resources/catalogs/catalog.xml";
		String catalogUri = Paths.get(catalogPath).toAbsolutePath().toUri().toString();
		String targetSchemaURI = XMLEntityManager.expandSystemId("xsd/edmx.xsd", xmlFile, true);
		String edmxNs = "http://docs.oasis-open.org/odata/ns/edmx";

		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Ur|i=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n";

		testTypeDefinitionFor(xmlLanguageService, catalogPath, xml, xmlFile,
				ll(targetSchemaURI, r(2, 18, 2, 21), r(82, 23, 82, 28)),
				ll(catalogUri, r(2, 18, 2, 21),
						catalogEntryTargetRange(catalogPath, edmxNs),
						catalogEntrySelectionRange(catalogPath, edmxNs)));

	}

	@Test
	public void localXSElementOutsideXSComplexType() throws BadLocationException {
		String xmlFile = "src/test/resources/Format.xml";
		String xsdFile = "xsd/Format.xsd";

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<Configuration\r\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"        xsi:noNamespaceSchemaLocation=\"" + xsdFile + "\">\r\n" + //
				"	<ViewDefinitions>\r\n" + //
				"		<View>\r\n" + //
				"			<Name></Name>\r\n" + //
				"			<OutOfBand>false</OutOfBand>\r\n" + //
				"			<ViewSelectedBy></ViewSelectedBy>\r\n" + //
				"			<Controls>\r\n" + //
				"				<Control>\r\n" + //
				"					<CustomControl>\r\n" + //
				"						<CustomEntries>\r\n" + //
				"							<CustomEntry>\r\n" + //
				"								<CustomItem>\r\n" + //
				"									<Frame>\r\n" + //
				"										<CustomItem>\r\n" + //
				"											<ExpressionBinding>\r\n" + //
				"												<Proper|tyName>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(18, 13, 18, 25), r(268, 23, 268, 37)));
	}

	@Test
	public void xsiTypeWithNamespacePrefixTypeDefinition() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/xsitype-ns.xml";
		String targetSchemaURI = XMLEntityManager.expandSystemId("xsd/xsitype-ns.xsd", xmlFile, true);

		String xml = "<root xmlns=\"http://example.com/test\"\r\n" + //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"      xmlns:tns=\"http://example.com/test\"\r\n" + //
				"      xsi:schemaLocation=\"http://example.com/test xsd/xsitype-ns.xsd\">\r\n" + //
				"  <item xsi:type=\"tns:DerivedType\" name=\"test\">\r\n" + //
				"    <tns:derived|Prop></tns:derivedProp>\r\n" + //
				"  </item>\r\n" + //
				"</root>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(5, 5, 5, 20), r(21, 37, 21, 50)));
	}

	@Test
	public void xsiTypeWithDefaultNamespaceTypeDefinition() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/xsitype-ns.xml";
		String targetSchemaURI = XMLEntityManager.expandSystemId("xsd/xsitype-ns.xsd", xmlFile, true);

		String xml = "<root xmlns=\"http://example.com/test\"\r\n" + //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"      xsi:schemaLocation=\"http://example.com/test xsd/xsitype-ns.xsd\">\r\n" + //
				"  <item xsi:type=\"DerivedType\" name=\"test\">\r\n" + //
				"    <derived|Prop></derivedProp>\r\n" + //
				"  </item>\r\n" + //
				"</root>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(4, 5, 4, 16), r(21, 37, 21, 50)));
	}

	/**
	 * See https://github.com/eclipse-lemminx/lemminx/issues/1787
	 *
	 * Test type definition on child elements inside xsi:type-derived types
	 * across namespaces, with both a:ADerived and b:BDerived blocks present.
	 */
	@Test
	public void xsiTypeDerivedChildAcrossNamespacesTypeDefinition() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/xsd/xsi-type-derived/test.xml";
		String aSchemaURI = XMLEntityManager.expandSystemId("xsd/xsi-type-derived/a.xsd",
				"src/test/resources/test.xml", true);
		String bSchemaURI = XMLEntityManager.expandSystemId("xsd/xsi-type-derived/b.xsd",
				"src/test/resources/test.xml", true);

		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		// Type definition on <a:Title> inside xsi:type="a:ADerived" — with both blocks
		String xmlA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<m:Root xmlns:m=\"http://main\"\n" + //
				"        xmlns:a=\"http://a\"\n" + //
				"        xmlns:b=\"http://b\"\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"        xsi:schemaLocation=\"http://main main.xsd\">\n" + //
				"  <m:Data xsi:type=\"a:ADerived\">\n" + //
				"    <a:Tit|le/>\n" + //
				"  </m:Data>\n" + //
				"  <m:Data xsi:type=\"b:BDerived\">\n" + //
				"    <b:Title/>\n" + //
				"  </m:Data>\n" + //
				"</m:Root>";
		testTypeDefinitionFor(xmlLanguageService, xmlA, xmlFile,
				ll(aSchemaURI, r(7, 5, 7, 12), r(12, 37, 12, 44)));

		// Type definition on <b:Title> inside xsi:type="b:BDerived" — with both blocks
		String xmlB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<m:Root xmlns:m=\"http://main\"\n" + //
				"        xmlns:a=\"http://a\"\n" + //
				"        xmlns:b=\"http://b\"\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"        xsi:schemaLocation=\"http://main main.xsd\">\n" + //
				"  <m:Data xsi:type=\"a:ADerived\">\n" + //
				"    <a:Title/>\n" + //
				"  </m:Data>\n" + //
				"  <m:Data xsi:type=\"b:BDerived\">\n" + //
				"    <b:Tit|le/>\n" + //
				"  </m:Data>\n" + //
				"</m:Root>";
		testTypeDefinitionFor(xmlLanguageService, xmlB, xmlFile,
				ll(bSchemaURI, r(10, 5, 10, 12), r(12, 37, 12, 44)));
	}

	// --- catalog entry range helpers ---

	private static Range catalogEntryTargetRange(String catalogPath, String namespaceUri) {
		try {
			java.nio.file.Path path = Paths.get(catalogPath);
			String content = Files.readString(path);
			String uri = path.toAbsolutePath().toUri().toString();
			DOMDocument doc = DOMParser.getInstance().parse(content, uri, null);
			for (CatalogEntry entry : CatalogUtils.getCatalogEntries(doc)) {
				if (namespaceUri.equals(CatalogUtils.getCatalogIdentifier(entry))) {
					return XMLPositionUtility.createRange(entry.getEntryElement());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private static Range catalogEntrySelectionRange(String catalogPath, String namespaceUri) {
		try {
			java.nio.file.Path path = Paths.get(catalogPath);
			String content = Files.readString(path);
			String uri = path.toAbsolutePath().toUri().toString();
			DOMDocument doc = DOMParser.getInstance().parse(content, uri, null);
			for (CatalogEntry entry : CatalogUtils.getCatalogEntries(doc)) {
				if (namespaceUri.equals(CatalogUtils.getCatalogIdentifier(entry))) {
					DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(entry.getEntryElement());
					return XMLPositionUtility.selectAttributeValue(attr, true);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

}
