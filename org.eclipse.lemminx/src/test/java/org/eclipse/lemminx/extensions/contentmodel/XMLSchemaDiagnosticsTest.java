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

import static java.lang.System.lineSeparator;
import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.createFile;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.l;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.teOp;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.SchemaEnabled;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.EnforceQuoteStyle;
import org.eclipse.lemminx.settings.QuoteStyle;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.junit.jupiter.api.Test;

/**
 * XML diagnostics services tests
 *
 */
public class XMLSchemaDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void prematureEOFNoErrorReported() throws Exception {
		String xml = " ";
		testDiagnosticsFor(xml);
	}

	@Test
	public void prematureEOFWithPrologNoErrorReported() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> ";
		testDiagnosticsFor(xml);
	}

	@Test
	public void cvc_complex_type_2_3() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean>\r\n" + //
				"		XXXXXXXXXXXXX\r\n" + // <-- error
				"	</bean>\r\n" + //
				"</beans>";
		Diagnostic d = d(3, 2, 3, 15, XMLSchemaErrorCode.cvc_complex_type_2_3,
				"Element \'bean\' cannot contain text content.\nThe content type is defined as element-only.\n\nCode:");
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 2, 3, 15, "")));
	}

	@Test
	public void cvc_complex_type_4() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean>\r\n" + //
				"		<property></property>\r\n" + //
				"	</bean>\r\n" + //
				"</beans>";
		Diagnostic d = d(3, 3, 3, 11, XMLSchemaErrorCode.cvc_complex_type_4,
				"Attribute 'name' is missing from element 'property'.\n\nCode:");
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 11, 3, 11, " name=\"\"")));
	}

	@Test
	public void cvc_type_4_Multiple_attributes() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number>2</number>\r\n" + //
				"  <products>\r\n" + //
				"  	<product />\r\n" + // <- error
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		Diagnostic d2 = d(6, 4, 6, 11, XMLSchemaErrorCode.cvc_complex_type_4,
				"Attribute 'description' is missing from element 'product'.\n\nCode:");
		Diagnostic d1 = d(6, 4, 6, 11, XMLSchemaErrorCode.cvc_complex_type_4,
				"Attribute 'price' is missing from element 'product'.\n\nCode:");
		testDiagnosticsFor(xml, d1, d2);

		testCodeActionsFor(xml, d1, ca(d1, te(6, 11, 6, 11, " price=\"\" description=\"\"")));
	}

	@Test
	public void cvc_type_4_With_existing_attribute() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number>2</number>\r\n" + //
				"  <products>\r\n" + //
				"  	<product description=\"test\" />\r\n" + // <- error: missing 'price'
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		Diagnostic d = d(6, 4, 6, 11, XMLSchemaErrorCode.cvc_complex_type_4,
				"Attribute 'price' is missing from element 'product'.\n\nCode:");
		testDiagnosticsFor(xml, d);
		// Insert after the last existing attribute (description="test"), not after the tag name
		testCodeActionsFor(xml, d, ca(d, te(6, 30, 6, 30, " price=\"\"")));
	}

	@Test
	public void cvc_complex_type_2_4_a() throws Exception {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<XXX></XXX>\r\n" + // <- error
				"</project>";

		String message = "Element name 'XXX' is invalid.\n\nOne of the following is expected:\n - modelVersion\n - parent\n - groupId\n - artifactId\n - version\n - packaging\n - name\n - description\n - url\n - inceptionYear\n - organization\n - licenses\n - developers\n - contributors\n - mailingLists\n - prerequisites\n - modules\n - scm\n - issueManagement\n - ciManagement\n - distributionManagement\n - properties\n - dependencyManagement\n - dependencies\n - repositories\n - pluginRepositories\n - build\n - reports\n - reporting\n - profiles\n\nError indicated by:\n {http://maven.apache.org/POM/4.0.0}\nwith code:";
		testDiagnosticsWithCatalogFor(xml, d(3, 2, 3, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_a, message));
	}

	@Test
	public void cvc_complex_type_2_4_a_Disabled_Validation() throws Exception {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"	<XXX></XXX>\r\n" + // <- error
				"</project>";
		testDiagnosticsDisabledValidation(xml);
	}

	@Test
	public void cvc_complex_type_2_4_d() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<beans xmlns=\"http://www.springframework.org/schema/beans\" " + //
				"xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean>\r\n" + //
				"		<description>\r\n" + //
				"			<XXXX />\r\n" + // <- error
				"		</description>\r\n" + //
				"	</bean>\r\n" + //
				"</beans>";
		testDiagnosticsWithCatalogFor(xml, d(4, 4, 4, 8, XMLSchemaErrorCode.cvc_complex_type_2_4_d));
	}

	@Test
	public void cvc_complex_type_2_4_f() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<root\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/tag.xsd\">\r\n" + //
				"  <tag></tag>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"</root>";
		testDiagnosticsFor(xml, d(7, 3, 7, 11, XMLSchemaErrorCode.cvc_complex_type_2_4_f));
	}

	@Test
	public void cvc_type_3_1_1() throws Exception {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"<modelVersion XXXX=\"\" ></modelVersion>" + "</project>";
		Diagnostic d = d(3, 14, 3, 21, XMLSchemaErrorCode.cvc_type_3_1_1);
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(3, 14, 3, 21, "")));
	}

	@Test
	public void cvc_complex_type_3_2_2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean XXXX=\"\" >\r\n" + // <- error
				"	</bean>              \r\n" + //
				"</beans>";
		Diagnostic d = d(2, 7, 2, 11, XMLSchemaErrorCode.cvc_complex_type_3_2_2);
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 7, 2, 14, "")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_rootNewLine() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique_multiElement.xsd\">\r\n" + //
				"</root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 83, 2, 0, //
				"\r\n" + //
						"\t<authors>\r\n" + //
						"\t\t<author></author>\r\n" + //
						"\t</authors>\r\n" + //
						"\t<assistant></assistant>\r\n")),
				ca(d, te(1, 83, 2, 0, //
						"\r\n" + //
								"\t<authors>\r\n" + //
								"\t\t<author></author>\r\n" + //
								"\t</authors>\r\n")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_rootSameLine() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique_multiElement.xsd\"></root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 79, 1, 79, //
				"\r\n" + //
						"\t<authors>\r\n" + //
						"\t\t<author></author>\r\n" + //
						"\t</authors>\r\n" + //
						"\t<assistant></assistant>\r\n")),
				ca(d, te(1, 79, 1, 79, //
						"\r\n" + //
								"\t<authors>\r\n" + //
								"\t\t<author></author>\r\n" + //
								"\t</authors>\r\n")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_nonRootElement() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique_multiElement.xsd\">\r\n" + //
				"\t<authors></authors>\r\n" + //
				"\t<assistant></assistant>\r\n" + //
				"</root>";
		Diagnostic d = d(2, 2, 2, 9, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 10, 2, 10, //
				"\r\n" + //
						"\t\t<author></author>\r\n\t")),
				ca(d, te(2, 10, 2, 10, //
						"\r\n" + //
								"\t\t<author></author>\r\n\t")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_Only_Required() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique_multiElement.xsd\"></root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 79, 1, 79, //
				"\r\n" + //
						"\t<authors>\r\n" + //
						"\t\t<author></author>\r\n" + //
						"\t</authors>\r\n" + //
						"\t<assistant></assistant>\r\n")),
				ca(d, te(1, 79, 1, 79, //
						"\r\n" + //
								"\t<authors>\r\n" + //
								"\t\t<author></author>\r\n" + //
								"\t</authors>\r\n")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_Only_Required_Sequence() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/order.xsd\"></root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 65, 1, 65, //
				"\r\n" + //
						"\t<a></a>\r\n" + //
						"\t<b></b>\r\n" + //
						"\t<c></c>\r\n" + //
						"\t<d age=\"\">\r\n" + //
						"\t\t<d1></d1>\r\n" + //
						"\t</d>\r\n" + //
						"\t<e></e>\r\n")),
				ca(d, te(1, 65, 1, 65, //
						"\r\n" + //
								"\t<a></a>\r\n" + //
								"\t<d age=\"\"></d>\r\n" + //
								"\t<e></e>\r\n")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_Only_Required_Ordered() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/order.xsd\">\r\n" + //
				"\t<a></a>\r\n" + //
				"\t<c></c>\r\n" + //
				"</root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 65, 4, 0, //
				"\r\n" + //
						"\t<a></a>\r\n" + //
						"\t<b></b>\r\n" + //
						"\t<c></c>\r\n" + //
						"\t<d age=\"\">\r\n" + //
						"\t\t<d1></d1>\r\n" + //
						"\t</d>\r\n" + //
						"\t<e></e>\r\n")),
				ca(d, te(1, 65, 4, 0, //
						"\r\n" + //
								"\t<a></a>\r\n" + //
								"\t<d age=\"\"></d>\r\n" + //
								"\t<e></e>\r\n")));
	}

	@Test
	public void cvc_complex_type_2_4_bCodeAction_Complex_Children() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/order_complex_children.xsd\"></root>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 82, 1, 82, //
				"\r\n" + //
						"\t<a>\r\n" + //
						"\t</a>\r\n" + //
						"\t<b>\r\n" + //
						"\t</b>\r\n" + //
						"\t<c>\r\n" + //
						"\t</c>\r\n" + //
						"\t<d age=\"\">\r\n" + //
						"\t\t<d1></d1>\r\n" + //
						"\t</d>\r\n" + //
						"\t<e></e>\r\n")),
				ca(d, te(1, 82, 1, 82, //
						"\r\n" + //
								"\t<a>\r\n" + //
								"\t</a>\r\n" + //
								"\t<d age=\"\"></d>\r\n" + //
								"\t<e></e>\r\n")));
	}

	@Test
	public void cvc_attribute_3() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
				+ //
				"	<bean autowire=\"ERROR\" />\r\n" + // <- error
				"</beans>";
		testDiagnosticsWithCatalogFor(xml, d(2, 16, 2, 23, XMLSchemaErrorCode.cvc_enumeration_valid),
				d(2, 16, 2, 23, XMLSchemaErrorCode.cvc_attribute_3));
	}

	@Test
	public void cvc_type_3_1_3() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30_INVALID</date>\r\n" + // <- error
				"  <number>0</number>\r\n" + //
				"  <products>\r\n" + //
				"  	<product price=\"1\" description=\"\"/>\r\n" + //
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		testDiagnosticsFor(xml, d(3, 8, 3, 26, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(3, 8, 3, 26, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_enumeration_validOnAttribute() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number>0</number>\r\n" + //
				"  <products>\r\n" + //
				"  	<product price=\"1\" description=\"\"/>\r\n" + //
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit_invalid\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		testDiagnosticsFor(xml, d(9, 30, 9, 46, XMLSchemaErrorCode.cvc_enumeration_valid),
				d(9, 30, 9, 46, XMLSchemaErrorCode.cvc_attribute_3));
	}

	@Test
	public void cvc_enumeration_validOnText() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"	<member\r\n" + //
				"	       name=\"John\"\r\n" + //
				"	       badgeNumber=\"1\"\r\n" + //
				"	       role=\"architect\">\r\n" + //
				"		<skills>\r\n" + //
				"			<skill>XXXXX</skill>\r\n" + // <- error
				"		</skills> \r\n" + //
				"		<focus>\r\n" + //
				"			<server\r\n" + //
				"			       language=\"Java\" />\r\n" + //
				"		</focus>\r\n" + //
				"	</member>\r\n" + //
				"</team>";
		Diagnostic d = d(10, 10, 10, 15, XMLSchemaErrorCode.cvc_enumeration_valid);
		testDiagnosticsFor(xml, d, d(10, 10, 10, 15, XMLSchemaErrorCode.cvc_type_3_1_3));
		testCodeActionsFor(xml, d, ca(d, te(10, 10, 10, 15, "Java")), ca(d, te(10, 10, 10, 15, "Node")),
				ca(d, te(10, 10, 10, 15, "XML")));
	}

	@Test
	public void cvc_datatype_valid_1_2_1OnAttributeValue() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number>5235</number> \r\n" + //
				"  <products> \r\n" + //
				"    <product description=\"laptop\" price=\"700.00_INVALID\"/>  \r\n" + // <- error
				"    <product description=\"mouse\" price=\"30.00\" />\r\n" + //
				"  </products> \r\n" + //
				"  <payments>\r\n" + //
				"    <payment amount=\"770.00\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice> ";
		testDiagnosticsFor(xml, d(6, 40, 6, 56, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(6, 40, 6, 56, XMLSchemaErrorCode.cvc_attribute_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_1OnText() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
				+ "<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ " xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number>5235_INVALID</number> \r\n" + // <- error
				"  <products> \r\n" + //
				"    <product description=\"laptop\" price=\"700.00\"/>  \r\n" + //
				"    <product description=\"mouse\" price=\"30.00\" />\r\n" + //
				"  </products> \r\n" + //
				"  <payments>\r\n" + //
				"    <payment amount=\"770.00\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice> ";
		testDiagnosticsFor(xml, d(4, 10, 4, 22, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(4, 10, 4, 22, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_1_TextOnlyWithWhitespace() throws Exception {
		String xml = "<a xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
				+ "    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/integerElement.xsd\">\r\n" + "\r\n"
				+ "    TEXT\r\n" + "\r\n" + "</a>";
		testDiagnosticsFor(xml, d(3, 4, 3, 8, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(3, 4, 3, 8, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_1_OneElement() throws Exception {
		String xml = "<a xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
				+ "    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/integerElement.xsd\">\r\n" + "\r\n"
				+ "    <b></b>\r\n" + "\r\n" + "</a>";

		testDiagnosticsFor(xml, d(0, 1, 0, 2, XMLSchemaErrorCode.cvc_type_3_1_2),
				d(3, 4, 3, 11, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(3, 4, 3, 11, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_1_TwoElements() throws Exception {
		String xml = "<a xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
				+ "    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/integerElement.xsd\">\r\n" + "\r\n"
				+ "    <b></b>\r\n" + "    <c></c>\r\n" + "\r\n" + "</a>";

		testDiagnosticsFor(xml, d(0, 1, 0, 2, XMLSchemaErrorCode.cvc_type_3_1_2),
				d(3, 4, 3, 11, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(3, 4, 3, 11, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_3OnText() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<dresssize xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/dressSize.xsd\">\r\n" + //
				"               XXX           \r\n" + // <-- error
				"</dresssize>  ";
		testDiagnosticsFor(xml, d(3, 15, 3, 18, XMLSchemaErrorCode.cvc_datatype_valid_1_2_3),
				d(3, 15, 3, 18, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_datatype_valid_1_2_3Empty() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<dresssize xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/dressSize.xsd\"></dresssize>";
		testDiagnosticsFor(xml, //
				d(1, 1, 1, 10, XMLSchemaErrorCode.cvc_datatype_valid_1_2_3),
				d(1, 1, 1, 10, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void cvc_minLength() throws Exception {
		// See issue https://github.com/redhat-developer/vscode-xml/issues/524
		String xml = "<?xml version=\"1.0\"?>\r\n"
				+ "<?xml-model href=\"src/test/resources/xsd/minLength.xsd\"?>\r\n"
				+ "<Root>\r\n"
				+ "    <Test Some_String=\"\" Some_Restricted_Value=\"\"/>\r\n"
				+ "</Root>";
		testDiagnosticsFor(xml, //
				d(3, 47, 3, 49, XMLSchemaErrorCode.cvc_minlength_valid),
				d(3, 47, 3, 49, XMLSchemaErrorCode.cvc_attribute_3));
	}

	@Test
	public void cvc_maxLength_validOnAttribute() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"	<member\r\n" + //
				"	       name=\"John\"\r\n" + //
				"	       badgeNumber=\"1\"\r\n" + //
				"	       role=\"architect\">\r\n" + //
				"		<skills>\r\n" + //
				"			<skill>Java</skill>\r\n" + "		</skills> \r\n" + //
				"		<focus>\r\n" + //
				"			<server\r\n" + //
				"			       language=\"Java\" />\r\n" + //
				"		</focus>\r\n" + //
				"	</member>\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, //
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_maxlength_valid),
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_attribute_3));

	}

	@Test
	public void cvc_complex_type_2_1() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans\r\n" + //
				"      xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"      xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\r\n"
				+ //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + //
				"	<alias name=\"\" alias=\"\" >XXXX</alias>\r\n" + // <- error
				"</beans>";
		Diagnostic d = d(5, 26, 5, 30, XMLSchemaErrorCode.cvc_complex_type_2_1);
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(5, 25, 5, 38, "/>")));
	}

	@Test
	public void cvc_complex_type_2_1_SelfClosing() throws Exception {
		String xml = "<money xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/money.xsd\" currency=\"euros\"> </money>";

		Diagnostic d = d(0, 143, 0, 144, XMLSchemaErrorCode.cvc_complex_type_2_1);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 142, 0, 152, "/>")));
	}

	@Test
	public void cvc_complex_type_2_1WithLinefeed() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<beans\r\n" + //
				"      xmlns=\"http://www.springframework.org/schema/beans\"\r\n" + //
				"      xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"\r\n"
				+ //
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + //
				"	<alias name=\"\" alias=\"\" >\r\n   \r\n</alias>\r\n" + // <- error
				"</beans>";
		Diagnostic d = d(5, 26, 7, 0, XMLSchemaErrorCode.cvc_complex_type_2_1);
		testDiagnosticsWithCatalogFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(5, 25, 7, 8, "/>")));
	}

	@Test
	public void cvc_pattern_valid() throws Exception {
		String xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/pattern.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		Diagnostic patternValid = d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_pattern_valid);
		Diagnostic cvcAttribute3 = d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_attribute_3);
		testDiagnosticsFor(xml, patternValid, cvcAttribute3);
	}

	@Test
	public void cvc_pattern_valid_With_Buffer() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\r\n"
				+ "<cpr xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
				+ "     xsi:noNamespaceSchemaLocation=\"https://www.dgai.de/cpr/schema/ev/cpr-ev-2.0.xsd\">\r\n"
				+ "    <cprev>\r\n" + "        <VERSION>2.0</VERSION>\r\n" + "        <DATUM>2019-08-09</DATUM>\r\n"
				+ "        <STOKENN>FIX_ERROR_RANGE_HERE</STOKENN>\r\n" + // <-- Error should follow pattern [0-9]{8}
				"    </cprev>\r\n" + "</cpr>";
		Diagnostic patternValid = d(6, 17, 6, 37, XMLSchemaErrorCode.cvc_pattern_valid);
		Diagnostic cvcType313 = d(6, 17, 6, 37, XMLSchemaErrorCode.cvc_type_3_1_3);
		Diagnostic cvcType24b = d(3, 5, 3, 10, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsWithCatalogFor(xml, patternValid, cvcType313, cvcType24b);
	}

	/**
	 * @see https://github.com/eclipse/lemminx/issues/217
	 */
	@Test
	public void issue217() {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<edmx:Edmx xmlns:edmx=\"http://docs.oasis-open.org/odata/ns/edmx\" xmlns=\"http://docs.oasis-open.org/odata/ns/edm\" Version=\"4.0\">\r\n"
				+ //
				"  \r\n" + //
				"</edmx:Edmx>";
		Diagnostic d = d(1, 1, 1, 10, XMLSchemaErrorCode.cvc_complex_type_2_4_b,
				"Child elements are missing from element 'edmx:Edmx'.\n\nThe following elements are expected:\n - Reference\n - DataServices\n\nError indicated by:\n {http://docs.oasis-open.org/odata/ns/edmx\":Reference, \"http://docs.oasis-open.org/odata/ns/edmx}\nwith code:");
		testDiagnosticsWithCatalogFor(xml, d);
	}

	@Test
	public void cvc_type_3_2_1() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date xsi:nil=\"true\">2017-11-30</date>\r\n" + // <- error
				"  <number>0</number>\r\n" + //
				"  <products>\r\n" + //
				"  	<product price=\"1\" description=\"\"/>\r\n" + //
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		testDiagnosticsFor(xml, d(3, 23, 3, 33, XMLSchemaErrorCode.cvc_elt_3_2_1));
	}

	@Test
	public void cvc_type_3_1_2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/invoice.xsd\">\r\n" + //
				"  <date>2017-11-30</date>\r\n" + //
				"  <number><a></a></number>\r\n" + // <- error
				"  <products>\r\n" + //
				"  	<product price=\"1\" description=\"\"/>\r\n" + //
				"  </products>\r\n" + //
				"  <payments>\r\n" + //
				"  	<payment amount=\"1\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice>";
		testDiagnosticsFor(xml, //
				d(4, 3, 4, 9, XMLSchemaErrorCode.cvc_type_3_1_2),
				d(4, 10, 4, 17, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1),
				d(4, 10, 4, 17, XMLSchemaErrorCode.cvc_type_3_1_3));
	}

	@Test
	public void testSrcElement3() throws Exception {
		String xml = "<a xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/srcElement3.xsd\">\r\n" + "	<b></b>\r\n"
				+ "</a>";
		testDiagnosticsFor(xml, d(0, 1, 0, 2, XMLSchemaErrorCode.src_element_3));
	}

	@Test
	public void schema_reference_4_withSchemaLocation() {
		String xml = "<IODevice xmlns=\"http://www.io-link.com/IODD/2010/10\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
				+ //
				"  xsi:schemaLocation=\"http://www.io-link.com/IODD/2010/10 IODD1.1.xsd\">\r\n" + //
				"	</IODevice>";
		testDiagnosticsWithCatalogFor(xml, //
				d(1, 58, 1, 69, XMLSchemaErrorCode.schema_reference_4), //
				d(0, 1, 0, 9, XMLSchemaErrorCode.cvc_elt_1_a));
	}

	@Test
	public void schema_reference_4_schemaLocationMultipleOneWrong() {
		String xml = "<root:root\n" + //
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"xmlns:root=\"http://example.org/schema/root\"\n" + //
				"xmlns:other=\"http://example.org/schema/other\"\n" + //
				"xsi:schemaLocation=\"http://example.org/schema/root root.xsd http://example.org/schema/other other.xsd\">\n"
				+ //
				"<other:other />\n" + //
				"</root:root>";
		testDiagnosticsWithCatalogFor(xml, //
				d(4, 92, 4, 101, XMLSchemaErrorCode.schema_reference_4), //
				d(5, 1, 5, 12, XMLSchemaErrorCode.cvc_complex_type_2_4_c));
	}

	@Test
	public void schema_reference_4_schemaLocationMultipleBothWrong() {
		String xml = "<root:root\n" + //
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"xmlns:root=\"http://example.org/schema/robot\"\n" + //
				"xmlns:other=\"http://example.org/schema/other\"\n" + //
				"xsi:schemaLocation=\"http://example.org/schema/robot robot.xsd http://example.org/schema/other other.xsd\">\n"
				+ //
				"<other:other />\n" + //
				"</root:root>";
		testDiagnosticsFor(xml, //
				d(4, 52, 4, 61, XMLSchemaErrorCode.schema_reference_4), //
				d(0, 1, 0, 10, XMLSchemaErrorCode.cvc_elt_1_a), //
				d(4, 94, 4, 103, XMLSchemaErrorCode.schema_reference_4));
	}

	@Test
	public void fuzzyElementNameCodeActionTest() throws Exception {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" \r\n"
				+ "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ "    <modules>\r\n" + //
				"      <bodule></bodule>\r\n" + // should be 'module'
				"    </modules>\r\n" + //
				"</project>";
		Diagnostic diagnostic = d(4, 7, 4, 13, XMLSchemaErrorCode.cvc_complex_type_2_4_a,
				"Element name 'bodule' is invalid.\n\nOne of the following is expected:\n - module\n\nError indicated by:\n {http://maven.apache.org/POM/4.0.0}\nwith code:");
		testDiagnosticsWithCatalogFor(xml, diagnostic);

		testCodeActionsWithCatalogFor(xml, diagnostic,
				ca(diagnostic, te(4, 7, 4, 13, "module"), te(4, 16, 4, 22, "module")));
	}

	private void testCodeActionsWithCatalogFor(String xml, Diagnostic diagnostic, CodeAction... expected)
			throws BadLocationException {
		XMLAssert.testCodeActionsFor(xml, diagnostic, "src/test/resources/catalogs/catalog.xml", expected);
	}

	@Test
	public void fuzzyElementNamesWithOtherOptionsCodeActionTest() throws Exception {
		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" \r\n" + //
				"         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"    <ciManagement>\r\n" + //
				"      <XXXXXXXXX></XXXXXXXXX>\r\n" + // does not fuzzy match any, so provide code action for all
														// possible
				"    </ciManagement>\r\n" + //
				"</project>";
		Diagnostic diagnostic = d(4, 7, 4, 16, XMLSchemaErrorCode.cvc_complex_type_2_4_a, //
				"Element name 'XXXXXXXXX' is invalid.\n\n" + //
						"One of the following is expected:\n" + //
						" - system\n" + //
						" - url\n" + //
						" - notifiers\n\n" + "Error indicated by:\n" + //
						" {http://maven.apache.org/POM/4.0.0}\n" + //
						"with code:");
		testDiagnosticsWithCatalogFor(xml, diagnostic);
		testCodeActionsWithCatalogFor(xml, diagnostic, //
				ca(diagnostic, te(4, 7, 4, 16, "notifiers"), te(4, 19, 4, 28, "notifiers")), //
				ca(diagnostic, te(4, 7, 4, 16, "system"), te(4, 19, 4, 28, "system")), //
				ca(diagnostic, te(4, 7, 4, 16, "url"), te(4, 19, 4, 28, "url")));
	}

	/**
	 *
	 * @throws Exception
	 * @see https://github.com/eclipse/lemminx/issues/856
	 */
	@Test
	public void elementFormDefaultUnqualified() throws Exception {
		String xml = "<foo xmlns=\"http://foo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://foo foo-unqualified.xsd\">\r\n"
				+ //
				"	<bar />\r\n" + //
				"</foo>";
		Diagnostic diagnostic = d(1, 2, 1, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_a, //
				"Element name 'bar' is invalid.\n\n" + //
						"One of the following is expected:\n" + //
						" - bar\n\n" + //
						"Error indicated by:\n" + //
						" {http://foo}\n" + //
						"with code:");
		testDiagnosticsWithCatalogFor(xml, diagnostic);
	}

	@Test
	public void fuzzyElementNamesWithPrefix() throws Exception {
		String xml = "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n" + //
				"        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"        xmlns:camel=\"http://camel.apache.org/schema/spring\"\n" + //
				"        xsi:schemaLocation=\"\n" + //
				"           http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd\n"
				+ //
				"           http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd\"> \n"
				+ //
				"    <camel:beani></camel:beani>\n" + //
				"</beans>";
		Diagnostic diagnostic = d(6, 5, 6, 16, XMLSchemaErrorCode.cvc_complex_type_2_4_c,
				"cvc-complex-type.2.4.c: The matching wildcard is strict, but no declaration can be found for element 'camel:beani'.");
		testDiagnosticsWithCatalogFor(xml, diagnostic);
		testCodeActionsWithCatalogFor(xml, diagnostic, //
				ca(diagnostic, te(6, 11, 6, 16, "bean"), te(6, 25, 6, 30, "bean")), //
				ca(diagnostic, te(6, 11, 6, 16, "beanio"), te(6, 25, 6, 30, "beanio")));
	}

	@Test
	public void fuzzyElementNamesWithPrefixAndNoMatch() throws Exception {
		String xml = "<schemaB:BRootElement \n" + "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
				+ "      xsi:schemaLocation=\"http://schemaA src/test/resources/xsd/fuzzyCodeAction/FuzzySchemaA.xsd http://schemaB src/test/resources/xsd/fuzzyCodeAction/FuzzySchemaB.xsd\" \n"
				+ "      xmlns:schemaA=\"http://schemaA\" \n" + "      xmlns:schemaB=\"http://schemaB\">\n"
				+ "   <schemaA:XXXXX></schemaA:XXXXX>\n" + "</schemaB:BRootElement>";
		Diagnostic diagnostic = d(5, 4, 5, 17, XMLSchemaErrorCode.cvc_complex_type_2_4_c,
				"cvc-complex-type.2.4.c: The matching wildcard is strict, but no declaration can be found for element 'schemaA:XXXXX'.");
		testDiagnosticsFor(xml, diagnostic);
		testCodeActionsFor(xml, diagnostic,
				ca(diagnostic, te(5, 12, 5, 17, "AElement1"), te(5, 28, 5, 33, "AElement1")),
				ca(diagnostic, te(5, 12, 5, 17, "AElement2"), te(5, 28, 5, 33, "AElement2")));
	}

	@Test
	public void fuzzyElementMemberValueCodeActionTest() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<dress \r\n" + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ "xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/dressSize.xsd\"\r\n" + "size=\"larg\"/>";
		Diagnostic diagnostic1 = d(4, 5, 4, 11, XMLSchemaErrorCode.cvc_attribute_3,
				"cvc-attribute.3: The value 'larg' of attribute 'size' on element 'dress' is not valid with respect to its type, 'SizeType'.");
		testCodeActionsFor(xml, diagnostic1, ca(diagnostic1, te(4, 6, 4, 10, "large")),
				ca(diagnostic1, te(4, 6, 4, 10, "x-large")));
	}

	@Test
	public void cvc_complex_type_2_2_withElement() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<int xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://integer\" xsi:schemaLocation=\"http://integer src/test/resources/xsd/namedInteger.xsd\">\n"
				+ //
				"    <int>42</int>\n" + //
				"</int>";
		Diagnostic diagnostic = d(1, 1, 1, 4, XMLSchemaErrorCode.cvc_complex_type_2_2,
				"cvc-complex-type.2.2: Element 'int' must have no element [children], and the value must be valid.");
		testDiagnosticsWithCatalogFor(xml, diagnostic);
	}

	@Test
	public void cvc_complex_type_2_2_withText() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<int xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://integer\" xsi:schemaLocation=\"http://integer src/test/resources/xsd/namedInteger.xsd\">\n"
				+ //
				"    Bob</int>";
		Diagnostic diagnosticBob = d(2, 4, 2, 7, XMLSchemaErrorCode.cvc_datatype_valid_1_2_1,
				"Content of type 'integer' is expected.\n\nThe following content is not a valid type:\n 'Bob'\n\nCode:");
		Diagnostic diagnostic_cvc_2_2 = d(1, 1, 1, 4, XMLSchemaErrorCode.cvc_complex_type_2_2,
				"cvc-complex-type.2.2: Element 'int' must have no element [children], and the value must be valid.");
		testDiagnosticsWithCatalogFor(xml, diagnosticBob, diagnostic_cvc_2_2);
	}

	@Test
	public void DuplicateUnique_InText() throws Exception {
		String xml = "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\r\n"
				+ //
				"    version=\"2.4\">\r\n" + //
				"    <servlet>\r\n" + //
				"    <servlet-name>dispatcher</servlet-name>\r\n" + //
				"    <servlet-class></servlet-class>\r\n" + //
				"    </servlet>\r\n" + //
				"    <servlet>\r\n" + //
				"    <servlet-name>dispatcher</servlet-name>\r\n" + //
				"    <servlet-class></servlet-class>\r\n" + //
				"    </servlet>\r\n" + //
				"</web-app>";
		Diagnostic diagnostic = d(9, 18, 9, 28, XMLSchemaErrorCode.DuplicateUnique);
		testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog-web-app.xml", diagnostic);
	}

	@Test
	public void DuplicateUnique_InAttribute() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</root>";
		Diagnostic diagnostic = d(5, 22, 5, 27, XMLSchemaErrorCode.DuplicateUnique);
		testDiagnosticsFor(xml, diagnostic);
	}

	@Test
	public void DuplicateUnique_MultiAttribute() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\" age = \"20\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"    <authors status = \"new\" age = \"20\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</root>";
		Diagnostic diagnostic1 = d(5, 22, 5, 27, XMLSchemaErrorCode.DuplicateUnique);
		Diagnostic diagnostic2 = d(5, 34, 5, 38, XMLSchemaErrorCode.DuplicateUnique);

		testDiagnosticsFor(xml, diagnostic1, diagnostic2);
	}

	@Test
	public void DuplicateUnique_InBoth() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</root>";
		Diagnostic diagnosticText = d(4, 12, 4, 17, XMLSchemaErrorCode.DuplicateUnique);
		Diagnostic diagnosticAttr = d(6, 22, 6, 27, XMLSchemaErrorCode.DuplicateUnique);
		testDiagnosticsFor(xml, diagnosticText, diagnosticAttr);
	}

	@Test
	public void DuplicateUnique_FalseNeg() throws Exception {
		String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    <author>john</author>\r\n" + //
				"    </authors>\r\n" + //
				"    <authors status = \"old\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</root>";
		testDiagnosticsFor(xml);
	}

	@Test
	public void testTargetNamespace_1Normal() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + //
				"<?xml-model href=\"src/test/resources/xsd/two-letter-name.xsd\"?>\n" + //
				"<two-letter-name xmlns=\"BAD_NS\">Io</two-letter-name>";
		Diagnostic targetNamespace = d(2, 23, 2, 31, XMLSchemaErrorCode.TargetNamespace_1,
				"TargetNamespace.1: Expecting namespace 'BAD_NS', but the target namespace of the schema document is 'http://two-letter-name'.");
		testDiagnosticsWithCatalogFor(xml, targetNamespace, d(2, 1, 2, 16, XMLSchemaErrorCode.cvc_elt_1_a,
				"cvc-elt.1.a: Cannot find the declaration of element 'two-letter-name'."));
		testCodeActionsFor(xml, targetNamespace, ca(targetNamespace, te(2, 23, 2, 31, "\"http://two-letter-name\"")));
	}

	@Test
	public void testTargetNamespace_1ShortNS() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + //
				"<?xml-model href=\"src/test/resources/xsd/two-letter-name.xsd\"?>\n" + //
				"<two-letter-name xmlns=\"_\">Io</two-letter-name>";
		Diagnostic targetNamespace = d(2, 23, 2, 26, XMLSchemaErrorCode.TargetNamespace_1,
				"TargetNamespace.1: Expecting namespace '_', but the target namespace of the schema document is 'http://two-letter-name'.");
		testDiagnosticsWithCatalogFor(xml, targetNamespace, d(2, 1, 2, 16, XMLSchemaErrorCode.cvc_elt_1_a,
				"cvc-elt.1.a: Cannot find the declaration of element 'two-letter-name'."));
		testCodeActionsFor(xml, targetNamespace, ca(targetNamespace, te(2, 23, 2, 26, "\"http://two-letter-name\"")));
	}

	@Test
	public void testTargetNamespace_1SingleQuotes() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + //
				"<?xml-model href=\"src/test/resources/xsd/two-letter-name.xsd\"?>\n" + //
				"<two-letter-name xmlns=\"_\">Io</two-letter-name>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		Diagnostic targetNamespace = d(2, 23, 2, 26, XMLSchemaErrorCode.TargetNamespace_1,
				"TargetNamespace.1: Expecting namespace '_', but the target namespace of the schema document is 'http://two-letter-name'.");
		testCodeActionsFor(xml, targetNamespace, settings,
				ca(targetNamespace, te(2, 23, 2, 26, "'http://two-letter-name'")));
	}

	@Test
	public void testTargetNamespace_2() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + //
				"<?xml-model href=\"src/test/resources/xsd/two-letter-name.xsd\"?>\n" + //
				"<two-letter-name>Io</two-letter-name>";
		Diagnostic targetNamespace = d(2, 1, 2, 16, XMLSchemaErrorCode.TargetNamespace_2,
				"TargetNamespace.2: Expecting no namespace, but the schema document has a target namespace of 'http://two-letter-name'.");
		testDiagnosticsWithCatalogFor(xml, targetNamespace, d(2, 1, 2, 16, XMLSchemaErrorCode.cvc_elt_1_a,
				"cvc-elt.1.a: Cannot find the declaration of element 'two-letter-name'."));
		testCodeActionsFor(xml, targetNamespace,
				ca(targetNamespace, te(2, 16, 2, 16, " xmlns=\"http://two-letter-name\"")));
	}

	@Test
	public void testTargetNamespace_2SingleQuotes() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + //
				"<?xml-model href=\"src/test/resources/xsd/two-letter-name.xsd\"?>\n" + //
				"<two-letter-name>Io</two-letter-name>";
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setEnforceQuoteStyle(EnforceQuoteStyle.preferred);
		settings.getPreferences().setQuoteStyle(QuoteStyle.singleQuotes);
		Diagnostic targetNamespace = d(2, 1, 2, 16, XMLSchemaErrorCode.TargetNamespace_2,
				"TargetNamespace.2: Expecting no namespace, but the schema document has a target namespace of 'http://two-letter-name'.");
		testCodeActionsFor(xml, targetNamespace, settings,
				ca(targetNamespace, te(2, 16, 2, 16, " xmlns='http://two-letter-name'")));
	}

	@Test
	public void localSchemaFileMissingCodeAction() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"/sala'd.xsd\">\n" + //
				"</invoice>";
		Diagnostic missingSchemaDiagnostic = d(2, 33, 44, XMLSchemaErrorCode.schema_reference_4);
		missingSchemaDiagnostic
				.setMessage("schema_reference.4: Failed to read schema document " + "'file:///sala'd.xsd',"
						+ " because 1) could not find the document; 2) the document could not be read;"
						+ " 3) the root element of the document is not <xsd:schema>.");
		Diagnostic eltDiagnostic = d(1, 1, 8, XMLSchemaErrorCode.cvc_elt_1_a);
		eltDiagnostic.setMessage("cvc-elt.1.a: Cannot find the declaration of element 'invoice'.");
		testDiagnosticsFor(xml, missingSchemaDiagnostic, eltDiagnostic);

		SharedSettings settings = new SharedSettings();
		WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
		WorkspaceEditCapabilities workspaceEdit = new WorkspaceEditCapabilities();
		workspaceEdit.setResourceOperations(Arrays.asList(ResourceOperationKind.Create));
		workspace.setWorkspaceEdit(workspaceEdit);
		settings.getWorkspaceSettings().setCapabilities(workspace);

		XMLAssert.testCodeActionsFor(xml, //
				missingSchemaDiagnostic, //
				settings, //
				ca(missingSchemaDiagnostic, //
						createFile("file:///sala'd.xsd", false), //
						teOp("file:///sala'd.xsd", 0, 0, 0, 0, //
								"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator() + //
										"<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">" + lineSeparator() + //
										"  <xs:element name=\"invoice\" type=\"xs:string\" />" + lineSeparator() + //
										"</xs:schema>")));
	}

	@Test
	public void localSchemaFileMissingCodeActionNotSupported() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"/salad.xsd\">\n" + //
				"</invoice>";
		Diagnostic missingSchema = d(2, 33, 43, XMLSchemaErrorCode.schema_reference_4);
		missingSchema.setMessage("schema_reference.4: Failed to read schema document " + "'file:///salad.xsd',"
				+ " because 1) could not find the document; 2) the document could not be read;"
				+ " 3) the root element of the document is not <xsd:schema>.");
		Diagnostic eltDiagnostic = d(1, 1, 8, XMLSchemaErrorCode.cvc_elt_1_a);
		eltDiagnostic.setMessage("cvc-elt.1.a: Cannot find the declaration of element 'invoice'.");
		testDiagnosticsFor(xml, missingSchema, eltDiagnostic);

		XMLAssert.testCodeActionsFor(xml, missingSchema);
	}

	@Test
	public void cvc_complex_type_2_3_singleLine() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar>/bar></bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 7, 12, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 7, 3, 12, "")));
	}

	@Test
	public void cvc_complex_type_2_3_multiLine() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar>/bar>\n" + //
				"barbarbar</bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 7, 12, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 7, 3, 12, "")));
	}

	@Test
	public void cvc_complex_type_2_3_singleLineSpaces() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar>    	/bar> 	 	</bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 12, 17, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 12, 3, 17, "")));
	}

	@Test
	public void cvc_complex_type_2_3_singleLineCData() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar> <![CDATA[ bar ]]> </bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 18, 21, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 18, 3, 21, "")));
	}

	@Test
	public void cvc_complex_type_2_3_multiLineCData() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar> <![CDATA[ bar\n" + //
				"   hi ]]> </bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 18, 21, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 18, 3, 21, "")));
	}

	@Test
	public void cvc_complex_type_2_3_blankCData() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar> <![CDATA[  ]]> </bar>\n" + //
				"</foo>";
		testDiagnosticsFor(xml);
	}

	@Test
	public void cvc_complex_type_2_3_blankCDataWithTextAfter() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar> <![CDATA[  ]]> TextContent </bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 23, 34, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 23, 3, 34, "")));
	}

	@Test
	public void cvc_complex_type_2_3_elementBeforeText() throws BadLocationException {
		String xml = "<foo\n" + //
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + //
				"  xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/close-tag-type.xsd\">\n" + //
				"  <bar /> TextContent <bar></bar>\n" + //
				"</foo>";
		Diagnostic diagnostic = d(3, 10, 21, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(3, 10, 3, 21, "")));
	}

	@Test
	public void cvc_complex_type_2_3_textAfterComment() throws BadLocationException {
		// See https://github.com/eclipse/lemminx/issues/1495
		String xml = "<?xml version='1.0' encoding='utf-8'?>\r\n"
				+ "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n"
				+ "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
				+ "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ "    <modelVersion>4.0.0</modelVersion>\r\n"
				+ "    <!-- Comment -->\r\n"
				+ "    abcd efgh\r\n"
				+ "     \r\n"
				+ "</project>";
		Diagnostic diagnostic = d(6, 4, 8, XMLSchemaErrorCode.cvc_complex_type_2_3);
		testDiagnosticsWithCatalogFor(xml, diagnostic);
		XMLAssert.testCodeActionsFor(xml, diagnostic, ca(diagnostic, te(6, 4, 6, 8, "")));
	}

	@Test
	public void diagnosticRelatedInformationWithXMLModelSchemaProblem() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<?xml-model href=\"xsd/foo-invalid-schema.xsd\" ?>\r\n" + //
				"<foo>\r\n" + //
				"	<barX></barX> \r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(0, 17, 0, 45), "There is '1' error in 'foo-invalid-schema.xsd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String xsdFileURI = getGrammarFileURI("foo-invalid-schema.xsd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(xsdFileURI, r(1, 71, 1, 73)), ""));

		Diagnostic diagnosticBasedOnXSD = new Diagnostic(r(2, 2, 2, 6),
				"Element name 'barX' is invalid.\n\nOne of the following is expected:\n - bar\n\nError indicated by:\n {the schema}\nwith code:",
				DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_complex_type_2_4_a.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid XSD grammar
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
	}

	@Test
	public void diagnosticRelatedInformationWithNoNamespaceSchemaLocationSchemaProblem() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"	 xsi:noNamespaceSchemaLocation=\"xsd/foo-invalid-schema.xsd\">\r\n" + //
				"	<barX />\r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(1, 32, 1, 60), "There is '1' error in 'foo-invalid-schema.xsd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String xsdFileURI = getGrammarFileURI("foo-invalid-schema.xsd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(xsdFileURI, r(1, 71, 1, 73)), ""));

		Diagnostic diagnosticBasedOnXSD = new Diagnostic(r(2, 2, 2, 6),
				"Element name 'barX' is invalid.\n\nOne of the following is expected:\n - bar\n\nError indicated by:\n {the schema}\nwith code:",
				DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_complex_type_2_4_a.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid XSD grammar
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
	}

	@Test
	public void diagnosticRelatedInformationWithNoNamespaceSchemaLocationSyntaxProblem() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"	 xsi:noNamespaceSchemaLocation=\"xsd/foo-invalid-syntax.xsd\">\r\n" + //
				"	<bar />\r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(1, 32, 1, 60), "There is '1' error in 'foo-invalid-syntax.xsd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String xsdFileURI = getGrammarFileURI("foo-invalid-syntax.xsd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(xsdFileURI, r(1, 1, 1, 54)), ""));

		Diagnostic diagnosticBasedOnXSD = new Diagnostic(r(0, 1, 0, 4),
				"cvc-elt.1.a: Cannot find the declaration of element 'foo'.", DiagnosticSeverity.Error, "xml",
				XMLSchemaErrorCode.cvc_elt_1_a.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid XSD grammar
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD);
	}

	@Test
	public void diagnosticRelatedInformationWithSchemaLocationSyntaxProblem() throws Exception {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(true);
		XMLValidationRootSettings validationSettings = new XMLValidationRootSettings();
		validationSettings.setCapabilities(new PublishDiagnosticsCapabilities(true)); // with related information
		settings.setValidation(validationSettings);

		String xml = "<foo\r\n" + //
				"		xmlns=\"http://foo\"\r\n" + //
				"		xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"		xsi:schemaLocation=\"http://foo xsd/foo-ns-invalid-syntax.xsd\">\r\n" + //
				"	<bar />\r\n" + //
				"</foo>";
		Diagnostic diagnostic = new Diagnostic(r(3, 33, 3, 62), "There is '1' error in 'foo-ns-invalid-syntax.xsd'.",
				DiagnosticSeverity.Error, "xml");
		diagnostic.setRelatedInformation(new ArrayList<>());
		String xsdFileURI = getGrammarFileURI("foo-ns-invalid-syntax.xsd");
		diagnostic.getRelatedInformation().add(new DiagnosticRelatedInformation(l(xsdFileURI, r(1, 1, 4, 29)), ""));

		Diagnostic diagnosticBasedOnXSD1 = new Diagnostic(r(1, 8, 1, 20),
				"TargetNamespace.1: Expecting namespace 'http://foo', but the target namespace of the schema document is 'xs:element name=\"foo\">'.",
				DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.TargetNamespace_1.getCode());

		Diagnostic diagnosticBasedOnXSD2 = new Diagnostic(r(0, 1, 0, 4),
				"cvc-elt.1.a: Cannot find the declaration of element 'foo'.", DiagnosticSeverity.Error, "xml",
				XMLSchemaErrorCode.cvc_elt_1_a.getCode());

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		// First validation
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD1, diagnosticBasedOnXSD2);
		// Restart the validation to check the validation is working since Xerces cache
		// the invalid XSD grammar
		testDiagnosticsFor(xmlLanguageService, xml, null, null, "src/test/resources/test.xml", false, settings, //
				diagnostic, diagnosticBasedOnXSD1, diagnosticBasedOnXSD2);
	}

	@Test
	public void diagnosticsWithCatalogAndXSDInclude() throws BadLocationException {
		String xml = "<document xmlns=\"http://foobar.com/test\">\r\n" + //
				"	<page></page>\r\n" + //
				"</document>";
		Diagnostic diagnostic = d(1, 2, 1, 6, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, "src/test/resources/catalogs/include/catalog-include.xml", diagnostic);
	}

	@Test
	public void noHintSchemaLocationForRootElement() {
		// Here the xsi:schemaLocation doens't declare the hint for
		// http://www.eclipse.org/oomph/setup/1.0 (used in the root element)
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<setup:Configuration \r\n" + //
				"    xmi:version=\"2.0\"\r\n" + //
				"    xmlns:xmi=\"http://www.omg.org/XMI\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
				"    xmlns:setup=\"http://www.eclipse.org/oomph/setup/1.0\"\r\n" + //
				"    xmlns:setup.p2=\"http://www.eclipse.org/oomph/setup/p2/1.0\"\r\n" + //
				"    xmlns:workbench=\"http://www.eclipse.org/oomph/setup/workbench/1.0\"\r\n" + //
				"    xsi:schemaLocation=\"http://www.eclipse.org/oomph/setup/workbench/1.0 http://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/models/Workbench.ecore\"\r\n"
				+ //
				"    label=\"Gael Eclipse Installation\"> \r\n" + //
				"  <installation name=\"com.github.glhez.eclipse.install\" label=\"Gael Eclipse Installation Installation\">\r\n"
				+ //
				"    <setupTask xsi:type=\"setup.p2:P2Task\" label=\"Oomph Setup Task\">\r\n" + //
				"      <requirement name=\"org.eclipse.oomph.setup.feature.group\"/>\r\n" + //
				"      <repository url=\"${oomph.update.url}\"/>\r\n" + //
				"    </setupTask>  \r\n" + //
				"   </installation>\r\n" + //
				"</setup:Configuration>";

		// always
		testDiagnosticsFor(xml,
				d(1, 1, 1, 20, XMLSchemaErrorCode.cvc_elt_1_a,
						"cvc-elt.1.a: Cannot find the declaration of element 'setup:Configuration'."), //
				d(11, 67, 11, 67, XMLSchemaErrorCode.cvc_elt_4_2,
						"cvc-elt.4.2: Cannot resolve 'setup.p2:P2Task' to a type definition for element 'setupTask'."));

		// on schema valid
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.onValidSchema);
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void noNamespaceSchemaLocationEnabledWithAlways() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.always);

		// good XSD location
		String xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/pattern.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_pattern_valid), //
				d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_attribute_3));

		// bad XSD location
		xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"BAD_LOCATION.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(2, 32, 2, 48, XMLSchemaErrorCode.schema_reference_4), //
				d(0, 1, 0, 11, XMLSchemaErrorCode.cvc_elt_1_a));
	}

	@Test
	public void noNamespaceSchemaLocationEnabledWithNever() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.never);

		// good XSD location
		String xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/pattern.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// bad XSD location
		xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"BAD_LOCATION.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void noNamespaceSchemaLocationEnabledWithOnValidSchema() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.onValidSchema);

		// good XSD location
		String xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/pattern.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_pattern_valid), //
				d(3, 6, 3, 9, XMLSchemaErrorCode.cvc_attribute_3));

		// bad XSD location
		xml = "<Annotation\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"BAD_LOCATION.xsd\"\r\n" + //
				"	Term=\"X\"></Annotation>";
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void schemaLocationEnabledWithAlways() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.always);

		// good XSD location and namespace
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_maxlength_valid), //
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_attribute_3),
				d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b));

		// bad XSD namespace
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"BAD_NAMESPACE\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a));

		// bad XSD location
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace BAD_LOCATION.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(5, 40, 5, 56, XMLSchemaErrorCode.schema_reference_4), //
				d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a));
	}

	@Test
	public void schemaLocationEnabledWithNever() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.never);

		// good XSD location and namespace
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// bad XSD namespace
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"BAD_NAMESPACE\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// bad XSD location
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace BAD_LOCATION.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void schemaLocationEnabledWithOnValidSchema() throws Exception {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.onValidSchema);

		// good XSD location and namespace
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings, //
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_maxlength_valid), //
				d(2, 10, 2, 29, XMLSchemaErrorCode.cvc_attribute_3),
				d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_complex_type_2_4_b));

		// bad XSD namespace
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"BAD_NAMESPACE\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"     xsi:schemaLocation=\"team_namespace src/test/resources/xsd/team.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings);

		// bad XSD location
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<team\r\n" + //
				"     name=\"too long a string\"\r\n" + // <- error
				"     xmlns=\"team_namespace\"\r\n" + //
				"     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + // SchemaLocation
				"     xsi:schemaLocation=\"team_namespace BAD_LOCATION.xsd \">\r\n" + //
				"</team>";
		testDiagnosticsFor(xml, null, null, null, true, settings);
	}

	@Test
	public void diagnosticsWithWebApp() throws BadLocationException {
		String xml = "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"\r\n"
				+ //
				"    version=\"2.4\">\r\n" + //
				"    <servlet></servlet>\r\n" + // <-- error : "Child elements are missing from element:\n -
												// servlet\n\nThe following elements are expected:\n - description\n -
												// display-name\n - icon\n - servlet-name\n\nError indicated by\n
												// {http://java.sun.com/xml/ns/j2ee":description,
												// "http://java.sun.com/xml/ns/j2ee":display-name,
												// "http://java.sun.com/xml/ns/j2ee":icon,
												// "http://java.sun.com/xml/ns/j2ee}\nwith code:"
				"</web-app>";
		Diagnostic diagnostic = d(4, 5, 4, 12, XMLSchemaErrorCode.cvc_complex_type_2_4_b);
		testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog-web-app.xml", diagnostic);
	}

	@Test
	public void schemaLocationWithOddUris() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<ns:root\r\n" + "xmlns:ns='http://foo'\r\n" + //
				"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\r\n" + //
				"xsi:schemaLocation='http://foo foo.xsd http://bar'>\r\n" + "</ns:root>";

		Diagnostic d1 = d(4, 19, 4, 50, XMLSchemaErrorCode.SchemaLocation);
		Diagnostic d2 = d(4, 31, 4, 38, XMLSchemaErrorCode.schema_reference_4);
		Diagnostic d3 = d(1, 1, 1, 8, XMLSchemaErrorCode.cvc_elt_1_a);
		testDiagnosticsFor(xml, d1, d2, d3);
	}

	private static void testDiagnosticsWithCatalogFor(String xml, Diagnostic... expected) {
		testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", expected);
	}

	private static void testDiagnosticsDisabledValidation(String xml) {
		ContentModelSettings settings = XMLAssert.getContentModelSettings(true, SchemaEnabled.never);
		testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", null, null, true, settings);
	}

	private static String getGrammarFileURI(String grammarURI) throws MalformedURIException {
		int index = grammarURI.lastIndexOf('.');
		String path = grammarURI.substring(index + 1, grammarURI.length());
		return XMLEntityManager.expandSystemId(grammarURI, "src/test/resources/" + path + "/test.xml", true);
	}
}
