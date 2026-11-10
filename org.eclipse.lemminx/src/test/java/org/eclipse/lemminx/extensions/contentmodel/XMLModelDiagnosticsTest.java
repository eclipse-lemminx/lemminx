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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSyntaxErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.SchemaEnabled;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

/**
 * XML Validation test with xml-model processing instruction association.
 *
 */
public class XMLModelDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void xmlModelWithBadDTD() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<?xml-model href=\"BAD.dtd\"?>\r\n" + //
				"<root><item /></root>";
		testDiagnosticsFor(xml, d(1, 17, 26, DTDErrorCode.DTDNotFound));
	}

	@Test
	public void xmlModelWithDTD() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<?xml-model href=\"src/test/resources/dtd/web-app_2_3.dtd\"?>\r\n" + //
				"<web-app>\r\n" + //
				"	<XXX></XXX>\r\n" + //
				"</web-app>";
		testDiagnosticsFor(xml, d(3, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED),
				d(2, 1, 8, DTDErrorCode.MSG_CONTENT_INVALID));
	}

	@Test
	public void xmlModelWithBadXSD() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?> \r\n" + //
				"<?xml-model href=\"BAD.xsd\"?>\r\n" + //
				"<root><item /></root>";
		testDiagnosticsFor(xml, d(1, 18, 25, XMLSchemaErrorCode.schema_reference_4), //
				d(2, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a));
	}

	@Test
	public void xmlModelWithXSD() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<?xml-model href=\"src/test/resources/xsd/tag.xsd\"?>\r\n" + //
				"<root>\r\n" + //
				"  <tag></tag>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"</root>";
		testDiagnosticsFor(xml, d(6, 3, 6, 11, XMLSchemaErrorCode.cvc_complex_type_2_4_f));
	}

	@Test
	public void xmlModelWithXSDAndNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<?xml-model href=\"http://www.springframework.org/schema/beans/spring-beans-3.0.xsd\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" >\r\n" + //
				"	<bean>\r\n" + //
				"		XXXXXXXXXXXXX\r\n" + // <-- error
				"	</bean>\r\n" + //
				"</beans>";
		Diagnostic d = d(4, 2, 4, 15, XMLSchemaErrorCode.cvc_complex_type_2_3,
				"Element \'bean\' cannot contain text content.\nThe content type is defined as element-only.\n\nCode:");
		testDiagnosticsFor(xml, d);
	}

	@Test
	public void cvc_elt_1_a_basic() throws Exception {
		String xml = "<test xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</test>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_elt_1_a);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 1, 0, 5, "root"),
				te(5, 2, 5, 6, "root")));
	}

	@Test
	public void cvc_elt_1_a_multiple_roots_defined() throws Exception {
		String xml = "<test xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique_multiroot.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n" + //
				"</test>";
		Diagnostic d = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_elt_1_a);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(0, 1, 0, 5, "root"),
				te(5, 2, 5, 6, "root")),
				ca(d, te(0, 1, 0, 5, "root2"),
						te(5, 2, 5, 6, "root2")));
	}

	@Test
	public void cvc_elt_1_a_no_end_tag() throws Exception {
		String xml = "<test xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"src/test/resources/xsd/unique.xsd\">\r\n" + //
				"    <authors status = \"new\">\r\n" + //
				"    <author>smith</author>\r\n" + //
				"    </authors>\r\n";
		Diagnostic d1 = d(0, 1, 0, 5, XMLSchemaErrorCode.cvc_elt_1_a);
		Diagnostic d2 = d(0, 1, 0, 5, XMLSyntaxErrorCode.MarkupEntityMismatch);
		testDiagnosticsFor(xml, d1, d2);
		testCodeActionsFor(xml, d1, ca(d1, te(0, 1, 0, 5, "root")));
	}
	
	@Test
	public void noDiagnosticsWhenSchemaBasedValidationDisabled() throws Exception {
		// Use the same settings as in XMLAssert, but completely disable schema validation
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		XMLValidationRootSettings problems = new XMLValidationRootSettings();
		problems.getSchema().setEnabled(SchemaEnabled.never);
		problems.setNoGrammar("ignore");
		settings.setValidation(problems);

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<?xml-model href=\"src/test/resources/xsd/tag.xsd\"?>\r\n" + //
				"<root>\r\n" + //
				"  <tag></tag>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"  <optional></optional>\r\n" + //
				"</root>";
		XMLAssert.testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", null, null, true, settings);
	}

	private static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, "src/test/resources/catalogs/catalog.xml", expected);
	}

}
