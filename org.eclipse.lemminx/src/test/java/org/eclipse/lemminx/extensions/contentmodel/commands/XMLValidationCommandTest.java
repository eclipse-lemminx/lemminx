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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.pd;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.contentmodel.BaseFileTempTest;
import org.eclipse.lemminx.extensions.contentmodel.participants.DTDErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.ExternalResourceErrorCode;
import org.eclipse.lemminx.extensions.contentmodel.participants.XMLSchemaErrorCode;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lemminx.uriresolver.FileServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

/**
 * Test for validate command which evict caches and revalidate XML files.
 * 
 * @author Angelo ZERR
 *
 */
public class XMLValidationCommandTest extends BaseFileTempTest {

	@Test
	public void validationFileCommand() throws Exception {
		Path baseDir = getTempDirPath().resolve("_lemminx_commands");
		FileServer httpServer = new FileServer(baseDir);
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		try {
			httpServer.start();
			// Create XSD file
			String xsdPath = baseDir.resolve("tag.xsd").toString();
			String xsdURL = httpServer.getUri("tag.xsd"); // ex : http://localhost:56946/tag.xsd
			String cachePath = CacheResourcesManager.getResourceCachePath(xsdURL).toString();

			// Create a XSD file in the temp directory of the HTTP server
			String xsd = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
					"<xs:schema\r\n" + //
					"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
					"    elementFormDefault=\"qualified\">\r\n" + //
					"  <xs:element name=\"root\">\r\n" + //
					"    <xs:complexType>\r\n" + //
					"      <xs:sequence>\r\n" + //
					"        <xs:element name=\"tag\"/>\r\n" + //
					"      </xs:sequence>\r\n" + //
					"    </xs:complexType>\r\n" + //
					"  </xs:element>\r\n" + //
					"</xs:schema>";
			createFile(xsdPath, xsd);

			// Open the XML file
			String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
					"<root\r\n" + //
					"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
					"    xsi:noNamespaceSchemaLocation=\"" + xsdURL + "\">\r\n" + //
					"  <tags />\r\n" + // <-- error
					"</root>";

			// Open the XML document, the validation is triggered asynchronously
			TextDocumentIdentifier xmlIdentifier = languageServer.didOpen("test.xml", xml);

			// Wait for downloading of XSD and validation to trigger
			languageServer.waitForDiagnosticCount(2, 5000);

			// 1.1 Validation test

			// We should have 2 diagnostics:
			// - one for downloading resources (Information)
			// - One for validation result (Error)
			List<PublishDiagnosticsParams> actualDiagnostics = languageServer.getPublishDiagnostics();
			XMLAssert.assertPublishDiagnostics(actualDiagnostics,
					pd(xmlIdentifier.getUri(),
							d(3, 35, 3, 65, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + xsdURL + "' is downloading in the cache path '" + cachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information),
							d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a,
									"cvc-elt.1.a: Cannot find the declaration of element 'root'.", "xml",
									DiagnosticSeverity.Error)), //
					pd(xmlIdentifier.getUri(), d(4, 3, 4, 7, XMLSchemaErrorCode.cvc_complex_type_2_4_a,
							"Element name 'tags' is invalid.\n\nOne of the following is expected:\n - tag\n\nError indicated by:\n {the schema}\nwith code:",
							"xml", DiagnosticSeverity.Error)

					));
			actualDiagnostics.clear();

			// 1.2 Completion test
			CompletionList list = completion(languageServer, xmlIdentifier);
			XMLAssert.assertCompletion(list, c("tag", "<tag></tag>"), 5 /* region, endregion, cdata, comment, tag */);

			// Here the HTTP server updates the XSD with xsd:element tag --> tags
			xsd = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
					"<xs:schema\r\n" + //
					"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
					"    elementFormDefault=\"qualified\">\r\n" + //
					"  <xs:element name=\"root\">\r\n" + //
					"    <xs:complexType>\r\n" + //
					"      <xs:sequence>\r\n" + //
					"        <xs:element name=\"tags\"/>\r\n" + // <-- here tag is updated to tags
					"      </xs:sequence>\r\n" + //
					"    </xs:complexType>\r\n" + //
					"  </xs:element>\r\n" + //
					"</xs:schema>";
			updateFile(xsdPath, xsd);

			// 2.2 Completion test
			// Here completion returns the old tag cmpletion, because evict cache is not
			// done
			list = completion(languageServer, xmlIdentifier);
			XMLAssert.assertCompletion(list, c("tag", "<tag></tag>"), 5 /* region, endregion, cdata, comment, tag */);

			// Execute command cache
			languageServer.executeCommand(XMLValidationFileCommand.COMMAND_ID, xmlIdentifier).get();

			// Wait for downloading and validation to trigger
			languageServer.waitForDiagnosticCount(2, 5000);

			// 3.1 Validation test

			// We should have 2 publish diagnostics:
			// - one for downloading resources (Information)
			// - one with empty diagnostics array (none errors)
			actualDiagnostics = languageServer.getPublishDiagnostics();
			XMLAssert.assertPublishDiagnostics(actualDiagnostics, //
					pd(xmlIdentifier.getUri(),
							d(3, 35, 3, 65, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + xsdURL + "' is downloading in the cache path '" + cachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information),
							d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a,
									"cvc-elt.1.a: Cannot find the declaration of element 'root'.", "xml",
									DiagnosticSeverity.Error)), //
					pd(xmlIdentifier.getUri()));
			actualDiagnostics.clear();

