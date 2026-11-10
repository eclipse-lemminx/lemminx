/**
 *  Copyright (c) 2026 Angelo ZERR.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.eclipse.lemminx.commons.ModelTextDocument;
import org.eclipse.lemminx.commons.ModelTextDocuments;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.commons.config.ConfigurationField;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentColorParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.InlineCompletionList;
import org.eclipse.lsp4j.InlineCompletionParams;
import org.eclipse.lsp4j.LinkedEditingRangeParams;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Abstract base class for text document services that handle a specific file
 * type (e.g., XML, RNC).
 *
 * <p>
 * Provides common infrastructure:
 * <ul>
 * <li>Document management via {@link ModelTextDocuments}</li>
 * <li>Lifecycle methods ({@code didOpen}, {@code didChange},
 * {@code didClose})</li>
 * <li>Default (empty) implementations of all LSP query methods</li>
 * <li>Helper methods for publishing diagnostics and async model
 * computation</li>
 * </ul>
 *
 * @param <T> the model type (e.g., DOMDocument, RNCDocument)
 */
public abstract class AbstractTextDocumentService<T> implements TextDocumentService {

	protected final XMLLanguageServer xmlLanguageServer;
	protected final ModelTextDocuments<T, SharedSettings> documents;

	private Boolean clientConfigurationSupport;
	private volatile int settingsGeneration;

	protected AbstractTextDocumentService(XMLLanguageServer xmlLanguageServer,
			BiFunction<TextDocument, CancelChecker, T> parse) {
		this.xmlLanguageServer = xmlLanguageServer;
		this.documents = new ModelTextDocuments<>(parse);
	}

	/**
	 * Returns whether this service can handle the document with the given URI.
	 *
	 * @param uri the document URI.
	 * @return true if this service handles the file type.
	 */
	public abstract boolean canHandle(String uri);

	// -------------------- Document lifecycle --------------------

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		ModelTextDocument<T, SharedSettings> document = documents.onDidOpenTextDocument(params);
		onDidOpen(document);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		ModelTextDocument<T, SharedSettings> document = documents.onDidChangeTextDocument(params);
		onDidChange(document);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		String uri = params.getTextDocument().getUri();
		onDidClose(uri);
		publishDiagnostics(uri, Collections.emptyList());
		documents.onDidCloseTextDocument(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// Default: no-op
	}

	/**
	 * Called after a document has been opened and registered.
	 * Override to trigger validation, notify participants, etc.
	 */
	protected void onDidOpen(ModelTextDocument<T, SharedSettings> document) {
	}

	/**
	 * Called after a document has been changed.
	 * Override to trigger validation, notify participants, etc.
	 */
	protected void onDidChange(ModelTextDocument<T, SharedSettings> document) {
	}

	/**
	 * Called when a document is about to be closed.
	 * Override to clean up resources, cancel pending validation, etc.
	 */
	protected void onDidClose(String uri) {
	}

	// -------------------- Document access --------------------

	public ModelTextDocument<T, SharedSettings> getDocument(String uri) {
		return documents.get(uri);
	}

	public Collection<ModelTextDocument<T, SharedSettings>> allDocuments() {
		return documents.all();
	}

	public boolean documentIsOpen(String uri) {
		return documents.get(uri) != null;
	}

	public boolean isIncrementalSupport() {
		return documents.isIncremental();
	}

	// -------------------- Helpers --------------------

	protected SharedSettings getSharedSettings() {
		return xmlLanguageServer.getSharedSettings();
	}

	protected void publishDiagnostics(String uri, List<Diagnostic> diagnostics) {
		xmlLanguageServer.getLanguageClient()
				.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
	}

	public <R> CompletableFuture<R> computeModelAsync(TextDocumentIdentifier documentIdentifier,
			BiFunction<T, CancelChecker, R> code) {
		return documents.computeModelAsync(documentIdentifier, code);
	}

