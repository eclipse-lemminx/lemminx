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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.reference;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides "Find References" support for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Use case: the user places the cursor on a define name like {@code inline}
 * and invokes "Find All References". This class locates all positions where
 * {@code inline} is used as a pattern reference (excluding element/attribute
 * names and the define declaration itself, unless requested).</p>
 *
 * <p>Example: in this document, finding references for {@code inline} returns
 * the reference at line 2 ({@code inline*}):</p>
 * <pre>
 * start = element root {
 *   inline*
 * }
 * inline = element span { text }
 * </pre>
 *
 * @see RNCSearchUtils
 */
public class RNCReferences {

	private static final Logger LOGGER = Logger.getLogger(RNCReferences.class.getName());

	/**
	 * Finds all references to the define name at the given cursor position.
	 *
	 * @param rncDocument   the parsed RNC document.
	 * @param position      the cursor position on the define name.
	 * @param context       the reference context; when
	 *                      {@link ReferenceContext#isIncludeDeclaration()} is
	 *                      {@code true}, the define declaration itself is included.
	 * @param cancelChecker used to check if the operation has been canceled.
	 * @return a list of {@link Location} for each reference found, or an empty
	 *         list if the word is not a define name.
	 */
	public List<? extends Location> findReferences(RNCDocument rncDocument, Position position,
			ReferenceContext context, CancelChecker cancelChecker) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word == null || word.isEmpty()) {
				return Collections.emptyList();
			}

			RNCDefine define = rncDocument.findDefine(word);
			if (define == null) {
				return Collections.emptyList();
			}

			boolean includeDeclaration = context != null && context.isIncludeDeclaration();
			return RNCSearchUtils.findReferences(textDocument, text, define, includeDeclaration);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC references", e);
		}
		return Collections.emptyList();
	}
}
