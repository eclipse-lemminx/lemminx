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
package org.eclipse.lemminx.services.extensions;

import java.util.List;

import org.eclipse.lsp4j.FileEvent;

/**
 * Context passed to
 * {@link IWorkspaceServiceParticipant#didChangeWatchedFiles(List, IWatchedFilesContext)}
 * so extensions can request deferred client notifications after all participants have
 * processed the current file-change batch.
 *
 * <p><b>Design — N-to-1 refresh:</b> Multiple extensions may call
 * {@link ICodeLensRefresher#requestCodeLensRefresh()} for the same batch. The workspace service
 * inspects {@link ICodeLensRefresher#isCodeLensRefreshRequested()} once after all participants
 * finish and sends exactly one {@code workspace/codeLens/refresh} to the client.
 * This mirrors the {@code ISaveContext} pattern used for document saves.
 *
 * <p><b>Use case — file copied / created:</b>
 * The user copies {@code order.xml} (which declares
 * {@code xsi:noNamespaceSchemaLocation="schema.xsd"}) into the workspace.
 * The file watcher fires a {@code Created} event.
 * {@link IWorkspaceServiceParticipant#didChangeWatchedFiles(List, IWatchedFilesContext)}
 * is called; {@code WorkspaceIndexPlugin} re-indexes the file and calls
 * {@link ICodeLensRefresher#requestCodeLensRefresh()} because grammar references were found.
 * After all participants finish, the workspace service sends one
 * {@code workspace/codeLens/refresh}, causing the "N references" lens on
 * {@code schema.xsd} to update immediately.
 *
 * <p><b>Conditional refresh:</b> The refresh is only requested when the indexed
 * file actually contains grammar references ({@link WorkspaceIndex#indexFile}
 * returns {@code true}). Creating a plain XML file with no grammar declarations
 * does not trigger an unnecessary client round-trip.
 *
 * <p><b>Use case — diagnostics refresh:</b>
 * After a file is indexed, the "unused" diagnostics on open catalog documents may change
 * (a newly pasted XML file may reference a catalog entry that was previously unused).
 * {@link WorkspaceIndexPlugin} calls {@link #requestDiagnosticsRefresh()} so that
 * catalog diagnostics are republished without requiring the user to save or reopen the file.
 *
 * @see IWorkspaceServiceParticipant
 * @see ICodeLensRefresher
 */
public interface IWatchedFilesContext extends ICodeLensRefresher {

	/**
	 * Signals that diagnostics should be refreshed for documents affected by the
	 * current file-change batch (e.g. open catalog documents whose "unused" warnings
	 * may now be stale). The workspace service sends the refresh once after all
	 * participants finish, regardless of how many times this method is called.
	 */
	void requestDiagnosticsRefresh();

	/**
	 * Returns {@code true} if at least one participant requested a diagnostics refresh.
	 */
	boolean isDiagnosticsRefreshRequested();
}
