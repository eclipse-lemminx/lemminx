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
package org.eclipse.lemminx.extensions.xsd;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.pd;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.ExternalResourceErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.DownloadDisabledResourceCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

/**
 * XSD file diagnostics.
 *
 */
public class XSDValidationExternalResourcesTest extends AbstractCacheBasedTest {

	@Test
	public void includeSchemaLocationDownloadDisabled() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		// Disable download
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		contentModelManager.setDownloadExternalResources(false);

		String xsd = "<xs:schema\r\n" + //
				"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"    elementFormDefault=\"qualified\">\r\n" + //
				"    <xs:include schemaLocation=\"http://server:8080/sequence.xsd\" />\r\n" + //
				"</xs:schema>";

		String fileURI = "test.xsd";
		// Downloading...
		Diagnostic d = new Diagnostic(r(3, 32, 3, 63), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xsd", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(xsd, fileURI, validation, ls, pd(fileURI, d));

		// Test code action
		testCodeActionsFor(xsd, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://server:8080/sequence.xsd'.", "http://server:8080/sequence.xsd",
						fileURI)));
	}

	@Test
	public void includeSchemaLocationDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String xsd = "<xs:schema\r\n" + //
				"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
				"    elementFormDefault=\"qualified\">\r\n" + //
				"    <xs:include schemaLocation=\"http://server:8080/sequence.xsd\" />\r\n" + //
				"</xs:schema>";

		String xsdCachePath = CacheResourcesManager.getResourceCachePath("http://server:8080/sequence.xsd").toString();
		String fileURI = "test.xsd";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xsd, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(3, 32, 3, 63),
								"The resource 'http://server:8080/sequence.xsd' is downloading in the cache path '"
										+ xsdCachePath + "'.",
								DiagnosticSeverity.Information, "xsd",
								ExternalResourceErrorCode.DownloadingResource.getCode())));

		XMLAssert.awaitDownloads(ls, xsd, fileURI, validation);

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xsd, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(3, 32, 3, 63),
								"Error while downloading 'http://server:8080/sequence.xsd' to '" + xsdCachePath + "' : '[java.net.UnknownHostException] server'.",
								DiagnosticSeverity.Error, "xsd", ExternalResourceErrorCode.DownloadProblem.getCode())));

	}

	public static void testDiagnosticsFor(String xml, String fileURI, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, fileURI, true, expected);
	}

	public static void testDiagnosticsFor(String xml, String fileURI, ContentModelSettings settings,
			Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(xml, null, null, fileURI, true, settings, expected);
	}

	public static void testDiagnosticsFor(XMLLanguageService ls, String xml, String fileURI,
			ContentModelSettings settings, Diagnostic... expected) {
		XMLAssert.testDiagnosticsFor(ls, xml, null, null, fileURI, true, settings, expected);
	}
}
