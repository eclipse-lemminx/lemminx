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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.eclipse.lemminx.commons.ParentProcessWatcher;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.launcher.multiclient.MultiClientLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

public class XMLServerLauncher {

	/**
	 * Calls {@link #launch(InputStream, OutputStream)}, using the standard input
	 * and output streams.
	 */
	public static void main(String[] args) {

		// Parse arguments for MCP mode
		boolean mcpEnabled = false;
		String workspacePath = null;
		int socketPort = 0; // 0 = free port (OS chooses)
		String clientName = null;
		String clientVersion = null;

		for (int i = 0; i < args.length; i++) {
			if ("--mcp-enabled".equals(args[i])) {
				mcpEnabled = true;
			} else if ("--workspace".equals(args[i]) && i + 1 < args.length) {
				workspacePath = args[i + 1];
				i++;
			} else if ("--port".equals(args[i]) && i + 1 < args.length) {
				socketPort = Integer.parseInt(args[i + 1]);
				i++;
			} else if ("--client-name".equals(args[i]) && i + 1 < args.length) {
				clientName = args[i + 1];
				i++;
			} else if ("--client-version".equals(args[i]) && i + 1 < args.length) {
				clientVersion = args[i + 1];
				i++;
			}
		}

		final String HTTP_PROXY_HOST = System.getenv("HTTP_PROXY_HOST");
		final String HTTP_PROXY_PORT = System.getenv("HTTP_PROXY_PORT");
		final String HTTP_PROXY_USERNAME = System.getenv("HTTP_PROXY_USERNAME");
		final String HTTP_PROXY_PASSWORD = System.getenv("HTTP_PROXY_PASSWORD");
		final boolean LEMMINX_DEBUG = System.getenv("LEMMINX_DEBUG") != null;

		if (HTTP_PROXY_HOST != null && HTTP_PROXY_PORT != null) {
			System.setProperty("http.proxyHost", HTTP_PROXY_HOST);
			System.setProperty("http.proxyPort", HTTP_PROXY_PORT);
			System.setProperty("https.proxyHost", HTTP_PROXY_HOST);
			System.setProperty("https.proxyPort", HTTP_PROXY_PORT);
		}

		if (HTTP_PROXY_USERNAME != null && HTTP_PROXY_PASSWORD != null) {
			System.setProperty("http.proxyUser", HTTP_PROXY_USERNAME);
			System.setProperty("http.proxyPassword", HTTP_PROXY_PASSWORD);
		}

		final String username = System.getProperty("http.proxyUser");
		final String password = System.getProperty("http.proxyPassword");

		if (username != null && password != null) {
			Authenticator.setDefault(new Authenticator() {

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password.toCharArray());
				}

			});
		}
		InputStream in = System.in;
		PrintStream out = System.out;
		System.setIn(new NoOpInputStream());
		System.setOut(new NoOpPrintStream());
		if (!LEMMINX_DEBUG) {
			System.setErr(new NoOpPrintStream());
		}

		// If MCP enabled, launch with shared server mode
		if (mcpEnabled && workspacePath != null) {
			launchWithMCP(in, out, workspacePath, socketPort, clientName, clientVersion);
		} else {
			// Normal stdio-only mode
			launch(in, out);
		}
	}

	/**
	 * Launches {@link XMLLanguageServer} and makes it accessible through the JSON
	 * RPC protocol defined by the LSP.
	 *
	 * @param launcherFuture The future returned by
	 *                       {@link org.eclipse.lsp4j.jsonrpc.Launcher#startListening()}.
	 *                       (I'm not 100% sure how it meant to be used though, as
	 *                       it's undocumented...)
	 */
	public static void launch(InputStream in, OutputStream out) {
		XMLLanguageServer server = new XMLLanguageServer();
		Function<MessageConsumer, MessageConsumer> wrapper;
		if ("false".equals(System.getProperty("watchParentProcess"))) {
			wrapper = it -> it;
		} else {
			wrapper = new ParentProcessWatcher(server);
		}
		Launcher<LanguageClient> launcher = createServerLauncher(server, in, out, Executors.newCachedThreadPool(),
				wrapper);
		server.setClient(launcher.getRemoteProxy());
		launcher.startListening();
	}

	/**
	 * Launch with MCP mode enabled - stdio + socket for shared server.
	 */
	private static void launchWithMCP(InputStream in, OutputStream out, String workspacePath, int socketPort,
			String clientName, String clientVersion) {
		XMLLanguageServer server = new XMLLanguageServer();
		Function<MessageConsumer, MessageConsumer> wrapper;
		if ("false".equals(System.getProperty("watchParentProcess"))) {
			wrapper = it -> it;
		} else {
			wrapper = new ParentProcessWatcher(server);
		}
		MultiClientLauncher.Builder<LanguageClient> builder = new MultiClientLauncher.Builder<LanguageClient>()
				.setLocalService(server) //
				.setRemoteInterface(XMLLanguageClientAPI.class) //
				.setInput(in) //
				.setOutput(out) //
				.setExecutorService(Executors.newCachedThreadPool()) //
				.wrapMessages(wrapper) //
				.enableSocket(socketPort, workspacePath, "lemminx");

		// Add client info if provided
		if (clientName != null || clientVersion != null) {
			builder.setClientInfo(clientName, clientVersion);
		}

		Launcher<LanguageClient> launcher = builder.create();
		server.setClient(launcher.getRemoteProxy());
		launcher.startListening();
	}

	/**
	 * Create a new Launcher for a language server and an input and output stream.
	 * Threads are started with the given executor service. The wrapper function is
	 * applied to the incoming and outgoing message streams so additional message
	 * handling such as validation and tracing can be included.
	 *
	 * @param server          - the server that receives method calls from the
	 *                        remote client
	 * @param in              - input stream to listen for incoming messages
	 * @param out             - output stream to send outgoing messages
	 * @param executorService - the executor service used to start threads
	 * @param wrapper         - a function for plugging in additional message
	 *                        consumers
	 */
	private static Launcher<LanguageClient> createServerLauncher(LanguageServer server, InputStream in,
			OutputStream out, ExecutorService executorService, Function<MessageConsumer, MessageConsumer> wrapper) {
		return new Builder<LanguageClient>().setLocalService(server) //
				.setRemoteInterface(XMLLanguageClientAPI.class) // Set client as XML language client
				.setInput(in) //
				.setOutput(out) //
				.setExecutorService(executorService) //
				.wrapMessages(wrapper) //
				.create();
	}
}
