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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.AbstractTextDocumentService;
import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.commons.ModelTextDocument;
import org.eclipse.lemminx.commons.ModelValidatorDelayer;
import org.eclipse.lemminx.commons.config.ConfigurationField;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCValidator;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCSettings;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLGeneralClientSettings;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.WorkspaceEdit;

import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;

/**
 * LSP text document service for RNC (RELAX NG Compact Syntax) {@code .rnc} files.
 *
 * <p>This service is registered with the {@code CompositeTextDocumentService} and
 * handles all LSP requests for files matching the {@code .rnc} extension. It routes
 * each request (completion, hover, formatting, etc.) to the {@link RNCLanguageService}
 * facade, analogous to how {@code XMLTextDocumentService} routes requests to
 * {@code XMLLanguageService}.</p>
 *
 * <p>Use case: when a user opens, edits, or closes a {@code .rnc} file in the editor,
 * this service is selected by {@link #canHandle(String)} and manages the full
 * lifecycle: parsing, validation with delay, and responding to LSP feature requests.</p>
 *
 * <p>Validation flow:</p>
 * <ul>
 *   <li>{@code onDidOpen} → validates immediately (async, no delay)</li>
 *   <li>{@code onDidChange} → validates with delay via {@link ModelValidatorDelayer}
 *       to debounce rapid keystrokes</li>
 *   <li>{@code onDidClose} → cancels any pending validation for the closed URI</li>
 * </ul>
 *
 * <p>Validation itself uses {@link RNCValidator} (Jing's {@code CompactParseable}) and
 * is version-aware: if the document changes while validation is running, the result is
 * discarded via {@link CancellationException}.</p>
 */
public class RNCTextDocumentService extends AbstractTextDocumentService<RNCDocument> {

	private static final Logger LOGGER = Logger.getLogger(RNCTextDocumentService.class.getName());

	/** The RNC language service that provides all feature implementations. */
	private final RNCLanguageService rncLanguageService;

	/** Delays validation after document changes to debounce rapid keystrokes. */
	private final ModelValidatorDelayer<RNCDocument> validatorDelayer;

	private boolean hierarchicalDocumentSymbolSupport;

	private RNCFormattingSettings rncFormattingSettings;

	/**
	 * Creates a new RNC text document service.
	 *
	 * @param server the XML language server that hosts this service.
	 */
	public RNCTextDocumentService(XMLLanguageServer server) {
		super(server, (document, cancelChecker) -> {
			RNCDocument rncDocument = RNCParser.getInstance().parse(document.getTextSequence(), document.getUri());
			rncDocument.setTextDocument(document);
			return rncDocument;
		});
		IRNCDocumentProvider documentProvider = uri -> {
			RNCDocument openDoc = documents.getExistingModel(uri);
			if (openDoc != null) {
				return openDoc;
			}
			return RNCIncludeUtils.parseFile(uri);
		};
		this.rncLanguageService = new RNCLanguageService(documentProvider);
		this.validatorDelayer = new ModelValidatorDelayer<>((document) -> {
			validate(document, document.getVersion());
		});
	}

	@Override
	public void updateSettings(Object initSettings) {
		RNCSettings rncSettings = RNCSettings.getRNCSettings(initSettings);
		if (rncSettings != null) {
			rncFormattingSettings = rncSettings.getFormat();
		}
	}

	@Override
	protected List<ConfigurationField> getScopedFields() {
		return XMLGeneralClientSettings.SCOPED_FIELDS;
	}

	/**
	 * Determines whether this service handles the given document URI.
	 *
	 * <p>Returns {@code true} for URIs ending with {@code .rnc} (RELAX NG Compact Syntax files).</p>
	 *
	 * @param uri the document URI to check.
	 * @return {@code true} if the URI points to a {@code .rnc} file.
	 */
	@Override
	public boolean canHandle(String uri) {
		return DOMUtils.isRelaxNGUriCompactSyntax(uri);
	}

	// -------------------- Lifecycle --------------------

	/**
	 * Called when an RNC document is opened. Triggers immediate validation
	 * (no delay) so the user sees diagnostics right away.
	 */
	@Override
	protected void onDidOpen(ModelTextDocument<RNCDocument, SharedSettings> document) {
		triggerValidation(document, false);
	}

	/**
	 * Called when an RNC document is changed. Triggers delayed validation
	 * to debounce rapid keystrokes and avoid unnecessary re-validation.
	 */
	@Override
	protected void onDidChange(ModelTextDocument<RNCDocument, SharedSettings> document) {
		triggerValidation(document, true);
	}

