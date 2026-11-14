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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCFileReferenceNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCRange;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Utility methods for computing positions and ranges in RNC documents.
 *
 * <p>Use case: when Jing reports an error at a character offset, these methods
 * locate the corresponding token (identifier, string literal, etc.) to produce
 * a precise diagnostic range that highlights the relevant word rather than a
 * single character.</p>
 *
 * <p>Example: Jing reports "reference to undefined pattern" at offset 42.
 * {@link #selectWord} finds the identifier boundaries around offset 42 and
 * returns a range covering {@code "myPattern"} (columns 8–17).</p>
 */
public class RNCPositionUtils {

	public static final Range ZERO_RANGE = new Range(new Position(0, 0), new Position(0, 0));

	private RNCPositionUtils() {
	}

	/**
	 * Selects the range of a known word near the given offset.
	 *
	 * <p>Use case: Jing reports an error mentioning a specific identifier
	 * (e.g., "undefined pattern \"foo\""). This method searches for
	 * {@code "foo"} near the error offset and returns its exact range
	 * for the diagnostic highlight.</p>
	 *
	 * @param document the text document for offset-to-position conversion.
	 * @param text     the document text.
	 * @param offset   the approximate character offset of the error.
	 * @param word     the word to search for near the offset.
	 * @return the range covering the word, or a fallback token range.
	 * @throws BadLocationException if offset conversion fails.
	 */
	public static Range selectWord(TextDocument document, String text, int offset, String word)
			throws BadLocationException {
		int searchStart = Math.max(0, offset - word.length() - 1);
		int searchEnd = Math.min(text.length(), offset + word.length());
		int idx = text.indexOf(word, searchStart);
		if (idx >= 0 && idx < searchEnd) {
			return new Range(document.positionAt(idx), document.positionAt(idx + word.length()));
		}
		return selectToken(document, text, offset);
	}

	/**
	 * Selects the token (identifier or string literal) at the given offset.
	 *
	 * <p>Use case: when no specific word is known, this method finds whatever
	 * token is at the error offset — an identifier, a quoted string, or a
	 * zero-width range if the offset is on whitespace.</p>
	 *
	 * @param document the text document for offset-to-position conversion.
	 * @param text     the document text.
	 * @param offset   the character offset to inspect.
	 * @return the range covering the token at the offset.
	 * @throws BadLocationException if offset conversion fails.
	 */
	public static Range selectToken(TextDocument document, String text, int offset) throws BadLocationException {
		int len = text.length();
		if (offset >= len) {
			offset = len > 0 ? len - 1 : 0;
		}

		int pos = offset;
		while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}

		int end = pos + 1;
		int start = pos;
		char ch = pos < len ? text.charAt(pos) : 0;

		if (isIdentifierChar(ch)) {
			while (start > 0 && isIdentifierChar(text.charAt(start - 1))) {
				start--;
			}
			while (end < len && isIdentifierChar(text.charAt(end))) {
				end++;
			}
		} else if (ch == '"') {
			if (pos > 0) {
				start = pos - 1;
				while (start > 0 && text.charAt(start) != '"') {
					start--;
				}
				end = pos + 1;
			}
		}

		if (start == end) {
			Position p = document.positionAt(offset);
			return new Range(p, p);
		}
		return new Range(document.positionAt(start), document.positionAt(end));
	}

	/**
	 * Selects the element/attribute name that precedes a {@code { }} block
	 * ending at or near the given offset.
	 *
	 * <p>Use case: Jing reports "missing-pattern" at the end of an empty
	 * block {@code element root { }}. This method scans backwards past the
	 * braces to select {@code "root"} for the diagnostic range.</p>
	 *
	 * @param document the text document for offset-to-position conversion.
	 * @param text     the document text.
	 * @param offset   the character offset at or after the closing brace.
	 * @return the range covering the name, or {@code null} if not found.
	 * @throws BadLocationException if offset conversion fails.
	 */
	public static Range selectNameBeforeBraces(TextDocument document, String text, int offset)
			throws BadLocationException {
		int len = text.length();
		int pos = offset;
		if (pos >= len) {
			pos = len - 1;
		}
		// Skip to '}'
		while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}
		if (pos < 0 || text.charAt(pos) != '}') {
			return null;
		}
		// Skip to '{'
		pos--;
		while (pos > 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}
		if (pos < 0 || text.charAt(pos) != '{') {
			return null;
		}
		// Skip whitespace before '{'
		pos--;
		while (pos >= 0 && Character.isWhitespace(text.charAt(pos))) {
			pos--;
		}
		if (pos < 0 || !isIdentifierChar(text.charAt(pos))) {
			return null;
		}
		// Find the name boundaries
		int nameEnd = pos + 1;
		while (pos > 0 && isIdentifierChar(text.charAt(pos - 1))) {
			pos--;
		}
		return new Range(document.positionAt(pos), document.positionAt(nameEnd));
	}

	/**
	 * Computes the LSP {@link Position} at the very end of the document.
	 *
	 * <p>Use case: code actions that append content (e.g., "Create define")
	 * need the end-of-document position to insert text at the bottom of
	 * the file.</p>
	 *
	 * @param source the document text as a CharSequence (avoids toString).
	 * @return the position after the last character in the document.
	 */
	public static Position getEndOfDocumentPosition(CharSequence source) {
		int len = source.length();
		int lastLine = 0;
		int lastLineStart = 0;
		for (int i = 0; i < len; i++) {
			if (source.charAt(i) == '\n') {
				lastLine++;
				lastLineStart = i + 1;
			}
		}
		boolean endsWithNewline = len > 0 && source.charAt(len - 1) == '\n';
		return new Position(lastLine, endsWithNewline ? 0 : len - lastLineStart);
	}

	/**
	 * Returns whether the document ends with a newline character.
	 *
	 * <p>Use case: code actions that append content need to know whether
	 * to prepend a newline before the inserted text.</p>
	 *
	 * @param source the document text.
	 * @return {@code true} if the last character is {@code '\n'}.
	 */
	public static boolean endsWithNewline(CharSequence source) {
		return source.length() > 0 && source.charAt(source.length() - 1) == '\n';
	}

	/**
	 * Returns the line separator used in the document, falling back to
	 * {@link System#lineSeparator()} if the document is empty.
	 */
	public static String getLineSeparator(TextDocument textDocument) {
		try {
			return textDocument.lineDelimiter(0);
		} catch (BadLocationException e) {
			return System.lineSeparator();
		}
	}

	/**
	 * Returns the range of the URI string content (without quotes) inside
	 * an include or external reference node.
	 *
	 * <p>Use case: document links and definition targets need the range of
	 * the URI value (excluding the surrounding quotes) for clickable navigation.</p>
	 *
	 * @param document the text document for offset-to-position conversion.
	 * @param node     the include or external reference node.
	 * @return the range of the URI content, or {@code null} if conversion fails.
	 */
	public static Range selectUri(TextDocument document, RNCFileReferenceNode node) {
		return createRange(document, node.getUriStart() + 1, node.getUriEnd() - 1);
	}

	/**
	 * Creates an LSP {@link Range} from character offsets, or {@code null}
	 * if the offsets are invalid.
	 *
	 * @param document    the text document for offset-to-position conversion.
	 * @param startOffset the inclusive start offset.
	 * @param endOffset   the exclusive end offset.
	 * @return the range, or {@code null} if conversion fails.
	 */
	public static Range createRange(TextDocument document, int startOffset, int endOffset) {
		try {
			return new Range(document.positionAt(startOffset), document.positionAt(endOffset));
		} catch (BadLocationException e) {
			return null;
		}
	}

	/**
	 * Creates an LSP {@link Range} from an {@link RNCRange} (parser error range).
	 *
	 * @param range the parser-reported range with start/end offsets and
	 *              a reference to the owner document.
	 * @return the LSP range, or {@code null} if conversion fails.
	 */
	public static Range createRange(RNCRange range) {
		return createRange(range.getOwnerDocument().getTextDocument(), range.getStart(), range.getEnd());
	}

	/**
	 * Returns the identifier word at the given offset, or {@code null} if
	 * the offset is not on an identifier character.
	 *
	 * <p>Use case: hover, definition, and references need to extract the
	 * word under the cursor to look up defines and keywords.</p>
	 *
	 * @param text   the document text.
	 * @param offset the character offset to inspect.
	 * @return the word at the offset, or {@code null} if none found.
	 */
	public static String getWordAt(CharSequence text, int offset) {
		int start = StringUtils.findStartWord(text, offset, RNCPositionUtils::isIdentifierCharPredicate);
		if (start == -1) {
			start = offset;
		}
		int end = StringUtils.findEndWord(text, offset, RNCPositionUtils::isIdentifierCharPredicate);
		if (end == -1) {
			end = offset;
		}
		if (start == end) {
			return null;
		}
		return text.subSequence(start, end).toString();
	}

	/**
	 * Returns the LSP range of the identifier word at the given offset.
	 *
	 * <p>Use case: document highlighting and rename need the word range to
	 * know which text span to highlight or replace.</p>
	 *
	 * @param textDocument the text document.
	 * @param offset       the character offset within the word.
	 * @param word         unused (kept for API consistency).
	 * @return the range of the word, or {@code null} if not on an identifier.
	 */
	public static Range getWordRange(TextDocument textDocument, int offset, String word) {
		return textDocument.getWordRangeAt(offset, RNCPositionUtils::isIdentifierCharPredicate);
	}

	private static boolean isIdentifierCharPredicate(Character ch) {
		return isIdentifierChar(ch);
	}

	/**
	 * Returns whether the character is a valid RNC identifier character
	 * (letter, digit, underscore, hyphen, or dot).
	 *
	 * @param ch the character to test.
	 * @return {@code true} if the character can appear in an RNC identifier.
	 */
	public static boolean isIdentifierChar(int ch) {
		return ch == ':' || ch == '_' || ch == '-' || ch == '.' || Character.isLetterOrDigit(ch);
	}
}
