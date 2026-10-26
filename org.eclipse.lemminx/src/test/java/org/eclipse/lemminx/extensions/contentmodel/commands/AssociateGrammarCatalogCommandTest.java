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

import static org.eclipse.lemminx.XMLAssert.sne;
import static org.eclipse.lemminx.XMLAssert.tde;
import static org.eclipse.lemminx.XMLAssert.tdeSnippet;
import static org.eclipse.lemminx.XMLAssert.te;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.contentmodel.commands.AssociateGrammarCommand.GrammarBindingType;
import org.eclipse.lemminx.extensions.contentmodel.participants.codeactions.nogrammarconstraints.NoGrammarConstraintsCodeAction;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions.SplitAttributes;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.Test;

/**
 * Test for catalog-based grammar binding and namespace existence checks via the
 * xml.associate.grammar.insert command.
 */
public class AssociateGrammarCatalogCommandTest {

	private static final CancelChecker NULL_CHECKER = () -> {
	};

	private static final String ls = System.lineSeparator();

	// ==========================================
	// Catalog binding: URI entry
	// ==========================================

	// Catalog entry: <uri name="http://example.org/schema/simple" uri="simple.rng"/>
	// <foo /> -> <?xml-model href="http://example.org/schema/simple"
	//              schematypens="http://relaxng.org/ns/structure/1.0"?>
	//            <foo />
	@Test
	public void catalogUriEntryWithRNG() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String rngURI = getFileURI("src/test/resources/relaxng/simple.rng");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/simple", rngURI, "simple.rng");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<?xml-model href=\"http://example.org/schema/simple\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\r\n")));
	}

	// Catalog entry: <uri name="http://example.org/schema/simple" uri="simple.rnc"/>
	// <foo /> -> <?xml-model href="http://example.org/schema/simple"
	//              schematypens="http://relaxng.org/ns/structure/1.0"?>
	//            <foo />
	@Test
	public void catalogUriEntryWithRNC() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String rncURI = getFileURI("src/test/resources/relaxng/simple.rng").replace(".rng", ".rnc");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/simple", rncURI, "simple.rnc");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<?xml-model href=\"http://example.org/schema/simple\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>\r\n")));
	}

	// Catalog entry: <uri name="http://example.org/schema/tag" uri="tag.xsd"/>
	// XSD has no targetNamespace
	// <foo /> -> <foo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:noNamespaceSchemaLocation="http://example.org/schema/tag"/>
	@Test
	public void catalogUriEntryWithXSDNoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/tag", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"http://example.org/schema/tag\"")));
	}

	// Catalog entry: <uri name="http://example.org/schema/team" uri="team.xsd"/>
	// XSD has targetNamespace="team_namespace"
	// <foo /> -> <foo xmlns="team_namespace"
	//                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:schemaLocation="team_namespace http://example.org/schema/team"/>
	@Test
	public void catalogUriEntryWithXSDWithNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/team", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace http://example.org/schema/team\"")));
	}

	// Catalog entry: <uri name="http://example.org/schema/note" uri="note.dtd"/>
	// URI entry pointing to DTD: falls back to DOCTYPE binding
	// <foo /> -> <!DOCTYPE foo SYSTEM "http://example.org/schema/note">
	//            <foo />
	@Test
	public void catalogUriEntryWithDTDFallback() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/note", dtdURI, "note.dtd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<!DOCTYPE foo SYSTEM \"http://example.org/schema/note\">\r\n")));
	}

	// ==========================================
	// Catalog binding: PUBLIC entry
	// ==========================================

	// Catalog entry: <public publicId="-//Example//DTD Foo//EN" uri="note.dtd"/>
	// <foo /> -> <!DOCTYPE foo PUBLIC "-//Example//DTD Foo//EN" "dtd/note.dtd">
	//            <foo />
	@Test
	public void catalogPublicEntryWithDTD() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"public", "-//Example//DTD Foo//EN", dtdURI, "note.dtd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<!DOCTYPE foo PUBLIC \"-//Example//DTD Foo//EN\" \"dtd/note.dtd\">\r\n")));
	}

	// ==========================================
	// Catalog binding: SYSTEM entry
	// ==========================================

	// Catalog entry: <system systemId="http://example.com/foo.dtd" uri="note.dtd"/>
	// <foo /> -> <!DOCTYPE foo SYSTEM "http://example.com/foo.dtd">
	//            <foo />
	@Test
	public void catalogSystemEntryWithDTD() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"system", "http://example.com/foo.dtd", dtdURI, "note.dtd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<!DOCTYPE foo SYSTEM \"http://example.com/foo.dtd\">\r\n")));
	}

	// Catalog entry: <system systemId="http://example.com/tag.xsd" uri="tag.xsd"/>
	// XSD has no targetNamespace
	// <foo /> -> <foo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:noNamespaceSchemaLocation="http://example.com/tag.xsd"/>
	@Test
	public void catalogSystemEntryWithXSDNoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"system", "http://example.com/tag.xsd", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"http://example.com/tag.xsd\"")));
	}

	// Catalog entry: <system systemId="http://example.com/team.xsd" uri="team.xsd"/>
	// XSD has targetNamespace="team_namespace"
	// <foo /> -> <foo xmlns="team_namespace"
	//                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:schemaLocation="team_namespace http://example.com/team.xsd"/>
	@Test
	public void catalogSystemEntryWithXSDWithNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"system", "http://example.com/team.xsd", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace http://example.com/team.xsd\"")));
	}

	// ==========================================
	// Standard binding (non-catalog)
	// ==========================================

	// Standard binding with XSD (no targetNamespace)
	// <foo /> -> <foo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:noNamespaceSchemaLocation="xsd/tag.xsd"/>
	@Test
	public void standardXSDNoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"")));
	}

	// Standard binding with XSD (targetNamespace="team_namespace")
	// <foo /> -> <foo xmlns="team_namespace"
	//                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:schemaLocation="team_namespace xsd/team.xsd"/>
	@Test
	public void standardXSDWithNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace xsd/team.xsd\"")));
	}

	// Standard binding with DTD
	// <foo /> -> <!DOCTYPE foo SYSTEM "dtd/note.dtd">
	//            <foo />
	@Test
	public void standardDTD() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, dtdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<!DOCTYPE foo SYSTEM \"dtd/note.dtd\">\r\n")));
	}

	// xml-model binding with DTD
	// <foo /> -> <?xml-model href="dtd/note.dtd"?>
	//            <foo />
	@Test
	public void standardXmlModelDTD() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, dtdURI,
				GrammarBindingType.XML_MODEL.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<?xml-model href=\"dtd/note.dtd\"?>\r\n")));
	}

	// xml-model binding with XSD (targetNamespace="team_namespace")
	// <foo /> -> <?xml-model href="xsd/team.xsd"?>
	//            <foo xmlns="team_namespace" />
	@Test
	public void standardXmlModelXSDWithNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.XML_MODEL.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, //
				te(1, 0, 1, 0, "<?xml-model href=\"xsd/team.xsd\"?>\r\n"), //
				te(1, 4, 1, 4, " xmlns=\"team_namespace\"")));
	}

	// xml-model binding with XSD (no targetNamespace)
	// <foo /> -> <?xml-model href="xsd/tag.xsd"?>
	//            <foo />
	@Test
	public void standardXmlModelXSDNoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.XML_MODEL.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<?xml-model href=\"xsd/tag.xsd\"?>\r\n")));
	}

	// xml-model binding with RNG: standard binding should NOT add schematypens
	// (only catalog binding adds schematypens for RNG)
	// <foo /> -> <?xml-model href="relaxng/simple.rng"?>
	//            <foo />
	@Test
	public void standardXmlModelRNGNoSchematypens() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String rngURI = getFileURI("src/test/resources/relaxng/simple.rng");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, rngURI,
				GrammarBindingType.XML_MODEL.getName());

		assertNotNull(actual);
		assertEquals(1, actual.getEdits().size());
		String newText = actual.getEdits().get(0).getLeft().getNewText();
		assertEquals(true, newText.contains("xml-model"));
		assertEquals(false, newText.contains("schematypens"),
				"Standard binding should not add schematypens (only catalog binding does)");
	}

	// ==========================================
	// Namespace existence: don't duplicate xmlns
	// ==========================================

	// xmlns="team_namespace" already exists -> only add xsi:schemaLocation (no duplicate xmlns)
	// <foo xmlns="team_namespace" /> -> <foo xmlns="team_namespace"
	//                                        xmlns:xsi="..."
	//                                        xsi:schemaLocation="team_namespace ..." />
	@Test
	public void catalogUriXSDWithExistingNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns=\"team_namespace\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/team", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 27, 1, 27,
				"\r\n    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace http://example.org/schema/team\"")));
	}

	// xmlns:xsi already exists -> only add xsi:noNamespaceSchemaLocation (no duplicate xmlns:xsi)
	// <foo xmlns:xsi="..." /> -> <foo xmlns:xsi="..."
	//                                 xsi:noNamespaceSchemaLocation="..." />
	@Test
	public void catalogUriXSDNoNamespaceWithExistingXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/tag", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				"\r\n    xsi:noNamespaceSchemaLocation=\"http://example.org/schema/tag\"")));
	}

	// xmlns:xsi already exists but xmlns not -> add xmlns + xsi:schemaLocation
	// <foo xmlns:xsi="..." /> -> <foo xmlns="team_namespace" xmlns:xsi="..."
	//                                 xsi:schemaLocation="team_namespace ..." />
	@Test
	public void catalogUriXSDWithNamespaceAndExistingXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/team", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace http://example.org/schema/team\"")));
	}

	// Both xmlns and xmlns:xsi already exist -> only add xsi:schemaLocation
	// <foo xmlns="team_namespace" xmlns:xsi="..." /> -> <foo ... xsi:schemaLocation="..." />
	@Test
	public void catalogUriXSDWithExistingXmlnsAndXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns=\"team_namespace\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/team", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 81, 1, 81,
				"\r\n    xsi:schemaLocation=\"team_namespace http://example.org/schema/team\"")));
	}

	// Standard binding: xmlns:xsi already exists -> only add xsi:noNamespaceSchemaLocation
	// <foo xmlns:xsi="..." /> -> <foo xmlns:xsi="..."
	//                                 xsi:noNamespaceSchemaLocation="xsd/tag.xsd" />
	@Test
	public void standardNoNamespaceSchemaLocationWithExistingXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				"\r\n    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"")));
	}

	// Standard binding: xmlns:xsi already exists -> add xmlns + xsi:schemaLocation
	// <foo xmlns:xsi="..." /> -> <foo xmlns="team_namespace" xmlns:xsi="..."
	//                                 xsi:schemaLocation="team_namespace xsd/team.xsd" />
	@Test
	public void standardSchemaLocationWithExistingXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace xsd/team.xsd\"")));
	}

	// Standard binding: both xmlns and xmlns:xsi exist -> only add xsi:schemaLocation
	// <foo xmlns="team_namespace" xmlns:xsi="..." /> -> <foo ... xsi:schemaLocation="..." />
	@Test
	public void standardSchemaLocationWithExistingXmlnsAndXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns=\"team_namespace\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 81, 1, 81,
				"\r\n    xsi:schemaLocation=\"team_namespace xsd/team.xsd\"")));
	}

	// xml-model binding: xmlns already exists -> only insert xml-model PI (no xmlns added)
	// <foo xmlns="team_namespace" /> -> <?xml-model href="xsd/team.xsd"?>
	//                                   <foo xmlns="team_namespace" />
	@Test
	public void xmlModelWithExistingNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns=\"team_namespace\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.XML_MODEL.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 0,
				"<?xml-model href=\"xsd/team.xsd\"?>\r\n")));
	}

	// ==========================================
	// System catalog entry: namespace existence
	// ==========================================

	// System entry + XSD no namespace: xmlns:xsi already exists -> only add xsi:noNamespaceSchemaLocation
	// <foo xmlns:xsi="..." /> -> <foo xmlns:xsi="..."
	//                                 xsi:noNamespaceSchemaLocation="http://example.com/tag.xsd" />
	@Test
	public void catalogSystemXSDWithExistingXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"system", "http://example.com/tag.xsd", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				"\r\n    xsi:noNamespaceSchemaLocation=\"http://example.com/tag.xsd\"")));
	}

	// System entry + XSD with namespace: both xmlns and xmlns:xsi exist -> only add xsi:schemaLocation
	// <foo xmlns="team_namespace" xmlns:xsi="..." /> -> <foo ... xsi:schemaLocation="..." />
	@Test
	public void catalogSystemXSDWithNamespaceAndExistingXmlnsAndXsi() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo xmlns=\"team_namespace\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"system", "http://example.com/team.xsd", xsdURI, "team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 81, 1, 81,
				"\r\n    xsi:schemaLocation=\"team_namespace http://example.com/team.xsd\"")));
	}

	// ==========================================
	// Edge cases: empty document
	// ==========================================

	// Empty document + catalog DTD: generates DOCTYPE + root element from scratch
	// "" -> <!DOCTYPE root-element SYSTEM "http://example.org/schema/note">
	//       <root-element></root-element>
	@Test
	public void catalogUriDTDWithEmptyDocument() throws Exception {
		String xml = "";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/note", dtdURI, "note.dtd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 0,
				"<!DOCTYPE root-element SYSTEM \"http://example.org/schema/note\">" + ls + //
						"<root-element></root-element>")));
	}

	// Empty document + catalog XSD (no namespace): generates root element with xsi attributes
	// "" -> <root-element xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//            xsi:noNamespaceSchemaLocation="http://example.org/schema/tag"></root-element>
	@Test
	public void catalogUriXSDWithEmptyDocument() throws Exception {
		String xml = "";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/tag", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 0,
				"<root-element" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls + //
						"    xsi:noNamespaceSchemaLocation=\"http://example.org/schema/tag\"></root-element>")));
	}

	// Empty document + standard DTD: generates DOCTYPE + root element
	// "" -> <!DOCTYPE root-element SYSTEM "dtd/note.dtd">
	//       <root-element></root-element>
	@Test
	public void standardDTDWithEmptyDocument() throws Exception {
		String xml = "";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, dtdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 0,
				"<!DOCTYPE root-element SYSTEM \"dtd/note.dtd\">" + ls + //
						"<root-element></root-element>")));
	}

	// ==========================================
	// Edge cases: HTTP targetURI
	// ==========================================

	// Catalog entry with absolute HTTP URI should not be corrupted
	// (regression: Paths.get + expandSystemId was stripping double slash from http://)
	// <foo /> -> <foo xmlns="team_namespace"
	//                 xmlns:xsi="..." xsi:schemaLocation="team_namespace ..."/>
	@Test
	public void catalogUriXSDWithHttpTargetAndNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/team.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/team", xsdURI, "http://example.org/xsd/team.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace http://example.org/schema/team\"")));
	}

	// ==========================================
	// Edge cases: XML without prolog
	// ==========================================

	// No XML prolog + catalog XSD: inserts xsi attributes into existing <foo>
	// <foo /> -> <foo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	//                 xsi:noNamespaceSchemaLocation="http://example.org/schema/tag" />
	@Test
	public void catalogUriXSDNoProlog() throws Exception {
		String xml = "<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/tag", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 4, 0, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls + //
						"    xsi:noNamespaceSchemaLocation=\"http://example.org/schema/tag\"")));
	}

	// No XML prolog + catalog RNG: inserts xml-model PI before <foo>
	// <foo /> -> <?xml-model href="..." schematypens="..."?>
	//            <foo />
	@Test
	public void catalogUriRNGNoProlog() throws Exception {
		String xml = "<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String rngURI = getFileURI("src/test/resources/relaxng/simple.rng");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/simple", rngURI, "simple.rng");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 0,
				"<?xml-model href=\"http://example.org/schema/simple\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>" + ls)));
	}

	// No XML prolog + standard DTD: inserts DOCTYPE before <foo>
	// <foo /> -> <!DOCTYPE foo SYSTEM "dtd/note.dtd">
	//            <foo />
	@Test
	public void standardDTDNoProlog() throws Exception {
		String xml = "<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, dtdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 0,
				"<!DOCTYPE foo SYSTEM \"dtd/note.dtd\">" + ls)));
	}

	// ==========================================
	// Edge cases: XML with just "<"
	// ==========================================

	// Just "<" (incomplete tag) + catalog XSD: replaces orphan "<" with root element
	// < -> <root-element xmlns:xsi="..." xsi:noNamespaceSchemaLocation="..."></root-element>
	@Test
	public void catalogUriXSDWithJustLessThan() throws Exception {
		String xml = "<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/tag", xsdURI, "tag.xsd");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 1,
				"<root-element" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls + //
						"    xsi:noNamespaceSchemaLocation=\"http://example.org/schema/tag\"></root-element>")));
	}

	// Just "<" + standard XSD: replaces orphan "<" with root element
	// < -> <root-element xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd"></root-element>
	@Test
	public void standardXSDWithJustLessThan() throws Exception {
		String xml = "<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String xsdURI = getFileURI("src/test/resources/xsd/tag.xsd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, xsdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 1,
				"<root-element" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + ls + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"></root-element>")));
	}

	// Just "<" + standard DTD: replaces orphan "<" with DOCTYPE + root element
	// < -> <!DOCTYPE root-element SYSTEM "dtd/note.dtd">
	//      <root-element></root-element>
	@Test
	public void standardDTDWithJustLessThan() throws Exception {
		String xml = "<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String dtdURI = getFileURI("src/test/resources/dtd/note.dtd");

		TextDocumentEdit actual = executeStandardBinding(xml, xmlURI, dtdURI,
				GrammarBindingType.STANDARD.getName());

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(0, 0, 0, 1,
				"<!DOCTYPE root-element SYSTEM \"dtd/note.dtd\">" + ls + //
						"<root-element></root-element>")));
	}

	// Just "<" + catalog RNG: replaces orphan "<" with xml-model PI + root element
	// < -> <?xml-model href="..." schematypens="..."?>
	//      <root-element></root-element>
	@Test
	public void catalogUriRNGWithJustLessThan() throws Exception {
		String xml = "<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		String rngURI = getFileURI("src/test/resources/relaxng/simple.rng");

		TextDocumentEdit actual = executeCatalogBinding(xml, xmlURI,
				"uri", "http://example.org/schema/simple", rngURI, "simple.rng");

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, //
				te(0, 0, 0, 0,
						"<?xml-model href=\"http://example.org/schema/simple\" schematypens=\"http://relaxng.org/ns/structure/1.0\"?>" + ls), //
				te(0, 0, 0, 1,
						"<root-element></root-element>")));
	}

	// ==========================================
	// Root element from grammar: single element
	// ==========================================

	// Grammar declares single root "tag": uses "tag" instead of "root-element" placeholder
	// <?xml version="1.0"?>
	// < -> <?xml version="1.0"?>
	//      <tag xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd"></tag>
	@Test
	public void rootElementSingleWithXSINoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = new SharedSettings();
		List<String> rootElementNames = Collections.singletonList("tag");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, false);

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 1,
				"<tag" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"></tag>")));
	}

	// Grammar declares single root "note": uses "note" in DOCTYPE
	// <?xml version="1.0"?>
	// < -> <?xml version="1.0"?>
	//      <!DOCTYPE note SYSTEM "dtd/note.dtd">
	//      <note></note>
	@Test
	public void rootElementSingleWithDocType() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = new SharedSettings();
		List<String> rootElementNames = Collections.singletonList("note");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createDocTypeEdit(
				"dtd/note.dtd", document, sharedSettings, rootElementNames, false);

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 1,
				"<!DOCTYPE note SYSTEM \"dtd/note.dtd\">\r\n" + //
						"<note></note>")));
	}

	// Grammar declares single root "config": uses "config" in xml-model + element
	// <?xml version="1.0"?>
	// < -> <?xml version="1.0"?>
	//      <?xml-model href="schema.rng"?>
	//      <config></config>
	@Test
	public void rootElementSingleWithXmlModel() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = new SharedSettings();
		List<String> rootElementNames = Collections.singletonList("config");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXmlModelEdit(
				"schema.rng", null, null, document, sharedSettings, rootElementNames, false);

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, //
				te(1, 0, 1, 0,
						"<?xml-model href=\"schema.rng\"?>\r\n"), //
				te(1, 0, 1, 1,
						"<config></config>")));
	}

	// ==========================================
	// Root element: existing document element
	// ==========================================

	// Document already has <foo>: root element names from grammar are ignored,
	// existing tag name "foo" is kept
	// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd" />
	@Test
	public void rootElementWithExistingDocumentElement() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = new SharedSettings();
		List<String> rootElementNames = Collections.singletonList("tag");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, false);

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"")));
	}

	// ==========================================
	// Snippet support: single element with cursor
	// ==========================================

	// Snippet: single root "tag" with $0 cursor inside element
	// < -> <tag xmlns:xsi="..." xsi:noNamespaceSchemaLocation="...">$0</tag>
	@Test
	public void snippetSingleElementWithXSINoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Collections.singletonList("tag");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<tag" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">$0</tag>")));
	}

	// Snippet: single root "note" with DOCTYPE and $0 cursor
	// < -> <!DOCTYPE note SYSTEM "dtd/note.dtd">
	//      <note>$0</note>
	@Test
	public void snippetSingleElementWithDocType() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Collections.singletonList("note");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createDocTypeEdit(
				"dtd/note.dtd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<!DOCTYPE note SYSTEM \"dtd/note.dtd\">\r\n" + //
						"<note>$0</note>")));
	}

	// Snippet: single root "config" with xml-model PI and $0 cursor
	// < -> <?xml-model href="schema.rng"?>
	//      <config>$0</config>
	@Test
	public void snippetSingleElementWithXmlModel() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Collections.singletonList("config");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXmlModelEdit(
				"schema.rng", null, null, document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, //
				sne(1, 0, 1, 0,
						"<?xml-model href=\"schema.rng\"?>\r\n"), //
				sne(1, 0, 1, 1,
						"<config>$0</config>")));
	}

	// ==========================================
	// Snippet support: multiple elements with choice
	// ==========================================

	// Snippet: multiple roots ["person","team"] -> choice placeholder ${1|person,team|}
	// < -> <${1|person,team|} xmlns:xsi="..." xsi:noNamespaceSchemaLocation="...">$0</$1>
	@Test
	public void snippetMultipleElementsWithXSINoNamespace() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Arrays.asList("person", "team");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<${1|person,team|}" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">$0</$1>")));
	}

	// Snippet: multiple roots ["note","email"] in DOCTYPE -> choice in DOCTYPE name and element
	// < -> <!DOCTYPE ${1|note,email|} SYSTEM "dtd/note.dtd">
	//      <$1>$0</$1>
	@Test
	public void snippetMultipleElementsWithDocType() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Arrays.asList("note", "email");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createDocTypeEdit(
				"dtd/note.dtd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<!DOCTYPE ${1|note,email|} SYSTEM \"dtd/note.dtd\">\r\n" + //
						"<$1>$0</$1>")));
	}

	// Snippet: multiple roots ["config","settings"] with xml-model PI
	// < -> <?xml-model href="schema.rng"?>
	//      <${1|config,settings|}>$0</$1>
	@Test
	public void snippetMultipleElementsWithXmlModel() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Arrays.asList("config", "settings");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXmlModelEdit(
				"schema.rng", null, null, document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, //
				sne(1, 0, 1, 0,
						"<?xml-model href=\"schema.rng\"?>\r\n"), //
				sne(1, 0, 1, 1,
						"<${1|config,settings|}>$0</$1>")));
	}

	// Snippet: multiple roots ["team","player"] with schemaLocation (XSD has namespace)
	// < -> <${1|team,player|} xmlns="team_namespace"
	//           xmlns:xsi="..." xsi:schemaLocation="team_namespace xsd/team.xsd">$0</$1>
	@Test
	public void snippetMultipleElementsWithSchemaLocation() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Arrays.asList("team", "player");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(
				"xsd/team.xsd", "team_namespace", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<${1|team,player|}" + //
						" xmlns=\"team_namespace\"\r\n" + //
						"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:schemaLocation=\"team_namespace xsd/team.xsd\">$0</$1>")));
	}

	// ==========================================
	// Snippet with forceAligned split attributes
	// ==========================================

	// Snippet: multiple roots with forceAligned -> indentation based on first choice length
	// < -> <${1|person,team|} xmlns:xsi="..."
	//              xsi:noNamespaceSchemaLocation="xsd/tag.xsd">$0</$1>
	@Test
	public void snippetMultipleElementsWithXSINoNamespaceAligned() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		sharedSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);
		List<String> rootElementNames = Arrays.asList("person", "team");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<${1|person,team|}" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"        xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\">$0</$1>")));
	}

	// Snippet: multiple roots with forceAligned + schemaLocation
	// < -> <${1|team,player|} xmlns="team_namespace"
	//            xmlns:xsi="..." xsi:schemaLocation="...">$0</$1>
	@Test
	public void snippetMultipleElementsWithSchemaLocationAligned() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		sharedSettings.getFormattingSettings().setSplitAttributes(SplitAttributes.forceAligned);
		List<String> rootElementNames = Arrays.asList("team", "player");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSISchemaLocationEdit(
				"xsd/team.xsd", "team_namespace", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		assertEquals(actual, tdeSnippet(xmlURI, 0, sne(1, 0, 1, 1,
				"<${1|team,player|}" + //
						" xmlns=\"team_namespace\"\r\n" + //
						"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"      xsi:schemaLocation=\"team_namespace xsd/team.xsd\">$0</$1>")));
	}

	// ==========================================
	// Snippet with existing document element (no snippet effect)
	// ==========================================

	// Snippet support enabled but document has existing <foo>: snippet placeholders
	// are NOT used because the existing element name is kept
	// <foo /> -> <foo xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd" />
	@Test
	public void snippetWithExistingDocumentElement() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<foo />";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = XMLAssert.createSnippetEditSettings();
		List<String> rootElementNames = Arrays.asList("person", "team");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, true);

		assertNotNull(actual);
		// Snippet should NOT apply when document element exists
		assertEquals(actual, tde(xmlURI, 0, te(1, 4, 1, 4,
				" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"")));
	}

	// ==========================================
	// No snippet support: multiple elements uses first
	// ==========================================

	// No snippet support + multiple roots ["person","team"]: uses first name "person"
	// < -> <person xmlns:xsi="..." xsi:noNamespaceSchemaLocation="xsd/tag.xsd"></person>
	@Test
	public void noSnippetMultipleElementsUsesFirst() throws Exception {
		String xml = "<?xml version=\"1.0\"?>\r\n" + //
				"<";
		String xmlURI = getFileURI("src/test/resources/tag.xml");
		DOMDocument document = DOMParser.getInstance().parse(xml, xmlURI, null);
		SharedSettings sharedSettings = new SharedSettings();
		List<String> rootElementNames = Arrays.asList("person", "team");

		TextDocumentEdit actual = NoGrammarConstraintsCodeAction.createXSINoNamespaceSchemaLocationEdit(
				"xsd/tag.xsd", document, sharedSettings, rootElementNames, false);

		assertNotNull(actual);
		assertEquals(actual, tde(xmlURI, 0, te(1, 0, 1, 1,
				"<person" + //
						" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
						"    xsi:noNamespaceSchemaLocation=\"xsd/tag.xsd\"></person>")));
	}

	// --- Helper methods ---

	private static TextDocumentEdit executeCatalogBinding(String xmlContent, String xmlURI,
			String entryType, String identifier, String targetURI, String uriValue) throws Exception {
		DOMDocument document = DOMParser.getInstance().parse(xmlContent, xmlURI, null);
		IXMLDocumentProvider documentProvider = uri -> {
			if (xmlURI.equals(uri)) {
				return document;
			}
			return null;
		};
		AssociateGrammarCommand command = new AssociateGrammarCommand(documentProvider);
		ExecuteCommandParams params = new ExecuteCommandParams(
				AssociateGrammarCommand.COMMAND_ID,
				Arrays.asList(
						new TextDocumentIdentifier(xmlURI),
						entryType,
						GrammarBindingType.CATALOG.getName(),
						identifier,
						targetURI,
						uriValue));
		SharedSettings sharedSettings = new SharedSettings();
		return (TextDocumentEdit) command.executeCommand(params, sharedSettings, NULL_CHECKER);
	}

	private static TextDocumentEdit executeStandardBinding(String xmlContent, String xmlURI,
			String grammarURI, String bindingType) throws Exception {
		DOMDocument document = DOMParser.getInstance().parse(xmlContent, xmlURI, null);
		IXMLDocumentProvider documentProvider = uri -> {
			if (xmlURI.equals(uri)) {
				return document;
			}
			return null;
		};
		AssociateGrammarCommand command = new AssociateGrammarCommand(documentProvider);
		ExecuteCommandParams params = new ExecuteCommandParams(
				AssociateGrammarCommand.COMMAND_ID,
				Arrays.asList(
						new TextDocumentIdentifier(xmlURI),
						grammarURI,
						bindingType));
		SharedSettings sharedSettings = new SharedSettings();
		return (TextDocumentEdit) command.executeCommand(params, sharedSettings, NULL_CHECKER);
	}

	private static String getFileURI(String fileName) {
		String uri = new File(fileName).toURI().toString();
		boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
		if (isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
