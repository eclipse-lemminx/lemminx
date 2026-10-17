/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
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

import static org.eclipse.lemminx.XMLAssert.tde;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.extensions.contentmodel.commands.GenerateGrammarFromXMLCommand.GenerateGrammarResult;
import org.eclipse.lemminx.utils.platform.Platform;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link GenerateGrammarFromXMLCommand} LSP command.
 *
 * <p>
 * Verifies that the command correctly generates grammar content and binding
 * edits for each grammar type (XSD, DTD, RNG, RNC) and binding type.
 * </p>
 */
public class GenerateGrammarFromXMLCommandTest extends AbstractCacheBasedTest {

	@Test
	public void generateXSDWithXsiNoNamespace() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo>\r\n  <bar />\r\n</foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.xsd");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "xsd", grammarFileUri, "xsi:schemaLocation")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("xs:schema"));
		assertTrue(result.getGrammarContent().contains("xs:element name=\"foo\""));
		assertTrue(result.getGrammarContent().contains("xs:element name=\"bar\""));

		// No namespace → uses noNamespaceSchemaLocation
		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1, te(0, 4, 0, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
						"    xsi:noNamespaceSchemaLocation=\"tag.xsd\"")));
	}

	@Test
	public void generateXSDWithXsiDefaultNamespace() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<root xmlns=\"urn:example\">\r\n  <child />\r\n</root>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.xsd");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "xsd", grammarFileUri, "xsi:schemaLocation")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("targetNamespace=\"urn:example\""));

		// Namespace exists → uses schemaLocation, xmlns is NOT duplicated
		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1, te(0, 25, 0, 25,
				"\r\n    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
						"    xsi:schemaLocation=\"urn:example tag.xsd\"")));
	}

	@Test
	public void generateXSDWithXsiPrefixedNamespace() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<p:root xmlns:p=\"urn:example\">\r\n  <p:child />\r\n</p:root>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.xsd");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "xsd", grammarFileUri, "xsi:schemaLocation")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("targetNamespace=\"urn:example\""));

		// Prefixed namespace → uses schemaLocation, xmlns:p is NOT duplicated
		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1, te(0, 29, 0, 29,
				"\r\n    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
						"    xsi:schemaLocation=\"urn:example tag.xsd\"")));
	}

	@Test
	public void generateDTDWithDoctype() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo>\r\n  <bar />\r\n</foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.dtd");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "dtd", grammarFileUri, "DOCTYPE")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("<!ELEMENT foo"));
		assertTrue(result.getGrammarContent().contains("<!ELEMENT bar"));

		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1,
				te(0, 0, 0, 0, "<!DOCTYPE foo SYSTEM \"tag.dtd\">\r\n")));
	}

	@Test
	public void generateRNGWithXmlModel() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo>\r\n  <bar />\r\n</foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.rng");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "rng", grammarFileUri, "xml-model")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("<grammar"));
		assertTrue(result.getGrammarContent().contains("<element name=\"foo\">"));

		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1,
				te(0, 0, 0, 0, "<?xml-model href=\"tag.rng\"?>\r\n")));
	}

	@Test
	public void generateRNCWithXmlModel() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo>\r\n  <bar />\r\n</foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.rnc");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "rnc", grammarFileUri, "xml-model")
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("element foo"));
		assertTrue(result.getGrammarContent().contains("element bar"));

		assertNotNull(result.getBindingEdit());
		assertEquals(result.getBindingEdit(), tde(xmlPath, 1,
				te(0, 0, 0, 0, "<?xml-model href=\"tag.rnc\"?>\r\n")));
	}

	@Test
	public void generateXSDWithFileAssociation() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo>\r\n  <bar />\r\n</foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.xsd");

		GenerateGrammarResult result = (GenerateGrammarResult) languageServer
				.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
						xmlIdentifier, "xsd", grammarFileUri)
				.get();

		assertNotNull(result);
		assertNotNull(result.getGrammarContent());
		assertTrue(result.getGrammarContent().contains("xs:schema"));
		assertNull(result.getBindingEdit());
	}

	@Test
	public void rejectXsiSchemaLocationWithDTD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo><bar /></foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.dtd");

		ExecutionException ex = assertThrows(ExecutionException.class, () -> {
			languageServer.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
					xmlIdentifier, "dtd", grammarFileUri, "xsi:schemaLocation").get();
		});
		assertTrue(ex.getCause().getMessage().contains("only compatible with XSD"));
	}

	@Test
	public void rejectDoctypeWithXSD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xml = "<foo><bar /></foo>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String grammarFileUri = getFileURI("src/test/resources/tag.xsd");

		ExecutionException ex = assertThrows(ExecutionException.class, () -> {
			languageServer.executeCommand(GenerateGrammarFromXMLCommand.COMMAND_ID,
					xmlIdentifier, "xsd", grammarFileUri, "DOCTYPE").get();
		});
		assertTrue(ex.getCause().getMessage().contains("only compatible with DTD"));
	}

	private static String getFileURI(String fileName) {
		String uri = new File(fileName).toURI().toString();
		if (Platform.isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
