/**
 * Copyright (c) 2026 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.lsp4j.launcher.multiclient;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Extension of LSP4J Launcher.Builder that supports multiple clients connecting to a shared server.
 *
 * <p>Usage example:
 * <pre>
 * new MultiClientLauncher.Builder&lt;LanguageClient&gt;()
 *     .setLocalService(server)
 *     .setRemoteInterface(LanguageClient.class)
 *     .setInput(in)
 *     .setOutput(out)
 *     .setExecutorService(executor)
 *     .wrapMessages(wrapper)
 *     .enableSocket(port, workspacePath, "lemminx")
 *     .create();
 * </pre>
 */
public class MultiClientLauncher {

	private static final Logger LOGGER = Logger.getLogger(MultiClientLauncher.class.getName());

	/**
	 * Wrapper that returns the multi-client proxy instead of the primary client.
	 */
	private static class LauncherWrapper<C> implements Launcher<C> {
		private final Launcher<C> delegate;
		private final C multiClientProxy;

		LauncherWrapper(Launcher<C> delegate, C multiClientProxy) {
			this.delegate = delegate;
			this.multiClientProxy = multiClientProxy;
		}

		@Override
		public java.util.concurrent.Future<Void> startListening() {
			return delegate.startListening();
		}

		@Override
		public C getRemoteProxy() {
			// Return multi-client proxy so server.setClient() sets the broadcast proxy
			return multiClientProxy;
		}

		@Override
		public org.eclipse.lsp4j.jsonrpc.RemoteEndpoint getRemoteEndpoint() {
			return delegate.getRemoteEndpoint();
		}
	}

	/**
	 * Builder that extends LSP4J's Launcher.Builder with multi-client socket support.
	 */
	public static class Builder<C> extends Launcher.Builder<C> {

		// Captured from parent builder calls
		private Object localService;
		private Class<C> remoteInterface;

		// Socket mode fields
		private int socketPort = -1;
		private String workspacePath;
		private String serverType;
		private String clientName;
		private String clientVersion;
		private SocketListenerManager socketManager;

