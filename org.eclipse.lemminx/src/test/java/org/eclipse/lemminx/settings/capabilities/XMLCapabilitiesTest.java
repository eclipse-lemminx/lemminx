/**
 *  Copyright (c) 2018, 2023 Red Hat, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.settings.capabilities;

import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.COMPLETION_ID;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_CODEACTION_OPTIONS;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_COMPLETION_OPTIONS;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_LINK_OPTIONS;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.DEFAULT_RENAME_OPTIONS;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.DOCUMENT_SYMBOL_ID;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.FORMATTING_ID;
import static org.eclipse.lemminx.settings.capabilities.ServerCapabilitiesConstants.FORMATTING_RANGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.eclipse.lemminx.AbstractCacheBasedTest;
import org.eclipse.lemminx.MockXMLLanguageClient;
import org.eclipse.lemminx.XMLTextDocumentService;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CodeActionCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities;
import org.eclipse.lsp4j.DocumentHighlightCapabilities;
import org.eclipse.lsp4j.DocumentLinkCapabilities;
import org.eclipse.lsp4j.DocumentSymbolCapabilities;
import org.eclipse.lsp4j.FoldingRangeCapabilities;
import org.eclipse.lsp4j.FormattingCapabilities;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.RangeFormattingCapabilities;
import org.eclipse.lsp4j.RenameCapabilities;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * XMLCapabilityManagerTest
 */
public class XMLCapabilitiesTest extends AbstractCacheBasedTest {

	private static final Either<Boolean, ?> TRUE = Either.forLeft(true);

	private static final Either<Boolean, ?> FALSE = Either.forLeft(false);

	private LanguageClient languageClient = new MockXMLLanguageClient();
	private XMLCapabilityManager manager;
	private ClientCapabilities clientCapabilities;
	private TextDocumentClientCapabilities textDocument;
	private WorkspaceClientCapabilities workspace;
	private XMLTextDocumentService textDocumentService;
	private Set<String> capabilityIDs;

	@BeforeEach
	public void startup() {

		textDocumentService = new XMLTextDocumentService(null, null);
		textDocumentService.getSharedSettings().getFormattingSettings().setEnabled(true);

		textDocument = new TextDocumentClientCapabilities();
		workspace = new WorkspaceClientCapabilities();
		manager = new XMLCapabilityManager(languageClient, textDocumentService);
		clientCapabilities = new ClientCapabilities();
		capabilityIDs = null;

	}

	@Test
	public void testAllDynamicCapabilities() {
		setAllCapabilities(true);
		setAndInitializeCapabilities();

		assertEquals(11, capabilityIDs.size());

		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(manager.getClientCapabilities(), false);
		assertEquals(FALSE, serverCapabilities.getDocumentRangeFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentSymbolProvider());
		assertEquals(FALSE, serverCapabilities.getHoverProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentHighlightProvider());
		assertEquals(FALSE, serverCapabilities.getFoldingRangeProvider());
		assertEquals(null, serverCapabilities.getCodeActionProvider());
		assertEquals(null, serverCapabilities.getCompletionProvider());
		assertEquals(null, serverCapabilities.getDocumentLinkProvider());
		assertEquals(null, serverCapabilities.getRenameProvider());
	}

	@Test
	public void testNoDynamicCapabilities() {
		setAllCapabilities(false);
		setAndInitializeCapabilities();

		assertEquals(0, capabilityIDs.size());

		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(manager.getClientCapabilities(), false);
		assertEquals(TRUE, serverCapabilities.getDocumentRangeFormattingProvider());
		assertEquals(TRUE, serverCapabilities.getDocumentFormattingProvider());
		assertEquals(TRUE, serverCapabilities.getDocumentSymbolProvider());
		assertEquals(TRUE, serverCapabilities.getHoverProvider());
		assertEquals(TRUE, serverCapabilities.getDocumentHighlightProvider());
		assertEquals(TRUE, serverCapabilities.getFoldingRangeProvider());
		assertEquals(Either.forRight(DEFAULT_CODEACTION_OPTIONS), serverCapabilities.getCodeActionProvider());
		assertEquals(DEFAULT_COMPLETION_OPTIONS, serverCapabilities.getCompletionProvider());
		assertEquals(DEFAULT_LINK_OPTIONS, serverCapabilities.getDocumentLinkProvider());
		assertEquals(Either.forRight(DEFAULT_RENAME_OPTIONS), serverCapabilities.getRenameProvider());
	}

	@Test
	public void testBothCapabilityTypes() {
		// Dynamic capabilities
		textDocument.setRangeFormatting(new RangeFormattingCapabilities(true));
		textDocument.setFormatting(new FormattingCapabilities(true));
		CompletionCapabilities completion = new CompletionCapabilities();
		completion.setDynamicRegistration(true);
		textDocument.setCompletion(completion);
		textDocument.setDocumentSymbol(new DocumentSymbolCapabilities(true));
		workspace.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities(true));

