/**
 *  Copyright (c) 2022 Angelo ZERR
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

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.pd;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import java.util.concurrent.TimeUnit;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.ExternalResourceErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.DownloadDisabledResourceCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * XML validation based on XSD with external download.
 *
 */
public class XMLValidationExternalResourcesBasedOnXSDTest extends AbstractCacheBasedTest {

	@Test
	public void noNamespaceSchemaLocationDownloadDisabled() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		// Disable download
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		contentModelManager.setDownloadExternalResources(false);

		String xml = "<root-element\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"http://localhost:8080/sample.xsd\">\r\n" + //
				"	\r\n" + //
				"</root-element>";

		String fileURI = "test.xml";
		// Downloading...
		Diagnostic d = new Diagnostic(r(2, 32, 2, 64), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI, d, //
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));

		// Test code action
		testCodeActionsFor(xml, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://localhost:8080/sample.xsd'.", "http://localhost:8080/sample.xsd",
						fileURI)));
	}

	@Test
	@Disabled // because of downloaded error
	public void noNamespaceSchemaLocationDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String xml = "<root-element\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:noNamespaceSchemaLocation=\"http://localhost:8080/sample.xsd\">\r\n" + //
				"	\r\n" + //
				"</root-element>";

		String xsdCachePath = CacheResourcesManager.getResourceCachePath("http://localhost:8080/sample.xsd").toString();
		String fileURI = "test.xml";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI,
				new Diagnostic(r(2, 32, 2, 64),
						"The resource 'http://localhost:8080/sample.xsd' is downloading in the cache path '"
								+ xsdCachePath + "'.",
						DiagnosticSeverity.Information, "xml", ExternalResourceErrorCode.DownloadingResource.getCode()),
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));

		TimeUnit.SECONDS.sleep(5); // HACK: to make the timing work on slow machines

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI,
				new Diagnostic(r(2, 32, 2, 64),
						"Error while downloading 'http://localhost:8080/sample.xsd' to '" + xsdCachePath
								+ "' : '[java.net.ConnectException] Connection refused'.",
						DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode()),
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));
	}

	@Test
	public void schemaLocationDownloadDisabled() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		// Disable download
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		contentModelManager.setDownloadExternalResources(false);

		String xml = "<root-element xmlns=\"https://github.com/eclipse/lemminx\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"\r\n" + //
				"		https://github.com/eclipse/lemminx http://localhost:8080/sample.xsd\">\r\n" + //
				"	\r\n" + //
				"</root-element>";

		String fileURI = "test.xml";
		// Downloading...
		Diagnostic d = new Diagnostic(r(3, 37, 3, 69), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI, d, //
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));

		// Test code action
		testCodeActionsFor(xml, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://localhost:8080/sample.xsd'.", "http://localhost:8080/sample.xsd",
						fileURI)));
	}

	@Test
	@Disabled
	public void schemaLocationDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String xml = "<root-element xmlns=\"https://github.com/eclipse/lemminx\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"\r\n" + //
				"		https://github.com/eclipse/lemminx http://localhost:8080/sample.xsd\">\r\n" + //
				"	\r\n" + //
				"</root-element>";

		String xsdCachePath = CacheResourcesManager.getResourceCachePath("http://localhost:8080/sample.xsd").toString();
		String fileURI = "test.xml";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI,
				new Diagnostic(r(3, 37, 3, 69),
						"The resource 'http://localhost:8080/sample.xsd' is downloading in the cache path '"
								+ xsdCachePath + "'.",
						DiagnosticSeverity.Information, "xml", ExternalResourceErrorCode.DownloadingResource.getCode()),
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));

		TimeUnit.SECONDS.sleep(5); // HACK: to make the timing work on slow machines

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI,
				new Diagnostic(r(3, 37, 3, 69),
						"Error while downloading 'http://localhost:8080/sample.xsd' to '" + xsdCachePath
								+ "' : '[java.net.ConnectException] Connection refused'.",
						DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode()),
				new Diagnostic(r(0, 1, 0, 13), "cvc-elt.1.a: Cannot find the declaration of element 'root-element'.",
						DiagnosticSeverity.Error, "xml", XMLSchemaErrorCode.cvc_elt_1_a.getCode())));
	}

}
