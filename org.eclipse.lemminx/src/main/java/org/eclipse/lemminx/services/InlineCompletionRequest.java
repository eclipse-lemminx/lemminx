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

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionRequest;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.InlineCompletionContext;
import org.eclipse.lsp4j.Position;

/**
 * Inline completion request implementation.
 */
class InlineCompletionRequest extends AbstractPositionRequest implements IInlineCompletionRequest {

	private final InlineCompletionContext context;
	private final SharedSettings sharedSettings;

	public InlineCompletionRequest(DOMDocument xmlDocument, Position position, 
	                               InlineCompletionContext context,
	                               SharedSettings settings,
	                               XMLExtensionsRegistry extensionsRegistry) throws BadLocationException {
		super(xmlDocument, position, extensionsRegistry);
		this.context = context;
		this.sharedSettings = settings;
	}

	@Override
	public InlineCompletionContext getContext() {
		return context;
	}

	@Override
	public SharedSettings getSharedSettings() {
		return sharedSettings;
	}

	@Override
	public boolean canSupportMarkupKind(String kind) {
		// Inline completion typically doesn't use markup documentation
		return false;
	}
}