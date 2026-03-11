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

import static org.eclipse.lemminx.XMLAssert.pd;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.d;

import java.util.function.Consumer;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.ExternalResourceErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.FileServer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

/**
 * Test with published diagnostics.
 *
 */
public class XMLSchemaPublishDiagnosticsTest extends AbstractCacheBasedTest {

	@Test
	public void schemaWithUrlWithoutCache() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is disabled
		// Result of test is to have one published diagnostics with several Xerces
		// errors (schema)

		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use cache on file system
			contentModelManager.setUseCache(false);
		};

		String fileURI = "test.xml";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"http://invoice.xsd\">\r\n" + //
				"</invoice> \r\n" + //
				"";
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, configuration, pd(fileURI, //
				new Diagnostic(r(2, 32, 2, 50),
						"schema_reference.4: Failed to read schema document 'http://invoice.xsd', because 1) could not find the document; 2) the document could not be read; 3) the root element of the document is not <xsd:schema>.",
						DiagnosticSeverity.Warning, "xml", "schema_reference.4"), //
				new Diagnostic(r(1, 1, 1, 8), "cvc-elt.1.a: Cannot find the declaration of element 'invoice'.",
						DiagnosticSeverity.Error, "xml", "cvc-elt.1.a")));
	}

	@Test
	public void schemaWithUrlWithoutCacheNoDuplicateWarning() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is disabled
		// Result of test is to have one published diagnostics with several Xerces
		// errors (schema)

		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use cache on file system
			contentModelManager.setUseCache(false);
		};

		String fileURI = "test.xml";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"http://invoice.xsd\">\r\n" + //
				"  <a></a>\r\n" + //
				"  <b></b>\r\n" + //
				"  <c></c>\r\n" + //
				"</invoice> \r\n" + //
				"";
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, configuration, pd(fileURI, //
				new Diagnostic(r(2, 32, 2, 50),
						"schema_reference.4: Failed to read schema document 'http://invoice.xsd', because 1) could not find the document; 2) the document could not be read; 3) the root element of the document is not <xsd:schema>.",
						DiagnosticSeverity.Warning, "xml", "schema_reference.4"), //
				new Diagnostic(r(1, 1, 1, 8), "cvc-elt.1.a: Cannot find the declaration of element 'invoice'.",
						DiagnosticSeverity.Error, "xml", "cvc-elt.1.a")));
	}

	@Test
	public void schemaWithUrlInvalidPathWithNamespace() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is disabled
		// Result of test is to have one published diagnostics with several Xerces
		// errors (schema)

		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use cache on file system
			contentModelManager.setUseCache(false);
		};

		String fileURI = "test.xml";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:schemaLocation=\"http://invoice.xsd\">\r\n" + //
				"</invoice> \r\n" + //
				"";
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, configuration, pd(fileURI, //
				new Diagnostic(r(2, 20, 2, 40),
						"SchemaLocation: schemaLocation value = 'http://invoice.xsd' must have even number of URI's.",
						DiagnosticSeverity.Warning, "xml", "SchemaLocation"), //
				new Diagnostic(r(1, 1, 1, 8), "cvc-elt.1.a: Cannot find the declaration of element 'invoice'.",
						DiagnosticSeverity.Error, "xml", "cvc-elt.1.a")));
	}

	@Test
	public void schemaWithUrlWithCache() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is enabled
		// Result of test is to have 2 published diagnostics (resource downloading as
		// info and error downloading).

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		// Use cache on file system
		contentModelManager.setUseCache(true);

		String fileURI = "test.xml";
		String xsdCachePath = CacheResourcesManager.getResourceCachePath("http://invoice.xsd").toString();
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"http://invoice.xsd\">\r\n" + //
				"</invoice> \r\n" + //
				"";

		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, ls, pd(fileURI,
				new Diagnostic(r(2, 32, 2, 50),
						"The resource 'http://invoice.xsd' is downloading in the cache path '" + xsdCachePath + "'.",
						DiagnosticSeverity.Information, "xml", ExternalResourceErrorCode.DownloadingResource.getCode()),
				new Diagnostic(r(1, 1, 1, 8), "cvc-elt.1.a: Cannot find the declaration of element 'invoice'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));

		XMLAssert.awaitDownloads(ls, xml, fileURI);

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, ls, pd(fileURI,
				new Diagnostic(r(2, 32, 2, 50),
						"Error while downloading 'http://invoice.xsd' to '" + xsdCachePath
								+ "' : '[java.net.UnknownHostException] invoice.xsd'.",
						DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode()),
				new Diagnostic(r(1, 1, 1, 8), "cvc-elt.1.a: Cannot find the declaration of element 'invoice'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));
	}

	@Test
	public void schemaWithUrlWithCacheAndWithCatalog() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is enabled
		// - Catalog using which resolves XML Schema of the http://invoice.xsd
		// Result of test is to validate the XML with XML Schema

		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use cache on file system
			contentModelManager.setUseCache(true);
			// use catalog which defines bind src/test/xsd/invoice.xsd with
			// http://invoice.xsd namespace
			contentModelManager.setCatalogs(new String[] { "src/test/resources/catalogs/catalog.xml" });
		};

		String fileURI = "test.xml";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"http://invoice.xsd\">\r\n" + //
				"  <date>2017-11-30_INVALID</date>      \r\n" + // <- here is the error
				"  <number>5235</number> \r\n" + //
				"  <products>   \r\n" + //
				"    <product description=\"laptop\" price=\"700.00\"/>\r\n" + //
				"    <product description=\"mouse\" price=\"30.00\"  />\r\n" + //
				"  </products>\r\n" + //
				"  <payments> \r\n" + //
				"    <payment amount=\"770.00\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice> \r\n" + //
				"";

		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, configuration, pd(fileURI, //
				new Diagnostic(r(3, 8, 3, 26),
						"Content of type 'date' is expected.\n\nThe following content is not a valid type:\n '2017-11-30_INVALID'\n\nCode:",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_datatype_valid_1_2_1.getCode()), //
				new Diagnostic(r(3, 8, 3, 26),
						"cvc-type.3.1.3: The value '2017-11-30_INVALID' of element 'date' is not valid.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_type_3_1_3.getCode())));
	}

	@Test
	public void schemaWithUrlWithoutCacheAndWithCatalog() throws Exception {
		// Here we test the following context:
		// - XML which have xsi:noNamespaceSchemaLocation="http://invoice.xsd"
		// - XMLCacheResolverExtension which is disabled
		// - Catalog using which resolves XML Schema of the http://invoice.xsd
		// Result of test is to validate the XML with XML Schema

		// use catalog which defines bind src/test/xsd/invoice.xsd with
		// http://invoice.xsd namespace

		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Don't use cache on file system
			contentModelManager.setUseCache(false);
			// use catalog which defines bind src/test/xsd/invoice.xsd with
			// http://invoice.xsd namespace
			contentModelManager.setCatalogs(new String[] { "src/test/resources/catalogs/catalog.xml" });
		};
		String fileURI = "test.xml";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<invoice xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				" xsi:noNamespaceSchemaLocation=\"http://invoice.xsd\">\r\n" + //
				"  <date>2017-11-30_INVALID</date>      \r\n" + // <- here is the error
				"  <number>5235</number> \r\n" + //
				"  <products>   \r\n" + //
				"    <product description=\"laptop\" price=\"700.00\"/>\r\n" + //
				"    <product description=\"mouse\" price=\"30.00\"  />\r\n" + //
				"  </products>\r\n" + //
				"  <payments> \r\n" + //
				"    <payment amount=\"770.00\" method=\"credit\"/>\r\n" + //
				"  </payments>\r\n" + //
				"</invoice> \r\n" + //
				"";

		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, configuration, pd(fileURI, //
				new Diagnostic(r(3, 8, 3, 26),
						"Content of type 'date' is expected.\n\nThe following content is not a valid type:\n '2017-11-30_INVALID'\n\nCode:",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_datatype_valid_1_2_1.getCode()), //
				new Diagnostic(r(3, 8, 3, 26),
						"cvc-type.3.1.3: The value '2017-11-30_INVALID' of element 'date' is not valid.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_type_3_1_3.getCode())));
	}

}
