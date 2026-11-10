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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.cl;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testCodeLensFor;
import static org.eclipse.lemminx.XMLAssert.r;

import org.junit.jupiter.api.Test;

public class RNCCodeLensTest {

	@Test
	public void testCodeLensReferences() {
		String value = "start = root" + LS +
				"root = element root { content }" + LS +
				"content = text";
		testCodeLensFor(value,
				cl(r(1, 0, 1, 4), "1 reference"),
				cl(r(2, 0, 2, 7), "1 reference"));
	}

	@Test
	public void testCodeLensMultipleReferences() {
		String value = "start = root" + LS +
				"a = root" + LS +
				"root = element root { text }";
		testCodeLensFor(value,
				cl(r(2, 0, 2, 4), "2 references"));
	}

	@Test
	public void testCodeLensNoReferences() {
		testCodeLensFor("unused = text");
	}

	@Test
	public void testCodeLensEmptyDocument() {
		testCodeLensFor("");
	}

	@Test
	public void testCodeLensNoDefines() {
		testCodeLensFor("start = element root { text }");
	}

	@Test
	public void testCodeLensDoesNotCountDefinition() {
		String value = "a = text" + LS +
				"start = a";
		testCodeLensFor(value,
				cl(r(0, 0, 0, 1), "1 reference"));
	}

	@Test
	public void testCodeLensFullSchema() {
		String value = "default namespace = \"http://example.com\"" + LS
				+ "start = root" + LS
				+ "root = element root {" + LS
				+ "  attribute id { text }," + LS
				+ "  name+" + LS
				+ "}" + LS
				+ "name = element name { text }";
		testCodeLensFor(value,
				cl(r(2, 0, 2, 4), "1 reference"),
				cl(r(6, 0, 6, 4), "1 reference"));
	}
}
