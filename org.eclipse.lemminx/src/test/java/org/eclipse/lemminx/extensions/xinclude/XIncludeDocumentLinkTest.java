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
package org.eclipse.lemminx.extensions.xinclude;

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * XInclude documentLink for @href
 */
public class XIncludeDocumentLinkTest extends AbstractCacheBasedTest {

	@Test
	public void includeHrefAtFirstLevel() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<book xmlns:xi=\"http://www.w3.org/2001/XInclude\"> \n" + //
				"  <xi:include href=\"reference.xml\" />\n" + // <-- documentLink
				"</book>\n";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xinclude/main.xml",
				dl(r(2, 20, 2, 33), "src/test/resources/xinclude/reference.xml"));
	}

	@Test
	public void includeHrefInAnyLevel() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<book xmlns:xi=\"http://www.w3.org/2001/XInclude\"> \n" + //
				"  <author> \n" + //
				"    <xi:include href=\"reference.xml\" />\n" + // <-- documentLink
				"  </author>\n" + //
				"</book>\n";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xinclude/main.xml",
				dl(r(3, 22, 3, 35), "src/test/resources/xinclude/reference.xml"));
	}

	@Test
	public void includeHrefZeroLength() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
				"<book xmlns:xi=\"http://www.w3.org/2001/XInclude\"> \n" + //
				"  <author> \n" + //
				"    <xi:include href=\"\" />\n" + // <-- documentLink
				"  </author>\n" + //
				"</book>\n";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/xinclude/main.xml");
	}
}
