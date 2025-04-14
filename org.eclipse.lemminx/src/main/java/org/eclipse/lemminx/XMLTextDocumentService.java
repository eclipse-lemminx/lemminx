/**
 *  Copyright (c) 2018 Angelo ZERR.
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

import static org.eclipse.lsp4j.jsonrpc.CompletableFutures.computeAsync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.client.ExtendedClientCapabilities;
import org.eclipse.lemminx.client.LimitExceededWarner;
import org.eclipse.lemminx.client.LimitFeature;
import org.eclipse.lemminx.commons.ModelTextDocument;
import org.eclipse.lemminx.commons.ModelTextDocuments;
import org.eclipse.lemminx.commons.ModelValidatorDelayer;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.services.DocumentSymbolsResult;
import org.eclipse.lemminx.services.SymbolInformationResult;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.data.DataEntryField;
import org.eclipse.lemminx.services.extensions.save.AbstractSaveContext;
import org.eclipse.lemminx.settings.CompositeSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLCodeLensSettings;
import org.eclipse.lemminx.settings.XMLCompletionSettings;
import org.eclipse.lemminx.settings.XMLFoldingSettings;
import org.eclipse.lemminx.settings.XMLFormattingOptions;
import org.eclipse.lemminx.settings.XMLPreferences;
import org.eclipse.lemminx.settings.XMLSymbolSettings;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.ClientCapabilities;
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
import org.eclipse.lsp4j.ConfigurationItem;
import org.eclipse.lsp4j.ConfigurationParams;
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
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.google.gson.JsonPrimitive;

/**
 * XML text document service.
 *
 */
public class XMLTextDocumentService implements TextDocumentService {

	private static final Logger LOGGER = Logger.getLogger(XMLTextDocumentService.class.getName());

	private final XMLLanguageServer xmlLanguageServer;
	private final ModelTextDocuments<DOMDocument> documents;
	private final ModelValidatorDelayer<DOMDocument> xmlValidatorDelayer;

	private SharedSettings sharedSettings;
	private LimitExceededWarner limitExceededWarner;

	/**
	 * Enumeration for Validation triggered by.
	 *
	 */
	private static enum TriggeredBy {
		didOpen, //
		didChange, //
		Other;
	}

	/**
	 * Save context.
	 */
	public class SaveContext extends AbstractSaveContext {

		private final Collection<ModelTextDocument<DOMDocument>> documentsToValidate;

		private boolean isRefreshCodeLenses;

		public boolean isRefreshCodeLenses() {
			return isRefreshCodeLenses;
		}

		public void setRefreshCodeLenses(boolean isRefreshCodeLenses) {
			this.isRefreshCodeLenses = isRefreshCodeLenses;
			return;
		}

		public SaveContext(Object settings) {
			super(settings);
			this.documentsToValidate = new ArrayList<>();
		}

		public SaveContext(String uri) {
			super(uri);
			this.documentsToValidate = new ArrayList<>();
		}

		@Override
		public void collectDocumentToValidate(Predicate<DOMDocument> validateDocumentPredicate) {
			documents.all().stream().forEach(document -> {
				DOMDocument xmlDocument = document.getModel();
				if (xmlDocument != null && !documentsToValidate.contains(document)
						&& validateDocumentPredicate.test(xmlDocument)) {
					documentsToValidate.add(document);
				}
			});
		}

		@Override
		public DOMDocument getDocument(String uri) {
			return xmlLanguageServer.getDocument(uri);
		}

		public void triggerValidationIfNeeded() {
			triggerValidationFor(documentsToValidate);
		}
	}

	private boolean codeActionLiteralSupport;
	private boolean hierarchicalDocumentSymbolSupport;
	private boolean definitionLinkSupport;
	private boolean typeDefinitionLinkSupport;

	private Boolean clientConfigurationSupport;

