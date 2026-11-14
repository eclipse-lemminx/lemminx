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

import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codeaction.RNCCodeActions;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.codelens.RNCCodeLenses;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion.RNCCompletionResolver;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion.RNCCompletions;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.definition.RNCDefinitions;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.documentlink.RNCDocumentLinks;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.folding.RNCFoldings;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.format.RNCFormatting;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.highlight.RNCHighlighting;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.hover.RNCHover;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.reference.RNCReferences;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.rename.RNCRename;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.symbols.RNCSymbols;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;

import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Facade for all RNC (RELAX NG Compact Syntax) language features, analogous to
 * {@code XMLLanguageService} for XML documents.
 *
 * <p>This service provides the core language intelligence for {@code .rnc} files,
 * including completion, hover, go-to-definition, document highlighting, folding,
 * code lens, code actions, semantic tokens, and formatting. Each feature is
 * implemented by a dedicated delegate class in its own sub-package.</p>
 *
 * <p>Use case: {@link RNCTextDocumentService} creates an instance of this service
 * and delegates all LSP requests (completion, hover, formatting, etc.) to it.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * RNCLanguageService service = new RNCLanguageService(documentProvider);
 * CompletionList list = service.doComplete(document, position, true, cancelChecker);
 * Hover hover = service.doHover(document, position, cancelChecker);
 * List&lt;? extends TextEdit&gt; edits = service.format(document, sharedSettings);
 * </pre>
 *
 * <p>Static helper methods for identifier resolution and range creation are
 * in {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils}.</p>
 */
public class RNCLanguageService {

	private final RNCCompletions completions;
	private final RNCHover hover;
	private final RNCDefinitions definitions;
	private final RNCHighlighting highlighting;
	private final RNCReferences references;
	private final RNCRename rename;
	private final RNCFoldings foldings;
	private final RNCCodeLenses codeLenses;
	private final RNCCodeActions codeActions;

	private final RNCDocumentLinks documentLinks;
	private final RNCSymbols symbols;
	private final RNCFormatting formatting;

	private final IRNCDocumentProvider documentProvider;

	/**
	 * Creates a new RNC language service, initializing all feature delegates.
	 *
	 * @param documentProvider provides access to open RNC documents (checks
	 *                         in-memory documents first, falls back to disk).
	 */
	public RNCLanguageService(IRNCDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
		this.completions = new RNCCompletions(documentProvider);
		this.hover = new RNCHover(documentProvider);
		this.definitions = new RNCDefinitions(documentProvider);
		this.highlighting = new RNCHighlighting();
		this.references = new RNCReferences();
		this.rename = new RNCRename();
		this.foldings = new RNCFoldings();
		this.codeLenses = new RNCCodeLenses();
		this.codeActions = new RNCCodeActions(documentProvider);

		this.documentLinks = new RNCDocumentLinks();
		this.symbols = new RNCSymbols();
		this.formatting = new RNCFormatting();
	}

	// -------------------- Completion --------------------

	/**
	 * Computes completion items for the given position in an RNC document.
	 *
	 * <p>Use case: triggered when the user presses Ctrl+Space or types a
	 * trigger character, offering keyword, define, datatype, and snippet
	 * completions based on cursor context.</p>
	 *
	 * @param rncDocument    the parsed RNC document (AST).
	 * @param position       the cursor position.
	 * @param sharedSettings the shared settings (snippet support, trigger-suggest, etc.).
	 * @param cancelChecker  used to abort if the request is cancelled.
	 * @return the list of completion items.
	 */
	public CompletionList doComplete(RNCDocument rncDocument, Position position, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		return completions.doComplete(rncDocument, position, sharedSettings, cancelChecker);
	}

	/**
	 * Resolves a completion item by adding documentation (hover content).
	 *
	 * <p>Use case: the client requests lazy resolution for a selected
	 * completion item to fetch its full documentation on demand.</p>
	 *
	 * @param item           the unresolved completion item.
	 * @param rncDocument    the parsed RNC document (AST).
	 * @param sharedSettings settings for markup kind support.
	 * @param cancelChecker  used to abort if the request is cancelled.
	 * @return the resolved completion item with documentation filled in.
	 */
	public CompletionItem resolveCompletionItem(CompletionItem item, RNCDocument rncDocument,
			SharedSettings sharedSettings, CancelChecker cancelChecker) {
		return RNCCompletionResolver.resolveCompletionItem(item, rncDocument, hover, sharedSettings,
				documentProvider, cancelChecker);
	}

	// -------------------- Hover --------------------

