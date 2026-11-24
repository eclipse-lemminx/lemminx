/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.contentmodel.settings;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Resolves VS Code-style predefined variables in file association patterns and
 * systemIds.
 *
 * <p>
 * Supported variables:
 * </p>
 * <ul>
 * <li>{@code ${workspaceFolder}} — workspace folder containing the
 * document</li>
 * <li>{@code ${workspaceFolderBasename}} — basename of the workspace
 * folder</li>
 * <li>{@code ${file}} — full path of the document</li>
 * <li>{@code ${fileWorkspaceFolder}} — workspace folder of the document (same
 * as workspaceFolder)</li>
 * <li>{@code ${relativeFile}} — document path relative to workspace
 * folder</li>
 * <li>{@code ${relativeFileDirname}} — dirname of relativeFile</li>
 * <li>{@code ${fileBasename}} — basename of the document</li>
 * <li>{@code ${fileBasenameNoExtension}} — basename without extension</li>
 * <li>{@code ${fileExtname}} — extension of the document (with dot)</li>
 * <li>{@code ${fileDirname}} — directory of the document</li>
 * <li>{@code ${env:Name}} — environment variable</li>
 * </ul>
 */
public class FileAssociationVariableSupport {

	private FileAssociationVariableSupport() {
	}

	/**
	 * Resolves all supported variables in the given value using the document URI and
	 * workspace folder URIs.
	 *
	 * @param value               the string containing variable references.
	 * @param documentUri         the URI of the document being matched.
	 * @param workspaceFolderUris the list of workspace folder URIs.
	 * @return the string with all resolvable variables substituted.
	 */
	public static String resolve(String value, URI documentUri, List<String> workspaceFolderUris) {
		if (!VariableSupportUtils.hasVariables(value)) {
			return value;
		}

		String result = value;

		// Path-independent variables: resolve before Paths.get so non-file URIs
		// still get environment variables resolved.
		result = VariableSupportUtils.resolveEnvVariables(result);

		Path documentPath;
		try {
			documentPath = Paths.get(documentUri);
		} catch (Exception e) {
			return result;
		}

		// File-level variables
		result = replaceIfPresent(result, "${file}", normalizePathSeparator(documentPath.toString()));
		Path parentPath = documentPath.getParent();
		if (parentPath != null) {
			result = replaceIfPresent(result, "${fileDirname}", normalizePathSeparator(parentPath.toString()));
		}
		Path fileName = documentPath.getFileName();
		if (fileName != null) {
			String fileBasename = fileName.toString();
			result = replaceIfPresent(result, "${fileBasename}", fileBasename);
			int dotIndex = fileBasename.lastIndexOf('.');
			if (dotIndex > 0) {
				result = replaceIfPresent(result, "${fileBasenameNoExtension}",
						fileBasename.substring(0, dotIndex));
				result = replaceIfPresent(result, "${fileExtname}",
						fileBasename.substring(dotIndex));
			} else {
				result = replaceIfPresent(result, "${fileBasenameNoExtension}", fileBasename);
				result = replaceIfPresent(result, "${fileExtname}", "");
			}
		}

		// Workspace-level variables
		if (workspaceFolderUris != null && !workspaceFolderUris.isEmpty()) {
			String workspaceFolderPath = findWorkspaceFolder(documentPath, workspaceFolderUris);
			if (workspaceFolderPath != null) {
				result = replaceIfPresent(result, "${workspaceFolder}", workspaceFolderPath);
				result = replaceIfPresent(result, "${fileWorkspaceFolder}", workspaceFolderPath);
				Path wsFolderPath = Paths.get(workspaceFolderPath);
				Path wsName = wsFolderPath.getFileName();
				if (wsName != null) {
					result = replaceIfPresent(result, "${workspaceFolderBasename}", wsName.toString());
				}
				try {
					Path relativePath = wsFolderPath.relativize(documentPath);
					result = replaceIfPresent(result, "${relativeFile}",
							normalizePathSeparator(relativePath.toString()));
					Path relativeParent = relativePath.getParent();
					result = replaceIfPresent(result, "${relativeFileDirname}",
							relativeParent != null ? normalizePathSeparator(relativeParent.toString()) : "");
				} catch (IllegalArgumentException e) {
					// Cannot relativize (different roots on Windows)
				}
			}
		}

		return result;
	}

	/**
	 * Finds the workspace folder that contains the given document path.
	 *
	 * @param documentPath       the document path.
	 * @param workspaceFolderUris the list of workspace folder URIs.
	 * @return the file path of the matching workspace folder, or the first workspace
	 *         folder if none matches, or null if the list is empty.
	 */
	private static String findWorkspaceFolder(Path documentPath, List<String> workspaceFolderUris) {
		String firstFolder = null;
		String bestMatch = null;
		int bestDepth = -1;
		for (String wsUri : workspaceFolderUris) {
			try {
				Path wsPath = Paths.get(URI.create(wsUri));
				String wsPathStr = normalizePathSeparator(wsPath.toString());
				if (firstFolder == null) {
					firstFolder = wsPathStr;
				}
				if (documentPath.startsWith(wsPath)) {
					int depth = wsPath.getNameCount();
					if (depth > bestDepth) {
						bestMatch = wsPathStr;
						bestDepth = depth;
					}
				}
			} catch (Exception e) {
				// Skip invalid workspace folder URI
			}
		}
		return bestMatch != null ? bestMatch : firstFolder;
	}

	private static String replaceIfPresent(String value, String variable, String replacement) {
		if (value.contains(variable)) {
			return value.replace(variable, replacement);
		}
		return value;
	}

	private static String normalizePathSeparator(String path) {
		return path.replace('\\', '/');
	}
}
