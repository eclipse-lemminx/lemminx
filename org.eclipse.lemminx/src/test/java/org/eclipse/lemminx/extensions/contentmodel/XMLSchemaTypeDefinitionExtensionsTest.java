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

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
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
	public void globalXSElementWithCatalog() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/resources.xml";
		String firstXSDURI = XMLEntityManager.expandSystemId("xsd/edmx.xsd", xmlFile, true);
		String secondXSDURI = XMLEntityManager.expandSystemId("xsd/edm.xsd", xmlFile, true);

		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Ed|mx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n";

		testTypeDefinitionFor(xmlLanguageService, "src/test/resources/catalogs/catalog.xml", xml, xmlFile,
				ll(firstXSDURI, r(1, 1, 1, 10), r(68, 19, 68, 25)));

		xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Uri=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n"
				+ //
				"    <edmx:Include Namespace=\"Org.OData.Core.V1\" Alias=\"Core\">\r\n" + //
				"      <Ann|otation Term=\"Core.DefaultNamespace\" />";

		testTypeDefinitionFor(xmlLanguageService, "src/test/resources/catalogs/catalog.xml", xml, xmlFile,
				ll(secondXSDURI, r(4, 7, 4, 17), r(229, 19, 229, 31)));

	}

	@Test
	public void localXSAttributeWithCatalog() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/resources.xml";
		String targetSchemaURI = XMLEntityManager.expandSystemId("xsd/edmx.xsd", xmlFile, true);

		XMLLanguageService xmlLanguageService = new XMLLanguageService();

		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  <edmx:Reference Ur|i=\"https://oasis-tcs.github.io/odata-vocabularies/vocabularies/Org.OData.Core.V1.xml\">\r\n";

		testTypeDefinitionFor(xmlLanguageService, "src/test/resources/catalogs/catalog.xml", xml, xmlFile,
				ll(targetSchemaURI, r(2, 18, 2, 21), r(82, 23, 82, 28)));

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

}