			// 3.2 Completion test
			// Here completion should return tags
			list = completion(languageServer, xmlIdentifier);
			XMLAssert.assertCompletion(list, c("tags", "<tags></tags>"),
					5 /* region, endregion, cdata, comment, tag */);

		} finally {
			httpServer.stop();
		}
	}

	@Test
	public void validationAllFilesCommand() throws Exception {
		Path baseDir = getTempDirPath().resolve("_lemminx_commands");
		FileServer httpServer = new FileServer(baseDir);
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		try {
			httpServer.start();

			// Create XSD file
			String xsdPath = baseDir.resolve("tag.xsd").toString();
			String xsdURL = httpServer.getUri("tag.xsd"); // ex : http://localhost:56946/tag.xsd
			String xsdCachePath = CacheResourcesManager.getResourceCachePath(xsdURL).toString();

			// Create a XSD file in the temp directory
			String xsd = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
					"<xs:schema\r\n" + //
					"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
					"    elementFormDefault=\"qualified\">\r\n" + //
					"  <xs:element name=\"root\">\r\n" + //
					"    <xs:complexType>\r\n" + //
					"      <xs:sequence>\r\n" + //
					"        <xs:element name=\"tag\"/>\r\n" + //
					"      </xs:sequence>\r\n" + //
					"    </xs:complexType>\r\n" + //
					"  </xs:element>\r\n" + //
					"</xs:schema>";
			createFile(xsdPath, xsd);

			// Create DTD file
			String dtdPath = baseDir.resolve("tag.dtd").toString();
			String dtdURL = httpServer.getUri("tag.dtd"); // ex : http://localhost:56946/tag.dtd
			String dtdCachePath = CacheResourcesManager.getResourceCachePath(dtdURL).toString();

			// Create a DTD file in the temp directory
			String dtd = "<!ELEMENT root (tag)>\r\n" + //
					"<!ELEMENT tag EMPTY>";
			createFile(dtdPath, dtd);

			// Open the first XML file bound to the XSD
			String xml1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
					"<root\r\n" + //
					"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
					"    xsi:noNamespaceSchemaLocation=\"" + xsdURL + "\">\r\n" + //
					"  <tags />\r\n" + // <-- error
					"</root>";

			// Open the XML document, the validation is triggered asynchronously
			TextDocumentIdentifier xml1Identifier = languageServer.didOpen("test1.xml", xml1);

			// Wait for XSD diagnostics before opening second file
			languageServer.waitForDiagnosticCount(2, 5000);

			// Open the second XML file bound to the DTD
			String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
					"<!DOCTYPE root SYSTEM \"" + dtdURL + "\">\r\n" + //
					"<root>\r\n" + //
					"  <tags />\r\n" + // <-- error
					"</root>";

			// Open the XML document, the validation is triggered asynchronously
			TextDocumentIdentifier xml2Identifier = languageServer.didOpen("test2.xml", xml2);

			// Wait for all 4 diagnostics (2 for each XML file)
			languageServer.waitForDiagnosticCount(4, 5000);

			// 1.1 Validation test

			// We should have 4 diagnostics:
			// - one for downloading XSD resource (Information)
			// - One for validation of the first XML result (Error)
			// - one for downloading DTD resource (Information)
			// - One for validation of the second XML result (Error)
			List<PublishDiagnosticsParams> actualDiagnostics = languageServer.getPublishDiagnostics();

			XMLAssert.assertPublishDiagnostics(actualDiagnostics, //
					pd(xml1Identifier.getUri(),
							d(3, 35, 3, 65, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + xsdURL + "' is downloading in the cache path '" + xsdCachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information),
							d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a,
									"cvc-elt.1.a: Cannot find the declaration of element 'root'.", "xml",
									DiagnosticSeverity.Error)), //
					pd(xml1Identifier.getUri(), d(4, 3, 4, 7, XMLSchemaErrorCode.cvc_complex_type_2_4_a,
							"Element name 'tags' is invalid.\n\nOne of the following is expected:\n - tag\n\nError indicated by:\n {the schema}\nwith code:",
							"xml", DiagnosticSeverity.Error)), //
					pd(xml2Identifier.getUri(),
							d(1, 23, 1, 53, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + dtdURL + "' is downloading in the cache path '" + dtdCachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information), //
							d(2, 1, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED, //
									"Element type \"root\" must be declared.", "xml", DiagnosticSeverity.Error), //
							d(3, 3, 3, 7, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED,
									"Element type \"tags\" must be declared.", "xml", DiagnosticSeverity.Error)), //
					pd(xml2Identifier.getUri(), //
							d(3, 3, 3, 7, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED,
									"Element type \"tags\" must be declared.", "xml", DiagnosticSeverity.Error),
							d(2, 1, 2, 5, DTDErrorCode.MSG_CONTENT_INVALID,
									"The content of element type \"root\" must match \"(tag)\".", "xml",
									DiagnosticSeverity.Error)));
			actualDiagnostics.clear();

			// 1.2 Completion test
			CompletionList list1 = completion(languageServer, xml1Identifier);
			XMLAssert.assertCompletion(list1, c("tag", "<tag></tag>"), 5 /* region, endregion, cdata, comment, tag */);
			CompletionList list2 = completion(languageServer, xml2Identifier);
			XMLAssert.assertCompletion(list2, c("tag", "<tag />"), 5 /* region, endregion, cdata, comment, tag */);

			// Here the HTTP server updates the XSD with xsd:element tag --> tags
			xsd = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\r\n" + //
					"<xs:schema\r\n" + //
					"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n" + //
					"    elementFormDefault=\"qualified\">\r\n" + //
					"  <xs:element name=\"root\">\r\n" + //
					"    <xs:complexType>\r\n" + //
					"      <xs:sequence>\r\n" + //
					"        <xs:element name=\"tags\"/>\r\n" + // <-- here tag is updated to tags
					"      </xs:sequence>\r\n" + //
					"    </xs:complexType>\r\n" + //
					"  </xs:element>\r\n" + //
					"</xs:schema>";
			updateFile(xsdPath, xsd);

			// Here the HTTP server updates the DTD with tag --> tags
			dtd = "<!ELEMENT root (tags)>\r\n" + //
					"<!ELEMENT tags EMPTY>";
			updateFile(dtdPath, dtd);

			// 2.2 Completion test
			// Here completion returns the old tag completion, because evict cache is not
			// done
			list1 = completion(languageServer, xml1Identifier);
			XMLAssert.assertCompletion(list1, c("tag", "<tag></tag>"), 5 /* region, endregion, cdata, comment, tag */);
			list2 = completion(languageServer, xml2Identifier);
			XMLAssert.assertCompletion(list2, c("tag", "<tag />"), 5 /* region, endregion, cdata, comment, tag */);

			// Execute command cache
			languageServer.executeCommand(XMLValidationAllFilesCommand.COMMAND_ID).get();

			// Wait for downloading and validation to trigger (4 diagnostics: 2 per file)
			languageServer.waitForDiagnosticCount(4, 5000);

			// 3.1 Validation test

			// We should have 2 publish diagnostics:
			// - one for downloading XSD resource (Information)
			// - one with empty diagnostics array for the first XML (none errors)
			// - one for downloading DTD resource (Information)
			// - one with empty diagnostics array for the second XML (none errors)
			actualDiagnostics = languageServer.getPublishDiagnostics();
			Collections.sort(actualDiagnostics, (d1, d2) -> {
				return d1.getUri().compareTo(d2.getUri());
			});
			XMLAssert.assertPublishDiagnostics(actualDiagnostics, //
					pd(xml1Identifier.getUri(), //
							d(3, 35, 3, 65, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + xsdURL + "' is downloading in the cache path '" + xsdCachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information),
							d(1, 1, 1, 5, XMLSchemaErrorCode.cvc_elt_1_a,
									"cvc-elt.1.a: Cannot find the declaration of element 'root'.", "xml",
									DiagnosticSeverity.Error)), //
					pd(xml1Identifier.getUri()), //
					pd(xml2Identifier.getUri(),
							d(1, 23, 1, 53, ExternalResourceErrorCode.DownloadingResource,
									"The resource '" + dtdURL + "' is downloading in the cache path '" + dtdCachePath
											+ "'.",
									"xml", DiagnosticSeverity.Information), //
							d(2, 1, 2, 5, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED,
									"Element type \"root\" must be declared.", "xml", DiagnosticSeverity.Error), //
							d(3, 3, 3, 7, DTDErrorCode.MSG_ELEMENT_NOT_DECLARED,
									"Element type \"tags\" must be declared.", "xml", DiagnosticSeverity.Error)), //
					pd(xml2Identifier.getUri()));
			actualDiagnostics.clear();

			// 3.2 Completion test
			// Here completion should return tags
			list1 = completion(languageServer, xml1Identifier);
			XMLAssert.assertCompletion(list1, c("tags", "<tags></tags>"),
					5 /* region, endregion, cdata, comment, tags */);
			list2 = completion(languageServer, xml2Identifier);
			XMLAssert.assertCompletion(list2, c("tags", "<tags />"), 5 /* region, endregion, cdata, comment, tags */);
		} finally {
			httpServer.stop();
		}
	}

	private static CompletionList completion(MockXMLLanguageServer languageServer, TextDocumentIdentifier xmlIdentifier)
			throws BadLocationException, InterruptedException, ExecutionException {
		DOMDocument document = languageServer.getDocument(xmlIdentifier.getUri());
		int offset = document.getText().indexOf("<tags />");
		Position position = document.positionAt(offset);
		CompletionParams completionParams = new CompletionParams();
		completionParams.setTextDocument(xmlIdentifier);
		completionParams.setPosition(position);
		Either<List<CompletionItem>, CompletionList> result = languageServer.getTextDocumentService()
				.completion(completionParams).get();
		CompletionList list = result.getRight();
		return list;
	}
}
