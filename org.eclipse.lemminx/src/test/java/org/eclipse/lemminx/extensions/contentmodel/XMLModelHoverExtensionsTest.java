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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.r;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

/**
 * XML hover tests with xml-model.
 *
 */
public class XMLModelHoverExtensionsTest extends AbstractCacheBasedTest {

	private static final String HOVER_SEPARATOR = "___";

	@Test
	public void hoverBasedOnXSDWithXMLModel() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURINormalize("spring-beans-3.0.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + //
				"<?xml-model href=\"http://www.springframework.org/schema/beans/spring-beans.xsd\"?>\r\n" + //
				"<beans xmlns=\"http://www.springframework.org/schema/beans\" >\r\n" + //
				"	<bea|n>";
		assertHover(xml,
				"Defines a single (usually named) bean. A bean definition may contain nested tags for constructor arguments, property values, lookup methods, and replaced methods. Mixing constructor injection and setter injection on the same bean is explicitly supported."
						+ //
						System.lineSeparator() + //
						System.lineSeparator() + "Source: [spring-beans-3.0.xsd](" + schemaURI + ")",
				r(3, 2, 3, 6));
	}

	@Test
	public void multipleXMLModel() throws BadLocationException, MalformedURIException {
		String dtdURI = getXMLModelFileURI("grammar.dtd");
		String schemaURI = getXMLModelFileURI("grammar.xsd");
		String xml = "<?xml-model href=\"grammar.dtd\" ?>\r\n" + //
				"<?xml-model href=\"grammar.xsd\" ?>\r\n" + //
				"<grammar>\r\n" + //
				"	<f|rom-xsd/>" + //
				"</grammar>";
		assertHover(xml, "DTD documentation for from-xsd" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Source: [grammar.dtd](" + dtdURI + ")" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				HOVER_SEPARATOR + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"XSD documentation for from-xsd" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Source: [grammar.xsd](" + schemaURI + ")", //
				r(3, 2, 3, 10), //
				"src/test/resources/xml-model/grammar.xml");
	}

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		assertHover(value, expectedHoverLabel, expectedHoverRange, null);
	}

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange, String fileURI)
			throws BadLocationException {
		XMLAssert.assertHover(new XMLLanguageService(), value, "src/test/resources/catalogs/catalog.xml", fileURI,
				expectedHoverLabel, expectedHoverRange);
	}

	private static String getXMLSchemaFileURINormalize(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true).replaceAll("file:///", "file:/");
	}	
	
	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true);
	}

	private static String getXMLModelFileURI(String grammarURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId(grammarURI, "src/test/resources/xml-model/grammar.xml", true);
	}
}
