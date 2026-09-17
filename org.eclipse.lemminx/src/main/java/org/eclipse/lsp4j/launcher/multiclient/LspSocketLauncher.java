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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageServer;

/**
 * Generic LSP server socket launcher with multi-client support.
 * Multiple clients can connect to the same server instance via TCP socket.
 *
 * @param <S> the server interface type (extends LanguageServer)
 * @param <C> the client interface type
 */
public class LspSocketLauncher<S extends LanguageServer, C> {

	private static final Logger LOGGER = Logger.getLogger(LspSocketLauncher.class.getName());
	private static final int DEFAULT_PORT = 5_008;

	protected int port;
	protected InstancePaths instancePaths;
	protected boolean writeInstanceFiles = false;
	protected MultiClientSocketLauncher<S, C> multiClientLauncher;
	protected S sharedServerInstance;
	protected MultiClientProxy<C> clientProxy;
	protected InitializeResult cachedInitializeResult;

	/**
	 * Creates a launcher for the specified server type.
	 *
	 * @param serverType the server type identifier (e.g., "lemminx", "jdtls")
	 */
	public LspSocketLauncher(String serverType) {
		this.instancePaths = InstancePaths.builder().serverType(serverType).build();
	}

	/**
	 * Launch socket listener for MCP clients.
	 * Shares the same server instance with the stdio client.
	 *
	 * @param sharedServer the shared server instance
	 * @param port the port to listen on
	 * @param workspacePath the workspace path (optional, can be null to disable instance files)
	 * @param clientProxy the multi-client proxy to add socket clients to
	 * @param clientInterface the client interface class
	 */
	public void launchForSharedServer(S sharedServer, int port, String workspacePath,
			MultiClientProxy<C> clientProxy, Class<C> clientInterface) throws Exception {
		this.port = port;
		this.instancePaths = InstancePaths.builder()
			.serverType(instancePaths.getServerType())
			.workspacePath(workspacePath)
			.build();
		this.sharedServerInstance = sharedServer;
		this.clientProxy = clientProxy;
		this.writeInstanceFiles = instancePaths.hasWorkspace();

		// Register shutdown hook
		registerShutdownHook();

		// Bind socket
		final AsynchronousServerSocketChannel serverSocket = bindSocket();

		// Write instance file (only if workspace path provided)
		if (writeInstanceFiles) {
			writeInstanceFile();
		}

		LOGGER.log(Level.INFO, "{0} MCP socket listener on port {1} for workspace {2} (shared server mode)",
			new Object[] { instancePaths.getServerType(), this.port, instancePaths.getWorkspacePath() });

		// Accept clients in a loop
		while (true) {
			final AsynchronousSocketChannel socketChannel = serverSocket.accept().get();
			acceptSocketClient(socketChannel, clientInterface);
		}
	}

	/**
	 * Launches the server in standalone socket mode.
	 * Each connecting client gets its own server instance.
	 *
	 * @param args standard launch arguments. may contain <code>--port</code> and <code>--workspace</code>
	 * @param serverFactory factory to create server instances
	 * @param clientInterface the client interface class
	 */
	public void launch(String[] args, Function<C, S> serverFactory, Class<C> clientInterface) throws Exception {
		// Parse arguments
		this.port = getPort(args);
		String workspacePath = getWorkspace(args);
		this.instancePaths = InstancePaths.builder()
			.serverType(instancePaths.getServerType())
			.workspacePath(workspacePath)
			.build();
		this.writeInstanceFiles = instancePaths.hasWorkspace();

		// Register shutdown hook for cleanup
		registerShutdownHook();

		// Bind socket with retry
		final AsynchronousServerSocketChannel serverSocket = bindSocket();

		// Write instance file (after successful bind, so port is confirmed)
		if (writeInstanceFiles) {
			writeInstanceFile();
		}

		LOGGER.log(Level.INFO, "{0} multi-client server listening on port {1} for workspace {2}",
				new Object[] { instancePaths.getServerType(), this.port, instancePaths.getWorkspacePath() });

		// Create the multi-client launcher
		this.multiClientLauncher = new MultiClientSocketLauncher<>(serverFactory, clientInterface);

		// Accept clients in a loop
		while (true) {
			final AsynchronousSocketChannel socketChannel = serverSocket.accept().get();
			multiClientLauncher.acceptClient(socketChannel);
			LOGGER.log(Level.INFO, "Client connected. Total clients: {0}", multiClientLauncher.getClientCount());
		}
	}

