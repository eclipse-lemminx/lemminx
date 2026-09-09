/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.extensions.contentmodel;

import static org.eclipse.lemminx.XMLAssert.c;
import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.XMLAssert.testCompletionFor;
import static org.eclipse.lemminx.XMLAssert.testTypeDefinitionFor;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.junit.jupiter.api.Test;

/**
 * XML hover, completion, and type definition tests for elements declared inside
 * xs:group.
 *
 * @see <a href=
 *      "https://github.com/redhat-developer/vscode-xml/issues/1129">vscode-xml#1129</a>
 */
public class XMLSchemaHoverGroupTest extends AbstractCacheBasedTest {

	// ------------------- Hover -------------------

	@Test
	public void testHoverOnGlobalElement() throws BadLocationException, MalformedURIException {
		String schemaURI = getSchemaURI("group.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns:g=\"http://group-test\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:resu|lt>Test</g:result>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/group.xml",
				"Result documentation" +
						System.lineSeparator() +
						System.lineSeparator() + "Source: [group.xsd](" + schemaURI + ")",
				r(4, 5, 4, 13));
	}

	@Test
	public void testHoverOnElementInGroup() throws BadLocationException, MalformedURIException {
		String schemaURI = getSchemaURI("group.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns:g=\"http://group-test\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:result|Time>2024-01-01</g:resultTime>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/group.xml",
				"Result time documentation" +
						System.lineSeparator() +
						System.lineSeparator() + "Source: [group.xsd](" + schemaURI + ")",
				r(4, 5, 4, 17));
	}

	@Test
	public void testHoverOnAnotherElementInGroup() throws BadLocationException, MalformedURIException {
		String schemaURI = getSchemaURI("group.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns:g=\"http://group-test\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:proce|dure>test</g:procedure>\r\n" +
				"</root>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/group.xml",
				"Procedure documentation" +
						System.lineSeparator() +
						System.lineSeparator() + "Source: [group.xsd](" + schemaURI + ")",
				r(4, 5, 4, 16));
	}

	@Test
	public void testHoverOnElementInGroupWithSchemaParent() throws BadLocationException, MalformedURIException {
		String schemaURI = getSchemaURI("group.xsd");
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<g:observation xmlns:g=\"http://group-test\"\r\n" +
				"               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"               xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:result|Time>2024-01-01</g:resultTime>\r\n" +
				"</g:observation>";
		XMLAssert.assertHover(new XMLLanguageService(), xml, null, "src/test/resources/group.xml",
				"Result time documentation" +
						System.lineSeparator() +
						System.lineSeparator() + "Source: [group.xsd](" + schemaURI + ")",
				r(4, 5, 4, 17));
	}

	// ------------------- Completion -------------------

	@Test
	public void testCompletionOfFirstGroupElement() throws BadLocationException {
		// Inside <g:observation>, the first expected element from the group sequence
		// is g:resultTime
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<g:observation xmlns:g=\"http://group-test\"\r\n" +
				"               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"               xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <|\r\n" +
				"</g:observation>";
		testCompletionFor(xml, null, "src/test/resources/group.xml", null,
				c("g:resultTime", "<g:resultTime></g:resultTime>"));
	}

	@Test
	public void testCompletionOfNextGroupElement() throws BadLocationException {
		// After g:resultTime is present, the next expected element is g:procedure
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<g:observation xmlns:g=\"http://group-test\"\r\n" +
				"               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"               xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:resultTime>2024-01-01</g:resultTime>\r\n" +
				"    <|\r\n" +
				"</g:observation>";
		testCompletionFor(xml, null, "src/test/resources/group.xml", null,
				c("g:procedure", "<g:procedure></g:procedure>"));
	}

	@Test
	public void testCompletionOfGlobalElementAfterGroup() throws BadLocationException {
		// After all group elements are present, the global element ref g:result
		// should be offered
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<g:observation xmlns:g=\"http://group-test\"\r\n" +
				"               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"               xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:resultTime>2024-01-01</g:resultTime>\r\n" +
				"    <g:procedure>test</g:procedure>\r\n" +
				"    <|\r\n" +
				"</g:observation>";
		testCompletionFor(xml, null, "src/test/resources/group.xml", null,
				c("g:result", "<g:result></g:result>"));
	}

	// ------------------- Type definition -------------------

	@Test
	public void testTypeDefinitionOnGlobalElement() throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/group.xml";
		String xsdFile = "xsd/group.xsd";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<root xmlns:g=\"http://group-test\"\r\n" +
				"      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"      xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:resu|lt>Test</g:result>\r\n" +
				"</root>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		// "result" is at line 6 (0-indexed), name="result" at columns 22-30
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(4, 5, 4, 13), r(6, 22, 6, 30)));
	}

	@Test
	public void testTypeDefinitionOnElementInGroupWithSchemaParent()
			throws BadLocationException, MalformedURIException {
		String xmlFile = "src/test/resources/group.xml";
		String xsdFile = "xsd/group.xsd";
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<g:observation xmlns:g=\"http://group-test\"\r\n" +
				"               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"               xsi:schemaLocation=\"http://group-test xsd/group.xsd\">\r\n" +
				"    <g:result|Time>2024-01-01</g:resultTime>\r\n" +
				"</g:observation>";
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		String targetSchemaURI = xmlLanguageService.getResolverExtensionManager().resolve(xmlFile, null, xsdFile);
		// "resultTime" is at line 14 (0-indexed), name="resultTime" at columns 30-42
		testTypeDefinitionFor(xmlLanguageService, xml, xmlFile,
				ll(targetSchemaURI, r(4, 5, 4, 17), r(14, 30, 14, 42)));
	}

	private static String getSchemaURI(String schemaFileName) throws MalformedURIException {
		return XMLEntityManager
				.expandSystemId("xsd/" + schemaFileName, "src/test/resources/test.xml", true)
				.replace("///", "/");
	}
}
