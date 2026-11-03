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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.lemminx.extensions.embeddedLanguages.participants.IEmbeddedLanguageTokenizer.EmbeddedToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TM4EEmbeddedLanguageTokenizerTest {

	private static final String TEST_GRAMMAR_PATH = Path
			.of("src/test/resources/test-grammar.tmLanguage.json")
			.toAbsolutePath()
			.toString();

	private TM4EEmbeddedLanguageTokenizer tokenizer;

	@BeforeEach
	void setUp() {
		tokenizer = new TM4EEmbeddedLanguageTokenizer();
		tokenizer.registerGrammarPath("test", TEST_GRAMMAR_PATH);
	}

	@Test
	void tokenizeKeyword() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("if", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "keyword.control.test");
	}

	@Test
	void tokenizeString() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("\"hello\"", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "string.quoted.double.test");
	}

	@Test
	void tokenizeComment() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("// a comment", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "comment.line.double-slash.test");
	}

	@Test
	void tokenizeNumber() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("42", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "constant.numeric.test");
	}

	@Test
	void tokenizeFunction() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("myFunc()", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "entity.name.function.test");
	}

	@Test
	void tokenizeMultipleTokenTypes() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("return \"value\"", "test");
		assertFalse(tokens.isEmpty());
		assertHasTokenWithScope(tokens, "keyword.control.test");
		assertHasTokenWithScope(tokens, "string.quoted.double.test");
	}

	@Test
	void tokenizeMultiLineCode() {
		String code = "def x\nreturn 42";
		List<EmbeddedToken> tokens = tokenizer.tokenize(code, "test");
		assertFalse(tokens.isEmpty());

		// "def" on line 1 at offset 0
		EmbeddedToken defToken = findTokenWithScope(tokens, "keyword.control.test");
		assertTrue(defToken != null);
		assertEquals(0, defToken.getStartIndex());
		assertEquals(3, defToken.getEndIndex());

		// "return" on line 2 starts at offset 6 ("def x\n" = 5+1 = 6)
		// "42" on line 2 starts at offset 13 ("def x\nreturn " = 6+7 = 13)
		EmbeddedToken numToken = findTokenWithScope(tokens, "constant.numeric.test");
		assertTrue(numToken != null);
		assertEquals(13, numToken.getStartIndex());
		assertEquals(15, numToken.getEndIndex());
	}

	@Test
	void tokenizeMultiLineOffsets() {
		String code = "// line1\n\"str\"";
		List<EmbeddedToken> tokens = tokenizer.tokenize(code, "test");
		assertFalse(tokens.isEmpty());

		EmbeddedToken commentToken = findTokenWithScope(tokens, "comment.line.double-slash.test");
		assertTrue(commentToken != null);
		assertEquals(0, commentToken.getStartIndex());

		// "\"str\"" starts at offset 9 ("// line1\n" = 9)
		EmbeddedToken stringToken = findTokenWithScope(tokens, "string.quoted.double.test");
		assertTrue(stringToken != null);
		assertEquals(9, stringToken.getStartIndex());
		assertEquals(14, stringToken.getEndIndex());
	}

	@Test
	void tokenizeUnregisteredLanguageReturnsEmpty() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("if else", "unknown");
		assertTrue(tokens.isEmpty());
	}

	@Test
	void tokenizeEmptyStringReturnsEmpty() {
		List<EmbeddedToken> tokens = tokenizer.tokenize("", "test");
		assertTrue(tokens.isEmpty());
	}

	@Test
	void reRegisterGrammarClearsCache() {
		List<EmbeddedToken> tokens1 = tokenizer.tokenize("if", "test");
		assertFalse(tokens1.isEmpty());

		// Re-register same grammar path (should clear cache and still work)
		tokenizer.registerGrammarPath("test", TEST_GRAMMAR_PATH);
		List<EmbeddedToken> tokens2 = tokenizer.tokenize("if", "test");
		assertFalse(tokens2.isEmpty());
		assertHasTokenWithScope(tokens2, "keyword.control.test");
	}

	private static void assertHasTokenWithScope(List<EmbeddedToken> tokens, String scope) {
		assertTrue(
				findTokenWithScope(tokens, scope) != null,
				"Expected a token with scope '" + scope + "' but none found in: " + formatTokens(tokens));
	}

	private static EmbeddedToken findTokenWithScope(List<EmbeddedToken> tokens, String scope) {
		for (EmbeddedToken token : tokens) {
			for (String s : token.getScopes()) {
				if (s.contains(scope)) {
					return token;
				}
			}
		}
		return null;
	}

	private static String formatTokens(List<EmbeddedToken> tokens) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < tokens.size(); i++) {
			EmbeddedToken t = tokens.get(i);
			if (i > 0)
				sb.append(", ");
			sb.append("{").append(t.getStartIndex()).append("-").append(t.getEndIndex())
					.append(" ").append(t.getScopes()).append("}");
		}
		return sb.append("]").toString();
	}
}
