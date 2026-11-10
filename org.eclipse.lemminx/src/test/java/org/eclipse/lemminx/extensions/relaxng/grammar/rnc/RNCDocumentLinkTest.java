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

import static org.eclipse.lemminx.XMLAssert.r;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.dl;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testDocumentLinksFor;

import org.junit.jupiter.api.Test;

public class RNCDocumentLinkTest {

	@Test
	public void testIncludeDocumentLink() {
		String value = "include \"other.rnc\"";
		testDocumentLinksFor(value,
				dl(r(0, 9, 0, 18)));
	}

	@Test
	public void testIncludeWithBodyDocumentLink() {
		String value = "include \"base.rnc\" {\n" +
				"  start = element root { text }\n" +
				"}";
		testDocumentLinksFor(value,
				dl(r(0, 9, 0, 17)));
	}

	@Test
	public void testMultipleIncludes() {
		String value = "include \"a.rnc\"\n" +
				"\n" +
				"include \"b.rnc\"";
		testDocumentLinksFor(value,
				dl(r(0, 9, 0, 14)),
				dl(r(2, 9, 2, 14)));
	}

	@Test
	public void testNoInclude() {
		String value = "start = element root { text }";
		testDocumentLinksFor(value);
	}

	@Test
	public void testExternalDocumentLink() {
		String value = "start = external \"other.rnc\"";
		testDocumentLinksFor(value,
				dl(r(0, 18, 0, 27)));
	}

	@Test
	public void testExternalInsideDefine() {
		String value = "start = root\n" +
				"root = external \"content.rnc\"";
		testDocumentLinksFor(value,
				dl(r(1, 17, 1, 28)));
	}

	@Test
	public void testIncludeAndExternal() {
		String value = "include \"base.rnc\"\n" +
				"\n" +
				"start = external \"other.rnc\"";
		testDocumentLinksFor(value,
				dl(r(0, 9, 0, 17)),
				dl(r(2, 18, 2, 27)));
	}
}
