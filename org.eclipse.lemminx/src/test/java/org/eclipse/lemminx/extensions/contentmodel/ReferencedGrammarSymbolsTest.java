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

import static org.eclipse.lemminx.XMLAssert.ds;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testDocumentSymbolsFor;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLFileAssociation;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.XMLSymbolSettings;
import org.eclipse.lemminx.uriresolver.CacheResourcesManager;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.jupiter.api.Test;

/**
 * Referenced grammar symbols test.
 */
public class ReferencedGrammarSymbolsTest extends AbstractCacheBasedTest {

	@Test
	public void withDoctype() throws BadLocationException, MalformedURIException {
		String dtdFileURI = getDTDFileURI("src/test/resources/dtd/entities/base.dtd");
		String xml = "<!DOCTYPE root-element SYSTEM \"dtd/entities/base.dtd\">\r\n" + //
				"<root-element>\r\n" + //
				"</root-element>";
		testDocumentSymbolsFor(xml, "src/test/resources/base.xml", //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(dtdFileURI, SymbolKind.File, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: doctype", SymbolKind.Property, r(0, 30, 0, 53),
														r(0, 30, 0, 53), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("DOCTYPE:root-element", SymbolKind.Struct, r(0, 0, 0, 54), r(0, 0, 0, 54), null, Arrays.asList()), //
				ds("root-element", SymbolKind.Field, r(1, 0, 2, 15), r(1, 0, 2, 15), null, Arrays.asList()) //
		);
	}

	@Test
	public void withNoNamespaceSchemaLocation() throws BadLocationException, MalformedURIException {
		String xml = "<Configuration\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xsi:noNamespaceSchemaLocation=\"xsd/Format.xsd\">\r\n" + //
				"</Configuration>";
		testDocumentSymbolsFor(xml, "src/test/resources/Test.Format.ps1xml", //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(getXMLSchemaFileURI("Format.xsd", false), SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: xsi:noNamespaceSchemaLocation", SymbolKind.Property,
														r(2, 34, 2, 50), r(2, 34, 2, 50), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("Configuration", SymbolKind.Field, r(0, 0, 3, 16), r(0, 0, 3, 16), null, Arrays.asList()) //
		);
	}

	@Test
	public void withSchemaLocation() throws BadLocationException, MalformedURIException {
		String xml = "<team\r\n" + //
				"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"    xmlns:xsi=\"team_namespace\"\r\n" + //
				"    xsi:schemaLocation=\"team_namespace xsd/team.xsd\">\r\n" + //
				"</team>";
		testDocumentSymbolsFor(xml, "src/test/resources/team.xml", //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(getXMLSchemaFileURI("team.xsd", false), SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: xsi:schemaLocation", SymbolKind.Property, r(3, 39, 3, 51),
														r(3, 39, 3, 51), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("team", SymbolKind.Field, r(0, 0, 4, 7), r(0, 0, 4, 7), null, Arrays.asList()) //
		);
	}

	@Test
	public void withXMLModel() throws BadLocationException, MalformedURIException {
		String xml = "<?xml-model href=\"xsd/Format.xsd\" ?>\r\n" + //
				"<Configuration>\r\n" + //
				"</Configuration>";
		testDocumentSymbolsFor(xml, "src/test/resources/Test.Format.ps1xml", //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(getXMLSchemaFileURI("Format.xsd", false), SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: xml-model", SymbolKind.Property, r(0, 17, 0, 33),
														r(0, 17, 0, 33), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("xml-model", SymbolKind.Property, r(0, 0, 0, 36), r(0, 0, 0, 36), null, Arrays.asList()), //
				ds("Configuration", SymbolKind.Field, r(1, 0, 2, 16), r(1, 0, 2, 16), null, Arrays.asList()) //
		);
	}

