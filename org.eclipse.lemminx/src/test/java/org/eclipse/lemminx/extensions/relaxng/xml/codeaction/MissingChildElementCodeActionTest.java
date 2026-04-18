/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.xml.codeaction;

import static org.eclipse.lemminx.XMLAssert.ca;
import static org.eclipse.lemminx.XMLAssert.d;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.eclipse.lemminx.XMLAssert.teOp;
import static org.eclipse.lemminx.XMLAssert.testCodeActionsFor;
import static org.eclipse.lemminx.XMLAssert.testDiagnosticsFor;
import static org.eclipse.lemminx.XMLAssert.testResolveCodeActionsFor;

import java.util.Arrays;
import java.util.List;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_element_missingCodeActionResolver;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.missingelement.required_elements_missing_expectedCodeActionResolver;
import org.eclipse.lemminx.extensions.minify.participants.MinifyCodeActionResolver;
import org.eclipse.lemminx.extensions.relaxng.xml.validator.RelaxNGErrorCode;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CodeActionResolveSupportCapabilities;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.ResourceOperationKind;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceEditCapabilities;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Test for generating missing child elements for RELAX NG
 *
 */
public class MissingChildElementCodeActionTest extends AbstractCacheBasedTest {

	@Test
	public void incomplete_element_required_element_missing_only_root() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(1, 1, 1, 12, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 13, 2, 0, //
				"\r\n" + //
						"\t<card>\r\n" + //
						"\t\t<name></name>\r\n" + //
						"\t\t<email></email>\r\n" + //
						"\t</card>\r\n")),
				ca(d, te(1, 13, 2, 0, //
						"\r\n" + //
								"\t<card>\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email></email>\r\n" + //
								"\t</card>\r\n")));
	}

	@Test
	public void incomplete_element_required_element_missing_simple() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 7, 3, 1, //
				"\r\n" + //
						"\t\t<name></name>\r\n" + //
						"\t\t<email></email>\r\n\t")),
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email></email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_newlines() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 7, 7, 1, //
				"\r\n" + //
						"\t\t<name></name>\r\n" + //
						"\t\t<email></email>\r\n\t")),
				ca(d, te(2, 7, 7, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email></email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_with_existing() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"		<name></name>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 7, 4, 1, //
				"\r\n" + //
						"\t\t<name></name>\r\n" + //
						"\t\t<email></email>\r\n\t")),
				ca(d, te(2, 7, 4, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email></email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_with_ref() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v2.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(2, 7, 3, 1, //
				"\r\n" + //
						"\t\t<name></name>\r\n" + //
						"\t\t<email></email>\r\n\t")),
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email></email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_TEI_teiHeader() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"</TEI>";
		Diagnostic d = d(1, 1, 1, 4, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, 0, ca(d, te(1, 41, 2, 0, //
				"\r\n" + //
						"\t<teiHeader>\r\n" + //
						"\t\t<fileDesc>\r\n" + //
						"\t\t\t<titleStmt>\r\n" + //
						"\t\t\t\t<title></title>\r\n" + //
						"\t\t\t</titleStmt>\r\n" + //
						"\t\t\t<publicationStmt>\r\n" + //
						"\t\t\t\t<publisher></publisher>\r\n" + //
						"\t\t\t</publicationStmt>\r\n" + //
						"\t\t\t<sourceDesc>\r\n" + //
						"\t\t\t\t<p></p>\r\n" + //
						"\t\t\t</sourceDesc>\r\n" + //
						"\t\t</fileDesc>\r\n" + //
						"\t</teiHeader>\r\n" + //
						"\t<text>\r\n" + //
						"\t\t<body>\r\n" + //
						"\t\t\t<div></div>\r\n" + //
						"\t\t</body>\r\n" + //
						"\t</text>\r\n")));
	}

	@Test
	public void incomplete_element_required_element_missing_optional_element() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v4.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				// insert only required - emailContent is optional in schema - indicated by
				// <optional>
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t</email>\r\n\t")),
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t\t<emailOptional></emailOptional>\r\n" + //
								"\t\t</email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_optional_element_with_resolve() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v4.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		SharedSettings settings = createSharedSettings(true);
		XMLLanguageService ls = new XMLLanguageService();

		List<CodeAction> actual = testCodeActionsFor(xml, d, null, settings, ls,
				ca(d, createData("test.xml", required_element_missingCodeActionResolver.PARTICIPANT_ID, true)),
				ca(d, createData("test.xml", required_element_missingCodeActionResolver.PARTICIPANT_ID, false)), //
				// XML Minify
				ca(null, createData("test.xml", MinifyCodeActionResolver.PARTICIPANT_ID, null)));

		CodeAction unresolved1 = actual.get(0);
		testResolveCodeActionsFor(xml, unresolved1, settings, ls,
				ca(d, createData("test.xml", required_element_missingCodeActionResolver.PARTICIPANT_ID, true),
						teOp("test.xml", 2, 7, 3, 1, //
								"\r\n" + //
										"\t\t<name></name>\r\n" + //
										"\t\t<email>\r\n" + //
										"\t\t\t<emailContent></emailContent>\r\n" + //
										"\t\t</email>\r\n\t")));

		CodeAction unresolved2 = actual.get(1);
		testResolveCodeActionsFor(xml, unresolved2, settings, ls,
				ca(d, createData("test.xml", required_element_missingCodeActionResolver.PARTICIPANT_ID, false),
						teOp("test.xml", 2, 7, 3, 1, //
								"\r\n" + //
										"\t\t<name></name>\r\n" + //
										"\t\t<email>\r\n" + //
										"\t\t\t<emailContent></emailContent>\r\n" + //
										"\t\t\t<emailOptional></emailOptional>\r\n" + //
										"\t\t</email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_optional_element_zeroOrMore() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v5.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d,
				// insert only required - emailContent is optional in schema - indicated by
				// <zeroOrMore>
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t</email>\r\n\t")),
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t\t<emailOptional></emailOptional>\r\n" + //
								"\t\t</email>\r\n\t")));
	}

	// https://github.com/eclipse/lemminx/issues/1458
	@Test
	public void incomplete_element_required_element_missing_optional_element_zeroOrMore_autoCloseTags()
			throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/addressBook_v5.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"	<card>\r\n" + //
				"	</card>\r\n" + //
				"</addressBook>";
		Diagnostic d = d(2, 2, 2, 6, RelaxNGErrorCode.incomplete_element_required_element_missing);
		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(false);
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setInsertSpaces(false);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, settings,
				// insert only required - emailContent is optional in schema - indicated by
				// <zeroOrMore>
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t</email>\r\n\t")),
				ca(d, te(2, 7, 3, 1, //
						"\r\n" + //
								"\t\t<name></name>\r\n" + //
								"\t\t<email>\r\n" + //
								"\t\t\t<emailContent></emailContent>\r\n" + //
								"\t\t\t<emailOptional></emailOptional>\r\n" + //
								"\t\t</email>\r\n\t")));
	}

	@Test
	public void incomplete_element_required_element_missing_choice() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/article.rng\" ?>\r\n" + //
				"<article>\r\n" + //
				"</article>";
		Diagnostic d = d(1, 1, 1, 8, RelaxNGErrorCode.incomplete_element_required_element_missing);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 9, 2, 0, //
				"\r\n" + //
						"\t<title></title>\r\n")),
				// Generate all elements will generate duplicate elemenst in this case. Needs
				// fix in future.
				ca(d, te(1, 9, 2, 0, //
						"\r\n" + //
								"\t<title></title>\r\n" + //
								"\t<title>\r\n" + //
								"\t\t<line></line>\r\n" + //
								"\t</title>\r\n")));
	}

	@Test
	public void incomplete_element_required_elements_missing_choice() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/article_choice.rng\" ?>\r\n" + //
				"<article>\r\n" + //
				"</article>";
		Diagnostic d = d(1, 1, 1, 8, RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(1, 9, 2, 0, //
				"\r\n" + //
						"\t<title></title>\r\n")),
				ca(d, te(1, 9, 2, 0, //
						"\r\n" + //
								"\t<title2>\r\n" + //
								"\t\t<line></line>\r\n" + //
								"\t</title2>\r\n")),
				ca(d, te(1, 9, 2, 0, //
						"\r\n" + //
								"\t<titleChoice></titleChoice>\r\n")));
	}

	@Test
	public void incomplete_element_required_elements_missing_choice_TEItext() throws Exception {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"\t<teiHeader>\r\n" + //
				"\t\t<fileDesc>\r\n" + //
				"\t\t\t<titleStmt>\r\n" + //
				"\t\t\t\t<title></title>\r\n" + //
				"\t\t\t</titleStmt>\r\n" + //
				"\t\t\t<publicationStmt>\r\n" + //
				"\t\t\t\t<publisher></publisher>\r\n" + //
				"\t\t\t</publicationStmt>\r\n" + //
				"\t\t\t<sourceDesc>\r\n" + //
				"\t\t\t\t<p></p>\r\n" + //
				"\t\t\t</sourceDesc>\r\n" + //
				"\t\t</fileDesc>\r\n" + //
				"\t</teiHeader>\r\n" + //
				"\t<text>\r\n" + //
				"\t</text>\r\n" + //
				"</TEI>";
		Diagnostic d = d(15, 2, 15, 6, RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);
		testDiagnosticsFor(xml, d);
		testCodeActionsFor(xml, d, ca(d, te(15, 7, 16, 1, //
				"\r\n" + //
						"\t\t<body>\r\n" + //
						"\t\t\t<div></div>\r\n" + //
						"\t\t</body>\r\n\t")),
				ca(d, te(15, 7, 16, 1, //
						"\r\n" + //
								"\t\t<group>\r\n" + //
								"\t\t\t<text>\r\n" + //
								"\t\t\t\t<body>\r\n" + //
								"\t\t\t\t\t<div></div>\r\n" + //
								"\t\t\t\t</body>\r\n" + //
								"\t\t\t</text>\r\n" + //
								"\t\t</group>\r\n\t")));
	}

	@Test
	public void with_codeAction_resolver_support_choice() throws BadLocationException {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/article_choice.rng\" ?>\r\n" + //
				"<article>\r\n" + //
				"</article>";
		Diagnostic d = d(1, 1, 1, 8, RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);

		SharedSettings settings = createSharedSettings(true);

		XMLLanguageService ls = new XMLLanguageService();

		List<CodeAction> actual = testCodeActionsFor(xml, d, null, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"title")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"title2")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"titleChoice")), //
				// XML Minify
				ca(null, createData("test.xml", MinifyCodeActionResolver.PARTICIPANT_ID, null)));

		CodeAction unresolved1 = actual.get(0);
		testResolveCodeActionsFor(xml, unresolved1, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"title"),
						teOp("test.xml", 1, 9, 2, 0, //
								"\r\n" + //
										"\t<title></title>\r\n")));

		CodeAction unresolved2 = actual.get(1);
		testResolveCodeActionsFor(xml, unresolved2, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"title2"),
						teOp("test.xml", 1, 9, 2, 0, //
								"\r\n" + //
										"\t<title2>\r\n" + //
										"\t\t<line></line>\r\n" + //
										"\t</title2>\r\n")));

		CodeAction unresolved3 = actual.get(2);
		testResolveCodeActionsFor(xml, unresolved3, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"titleChoice"),
						teOp("test.xml", 1, 9, 2, 0, //
								"\r\n" + //
										"\t<titleChoice></titleChoice>\r\n")));

	}

	@Test
	public void with_codeAction_resolver_support_choice_pretext() throws BadLocationException {
		String xml = "<?xml-model href=\"https://raw.githubusercontent.com/PreTeXtBook/pretext/master/schema/pretext.rng\" ?>\r\n"
				+ //
				"<pretext>\r\n" + //
				"</pretext>";
		Diagnostic d = d(1, 1, 1, 8, RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);

		SharedSettings settings = createSharedSettings(true);

		XMLLanguageService ls = new XMLLanguageService();

		List<CodeAction> actual = testCodeActionsFor(xml, d, null, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"article")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"book")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"letter")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"memo")), //
				// XML Minify
				ca(null, createData("test.xml", MinifyCodeActionResolver.PARTICIPANT_ID, null)));

		CodeAction unresolved1 = actual.get(1);
		testResolveCodeActionsFor(xml, unresolved1, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"book"),
						teOp("test.xml", 1, 9, 2, 0, //
								"\r\n" + //
										"\t<book>\r\n" + //
										"\t\t<title></title>\r\n" + //
										"\t\t<part>\r\n" + //
										"\t\t\t<title></title>\r\n" + //
										"\t\t\t<chapter>\r\n" + //
										"\t\t\t\t<title></title>\r\n" + //
										"\t\t\t\t<p></p>\r\n" + //
										"\t\t\t</chapter>\r\n" + //
										"\t\t</part>\r\n" + //
										"\t</book>\r\n")));
	}

	@Test
	public void with_codeAction_resolver_TEI_text() throws BadLocationException {
		String xml = "<?xml-model href=\"src/test/resources/relaxng/tei_all.rng\" ?>\r\n" + //
				"<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\r\n" + //
				"\t<teiHeader>\r\n" + //
				"\t\t<fileDesc>\r\n" + //
				"\t\t\t<titleStmt>\r\n" + //
				"\t\t\t\t<title></title>\r\n" + //
				"\t\t\t</titleStmt>\r\n" + //
				"\t\t\t<publicationStmt>\r\n" + //
				"\t\t\t\t<publisher></publisher>\r\n" + //
				"\t\t\t</publicationStmt>\r\n" + //
				"\t\t\t<sourceDesc>\r\n" + //
				"\t\t\t\t<p></p>\r\n" + //
				"\t\t\t</sourceDesc>\r\n" + //
				"\t\t</fileDesc>\r\n" + //
				"\t</teiHeader>\r\n" + //
				"\t<text>\r\n" + //
				"\t</text>\r\n" + //
				"</TEI>";
		Diagnostic d = d(15, 2, 15, 6, RelaxNGErrorCode.incomplete_element_required_elements_missing_expected);

		SharedSettings settings = createSharedSettings(true);

		XMLLanguageService ls = new XMLLanguageService();

		List<CodeAction> actual = testCodeActionsFor(xml, d, null, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"body")),
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"group")), //
				// XML Minify
				ca(null, createData("test.xml", MinifyCodeActionResolver.PARTICIPANT_ID, null)));

		CodeAction unresolved1 = actual.get(0);
		testResolveCodeActionsFor(xml, unresolved1, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"body"),
						teOp("test.xml", 15, 7, 16, 1, //
								"\r\n" + //
										"\t\t<body>\r\n" + //
										"\t\t\t<div></div>\r\n" + //
										"\t\t</body>\r\n\t")));

		CodeAction unresolved2 = actual.get(1);
		testResolveCodeActionsFor(xml, unresolved2, settings, ls,
				ca(d, createData("test.xml", required_elements_missing_expectedCodeActionResolver.PARTICIPANT_ID,
						"group"),
						teOp("test.xml", 15, 7, 16, 1, //
								"\r\n" + //
										"\t\t<group>\r\n" + //
										"\t\t\t<text>\r\n" + //
										"\t\t\t\t<body>\r\n" + //
										"\t\t\t\t\t<div></div>\r\n" + //
										"\t\t\t\t</body>\r\n" + //
										"\t\t\t</text>\r\n" + //
										"\t\t</group>\r\n\t")));
	}

	private JsonObject createData(String uri, String particpantId, String elementName) {
		JsonObject data = DataEntryField.createData(uri, particpantId);
		if (elementName != null) {
			data.addProperty("element", elementName);
		}
		return data;
	}

	private JsonObject createData(String uri, String particpantId, boolean isGenerateRequired) {
		JsonObject data = DataEntryField.createData(uri, particpantId);
		String property = isGenerateRequired ? "true" : "false";
		data.addProperty("onlyGenerateRequired", property);
		return data;
	}

	private static SharedSettings createSharedSettings(boolean resolveCodeAction) {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setInsertSpaces(false);
		WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
		WorkspaceEditCapabilities workspaceEdit = new WorkspaceEditCapabilities();
		workspaceEdit.setResourceOperations(Arrays.asList(ResourceOperationKind.Create));
		workspace.setWorkspaceEdit(workspaceEdit);
		settings.getWorkspaceSettings().setCapabilities(workspace);
		// Expose `xml.open.binding.wizard` command
		settings.setBindingWizardSupport(true);

		if (resolveCodeAction) {
			CodeActionCapabilities codeAction = new CodeActionCapabilities();
			codeAction.setResolveSupport(new CodeActionResolveSupportCapabilities());
			settings.getCodeActionSettings().setCapabilities(codeAction);
		}

		return settings;
	}
}
