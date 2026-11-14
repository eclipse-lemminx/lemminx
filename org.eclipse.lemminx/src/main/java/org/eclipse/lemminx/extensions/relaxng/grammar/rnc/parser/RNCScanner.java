/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser;

import java.util.function.Predicate;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCKeywords;
import org.eclipse.lemminx.utils.StringUtils;

/**
 * A scanner (lexer/tokenizer) for RELAX NG Compact Syntax (RNC) text.
 *
 * <p>
 * Produces a stream of {@link RNCTokenType} tokens by scanning the input
 * character-by-character. The scanner is forward-only: call {@link #scan()}
 * repeatedly to advance through the token stream until {@link RNCTokenType#EOS}
 * is returned.
 * </p>
 *
 * <p>
 * Use case: all RNC language features (formatting, semantic tokens, folding,
 * highlighting, completions, code actions, diagnostics) use this scanner to
 * tokenize RNC content without building a full parse tree. This keeps each
 * feature lightweight and fast.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * CharSequence text = "start = element root { text }";
 * RNCScanner scanner = RNCScanner.createScanner(text);
 * RNCTokenType token = scanner.scan();
 * while (token != RNCTokenType.EOS) {
 *     System.out.println(token + ": " + scanner.getTokenText()
 *         + " [" + scanner.getTokenOffset() + ".." + scanner.getTokenEnd() + ")");
 *     token = scanner.scan();
 * }
 * // Output:
 * // KeywordStart: start [0..5)
 * // Whitespace:   [5..6)
 * // Assign: = [6..7)
 * // Whitespace:   [7..8)
 * // KeywordElement: element [8..15)
 * // Whitespace:   [15..16)
 * // Identifier: root [16..20)
 * // Whitespace:   [20..21)
 * // BraceOpen: { [21..22)
 * // Whitespace:   [22..23)
 * // KeywordText: text [23..27)
 * // Whitespace:   [27..28)
 * // BraceClose: } [28..29)
 * </pre>
 *
 * <h3>Design Notes</h3>
 * <ul>
 * <li>Uses char-by-char scanning (no regex or substring) for performance
 * on large files.</li>
 * <li>Keywords are recognized context-free: the word {@code element} always
 * produces {@link RNCTokenType#KeywordElement}. The escaped form
 * {@code \element} produces {@link RNCTokenType#Identifier}, allowing
 * reserved words to be used as names.</li>
 * <li>Whitespace tokens are emitted (not skipped) so that the formatter
 * can inspect and adjust them.</li>
 * </ul>
 *
 * @see RNCTokenType
 * @see RNCScannerState
 */
public class RNCScanner {

	/** Predicate for the first character of an identifier: letter or underscore. */
	private static final Predicate<Integer> IDENTIFIER_START_PREDICATE = ch -> {
		return ch == '_' || Character.isLetter(ch);
	};

	/** Predicate for subsequent characters of an identifier: letter, digit, '_', '-', or '.'. */
	private static final Predicate<Integer> IDENTIFIER_PART_PREDICATE = ch -> {
		return ch == '_' || ch == '-' || ch == '.' || Character.isLetterOrDigit(ch);
	};

	/** Predicate for whitespace characters (space, tab, newline, carriage return, form feed). */
	private static final Predicate<Integer> WHITESPACE_PREDICATE = ch -> {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
	};

	/** The input text being scanned. */
	private final CharSequence source;

	/** The total length of the input text. */
	private final int len;

	/** The current scan position (next character to be consumed). */
	private int position;

	/** The current scanner state (reserved for future stateful scanning). */
	private RNCScannerState state;

	/** The offset of the current token's first character. */
	private int tokenOffset;

	/** The type of the current token. */
	private RNCTokenType tokenType;

	/** An error message associated with the current token, if any. */
	private String tokenError;

	/**
	 * Creates a scanner starting at the given offset.
	 *
	 * @param input         the RNC text to scan.
	 * @param initialOffset the offset to start scanning from.
	 */
	public RNCScanner(CharSequence input, int initialOffset) {
		this.source = input;
		this.len = input.length();
		this.position = initialOffset;
		this.state = RNCScannerState.WithinContent;
		this.tokenOffset = initialOffset;
		this.tokenType = RNCTokenType.Unknown;
	}

