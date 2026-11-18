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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codelens;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexCodeLensSupport;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lemminx.services.extensions.codelens.ReferenceCommand;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides code lenses for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p><b>Two kinds of lenses:</b>
 * <ol>
 *   <li><b>Workspace "N references" lens (line 0):</b> Shows how many XML files
 *       in the workspace reference this RNC file via {@code href}, {@code include},
 *       {@code xsi:noNamespaceSchemaLocation}, etc. Computed via
 *       {@link WorkspaceIndexCodeLensSupport} (shared with the XSD/DTD/RNG participant).
 *       Only appears when {@link WorkspaceIndex} is populated and the RNC file is
 *       referenced by at least one workspace file.</li>
 *   <li><b>Per-define "N references" lens:</b> Shows how many times each
 *       {@code define} pattern is used within the same RNC document (or across
 *       included files). Clicking triggers "Find All References" for that define.</li>
 * </ol>
 *
 * <p><b>Use case — workspace lens:</b>
 * <pre>
 * 2 references   ← XML files referencing this .rnc via href="schema.rnc"
 * # schema.rnc
 *
 * 3 references   ← define 'root' used 3 times in this file/includes
 * root = element root { ... }
 * </pre>
 *
 * @see WorkspaceIndexCodeLensSupport
 * @see RNCSearchUtils
 */
public class RNCCodeLenses {

	private static final Logger LOGGER = Logger.getLogger(RNCCodeLenses.class.getName());

	/**
	 * Computes code lenses for all defines in the given RNC document, plus a
	 * workspace "N references" lens at the top of the file.
	 *
	 * <p><b>Workspace lens use case:</b> {@code workspaceIndex} is non-null when the
	 * workspace Bloom index is enabled. The lens at position {@code (0,0)} shows how
	 * many XML files reference this RNC file. If {@code workspaceIndex} is {@code null}
	 * (index disabled or not yet initialized), only the per-define lenses are returned.
	 *
	 * @param rncDocument       the parsed RNC document containing defines.
	 * @param supportedByClient {@code true} if the client supports the
	 *                          {@code editor.action.showReferences} command.
	 * @param workspaceIndex    the populated workspace index, or {@code null} if
	 *                          the workspace index feature is disabled.
	 * @param cancelChecker     used to abort computation if the document changes.
	 * @return a list of {@link CodeLens} objects: one workspace lens (if references
	 *         are found) followed by per-define lenses.
	 */
	public List<CodeLens> getCodeLens(RNCDocument rncDocument, boolean supportedByClient,
			WorkspaceIndex workspaceIndex, CancelChecker cancelChecker) {
		List<CodeLens> lenses = new ArrayList<>();
		try {
			// Workspace "N references" lens at position (0,0).
			// RNC has no DOM, so position (0,0) is used instead of the root element range.
			if (workspaceIndex != null) {
				String uri = rncDocument.getTextDocument().getUri();
				Range topRange = new Range(new Position(0, 0), new Position(0, 0));
				CodeLens workspaceLens = WorkspaceIndexCodeLensSupport.buildWorkspaceReferenceLens(
						workspaceIndex, uri, topRange, supportedByClient, cancelChecker);
				if (workspaceLens != null) {
					lenses.add(workspaceLens);
				}
			}

			// Per-define lenses (intra-file / include references)
			TextDocument textDocument = rncDocument.getTextDocument();
			List<RNCDefine> defines = rncDocument.getDefines();
			for (RNCDefine define : defines) {
				cancelChecker.checkCanceled();
				String name = define.getName();
				if (name == null) {
					continue;
				}
				List<Location> refs = RNCSearchUtils.findReferences(textDocument, define, false);
				int refCount = refs.size();
				for (RNCDefine other : defines) {
					if (other != define && name.equals(other.getName())) {
						refCount++;
					}
				}
				if (refCount > 0) {
					Range range = RNCPositionUtils.createRange(textDocument, define.getNameStart(),
							define.getNameEnd());
					if (range != null) {
						CodeLens codeLens = new CodeLens(range);
						ReferenceCommand command = new ReferenceCommand(textDocument.getUri(),
								range.getStart(), supportedByClient);
						for (int i = 1; i < refCount; i++) {
							command.increment();
						}
						codeLens.setCommand(command);
						lenses.add(codeLens);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC code lens", e);
		}
		return lenses;
	}
}
