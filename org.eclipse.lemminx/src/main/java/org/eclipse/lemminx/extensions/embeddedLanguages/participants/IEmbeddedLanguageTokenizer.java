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
package org.eclipse.lemminx.extensions.embeddedLanguages.participants;

import java.util.List;

/**
 * Tokenizer for embedded language content using TextMate grammars.
 */
public interface IEmbeddedLanguageTokenizer {

	/**
	 * Register a grammar file path for the given language ID.
	 *
	 * @param languageId  the language identifier (e.g. "java", "sql").
	 * @param grammarPath the path to the .tmLanguage.json file.
	 */
	void registerGrammarPath(String languageId, String grammarPath);

	/**
	 * Tokenize the given text using the grammar for the specified language.
	 *
	 * @param text       the text content to tokenize.
	 * @param languageId the language identifier (e.g. "java", "sql", "javascript").
	 * @return a list of embedded tokens, or an empty list if no grammar is
	 *         available.
	 */
	List<EmbeddedToken> tokenize(String text, String languageId);

	/**
	 * An embedded token produced by TextMate tokenization.
	 */
	class EmbeddedToken {
		private final int startIndex;
		private final int endIndex;
		private final List<String> scopes;

		public EmbeddedToken(int startIndex, int endIndex, List<String> scopes) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.scopes = scopes;
		}

		public int getStartIndex() {
			return startIndex;
		}

		public int getEndIndex() {
			return endIndex;
		}

		public List<String> getScopes() {
			return scopes;
		}
	}
}
