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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Generic multi-client socket launcher for LSP servers.
 * Manages a shared language server instance and allows multiple clients to connect.
 *
 * @param <S> the server interface type (extends LanguageServer)
 * @param <C> the client interface type
 */
public class MultiClientSocketLauncher<S extends LanguageServer, C> {

	private static final Logger LOGGER = Logger.getLogger(MultiClientSocketLauncher.class.getName());

	private final S sharedServer;
	private final Class<C> clientInterface;
	private final MultiClientProxy<C> multiClientProxy;
	private InitializeResult cachedInitializeResult;
	private boolean isPrimaryClient = true;
	private final Object lock = new Object();

	/**
	 * Creates a multi-client launcher.
	 *
	 * @param serverFactory factory to create the shared server instance
	 * @param clientInterface the client interface class
	 */
	@SuppressWarnings("unchecked")
	public MultiClientSocketLauncher(Function<C, S> serverFactory, Class<C> clientInterface) {
		this.clientInterface = clientInterface;

		// Create the multi-client proxy
		C proxyClient = MultiClientProxy.create(clientInterface);
		this.multiClientProxy = (MultiClientProxy<C>) Proxy.getInvocationHandler(proxyClient);

		// Create the shared server with the multi-client proxy as its client
		this.sharedServer = serverFactory.apply(proxyClient);
	}

	/**
	 * Accepts a new client connection.
	 *
	 * @param socketChannel the socket channel for the new client
	 * @throws Exception if connection fails
	 */
	public void acceptClient(AsynchronousSocketChannel socketChannel) throws Exception {
		final InputStream in = Channels.newInputStream(socketChannel);
		final OutputStream out = Channels.newOutputStream(socketChannel);
		final ExecutorService executorService = Executors.newCachedThreadPool();

		synchronized (lock) {
			if (isPrimaryClient) {
				acceptPrimaryClient(in, out, executorService);
			} else {
				acceptSecondaryClient(in, out, executorService);
			}
		}
	}

	private void acceptPrimaryClient(InputStream in, OutputStream out, ExecutorService executorService) {
		LOGGER.log(Level.INFO, "Primary client connecting...");

		// Create launcher for primary client
		final Launcher<C> launcher = Launcher.createIoLauncher(
			sharedServer,
			clientInterface,
			in, out, executorService,
			(MessageConsumer it) -> it
		);

		// Add this client to the multi-client proxy
		C client = launcher.getRemoteProxy();
		multiClientProxy.addClient(client);

		// Start listening
		Future<Void> listening = launcher.startListening();

		// Hook into the initialize response to cache it
		// We need to intercept the server's initialize method completion
		// For now, we'll use a simple approach with a callback
		isPrimaryClient = false;

		LOGGER.log(Level.INFO, "Primary client connected successfully");
	}

	private void acceptSecondaryClient(InputStream in, OutputStream out, ExecutorService executorService)
			throws InterruptedException {
		LOGGER.log(Level.INFO, "Secondary client connecting...");

		// Wait for primary client to initialize
		while (cachedInitializeResult == null) {
			Thread.sleep(100);
		}

		// Wrap the server to return cached initialize result
		LanguageServer wrappedServer = new SecondaryClientServerWrapper(sharedServer, cachedInitializeResult);

		// Create launcher for secondary client
		final Launcher<C> launcher = Launcher.createIoLauncher(
			wrappedServer,
			clientInterface,
			in, out, executorService,
			(MessageConsumer it) -> it
		);

		// Add this client to the multi-client proxy
		C client = launcher.getRemoteProxy();
		multiClientProxy.addClient(client);

		// Start listening
		launcher.startListening();

		LOGGER.log(Level.INFO, "Secondary client connected successfully");
	}

	/**
	 * Called when the primary client completes initialization.
	 * This allows us to cache the result for secondary clients.
	 *
	 * @param result the initialize result
	 */
	public void setInitializeResult(InitializeResult result) {
		this.cachedInitializeResult = result;
		LOGGER.log(Level.INFO, "Cached initialize result for secondary clients");
	}

	/**
	 * Returns the number of connected clients.
	 */
	public int getClientCount() {
		return multiClientProxy.getClientCount();
	}

	/**
	 * Returns the shared server instance.
	 */
	public S getSharedServer() {
		return sharedServer;
	}
}