		// Non dynamic capabilities
		textDocument.setHover(new HoverCapabilities(false));
		textDocument.setDocumentHighlight(new DocumentHighlightCapabilities(false));
		textDocument.setRename(new RenameCapabilities(false));
		FoldingRangeCapabilities folding = new FoldingRangeCapabilities();
		folding.setDynamicRegistration(false);
		textDocument.setFoldingRange(folding);
		textDocument.setDocumentLink(new DocumentLinkCapabilities(false));
		textDocument.setCodeAction(new CodeActionCapabilities(false));

		setAndInitializeCapabilities();

		assertEquals(5, capabilityIDs.size());
		assertEquals(true, capabilityIDs.contains(FORMATTING_ID));
		assertEquals(true, capabilityIDs.contains(FORMATTING_RANGE_ID));
		assertEquals(true, capabilityIDs.contains(COMPLETION_ID));
		assertEquals(true, capabilityIDs.contains(DOCUMENT_SYMBOL_ID));

		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(manager.getClientCapabilities(), false);
		assertEquals(FALSE, serverCapabilities.getDocumentRangeFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentSymbolProvider());
		assertEquals(TRUE, serverCapabilities.getHoverProvider());
		assertEquals(TRUE, serverCapabilities.getDocumentHighlightProvider());
		assertEquals(TRUE, serverCapabilities.getFoldingRangeProvider());
		assertEquals(Either.forRight(DEFAULT_CODEACTION_OPTIONS), serverCapabilities.getCodeActionProvider());
		assertEquals(null, serverCapabilities.getCompletionProvider());
		assertEquals(DEFAULT_LINK_OPTIONS, serverCapabilities.getDocumentLinkProvider());
		assertEquals(Either.forRight(DEFAULT_RENAME_OPTIONS), serverCapabilities.getRenameProvider());
	}

	@Test
	public void testDynamicFormattingWithPreferenceFalse() {
		textDocumentService.getSharedFormattingSettings().setEnabled(false);
		// Non Dynamic capabilities
		textDocument.setRangeFormatting(new RangeFormattingCapabilities(true));
		textDocument.setFormatting(new FormattingCapabilities(true));

		setAndInitializeCapabilities();

		Set<String> capabilityIDs = manager.getRegisteredCapabilities();
		assertEquals(0, capabilityIDs.size());

		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(manager.getClientCapabilities(), false);
		assertEquals(FALSE, serverCapabilities.getDocumentRangeFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentFormattingProvider());
	}

	@Test
	public void testDynamicFormattingWithPreferenceTrue() {
		textDocumentService.getSharedFormattingSettings().setEnabled(true);
		// Dynamic capabilities
		textDocument.setRangeFormatting(new RangeFormattingCapabilities(true));
		textDocument.setFormatting(new FormattingCapabilities(true));

		setAndInitializeCapabilities();

		Set<String> capabilityIDs = manager.getRegisteredCapabilities();
		assertEquals(2, capabilityIDs.size());
		assertEquals(true, capabilityIDs.contains(FORMATTING_ID));
		assertEquals(true, capabilityIDs.contains(FORMATTING_RANGE_ID));

		ServerCapabilities serverCapabilities = ServerCapabilitiesInitializer
				.getNonDynamicServerCapabilities(manager.getClientCapabilities(), false);
		assertEquals(FALSE, serverCapabilities.getDocumentRangeFormattingProvider());
		assertEquals(FALSE, serverCapabilities.getDocumentFormattingProvider());
	}

	private void setAllCapabilities(boolean areAllDynamic) {
		textDocument.setRangeFormatting(new RangeFormattingCapabilities(areAllDynamic));
		textDocument.setFormatting(new FormattingCapabilities(areAllDynamic));
		CompletionCapabilities completion = new CompletionCapabilities();
		completion.setDynamicRegistration(areAllDynamic);
		textDocument.setCompletion(completion);
		textDocument.setDocumentSymbol(new DocumentSymbolCapabilities(areAllDynamic));
		textDocument.setHover(new HoverCapabilities(areAllDynamic));
		textDocument.setDocumentHighlight(new DocumentHighlightCapabilities(areAllDynamic));
		textDocument.setRename(new RenameCapabilities(areAllDynamic));
		FoldingRangeCapabilities folding = new FoldingRangeCapabilities();
		folding.setDynamicRegistration(areAllDynamic);
		textDocument.setFoldingRange(folding);
		textDocument.setDocumentLink(new DocumentLinkCapabilities(areAllDynamic));
		textDocument.setCodeAction(new CodeActionCapabilities(areAllDynamic));
		workspace.setDidChangeWatchedFiles(new DidChangeWatchedFilesCapabilities(areAllDynamic));
	}

	private void setAndInitializeCapabilities() {
		clientCapabilities.setTextDocument(textDocument);
		clientCapabilities.setWorkspace(workspace);
		manager.setClientCapabilities(clientCapabilities, null);
		manager.initializeCapabilities();
		capabilityIDs = manager.getRegisteredCapabilities();
	}

}