/**
 *  Copyright (c) 2018, 2023 Angelo ZERR
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
package org.eclipse.lemminx.services;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.customservice.AutoCloseTagResponse;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationSettings;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.diagnostics.DiagnosticsResult;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLCodeLensSettings;
import org.eclipse.lemminx.settings.XMLCompletionSettings;
import org.eclipse.lemminx.settings.XMLFoldingSettings;
import org.eclipse.lemminx.settings.XMLSymbolSettings;
import org.eclipse.lemminx.utils.TextEditUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * XML Language service.
 *
 */
public class XMLLanguageService extends XMLExtensionsRegistry implements IXMLFullFormatter {

	private static final CancelChecker NULL_CHECKER = new CancelChecker() {

		@Override
		public void checkCanceled() {
			// Do nothing
		}
	};

	private final XMLFormatter formatter;
	private final XMLHighlighting highlighting;
	private final XMLSymbolsProvider symbolsProvider;
	private final XMLCompletions completions;
	private final XMLHover hover;
	private final XMLDiagnostics diagnostics;
	private final XMLFoldings foldings;
	private final XMLDocumentLink documentLink;
	private final XMLDefinition definition;
	private final XMLTypeDefinition typeDefinition;
	private final XMLReference reference;
	private final XMLCodeLens codelens;
	private final XMLCodeActions codeActions;
	private final XMLPrepareRename prepareRename;
	private final XMLRename rename;
	private final XMLSelectionRanges selectionRanges;
	private final XMLLinkedEditing linkedEditing;
	private final XMLDocumentColor documentColor;

	private boolean retriggerValidationWhenDownloadError;

	public XMLLanguageService() {
		this.formatter = new XMLFormatter(this);
		this.highlighting = new XMLHighlighting(this);
		this.symbolsProvider = new XMLSymbolsProvider(this);
		this.completions = new XMLCompletions(this);
		this.hover = new XMLHover(this);
		this.diagnostics = new XMLDiagnostics(this);
		this.foldings = new XMLFoldings(this);
		this.documentLink = new XMLDocumentLink(this);
		this.documentColor = new XMLDocumentColor(this);
		this.definition = new XMLDefinition(this);
		this.typeDefinition = new XMLTypeDefinition(this);
		this.reference = new XMLReference(this);
		this.codelens = new XMLCodeLens(this);
		this.codeActions = new XMLCodeActions(this);
		this.prepareRename = new XMLPrepareRename(this);
		this.rename = new XMLRename(this);
		this.selectionRanges = new XMLSelectionRanges();
		this.linkedEditing = new XMLLinkedEditing(this);
		setRetriggerValidationWhenDownloadError(true);
	}

	@Override
	public String formatFull(String text, String uri, SharedSettings sharedSettings, CancelChecker cancelChecker) {
		DOMDocument xmlDocument = DOMParser.getInstance().parse(new TextDocument(text, uri), null);
		List<? extends TextEdit> edits = this.format(xmlDocument, null, sharedSettings);
		try {
			return TextEditUtils.applyEdits(xmlDocument.getTextDocument(), edits);
		} catch (Exception e) {
			if (edits.size() == 1) {
				return edits.get(0).getNewText();
			}
			return text;
		}
	}

	public List<? extends TextEdit> format(DOMDocument xmlDocument, Range range, SharedSettings sharedSettings) {
		return formatter.format(xmlDocument, range, sharedSettings);
	}

	public List<DocumentHighlight> findDocumentHighlights(DOMDocument xmlDocument, Position position) {
		return findDocumentHighlights(xmlDocument, position, NULL_CHECKER);
	}

	public List<DocumentHighlight> findDocumentHighlights(DOMDocument xmlDocument, Position position,
			CancelChecker cancelChecker) {
		return highlighting.findDocumentHighlights(xmlDocument, position, cancelChecker);
	}

	public List<SymbolInformation> findSymbolInformations(DOMDocument xmlDocument, XMLSymbolSettings symbolSettings) {
		return findSymbolInformations(xmlDocument, symbolSettings, NULL_CHECKER);
	}

	public SymbolInformationResult findSymbolInformations(DOMDocument xmlDocument, XMLSymbolSettings symbolSettings,
			CancelChecker cancelChecker) {
		return symbolsProvider.findSymbolInformations(xmlDocument, symbolSettings, cancelChecker);
	}

