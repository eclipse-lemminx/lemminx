/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.extensions.contentmodel.commands.AssociateGrammarCommand.GrammarBindingType;
import org.eclipse.lemminx.utils.platform.Platform;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

/**
 * Test for associating a grammar to a given XML command.
 *
 * @author Angelo ZERR
 *
 */
public class AssociateGrammarCommandTest extends AbstractCacheBasedTest {

	@Test
	public void associateWithXSDNoNamespaceSchemaLocation() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String xsdPath = getFileURI("src/test/resources/xsd/tag.xsd");
		String bindingType = GrammarBindingType.STANDARD.getName();

		TextDocumentEdit actual = (TextDocumentEdit) languageServer
				.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, xsdPath, bindingType).get();
		assertNotNull(actual);

		assertEquals(actual, tde(xmlPath, 1, te(1, 4, 1, 4, //
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"")));
	}

	@Test
	public void associateWithXSDSchemaLocation() throws Exception {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);

		String xsdPath = getFileURI("src/test/resources/xsd/team.xsd");
		String bindingType = GrammarBindingType.STANDARD.getName();

		TextDocumentEdit actual = (TextDocumentEdit) languageServer
				.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, xsdPath, bindingType).get();
		assertNotNull(actual);

		assertEquals(actual, tde(xmlPath, 1, te(1, 4, 1, 4, //
				" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace xsd/team.xsd\"")));
	}

	@Test
	public void associateWithDTD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String dtdPath = getFileURI("src/test/resources/dtd/tag.dtd");
		String bindingType = GrammarBindingType.STANDARD.getName();

		TextDocumentEdit actual = (TextDocumentEdit) languageServer
				.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, dtdPath, bindingType).get();
		assertNotNull(actual);

		assertEquals(actual, tde(xmlPath, 1, //
				te(1, 0, 1, 0, "<!DOCTYPE foo SYSTEM \"dtd/tag.dtd\">\r\n")));
	}

	@Test
	public void associateWithXMLModel() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String dtdPath = getFileURI("src/test/resources/dtd/tag.dtd");
		String bindingType = GrammarBindingType.XML_MODEL.getName();

		TextDocumentEdit actual = (TextDocumentEdit) languageServer
				.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, dtdPath, bindingType).get();
		assertNotNull(actual);

		assertEquals(actual, tde(xmlPath, 1, //
				te(1, 0, 1, 0, "<?xml-model href=\"dtd/tag.dtd\"?>\r\n")));
	}

	@Test
	public void associateWithXMLModelAXSDWithTargetNamespace() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String xsdPath = getFileURI("src/test/resources/xsd/team.xsd");
		String bindingType = GrammarBindingType.XML_MODEL.getName();

		TextDocumentEdit actual = (TextDocumentEdit) languageServer
				.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, xsdPath, bindingType).get();
		assertNotNull(actual);

		assertEquals(actual, tde(xmlPath, 1, //
				te(1, 0, 1, 0, "<?xml-model href=\"xsd/team.xsd\"?>\r\n"), //
				te(1, 4, 1, 4, " xmlns=\"team_namespace\"")));
	}

	@Test
	public void unknownBindingTypeException() throws InterruptedException, ExecutionException {

		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xml = "<?xml version=\"1.0\" ?>\r\n" + //
				"<foo/>";
		String xmlPath = getFileURI("src/test/resources/tag.xml");
		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(xmlPath, xml);
		String dtdPath = getFileURI("src/test/resources/dtd/tag.dtd");
		String bindingType = "BAD";

		try {
			languageServer.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, dtdPath, bindingType)
					.get();
			fail("Unknown binding type should throw an exception.");
		} catch (Exception e) {
			assertEquals("Unknown binding type 'BAD'. Allowed values are [standard, xml-model]",
					e.getCause().getMessage());
		}

	}

	@Test
	public void unknownFileUri() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		String xmlPath = "tag.xml";
		TextDocumentIdentifier xmlIdentifier = new TextDocumentIdentifier(xmlPath);
		String xsdPath = "tag.xsd";
		String bindingType = GrammarBindingType.STANDARD.getName();
		try {
			languageServer.executeCommand(AssociateGrammarCommand.COMMAND_ID, xmlIdentifier, xsdPath, bindingType)
					.get();
			fail("Unknown file URI should throw an exception.");
		} catch (Exception e) {
			assertEquals("Command 'xml.associate.grammar.insert' cannot find the DOM document with the URI 'tag.xml'.",
					e.getCause().getMessage());
		}

	}

	private static String getFileURI(String fileName) {
		String uri = new File(fileName).toURI().toString();
		if (Platform.isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