	/**
	 * Creates a scanner for the given input, starting from the beginning.
	 *
	 * <p>
	 * Use case: most callers scan the entire document from offset 0.
	 * </p>
	 *
	 * @param input the RNC text to scan.
	 * @return a new scanner positioned at offset 0.
	 */
	public static RNCScanner createScanner(CharSequence input) {
		return new RNCScanner(input, 0);
	}

	/**
	 * Creates a scanner for the given input, starting at a specific offset.
	 *
	 * <p>
	 * Use case: scanning a sub-region of the document (e.g. for incremental
	 * updates or range formatting).
	 * </p>
	 *
	 * @param input         the RNC text to scan.
	 * @param initialOffset the offset to start scanning from.
	 * @return a new scanner positioned at the given offset.
	 */
	public static RNCScanner createScanner(CharSequence input, int initialOffset) {
		return new RNCScanner(input, initialOffset);
	}

	/**
	 * Scans and returns the next token.
	 *
	 * <p>
	 * After this call, use {@link #getTokenOffset()}, {@link #getTokenEnd()},
	 * {@link #getTokenLength()}, and {@link #getTokenText()} to inspect the
	 * returned token.
	 * </p>
	 *
	 * @return the next token type, or {@link RNCTokenType#EOS} when the input
	 *         is exhausted.
	 */
	public RNCTokenType scan() {
		if (position >= len) {
			tokenOffset = len;
			return finishToken(len, RNCTokenType.EOS);
		}

		int beforePosition = position;
		RNCTokenType type = internalScan();
		// Safety: if internalScan() returned a token but didn't advance the position,
		// force-advance by 1 to avoid an infinite loop.
		if (type != RNCTokenType.EOS && beforePosition == position) {
			position++;
			return finishToken(beforePosition, RNCTokenType.Unknown);
		}
		return type;
	}

