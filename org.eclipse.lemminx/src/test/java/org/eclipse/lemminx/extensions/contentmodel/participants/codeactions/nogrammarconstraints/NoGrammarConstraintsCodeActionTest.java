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
package org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints;

import static org.eclipse.lemminx.XMLAssert.te;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Generate grammar URI tests.
 *
 */
public class NoGrammarConstraintsCodeActionTest extends AbstractCacheBasedTest {

	private static final String TEST_DOCUMENT_URI = "file:///test.xml";

	private XMLLanguageService xmlLanguageService = new XMLLanguageService();

	private SharedSettings sharedSettings = new SharedSettings();

	@Test
	public void generateGrammarURI() {
		String actual = NoGrammarConstraintsCodeAction.getGrammarURI("file:///C:/test.xml", "xsd");
		assertEquals("file:///C:/test.xsd", actual);
	}

	@Test
	public void generateGrammarURIWithDot() {
		String actual = NoGrammarConstraintsCodeAction.getGrammarURI("file:///C:/.project", "xsd");
		assertEquals("file:///C:/.project.xsd", actual);
	}

	@Test
	public void testAssociateWithXmlModelCodeAction() throws BadLocationException {
		DOMDocument xmlDocument = getDOMDocument("");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXmlModelEdit("file:///my-schema.xsd", null,
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0, "<?xml-model href=\"file:///my-schema.xsd\"?>" + System.lineSeparator()),
						te(0, 0, 0, 0, "<root-element></root-element>")),
				actual);

		xmlDocument = getDOMDocument("");
		actual = NoGrammarConstraintsCodeAction.createXmlModelEdit("file:///my-schema.xsd", "https://google.ca",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0, "<?xml-model href=\"file:///my-schema.xsd\"?>" + System.lineSeparator()),
						te(0, 0, 0, 0, "<root-element xmlns=\"https://google.ca\"></root-element>")),
				actual);

		xmlDocument = getDOMDocument("<root></root>");
		actual = NoGrammarConstraintsCodeAction.createXmlModelEdit("file:///my-schema.xsd", "https://google.ca",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0, "<?xml-model href=\"file:///my-schema.xsd\"?>" + System.lineSeparator()),
						te(0, 5, 0, 5, " xmlns=\"https://google.ca\"")),
				actual);

		xmlDocument = getDOMDocument("<root hjkl=\"hjkl\"></root>");
		actual = NoGrammarConstraintsCodeAction.createXmlModelEdit("file:///my-schema.xsd", "https://google.ca",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0, "<?xml-model href=\"file:///my-schema.xsd\"?>" + System.lineSeparator()),
						te(0, 5, 0, 5, " xmlns=\"https://google.ca\"")),
				actual);

		xmlDocument = getDOMDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		actual = NoGrammarConstraintsCodeAction.createXmlModelEdit("file:///my-schema.xsd", "https://google.ca",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 38, 0, 38, System.lineSeparator() + "<?xml-model href=\"file:///my-schema.xsd\"?>" + System.lineSeparator()),
						te(0, 38, 0, 38, System.lineSeparator() + "<root-element xmlns=\"https://google.ca\"></root-element>")),
				actual);
	}

	@Test
	public void testAssociateWithDoctypeCodeAction() throws BadLocationException {
		DOMDocument xmlDocument = getDOMDocument("");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createDocTypeEdit("file:///my-schema.dtd", xmlDocument,
				sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0,
						"<!DOCTYPE root-element SYSTEM \"file:///my-schema.dtd\">" + System.lineSeparator()
								+ "<root-element></root-element>")),
				actual);

		xmlDocument = getDOMDocument("<root></root>");
		actual = NoGrammarConstraintsCodeAction.createDocTypeEdit("file:///my-schema.dtd", xmlDocument,
				sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 0, 0, 0, "<!DOCTYPE root SYSTEM \"file:///my-schema.dtd\">" + System.lineSeparator())),
				actual);

		xmlDocument = getDOMDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		actual = NoGrammarConstraintsCodeAction.createDocTypeEdit("file:///my-schema.dtd", xmlDocument,
				sharedSettings);
		assertTextDocumentEdit(
				tde(te(0, 38, 0, 38,
						System.lineSeparator() + "<!DOCTYPE root-element SYSTEM \"file:///my-schema.dtd\">"
								+ System.lineSeparator() + "<root-element></root-element>")),
				actual);
	}

	@Test
	public void testAssociateWithNoNamespaceSchemaLocationCodeAction() throws BadLocationException {
		DOMDocument xmlDocument = getDOMDocument("");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.dtd", xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 0, 0, 0,
				"<root-element xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.dtd\"></root-element>")),
				actual);

		xmlDocument = getDOMDocument("<root></root>");
		actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.dtd",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 5, 0, 5,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.dtd\"")),
				actual);

		xmlDocument = getDOMDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.dtd",
				xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 38, 0, 38,
				System.lineSeparator() + "<root-element xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
						+ System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.dtd\"></root-element>")),
				actual);
	}

	@Test
	public void testAssociateWithSchemaLocationCodeAction() throws BadLocationException {
		DOMDocument xmlDocument = getDOMDocument("");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit("file:///my-schema.dtd",
				"https://google.ca", xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 0, 0, 0,
				"<root-element xmlns=\"https://google.ca\"" + System.lineSeparator()
						+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:schemaLocation=\"https://google.ca file:///my-schema.dtd\"></root-element>")),
				actual);

		xmlDocument = getDOMDocument("<root></root>");
		actual = NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit("file:///my-schema.dtd",
				"https://google.ca", xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 5, 0, 5,
				" xmlns=\"https://google.ca\"" + System.lineSeparator()
						+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:schemaLocation=\"https://google.ca file:///my-schema.dtd\"")),
				actual);

		xmlDocument = getDOMDocument("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		actual = NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit("file:///my-schema.dtd",
				"https://google.ca", xmlDocument, sharedSettings);
		assertTextDocumentEdit(tde(te(0, 38, 0, 38,
				System.lineSeparator() + "<root-element xmlns=\"https://google.ca\"" + System.lineSeparator()
						+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:schemaLocation=\"https://google.ca file:///my-schema.dtd\"></root-element>")),
				actual);
	}

	// ---------- splitAttributes mode tests ----------

	@Test
	public void testNoNamespaceSchemaLocationForceMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		DOMDocument xmlDocument = getDOMDocument("<root></root>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, settings);
		assertTextDocumentEdit(tde(te(0, 5, 0, 5,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testNoNamespaceSchemaLocationForceAlignedMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		// <root> => tagName.length() + 2 = 4 + 2 = 6 spaces alignment
		DOMDocument xmlDocument = getDOMDocument("<root></root>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, settings);
		assertTextDocumentEdit(tde(te(0, 5, 0, 5,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "      xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testNoNamespaceSchemaLocationForceAlignedModeShortTag() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		// <foo> => tagName.length() + 2 = 3 + 2 = 5 spaces alignment
		DOMDocument xmlDocument = getDOMDocument("<foo></foo>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, settings);
		assertTextDocumentEdit(tde(te(0, 4, 0, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "     xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testNoNamespaceSchemaLocationForceExpandMultilineMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceExpandMultiline);

		DOMDocument xmlDocument = getDOMDocument("<root></root>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, settings);
		// In forceExpandMultiline, first attribute also goes on new line
		assertTextDocumentEdit(tde(te(0, 5, 0, 5,
				System.lineSeparator()
						+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testNoNamespaceSchemaLocationWithExistingAttrsForceMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		DOMDocument xmlDocument = getDOMDocument("<foo attr=\"123\"></foo>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, settings);
		// Replaces from after tag name (4) to first attr start (5), consuming the space
		// Existing attr gets pushed to a new line
		assertTextDocumentEdit(tde(te(0, 4, 0, 5,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"" + System.lineSeparator()
						+ "    ")),
				actual);
	}

	@Test
	public void testNoNamespaceSchemaLocationWithExistingAttrsPreserveMode() throws BadLocationException {
		// In preserve mode, existing attributes stay where they are
		DOMDocument xmlDocument = getDOMDocument("<foo attr=\"123\"></foo>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSINoNamespaceSchemaLocationEdit("file:///my-schema.xsd", xmlDocument, sharedSettings);
		// Pure insert, no range replacement
		assertTextDocumentEdit(tde(te(0, 4, 0, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:noNamespaceSchemaLocation=\"file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testSchemaLocationForceAlignedMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);

		// <foo> => 3 + 2 = 5 spaces alignment
		DOMDocument xmlDocument = getDOMDocument("<foo></foo>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSISchemaLocationEdit("file:///my-schema.xsd", "https://ns.example.com", xmlDocument, settings);
		assertTextDocumentEdit(tde(te(0, 4, 0, 4,
				" xmlns=\"https://ns.example.com\"" + System.lineSeparator()
						+ "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "     xsi:schemaLocation=\"https://ns.example.com file:///my-schema.xsd\"")),
				actual);
	}

	@Test
	public void testSchemaLocationWithExistingAttrsForceMode() throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setSplitAttributes(SplitAttributes.force);

		DOMDocument xmlDocument = getDOMDocument("<foo attr=\"123\"></foo>");
		TextDocumentEdit actual = NoGrammarConstraintsCodeAction
				.createXSISchemaLocationEdit("file:///my-schema.xsd", "https://ns.example.com", xmlDocument, settings);
		assertTextDocumentEdit(tde(te(0, 4, 0, 5,
				" xmlns=\"https://ns.example.com\"" + System.lineSeparator()
						+ "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + System.lineSeparator()
						+ "    xsi:schemaLocation=\"https://ns.example.com file:///my-schema.xsd\"" + System.lineSeparator()
						+ "    ")),
				actual);
	}

	private DOMDocument getDOMDocument(String xml) {
		TextDocument textDocument = new TextDocument(xml, TEST_DOCUMENT_URI);
		return DOMParser.getInstance().parse(textDocument, xmlLanguageService.getResolverExtensionManager());
	}

	private static TextDocumentEdit tde(TextEdit... edits) {
		return XMLAssert.tde(TEST_DOCUMENT_URI, 0, edits);
	}

	private static void assertTextDocumentEdit(TextDocumentEdit expected, TextDocumentEdit actual) {
		Assertions.assertEquals(expected.getTextDocument().getUri(), actual.getTextDocument().getUri());
		Assertions.assertEquals(expected.getEdits().size(), actual.getEdits().size());
		for (int i = 0; i < expected.getEdits().size(); i++) {
			Assertions.assertEquals(expected.getEdits().get(i), actual.getEdits().get(i));
		}
	}

}
