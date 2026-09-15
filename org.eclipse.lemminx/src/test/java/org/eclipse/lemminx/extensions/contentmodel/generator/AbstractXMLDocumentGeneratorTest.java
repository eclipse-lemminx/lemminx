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
package org.eclipse.lemminx.extensions.contentmodel.generator;

import java.io.File;
import java.util.concurrent.ExecutionException;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageServer;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLGenerationSettings;
import org.eclipse.lemminx.extensions.contentmodel.model.CMDocument;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.platform.Platform;

/**
 * Base test class for {@link XMLDocumentGenerator} tests.
 *
 * <p>
 * Provides helper methods to load grammars and generate XML documents. Each
 * concrete test class focuses on a specific grammar type (XSD, DTD, RNG, RNC).
 * </p>
 */
public abstract class AbstractXMLDocumentGeneratorTest extends AbstractCacheBasedTest {

	protected static final String ls = System.lineSeparator();

	/**
	 * Generates an XML document from the given grammar file and root element name.
	 *
	 * @param grammarPath     the relative path to the grammar file (e.g.,
	 *                        "src/test/resources/generator/xsd/simpleSequence.xsd").
	 * @param rootElementName the local name of the root element to generate.
	 * @return the generated XML document as a string.
	 */
	protected String generate(String grammarPath, String rootElementName) {
		String grammarURI = getFileURI(grammarPath);
		return generateFromURI(grammarURI, rootElementName);
	}

	/**
	 * Generates an XML document from the given grammar URI and root element name.
	 *
	 * <p>
	 * Uses {@link MockXMLLanguageServer} to ensure all content model providers
	 * (XSD, DTD, RelaxNG, RNC) are properly registered. Loads the grammar
	 * asynchronously via {@link ContentModelManager#loadCMDocumentAsync} then
	 * generates the XML using {@link XMLDocumentGenerator}.
	 * </p>
	 *
	 * @param grammarURI      the grammar file URI.
	 * @param rootElementName the local name of the root element to generate.
	 * @return the generated XML document as a string.
	 */
	protected String generateFromURI(String grammarURI, String rootElementName) {
		return generateFromURI(grammarURI, rootElementName, createDefaultSharedSettings());
	}

	protected String generateFromURI(String grammarURI, String rootElementName, SharedSettings sharedSettings) {
		XMLGenerationSettings settings = new XMLGenerationSettings();
		settings.setOptionalElements(true);
		return generateFromURI(grammarURI, rootElementName, sharedSettings, settings);
	}

	protected String generateFromURI(String grammarURI, String rootElementName,
			XMLGenerationSettings generationSettings) {
		return generateFromURI(grammarURI, rootElementName, createDefaultSharedSettings(), generationSettings);
	}

	private static SharedSettings createDefaultSharedSettings() {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setMaxLineWidth(0);
		return settings;
	}

	protected String generateFromURI(String grammarURI, String rootElementName, SharedSettings sharedSettings,
			XMLGenerationSettings generationSettings) {
		MockXMLLanguageServer server = new MockXMLLanguageServer();
		server.getXMLLanguageService().initializeIfNeeded();
		ContentModelManager contentModelManager = server.getXMLLanguageService()
				.getComponent(ContentModelManager.class);

		CMDocument cmDocument;
		try {
			cmDocument = contentModelManager.loadCMDocumentAsync(grammarURI).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

		XMLDocumentGenerator generator = new XMLDocumentGenerator(sharedSettings, generationSettings);
		return generator.generate(cmDocument, grammarURI, rootElementName);
	}

	/**
	 * Converts a relative file path to a file URI.
	 *
	 * @param relativePath the relative path to the file.
	 * @return the file URI string.
	 */
	protected static String getFileURI(String relativePath) {
		String uri = new File(relativePath).toURI().toString();
		if (Platform.isWindows && !uri.startsWith("file://")) {
			uri = uri.replace("file:/", "file:///");
		}
		return uri;
	}
}