	/**
	 * Computes hover information for the word at the given position.
	 *
	 * <p>Use case: the user hovers over a define reference, keyword, or
	 * datatype to see its documentation or definition text.</p>
	 *
	 * @param rncDocument    the parsed RNC document (AST).
	 * @param position       the cursor position.
	 * @param sharedSettings settings for markup kind support.
	 * @param cancelChecker  used to abort if the request is cancelled.
	 * @return the hover content, or {@code null} if nothing to show.
	 */
	public Hover doHover(RNCDocument rncDocument, Position position, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		return hover.doHover(rncDocument, position, sharedSettings, cancelChecker);
	}

	// -------------------- Document Highlight --------------------

	/**
	 * Finds all occurrences of the symbol at the given position for highlighting.
	 *
	 * <p>Use case: when the cursor is on a define name or reference, all
	 * usages in the document are highlighted (definition = Write,
	 * references = Read).</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param position      the cursor position.
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the list of document highlights.
	 */
	public List<DocumentHighlight> findDocumentHighlights(RNCDocument rncDocument, Position position,
			CancelChecker cancelChecker) {
		return highlighting.findDocumentHighlights(rncDocument, position, cancelChecker);
	}

	// -------------------- Definition --------------------

	/**
	 * Finds the definition location for the symbol at the given position.
	 *
	 * <p>Use case: "Go to Definition" on a define reference navigates to
	 * the define declaration; on an include URI navigates to the file.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param position      the cursor position.
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the list of definition location links.
	 */
	public List<? extends LocationLink> findDefinition(RNCDocument rncDocument, Position position,
			CancelChecker cancelChecker) {
		return definitions.findDefinition(rncDocument, position, cancelChecker);
	}

	// -------------------- References --------------------

	/**
	 * Finds all references to the symbol at the given position.
	 *
	 * <p>Use case: "Find All References" on a define name shows everywhere
	 * it is used in the document (and optionally includes the declaration).</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param position      the cursor position.
	 * @param context       whether to include the declaration itself.
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the list of reference locations.
	 */
	public List<? extends Location> findReferences(RNCDocument rncDocument, Position position,
			ReferenceContext context, CancelChecker cancelChecker) {
		return references.findReferences(rncDocument, position, context, cancelChecker);
	}

	// -------------------- Prepare Rename --------------------

	/**
	 * Validates whether rename is possible at the given position and returns
	 * the range of the symbol to rename.
	 *
	 * <p>Use case: the client calls this before showing the rename input box
	 * to verify the cursor is on a renameable symbol (define name or reference).</p>
	 *
	 * @param rncDocument the parsed RNC document (AST).
	 * @param position    the cursor position.
	 * @return the renameable range, or {@code null} if rename is not applicable.
	 */
	public Either<Range, PrepareRenameResult> prepareRename(RNCDocument rncDocument, Position position) {
		return rename.prepareRename(rncDocument, position);
	}

	// -------------------- Rename --------------------

	/**
	 * Renames the symbol at the given position across all its occurrences.
	 *
	 * <p>Use case: the user renames a define — all references and the
	 * declaration are updated to the new name.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param position      the cursor position on the symbol to rename.
	 * @param newName       the new name.
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the workspace edit with all text changes.
	 */
	public WorkspaceEdit doRename(RNCDocument rncDocument, Position position, String newName,
			CancelChecker cancelChecker) {
		return rename.doRename(rncDocument, position, newName, cancelChecker);
	}

	// -------------------- Folding --------------------

	/**
	 * Computes folding ranges for an RNC document.
	 *
	 * <p>Use case: provides fold regions for multi-line defines, includes, divs,
	 * consecutive declarations, and consecutive comment lines.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to check if the operation has been cancelled.
	 * @return a list of {@link FoldingRange} for the document.
	 */
	public List<FoldingRange> getFoldingRanges(RNCDocument rncDocument, CancelChecker cancelChecker) {
		return foldings.getFoldingRanges(rncDocument, cancelChecker);
	}

	// -------------------- CodeLens --------------------

	/**
	 * Computes code lenses for an RNC document.
	 *
	 * <p>Use case: shows reference counts above each define declaration
	 * (e.g. "2 references") as clickable code lenses.</p>
	 *
	 * @param rncDocument       the parsed RNC document (AST).
	 * @param supportedByClient whether the client supports code lens.
	 * @param cancelChecker     used to abort if the request is cancelled.
	 * @return the list of code lenses.
	 */
	public List<CodeLens> getCodeLens(RNCDocument rncDocument, boolean supportedByClient,
			CancelChecker cancelChecker) {
		return codeLenses.getCodeLens(rncDocument, supportedByClient, cancelChecker);
	}

