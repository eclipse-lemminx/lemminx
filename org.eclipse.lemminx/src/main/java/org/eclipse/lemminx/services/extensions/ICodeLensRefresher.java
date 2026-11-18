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

/**
 * Common API for requesting a {@code workspace/codeLens/refresh} to be sent to
 * the client after a batch event.
 *
 * <p>Implemented by contexts that process batches of events where multiple
 * extensions may independently decide that code lenses need updating:
 * <ul>
 *   <li>{@link org.eclipse.lemminx.services.extensions.save.ISaveContext} — after a
 *       document or settings save.</li>
 *   <li>{@link IWatchedFilesContext} — after a {@code workspace/didChangeWatchedFiles}
 *       batch.</li>
 * </ul>
 *
 * <p><b>N-to-1 design:</b> Multiple extensions may call
 * {@link #requestCodeLensRefresh()} for the same event batch. Only one
 * {@code workspace/codeLens/refresh} is sent to the client once all
 * participants finish.
 */
public interface ICodeLensRefresher {

	/**
	 * Requests a {@code workspace/codeLens/refresh} to be sent to the client once
	 * all extensions have finished processing the current event batch.
	 *
	 * <p>Multiple extensions may call this for the same batch; only one refresh is
	 * sent. The refresh is only sent if the client declared support for it
	 * ({@code workspace.codeLens.refreshSupport} in client capabilities).
	 */
	default void requestCodeLensRefresh() {
	}

	/**
	 * Returns {@code true} if any extension has requested a code lens refresh for
	 * the current event batch.
	 *
	 * <p>Called by the host service after all participants finish to decide whether
	 * to send {@code workspace/codeLens/refresh}.
	 */
	default boolean isCodeLensRefreshRequested() {
		return false;
	}
}
