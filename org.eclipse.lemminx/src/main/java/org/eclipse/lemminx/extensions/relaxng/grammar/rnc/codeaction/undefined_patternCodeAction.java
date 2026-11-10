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
import org.eclipse.lemminx.commons.SnippetsBuilder;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Code action for the {@code undefined-pattern} error — offers "Did you mean"
 * suggestions and "Create define" quick fixes.
 *
 * <p>Generates up to three types of code actions:</p>
 * <ol>
 *   <li><strong>"Did you mean"</strong> — suggests similar define names
 *       (using Levenshtein distance) and built-in keywords ({@code text},
 *       {@code empty}, etc.) when the undefined name is a likely typo.</li>
 *   <li><strong>"Create define as element"</strong> — generates a new
 *       element definition at the end of the document.</li>
 *   <li><strong>"Create define as attribute"</strong> — generates a new
 *       attribute definition.</li>
 *   <li><strong>"Create define"</strong> — generates a free-form definition
 *       with a placeholder for the pattern body.</li>
 * </ol>
 *
 * <p>Use case: the user writes {@code start = roo} (typo for {@code root}).
 * The code action offers "Did you mean 'root'?" as the first quick fix.</p>
 *
 * @see RNCKeywords#BUILTIN_PATTERNS
 */
public class undefined_patternCodeAction implements IRNCCodeActionParticipant {

	/**
	 * Generates "Did you mean" suggestions for similar defines and built-in
	 * keywords, plus "Create define" code actions to define the missing pattern.
	 */
	@Override
	public void doCodeAction(RNCDocument rncDocument, Diagnostic diagnostic, TextDocumentItem document,
			boolean snippetSupported, List<CodeAction> codeActions, IRNCDocumentProvider documentProvider,
			CancelChecker cancelChecker) {
		String name = RNCErrorCode.extractName(diagnostic);
		if (name == null) {
			return;
		}

		// "Did you mean" code actions: find similar define names (local + included)
		Range diagnosticRange = diagnostic.getRange();
		if (rncDocument != null) {
			for (RNCDefine define : rncDocument.getAllVisibleDefines(null, documentProvider)) {
				String defineName = define.getName();
				if (defineName != null && !defineName.equals(name) && isSimilar(defineName, name)) {
					codeActions.add(CodeActionFactory.replace("Did you mean '" + defineName + "'?",
							diagnosticRange, defineName, document, diagnostic));
				}
			}
		}

		// "Did you mean" for built-in pattern keywords (text, empty, notAllowed, etc.)
		for (String builtin : RNCKeywords.BUILTIN_PATTERNS) {
			if (!builtin.equals(name) && isSimilar(builtin, name)) {
				codeActions.add(CodeActionFactory.replace("Did you mean '" + builtin + "'?",
						diagnosticRange, builtin, document, diagnostic));
			}
		}

		// Skip "create define" code actions for top-level pattern form schemas
		// (e.g., `element root { tex }`) where defines are not applicable.
		if (rncDocument.hasTopLevelPattern() && rncDocument.getStartDecl() == null) {
			return;
		}

		CharSequence source = rncDocument.getSource();
		Position endPos = RNCPositionUtils.getEndOfDocumentPosition(source);
		String prefix = RNCPositionUtils.endsWithNewline(source) ? "" : "\n";
		String lineSeparator = RNCPositionUtils.getLineSeparator(rncDocument.getTextDocument());

		// Code action 1: Create define as element
		StringBuilder sb = new StringBuilder(prefix);
		sb.append(name).append(" = element ").append(name).append(" {").append(lineSeparator).append("  ");
		if (snippetSupported) {
			SnippetsBuilder.tabstops(0, sb);
		}
		sb.append(lineSeparator).append("}").append(lineSeparator);
		codeActions.add(CodeActionFactory.insert("Create define '" + name + "' as element", endPos, sb.toString(),
				document, diagnostic, snippetSupported));

		// Code action 2: Create define as attribute
		sb.setLength(0);
		sb.append(prefix).append(name).append(" = attribute ").append(name).append(" { ");
		if (snippetSupported) {
			SnippetsBuilder.placeholders(1, "text", sb);
		} else {
			sb.append("text");
		}
		sb.append(" }").append(lineSeparator);
		codeActions.add(CodeActionFactory.insert("Create define '" + name + "' as attribute", endPos, sb.toString(),
				document, diagnostic, snippetSupported));

		// Code action 3: Create define (free form)
		sb.setLength(0);
		sb.append(prefix).append(name).append(" = ");
		if (snippetSupported) {
			SnippetsBuilder.placeholders(1, "pattern", sb);
		}
		sb.append(lineSeparator);
		codeActions.add(CodeActionFactory.insert("Create define '" + name + "'", endPos, sb.toString(), document,
				diagnostic, snippetSupported));
	}
}
