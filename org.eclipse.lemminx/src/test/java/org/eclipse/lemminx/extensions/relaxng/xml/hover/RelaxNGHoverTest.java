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
package org.eclipse.lemminx.extensions.relaxng.xml.hover;

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
 * Tests that RelaxNG documentation is resolved correctly when child elements
 * with the same name appear under different parents.
 *
 * @see <a href="https://github.com/eclipse-lemminx/lemminx/issues/1745">issue
 *      1745</a>
 */
public class RelaxNGHoverTest extends AbstractCacheBasedTest {

	@Test
	public void hoverOnFooDate() throws BadLocationException, MalformedURIException {
		String schemaURI = getRelaxNGFileURI("docCollision.rng");
		String xml = "<?xml-model href=\"docCollision.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <foo>\r\n" + //
				"    <da|te>2024-01-01</date>\r\n" + //
				"  </foo>\r\n" + //
				"  <bar>\r\n" + //
				"    <date>2024-06-15</date>\r\n" + //
				"  </bar>\r\n" + //
				"</root>";
		assertHover(xml, "Date of a FOO object" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [docCollision.rng](" + schemaURI + ")", r(3, 5, 3, 9));
	}

	@Test
	public void hoverOnBarDate() throws BadLocationException, MalformedURIException {
		String schemaURI = getRelaxNGFileURI("docCollision.rng");
		String xml = "<?xml-model href=\"docCollision.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <foo>\r\n" + //
				"    <date>2024-01-01</date>\r\n" + //
				"  </foo>\r\n" + //
				"  <bar>\r\n" + //
				"    <da|te>2024-06-15</date>\r\n" + //
				"  </bar>\r\n" + //
				"</root>";
		assertHover(xml, "Date of a BAR object" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [docCollision.rng](" + schemaURI + ")", r(6, 5, 6, 9));
	}

	@Test
	public void hoverOnFoo() throws BadLocationException, MalformedURIException {
		String schemaURI = getRelaxNGFileURI("docCollision.rng");
		String xml = "<?xml-model href=\"docCollision.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <fo|o>\r\n" + //
				"    <date>2024-01-01</date>\r\n" + //
				"  </foo>\r\n" + //
				"  <bar>\r\n" + //
				"    <date>2024-06-15</date>\r\n" + //
				"  </bar>\r\n" + //
				"</root>";
		assertHover(xml, "A foo element" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [docCollision.rng](" + schemaURI + ")", r(2, 3, 2, 6));
	}

	@Test
	public void hoverOnBar() throws BadLocationException, MalformedURIException {
		String schemaURI = getRelaxNGFileURI("docCollision.rng");
		String xml = "<?xml-model href=\"docCollision.rng\" ?>\r\n" + //
				"<root>\r\n" + //
				"  <foo>\r\n" + //
				"    <date>2024-01-01</date>\r\n" + //
				"  </foo>\r\n" + //
				"  <ba|r>\r\n" + //
				"    <date>2024-06-15</date>\r\n" + //
				"  </bar>\r\n" + //
				"</root>";
		assertHover(xml, "A bar element" + //
				System.lineSeparator() + //
				System.lineSeparator() + "Source: [docCollision.rng](" + schemaURI + ")", r(5, 3, 5, 6));
	}

	private static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		XMLAssert.assertHover(new XMLLanguageService(), value, null, "src/test/resources/relaxng/test.xml",
				expectedHoverLabel, expectedHoverRange);
	}

	private static String getRelaxNGFileURI(String schemaURI) throws MalformedURIException {
		return XMLEntityManager
				.expandSystemId("relaxng/" + schemaURI, "src/test/resources/test.xml", true)
				.replace("///", "/");
	}
}
