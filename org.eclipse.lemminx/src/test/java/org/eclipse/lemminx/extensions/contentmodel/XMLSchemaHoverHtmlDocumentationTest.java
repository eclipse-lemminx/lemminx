/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
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
import org.junit.jupiter.api.Test;

/**
 * Tests that HTML elements inside xs:documentation do not cause
 * documentation content to be lost.
 *
 * @see <a href="https://github.com/eclipse/lemminx/issues/XXXX">issue</a>
 */
public class XMLSchemaHoverHtmlDocumentationTest extends AbstractCacheBasedTest {

	@Test
	public void testDocumentationWithBrElement() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <wi|thBr/>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"Line one." + System.lineSeparator() + "Line two." +
						System.lineSeparator() + System.lineSeparator() +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 9));
	}

	@Test
	public void testDocumentationWithBoldElement() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <wi|thBold/>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"This is **bold** text" +
						System.lineSeparator() + System.lineSeparator() +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 11));
	}

	@Test
	public void testDocumentationWithoutHtmlElements() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <no|Br/>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"Simple documentation" +
						System.lineSeparator() + System.lineSeparator() +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 7));
	}

	@Test
	public void testMultiLineProseJoined() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <multiLine|Prose/>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"In keeping with the standard policy, this document will persist at the given URI." +
						System.lineSeparator() + System.lineSeparator() +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 17));
	}

	@Test
	public void testProseWithIndentedContent() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <proseWith|Indented/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		String NBSP = "\u00A0";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"Previous versions include:" + NL + NL +
						NBSP + NBSP + "http://example.com/v1" + NL +
						NBSP + NBSP + NBSP + NBSP + "(Version 1)" + NL + NL +
						NBSP + NBSP + "http://example.com/v2" + NL +
						NBSP + NBSP + NBSP + NBSP + "(Version 2)" +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 20));
	}

	// --- Bug fix regression tests ---
	// These test the full pipeline: XSD annotation → documentation extraction → converter → hover tooltip.

	@Test
	public void testBareAngleBracketInCDATA() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <withBare|AngleBracket/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"The value must satisfy x < 100 and y > 0." +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 23));
	}

	@Test
	public void testEntityProseWithAngleBrackets() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <withEntity|Prose/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"The value must satisfy x < 100 and y > 0." +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 18));
	}

	@Test
	public void testLinkWithAmpersand() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <withAmpersand|Link/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"See [search results](https://example.com/search?q=a&b=c)." +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 20));
	}

	@Test
	public void testBlockquoteRendered() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <withBlock|quote/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"> Note: This element is deprecated. Use `new-element` instead." +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 17));
	}

	@Test
	public void testListWithParagraphInItem() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <withList|Paragraph/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				" *  First step." + NL +
						" *  Second step." +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 20));
	}

	@Test
	public void testTableWithRowspan() throws BadLocationException, MalformedURIException {
		String schemaURI = getXMLSchemaFileURI("htmlDocumentation.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns=\"http://htmlDocumentation\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://htmlDocumentation xsd/htmlDocumentation.xsd\">\r\n" +
				"  <with|Rowspan/>\r\n" +
				"</root>";
		String NL = System.lineSeparator();
		XMLAssert.assertHover(new XMLLanguageService(), xml, null,
				"src/test/resources/htmlDocumentation.xml",
				"| Category | Item |" + NL +
						"| --- | --- |" + NL +
						"| Fruit | Apple |" + NL +
						"|  | Banana |" +
						NL + NL +
						"Source: [htmlDocumentation.xsd](" + schemaURI + ")",
				r(4, 3, 4, 14));
	}

	private static String getXMLSchemaFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager.expandSystemId("xsd/" + schemaURI, "src/test/resources/test.xml", true)
				.replace("///", "/");
	}
}
