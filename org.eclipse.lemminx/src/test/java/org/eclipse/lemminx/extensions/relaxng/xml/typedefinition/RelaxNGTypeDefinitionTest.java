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
package org.eclipse.lemminx.extensions.relaxng.xml.typedefinition;

import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testTypeDefinitionFor;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.junit.jupiter.api.Test;

/**
 * RelaxNG type definition tests.
 *
 */
public class RelaxNGTypeDefinitionTest extends AbstractCacheBasedTest {

	@Test
	public void toElementDeclaration() throws BadLocationException {
		String xmlFile = "src/test/resources/relaxng/tei.xml";
		String xsdFile = "tei_all.rng";

		String xml = "<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<T|EI>\r\n" +
				"\r\n" + //
				"</TEI>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, //
				ll(targetSchemaURI, r(1, 1, 1, 4), r(7956, 15, 7956, 25)));
	}

	@Test
	public void toChildElementDeclaration() throws BadLocationException {
		String xmlFile = "src/test/resources/relaxng/addressBook.xml";
		String rngFile = "addressBook_v1.rng";

		String xml = "<?xml-model href=\"addressBook_v1.rng\" ?>\r\n" + //
				"<addressBook>\r\n" + //
				"  <car|d></card>\r\n" + //
				"</addressBook>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, rngFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, //
				ll(targetSchemaURI, r(2, 3, 2, 7), r(4, 13, 4, 24)));
	}

	@Test
	public void toAttributeDeclaration() throws BadLocationException {
		String xmlFile = "src/test/resources/relaxng/tei.xml";
		String xsdFile = "tei_all.rng";

		String xml = "<?xml-model href=\"tei_all.rng\" ?>\r\n" + //
				"<TEI ce|rt=\"\">\r\n" +
				"\r\n" + //
				"</TEI>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile, //
				ll(targetSchemaURI, r(1, 5, 1, 12), r(853, 20, 853, 31)));
	}
}
