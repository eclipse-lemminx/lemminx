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

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.inlayhint.IInlayHintRequest;
import org.eclipse.lemminx.settings.XMLInlayHintSettings;
import org.eclipse.lsp4j.Range;

/**
 * Inlay hint request implementation.
 */
class InlayHintRequest implements IInlayHintRequest {

	private final DOMDocument document;
	private final Range range;
	private final XMLInlayHintSettings settings;

	public InlayHintRequest(DOMDocument document, XMLInlayHintSettings settings, Range range) {
		this.document = document;
		this.settings = settings;
		this.range = range;
	}

	@Override
	public DOMDocument getDocument() {
		return document;
	}

	@Override
	public Range getRange() {
		return range;
	}

	@Override
	public XMLInlayHintSettings getInlayHintSettings() {
		return settings;
	}
}