	public XMLTextDocumentService(XMLLanguageServer xmlLanguageServer) {
		this.xmlLanguageServer = xmlLanguageServer;
		DOMParser parser = DOMParser.getInstance();
		this.documents = new ModelTextDocuments<DOMDocument>((document, cancelChecker) -> {
			return parser.parse(document, getXMLLanguageService().getResolverExtensionManager(), true, cancelChecker);
		});
		this.sharedSettings = new SharedSettings();
		this.limitExceededWarner = null;
		this.xmlValidatorDelayer = new ModelValidatorDelayer<DOMDocument>((document) -> {
			DOMDocument xmlDocument = document.getModel();
			validate(xmlDocument, Collections.emptyMap(), null);

			getXMLLanguageService().getDocumentLifecycleParticipants().forEach(participant -> {
				try {
					participant.didChange(xmlDocument);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error while processing didChange for the participant '"
							+ participant.getClass().getName() + "'.", e);
				}
			});
		});
	}

	public void updateClientCapabilities(ClientCapabilities capabilities,
			ExtendedClientCapabilities extendedClientCapabilities) {
		if (capabilities != null) {
			TextDocumentClientCapabilities textDocumentClientCapabilities = capabilities.getTextDocument();
			if (textDocumentClientCapabilities != null) {
				sharedSettings.getCompletionSettings().setCapabilities(textDocumentClientCapabilities.getCompletion());
				sharedSettings.getCodeActionSettings().setCapabilities(textDocumentClientCapabilities.getCodeAction());
				sharedSettings.getFoldingSettings().setCapabilities(textDocumentClientCapabilities.getFoldingRange());
				sharedSettings.getHoverSettings().setCapabilities(textDocumentClientCapabilities.getHover());
				sharedSettings.getValidationSettings()
						.setCapabilities(textDocumentClientCapabilities.getPublishDiagnostics());
				codeActionLiteralSupport = textDocumentClientCapabilities.getCodeAction() != null
						&& textDocumentClientCapabilities.getCodeAction().getCodeActionLiteralSupport() != null;
				hierarchicalDocumentSymbolSupport = textDocumentClientCapabilities.getDocumentSymbol() != null
						&& textDocumentClientCapabilities.getDocumentSymbol()
								.getHierarchicalDocumentSymbolSupport() != null
						&& textDocumentClientCapabilities.getDocumentSymbol().getHierarchicalDocumentSymbolSupport();
				definitionLinkSupport = textDocumentClientCapabilities.getDefinition() != null
						&& textDocumentClientCapabilities.getDefinition().getLinkSupport() != null
						&& textDocumentClientCapabilities.getDefinition().getLinkSupport();
				typeDefinitionLinkSupport = textDocumentClientCapabilities.getTypeDefinition() != null
						&& textDocumentClientCapabilities.getTypeDefinition().getLinkSupport() != null
						&& textDocumentClientCapabilities.getTypeDefinition().getLinkSupport();
			}
			// Workspace settings
			if (capabilities.getWorkspace() != null) {
				sharedSettings.getWorkspaceSettings().setCapabilities(capabilities.getWorkspace());
				clientConfigurationSupport = capabilities.getWorkspace().getConfiguration();
			}
		}
		if (extendedClientCapabilities != null) {
			// Extended client capabilities
			sharedSettings.getCodeLensSettings().setCodeLens(extendedClientCapabilities.getCodeLens());
			sharedSettings
					.setActionableNotificationSupport(extendedClientCapabilities.isActionableNotificationSupport());
			sharedSettings.setOpenSettingsCommandSupport(extendedClientCapabilities.isOpenSettingsCommandSupport());
			sharedSettings.setBindingWizardSupport(extendedClientCapabilities.isBindingWizardSupport());
		}

	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			CompletionList list = getXMLLanguageService().doComplete(xmlDocument, params.getPosition(), sharedSettings,
					cancelChecker);
			return Either.forRight(list);
		});
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		return computeDOMAsync(unresolved.getData(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().resolveCompletionItem(unresolved, xmlDocument, sharedSettings,
					cancelChecker);
		});
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().doHover(xmlDocument, params.getPosition(), sharedSettings, cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().findDocumentHighlights(xmlDocument, params.getPosition(), cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
			DocumentSymbolParams params) {

		TextDocument document = getDocument(params.getTextDocument().getUri());
		if (document == null) {
			return CompletableFuture.completedFuture(null);
		}
		XMLSymbolSettings symbolSettings = sharedSettings.getSymbolSettings();

		if (!symbolSettings.isEnabled() || symbolSettings.isExcluded(document.getUri())) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}

		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			boolean resultLimitExceeded = false;
			List<Either<SymbolInformation, DocumentSymbol>> symbols = null;

			if (hierarchicalDocumentSymbolSupport) {
				DocumentSymbolsResult result = getXMLLanguageService().findDocumentSymbols(xmlDocument, symbolSettings,
						cancelChecker);
				resultLimitExceeded = result.isResultLimitExceeded();
				symbols = result //
						.stream() //
						.map(s -> {
							Either<SymbolInformation, DocumentSymbol> e = Either.forRight(s);
							return e;
						}) //
						.collect(Collectors.toList());
			} else {
				SymbolInformationResult result = getXMLLanguageService().findSymbolInformations(xmlDocument,
						symbolSettings, cancelChecker);
				resultLimitExceeded = result.isResultLimitExceeded();
				symbols = result.stream() //
						.map(s -> {
							Either<SymbolInformation, DocumentSymbol> e = Either.forLeft(s);
							return e;
						}) //
						.collect(Collectors.toList());
			}
			if (resultLimitExceeded) {
				// send warning
				getLimitExceededWarner().onResultLimitExceeded(xmlDocument.getTextDocument().getUri(),
						LimitFeature.SYMBOLS);
			}
			return symbols;
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			CompositeSettings settings = new CompositeSettings(getSharedSettings(), params.getOptions());
			return getXMLLanguageService().format(xmlDocument, null, settings);
		});
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			CompositeSettings settings = new CompositeSettings(getSharedSettings(), params.getOptions());
			return getXMLLanguageService().format(xmlDocument, params.getRange(), settings);
		});
	}

	@Override
	public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>> prepareRename(
			PrepareRenameParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			Either<Range, PrepareRenameResult> either = getXMLLanguageService().prepareRename(xmlDocument,
					params.getPosition(), cancelChecker);
			if (either != null) {
				if (either.isLeft()) {
					return Either3.forFirst((Range) either.get());
				} else {
					return Either3.forSecond((PrepareRenameResult) either.get());
				}
			} else {
				return Either3.forThird(new PrepareRenameDefaultBehavior());
			}
		});
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().doRename(xmlDocument, params.getPosition(), params.getNewName(),
					cancelChecker);
		});
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		ModelTextDocument<DOMDocument> document = documents.onDidOpenTextDocument(params);
		triggerValidationFor(document, TriggeredBy.didOpen);
	}

	/**
	 * This method is triggered when the user types on an XML document.
	 */
	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		ModelTextDocument<DOMDocument> document = documents.onDidChangeTextDocument(params);
		triggerValidationFor(document, TriggeredBy.didChange, params.getContentChanges());
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		TextDocumentIdentifier identifier = params.getTextDocument();
		String uri = identifier.getUri();
		DOMDocument xmlDocument = documents.getExistingModel(uri);
		// Remove the document from the cache
		documents.onDidCloseTextDocument(params);
		// Remove the validation from the delayer
		xmlValidatorDelayer.cleanPendingValidation(uri);
		// Publish empty errors from the document
		xmlLanguageServer.getLanguageClient()
				.publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
		getLimitExceededWarner().evictValue(uri);
		// Manage didClose document lifecycle participants
		if (xmlDocument != null) {
			getXMLLanguageService().getDocumentLifecycleParticipants().forEach(participant -> {
				try {
					participant.didClose(xmlDocument);
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, "Error while processing didClose for the participant '"
							+ participant.getClass().getName() + "'.", e);
				}
			});
		}
	}

	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().getFoldingRanges(xmlDocument, sharedSettings.getFoldingSettings(),
					cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().findDocumentLinks(xmlDocument);
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(
			DefinitionParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			if (definitionLinkSupport) {
				return Either.forRight(
						getXMLLanguageService().findDefinition(xmlDocument, params.getPosition(), cancelChecker));
			}
			List<? extends Location> locations = getXMLLanguageService()
					.findDefinition(xmlDocument, params.getPosition(), cancelChecker) //
					.stream() //
					.map(locationLink -> XMLPositionUtility.toLocation(locationLink)) //
					.collect(Collectors.toList());
			return Either.forLeft(locations);
		});
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(
			TypeDefinitionParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			if (typeDefinitionLinkSupport) {
				return Either.forRight(
						getXMLLanguageService().findTypeDefinition(xmlDocument, params.getPosition(), cancelChecker));
			}
			List<? extends Location> locations = getXMLLanguageService()
					.findTypeDefinition(xmlDocument, params.getPosition(), cancelChecker) //
					.stream() //
					.map(locationLink -> XMLPositionUtility.toLocation(locationLink)) //
					.collect(Collectors.toList());
			return Either.forLeft(locations);
		});
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().findReferences(xmlDocument, params.getPosition(), params.getContext(),
					cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		if (!sharedSettings.getCodeLensSettings().isEnabled()) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().getCodeLens(xmlDocument, sharedSettings.getCodeLensSettings(),
					cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		String uri = params.getTextDocument().getUri();
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			XMLFormattingOptions indentationSettings = getIndentationSettings(uri);
			if (indentationSettings != null) {
				// FIXME, don't update the shared settings, but use in code action the new
				// indentationSettings.
				sharedSettings.getFormattingSettings().merge(indentationSettings);
			}

			return (List<Either<Command, CodeAction>>) getXMLLanguageService()
					.doCodeActions(params.getContext(), params.getRange(), xmlDocument, sharedSettings, cancelChecker) //
					.stream() //
					.map(ca -> {
						if (codeActionLiteralSupport) {
							Either<Command, CodeAction> e = Either.forRight(ca);
							return e;
						} else {
							List<Object> arguments = Arrays.asList(uri, xmlDocument.getTextDocument().getVersion(),
									ca.getEdit().getDocumentChanges().get(0).getLeft().getEdits());
							Command command = new Command(ca.getTitle(), "_xml.applyCodeAction", arguments);
							Either<Command, CodeAction> e = Either.forLeft(command);
							return e;
						}
					}) //
					.collect(Collectors.toList());
		});
	}

	@Override
	public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
		return computeDOMAsync(unresolved.getData(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().resolveCodeAction(unresolved, xmlDocument, sharedSettings, cancelChecker);
		});
	}

	/**
	 * Returns the indentation settings (`xml.format.tabSize` and
	 * `xml.format.insertSpaces`) for the document with the given URI.
	 *
	 * @param uri the uri of the document to get the indentation settings for
	 * @return the indentation settings (`xml.format.tabSize` and
	 *         `xml.format.insertSpaces`) for the document with the given URI
	 */
	private XMLFormattingOptions getIndentationSettings(@NonNull String uri) {
		if (clientConfigurationSupport == null || !clientConfigurationSupport.booleanValue()) {
			// The client doesn't support 'configuration/workspace'.
			return null;
		}
		ConfigurationItem insertSpaces = new ConfigurationItem();
		insertSpaces.setScopeUri(uri);
		insertSpaces.setSection("xml.format.insertSpaces");

		ConfigurationItem tabSize = new ConfigurationItem();
		tabSize.setScopeUri(uri);
		tabSize.setSection("xml.format.tabSize");

		XMLFormattingOptions newOptions = null;
		try {
			List<Object> indentationSettings = xmlLanguageServer.getLanguageClient()
					.configuration(new ConfigurationParams(Arrays.asList( //
							insertSpaces, tabSize //
					))).join();

			newOptions = new XMLFormattingOptions();
			newOptions.merge(sharedSettings.getFormattingSettings());
			if (!indentationSettings.isEmpty()) {
				if (indentationSettings.get(0) != null && (indentationSettings.get(0) instanceof JsonPrimitive)) {
					newOptions.setInsertSpaces(((JsonPrimitive) indentationSettings.get(0)).getAsBoolean());
				}
				if (indentationSettings.get(1) != null && (indentationSettings.get(1) instanceof JsonPrimitive)) {
					newOptions.setTabSize(((JsonPrimitive) indentationSettings.get(1)).getAsInt());
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error while processing getting indentation settings for code actions'.", e);
		}
		return newOptions;
	}

	@Override
	public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().getSelectionRanges(xmlDocument, params.getPositions(), cancelChecker);
		});
	}

	public CompletableFuture<LinkedEditingRanges> linkedEditingRange(LinkedEditingRangeParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().findLinkedEditingRanges(xmlDocument, params.getPosition(), cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().findDocumentColors(xmlDocument, cancelChecker);
		});
	}

	@Override
	public CompletableFuture<List<ColorPresentation>> colorPresentation(ColorPresentationParams params) {
		return computeDOMAsync(params.getTextDocument(), (xmlDocument, cancelChecker) -> {
			return getXMLLanguageService().getColorPresentations(xmlDocument, params, cancelChecker);
		});
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		computeAsync((monitor) -> {
			// A document was saved, collect documents to revalidate
			SaveContext context = new SaveContext(params.getTextDocument().getUri());
			doSave(context);

			// Manage didSave document lifecycle participants
			final DOMDocument xmlDocument = documents.getModel(params.getTextDocument().getUri());
			if (xmlDocument != null) {
				getXMLLanguageService().getDocumentLifecycleParticipants().forEach(participant -> {
					try {
						participant.didSave(xmlDocument);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Error while processing didSave for the participant '"
								+ participant.getClass().getName() + "'.", e);
					}
				});
			}
			return null;
		});
	}

	/**
	 * Update settings of the language service.
	 *
	 * @param settings
	 */
	public void updateSettings(Object settings) {
		SaveContext context = new SaveContext(settings);
		doSave(context);
	}

	void doSave(String uri) {
		SaveContext context = new SaveContext(uri);
		doSave(context);
	}

	/**
	 * Save settings or XML file.
	 *
	 * @param context
	 */
	void doSave(SaveContext context) {
		getXMLLanguageService().doSave(context);
		context.triggerValidationIfNeeded();
		if (context.isRefreshCodeLenses()) {
			xmlLanguageServer.getLanguageClient().refreshCodeLenses();
		}
	}

	private void triggerValidationFor(Collection<ModelTextDocument<DOMDocument>> documents) {
		if (!documents.isEmpty()) {
			xmlLanguageServer.schedule(() -> {
				documents.forEach(document -> {
					try {
						validate(document.getModel(), Collections.emptyMap(), null);
					} catch (CancellationException e) {
						// Ignore the error and continue to validate other documents
					}
				});
			}, 500, TimeUnit.MILLISECONDS);
		}
	}

	private void triggerValidationFor(TextDocument document, TriggeredBy triggeredBy) {
		triggerValidationFor(document, triggeredBy, null);
	}

	private void triggerValidationFor(TextDocument document, TriggeredBy triggeredBy,
			List<TextDocumentContentChangeEvent> changeEvents) {
		// Validate the DOM document
		// When validation is triggered by a didChange, we process the validation with
		// delay to avoid
		// reporting to many 'textDocument/publishDiagnostics' notifications on client
		// side.
		validate(document, triggeredBy == TriggeredBy.didChange);

	}

	/**
	 * Validate and publish diagnostics for the given DOM document.
	 *
	 * @param xmlDocument the DOM document.
	 *
	 * @throws CancellationException when the DOM document content changed and
	 *                               diagnostics must be stopped.
	 */
	@SuppressWarnings("unchecked")
	void validate(TextDocument document, boolean withDelay) throws CancellationException {
		if (withDelay) {
			xmlValidatorDelayer.validateWithDelay((ModelTextDocument<DOMDocument>) document);
		} else {
			CompletableFuture.runAsync(() -> {
				DOMDocument xmlDocument = ((ModelTextDocument<DOMDocument>) document).getModel();
				validate(xmlDocument, Collections.emptyMap(), null);
				getXMLLanguageService().getDocumentLifecycleParticipants().forEach(participant -> {
					try {
						participant.didOpen(xmlDocument);
					} catch (Exception e) {
						LOGGER.log(Level.SEVERE, "Error while processing didOpen for the participant '"
								+ participant.getClass().getName() + "'.", e);
					}
				});
			});
		}
	}

	/**
	 * Validate and publish diagnostics for the given DOM document.
	 *
	 * @param xmlDocument        the DOM document.
	 * @param validationArgs     the validation arguments.
	 * @param validationSettings
	 *
	 * @throws CancellationException when the DOM document content changed and
	 *                               diagnostics must be stopped.
	 */
	void validate(DOMDocument xmlDocument, Map<String, Object> validationArgs,
			XMLValidationRootSettings validationSettings)
			throws CancellationException {
		CancelChecker cancelChecker = xmlDocument.getCancelChecker();
		cancelChecker.checkCanceled();
		getXMLLanguageService().publishDiagnostics(xmlDocument,
				params -> xmlLanguageServer.getLanguageClient().publishDiagnostics(params),
				(doc) -> triggerValidationFor(doc, TriggeredBy.Other),
				validationSettings != null ? validationSettings : sharedSettings.getValidationSettings(),
				validationArgs, cancelChecker);
	}

	private XMLLanguageService getXMLLanguageService() {
		return xmlLanguageServer.getXMLLanguageService();
	}

	public void updateCompletionSettings(XMLCompletionSettings newCompletion) {
		sharedSettings.getCompletionSettings().merge(newCompletion);
	}

	public void updateSymbolSettings(XMLSymbolSettings newSettings) {
		sharedSettings.getSymbolSettings().merge(newSettings);
	}

	public void updateCodeLensSettings(XMLCodeLensSettings newSettings) {
		sharedSettings.getCodeLensSettings().merge(newSettings);
	}

	public void updatePreferences(XMLPreferences newPreferences) {
		sharedSettings.getPreferences().merge(newPreferences);
	}

	public XMLSymbolSettings getSharedSymbolSettings() {
		return sharedSettings.getSymbolSettings();
	}

	public XMLCodeLensSettings getSharedCodeLensSettings() {
		return sharedSettings.getCodeLensSettings();
	}

	public boolean isIncrementalSupport() {
		return documents.isIncremental();
	}

	public XMLFoldingSettings getSharedFoldingSettings() {
		return sharedSettings.getFoldingSettings();
	}

	public XMLFormattingOptions getSharedFormattingSettings() {
		return sharedSettings.getFormattingSettings();
	}

	public XMLValidationRootSettings getValidationSettings() {
		return sharedSettings.getValidationSettings();
	}

	public XMLPreferences getPreferences() {
		return sharedSettings.getPreferences();
	}

	public SharedSettings getSharedSettings() {
		return this.sharedSettings;
	}

	/**
	 * Returns the text document from the given uri.
	 *
	 * @param uri the uri
	 * @return the text document from the given uri.
	 */
	public ModelTextDocument<DOMDocument> getDocument(String uri) {
		return documents.get(uri);
	}

	public Collection<ModelTextDocument<DOMDocument>> allDocuments() {
		return documents.all();
	}

	public boolean documentIsOpen(String uri) {
		ModelTextDocument<DOMDocument> document = getDocument(uri);
		return document != null;
	}

	private <R> CompletableFuture<R> computeDOMAsync(Object data, BiFunction<DOMDocument, CancelChecker, R> code) {
		String uri = DataEntryField.getUri(data);
		if (uri == null) {
			return CompletableFuture.completedFuture(null);
		}
		TextDocumentIdentifier identifier = new TextDocumentIdentifier(uri);
		return computeDOMAsync(identifier, code);
	}

	/**
	 * Compute the DOM Document for a given uri in a future and then apply the given
	 * function.
	 *
	 * @param <R>
	 * @param documentIdentifier the document indetifier.
	 * @param code               a bi function that accepts a {@link CancelChecker}
	 *                           and parsed {@link DOMDocument} and returns the to
	 *                           be computed value
	 * @return the DOM Document for a given uri in a future and then apply the given
	 *         function.
	 */
	public <R> CompletableFuture<R> computeDOMAsync(TextDocumentIdentifier documentIdentifier,
			BiFunction<DOMDocument, CancelChecker, R> code) {
		return documents.computeModelAsync(documentIdentifier, code);
	}

	public LimitExceededWarner getLimitExceededWarner() {
		if (this.limitExceededWarner == null) {
			this.limitExceededWarner = new LimitExceededWarner(this.xmlLanguageServer);
		}
		return this.limitExceededWarner;
	}
}
