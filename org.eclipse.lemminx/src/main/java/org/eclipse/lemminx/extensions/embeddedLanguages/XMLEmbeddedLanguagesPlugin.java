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
package org.eclipse.lemminx.extensions.embeddedLanguages;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.extensions.embeddedLanguages.participants.EmbeddedLanguageSemanticTokensParticipant;
import org.eclipse.lemminx.extensions.embeddedLanguages.participants.IEmbeddedLanguageTokenizer;
import org.eclipse.lemminx.extensions.embeddedLanguages.settings.XMLEmbeddedLanguagesSettings;
import org.eclipse.lemminx.services.extensions.IXMLExtension;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.commands.IXMLCommandService;
import org.eclipse.lemminx.services.extensions.save.ISaveContext;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;

/**
 * XML embedded languages plugin.
 *
 * <p>
 * Provides syntax highlighting for embedded language content (e.g., Java, SQL,
 * JavaScript) inside XML text/CDATA nodes using TextMate grammars (via TM4E
 * when available on Java 21+).
 * </p>
 */
public class XMLEmbeddedLanguagesPlugin implements IXMLExtension {

	private static final Logger LOGGER = Logger.getLogger(XMLEmbeddedLanguagesPlugin.class.getName());

	private static final String TEXT_MATE_GRAMMAR_COMMAND = "xml.textMateGrammar";

	private final EmbeddedLanguageSemanticTokensParticipant semanticTokensParticipant;

	private XMLEmbeddedLanguagesSettings embeddedLanguagesSettings;

	private IEmbeddedLanguageTokenizer tokenizer;

	private XMLExtensionsRegistry registry;

	private final Set<String> resolvedLanguages = new HashSet<>();

	public XMLEmbeddedLanguagesPlugin() {
		semanticTokensParticipant = new EmbeddedLanguageSemanticTokensParticipant(this);
	}

	@Override
	public void doSave(ISaveContext context) {
		if (context.getType() != ISaveContext.SaveContextType.DOCUMENT) {
			updateSettings(context);
		}
	}

	private void updateSettings(ISaveContext saveContext) {
		Object initializationOptionsSettings = saveContext.getSettings();
		XMLEmbeddedLanguagesSettings settings = XMLEmbeddedLanguagesSettings
				.getXMLEmbeddedLanguagesSettings(initializationOptionsSettings);
		this.embeddedLanguagesSettings = settings;
		resolvedLanguages.clear();
	}

	@Override
	public void start(InitializeParams params, XMLExtensionsRegistry registry) {
		this.registry = registry;
		registry.registerSemanticTokensParticipant(semanticTokensParticipant);
		loadTokenizer();
	}

	@Override
	public void stop(XMLExtensionsRegistry registry) {
		registry.unregisterSemanticTokensParticipant(semanticTokensParticipant);
	}

	private void loadTokenizer() {
		try {
			ServiceLoader<IEmbeddedLanguageTokenizer> loader = ServiceLoader
					.load(IEmbeddedLanguageTokenizer.class);
			Iterator<IEmbeddedLanguageTokenizer> iterator = loader.iterator();
			if (iterator.hasNext()) {
				tokenizer = iterator.next();
				LOGGER.info("Embedded language tokenizer loaded: " + tokenizer.getClass().getName());
			} else {
				LOGGER.info("No embedded language tokenizer found. "
						+ "Embedded language highlighting is disabled.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to load embedded language tokenizer. "
					+ "Embedded language highlighting is disabled.", e);
		}
	}

	public XMLEmbeddedLanguagesSettings getEmbeddedLanguagesSettings() {
		return embeddedLanguagesSettings;
	}

	public IEmbeddedLanguageTokenizer getTokenizer() {
		return tokenizer;
	}

	@SuppressWarnings("unchecked")
	public void resolveGrammarIfNeeded(String languageId) {
		if (tokenizer == null || languageId == null || resolvedLanguages.contains(languageId)) {
			return;
		}
		resolvedLanguages.add(languageId);
		IXMLCommandService commandService = registry != null ? registry.getCommandService() : null;
		if (commandService == null) {
			return;
		}
		try {
			CompletableFuture<Object> future = commandService.executeClientCommand(
					new ExecuteCommandParams(TEXT_MATE_GRAMMAR_COMMAND, Arrays.asList(languageId)));
			Object result = future.get();
			if (result instanceof Map) {
				Map<String, Object> grammarInfo = (Map<String, Object>) result;
				String grammarPath = (String) grammarInfo.get("grammarPath");
				if (grammarPath != null) {
					tokenizer.registerGrammarPath(languageId, grammarPath);
					LOGGER.info("Resolved TextMate grammar for '" + languageId + "': " + grammarPath);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to resolve TextMate grammar for language: " + languageId, e);
		}
	}

	public void registerGrammarPath(String languageId, String grammarPath) {
		if (tokenizer != null) {
			tokenizer.registerGrammarPath(languageId, grammarPath);
			resolvedLanguages.add(languageId);
		}
	}

	void setTokenizer(IEmbeddedLanguageTokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
}