	public List<DocumentSymbol> findDocumentSymbols(DOMDocument xmlDocument, XMLSymbolSettings symbolSettings) {
		return findDocumentSymbols(xmlDocument, symbolSettings, NULL_CHECKER);
	}

	public DocumentSymbolsResult findDocumentSymbols(DOMDocument xmlDocument, XMLSymbolSettings symbolSettings,
			CancelChecker cancelChecker) {
		return symbolsProvider.findDocumentSymbols(xmlDocument, symbolSettings, cancelChecker);
	}

	public CompletionList doComplete(DOMDocument xmlDocument, Position position, SharedSettings settings) {
		return doComplete(xmlDocument, position, settings, NULL_CHECKER);
	}

	public CompletionList doComplete(DOMDocument xmlDocument, Position position, SharedSettings settings,
			CancelChecker cancelChecker) {
		return completions.doComplete(xmlDocument, position, settings, cancelChecker);
	}

	public CompletionItem resolveCompletionItem(CompletionItem unresolved, DOMDocument xmlDocument,
			SharedSettings sharedSettings, CancelChecker cancelChecker) {
		return completions.resolveCompletionItem(unresolved, xmlDocument, sharedSettings, cancelChecker);
	}

	public Hover doHover(DOMDocument xmlDocument, Position position, SharedSettings sharedSettings) {
		return doHover(xmlDocument, position, sharedSettings, NULL_CHECKER);
	}

	public Hover doHover(DOMDocument xmlDocument, Position position, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		return hover.doHover(xmlDocument, position, sharedSettings, cancelChecker);
	}

	public List<Diagnostic> doDiagnostics(DOMDocument xmlDocument, XMLValidationSettings validationSettings,
			Map<String, Object> validationArgs, CancelChecker cancelChecker) {
		return diagnostics.doDiagnostics(xmlDocument, validationSettings, validationArgs, cancelChecker);
	}

	public CompletableFuture<Path> publishDiagnostics(DOMDocument xmlDocument,
			Consumer<PublishDiagnosticsParams> publishDiagnostics, Consumer<TextDocument> triggerValidation,
			XMLValidationRootSettings validationSettings, Map<String, Object> validationArgs,
			CancelChecker cancelChecker) {
		String uri = xmlDocument.getDocumentURI();
		TextDocument document = xmlDocument.getTextDocument();
		XMLValidationSettings validationSettingsForUri = validationSettings != null
				? validationSettings.getValidationSettings(xmlDocument.getDocumentURI())
				: null;

		// Process validation
		DiagnosticsResult diagnostics = (DiagnosticsResult) this.doDiagnostics(xmlDocument, validationSettingsForUri,
				validationArgs, cancelChecker);
		cancelChecker.checkCanceled();
		publishDiagnostics.accept(new PublishDiagnosticsParams(uri, diagnostics));

		// If there are some XSD, DTD which are downloading, wait for all download and
		// re-trigger the validation.
		if (retriggerValidationWhenDownloadError) {
		List<CompletableFuture<?>> futures = diagnostics.getFutures();
		if (!futures.isEmpty()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			CompletableFuture<Void> allFutures = CompletableFuture
					.allOf(futures.toArray(new CompletableFuture[futures.size()]));
			allFutures.thenAccept(Void -> {
				triggerValidation.accept(document);
			}).exceptionally(downloadException -> {
				triggerValidation.accept(document);
				return null;
			});
		}
		}
		return null;
	}

	public List<FoldingRange> getFoldingRanges(DOMDocument xmlDocument, XMLFoldingSettings context) {
		return getFoldingRanges(xmlDocument, context, NULL_CHECKER);
	}

	public List<FoldingRange> getFoldingRanges(DOMDocument xmlDocument, XMLFoldingSettings context,
			CancelChecker cancelChecker) {
		return foldings.getFoldingRanges(xmlDocument.getTextDocument(), context, cancelChecker);
	}

	public List<SelectionRange> getSelectionRanges(DOMDocument xmlDocument, List<Position> positions,
			CancelChecker cancelChecker) {
		return selectionRanges.getSelectionRanges(xmlDocument, positions, cancelChecker);
	}

	public Either<Range, PrepareRenameResult> prepareRename(DOMDocument xmlDocument, Position position,
			CancelChecker cancelChecker) {
		return prepareRename.prepareRename(xmlDocument, position, cancelChecker);
	}

