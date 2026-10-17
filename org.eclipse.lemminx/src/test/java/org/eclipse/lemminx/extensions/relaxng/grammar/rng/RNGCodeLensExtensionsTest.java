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

import static org.eclipse.lemminx.XMLAssert.cl;
import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_BINDING_WIZARD;
import static org.eclipse.lemminx.client.ClientCommands.OPEN_URI;
import static org.eclipse.lemminx.client.ClientCommands.SHOW_REFERENCES;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

/**
 * RNG codelens tests
 *
 */
public class RNGCodeLensExtensionsTest extends AbstractCacheBasedTest {

	@Test
	public void codeLensOnDefineElement() throws BadLocationException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // [http://relaxng.org/ns/structure/1.0 (with
																		// embedded relaxng.rng)] code lens
				"<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\">\r\n" + //
				"    <start>\r\n" + //
				"        <element name=\"addressBook\">\r\n" + //
				"            <zeroOrMore>\r\n" + //
				"                <element name=\"card\">\r\n" + //
				"                    <ref name=\"cardContent\"/>\r\n" + //
				"                </element>\r\n" + //
				"            </zeroOrMore>\r\n" + //
				"        </element>\r\n" + //
				"    </start>\r\n" + //
				"    <define name=\"cardContent\">\r\n" + // [1 reference] code lens
				"        <element name=\"name\">\r\n" + //
				"            <text/>\r\n" + //
				"        </element>\r\n" + //
				"        <element name=\"email\">\r\n" + //
				"            <text/>\r\n" + //
				"        </element>\r\n" + //
				"    </define>\r\n" + //
				"</grammar>";
		XMLAssert.testCodeLensFor(xml, //
				cl(r(0, 0, 0, 0), "http://relaxng.org/ns/structure/1.0 (embedded relaxng.rng)", OPEN_URI), //
				cl(r(11, 12, 11, 30), "1 reference", SHOW_REFERENCES));
	}

	@Test
	public void codeLensOnNestedDefineElement() throws BadLocationException {
		String xml = "<grammar xmlns=\"http://relaxng.org/ns/structure/1.0\"\r\n"
				+ "  datatypeLibrary=\"http://www.w3.org/2001/XMLSchema-datatypes\">\r\n"
				+ "  <start>\r\n"
				+ "    <ref name=\"root-element\"/>\r\n"
				+ "  </start>\r\n"
				+ "  <define name=\"root-element\">\r\n"
				+ "      <element name=\"root-element\">\r\n"
				+ "        <ref name=\"asdf\"></ref>\r\n"
				+ "      </element>\r\n"
				+ "  </define>\r\n"
				+ "  <div>\r\n"
				+ "    <div>\r\n"
				+ "      <define name=\"asdf\">\r\n"
				+ "        <attribute name=\"child\"></attribute>\r\n"
				+ "      </define>\r\n"
				+ "    </div>\r\n"
				+ "  </div>\r\n"
				+ "</grammar>";
		XMLAssert.testCodeLensFor(xml, //
				cl(r(0, 0, 0, 0), "http://relaxng.org/ns/structure/1.0 (embedded relaxng.rng)", OPEN_URI), //
				cl(r(5, 10, 5, 29), "1 reference", SHOW_REFERENCES),
				cl(r(12, 14, 12, 25), "1 reference", SHOW_REFERENCES));
	}

	@Test
	public void codeLensEmptyDocument() throws BadLocationException {
		String xml = "";
		XMLAssert.testCodeLensFor(xml, cl(r(0, 0, 0, 0), "Bind to grammar/schema...", OPEN_BINDING_WIZARD));
	}

	@Test
	public void codeLensSpace() throws BadLocationException {
		String xml = " ";
		XMLAssert.testCodeLensFor(xml, cl(r(0, 0, 0, 0), "Bind to grammar/schema...", OPEN_BINDING_WIZARD));
	}

}
