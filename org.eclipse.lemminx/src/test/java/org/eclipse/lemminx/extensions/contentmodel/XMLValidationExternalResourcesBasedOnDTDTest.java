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
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.ExternalResourceErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.DownloadDisabledResourceCodeAction;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * XML validation based on DTD with external download.
 *
 */
public class XMLValidationExternalResourcesBasedOnDTDTest extends AbstractCacheBasedTest {

	@Test
	public void docTypeDownloadDisabled() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		// Disable download
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		contentModelManager.setDownloadExternalResources(false);

		String xml = "<!DOCTYPE root-element PUBLIC \"public-id\" \"http://localhost:8080/sample.dtd\">\r\n" + //
				"<root-element>\r\n" + //
				"</root-element>";

		String fileURI = "test.xml";
		// Downloading...
		Diagnostic d = new Diagnostic(r(0, 43, 0, 75), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI, d, //
				new Diagnostic(r(1, 1, 1, 13), "Element type \"root-element\" must be declared.",
						DiagnosticSeverity.Error, "xml", DTDErrorCode.MSG_ELEMENT_NOT_DECLARED.getCode())));

		// Test code action
		testCodeActionsFor(xml, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://localhost:8080/sample.dtd'.", "http://localhost:8080/sample.dtd",
						fileURI)));
	}

	@Test
	@Disabled // because of download issue
	public void docTypeDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String xml = "<!DOCTYPE root-element PUBLIC \"public-id\" \"http://localhost:8080/sample.dtd\">\r\n" + //
				"<root-element>\r\n" + //
				"</root-element>";

		String dtdCachePath = CacheResourcesManager.getResourceCachePath("http://localhost:8080/sample.dtd").toString();
		String fileURI = "test.xml";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(0, 43, 0, 75),
								"The resource 'http://localhost:8080/sample.dtd' is downloading in the cache path '"
										+ dtdCachePath + "'.",
								DiagnosticSeverity.Information, "xml",
								ExternalResourceErrorCode.DownloadingResource.getCode()),
						new Diagnostic(r(1, 1, 1, 13), "Element type \"root-element\" must be declared.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.MSG_ELEMENT_NOT_DECLARED.getCode())));

		TimeUnit.SECONDS.sleep(5); // HACK: to make the timing work on slow machines

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(0, 43, 0, 75),
								"Error while downloading 'http://localhost:8080/sample.dtd' to '" + dtdCachePath
										+ "' : '[java.net.ConnectException] Connection refused'.",
								DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode()),
						new Diagnostic(r(1, 1, 1, 13), "Element type \"root-element\" must be declared.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.MSG_ELEMENT_NOT_DECLARED.getCode())));
	}

	@Test
	public void entityRefDownloadDisabled() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();
		ls.initializeIfNeeded();
		// Disable download
		ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
		contentModelManager.setDownloadExternalResources(false);

		String xml = "<!DOCTYPE root-element [\r\n" + //
				"	<!ELEMENT root-element (#PCDATA)>\r\n" + //
				"	<!ENTITY % entity-name SYSTEM \"http://localhost:8080/sample.dtd\">\r\n" + //
				"	%entity-name;\r\n" + //
				"]>\r\n" + //
				"<root-element>\r\n" + //
				"	&abcd;\r\n" + //
				"</root-element>";

		String fileURI = "test.xml";
		// Downloading...
		Diagnostic d = new Diagnostic(r(2, 32, 2, 64), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls, pd(fileURI, d, //
				new Diagnostic(r(6, 1, 6, 7), "The entity \"abcd\" was referenced, but not declared.",
						DiagnosticSeverity.Error, "xml", DTDErrorCode.EntityNotDeclared.getCode())));

		// Test code action
		testCodeActionsFor(xml, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://localhost:8080/sample.dtd'.", "http://localhost:8080/sample.dtd",
						fileURI)));
	}

	@Test
	@Disabled
	public void entityRefDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String xml = "<!DOCTYPE root-element [\r\n" + //
				"	<!ELEMENT root-element (#PCDATA)>\r\n" + //
				"	<!ENTITY % entity-name SYSTEM \"http://localhost:8080/sample.dtd\">\r\n" + //
				"	%entity-name;\r\n" + //
				"]>\r\n" + //
				"<root-element>\r\n" + //
				"	&abcd;\r\n" + //
				"</root-element>";

		String dtdCachePath = CacheResourcesManager.getResourceCachePath("http://localhost:8080/sample.dtd").toString();
		String fileURI = "test.xml";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(2, 32, 2, 64),
								"The resource 'http://localhost:8080/sample.dtd' is downloading in the cache path '"
										+ dtdCachePath + "'.",
								DiagnosticSeverity.Information, "xml",
								ExternalResourceErrorCode.DownloadingResource.getCode()),
						new Diagnostic(r(6, 1, 6, 7), "The entity \"abcd\" was referenced, but not declared.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.EntityNotDeclared.getCode())));

		TimeUnit.SECONDS.sleep(5); // HACK: to make the timing work on slow machines

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(xml, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(2, 32, 2, 64),
								"Error while downloading 'http://localhost:8080/sample.dtd' to '" + dtdCachePath
										+ "' : '[java.net.ConnectException] Connection refused'.",
								DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode()),
						new Diagnostic(r(6, 1, 6, 7), "The entity \"abcd\" was referenced, but not declared.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.EntityNotDeclared.getCode())));
	}

}
