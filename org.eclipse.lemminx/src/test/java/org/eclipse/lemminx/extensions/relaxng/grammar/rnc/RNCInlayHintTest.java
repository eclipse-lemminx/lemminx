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

import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.ih;
import static org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCAssert.testInlayHintFor;

import java.io.IOException;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

/**
 * Tests for RNC inlay hints — inherited start declarations shown at include statements.
 *
 * <p>When an RNC file has an {@code include} but no own {@code start} declaration,
 * an inlay hint is shown at the end of the include URI displaying the inherited
 * {@code start} pattern from the included file.</p>
 */
public class RNCInlayHintTest {

	private static final String LS = System.lineSeparator();

	// -------------------- Inherited start from simple include --------------------

	@Test
	public void testInlayHintSimpleIncludeInheritsStart() throws BadLocationException, IOException {
		// include-base.rnc declares: start = element doc { paragraph+ }
		// A simple include without own start → hint shown at end of URI (offset after closing quote)
		String fileUri = RNCAssert.testFileUri("inlay-hint-no-start.rnc");
		// include "include-base.rnc"
		//                           ^-- position (0, 26) — after closing quote of URI
		String value = "include \"include-base.rnc\"";
		testInlayHintFor(value, fileUri,
				ih(new Position(0, 26), "start = element doc { paragraph+ }"));
	}

	@Test
	public void testInlayHintIncludeWithPartialOverrideInheritsStart() throws BadLocationException, IOException {
		// include-base.rnc declares: start = element doc { paragraph+ }
		// Include with body that overrides paragraph but NOT start → still inherits start
		String fileUri = RNCAssert.testFileUri("inlay-hint-no-start.rnc");
		String value = "include \"include-base.rnc\" {" + LS
				+ "  paragraph = element p { text }" + LS
				+ "}";
		// The include overrides paragraph but NOT start. URI end is at offset 26 → position (0, 26)
		testInlayHintFor(value, fileUri,
				ih(new Position(0, 26), "start = element doc { paragraph+ }"));
	}

	@Test
	public void testNoInlayHintWhenStartExplicit() throws BadLocationException, IOException {
		// File has its own start → no inlay hint for inherited start
		String fileUri = RNCAssert.testFileUri("inlay-hint-no-start.rnc");
		String value = "include \"include-base.rnc\" {" + LS
				+ "  start = element root { text }" + LS
				+ "}";
		testInlayHintFor(value, fileUri /* no hints expected */);
	}

	@Test
	public void testNoInlayHintWhenIncludeOverridesStart() throws BadLocationException, IOException {
		// Include explicitly overrides start → no hint (the override IS the start, it's explicit)
		String fileUri = RNCAssert.testFileUri("inlay-hint-no-start.rnc");
		String value = "include \"include-base.rnc\" {" + LS
				+ "  start = element newsitem { text }" + LS
				+ "}";
		testInlayHintFor(value, fileUri /* no hints */);
	}

	@Test
	public void testNoInlayHintForTopLevelStart() throws BadLocationException, IOException {
		// File has its own top-level start → no inlay hint
		String value = "start = element root { text }";
		testInlayHintFor(value /* no hints */);
	}

	@Test
	public void testNoInlayHintForPlainDocument() throws BadLocationException, IOException {
		// No include → no hint
		String value = "foo = element foo { text }";
		testInlayHintFor(value /* no hints */);
	}
}
