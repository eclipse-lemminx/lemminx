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
package org.eclipse.lemminx.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.inlayhint.IInlayHintParticipant;
import org.eclipse.lemminx.services.extensions.inlayhint.IInlayHintRequest;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Inlay Hint support.
 */
class XMLInlayHint {

	private final XMLExtensionsRegistry extensionsRegistry;

	private static final Logger LOGGER = Logger.getLogger(XMLInlayHint.class.getName());

	public XMLInlayHint(XMLExtensionsRegistry extensionsRegistry) {
		this.extensionsRegistry = extensionsRegistry;
	}

	public List<InlayHint> getInlayHint(DOMDocument xmlDocument, XMLInlayHintSettings settings, Range range,
			CancelChecker cancelChecker) {
		cancelChecker.checkCanceled();

		IInlayHintRequest request = new InlayHintRequest(xmlDocument, settings, range);
		List<InlayHint> inlayHints = new ArrayList<>();
		for (IInlayHintParticipant participant : extensionsRegistry.getInlayHintParticipants()) {
			try {
				participant.doInlayHint(request, inlayHints, cancelChecker);
			} catch (CancellationException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error while processing inlay hints for the participant '"
						+ participant.getClass().getName() + "'.", e);
			}
		}

		cancelChecker.checkCanceled();
		return inlayHints;
	}
}
