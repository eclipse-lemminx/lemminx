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
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.r;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLFileAssociation;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

/**
 * XML hover tests with file association.
 *
 */
public class XMLFileAssociationsHoverTest extends AbstractCacheBasedTest {

	@Test
	public void hoverBasedOnXSDWithFileAssociation() throws BadLocationException, MalformedURIException {
		ContentModelSettings modelSettings = new ContentModelSettings();
		modelSettings.setFileAssociations(createXSDAssociationsSchemaLocationLike("src/test/resources/xsd/"));

		String schemaURI = getXMLSchemaFileURI("maven-4.0.0.xsd");
		String xml = "<pro|ject xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n>\r\n" + //
				"</project>";
		assertHover(xml, "file:///test/pom.xml", modelSettings, 
				"3.0.0+\r\n" +
				"The `<project>` element is the root of the descriptor. The following table lists all of the possible child elements.\r\n\r\n"+
				"Source: [maven-4.0.0.xsd](" + schemaURI + ")\r\n", r(0, 1, 0, 8));
	}

	@Test
	public void hoverBasedOnXSDWithFileAssociationAndDocType() throws BadLocationException, MalformedURIException {
		ContentModelSettings modelSettings = new ContentModelSettings();
		modelSettings.setFileAssociations(createXSDAssociationsSchemaLocationLike("src/test/resources/xsd/"));

		String schemaURI = getXMLSchemaFileURI("maven-4.0.0.xsd");
		String xml = "<!DOCTYPE opt [\r\n" +
				"  <!ENTITY size \"short\">\r\n" +
				"]>\r\n" + //
				"<pro|ject xmlns=\"http://maven.apache.org/POM/4.0.0\"\r\n>\r\n" +
				"</project>";
		
		assertHover(xml, "file:///test/pom.xml", modelSettings, "3.0.0+" +
				System.lineSeparator() +
				System.lineSeparator() +
				"The `<project>` element is the root of the descriptor. The following table lists all of the possible child elements." +
				System.lineSeparator() +
				System.lineSeparator() +
				"Source: [maven-4.0.0.xsd](" + schemaURI + ")\r\n", r(3, 1, 3, 8));
	}

	private static XMLFileAssociation[] createXSDAssociationsSchemaLocationLike(String baseSystemId) {
		XMLFileAssociation maven = new XMLFileAssociation();
		maven.setPattern("**/pom.xml");
		maven.setSystemId(baseSystemId + "maven-4.0.0.xsd");
		return new XMLFileAssociation[] { maven };
	}

	private static void assertHover(String value, String fileURI, ContentModelSettings modelSettings,
			String expectedHoverLabel, Range expectedHoverRange) throws BadLocationException {
		XMLAssert.assertHover(new XMLLanguageService(), value, null, fileURI, expectedHoverLabel, expectedHoverRange,
				modelSettings);
	}

	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		String res = XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true);
		if (res.startsWith("file:/") && !res.startsWith("file:///")) {
			res = res.replaceFirst("file:/", "file:///");
		}
		return res;
		
	}

}