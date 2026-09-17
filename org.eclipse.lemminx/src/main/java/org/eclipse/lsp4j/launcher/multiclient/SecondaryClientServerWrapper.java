/**
 * Copyright (c) 2024 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.lsp4j.launcher.multiclient;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Generic wrapper for secondary clients connecting to an already-initialized server.
 * Intercepts initialize/initialized to return cached capabilities without
 * re-initializing the underlying server.
 *
 * This class is generic and can be used with any LSP LanguageServer implementation.
 */
public class SecondaryClientServerWrapper implements LanguageServer {

	private final LanguageServer delegate;
	private final InitializeResult cachedResult;

	/**
	 * Creates a wrapper for secondary clients.
	 *
	 * @param delegate the underlying language server (already initialized)
	 * @param cachedResult the initialize result from the primary client
	 */
	public SecondaryClientServerWrapper(LanguageServer delegate, InitializeResult cachedResult) {
		this.delegate = delegate;
		this.cachedResult = cachedResult;
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		// Return cached capabilities without touching the underlying server
		return CompletableFuture.completedFuture(cachedResult);
	}

	@Override
	public void initialized(InitializedParams params) {
		// No-op: server is already initialized by the primary client
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		// Don't actually shutdown the shared server when a secondary client disconnects
		// Just acknowledge the shutdown request
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void exit() {
		// No-op: server continues running for other clients
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		// Delegate all actual LSP operations to the shared server
		return delegate.getTextDocumentService();
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		// Delegate all actual LSP operations to the shared server
		return delegate.getWorkspaceService();
	}
}
