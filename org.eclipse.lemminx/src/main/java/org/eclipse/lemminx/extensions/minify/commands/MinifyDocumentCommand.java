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
package org.eclipse.lemminx.extensions.minify.commands;

import java.util.List;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.XMLMinifier;
import org.eclipse.lemminx.services.extensions.commands.AbstractDOMDocumentCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.minify.document" to minify an XML document by removing
 * unnecessary whitespace.
 *
 */
public class MinifyDocumentCommand extends AbstractDOMDocumentCommandHandler {

	public static final String COMMAND_ID = "xml.minify.document";

	private final XMLMinifier minifier;

	public MinifyDocumentCommand(IXMLDocumentProvider documentProvider) {
		super(documentProvider);
		this.minifier = new XMLMinifier();
	}

	@Override
	protected Object executeCommand(DOMDocument document, ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		// Get the minification text edits
		List<? extends TextEdit> edits = minifier.minify(document, null, sharedSettings);
		return edits;
	}
}