		// Override parent methods to capture values and return our Builder type
		@Override
		@SuppressWarnings("unchecked")
		public Builder<C> setLocalService(Object localService) {
			this.localService = localService;
			super.setLocalService(localService);
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Builder<C> setRemoteInterface(Class<? extends C> remoteInterface) {
			this.remoteInterface = (Class<C>) remoteInterface;
			super.setRemoteInterface(remoteInterface);
			return this;
		}

		@Override
		public Builder<C> setInput(java.io.InputStream input) {
			super.setInput(input);
			return this;
		}

		@Override
		public Builder<C> setOutput(java.io.OutputStream output) {
			super.setOutput(output);
			return this;
		}

		@Override
		public Builder<C> setExecutorService(java.util.concurrent.ExecutorService executorService) {
			super.setExecutorService(executorService);
			return this;
		}

		@Override
		public Builder<C> wrapMessages(java.util.function.Function<org.eclipse.lsp4j.jsonrpc.MessageConsumer, org.eclipse.lsp4j.jsonrpc.MessageConsumer> wrapper) {
			super.wrapMessages(wrapper);
			return this;
		}

		/**
		 * Enable socket mode for secondary clients.
		 *
		 * @param port the port to listen on (0 for automatic free port)
		 * @param workspacePath the workspace path (for instance file)
		 * @param serverType the server type identifier (e.g., "lemminx", "jdtls")
		 * @return this builder
		 */
		public Builder<C> enableSocket(int port, String workspacePath, String serverType) {
			this.socketPort = port;
			this.workspacePath = workspacePath;
			this.serverType = serverType;
			return this;
		}

		/**
		 * Set client information to be written to the instance file.
		 * Optional - if not called, clientName and clientVersion will be null.
		 *
		 * @param clientName the name of the IDE/client (e.g., "VS Code", "IntelliJ IDEA")
		 * @param clientVersion the version of the IDE/client (e.g., "1.95.0")
		 * @return this builder
		 */
		public Builder<C> setClientInfo(String clientName, String clientVersion) {
			this.clientName = clientName;
			this.clientVersion = clientVersion;
			return this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Launcher<C> create() {
			if (socketPort < 0 || workspacePath == null || serverType == null) {
				// Normal single-client mode
				return super.create();
			}

			// Multi-client mode - wrap the server and setup socket listener
			LanguageServer originalServer = (LanguageServer) localService;

			// Create multi-client proxy for broadcasting
			C multiClientProxy = MultiClientProxy.create(remoteInterface);
			MultiClientProxy<C> proxyHandler = (MultiClientProxy<C>) Proxy.getInvocationHandler(multiClientProxy);

			// Create socket manager to handle the InitializeResult capture and socket listener
			socketManager = new SocketListenerManager(originalServer, proxyHandler, remoteInterface,
				socketPort, workspacePath, serverType, clientName, clientVersion);

			// Wrap server to intercept initialize()
			LanguageServer wrappedServer = socketManager.wrapServer();

			// Create primary launcher using parent builder with wrapped server
			super.setLocalService(wrappedServer);
			Launcher<C> primaryLauncher = super.create();

			// Add primary client to proxy
			proxyHandler.addClient(primaryLauncher.getRemoteProxy());

			// Start socket listener in background
			socketManager.startSocketListener();

			// Return a launcher wrapper that returns multiClientProxy instead of the primary client
			return new LauncherWrapper<>(primaryLauncher, multiClientProxy);
		}
	}

	/**
	 * Manages socket listener and InitializeResult capture for secondary clients.
	 */
	private static class SocketListenerManager {
		private final LanguageServer originalServer;
		private final MultiClientProxy<?> clientProxy;
		private final Class<?> clientInterface;
		private final int port;
		private final String workspacePath;
		private final String serverType;
		private final String clientName;
		private final String clientVersion;
		private volatile InitializeResult cachedResult;

		SocketListenerManager(LanguageServer server, MultiClientProxy<?> proxy, Class<?> clientInterface,
				int port, String workspacePath, String serverType, String clientName, String clientVersion) {
			this.originalServer = server;
			this.clientProxy = proxy;
			this.clientInterface = clientInterface;
			this.port = port;
			this.workspacePath = workspacePath;
			this.serverType = serverType;
			this.clientName = clientName;
			this.clientVersion = clientVersion;
		}

		LanguageServer wrapServer() {
			return (LanguageServer) Proxy.newProxyInstance(
				originalServer.getClass().getClassLoader(),
				new Class<?>[] { LanguageServer.class },
				new InitializeInterceptor(originalServer, this)
			);
		}

		void setInitializeResult(InitializeResult result) {
			this.cachedResult = result;
			System.err.println("[MultiClientLauncher] Cached InitializeResult for secondary clients");
		}

		InitializeResult getInitializeResult() {
			return cachedResult;
		}

		void waitForInitialize() throws InterruptedException {
			while (cachedResult == null) {
				Thread.sleep(100);
			}
		}

		void startSocketListener() {
			InstancePaths paths = InstancePaths.builder()
				.serverType(serverType)
				.workspacePath(workspacePath)
				.build();

			Thread thread = new Thread(() -> {
				try {
					runSocketListener(paths);
				} catch (Exception e) {
					System.err.println("[MultiClientLauncher] Socket listener failed: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			}, "Socket-Listener-" + serverType);

			thread.setDaemon(true);
			thread.start();
		}

		@SuppressWarnings("unchecked")
		private void runSocketListener(InstancePaths paths) throws Exception {
			LspSocketLauncher<LanguageServer, Object> socketLauncher =
				new LspSocketLauncher<>(serverType);

			// Bind socket and write instance file
			socketLauncher.port = port;
			socketLauncher.instancePaths = paths;
			socketLauncher.writeInstanceFiles = paths.hasWorkspace();

			AsynchronousServerSocketChannel serverSocket = socketLauncher.bindSocket();

			if (socketLauncher.writeInstanceFiles) {
				try {
					LspInstanceRegistry.registerInstance(paths, socketLauncher.port, clientName, clientVersion);
					System.err.println("[MultiClientLauncher] Registered " + serverType +
						" instance in " + paths.getInstanceFilePath());
				} catch (IOException e) {
					System.err.println("[MultiClientLauncher] Failed to write instance file: " + e.getMessage());
				}
			}

			// Accept secondary clients
			System.err.println("[MultiClientLauncher] " + serverType +
				" socket listener ready on port " + socketLauncher.port);

			while (true) {
				AsynchronousSocketChannel socketChannel = serverSocket.accept().get();
				acceptSecondaryClient(socketChannel);
			}
		}

		@SuppressWarnings("unchecked")
		private void acceptSecondaryClient(AsynchronousSocketChannel socketChannel) throws Exception {
			// Wait for primary client to initialize
			waitForInitialize();

			var in = java.nio.channels.Channels.newInputStream(socketChannel);
			var out = java.nio.channels.Channels.newOutputStream(socketChannel);
			var executor = java.util.concurrent.Executors.newCachedThreadPool();

			// Wrap server for secondary client
			LanguageServer wrappedForSecondary = new SecondaryClientServerWrapper(originalServer, cachedResult);

			// Create launcher for secondary client
			Launcher<?> secondaryLauncher = new Launcher.Builder<>()
				.setLocalService(wrappedForSecondary)
				.setRemoteInterface(clientInterface)
				.setInput(in)
				.setOutput(out)
				.setExecutorService(executor)
				.create();

			// Add to multi-client proxy
			Object remoteProxy = secondaryLauncher.getRemoteProxy();
			((MultiClientProxy<Object>) clientProxy).addClient(remoteProxy);

			System.err.println("[MultiClientLauncher] Secondary client connected. Total clients: " +
				clientProxy.getClientCount());

			// Start listening - returns a Future that completes when connection closes
			java.util.concurrent.Future<Void> listening = secondaryLauncher.startListening();

			// Monitor connection in background thread - when it closes, remove client
			Thread cleanupThread = new Thread(() -> {
				try {
					listening.get(); // Blocks until connection closes
				} catch (Exception e) {
					// Connection closed (normal or error)
				} finally {
					System.err.println("[MultiClientLauncher] Secondary client disconnected, removing from proxy");
					((MultiClientProxy<Object>) clientProxy).removeClient(remoteProxy);
					System.err.println("[MultiClientLauncher] Remaining clients: " + clientProxy.getClientCount());
				}
			}, "Cleanup-" + System.identityHashCode(remoteProxy));
			cleanupThread.setDaemon(true);
			cleanupThread.start();
		}
	}

	/**
	 * InvocationHandler that intercepts initialize() to cache the result.
	 */
	private static class InitializeInterceptor implements InvocationHandler {
		private final LanguageServer delegate;
		private final SocketListenerManager manager;

		InitializeInterceptor(LanguageServer delegate, SocketListenerManager manager) {
			this.delegate = delegate;
			this.manager = manager;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Object result = method.invoke(delegate, args);

			if ("initialize".equals(method.getName()) && result instanceof CompletableFuture) {
				return ((CompletableFuture<InitializeResult>) result).thenApply(initResult -> {
					manager.setInitializeResult(initResult);
					return initResult;
				});
			}

			return result;
		}
	}
}
