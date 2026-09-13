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
package org.eclipse.lemminx.extensions.contentmodel.commands;

import org.eclipse.lemminx.commons.progress.ProgressMonitor;
import org.eclipse.lemminx.commons.progress.ProgressSupport;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLDocumentGenerator;
import org.eclipse.lemminx.extensions.contentmodel.generator.XMLGenerationSettings;
import org.eclipse.lemminx.extensions.contentmodel.model.ContentModelManager;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.services.extensions.commands.ArgumentsUtils;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService.IDelegateCommandHandler;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.JSONUtility;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * XML Command "xml.grammar.generate" to generate an XML document from a given
 * grammar (XSD, DTD, RelaxNG, etc.) and a root element name.
 *
 * <p>
 * The command parameters {@link ExecuteCommandParams} must be filled with 2 or 3
 * parameters:
 * </p>
 *
 * <ul>
 * <li>grammar URI (String) : the grammar file URI.</li>
 * <li>root element name (String) : the local name of the root element to
 * generate.</li>
 * <li>generation settings (optional, {@link XMLGenerationSettings}) :
 * non-persistent settings overriding the persistent ones (e.g. from a wizard).
 * If not provided, persistent settings from client configuration are used,
 * including profile resolution.</li>
 * </ul>
 */
public class GenerateXMLFromGrammarCommand implements IDelegateCommandHandler {

	public static final String COMMAND_ID = "xml.grammar.generate";

	private final ContentModelManager contentModelManager;

	private final ProgressSupport progressSupport;

	private ContentModelSettings contentModelSettings;

	public GenerateXMLFromGrammarCommand(ContentModelManager contentModelManager, ProgressSupport progressSupport) {
		this.contentModelManager = contentModelManager;
		this.progressSupport = progressSupport;
	}

	/**
	 * Updates the persistent content model settings (called when client
	 * configuration changes).
	 *
	 * @param contentModelSettings the content model settings.
	 */
	public void updateContentModelSettings(ContentModelSettings contentModelSettings) {
		this.contentModelSettings = contentModelSettings;
	}

	@Override
	public Object executeCommand(ExecuteCommandParams params, SharedSettings sharedSettings,
			CancelChecker cancelChecker) throws Exception {
		String grammarURI = ArgumentsUtils.getArgAt(params, 0, String.class);
		String rootElementName = ArgumentsUtils.getArgAt(params, 1, String.class);

		// Try to get non-persistent settings from command parameters (e.g. from wizard)
		XMLGenerationSettings generationSettings = null;
		Object settingsArg = ArgumentsUtils.getArgAt(params.getArguments(), 2);
		if (settingsArg != null) {
			generationSettings = JSONUtility.toModel(settingsArg, XMLGenerationSettings.class);
		}

		// Fallback to persistent settings from client configuration
		if (generationSettings == null) {
			if (contentModelSettings != null && contentModelSettings.getGeneration() != null) {
				generationSettings = contentModelSettings.getGeneration().resolve(grammarURI);
			} else {
				generationSettings = new XMLGenerationSettings();
			}
		}

		final XMLGenerationSettings settings = generationSettings;
		ProgressMonitor monitor = progressSupport != null ? progressSupport.createProgressMonitor() : null;
		if (monitor != null) {
			monitor.begin("Generating XML...", "Loading grammar", null, null);
		}

		return contentModelManager.loadCMDocumentAsync(grammarURI)
				.thenApply(cmDocument -> {
					try {
						if (monitor != null) {
							monitor.report("Generating XML skeleton", null, null);
						}
						XMLDocumentGenerator generator = new XMLDocumentGenerator(sharedSettings, settings);
						return generator.generate(cmDocument, grammarURI, rootElementName);
					} finally {
						if (monitor != null) {
							monitor.end("");
						}
					}
				});
	}
}
