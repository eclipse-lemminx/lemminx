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
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Generic registry for language server instances running in socket mode.
 * Manages instance files in ${workspace}/.lsp-servers/{serverType}.json
 * Works with any LSP server type (lemminx, jdtls, qute-ls, etc.).
 */
public class LspInstanceRegistry {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Registers a language server instance.
	 * Writes to ${workspace}/.lsp-servers/{type}.json
	 *
	 * @param instancePaths the instance paths (containing workspace and server type)
	 * @param port          the port the server is listening on
	 * @throws IOException if the file cannot be written
	 */
	public static synchronized void registerInstance(InstancePaths instancePaths, int port) throws IOException {
		registerInstance(instancePaths, port, null, null);
	}

	/**
	 * Registers a language server instance with client information.
	 * Writes to ${workspace}/.lsp-servers/{type}.json
	 *
	 * @param instancePaths the instance paths (containing workspace and server type)
	 * @param port          the port the server is listening on
	 * @param clientName    the name of the IDE/client (e.g., "VS Code", "IntelliJ IDEA"), can be null
	 * @param clientVersion the version of the IDE/client (e.g., "1.95.0"), can be null
	 * @throws IOException if the file cannot be written
	 */
	public static synchronized void registerInstance(InstancePaths instancePaths, int port, String clientName, String clientVersion) throws IOException {
		if (!instancePaths.hasWorkspace()) {
			return; // No workspace, nothing to write
		}

		InstanceInfo info = new InstanceInfo();
		info.port = port;
		info.pid = ProcessHandle.current().pid();
		info.clientName = clientName;
		info.clientVersion = clientVersion;

		// Create .lsp-servers directory if needed
		Files.createDirectories(instancePaths.getInstanceDir());

		// Write instance file
		try (Writer writer = Files.newBufferedWriter(instancePaths.getInstanceFilePath())) {
			GSON.toJson(info, writer);
		}
	}

	/**
	 * Unregisters a language server instance.
	 * Deletes ${workspace}/.lsp-servers/{type}.json
	 *
	 * @param instancePaths the instance paths (containing workspace and server type)
	 * @throws IOException if the file cannot be deleted
	 */
	public static synchronized void unregisterInstance(InstancePaths instancePaths) throws IOException {
		if (!instancePaths.hasWorkspace()) {
			return; // No workspace, nothing to delete
		}

		// Delete instance file
		Files.deleteIfExists(instancePaths.getInstanceFilePath());

		// Delete .lsp-servers directory if empty
		Path dir = instancePaths.getInstanceDir();
		if (Files.exists(dir) && Files.list(dir).count() == 0) {
			Files.deleteIfExists(dir);
		}
	}

	/**
	 * Instance information stored in the registry.
	 */
	public static class InstanceInfo {
		public int port;
		public long pid;
		public String clientName;    // e.g., "VS Code", "IntelliJ IDEA"
		public String clientVersion; // e.g., "1.95.0"
	}
}