	/**
	 * Accept a socket client connection and add it to the multi-client proxy.
	 */
	private void acceptSocketClient(AsynchronousSocketChannel socketChannel, Class<C> clientInterface) throws Exception {
		// Wait for initialize result from stdio client
		while (cachedInitializeResult == null) {
			Thread.sleep(100);
		}

		final InputStream in = Channels.newInputStream(socketChannel);
		final OutputStream out = Channels.newOutputStream(socketChannel);
		final ExecutorService executorService = Executors.newCachedThreadPool();

		// Create launcher for socket client with wrapper that returns cached result
		final Launcher<C> launcher = Launcher.createIoLauncher(
			new SecondaryClientServerWrapper(sharedServerInstance, cachedInitializeResult),
			clientInterface,
			in, out, executorService,
			(MessageConsumer it) -> it
		);

		// Add this socket client to the multi-client proxy so it receives publishDiagnostics etc.
		C socketClient = launcher.getRemoteProxy();
		clientProxy.addClient(socketClient);

		// Start listening
		launcher.startListening();

		LOGGER.log(Level.INFO, "Socket client connected. Total clients: {0}", clientProxy.getClientCount());
	}

	protected int getPort(final String... args) {
		for (int i = 0; (i < (args.length - 1)); i++) {
			String _get = args[i];
			boolean _equals = Objects.equals(_get, "--port");
			if (_equals) {
				return Integer.parseInt(args[(i + 1)]);
			}
		}
		return DEFAULT_PORT;
	}

	protected String getWorkspace(final String... args) {
		for (int i = 0; (i < (args.length - 1)); i++) {
			String _get = args[i];
			boolean _equals = Objects.equals(_get, "--workspace");
			if (_equals) {
				return args[(i + 1)];
			}
		}
		return null;
	}

	private void writeInstanceFile() {
		if (!writeInstanceFiles) {
			return;
		}
		try {
			LspInstanceRegistry.registerInstance(instancePaths, this.port);
			LOGGER.log(Level.INFO, "Registered {0} instance in {1}",
				new Object[] {
					instancePaths.getServerType(),
					instancePaths.getInstanceFilePath()
				});
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Failed to write instance file", e);
		}
	}

	protected AsynchronousServerSocketChannel bindSocket() throws Exception {
		// If port is 0, let OS choose
		if (this.port == 0) {
			LOGGER.log(Level.INFO, "Port is 0, asking OS to choose a free port");
			AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open();
			InetSocketAddress address = new InetSocketAddress("0.0.0.0", 0);
			channel.bind(address);

			InetSocketAddress localAddress = (InetSocketAddress) channel.getLocalAddress();
			if (localAddress == null) {
				throw new Exception("Failed to get local address after binding to port 0");
			}

			int chosenPort = localAddress.getPort();
			LOGGER.log(Level.INFO, "OS chose port {0}, updating this.port from {1} to {2}",
				new Object[] { chosenPort, this.port, chosenPort });

			this.port = chosenPort;

			LOGGER.log(Level.INFO, "After update, this.port is now {0}", this.port);
			return channel;
		}

		// Otherwise try the specified port, then port+1, port+2... (max 10 attempts)
		int maxRetries = 10;
		int currentPort = this.port;

		for (int i = 0; i < maxRetries; i++) {
			try {
				AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open();
				InetSocketAddress address = new InetSocketAddress("0.0.0.0", currentPort);
				channel.bind(address);

				if (currentPort != this.port) {
					LOGGER.log(Level.WARNING, "Port {0} was in use, bound to port {1} instead",
						new Object[] { this.port, currentPort });
					this.port = currentPort;
				} else {
					LOGGER.log(Level.INFO, "Successfully bound to port {0}", this.port);
				}

				return channel;
			} catch (java.net.BindException e) {
				if (i < maxRetries - 1) {
					LOGGER.log(Level.WARNING, "Port {0} is in use, trying {1}",
						new Object[] { currentPort, currentPort + 1 });
					currentPort++;
				} else {
					throw new Exception("Could not bind to any port from " + this.port + " to " + currentPort, e);
				}
			}
		}

		throw new Exception("Failed to bind socket after " + maxRetries + " attempts");
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (writeInstanceFiles) {
				try {
					LspInstanceRegistry.unregisterInstance(instancePaths);
					LOGGER.log(Level.INFO, "Unregistered {0} instance from {1}",
						new Object[] {
							instancePaths.getServerType(),
							instancePaths.getInstanceFilePath()
						});
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "Failed to remove instance file entry", e);
				}
			}
		}));
	}

	/**
	 * Set the initialize result from the stdio client.
	 * Must be called after the stdio client completes initialization.
	 */
	public void setInitializeResult(InitializeResult result) {
		this.cachedInitializeResult = result;
		if (multiClientLauncher != null) {
			multiClientLauncher.setInitializeResult(result);
		}
		LOGGER.log(Level.INFO, "Cached initialize result for socket clients");
	}
}