	/**
	 * Called when an RNC document is closed. Cancels any pending validation
	 * to avoid publishing stale diagnostics for a closed file.
	 */
	@Override
	protected void onDidClose(String uri) {
		validatorDelayer.cleanPendingValidation(uri);
	}

	/**
	 * Triggers validation either immediately or with a delay.
	 *
	 * @param document  the document to validate.
	 * @param withDelay {@code true} to debounce (used on change), {@code false} for immediate (used on open).
	 */
	private void triggerValidation(ModelTextDocument<RNCDocument, SharedSettings> document, boolean withDelay) {
		if (withDelay) {
			validatorDelayer.validateWithDelay(document);
		} else {
			int version = document.getVersion();
			CompletableFuture.runAsync(() -> {
				validate(document, version);
			});
		}
	}

	/**
	 * Validates the RNC document using Jing's {@code CompactParseable} and publishes
	 * diagnostics. If the document version changes during validation (user kept typing),
	 * the result is discarded to avoid publishing stale diagnostics.
	 *
	 * @param document     the document to validate.
	 * @param startVersion the document version when validation was initiated.
	 */
	private void validate(ModelTextDocument<RNCDocument, ?> document, int startVersion) {
		try {
			// Cancel checker that detects if the document was modified since validation started
			CancelChecker cancelChecker = () -> {
				if (document.getVersion() != startVersion) {
					throw new CancellationException("Document version changed");
				}
			};
			cancelChecker.checkCanceled();
			List<Diagnostic> diagnostics = RNCValidator.validate(document,
					isUnnecessaryTagSupported(), cancelChecker);
			cancelChecker.checkCanceled();
			publishDiagnostics(document.getUri(), diagnostics);
		} catch (CancellationException e) {
			// Document was modified, skip publishing diagnostics
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while validating RNC document.", e);
		}
	}

	/**
	 * Returns {@code true} if the client supports the {@link DiagnosticTag#Unnecessary}
	 * tag, allowing unused defines to be rendered with a fade-out effect.
	 */
	private boolean isUnnecessaryTagSupported() {
		return getSharedSettings().getValidationSettings()
				.isTagSupported(DiagnosticTag.Unnecessary);
	}

	// -------------------- Completion --------------------

