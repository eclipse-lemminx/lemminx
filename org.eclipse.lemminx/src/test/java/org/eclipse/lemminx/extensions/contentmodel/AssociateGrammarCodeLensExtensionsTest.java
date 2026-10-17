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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.cl;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCodeLensFor;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_BINDING_WIZARD;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_URI;
import static org.eclipse.lemminx.client.ClientCommands.UPDATE_CONFIGURATION;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLFileAssociation;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.utils.FilesUtils;
import org.junit.jupiter.api.Test;

/**
 * Associate grammar codelens tests
 *
 */
public class AssociateGrammarCodeLensExtensionsTest extends AbstractCacheBasedTest {

	// Codelens for Binding

	@Test
	public void noGrammarWithAssociationSupportInXML() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				Collections.singletonList(CodeLensKind.Association), //
				cl(r(1, 1, 1, 4), "Bind to grammar/schema...", OPEN_BINDING_WIZARD));
	}

	@Test
	public void noGrammarWithAssociationSupportInXSD() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<schema />";
		testCodeLensFor(xml, "test.xsd", //
				Collections.singletonList(CodeLensKind.Association));
	}

	@Test
	public void noGrammarWithAssociationSupportInDTD() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.dtd", //
				Collections.singletonList(CodeLensKind.Association));
	}

	@Test
	public void noGrammarWithoutAssociationSupport() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				Collections.emptyList());
	}

	@Test
	public void withGrammar() throws BadLocationException {
		String xml = "<?xml-model href='test.dtd' ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				cl(r(0, 0, 0, 0), "test.dtd (xml-model)", OPEN_URI));
	}

	// Codelens for referenced grammar

	@Test
	public void referencedGrammarWithXMlModel() throws BadLocationException {
		String xml = "<?xml-model href='test.dtd' ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				cl(r(0, 0, 0, 0), "test.dtd (xml-model)", OPEN_URI));
	}

	@Test
	public void referencedGrammarWithXMlModelWithoutOpenUriSupport() throws BadLocationException {
		String xml = "<?xml-model href='test.dtd' ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				Collections.emptyList(), //
				cl(r(0, 0, 0, 0), "test.dtd (xml-model)", "")); // <-- command id is empty here
	}

	@Test
	public void referencedGrammarWith2XMlModel() throws BadLocationException {
		String xml = "<?xml-model href='test.dtd' ?>\r\n" + //
				"<?xml-model href='test2.dtd' ?>\r\n" + //
				"<foo />";
		testCodeLensFor(xml, "test.xml", //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "test.dtd (xml-model)", OPEN_URI),
				//
				cl(r(0, 0, 0, 0), "test2.dtd (xml-model)", OPEN_URI));
	}

	@Test
	public void referencedGrammarWithXSINoNamespaceSchemaLocation() throws BadLocationException {
		String xml = "<foo\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"test.xsd\">\r\n" + //
				"</root>";
		testCodeLensFor(xml, "test.xml", //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "test.xsd (xsi:noNamespaceSchemaLocation)", OPEN_URI));
	}

	@Test
	public void referencedGrammarWithXSISchemaLocation() throws BadLocationException {
		String xml = "<foo\r\n" + //
				"    xmlns=\"http://foo\"\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:schemaLocation=\"http://foo test.xsd\">\r\n" + //
				"</root>";
		testCodeLensFor(xml, "test.xml", //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "test.xsd (xsi:schemaLocation)", OPEN_URI));
	}

	@Test
	public void referencedGrammarWithDOCTYPE() throws BadLocationException {
		String xml = "<!DOCTYPE foo SYSTEM \"test.dtd\">\r\n" + //
				"<foo>\r\n" + //
				"</foo>";
		testCodeLensFor(xml, "test.xml", //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "test.dtd (doctype)", OPEN_URI));
	}

	@Test
	public void referencedGrammarInXSDSchema() throws BadLocationException {
		String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n" + //
				"</xs:schema>";
		testCodeLensFor(xsd, "test.xsd", //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "http://www.w3.org/2001/XMLSchema (embedded xml.xsd)", OPEN_URI));
	}

	@Test
	public void referencedGrammarUsingFileAssociation() throws BadLocationException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use root URI which ends with slash
			contentModelManager.setRootURI("src/test/resources/xsd/");
			contentModelManager.setFileAssociations(createXSDAssociationsNoNamespaceSchemaLocationLike(""));
		};

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"  <Bad-Root></Bad-Root>";

		testCodeLensFor(xml, "file:///test/resources.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "resources.xsd (file association)", OPEN_URI));
	}

	@Test
	public void referencedGrammarUsingFileAssociationEmptyDocument() throws BadLocationException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			// Use root URI which ends with slash
			contentModelManager.setRootURI("src/test/resources/xsd/");
			contentModelManager.setFileAssociations(createXSDAssociationsNoNamespaceSchemaLocationLike(""));
		};

		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

		testCodeLensFor(xml, "file:///test/resources.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "resources.xsd (file association)", OPEN_URI));

		xml = "";

		testCodeLensFor(xml, "file:///test/resources.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "resources.xsd (file association)", OPEN_URI));
	}

	public void unregisterCatalog() throws BadLocationException, IOException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setCatalogs(new String[] { "src/test/resources/catalogs/catalog.xml" });
		};

		String xml = FilesUtils.readString(FilesUtils.getPath("src/test/resources/catalogs/catalog.xml"));

		testCodeLensFor(xml, "src/test/resources/catalogs/catalog.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "urn:oasis:names:tc:entity:xmlns:xml:catalog (embedded catalog.xsd)", OPEN_URI),
				cl(r(1, 1, 1, 8), "Unregister Catalog", UPDATE_CONFIGURATION));
	}

	@Test
	public void registerCatalogEmptyCatalog() throws BadLocationException, IOException {

		String xml = FilesUtils.readString(FilesUtils.getPath("src/test/resources/catalogs/catalog.xml"));

		testCodeLensFor(xml, "src/test/resources/catalogs/catalog.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				cl(r(0, 0, 0, 0), "urn:oasis:names:tc:entity:xmlns:xml:catalog (embedded catalog.xsd)", OPEN_URI),
				cl(r(1, 1, 1, 8), "Register Catalog", UPDATE_CONFIGURATION));
	}

	@Test
	public void registerCatalogWithOtherExistingCatalog() throws BadLocationException, IOException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setCatalogs(new String[] { "src/test/resources/catalogs/catalog-public.xml" });
		};

		String xml = FilesUtils.readString(FilesUtils.getPath("src/test/resources/catalogs/catalog.xml"));

		testCodeLensFor(xml, "src/test/resources/catalogs/catalog.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "urn:oasis:names:tc:entity:xmlns:xml:catalog (embedded catalog.xsd)", OPEN_URI),
				cl(r(1, 1, 1, 8), "Register Catalog", UPDATE_CONFIGURATION));
	}

	@Test
	public void unregisterCatalogWithExistingCatalog() throws BadLocationException, IOException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setCatalogs(new String[] { //
					"src/test/resources/catalogs/catalog-public.xml", //
					"src/test/resources/catalogs/catalog.xml" });
		};

		String xml = FilesUtils.readString(FilesUtils.getPath("src/test/resources/catalogs/catalog.xml"));

		testCodeLensFor(xml, "src/test/resources/catalogs/catalog.xml", new XMLLanguageService(), //
				Collections.singletonList(CodeLensKind.OpenUri), //
				configuration, //
				cl(r(0, 0, 0, 0), "urn:oasis:names:tc:entity:xmlns:xml:catalog (embedded catalog.xsd)", OPEN_URI),
				cl(r(1, 1, 1, 8), "Unregister Catalog", UPDATE_CONFIGURATION));
	}

	private static XMLFileAssociation[] createXSDAssociationsNoNamespaceSchemaLocationLike(String baseSystemId) {
		XMLFileAssociation resources = new XMLFileAssociation();
		resources.setPattern("**/*resources*.xml");
		resources.setSystemId(baseSystemId + "resources.xsd");
		return new XMLFileAssociation[] { resources };
	}
}
