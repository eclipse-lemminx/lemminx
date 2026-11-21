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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexReferenceParticipant;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lemminx.utils.FilesUtils;
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
	 * <p>When the cursor is not on a define name and {@code workspaceIndex} is
	 * non-null, falls back to workspace cross-file references — the XML files in
	 * the workspace that reference this RNC grammar file. This handles the
	 * "N references" code lens click, which passes position {@code (0,0)}
	 * (not on any define), expecting cross-file results.</p>
	 *
	 * @param rncDocument    the parsed RNC document.
	 * @param position       the cursor position on the define name.
	 * @param context        the reference context; when
	 *                       {@link ReferenceContext#isIncludeDeclaration()} is
	 *                       {@code true}, the define declaration itself is included.
	 * @param workspaceIndex the workspace Bloom index for cross-file lookup, or
	 *                       {@code null} if the workspace index feature is disabled.
	 * @param cancelChecker  used to check if the operation has been canceled.
	 * @return a list of {@link Location} for each reference found, or an empty
	 *         list if the word is not a define name.
	 */
	public List<? extends Location> findReferences(RNCDocument rncDocument, Position position,
			ReferenceContext context, WorkspaceIndex workspaceIndex, CancelChecker cancelChecker) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word != null && !word.isEmpty()) {
				RNCDefine define = rncDocument.findDefine(word);
				if (define != null) {
					boolean includeDeclaration = context != null && context.isIncludeDeclaration();
					List<Location> defineRefs = RNCSearchUtils.findReferences(textDocument, define,
							includeDeclaration);
					if (!defineRefs.isEmpty()) {
						return defineRefs;
					}
					// Define has no intra-file references — fall through to workspace index.
					// This handles the workspace "N references" code lens click when the lens
					// position (0,0) coincides with a define that has 0 internal uses (e.g.
					// the top-level "start" define). Workspace backlinks are more useful
					// than an empty list in that case.
				}
			}

			// No define at cursor, or define found with no intra-file references —
			// look up cross-file workspace references (e.g. code lens click).
			if (workspaceIndex != null) {
				Path targetPath = FilesUtils.getPathOrNull(textDocument.getUri());
				if (targetPath != null) {
					List<Location> crossFile = WorkspaceIndexReferenceParticipant
							.collectCrossFileReferences(workspaceIndex, targetPath, cancelChecker);
					if (!crossFile.isEmpty()) {
						return crossFile;
					}
				}
			}
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC references", e);
		}
		return Collections.emptyList();
	}
}
