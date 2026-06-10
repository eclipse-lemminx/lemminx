/**
 *  Copyright (c) 2026 Red Hat Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Red Hat Inc. - initial API and implementation
 */
package org.eclipse.lemminx.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.InlineCompletionContext;
import org.eclipse.lsp4j.InlineCompletionItem;
import org.eclipse.lsp4j.InlineCompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML inline completion service.
 */
public class XMLInlineCompletion {

	private static final Logger LOGGER = Logger.getLogger(XMLInlineCompletion.class.getName());

	private final XMLExtensionsRegistry extensionsRegistry;

	public XMLInlineCompletion(XMLExtensionsRegistry extensionsRegistry) {
		this.extensionsRegistry = extensionsRegistry;
	}

	/**
	 * Returns inline completion items for the given document and position.
	 * 
	 * @param document      the DOM document
	 * @param position      the position where inline completion is requested
	 * @param context       the inline completion context
	 * @param settings      the shared settings
	 * @param cancelChecker the cancel checker
	 * @return the inline completion list
	 */
	public InlineCompletionList doInlineCompletion(DOMDocument document, Position position,
	                                               InlineCompletionContext context,
	                                               SharedSettings settings,
	                                               CancelChecker cancelChecker) {
		InlineCompletionResponse response = new InlineCompletionResponse();
		
		try {
			InlineCompletionRequest request = new InlineCompletionRequest(document, position, context, settings,
					extensionsRegistry);
			
			// Call all registered inline completion participants
			for (IInlineCompletionParticipant participant : extensionsRegistry.getInlineCompletionParticipants()) {
				try {
					cancelChecker.checkCanceled();
					participant.onInlineCompletion(request, response, cancelChecker);
				} catch (CancellationException e) {
					throw e;
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE,
							"Error while processing inline completion participant " + participant.getClass().getName(),
							e);
				}
			}
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error while computing inline completion", e);
		}
		
		InlineCompletionList result = new InlineCompletionList();
		result.setItems(response.getItems());
		return result;
	}
}