	public WorkspaceEdit doRename(DOMDocument xmlDocument, Position position, String newText,
			CancelChecker cancelChecker) {
		return rename.doRename(xmlDocument, position, newText, cancelChecker);
	}

	public List<DocumentLink> findDocumentLinks(DOMDocument document) {
		return documentLink.findDocumentLinks(document);
	}

	public List<ColorInformation> findDocumentColors(DOMDocument xmlDocument, CancelChecker cancelChecker) {
		return documentColor.findDocumentColors(xmlDocument, cancelChecker);
	}

	public List<ColorPresentation> getColorPresentations(DOMDocument xmlDocument, ColorPresentationParams params,
			CancelChecker cancelChecker) {
		return documentColor.getColorPresentations(xmlDocument, params, cancelChecker);
	}

	public List<? extends LocationLink> findDefinition(DOMDocument xmlDocument, Position position,
			CancelChecker cancelChecker) {
		return definition.findDefinition(xmlDocument, position, cancelChecker);
	}

	public List<? extends LocationLink> findTypeDefinition(DOMDocument xmlDocument, Position position,
			CancelChecker cancelChecker) {
		return typeDefinition.findTypeDefinition(xmlDocument, position, cancelChecker);
	}

	public List<? extends Location> findReferences(DOMDocument xmlDocument, Position position, ReferenceContext context,
			CancelChecker cancelChecker) {
		return reference.findReferences(xmlDocument, position, context, cancelChecker);
	}

	public List<? extends CodeLens> getCodeLens(DOMDocument xmlDocument, XMLCodeLensSettings settings,
			CancelChecker cancelChecker) {
		return codelens.getCodelens(xmlDocument, settings, cancelChecker);
	}

	public List<CodeAction> doCodeActions(CodeActionContext context, Range range, DOMDocument document,
			SharedSettings sharedSettings, CancelChecker cancelChecker) {
		return codeActions.doCodeActions(context, range, document, sharedSettings, cancelChecker);
	}

	public CodeAction resolveCodeAction(CodeAction unresolved, DOMDocument document, SharedSettings sharedSettings,
			CancelChecker cancelChecker) {
		return codeActions.resolveCodeAction(unresolved, document, sharedSettings, cancelChecker);
	}

	public AutoCloseTagResponse doTagComplete(DOMDocument xmlDocument, XMLCompletionSettings completionSettings,
			Position position) {
		return doTagComplete(xmlDocument, position, completionSettings, NULL_CHECKER);
	}

	public AutoCloseTagResponse doTagComplete(DOMDocument xmlDocument, Position position,
			XMLCompletionSettings completionSettings, CancelChecker cancelChecker) {
		return completions.doTagComplete(xmlDocument, position, completionSettings, cancelChecker);
	}

	public AutoCloseTagResponse doAutoClose(DOMDocument xmlDocument, Position position,
			XMLCompletionSettings completionSettings, CancelChecker cancelChecker) {
		try {
			int offset = xmlDocument.offsetAt(position);
			String text = xmlDocument.getText();
			if (offset > 0) {
				char c = text.charAt(offset - 1);
				if (c == '>' || c == '/') {
					return doTagComplete(xmlDocument, position, completionSettings, cancelChecker);
				}
			}
			return null;
		} catch (BadLocationException e) {
			return null;
		}
	}

	public Position getMatchingTagPosition(DOMDocument xmlDocument, Position position, CancelChecker cancelChecker) {
		return XMLPositionUtility.getMatchingTagPosition(xmlDocument, position);
	}

	/**
	 * Returns the linked editing ranges for the given <code>xmlDocument</code> at
	 * the given <code>position</code> and null otherwise.
	 *
	 * @param xmlDocument   the DOM document.
	 * @param position      the position.
	 * @param cancelChecker the cancel checker.
	 * @return the linked editing ranges for the given <code>xmlDocument</code> at
	 *         the given <code>position</code> and null otherwise.
	 */
	public LinkedEditingRanges findLinkedEditingRanges(DOMDocument xmlDocument, Position position,
			CancelChecker cancelChecker) {
		return linkedEditing.findLinkedEditingRanges(xmlDocument, position, cancelChecker);
	}
	
	public void setRetriggerValidationWhenDownloadError(boolean retriggerValidationWhenDownloadError) {
		this.retriggerValidationWhenDownloadError = retriggerValidationWhenDownloadError;
	}

}