	@Test
	public void withFileAssociation() throws BadLocationException, MalformedURIException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setFileAssociations(createXSDAssociations("src/test/resources/xsd/"));
		};

		String xml = "<Configuration></Configuration>";
		testDocumentSymbolsFor(xml, "file:///test/Test.Format.ps1xml", //
				new XMLSymbolSettings(), //
				configuration, //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(getXMLSchemaFileURI("Format.xsd", false), SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: file association", SymbolKind.Property, r(0, 0, 0, 1),
														r(0, 0, 0, 1), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("Configuration", SymbolKind.Field, r(0, 0, 0, 31), r(0, 0, 0, 31), null, Arrays.asList()) //
		);
	}

	@Test
	public void withCache() throws BadLocationException, IOException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setUseCache(true);
		};

		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"</project>";
		String cachedResolvedUri = CacheResourcesManager
				.getResourceCachePath("http://maven.apache.org/xsd/maven-4.0.0.xsd").toUri().toString();
		testDocumentSymbolsFor(xml, "pom.xml", //
				new XMLSymbolSettings(), //
				configuration, //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds("http://maven.apache.org/xsd/maven-4.0.0.xsd", SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: xsi:schemaLocation", SymbolKind.Property, r(2, 55, 2, 98),
														r(2, 55, 2, 98), null, null), //
												ds("Cache", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
														Arrays.asList( //
																ds(cachedResolvedUri, SymbolKind.File, r(0, 0, 0, 1),
																		r(0, 0, 0, 1), null, null))

												))))), //

				// XML symbols
				ds("project", SymbolKind.Field, r(0, 0, 3, 10), r(0, 0, 3, 10), null, Arrays.asList()) //
		);

	}

	@Test
	public void withCatalog() throws BadLocationException, MalformedURIException {
		Consumer<XMLLanguageService> configuration = ls -> {
			ContentModelManager contentModelManager = ls.getComponent(ContentModelManager.class);
			contentModelManager.setUseCache(true);
			contentModelManager.setCatalogs(new String[] { "src/test/resources/catalogs/catalog.xml" });
		};

		String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n" + //
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + //
				"	xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\r\n"
				+ //
				"</project>";
		testDocumentSymbolsFor(xml, "pom.xml", //
				new XMLSymbolSettings(), //
				configuration, //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(getXMLSchemaFileURI("maven-4.0.0.xsd", true), SymbolKind.File, r(0, 0, 0, 1),
										r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: xsi:schemaLocation with catalog", SymbolKind.Property,
														r(2, 55, 2, 98), r(2, 55, 2, 98), null, null), //
												ds("Cache: false", SymbolKind.Property, r(0, 0, 0, 1), r(0, 0, 0, 1),
														null, null))))), //

				// XML symbols
				ds("project", SymbolKind.Field, r(0, 0, 3, 10), r(0, 0, 3, 10), null, Arrays.asList()) //
		);

	}

	@Test
	public void withSymbolLimit() throws BadLocationException, MalformedURIException {
		XMLSymbolSettings settings = new XMLSymbolSettings();
		settings.setMaxItemsComputed(3);

		String dtdFileURI = getDTDFileURI("src/test/resources/dtd/entities/base.dtd");
		String xml = "<!DOCTYPE root-element SYSTEM \"dtd/entities/base.dtd\">\r\n" + //
				"<root-element>\r\n" + //
				"</root-element>";
		testDocumentSymbolsFor(xml, "src/test/resources/base.xml", settings, //
				// Referenced grammar symbols
				ds("Grammars", SymbolKind.Module, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
						Arrays.asList( //
								ds(dtdFileURI, SymbolKind.File, r(0, 0, 0, 1), r(0, 0, 0, 1), null, //
										Arrays.asList( //
												ds("Binding: doctype", SymbolKind.Property, r(0, 30, 0, 53),
														r(0, 30, 0, 53), null, null))))) //
		);
	}

	private static XMLFileAssociation[] createXSDAssociations(String baseSystemId) {
		XMLFileAssociation format = new XMLFileAssociation();
		format.setPattern("**/*.Format.ps1xml");
		format.setSystemId(baseSystemId + "Format.xsd");
		XMLFileAssociation resources = new XMLFileAssociation();
		resources.setPattern("**/*resources*.xml");
		resources.setSystemId(baseSystemId + "resources.xsd");
		return new XMLFileAssociation[] { format, resources };
	}

	private static String getXMLSchemaFileURI(String schemaURI, boolean replaceSlash) throws MalformedURIException {
		String result = XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true);
		if (replaceSlash) {
			return result.replace("///", "/");
		}
		return result;
	}

	private static String getDTDFileURI(String dtdURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId(dtdURI, "test.xml", true); // .replace("///", "/");
	}
}
