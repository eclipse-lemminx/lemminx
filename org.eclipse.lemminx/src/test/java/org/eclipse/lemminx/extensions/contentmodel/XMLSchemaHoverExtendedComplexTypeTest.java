/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.r;

import java.util.Arrays;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.settings.SchemaDocumentationType;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

public class XMLSchemaHoverExtendedComplexTypeTest extends AbstractCacheBasedTest {

	@Test
	public void testHoverComplexTypeDocumentation() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("extendedComplexType.xsd");

		String xml = "<t|estType\n" +
				"	xmlns=\"http://extendedComplexType\"\n" +
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"	xsi:schemaLocation=\"http://extendedComplexType xsd/extendedComplexType.xsd\">\n" +
				"</testType>\n";

		String expected = "base type documentation value" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Source: [extendedComplexType.xsd](" + schemaURI + ")";

		assertHover(xml, expected, r(0, 1, 0, 9));
	}

	@Test
	public void testHoverExtendedComplexTypeDocumentation() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("extendedComplexType.xsd");

		String xml = "<e|xtendedTestType\n" +
				"	xmlns=\"http://extendedComplexType\"\n" +
				"	xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				"	xsi:schemaLocation=\"http://extendedComplexType xsd/extendedComplexType.xsd\">\n" +
				"</extendedTestType>\n";

		String expected = "extending type documentation value" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"base type documentation value" + //
				System.lineSeparator() + //
				System.lineSeparator() + //
				"Source: [extendedComplexType.xsd](" + schemaURI + ")";

		assertHover(xml, expected, r(0, 1, 0, 17));
	}

	private SharedSettings createSharedSettings(SchemaDocumentationType docSource, boolean markdownSupported) {
		SharedSettings settings = new SharedSettings();
		if (markdownSupported) {
			HoverCapabilities capabilities = new HoverCapabilities(Arrays.asList(MarkupKind.MARKDOWN), false);
			settings.getHoverSettings().setCapabilities(capabilities);
		}
		settings.getPreferences()
				.setShowSchemaDocumentationType(docSource);
		return settings;
	}

//	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
//		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true).replace("///",
//				"/");
//	}

	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		String res = XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true);
		if (res.startsWith("file:/") && !res.startsWith("file:///")) {
			res = res.replaceFirst("file:/", "file:///");
		}
		return res;
		
	}
	
	private void assertHover(String xml, String expected, Range range) throws BadLocationException, MalformedURIException {
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/extendedComplexType.xml", expected, range, //
				createSharedSettings(SchemaDocumentationType.documentation, true));
	}
}