	/**
	 * Core scanning logic. Examines the character at the current position and
	 * dispatches to the appropriate token-recognition routine.
	 *
	 * @return the recognized token type.
	 */
	private RNCTokenType internalScan() {
		tokenOffset = position;
		int ch = peekChar();

		// --- Whitespace ---
		// Consume contiguous whitespace as a single token.
		if (isWhitespace(ch)) {
			advanceWhileChar(WHITESPACE_PREDICATE);
			return finishToken(tokenOffset, RNCTokenType.Whitespace);
		}

		// --- Comments ---
		// '#' starts a comment; '##' starts a documentation comment.
		if (ch == '#') {
			advance(1);
			if (peekChar() == '#') {
				// Documentation comment: ## ... (until end of line)
				advance(1);
				advanceUntilEOL();
				return finishToken(tokenOffset, RNCTokenType.DocumentationComment);
			}
			// Regular comment: # ... (until end of line)
			advanceUntilEOL();
			return finishToken(tokenOffset, RNCTokenType.Comment);
		}

		// --- String literals ---
		// Supports single quotes, double quotes, and triple-quoted variants.
		if (ch == '"' || ch == '\'') {
			return scanLiteral(ch);
		}

		// --- Escaped identifiers ---
		// '\' followed by a keyword allows the keyword to be used as an identifier.
		// Use case: \element can be used as a define name when "element" is reserved.
		if (ch == '\\') {
			advance(1);
			if (position < len && IDENTIFIER_START_PREDICATE.test(peekChar())) {
				advance(1);
				advanceWhileChar(IDENTIFIER_PART_PREDICATE);
				return finishToken(tokenOffset, RNCTokenType.Identifier);
			}
			return finishToken(tokenOffset, RNCTokenType.Backslash);
		}

		// --- Identifiers and keywords ---
		// Start with a letter or underscore, continue with letters, digits, '_', '-', '.'.
		// After collecting the word, check if it's a keyword; otherwise it's an Identifier.
		if (IDENTIFIER_START_PREDICATE.test(ch)) {
			advance(1);
			advanceWhileChar(IDENTIFIER_PART_PREDICATE);
			String word = getText(tokenOffset, position);
			RNCTokenType keywordType = resolveKeyword(word);
			return finishToken(tokenOffset, keywordType);
		}

		// --- Operators and delimiters ---
		// Single- or double-character operators.
		switch (ch) {
			case '{':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.BraceOpen);
			case '}':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.BraceClose);
			case '(':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.ParenOpen);
			case ')':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.ParenClose);
			case '[':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.BracketOpen);
			case ']':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.BracketClose);
			case '=':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Assign);
			case '|':
				// '|' alone is Pipe; '|=' is AssignChoice
				advance(1);
				if (peekChar() == '=') {
					advance(1);
					return finishToken(tokenOffset, RNCTokenType.AssignChoice);
				}
				return finishToken(tokenOffset, RNCTokenType.Pipe);
			case '&':
				// '&' alone is Ampersand; '&=' is AssignInterleave
				advance(1);
				if (peekChar() == '=') {
					advance(1);
					return finishToken(tokenOffset, RNCTokenType.AssignInterleave);
				}
				return finishToken(tokenOffset, RNCTokenType.Ampersand);
			case ',':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Comma);
			case '?':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.QuestionMark);
			case '*':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Star);
			case '+':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Plus);
			case '-':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Minus);
			case '~':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Tilde);
			case ':':
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Colon);
			default:
				// Unknown character — advance past it to avoid stalling
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Unknown);
		}
	}

	/**
	 * Scans a string literal (single-quoted, double-quoted, or triple-quoted).
	 *
	 * <p>
	 * Handles:
	 * </p>
	 * <ul>
	 * <li>{@code "hello"} — double-quoted literal</li>
	 * <li>{@code 'hello'} — single-quoted literal</li>
	 * <li>{@code """multi\nline"""} — triple-double-quoted literal</li>
	 * <li>{@code '''multi\nline'''} — triple-single-quoted literal</li>
	 * </ul>
	 *
	 * <p>
	 * Unterminated literals (missing closing quote) are still returned as
	 * {@link RNCTokenType#Literal} to allow error reporting by the validator.
	 * </p>
	 *
	 * @param quoteChar the opening quote character ({@code '"'} or {@code '\''}).
	 * @return {@link RNCTokenType#Literal}.
	 */
	private RNCTokenType scanLiteral(int quoteChar) {
		advance(1);
		// Check for triple-quoted literal (""" or ''')
		if (peekChar() == quoteChar && peekChar(1) == quoteChar) {
			advance(2);
			// Scan until closing triple quote
			while (position < len) {
				int c = peekChar();
				if (c == quoteChar && peekChar(1) == quoteChar && peekChar(2) == quoteChar) {
					advance(3);
					return finishToken(tokenOffset, RNCTokenType.Literal);
				}
				advance(1);
			}
			// Unterminated triple-quoted literal — return what we have
			return finishToken(tokenOffset, RNCTokenType.Literal);
		}
		// Single-quoted literal — scan until closing quote or end of line
		while (position < len) {
			int c = peekChar();
			if (c == quoteChar) {
				advance(1);
				return finishToken(tokenOffset, RNCTokenType.Literal);
			}
			if (c == '\n' || c == '\r') {
				// Unterminated literal at end of line
				return finishToken(tokenOffset, RNCTokenType.Literal);
			}
			advance(1);
		}
		// Unterminated literal at end of stream
		return finishToken(tokenOffset, RNCTokenType.Literal);
	}

	/**
	 * Resolves a word to a keyword token type, or {@link RNCTokenType#Identifier}
	 * if the word is not a recognized keyword.
	 *
	 * <p>
	 * All 19 RNC keywords are recognized context-free (the scanner does not
	 * consider surrounding context). To use a keyword as an identifier, escape
	 * it with a backslash: {@code \element}.
	 * </p>
	 *
	 * @param word the word to resolve.
	 * @return the keyword token type, or {@link RNCTokenType#Identifier}.
	 */
	private static RNCTokenType resolveKeyword(String word) {
		switch (word) {
			case RNCKeywords.NAMESPACE:
				return RNCTokenType.KeywordNamespace;
			case RNCKeywords.DEFAULT:
				return RNCTokenType.KeywordDefault;
			case RNCKeywords.DATATYPES:
				return RNCTokenType.KeywordDatatypes;
			case RNCKeywords.ELEMENT:
				return RNCTokenType.KeywordElement;
			case RNCKeywords.ATTRIBUTE:
				return RNCTokenType.KeywordAttribute;
			case RNCKeywords.LIST:
				return RNCTokenType.KeywordList;
			case RNCKeywords.MIXED:
				return RNCTokenType.KeywordMixed;
			case RNCKeywords.TEXT:
				return RNCTokenType.KeywordText;
			case RNCKeywords.EMPTY:
				return RNCTokenType.KeywordEmpty;
			case RNCKeywords.NOT_ALLOWED:
				return RNCTokenType.KeywordNotAllowed;
			case RNCKeywords.EXTERNAL:
				return RNCTokenType.KeywordExternal;
			case RNCKeywords.GRAMMAR:
				return RNCTokenType.KeywordGrammar;
			case RNCKeywords.START:
				return RNCTokenType.KeywordStart;
			case RNCKeywords.INCLUDE:
				return RNCTokenType.KeywordInclude;
			case RNCKeywords.DIV:
				return RNCTokenType.KeywordDiv;
			case RNCKeywords.PARENT:
				return RNCTokenType.KeywordParent;
			case RNCKeywords.INHERIT:
				return RNCTokenType.KeywordInherit;
			case RNCKeywords.STRING:
				return RNCTokenType.KeywordString;
			case RNCKeywords.TOKEN:
				return RNCTokenType.KeywordToken;
			default:
				return RNCTokenType.Identifier;
		}
	}

	// --- Stream operations (char-by-char, no regex) ---

	/**
	 * Peeks at the character at the current position without advancing.
	 *
	 * @return the code point at the current position, or -1 if at end of input.
	 */
	private int peekChar() {
		return peekChar(0);
	}

	/**
	 * Peeks at a character at an offset from the current position.
	 *
	 * @param offset the look-ahead distance (0 = current position).
	 * @return the code point at the offset position, or -1 if out of bounds.
	 */
	private int peekChar(int offset) {
		int pos = position + offset;
		if (pos >= len) {
			return -1;
		}
		return Character.codePointAt(source, pos);
	}

	/**
	 * Advances the position by {@code n} characters.
	 *
	 * @param n the number of characters to advance.
	 */
	private void advance(int n) {
		position += n;
	}

	/**
	 * Advances the position while the character at the current position
	 * satisfies the given predicate.
	 *
	 * @param condition the predicate to test each character.
	 */
	private void advanceWhileChar(Predicate<Integer> condition) {
		while (position < len && condition.test(peekChar())) {
			position++;
		}
	}

	/**
	 * Advances the position until a newline character ({@code \n} or
	 * {@code \r}) is reached (the newline is not consumed).
	 */
	private void advanceUntilEOL() {
		while (position < len) {
			int c = peekChar();
			if (c == '\n' || c == '\r') {
				return;
			}
			position++;
		}
	}

	/**
	 * Tests whether a character is whitespace.
	 *
	 * @param ch the character code point.
	 * @return {@code true} for space, tab, newline, carriage return, form feed.
	 */
	private static boolean isWhitespace(int ch) {
		return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f';
	}

	/**
	 * Extracts text from the source between the given offsets.
	 *
	 * @param start the start offset (inclusive).
	 * @param end   the end offset (exclusive).
	 * @return the extracted text.
	 */
	private String getText(int start, int end) {
		return StringUtils.getString(source, start, end);
	}

	/**
	 * Records the token type and offset, returning the type.
	 *
	 * @param offset the token's start offset.
	 * @param type   the token type.
	 * @return the token type (for convenient return).
	 */
	private RNCTokenType finishToken(int offset, RNCTokenType type) {
		this.tokenType = type;
		this.tokenOffset = offset;
		return type;
	}

	// --- Public API ---

	/**
	 * Returns the type of the current token (set by the last {@link #scan()} call).
	 *
	 * @return the current token type.
	 */
	public RNCTokenType getTokenType() {
		return tokenType;
	}

	/**
	 * Returns the start offset of the current token in the source text.
	 *
	 * @return the token's start offset (0-based, inclusive).
	 */
	public int getTokenOffset() {
		return tokenOffset;
	}

	/**
	 * Returns the end offset of the current token in the source text.
	 *
	 * @return the token's end offset (0-based, exclusive).
	 */
	public int getTokenEnd() {
		return position;
	}

	/**
	 * Returns the length of the current token (end - offset).
	 *
	 * @return the token length in characters.
	 */
	public int getTokenLength() {
		return position - tokenOffset;
	}

	/**
	 * Returns the text content of the current token.
	 *
	 * <p>
	 * Use case: extracting the identifier name or literal value after scanning.
	 * </p>
	 *
	 * @return the token's text.
	 */
	public String getTokenText() {
		return getText(tokenOffset, position);
	}

	/**
	 * Returns any error message associated with the current token.
	 *
	 * @return the error message, or {@code null} if no error.
	 */
	public String getTokenError() {
		return tokenError;
	}

	/**
	 * Returns the current scanner state.
	 *
	 * @return the scanner state.
	 */
	public RNCScannerState getScannerState() {
		return state;
	}
}
