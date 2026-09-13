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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.extensions.contentmodel.commands.ListRootElementsCommand.RootElementInfo;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLGenerationSettings;
import org.eclipse.lemminx.utils.platform.Platform;
import org.junit.jupiter.api.Test;

/**
 * Tests for the LSP commands {@link ListRootElementsCommand} and
 * {@link GenerateXMLFromGrammarCommand}.
 *
 * <p>
 * These tests verify that the commands work correctly through the LSP command
 * infrastructure ({@code workspace/executeCommand}). For detailed XML
 * generation tests, see the {@code XMLDocumentGenerator*Test} classes in the
 * {@code generator} package.
 * </p>
 */
public class GenerateXMLFromGrammarCommandTest extends AbstractCacheBasedTest {

	private static final String ls = System.lineSeparator();

	// ==================== ListRootElements ====================

	/**
	 * Tests listing root elements from an XSD without namespace.
	 */
	@Test
	public void listRootElementsFromXSD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/xsd/tag.xsd");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, xsdPath).get();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("root", result.get(0).getName());
	}

	/**
	 * Tests listing root elements from an XSD with a target namespace.
	 */
	@Test
	public void listRootElementsFromXSDWithNamespace() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/xsd/team.xsd");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, xsdPath).get();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("team", result.get(0).getName());
		assertEquals("team_namespace", result.get(0).getNamespace());
	}

	/**
	 * Tests listing root elements from a DTD.
	 */
	@Test
	public void listRootElementsFromDTD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String dtdPath = getFileURI("src/test/resources/dtd/note.dtd");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, dtdPath).get();
		assertNotNull(result);
		assertTrue(result.size() > 0);
		assertTrue(result.stream().anyMatch(e -> "note".equals(e.getName())));
	}

	/**
	 * Tests listing root elements from a RelaxNG (.rng) grammar.
	 */
	@Test
	public void listRootElementsFromRNG() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String rngPath = getFileURI("src/test/resources/relaxng/addressBook_v1.rng");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, rngPath).get();
		assertNotNull(result);
		assertTrue(result.size() > 0);
		assertTrue(result.stream().anyMatch(e -> "addressBook".equals(e.getName())));
	}

	/**
	 * Tests listing root elements from a RelaxNG Compact (.rnc) grammar.
	 */
	@Test
	public void listRootElementsFromRNC() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String rncPath = getFileURI("src/test/resources/relaxng/addressBook.rnc");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, rncPath).get();
		assertNotNull(result);
		assertTrue(result.size() > 0);
		assertTrue(result.stream().anyMatch(e -> "addressBook".equals(e.getName())));
	}

	/**
	 * Tests that listing root elements from an XSD with imports only returns
	 * elements from the main schema, not imported ones.
	 */
	@Test
	public void listRootElementsFromXSDWithImport() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/generator/xsd/importedElement/main.xsd");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, xsdPath).get();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("root", result.get(0).getName());
		assertEquals("http://example.com/main", result.get(0).getNamespace());
	}

	/**
	 * Tests that listing root elements from the imported schema (product.xsd)
	 * returns its own element (QuestionValue).
	 */
	@Test
	public void listRootElementsFromXSDImportedSchema() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/generator/xsd/importedElement/product.xsd");
		@SuppressWarnings("unchecked")
		List<RootElementInfo> result = (List<RootElementInfo>) languageServer
				.executeCommand(ListRootElementsCommand.COMMAND_ID, xsdPath).get();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("QuestionValue", result.get(0).getName());
		assertEquals("http://example.com/product", result.get(0).getNamespace());
	}

	// ==================== GenerateXMLFromGrammar ====================

	/**
	 * Tests generating XML from an XSD via the LSP command.
	 * <p>
	 * Verifies the full generated content including XML declaration, grammar
	 * binding, and element structure.
	 * </p>
	 */
	@Test
	public void generateFromXSD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/xsd/tag.xsd");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(true);
		String result = (String) languageServer
				.executeCommand(GenerateXMLFromGrammarCommand.COMMAND_ID, xsdPath, "root", settings).get();
		assertTrue(result.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		assertTrue(result.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
		assertTrue(result.contains("xsi:noNamespaceSchemaLocation=\"" + xsdPath + "\""));
		assertTrue(result.contains("<tag>"));
		assertTrue(result.contains("</tag>"));
		assertTrue(result.contains("<optional>"));
		assertTrue(result.contains("</optional>"));
		assertTrue(result.contains("</root>"));
	}

	/**
	 * Tests generating XML from a DTD via the LSP command.
	 */
	@Test
	public void generateFromDTD() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String dtdPath = getFileURI("src/test/resources/dtd/note.dtd");
		String result = (String) languageServer
				.executeCommand(GenerateXMLFromGrammarCommand.COMMAND_ID, dtdPath, "note").get();
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<!DOCTYPE note SYSTEM \"" + dtdPath + "\">" + ls +
				"<note>" + ls +
				"  <to>" + ls +
				"  </to>" + ls +
				"  <from>" + ls +
				"  </from>" + ls +
				"  <body>" + ls +
				"  </body>" + ls +
				"</note>" + ls, result);
	}

	/**
	 * Tests generating XML from a RelaxNG (.rng) grammar via the LSP command.
	 */
	@Test
	public void generateFromRNG() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String rngPath = getFileURI("src/test/resources/relaxng/addressBook_v1.rng");
		String result = (String) languageServer
				.executeCommand(GenerateXMLFromGrammarCommand.COMMAND_ID, rngPath, "addressBook").get();
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + rngPath + "\"?>" + ls +
				"<addressBook>" + ls +
				"  <card>" + ls +
				"    <name>" + ls +
				"    </name>" + ls +
				"    <email>" + ls +
				"    </email>" + ls +
				"  </card>" + ls +
				"</addressBook>" + ls, result);
	}

	/**
	 * Tests generating XML from a RelaxNG Compact (.rnc) grammar via the LSP
	 * command.
	 */
	@Test
	public void generateFromRNC() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String rncPath = getFileURI("src/test/resources/relaxng/addressBook.rnc");
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(true);
		String result = (String) languageServer
				.executeCommand(GenerateXMLFromGrammarCommand.COMMAND_ID, rncPath, "addressBook", settings).get();
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + ls +
				"<?xml-model href=\"" + rncPath + "\"?>" + ls +
				"<addressBook>" + ls +
				"  <card>" + ls +
				"    <name>" + ls +
				"    </name>" + ls +
				"    <email>" + ls +
				"    </email>" + ls +
				"    <age>" + ls +
				"    </age>" + ls +
				"  </card>" + ls +
				"</addressBook>" + ls, result);
	}

	/**
	 * Tests that generating XML with an unknown root element returns an empty
	 * string.
	 */
	@Test
	public void unknownRootElement() throws InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();
		String xsdPath = getFileURI("src/test/resources/xsd/tag.xsd");
		String result = (String) languageServer
				.executeCommand(GenerateXMLFromGrammarCommand.COMMAND_ID, xsdPath, "unknown").get();
		assertEquals("", result);
	}

	// ==================== Helpers ====================

	private static String getFileURI(String fileName) {
		String uri = new File(fileName).toURI().toString();
		if (Platform.isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
