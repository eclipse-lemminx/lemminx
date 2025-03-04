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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests the functionality where an XML file undergoes
 * validation against its schema when the XSD schema file (or DTD file)
 * has been modified and saved externally through means, not within
 * the LS client.
 *
 * @author David Kwon
 *
 */
public class XMLExternalTest extends BaseFileTempTest {

	private int threadSleepMs = 600;
	private MockXMLLanguageServer languageServer;

	@BeforeEach
	public void before() {
		languageServer = new MockXMLLanguageServer();
	}

	@Test
	public void externalDTDTest() throws InterruptedException, IOException {
		String dtdPath = getTempDirPath().toString() + "/note.dtd";

		//@formatter:off
		String dtdContents =
		"<!ELEMENT note (to,from,heading,body)>\n" +
		"<!ELEMENT to (#PCDATA)>\n" +
		"<!ELEMENT from (#PCDATA)>\n" +
		"<!ELEMENT heading (#PCDATA)>\n" +
		"<!ELEMENT body (#PCDATA)>";

		String xmlContents =
		"<?xml version=\"1.0\"?>\n" +
		"<!DOCTYPE note SYSTEM \"note.dtd\">\n" +
		"<note>\n" +
		"  <to>Tove</to>\n" +
		"  <from>Jani</from>\n" +
		"  <heading>Reminder</heading>\n" +
		"  <body>Don\'t forget me this weekend!</body>\n" +
		"</note>";
		//@formatter:on


		TextDocumentItem xmlTextDocument = getXMLTextDocumentItem("test.xml", xmlContents);
		createFile(dtdPath, dtdContents);
		File testDtd = new File(dtdPath);

		clientOpenFile(languageServer, xmlTextDocument);

		Thread.sleep(threadSleepMs);

		List<PublishDiagnosticsParams> actualDiagnostics = getDiagnostic(1);
		assertEquals(1, actualDiagnostics.size());
		assertEquals(0, actualDiagnostics.get(0).getDiagnostics().size());

		editFile(testDtd, 2, "");
		didChangedWatchedFiles(languageServer, testDtd);

		Thread.sleep(threadSleepMs);
		actualDiagnostics = getDiagnostic(2);
		assertEquals(2, actualDiagnostics.size());
		assertFalse(actualDiagnostics.get(1).getDiagnostics().isEmpty());
		assertEquals("MSG_ELEMENT_NOT_DECLARED", actualDiagnostics.get(1).getDiagnostics().get(0).getCode().getLeft());
	}

	@Test
	public void externalXSDTest() throws InterruptedException, IOException {
		String xsdPath = getTempDirPath().toString() + "/sequence.xsd";

		//@formatter:off
		String xsdContents =
		"<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
		"<xs:schema\n" +
		"    xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n" +
		"    elementFormDefault=\"qualified\">\n" +
		"  <xs:element name=\"root\">\n" +
		"    <xs:complexType>\n" +
		"      <xs:sequence>\n" +
		"        <xs:element name=\"tag\"/>\n" +
		"        <xs:element\n" +
		"            name=\"optional\"\n" +
		"            minOccurs=\"0\"\n" +
		"            maxOccurs=\"3\"/>\n" +
		"      </xs:sequence>\n" +
		"    </xs:complexType>\n" +
		"  </xs:element>\n" +
		"</xs:schema>";

		String xmlContents =
		"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
		"<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"sequence.xsd\">\n" +
		"  <tag></tag>\n" +
		"  <optional></optional>\n" +
		"  <optional></optional>\n" +
		"  <optional></optional>\n" +
		"</root>";
		//@formatter:on

		TextDocumentItem xmlTextDocument = getXMLTextDocumentItem("test.xml", xmlContents);
		createFile(xsdPath, xsdContents);
		File testXsd = new File(xsdPath);

		clientOpenFile(languageServer, xmlTextDocument);

		List<PublishDiagnosticsParams> actualDiagnostics = getDiagnostic(1);
		assertEquals(1, actualDiagnostics.size());
		assertEquals(0, actualDiagnostics.get(0).getDiagnostics().size());

		editFile(testXsd, 12, "            maxOccurs=\"2\"/>");
		didChangedWatchedFiles(languageServer, testXsd);
		actualDiagnostics = getDiagnostic(2);
		assertEquals(2, actualDiagnostics.size());
		PublishDiagnosticsParams params = actualDiagnostics.get(1);
		List<Diagnostic> diagnostics = params.getDiagnostics();
		assertTrue(diagnostics.size() > 0);
		assertEquals("cvc-complex-type.2.4.f", diagnostics.get(0).getCode().getLeft());
	}

	private List<PublishDiagnosticsParams> getDiagnostic(int wanted) {
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
		while (System.currentTimeMillis() < deadline) {
			List<PublishDiagnosticsParams> list = new ArrayList<>(languageServer.getPublishDiagnostics());
			if (list.size() >= wanted) {
				return list;
			}
			Thread.yield();
		}
		fail("Did not recived at laest " + wanted + " diagnostics withing time frame!");
		return new ArrayList<>();
	}

	private TextDocumentItem getXMLTextDocumentItem(String filename, String xmlContents) {
		String languageId = "xml";
		int version = 1;
		return new TextDocumentItem(getTempDirPath().toString().replace('\\', '/') + "/" + filename, languageId, version, xmlContents);
	}

	private void clientOpenFile(XMLLanguageServer languageServer, TextDocumentItem textDocumentItem) {
		DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(textDocumentItem);
		languageServer.getTextDocumentService().didOpen(params);
	}

	private void editFile(File file, int lineNumber, String newContent) throws IOException {
		List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
		lines.set(lineNumber - 1, newContent);
		java.nio.file.Files.write(file.toPath(), lines);
	}

	private void didChangedWatchedFiles(XMLLanguageServer ls, File file) {
		List<FileEvent> changes = new ArrayList<>();
		changes.add(new FileEvent(file.toURI().toString(), FileChangeType.Changed));
		DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams(changes);
		ls.getWorkspaceService().didChangeWatchedFiles(params);
	}
}