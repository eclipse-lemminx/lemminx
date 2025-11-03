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

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * XML Model document links tests
 *
 */
public class XMLModelDocumentLinkTest extends AbstractCacheBasedTest {

	@Test
	public void xmlModelHref() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<?xml-model href=\"xsd/Format.xsd\" ?>\r\n" + //
				"<Configuration>\r\n" + //
				"  <ViewDefinitions>\r\n" + //
				"    <View>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/Format.xml",
				dl(r(1, 18, 1, 32), "src/test/resources/xsd/Format.xsd"));
	}

	@Test
	public void xmlModelHrefZeroLength() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<?xml-model href=\"\" ?>\r\n" + //
				"<Configuration>\r\n" + //
				"  <ViewDefinitions>\r\n" + //
				"    <View>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/Format.xml");
	}

	@Test
	public void xmlModelHrefZeroLength2() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<?xml-model href= ?>\r\n" + //
				"<Configuration>\r\n" + //
				"  <ViewDefinitions>\r\n" + //
				"    <View>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/Format.xml");
	}

	@Test
	public void xmlModelHrefZeroLength3() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" + //
				"<?xml-model href ?>\r\n" + //
				"<Configuration>\r\n" + //
				"  <ViewDefinitions>\r\n" + //
				"    <View>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/Format.xml");
	}
}
