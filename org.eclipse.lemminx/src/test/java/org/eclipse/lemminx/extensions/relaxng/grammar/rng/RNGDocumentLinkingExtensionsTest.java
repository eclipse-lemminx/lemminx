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
package org.eclipse.lemminx.extensions.relaxng.grammar.rng;

import static org.eclipse.lemminx.XMLAssert.dl;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * RNG documentLink for include/@href and externalRef/@href
 */
public class RNGDocumentLinkingExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void includeHrefAtFirstLevel() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" >\n"
				+ "	<include href=\"addressBook.rng\">\n" // <-- documentLink
				+ "		<define name=\"cardContent\">\n"
				+ "			<element name=\"name\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<element name=\"emailAddress\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "		</define>\n"
				+ "	</include>\n"
				+ "</grammar>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng",
				dl(r(1, 16, 1, 31), "src/test/resources/relaxng/addressBook.rng"));
	}

	@Test
	public void includeHrefInAnyLevel() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" >\n"
				+ "\n"
				+ "	<start>\n"
				+ "		<element name=\"doc\">\n"
				+ "			<zeroOrMore>\n"
				+ "				<choice>\n"
				+ "					<element name=\"p\">\n"
				+ "						<ref name=\"inline\" />\n"
				+ "					</element>\n"
				+ "					<grammar>\n"
				+ "						<include href=\"table.rng\">\n" // <-- documentLink
				+ "							<define name=\"cell.content\">\n"
				+ "								<parentRef name=\"inline\" />\n"
				+ "							</define>\n"
				+ "						</include>\n"
				+ "					</grammar>\n"
				+ "				</choice>\n"
				+ "			</zeroOrMore>\n"
				+ "		</element>\n"
				+ "	</start>\n"
				+ "\n"
				+ "	<define name=\"inline\">\n"
				+ "		<zeroOrMore>\n"
				+ "			<choice>\n"
				+ "				<text />\n"
				+ "				<element name=\"em\">\n"
				+ "					<ref name=\"inline\" />\n"
				+ "				</element>\n"
				+ "			</choice>\n"
				+ "		</zeroOrMore>\n"
				+ "	</define>\n"
				+ "\n"
				+ "</grammar>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng",
				dl(r(10, 21, 10, 30), "src/test/resources/relaxng/table.rng"));
	}
	
	@Test
	public void externalRef() throws BadLocationException {
		String xml = "<element name=\"addressBook\">\n"
				+ "	<zeroOrMore>\n"
				+ "		<element name=\"card\">\n"
				+ "			<element name=\"name\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<element name=\"email\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<optional>\n"
				+ "				<element name=\"note\">\n"
				+ "					<externalRef href=\"inline.rng\" />\n" // <-- documentLink
				+ "				</element>\n"
				+ "			</optional>\n"
				+ "		</element>\n"
				+ "	</zeroOrMore>\n"
				+ "</element>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng",
				dl(r(11, 24, 11, 34), "src/test/resources/relaxng/inline.rng"));
	}

	@Test
	public void emptyIncludeHref() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" >\n"
				+ "	<include href=\"\">\n"
				+ "		<define name=\"cardContent\">\n"
				+ "			<element name=\"name\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<element name=\"emailAddress\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "		</define>\n"
				+ "	</include>\n"
				+ "</grammar>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng");
	}

	@Test
	public void emptyIncludeHref2() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" >\n"
				+ "	<include href= >\n"
				+ "		<define name=\"cardContent\">\n"
				+ "			<element name=\"name\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<element name=\"emailAddress\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "		</define>\n"
				+ "	</include>\n"
				+ "</grammar>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng");
	}

	@Test
	public void emptyIncludeHref3() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\" >\n"
				+ "	<include href >\n"
				+ "		<define name=\"cardContent\">\n"
				+ "			<element name=\"name\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "			<element name=\"emailAddress\">\n"
				+ "				<text />\n"
				+ "			</element>\n"
				+ "		</define>\n"
				+ "	</include>\n"
				+ "</grammar>";
		XMLAssert.testDocumentLinkFor(xml, "src/test/resources/relaxng/main.rng");
	}
}
