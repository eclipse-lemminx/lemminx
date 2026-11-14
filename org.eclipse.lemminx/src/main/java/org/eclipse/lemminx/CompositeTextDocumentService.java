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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Composite text document service that routes LSP requests to the appropriate
 * {@link AbstractTextDocumentService} based on file type.
 *
 * <p>
 * Contributed services are discovered via {@link ServiceLoader} using the
 * {@link ITextDocumentServiceContribution} SPI. If no contributed service
 * handles a given URI, requests are routed to the default
 * {@link XMLTextDocumentService}.
 * </p>
 */
public class CompositeTextDocumentService implements TextDocumentService {

	private static final Logger LOGGER = Logger.getLogger(CompositeTextDocumentService.class.getName());

	private final XMLTextDocumentService defaultService;
	private final List<AbstractTextDocumentService<?>> contributedServices;

	public CompositeTextDocumentService(XMLTextDocumentService defaultService, XMLLanguageServer server) {
		this.defaultService = defaultService;
		this.contributedServices = new ArrayList<>();
		loadContributions(server);
	}

	private void loadContributions(XMLLanguageServer server) {
		Iterator<ITextDocumentServiceContribution> contributions = ServiceLoader
				.load(ITextDocumentServiceContribution.class).iterator();
		while (contributions.hasNext()) {
			try {
				ITextDocumentServiceContribution contribution = contributions.next();
				AbstractTextDocumentService<?> service = contribution.createService(server);
				if (service != null) {
					contributedServices.add(service);
					LOGGER.info("Registered text document service: " + service.getClass().getName());
				}
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error loading text document service contribution", e);
			}
		}
	}

	public XMLTextDocumentService getXMLTextDocumentService() {
		return defaultService;
	}

	public void setClientConfigurationSupport(Boolean supported) {
		defaultService.setClientConfigurationSupport(supported);
		for (AbstractTextDocumentService<?> service : contributedServices) {
			service.setClientConfigurationSupport(supported);
		}
	}

	public void clearAllScopedSettings() {
		defaultService.clearAllScopedSettings();
		for (AbstractTextDocumentService<?> service : contributedServices) {
			service.clearAllScopedSettings();
		}
	}

	public void updateContributedSettings(Object initSettings) {
		for (AbstractTextDocumentService<?> service : contributedServices) {
			service.updateSettings(initSettings);
		}
	}

	private TextDocumentService getService(String uri) {
		for (AbstractTextDocumentService<?> service : contributedServices) {
			if (service.canHandle(uri)) {
				return service;
			}
		}
		return defaultService;
	}

	// -------------------- Document lifecycle --------------------

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		getService(params.getTextDocument().getUri()).didOpen(params);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		getService(params.getTextDocument().getUri()).didChange(params);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		getService(params.getTextDocument().getUri()).didClose(params);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		getService(params.getTextDocument().getUri()).didSave(params);
	}

	// -------------------- Query methods --------------------

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return getService(params.getTextDocument().getUri()).completion(params);
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		String uri = DataEntryField.getUri(unresolved.getData());
		if (uri != null) {
			return getService(uri).resolveCompletionItem(unresolved);
		}
		return defaultService.resolveCompletionItem(unresolved);
	}

	@Override
	public CompletableFuture<Either<List<InlineCompletionItem>, InlineCompletionList>> inlineCompletion(
			InlineCompletionParams params) {
		return getService(params.getTextDocument().getUri()).inlineCompletion(params);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return getService(params.getTextDocument().getUri()).hover(params);
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return getService(params.getTextDocument().getUri()).documentHighlight(params);
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return getService(params.getTextDocument().getUri()).documentSymbol(params);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return getService(params.getTextDocument().getUri()).formatting(params);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return getService(params.getTextDocument().getUri()).rangeFormatting(params);
	}

	@Override
	public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
			PrepareRenameParams params) {
		return getService(params.getTextDocument().getUri()).prepareRename(params);
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return getService(params.getTextDocument().getUri()).rename(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return getService(params.getTextDocument().getUri()).definition(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(
			TypeDefinitionParams params) {
		return getService(params.getTextDocument().getUri()).typeDefinition(params);
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return getService(params.getTextDocument().getUri()).references(params);
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return getService(params.getTextDocument().getUri()).codeAction(params);
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		String uri = DataEntryField.getUri(unresolved.getData());
		if (uri != null) {
			return getService(uri).resolveCodeAction(unresolved);
		}
		return defaultService.resolveCodeAction(unresolved);
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		return getService(params.getTextDocument().getUri()).codeLens(params);
	}

	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return getService(params.getTextDocument().getUri()).foldingRange(params);
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return getService(params.getTextDocument().getUri()).documentLink(params);
	}

	@Override
	public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
		return getService(params.getTextDocument().getUri()).selectionRange(params);
	}

	@Override
	public CompletableFuture<LinkedEditingRanges> linkedEditingRange(LinkedEditingRangeParams params) {
		return getService(params.getTextDocument().getUri()).linkedEditingRange(params);
	}

	@Override
	public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
		return getService(params.getTextDocument().getUri()).documentColor(params);
	}

	@Override
	public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
		return getService(params.getTextDocument().getUri()).colorPresentation(params);
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		return getService(params.getTextDocument().getUri()).semanticTokensFull(params);
	}

	@Override
	public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
		return getService(params.getTextDocument().getUri()).inlayHint(params);
	}
}