	/**
	 * Handles {@code textDocument/completion} for RNC files.
	 *
	 * <p>Use case: provides keyword, snippet, and define-reference completions
	 * based on the cursor context (top-level, after assign, inside pattern, etc.).</p>
	 */
	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return Either.forRight(rncLanguageService.doComplete(
					rncDocument, params.getPosition(), getSharedSettings(), cancelChecker));
		});
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		String uri = DataEntryField.getUri(unresolved.getData());
		if (uri == null) {
			return CompletableFuture.completedFuture(unresolved);
		}
		return computeModelAsync(new TextDocumentIdentifier(uri), (rncDocument, cancelChecker) -> {
			return rncLanguageService.resolveCompletionItem(unresolved, rncDocument, getSharedSettings(),
					cancelChecker);
		});
	}

	// -------------------- Hover --------------------

	/**
	 * Handles {@code textDocument/hover} for RNC files.
	 *
	 * <p>Use case: shows define declarations or keyword documentation on hover.</p>
	 */
	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.doHover(rncDocument, params.getPosition(), getSharedSettings(), cancelChecker);
		});
	}

	// -------------------- Document Highlight --------------------

	/**
	 * Handles {@code textDocument/documentHighlight} for RNC files.
	 *
	 * <p>Use case: highlights the definition (Write) and all references (Read)
	 * of a define name when the cursor is placed on it.</p>
	 */
	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.findDocumentHighlights(rncDocument, params.getPosition(), cancelChecker);
		});
	}

	// -------------------- Document Symbols --------------------

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			if (isHierarchicalDocumentSymbolSupport()) {
				return rncLanguageService.findDocumentSymbols(rncDocument, cancelChecker)
						.stream()
						.map(s -> {
							Either<SymbolInformation, DocumentSymbol> e = Either.forRight(s);
							return e;
						})
						.collect(Collectors.toList());
			}
			return rncLanguageService.findSymbolInformations(rncDocument, cancelChecker)
					.stream()
					.map(s -> {
						Either<SymbolInformation, DocumentSymbol> e = Either.forLeft(s);
						return e;
					})
					.collect(Collectors.toList());
		});
	}

	private boolean isHierarchicalDocumentSymbolSupport() {
		if (hierarchicalDocumentSymbolSupport) {
			return true;
		}
		try {
			hierarchicalDocumentSymbolSupport = xmlLanguageServer.getCapabilityManager()
					.getClientCapabilities().capabilities.getTextDocument()
					.getDocumentSymbol().getHierarchicalDocumentSymbolSupport();
		} catch (NullPointerException e) {
			hierarchicalDocumentSymbolSupport = false;
		}
		return hierarchicalDocumentSymbolSupport;
	}

	// -------------------- Definition --------------------

	/**
	 * Handles {@code textDocument/definition} for RNC files.
	 *
	 * <p>Use case: navigates from a define reference (e.g. {@code root} in
	 * {@code start = root}) to its declaration ({@code root = element root \{ ... \}}).</p>
	 */
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			List<? extends LocationLink> links = rncLanguageService.findDefinition(
					rncDocument, params.getPosition(), cancelChecker);
			return Either.forRight(links);
		});
	}

	// -------------------- References --------------------

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.findReferences(
					rncDocument, params.getPosition(), params.getContext(), cancelChecker);
		});
	}

	// -------------------- Prepare Rename --------------------

	@Override
	public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
			PrepareRenameParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			Either<Range, PrepareRenameResult> result = rncLanguageService.prepareRename(
					rncDocument, params.getPosition());
			if (result == null) {
				return null;
			}
			if (result.isLeft()) {
				return Either3.forFirst(result.getLeft());
			}
			return Either3.forSecond(result.getRight());
		});
	}

	// -------------------- Rename --------------------

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.doRename(
					rncDocument, params.getPosition(), params.getNewName(), cancelChecker);
		});
	}

	// -------------------- Folding --------------------

	/**
	 * Handles {@code textDocument/foldingRange} for RNC files.
	 *
	 * <p>Use case: provides fold regions for brace blocks and consecutive comment groups.</p>
	 */
	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.getFoldingRanges(rncDocument, cancelChecker);
		});
	}

	// -------------------- Document Links --------------------

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return rncLanguageService.findDocumentLinks(rncDocument, cancelChecker);
		});
	}

	// -------------------- Code Lens --------------------

	/**
	 * Handles {@code textDocument/codeLens} for RNC files.
	 *
	 * <p>Use case: shows "N references" above each define declaration.</p>
	 */
	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		if (!getSharedSettings().getCodeLensSettings().isEnabled()) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			boolean supportedByClient = getSharedSettings().getCodeLensSettings()
					.isSupportedByClient(CodeLensKind.References);
			return rncLanguageService.getCodeLens(rncDocument, supportedByClient, cancelChecker);
		});
	}

	// -------------------- Code Action --------------------

	/**
	 * Handles {@code textDocument/codeAction} for RNC files.
	 *
	 * <p>Use case: provides quick-fix code actions for parser errors (e.g. insert
	 * missing {@code \}}, add missing {@code =}, insert URI placeholder).</p>
	 */
	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		return computeModelAsync(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			SharedSettings settings = getSharedSettings();
			boolean snippetSupported = settings.getWorkspaceSettings().isSnippetEditSupported();
			boolean resolveSupported = settings.getCodeActionSettings().canSupportResolve();
			List<CodeAction> actions = rncLanguageService.doCodeActions(
					rncDocument, params.getRange(), params.getContext(), snippetSupported,
					resolveSupported, cancelChecker);
			return actions.stream()
					.map(ca -> {
						Either<Command, CodeAction> e = Either.forRight(ca);
						return e;
					})
					.collect(Collectors.toList());
		});
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		String uri = DataEntryField.getUri(unresolved.getData());
		if (uri == null) {
			return CompletableFuture.completedFuture(unresolved);
		}
		return computeModelAsync(new TextDocumentIdentifier(uri), (rncDocument, cancelChecker) -> {
			SharedSettings settings = getSharedSettings();
			boolean snippetSupported = settings.getWorkspaceSettings().isSnippetEditSupported();
			return rncLanguageService.resolveCodeAction(rncDocument, unresolved, snippetSupported);
		});
	}

	// -------------------- Formatting --------------------

	/**
	 * Handles {@code textDocument/formatting} for RNC files.
	 *
	 * <p>Use case: formats the entire RNC document, adjusting indentation, spacing
	 * around operators, and blank lines between top-level definitions.</p>
	 */
	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return computeModelAsyncCompose(params.getTextDocument(), (rncDocument, cancelChecker) -> {
			return getOrPullScopedSettings(params.getTextDocument().getUri())
					.thenApply(scopedSettings -> {
						return rncLanguageService.format(
								documents.get(params.getTextDocument().getUri()),
								scopedSettings, rncFormattingSettings);
					});
		});
	}

}
