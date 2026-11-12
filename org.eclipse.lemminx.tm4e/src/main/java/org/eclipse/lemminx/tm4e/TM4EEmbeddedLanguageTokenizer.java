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
package org.eclipse.lemminx.tm4e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.extensions.embeddedLanguages.participants.IEmbeddedLanguageTokenizer;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.grammar.IStateStack;
import org.eclipse.tm4e.core.grammar.IToken;
import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;
import org.eclipse.tm4e.core.registry.IGrammarSource;
import org.eclipse.tm4e.core.registry.Registry;

/**
 * TM4E-based implementation of {@link IEmbeddedLanguageTokenizer}.
 *
 * <p>
 * Uses TM4E's {@link Registry} to load TextMate grammars and tokenize embedded
 * language content. Grammar file paths are provided by the client via a custom
 * LSP request.
 * </p>
 */
public class TM4EEmbeddedLanguageTokenizer implements IEmbeddedLanguageTokenizer {

	private static final Logger LOGGER = Logger.getLogger(TM4EEmbeddedLanguageTokenizer.class.getName());

	private final Registry registry = new Registry();

	private final Map<String, IGrammar> grammarCache = new ConcurrentHashMap<>();

	private final Map<String, String> grammarPaths = new ConcurrentHashMap<>();

	/**
	 * Register a grammar file path for the given language ID.
	 *
	 * @param languageId  the language identifier (e.g. "java", "sql").
	 * @param grammarPath the path to the .tmLanguage.json file.
	 */
	public void registerGrammarPath(String languageId, String grammarPath) {
		grammarPaths.put(languageId, grammarPath);
		grammarCache.remove(languageId);
	}

	@Override
	public List<EmbeddedToken> tokenize(String text, String languageId) {
		IGrammar grammar = getGrammar(languageId);
		if (grammar == null) {
			return Collections.emptyList();
		}
		List<EmbeddedToken> result = new ArrayList<>();
		String[] lines = text.split("\\r?\\n", -1);
		int lineOffset = 0;
		IStateStack state = null;
		for (String line : lines) {
			ITokenizeLineResult<IToken[]> lineResult = grammar.tokenizeLine(line, state, null);
			for (IToken token : lineResult.getTokens()) {
				int start = token.getStartIndex();
				int end = token.getEndIndex();
				if (start < end) {
					result.add(new EmbeddedToken(
							lineOffset + start,
							lineOffset + end,
							token.getScopes()));
				}
			}
			state = lineResult.getRuleStack();
			lineOffset += line.length() + 1; // +1 for the newline character
		}
		return result;
	}

	private IGrammar getGrammar(String languageId) {
		return grammarCache.computeIfAbsent(languageId, id -> {
			String path = grammarPaths.get(id);
			if (path == null) {
				LOGGER.log(Level.FINE, "No grammar path registered for language: {0}", id);
				return null;
			}
			try {
				return registry.addGrammar(IGrammarSource.fromFile(Path.of(path)));
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to load grammar for language: " + id, e);
				return null;
			}
		});
	}
}
