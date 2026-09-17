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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Holds pre-computed file paths for LSP server instance files.
 * Calculates workspace-specific paths once in the constructor to avoid repeated string concatenation.
 * Uses ${workspace}/.lsp-servers/{serverType}.json (e.g., .lsp-servers/lemminx.json)
 */
public class InstancePaths {

	private static final String INSTANCES_DIR_NAME = ".lsp-servers";

	private final String serverType;
	private final String workspacePath;

	// Pre-computed paths
	private final String instanceFileName;
	private final Path instanceDir;
	private final Path instanceFilePath;

	private InstancePaths(String serverType, String workspacePath) {
		this.serverType = serverType;
		this.workspacePath = workspacePath;

		// Compute instance file name and path: ${workspace}/.lsp-servers/{serverType}.json
		this.instanceFileName = serverType + ".json";
		if (workspacePath != null) {
			this.instanceDir = Paths.get(workspacePath, INSTANCES_DIR_NAME);
			this.instanceFilePath = instanceDir.resolve(instanceFileName);
		} else {
			this.instanceDir = null;
			this.instanceFilePath = null;
		}
	}

	/**
	 * Creates a builder for instance paths.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for InstancePaths.
	 */
	public static class Builder {
		private String serverType;
		private String workspacePath;

		public Builder serverType(String serverType) {
			this.serverType = serverType;
			return this;
		}

		public Builder workspacePath(String workspacePath) {
			this.workspacePath = workspacePath;
			return this;
		}

		public InstancePaths build() {
			if (serverType == null || serverType.isEmpty()) {
				throw new IllegalArgumentException("serverType is required");
			}
			return new InstancePaths(serverType, workspacePath);
		}
	}

	public String getServerType() {
		return serverType;
	}

	public String getWorkspacePath() {
		return workspacePath;
	}

	public String getInstanceFileName() {
		return instanceFileName;
	}

	public Path getInstanceDir() {
		return instanceDir;
	}

	public Path getInstanceFilePath() {
		return instanceFilePath;
	}

	public boolean hasWorkspace() {
		return workspacePath != null && !workspacePath.isEmpty();
	}
}
