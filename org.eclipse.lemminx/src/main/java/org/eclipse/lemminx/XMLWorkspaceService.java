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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.services.extensions.IWatchedFilesContext;
import org.eclipse.lemminx.services.extensions.IWorkspaceServiceParticipant;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * XML workspace service.
 *
 */
public class XMLWorkspaceService implements WorkspaceService, IXMLCommandService {

	private static final Logger LOGGER = Logger.getLogger(XMLWorkspaceService.class.getName());

	private final XMLLanguageServer xmlLanguageServer;

	private final Map<String, IDelegateCommandHandler> commands;

	public XMLWorkspaceService(XMLLanguageServer xmlLanguageServer) {
		this.xmlLanguageServer = xmlLanguageServer;
		this.commands = new HashMap<>();
	}

	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		xmlLanguageServer.getXMLLanguageService().initializeIfNeeded();
		synchronized (commands) {
			IDelegateCommandHandler handler = commands.get(params.getCommand());
			if (handler == null) {
				throw new ResponseErrorException(new ResponseError(ResponseErrorCode.InternalError,
						"No command handler for the command: " + params.getCommand(), null));
			}
			return CompletableFutures.computeAsync(cancelChecker -> {
				try {
					return handler.executeCommand(params, xmlLanguageServer.getSharedSettings(), cancelChecker);
				} catch (Exception e) {
					if (e instanceof ResponseErrorException) {
						throw (ResponseErrorException) e;
					} else if (e instanceof CancellationException) {
						throw (CancellationException) e;
					}
					throw new ResponseErrorException(
							new ResponseError(ResponseErrorCode.UnknownErrorCode, e.getMessage(), e));
				}
			}).thenCompose(result -> {
				if (result instanceof CompletableFuture<?>) {
					@SuppressWarnings("unchecked")
					CompletableFuture<Object> future = (CompletableFuture<Object>) result;
					return future;
				}
				return CompletableFuture.completedFuture(result);
			});
		}
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		xmlLanguageServer.updateSettings(params.getSettings());
		xmlLanguageServer.getCapabilityManager().syncDynamicCapabilitiesWithPreferences();
		((CompositeTextDocumentService) xmlLanguageServer.getTextDocumentService()).clearAllScopedSettings();
	}

	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		xmlLanguageServer.getXMLLanguageService().getWorkspaceServiceParticipants()
				.forEach(participant -> participant.didChangeWorkspaceFolders(params));
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		XMLTextDocumentService xmlTextDocumentService = ((CompositeTextDocumentService) xmlLanguageServer
				.getTextDocumentService()).getXMLTextDocumentService();
		List<FileEvent> changes = params.getChanges();
		for (FileEvent change : changes) {
			if (!xmlTextDocumentService.documentIsOpen(change.getUri())) {
				xmlTextDocumentService.doSave(change.getUri());
			}
		}
		// Create a shared context so multiple extensions can request deferred notifications;
		// only one refresh per type is sent after all participants finish (N-to-1 pattern).
		IWatchedFilesContext context = new IWatchedFilesContext() {
			private boolean codeLensRefreshRequested;
			private boolean diagnosticsRefreshRequested;
			@Override public void requestCodeLensRefresh() { codeLensRefreshRequested = true; }
			@Override public boolean isCodeLensRefreshRequested() { return codeLensRefreshRequested; }
			@Override public void requestDiagnosticsRefresh() { diagnosticsRefreshRequested = true; }
			@Override public boolean isDiagnosticsRefreshRequested() { return diagnosticsRefreshRequested; }
		};
		xmlLanguageServer.getXMLLanguageService().getWorkspaceServiceParticipants()
				.forEach(participant -> participant.didChangeWatchedFiles(changes, context));
		if (context.isCodeLensRefreshRequested()) {
			xmlLanguageServer.getXMLLanguageService().refreshCodeLenses();
		}
		if (context.isDiagnosticsRefreshRequested()) {
			// Re-publish diagnostics for open catalog documents: a file change may have
			// added or removed a reference to a catalog entry, making "unused" warnings stale.
			xmlLanguageServer.getXMLLanguageService().refreshDiagnostics(DOMUtils::isCatalog);
		}
	}

	@Override
	public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
		return CompletableFutures.computeAsync(cancelChecker -> {
			Map<String, List<TextEdit>> allChanges = new HashMap<>();
			for (IWorkspaceServiceParticipant participant : xmlLanguageServer.getXMLLanguageService()
					.getWorkspaceServiceParticipants()) {
				cancelChecker.checkCanceled();
				WorkspaceEdit edit = participant.willRenameFiles(params, cancelChecker);
				if (edit != null && edit.getChanges() != null) {
					edit.getChanges().forEach((uri, edits) ->
						allChanges.computeIfAbsent(uri, k -> new ArrayList<>()).addAll(edits)
					);
				}
			}
			if (allChanges.isEmpty()) {
				return null;
			}
			WorkspaceEdit result = new WorkspaceEdit();
			result.setChanges(allChanges);
			return result;
		});
	}

	@Override
	public void registerCommand(String commandId, IDelegateCommandHandler handler) {
		synchronized (commands) {
			if (commands.containsKey(commandId)) {
				throw new IllegalArgumentException("Command with id '" + commandId + "' is already registered");
			}
			commands.put(commandId, handler);
		}
	}

	@Override
	public void unregisterCommand(String commandId) {
		synchronized (commands) {
			commands.remove(commandId);
		}
	}

	@Override
	public CompletableFuture<Object> executeClientCommand(ExecuteCommandParams command) {
		return xmlLanguageServer.getLanguageClient().executeClientCommand(command);
	}

	@Override
	public void endCommandsRegistration() {
		if (!commands.isEmpty() && xmlLanguageServer.getCapabilityManager() != null) {
			xmlLanguageServer.getCapabilityManager().registerExecuteCommand(new ArrayList<>(commands.keySet()));
		}
	}
}
