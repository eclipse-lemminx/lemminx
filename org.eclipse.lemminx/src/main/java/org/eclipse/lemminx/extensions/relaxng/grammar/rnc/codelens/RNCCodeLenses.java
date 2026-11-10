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
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lemminx.services.extensions.codelens.ReferenceCommand;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides code lenses for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Each define in the document gets a code lens showing its reference count.
 * Clicking the lens triggers "Find All References" for that define.</p>
 *
 * <p>Use case: the editor displays "{@code 3 references}" above each define
 * declaration, helping users understand which defines are heavily used and
 * which are unused.</p>
 *
 * <p>Example:</p>
 * <pre>
 * 2 references
 * root = element root { inline*, block* }
 *
 * 1 reference
 * inline = element span { text }
 *
 * 0 references
 * unused = element old { empty }
 * </pre>
 *
 * @see RNCSearchUtils
 */
public class RNCCodeLenses {

	private static final Logger LOGGER = Logger.getLogger(RNCCodeLenses.class.getName());

	/**
	 * Computes code lenses for all defines in the given RNC document.
	 *
	 * @param rncDocument       the parsed RNC document containing defines.
	 * @param supportedByClient {@code true} if the client supports the
	 *                          {@code editor.action.showReferences} command.
	 * @param cancelChecker     used to abort computation if the document changes.
	 * @return a list of {@link CodeLens} objects, one per define with references.
	 */
	public List<CodeLens> getCodeLens(RNCDocument rncDocument, boolean supportedByClient,
			CancelChecker cancelChecker) {
		List<CodeLens> lenses = new ArrayList<>();
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			CharSequence text = textDocument.getTextSequence();
			List<RNCDefine> defines = rncDocument.getDefines();

			if (defines.isEmpty()) {
				return lenses;
			}

			for (RNCDefine define : defines) {
				cancelChecker.checkCanceled();
				String name = define.getName();
				if (name == null) {
					continue;
				}

				List<Location> refs = RNCSearchUtils.findReferences(textDocument, text, define, false);
				int refCount = refs.size();
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