	// -------------------- Document Links --------------------

	/**
	 * Finds document links (clickable URIs) in an RNC document.
	 *
	 * <p>Use case: the quoted URI in {@code include "types.rnc"} becomes a
	 * clickable link that opens the referenced file.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the list of document links.
	 */
	public List<DocumentLink> findDocumentLinks(RNCDocument rncDocument, CancelChecker cancelChecker) {
		return documentLinks.findDocumentLinks(rncDocument, cancelChecker);
	}

	// -------------------- Document Symbols --------------------

	/**
	 * Computes hierarchical document symbols for the Outline view.
	 *
	 * <p>Use case: populates the editor's Outline panel with a tree of
	 * namespace declarations, start, defines, includes, and nested
	 * element/attribute patterns.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return the list of hierarchical document symbols.
	 */
	public List<DocumentSymbol> findDocumentSymbols(RNCDocument rncDocument, CancelChecker cancelChecker) {
		return symbols.findDocumentSymbols(rncDocument, cancelChecker);
	}

	/**
	 * Computes flat symbol information for workspace-wide symbol search.
	 *
	 * <p>Use case: allows "Go to Symbol in Workspace" (Ctrl+T) to find
	 * RNC defines and declarations across all open files.</p>
	 *
	 * @param rncDocument   the parsed RNC document (AST).
	 * @param cancelChecker used to abort if the request is cancelled.
	 * @return a flat list of symbol information entries.
	 */
	public List<SymbolInformation> findSymbolInformations(RNCDocument rncDocument, CancelChecker cancelChecker) {
		return symbols.findSymbolInformations(rncDocument, cancelChecker);
	}

	// -------------------- Formatting --------------------

	/**
	 * Formats an RNC document by adjusting whitespace (indentation, spacing
	 * around operators, blank lines between definitions).
	 *
	 * <p>Use case: when the user triggers "Format Document" on a {@code .rnc} file.
	 * For example, {@code start=element root\{text\}} is formatted to:</p>
	 * <pre>
	 * start = element root {
	 *   text
	 * }
	 * </pre>
	 *
	 * <p>Respects {@link SharedSettings} for tab size, insert spaces vs tabs,
	 * trim trailing whitespace, insert final newline, and trim final newlines.</p>
	 *
	 * @param textDocument   the RNC text document.
	 * @param sharedSettings the formatting settings (tab size, insert spaces, etc.).
	 * @return a list of {@link TextEdit} that transform the document to the formatted form.
	 */
	public List<? extends TextEdit> format(TextDocument textDocument, SharedSettings sharedSettings,
			RNCFormattingSettings rncFormattingSettings) {
		return formatting.format(textDocument, sharedSettings, rncFormattingSettings);
	}

	// -------------------- Code Actions --------------------

	/**
	 * Computes code actions (quick fixes) for diagnostics in the given range.
	 *
	 * <p>Use case: when the user clicks the light bulb on an error, code
	 * actions offer fixes like "Create define", "Did you mean", "Add start",
	 * "Add closing brace", etc.</p>
	 *
	 * @param rncDocument      the parsed RNC document (AST).
	 * @param range            the range for which code actions are requested.
	 * @param context          the code action context containing diagnostics.
	 * @param snippetSupported whether the client supports snippet text edits.
	 * @param resolveSupported whether the client supports lazy resolution.
	 * @param cancelChecker    used to abort if the request is cancelled.
	 * @return the list of code actions.
	 */
	public List<CodeAction> doCodeActions(RNCDocument rncDocument, Range range, CodeActionContext context,
			boolean snippetSupported, boolean resolveSupported, CancelChecker cancelChecker) {
		return codeActions.doCodeActions(rncDocument, range, context, snippetSupported, resolveSupported,
				cancelChecker);
	}

	/**
	 * Resolves a code action by computing its workspace edit lazily.
	 *
	 * <p>Use case: for code actions like "Extract define" that require
	 * computing edits on demand rather than up front.</p>
	 *
	 * @param rncDocument      the parsed RNC document (AST).
	 * @param unresolved       the code action to resolve.
	 * @param snippetSupported whether the client supports snippet text edits.
	 * @return the resolved code action with its edit populated.
	 */
	public CodeAction resolveCodeAction(RNCDocument rncDocument, CodeAction unresolved,
			boolean snippetSupported) {
		return codeActions.resolveCodeAction(rncDocument, unresolved, snippetSupported);
	}

}
