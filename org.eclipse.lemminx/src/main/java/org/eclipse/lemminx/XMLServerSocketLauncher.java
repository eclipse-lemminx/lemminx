/**
 * Copyright (c) 2018 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * @author Dennis Huebner <dennis.huebner@gmail.com> - Initial contribution and API
 */
package org.eclipse.lemminx;

import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.eclipse.lsp4j.launcher.multiclient.LspSocketLauncher;
import org.eclipse.lsp4j.launcher.multiclient.MultiClientProxy;

/**
 * Launches {@link XMLLanguageServer} using asynchronous server-socket channel with multi-client support.
 * Multiple clients can connect to the same server instance via TCP socket.
 */
public class XMLServerSocketLauncher extends LspSocketLauncher<XMLLanguageServer, XMLLanguageClientAPI> {

	public XMLServerSocketLauncher() {
		super("lemminx");
	}

	/**
	 * Calls {@link #launch(String[], java.util.function.Function, Class)}
	 */
	public static void main(String[] args) throws Exception {
		XMLServerSocketLauncher launcher = new XMLServerSocketLauncher();
		launcher.launch(args, client -> {
			XMLLanguageServer server = new XMLLanguageServer();
			server.setClient(client);
			return server;
		}, XMLLanguageClientAPI.class);
	}

	/**
	 * Launch socket listener for MCP clients.
	 * Shares the same XMLLanguageServer instance with the stdio client.
	 *
	 * @param sharedServer the shared XMLLanguageServer instance
	 * @param port the port to listen on
	 * @param workspacePath the workspace path
	 * @param clientProxy the multi-client proxy to add socket clients to
	 */
	public void launchForSharedServer(XMLLanguageServer sharedServer, int port, String workspacePath,
			MultiClientProxy<XMLLanguageClientAPI> clientProxy) throws Exception {
		super.launchForSharedServer(sharedServer, port, workspacePath, clientProxy, XMLLanguageClientAPI.class);
	}
}
