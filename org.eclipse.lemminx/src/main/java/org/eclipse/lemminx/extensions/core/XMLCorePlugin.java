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
package org.eclipse.lemminx.extensions.core;

import org.eclipse.lemminx.extensions.core.participants.inlinecompletion.XMLCloseTagInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.inlinecompletion.IInlineCompletionParticipant;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.InitializeParams;

/**
 * XML Core plugin extension to provide core XML editing features that are
 * not specific to any grammar type (XSD, DTD, etc.).
 * 
 * <p>
 * This plugin provides:
 * <ul>
 * <li>Inline completion for closing tags</li>
 * </ul>
 * </p>
 */
public class XMLCorePlugin implements IXMLExtension {

	private IInlineCompletionParticipant inlineCompletionParticipant;

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		inlineCompletionParticipant = new XMLCloseTagInlineCompletionParticipant();
		registry.registerInlineCompletionParticipant(inlineCompletionParticipant);
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.unregisterInlineCompletionParticipant(inlineCompletionParticipant);
	}

	@Override
	public void doSave(ISaveContext context) {
		// No settings to save
	}
}