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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.LS;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testDefinitionFor;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testFileUri;
import static org.eclipse.lemminx.XMLAssert.ll;
import static org.eclipse.lemminx.XMLAssert.r;

import org.eclipse.lemminx.commons.BadLocationException;
import org.junit.jupiter.api.Test;

public class RNCDefinitionTest {

	@Test
	public void testDefinitionGoToDefine() throws BadLocationException {
		String value = "start = ro|ot" + LS +
				"root = element root { text }";
		testDefinitionFor(value,
				ll("test.rnc", r(0, 8, 0, 12), r(1, 0, 1, 4)));
	}

	@Test
	public void testDefinitionOnDefineItself() throws BadLocationException {
		testDefinitionFor("ro|ot = element root { text }",
				ll("test.rnc", r(0, 0, 0, 4), r(0, 0, 0, 4)));
	}

	@Test
	public void testDefinitionNotFound() throws BadLocationException {
		testDefinitionFor("start = fo|o");
	}

	@Test
	public void testDefinitionOnKeyword() throws BadLocationException {
		testDefinitionFor("start = ele|ment root { text }");
	}

	@Test
	public void testDefinitionOnWhitespace() throws BadLocationException {
		testDefinitionFor("start| = root");
	}

	@Test
	public void testDefinitionMultipleDefines() throws BadLocationException {
		String value = "start = a" + LS +
				"a = text" + LS +
				"b = text" + LS +
				"c = |a";
		testDefinitionFor(value,
				ll("test.rnc", r(3, 4, 3, 5), r(1, 0, 1, 1)));
	}

	@Test
	public void testDefinitionIncludeOverrideGoToOriginal() throws BadLocationException {
		// Ctrl+Click on "paragraph" override in include body → goes to original in include-base.rnc
		String fileUri = testFileUri("include-valid.rnc");
		String baseFileUri = testFileUri("include-base.rnc");
		String value = "include \"include-base.rnc\" {" + LS +
				"  par|agraph = element p {" + LS +
				"    mixed { inline* }" + LS +
				"  }" + LS +
				"}" + LS +
				LS +
				"inline =" + LS +
				"  element bold { text }" + LS +
				"  | element italic { text }";
		testDefinitionFor(value, fileUri,
				ll(baseFileUri, r(1, 2, 1, 11), r(2, 0, 2, 9)));
	}

	@Test
	public void testDefinitionReferenceToIncludedDefine() throws BadLocationException {
		// Ctrl+Click on a reference to a define that exists only in the included file
		String fileUri = testFileUri("include-valid.rnc");
		String baseFileUri = testFileUri("include-base.rnc");
		String value = "include \"include-base.rnc\"" + LS +
				"foo = par|agraph";
		testDefinitionFor(value, fileUri,
				ll(baseFileUri, r(1, 6, 1, 15), r(2, 0, 2, 9)));
	}
}
