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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.format;

import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;

/**
 * Entry point for RNC (RELAX NG Compact Syntax) document formatting.
 *
 * <p>
 * Delegates to {@link RNCFormatterDocument} which performs scanner-based
 * formatting by analyzing whitespace between tokens and producing
 * {@link TextEdit}s where the actual whitespace differs from the expected
 * formatting.
 * </p>
 *
 * <p>
 * Use case: invoked by the LSP formatting request
 * ({@code textDocument/formatting}) when the user formats an {@code .rnc}
 * file. The formatting respects the user's {@link SharedSettings} for
 * indentation (tabs vs spaces, tab size), trailing whitespace, and final
 * newline preferences.
 * </p>
 *
 * <p>
 * Example — before formatting:
 * </p>
 *
 * <pre>
 * start=element root{text}
 * </pre>
 *
 * <p>
 * After formatting:
 * </p>
 *
 * <pre>
 * start = element root {
 *   text
 * }
 * </pre>
 *
 * @see RNCFormatterDocument
 */
public class RNCFormatting {

	/**
	 * Formats the given RNC document and returns a list of {@link TextEdit}s
	 * that, when applied, produce a well-formatted document.
	 *
	 * <p>
	 * Use case: called from {@code RNCLanguageService.format()} which is invoked
	 * by the LSP {@code textDocument/formatting} request handler in
	 * {@code RNCTextDocumentService}.
	 * </p>
	 *
	 * @param textDocument   the RNC document to format.
	 * @param sharedSettings the user's formatting preferences (tab size,
	 *                       insert spaces, trim trailing whitespace, etc.).
	 * @return a list of text edits to apply to the document. Returns an empty
	 *         list if the document is already properly formatted.
	 */
	public List<? extends TextEdit> format(TextDocument textDocument, SharedSettings sharedSettings,
			RNCFormattingSettings rncFormattingSettings) {
		RNCFormatterDocument formatterDocument = new RNCFormatterDocument(textDocument, sharedSettings,
				rncFormattingSettings);
		return formatterDocument.format();
	}
}