	public <R> CompletableFuture<R> computeModelAsyncCompose(TextDocumentIdentifier documentIdentifier,
			BiFunction<T, CancelChecker, CompletableFuture<R>> code) {
		return documents.computeModelAsyncCompose(documentIdentifier, code);
	}

	// -------------------- Settings --------------------

	/**
	 * Called when the client sends updated settings. Subclasses may override
	 * to extract settings specific to their file type.
	 *
	 * @param initSettings the settings object (content under {@code xml}).
	 */
	public void updateSettings(Object initSettings) {
		// Default: no-op
	}

	// -------------------- Scoped settings --------------------

	/**
	 * Returns the list of configuration fields that can be scoped per folder
	 * via {@code workspace/configuration}. Subclasses should override to
	 * return the appropriate scoped fields for their file type.
	 *
	 * @return the scoped fields, or {@code null} if scoped settings are not
	 *         supported by this service.
	 */
	protected List<ConfigurationField> getScopedFields() {
		return null;
	}

	/**
	 * Sets whether the client supports {@code workspace/configuration}.
	 * Called during capability negotiation.
	 *
	 * @param supported {@code true} if the client supports configuration pull.
	 */
	public void setClientConfigurationSupport(Boolean supported) {
		this.clientConfigurationSupport = supported;
	}

	/**
	 * Clears all cached scoped settings, forcing a re-pull on next request.
	 */
	public void clearAllScopedSettings() {
		settingsGeneration++;
		documents.clearAllSettings();
	}

	/**
	 * Returns scoped settings for the given document URI. If the client
	 * supports {@code workspace/configuration} and scoped fields are defined,
	 * pulls per-folder settings from the client. Otherwise returns the global
	 * shared settings.
	 *
	 * @param uri the document URI to scope settings to.
	 * @return a future resolving to the (possibly scoped) shared settings.
	 */
	protected CompletableFuture<SharedSettings> getOrPullScopedSettings(String uri) {
		List<ConfigurationField> scopedFields = getScopedFields();
		if (clientConfigurationSupport == null || !clientConfigurationSupport.booleanValue()
				|| scopedFields == null || scopedFields.isEmpty()) {
			return CompletableFuture.completedFuture(getSharedSettings());
		}
		ModelTextDocument<T, SharedSettings> doc = documents.get(uri);
		if (doc != null) {
			SharedSettings cached = doc.getSettings();
			if (cached != null) {
				return CompletableFuture.completedFuture(cached);
			}
		}
		final int generationAtRequest = settingsGeneration;
		SharedSettings sharedSettings = getSharedSettings();
		ConfigurationParams params = ConfigurationField.createConfigurationParams(uri, scopedFields);
		CompletableFuture<List<Object>> configFuture = xmlLanguageServer.getLanguageClient()
				.configuration(params);
		return configFuture
				.thenApply(results -> {
					if (results == null) {
						return sharedSettings;
					}
					SharedSettings scoped = new SharedSettings(sharedSettings);
					ConfigurationField.applyConfigurationResults(scoped, scopedFields, results);
					if (ConfigurationField.haveSameValues(scoped, sharedSettings, scopedFields)) {
						if (doc != null && settingsGeneration == generationAtRequest) {
							doc.setSettings(sharedSettings);
						}
						return sharedSettings;
					}
					if (doc != null && settingsGeneration == generationAtRequest) {
						doc.setSettings(scoped);
					}
					return scoped;
				});
	}

	// -------------------- Default LSP method implementations --------------------

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return CompletableFuture.completedFuture(Either.forRight(new CompletionList()));
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return CompletableFuture.completedFuture(unresolved);
	}

	@Override
	public CompletableFuture<Either<List<InlineCompletionItem>, InlineCompletionList>> inlineCompletion(
			InlineCompletionParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
			PrepareRenameParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(
			TypeDefinitionParams params) {
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		return CompletableFuture.completedFuture(unresolved);
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<LinkedEditingRanges> linkedEditingRange(LinkedEditingRangeParams params) {
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		return CompletableFuture.completedFuture(new SemanticTokens(Collections.emptyList()));
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
}
