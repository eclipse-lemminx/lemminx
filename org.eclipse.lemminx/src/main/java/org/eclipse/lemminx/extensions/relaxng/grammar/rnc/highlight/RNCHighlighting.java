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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.highlight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCScanner;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCTokenType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCSearchUtils;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides document highlighting for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Use case: the user places the cursor on a define name like {@code root}
 * and the editor highlights all occurrences of {@code root} in the document —
 * the declaration is highlighted with {@link DocumentHighlightKind#Write} and
 * references with {@link DocumentHighlightKind#Read}.</p>
 *
 * <p>Example: placing the cursor on {@code root} highlights:</p>
 * <pre>
 * start = <b>root</b>            ← Read highlight
 * <b>root</b> = element root {   ← Write highlight (declaration)
 *   text
 * }
 * </pre>
 *
 * @see RNCSearchUtils
 */
public class RNCHighlighting {

	private static final Logger LOGGER = Logger.getLogger(RNCHighlighting.class.getName());

	/**
	 * Finds all occurrences of the define name at the given cursor position,
	 * returning them as document highlights with Write/Read kinds.
	 *
	 * <p>If the cursor is on an element or attribute name instead, only that
	 * single name is highlighted.</p>
	 *
	 * @param rncDocument   the parsed RNC document.
	 * @param position      the cursor position.
	 * @param cancelChecker used to check if the operation has been canceled.
	 * @return a list of {@link DocumentHighlight} entries, or an empty list
	 *         if the word is not a define or element/attribute name.
	 */
	public List<DocumentHighlight> findDocumentHighlights(RNCDocument rncDocument, Position position,
			CancelChecker cancelChecker) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word == null || word.isEmpty()) {
				return Collections.emptyList();
			}

			// Check if cursor is on an element/attribute name first
			List<DocumentHighlight> elementHighlight = highlightElementOrAttributeName(textDocument, text, offset,
					word);
			if (!elementHighlight.isEmpty()) {
				return elementHighlight;
			}

			RNCDefine define = rncDocument.findDefine(word);
			if (define != null) {
				List<DocumentHighlight> highlights = new ArrayList<>();
				// Add all declarations with this name as Write (e.g. tune = and tune |=)
				for (RNCDefine d : rncDocument.getDefines()) {
					if (word.equals(d.getName())) {
						Range r = RNCPositionUtils.createRange(textDocument, d.getNameStart(), d.getNameEnd());
						if (r != null) {
							highlights.add(new DocumentHighlight(r, DocumentHighlightKind.Write));
						}
					}
				}
				// Add all references as Read
				List<Location> locations = RNCSearchUtils.findReferences(textDocument, text, define, false);
				for (Location location : locations) {
					highlights.add(new DocumentHighlight(location.getRange(), DocumentHighlightKind.Read));
				}
				return highlights;
			}

			return Collections.emptyList();
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC document highlights", e);
		}
		return Collections.emptyList();
	}

	private static List<DocumentHighlight> highlightElementOrAttributeName(TextDocument textDocument,
			CharSequence text, int offset, String word) {
		int wordStart = offset;
		while (wordStart > 0 && RNCPositionUtils.isIdentifierChar(Character.codePointAt(text, wordStart - 1))) {
			wordStart--;
		}
		int wordEnd = wordStart + word.length();

		RNCScanner scanner = RNCScanner.createScanner(text);
		RNCTokenType prevMeaningful = null;
		RNCTokenType token = scanner.scan();
		while (token != RNCTokenType.EOS) {
			if (token == RNCTokenType.Identifier) {
				int tokOffset = scanner.getTokenOffset();
				int tokLen = scanner.getTokenLength();
				if (tokOffset == wordStart && tokLen == word.length()) {
					if (prevMeaningful == RNCTokenType.KeywordElement
							|| prevMeaningful == RNCTokenType.KeywordAttribute) {
						Range range = RNCPositionUtils.createRange(textDocument, wordStart, wordEnd);
						if (range != null) {
							return Collections.singletonList(
									new DocumentHighlight(range, DocumentHighlightKind.Read));
						}
					}
					return Collections.emptyList();
				}
			}
			if (token != RNCTokenType.Whitespace && token != RNCTokenType.Comment
					&& token != RNCTokenType.DocumentationComment) {
				prevMeaningful = token;
			}
			token = scanner.scan();
		}
		return Collections.emptyList();
	}
}
