/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.extensions;

import java.util.List;

import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.RenameFilesParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Workspace Service participant API.
 * @since 0.14.2
 */
public interface IWorkspaceServiceParticipant {

	/**
	 * Receive and handle notification about workspace folder changing on client side.
	 * @param params the workspace folders change description
	 * @see WorkspaceService#didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams)
	 */
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params);

	/**
	 * Called when watched files are created, changed, or deleted.
	 *
	 * <p>The {@link IWatchedFilesContext} lets extensions request a
	 * {@code workspace/codeLens/refresh} that is sent once — after all
	 * participants finish — regardless of how many extensions request it.
	 *
	 * @param changes the list of file events.
	 * @param context context for requesting deferred actions (e.g. code lens refresh).
	 * @see WorkspaceService#didChangeWatchedFiles(DidChangeWatchedFilesParams)
	 */
	default void didChangeWatchedFiles(List<FileEvent> changes, IWatchedFilesContext context) {
	}

	/**
	 * Called before files are renamed. Return a {@link WorkspaceEdit} with the
	 * text changes that should be applied, or {@code null} if none.
	 *
	 * @param params        the rename parameters (old/new URIs).
	 * @param cancelChecker checker to abort long-running computations.
	 * @return a WorkspaceEdit, or {@code null}.
	 * @see WorkspaceService#willRenameFiles(RenameFilesParams)
	 */
	default WorkspaceEdit willRenameFiles(RenameFilesParams params, CancelChecker cancelChecker) {
		return null;
	}
}
