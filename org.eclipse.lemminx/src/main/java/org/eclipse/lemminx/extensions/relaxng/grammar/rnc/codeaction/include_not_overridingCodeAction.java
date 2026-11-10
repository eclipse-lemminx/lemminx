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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction;

import static org.eclipse.lemminx.utils.StringUtils.isSimilar;

import java.util.List;

import org.eclipse.lemminx.commons.CodeActionFactory;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCStart;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code include-not-overriding} error — offers "Did you
 * mean" suggestions and a list of overridable defines from the included file.
 *
 * <p>Use case: the user writes {@code xparagraph = ...} inside an include body
 * but the included file defines {@code paragraph}, not {@code xparagraph}.
 * The code action offers "Did you mean 'paragraph'?" and lists all other
 * overridable defines ({@code start}, {@code header}, etc.).</p>
 *
 * <pre>
 * include "base.rnc" {
 *   xparagraph = element p { text }   ← error: does not override anything
 * }
 * </pre>
 *
 * @see RNCErrorCode#IncludeNotOverriding
 */
public class include_not_overridingCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Generates "Did you mean" and "Replace with" code actions by resolving
	 * the included file and comparing its defines with the misspelled name.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		String name = RNCErrorCode.extractName(diagnostic);
		if (name == null || rncDocument == null) {
			return;
		}

		Range diagnosticRange = diagnostic.getRange();

		for (RNCInclude include : rncDocument.getIncludes()) {
			if (!include.hasBody()) {
				continue;
			}
			String resolvedUri = RNCIncludeUtils.resolveUri(include, rncDocument.getUri());
			RNCDocument includedDoc = documentProvider.getDocument(resolvedUri);
			if (includedDoc == null) {
				continue;
			}

			// "Did you mean" for similar define names
			RNCStart startDecl = includedDoc.getStartDecl();
			if (startDecl != null && !RNCKeywords.START.equals(name) && isSimilar(RNCKeywords.START, name)) {
				codeActions.add(CodeActionFactory.replace("Did you mean 'start'?",
						diagnosticRange, RNCKeywords.START, document, diagnostic));
			}
			for (RNCDefine def : includedDoc.getDefines()) {
				String defineName = def.getName();
				if (defineName != null && !defineName.equals(name) && isSimilar(defineName, name)) {
					codeActions.add(CodeActionFactory.replace("Did you mean '" + defineName + "'?",
							diagnosticRange, defineName, document, diagnostic));
				}
			}

			// List all overridable defines
			if (startDecl != null && !RNCKeywords.START.equals(name)) {
				codeActions.add(CodeActionFactory.replace("Replace with 'start'",
						diagnosticRange, RNCKeywords.START, document, diagnostic));
			}
			for (RNCDefine def : includedDoc.getDefines()) {
				String defineName = def.getName();
				if (defineName != null && !defineName.equals(name) && !isSimilar(defineName, name)) {
					codeActions.add(CodeActionFactory.replace("Replace with '" + defineName + "'",
							diagnosticRange, defineName, document, diagnostic));
				}
			}
			break;
		}
	}
}
