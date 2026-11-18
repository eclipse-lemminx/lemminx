/**
 *  Copyright (c) 2018 Angelo ZERR
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
package org.eclipse.lemminx.services.extensions;

import org.eclipse.lemminx.services.extensions.completion.ICompletionParticipant;
import org.eclipse.lemminx.services.extensions.diagnostics.IDiagnosticsParticipant;
import org.eclipse.lemminx.services.extensions.hover.IHoverParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;

/**
 * XML extension.
 *
 */
public interface IXMLExtension {

	/**
	 * Start method to register participants like {@link ICompletionParticipant},
	 * {@link IHoverParticipant}, {@link IDiagnosticsParticipant} in the given
	 * registry.
	 * @param params
	 *
	 * @param registry
	 * @param settings
	 */
	void start(InitializeParams params, XMLExtensionsRegistry registry);

	/**
	 * Stop method to un-register participants like {@link ICompletionParticipant},
	 * {@link IHoverParticipant}, {@link IDiagnosticsParticipant} in the given
	 * registry.
	 *
	 * @param registry
	 */
	void stop(XMLExtensionsRegistry registry);

	/**
	 * Called once the LSP client has sent the {@code initialized} notification,
	 * meaning the client is fully ready. Extensions that need to trigger background
	 * work (e.g. a workspace scan) after the LSP handshake should override this.
	 *
	 * @param params   the initialized notification params (usually empty)
	 * @param registry the extensions registry
	 */
	default void initialized(InitializedParams params, XMLExtensionsRegistry registry) {

	}

	/**
	 * Called when the Settings or a Document have been saved.
	 * context.getType() can be used to determine what type was saved.
	 * doSave is called on extension start up with a settings context to
	 * provide the xml settings to the extension.
	 * @param context
	 */
	default void doSave(ISaveContext context) {

	}
}
