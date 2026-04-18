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
package org.eclipse.lemminx.extensions.minify;

import org.eclipse.lemminx.extensions.minify.commands.MinifyDocumentCommand;
import org.eclipse.lemminx.extensions.minify.participants.MinifyCodeActionParticipant;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.InitializeParams;

/**
 * Minify plugin to register the minify command and code action.
 */
public class MinifyPlugin implements IXMLExtension {

	private MinifyCodeActionParticipant codeActionParticipant;

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		// Register minify command
		IXMLCommandService commandService = registry.getCommandService();
		if (commandService != null) {
			IXMLDocumentProvider documentProvider = registry.getDocumentProvider();
			commandService.registerCommand(MinifyDocumentCommand.COMMAND_ID,
					new MinifyDocumentCommand(documentProvider));
		}

		// Register minify code action
		codeActionParticipant = new MinifyCodeActionParticipant();
		registry.registerCodeActionParticipant(codeActionParticipant);
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		// Unregister minify command
		IXMLCommandService commandService = registry.getCommandService();
		if (commandService != null) {
			commandService.unregisterCommand(MinifyDocumentCommand.COMMAND_ID);
		}

		// Unregister minify code action
		registry.unregisterCodeActionParticipant(codeActionParticipant);
	}

	@Override
	public void doSave(ISaveContext context) {
		// Nothing to do on save
	}
}
