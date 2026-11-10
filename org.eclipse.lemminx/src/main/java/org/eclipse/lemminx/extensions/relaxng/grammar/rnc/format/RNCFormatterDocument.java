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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.format;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCScanner;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCTokenType;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
 * Formats an RNC (RELAX NG Compact Syntax) document by producing a list of
 * {@link TextEdit}s that transform whitespace (indentation, line breaks,
 * spacing around operators) without altering semantic content.
 *
 * <h3>Approach</h3>
 * <p>
 * Unlike XML formatting (which walks a DOM tree), RNC formatting is
 * scanner-based because RNC has no DOM structure. The algorithm:
 * </p>
 * <ol>
 * <li>Scan all tokens using {@link RNCScanner} and collect their types and
 * offsets.</li>
 * <li>Iterate over each token and decide what whitespace should appear
 * before/after it based on the token type and current brace depth.</li>
 * <li>Compare the expected whitespace with the actual whitespace in the
 * document. If they differ, create a {@link TextEdit} to replace the
 * actual whitespace with the expected one.</li>
 * </ol>
 *
 * <h3>Formatting Rules</h3>
 * <ul>
 * <li><b>{@code &#123;} (BraceOpen)</b> — space before, content on new
 * indented line (unless the block is empty {@code &#123; &#125;}).</li>
 * <li><b>{@code &#125;} (BraceClose)</b> — on its own line at the parent
 * indent level.</li>
 * <li><b>{@code =}, {@code |=}, {@code &=} (Assign operators)</b> — space
 * before and space after.</li>
 * <li><b>{@code |} (Pipe), {@code &} (Ampersand)</b> — space before and
 * space after (choice/interleave operators).</li>
 * <li><b>{@code ,} (Comma)</b> — no space before, space after (group
 * operator).</li>
 * <li><b>{@code ?}, {@code *}, {@code +} (Postfix operators)</b> — no
 * space before the operator.</li>
 * <li><b>Top-level definitions</b> — blank line between consecutive
 * definitions at brace depth 0.</li>
 * <li><b>Keywords followed by name</b> ({@code element}, {@code attribute},
 * {@code namespace}, etc.) — space after the keyword.</li>
 * </ul>
 *
 * <p>
 * Use case: the user requests formatting of an {@code .rnc} file via the
 * LSP {@code textDocument/formatting} request.
 * </p>
 *
 * <p>
 * Example — unformatted input:
 * </p>
 *
 * <pre>
 * namespace a="http://example.com"
 * start=element root{attribute id{text},name+}
 * name=element name{text}
 * </pre>
 *
 * <p>
 * Formatted output:
 * </p>
 *
 * <pre>
 * namespace a = "http://example.com"
 *
 * start = element root {
 *   attribute id { text }, name+
 * }
 *
 * name = element name {
 *   text
 * }
 * </pre>
 *
 * @see RNCFormatting
 * @see RNCScanner
 */
public class RNCFormatterDocument {

	private static final Logger LOGGER = Logger.getLogger(RNCFormatterDocument.class.getName());

	/** The document being formatted. */
	private final TextDocument textDocument;

	/** User formatting preferences (tab size, insert spaces, etc.). */
	private final SharedSettings sharedSettings;

	/** RNC-specific formatting settings (blank lines between defines, etc.). */
	private final RNCFormattingSettings rncFormattingSettings;

	/** The line delimiter detected from the document (e.g. "\n" or "\r\n"). */
	private final String lineDelimiter;

	/**
	 * Creates a new formatter for the given RNC document.
	 *
	 * @param textDocument          the RNC document to format.
	 * @param sharedSettings        the user's formatting preferences.
	 * @param rncFormattingSettings the RNC-specific formatting settings, or
	 *                              {@code null} to use defaults.
	 */
	public RNCFormatterDocument(TextDocument textDocument, SharedSettings sharedSettings,
			RNCFormattingSettings rncFormattingSettings) {
		this.textDocument = textDocument;
		this.sharedSettings = sharedSettings;
		this.rncFormattingSettings = rncFormattingSettings != null ? rncFormattingSettings
				: new RNCFormattingSettings();
		this.lineDelimiter = RNCPositionUtils.getLineSeparator(textDocument);
	}

	/**
	 * Formats the document and returns a list of {@link TextEdit}s.
	 *
	 * <p>
	 * The method performs three passes:
	 * </p>
	 * <ol>
	 * <li><b>Token pass</b> — scans all tokens and adjusts whitespace around
	 * operators, braces, keywords, and definitions.</li>
	 * <li><b>Trailing whitespace pass</b> — removes trailing spaces on each
	 * line (if {@code trimTrailingWhitespace} is enabled).</li>
	 * <li><b>Final newline pass</b> — ensures a final newline exists (if
	 * {@code insertFinalNewline} is enabled) and trims excess final
	 * newlines (if {@code trimFinalNewlines} is enabled).</li>
	 * </ol>
	 *
	 * @return a list of text edits. Empty if the document is already
	 *         properly formatted.
	 */
	public List<? extends TextEdit> format() {
		List<TextEdit> edits = new ArrayList<>();
		CharSequence text = textDocument.getTextSequence();
		RNCScanner scanner = RNCScanner.createScanner(text);

		// Track nesting depth for indentation (incremented on '{', decremented on '}')
		int braceDepth = 0;
		boolean isFirstToken = true;
		boolean prevWasNewline = true;
		boolean lastDefinitionWasDeclaration = false;
		boolean inNameClass = false;
		Deque<Boolean> singleLineBlocks = new ArrayDeque<>();
		Deque<Integer> braceExtraIndent = new ArrayDeque<>();

		// Phase 1: Collect all tokens into arrays for random access.
		// This allows looking ahead/behind when deciding formatting for each token.
		List<RNCTokenType> types = new ArrayList<>();
		List<int[]> offsets = new ArrayList<>(); // each entry: [offset, length]

		RNCTokenType token = scanner.scan();
		while (token != RNCTokenType.EOS) {
			types.add(token);
			offsets.add(new int[] { scanner.getTokenOffset(), scanner.getTokenLength() });
			token = scanner.scan();
		}

		// Phase 2: Iterate over tokens and adjust whitespace.
		// For each token, determine what whitespace should appear before/after it.
		for (int i = 0; i < types.size(); i++) {
			RNCTokenType t = types.get(i);
			int tokOffset = offsets.get(i)[0];
			int tokLen = offsets.get(i)[1];

			if (t == RNCTokenType.BraceOpen) {
				inNameClass = false;
				int extra = isPrecededByChoiceOrInterleaveOnOwnLine(i, types, offsets, text)
						? (braceDepth == 0 ? 2 : 1)
						: 0;
				braceExtraIndent.push(extra);
				braceDepth++;
				replaceWhitespaceBefore(i, types, offsets, text, " ", edits);
				RNCTokenType nextMeaningful = getNextMeaningful(i, types);
				if (nextMeaningful != null && nextMeaningful != RNCTokenType.BraceClose) {
					boolean singleLine = hasWhitespaceWithoutNewlineAfter(i, types, offsets, text);
					singleLineBlocks.push(singleLine);
					if (singleLine) {
						replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
					} else {
						String afterBrace = lineDelimiter + getIndent(braceDepth + sumStack(braceExtraIndent));
						replaceWhitespaceAfter(i, types, offsets, text, afterBrace, edits);
					}
				} else if (nextMeaningful == RNCTokenType.BraceClose) {
					singleLineBlocks.push(true);
					replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
				} else {
					singleLineBlocks.push(false);
				}
			} else if (t == RNCTokenType.BraceClose) {
				braceDepth--;
				int poppedExtra = !braceExtraIndent.isEmpty() ? braceExtraIndent.pop() : 0;
				boolean singleLine = !singleLineBlocks.isEmpty() && singleLineBlocks.pop();
				if (singleLine) {
					replaceWhitespaceBefore(i, types, offsets, text, " ", edits);
				} else {
					String expectedWs = lineDelimiter + getIndent(braceDepth + poppedExtra + sumStack(braceExtraIndent));
					replaceWhitespaceBefore(i, types, offsets, text, expectedWs, edits);
				}
			} else if (t == RNCTokenType.Whitespace) {
				// Whitespace tokens are handled implicitly by the surrounding token rules
				// (replaceWhitespaceBefore/After). No explicit action needed.
			} else if (t == RNCTokenType.Comment || t == RNCTokenType.DocumentationComment) {
				if (isOnOwnLine(i, types, offsets, text)
						&& getPrevMeaningful(i, types) != RNCTokenType.BraceOpen) {
					String expectedWs = lineDelimiter + getIndent(braceDepth + sumStack(braceExtraIndent));
					replaceWhitespaceBefore(i, types, offsets, text, expectedWs, edits);
				}
			} else if (isAssign(t)) {
				replaceWhitespaceBefore(i, types, offsets, text, " ", edits);
				if (hasNewlineAfter(i, types, offsets, text)) {
					String afterAssign = lineDelimiter + getIndent(braceDepth + sumStack(braceExtraIndent) + 1);
					replaceWhitespaceAfter(i, types, offsets, text, afterAssign, edits);
				} else {
					replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
				}
			} else if (t == RNCTokenType.Pipe || t == RNCTokenType.Ampersand) {
				if (!inNameClass) {
					if (isOnOwnLine(i, types, offsets, text)) {
						int continuationIndent = Math.max(braceDepth + sumStack(braceExtraIndent), 1);
						String beforePipe = lineDelimiter + getIndent(continuationIndent);
						replaceWhitespaceBefore(i, types, offsets, text, beforePipe, edits);
					} else {
						replaceWhitespaceBefore(i, types, offsets, text, " ", edits);
					}
					replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
				}
			} else if (t == RNCTokenType.Comma) {
				replaceWhitespaceBefore(i, types, offsets, text, "", edits);
				if (hasNewlineAfter(i, types, offsets, text)) {
					int continuationIndent = Math.max(braceDepth + sumStack(braceExtraIndent), 1);
					String afterComma = lineDelimiter + getIndent(continuationIndent);
					replaceWhitespaceAfter(i, types, offsets, text, afterComma, edits);
				} else {
					replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
				}
			} else if (isPostfixOperator(t)) {
				if (!inNameClass) {
					// Postfix cardinality operators (?, *, +): no space before.
					// In name class context (element *), * is a wildcard — keep the space.
					replaceWhitespaceBefore(i, types, offsets, text, "", edits);
				}
			} else if (!inNameClass && isKeywordStartingDefinition(t, i, types)) {
				// Top-level definitions and declarations should start on their own line.
				// At top-level (braceDepth == 0), insert a blank line between definitions
				// for readability. Consecutive declarations (namespace/datatypes) are
				// kept together without blank lines.
				if (getPrevMeaningful(i, types) == RNCTokenType.BraceOpen) {
					// BraceOpen handler already formatted the whitespace after '{'
				} else if (!isFirstNonWhitespace(i, types)) {
					int blankLines = rncFormattingSettings.getBlankLinesBetweenDefines();
					if (braceDepth == 0 && blankLines < 0) {
						// Preserve mode: don't modify blank lines between definitions
					} else {
						int effectiveDepth = braceDepth + sumStack(braceExtraIndent);
						String expectedWs;
						if (braceDepth == 0) {
							if (isDeclarationKeyword(t) && lastDefinitionWasDeclaration) {
								expectedWs = lineDelimiter + getIndent(effectiveDepth);
							} else {
								StringBuilder sb = new StringBuilder();
								for (int b = 0; b <= blankLines; b++) {
									sb.append(lineDelimiter);
								}
								sb.append(getIndent(effectiveDepth));
								expectedWs = sb.toString();
							}
						} else {
							expectedWs = lineDelimiter + getIndent(effectiveDepth);
						}
						replaceWhitespaceBefore(i, types, offsets, text, expectedWs, edits);
					}
				} else if (isOnOwnLine(i, types, offsets, text)) {
					int blankLines = rncFormattingSettings.getBlankLinesBetweenDefines();
					if (!(braceDepth == 0 && blankLines < 0)) {
						int effectiveDepth = braceDepth + sumStack(braceExtraIndent);
						String expectedWs = lineDelimiter + getIndent(effectiveDepth);
						replaceWhitespaceBefore(i, types, offsets, text, expectedWs, edits);
					}
				}
				lastDefinitionWasDeclaration = isDeclarationKeyword(t);
			} else if (!inNameClass && isKeywordFollowedByName(t)) {
				// Keywords that take a name argument need a space after them.
				// Use case: "element  root" → "element root"
				// Use case: "attribute  id" → "attribute id"
				// Use case: "namespace  a" → "namespace a"
				// Skip when next token is '=' — the assign handler adds its own space.
				// Use case: "default namespace = ..." — namespace is not followed by a name.
				RNCTokenType nextMeaningful = getNextMeaningful(i, types);
				if (!isAssign(nextMeaningful)) {
					replaceWhitespaceAfter(i, types, offsets, text, " ", edits);
				}
			} else if (t == RNCTokenType.ParenOpen) {
				// No special spacing for '(' — default behavior is fine.
			} else if (t == RNCTokenType.ParenClose) {
				// No special spacing for ')' — default behavior is fine.
			} else if (t == RNCTokenType.Identifier) {
				// Identifiers: fix indentation if starting a line and followed by '='
				// (i.e. this is a define name).
				// Use case: inside a div block, definition names should be properly indented:
				//   div {
				//     myDefine = text
				//   }
				RNCTokenType next = getNextMeaningful(i, types);
				if (isAssign(next) && isOnOwnLine(i, types, offsets, text)) {
					String expectedWs = lineDelimiter + getIndent(braceDepth + sumStack(braceExtraIndent));
					replaceWhitespaceBefore(i, types, offsets, text, expectedWs, edits);
				}
			}

			// Track name class context: set after processing so the first
			// element/attribute keyword gets its normal formatting rules.
			if (t == RNCTokenType.KeywordElement || t == RNCTokenType.KeywordAttribute) {
				inNameClass = true;
			}
		}

		// Phase 3: Handle trailing whitespace — trim trailing spaces on each line.
		// Use case: "start = root   \n" → "start = root\n"
		if (sharedSettings.getFormattingSettings().isTrimTrailingWhitespace()) {
			trimTrailingWhitespace(text, edits);
		}

		// Phase 3b: Insert a final newline at end of file if missing.
		// Use case: "start = root" → "start = root\n"
		if (sharedSettings.getFormattingSettings().isInsertFinalNewline()) {
			int len = text.length();
			if (len > 0 && text.charAt(len - 1) != '\n') {
				try {
					Position end = textDocument.positionAt(len);
					edits.add(new TextEdit(new Range(end, end), lineDelimiter));
				} catch (BadLocationException e) {
					// ignore
				}
			}
		}

		// Phase 3c: Trim excess final newlines (multiple blank lines at end of file).
		// Use case: "start = root\n\n\n" → "start = root\n"
		if (sharedSettings.getFormattingSettings().isTrimFinalNewlines()) {
			trimFinalNewlines(text, edits);
		}

		return edits;
	}

	/**
	 * Builds the indentation string for the given nesting level.
	 *
	 * <p>
	 * Use case: at brace depth 2 with tab size 2, returns "    " (4 spaces)
	 * or "\t\t" (2 tabs) depending on the user's preference.
	 * </p>
	 *
	 * @param level the nesting depth (0 = no indent, 1 = one indent level, etc.).
	 * @return the indentation string.
	 */
	private String getIndent(int level) {
		StringBuilder sb = new StringBuilder();
		String singleIndent = getOneIndent();
		for (int i = 0; i < level; i++) {
			sb.append(singleIndent);
		}
		return sb.toString();
	}

	/**
	 * Returns the string for a single indentation level, based on the user's
	 * settings (spaces or tab).
	 *
	 * @return "  " (spaces, tab-size wide) or "\t" (tab character).
	 */
	private String getOneIndent() {
		if (sharedSettings.getFormattingSettings().isInsertSpaces()) {
			int tabSize = sharedSettings.getFormattingSettings().getTabSize();
			StringBuilder sb = new StringBuilder(tabSize);
			for (int i = 0; i < tabSize; i++) {
				sb.append(' ');
			}
			return sb.toString();
		}
		return "\t";
	}

	/**
	 * Replaces the whitespace immediately before the token at {@code tokenIndex}
	 * with the {@code expected} whitespace string. If the previous token is a
	 * whitespace token and its content matches, no edit is created.
	 *
	 * <p>
	 * Use case: ensure a space before '{' — if the previous token is an
	 * identifier with no whitespace between them (e.g. "root{"), this method
	 * inserts " " at the boundary.
	 * </p>
	 *
	 * @param tokenIndex the index of the current token in the token list.
	 * @param types      the list of all token types.
	 * @param offsets     the list of all token offsets ([offset, length]).
	 * @param text       the document text.
	 * @param expected   the expected whitespace (e.g. " ", "\n  ", "").
	 * @param edits      the list to add any resulting edit to.
	 */
	private void replaceWhitespaceBefore(int tokenIndex, List<RNCTokenType> types, List<int[]> offsets,
			CharSequence text, String expected, List<TextEdit> edits) {
		if (tokenIndex <= 0) {
			return;
		}
		int prevIndex = tokenIndex - 1;
		if (types.get(prevIndex) == RNCTokenType.Whitespace) {
			// Previous token is whitespace — compare its content with expected
			int wsOffset = offsets.get(prevIndex)[0];
			int wsLen = offsets.get(prevIndex)[1];
			String actual = text.subSequence(wsOffset, wsOffset + wsLen).toString();
			if (!actual.equals(expected)) {
				createTextEdit(wsOffset, wsOffset + wsLen, expected, edits);
			}
		} else if (!expected.isEmpty()) {
			// No whitespace token exists, but we need to insert whitespace
			int insertOffset = offsets.get(prevIndex)[0] + offsets.get(prevIndex)[1];
			createTextEdit(insertOffset, insertOffset, expected, edits);
		}
	}

	/**
	 * Replaces the whitespace immediately after the token at {@code tokenIndex}
	 * with the {@code expected} whitespace string. If the next token is a
	 * whitespace token and its content matches, no edit is created.
	 *
	 * <p>
	 * Use case: ensure a space after '=' — if the next token is a keyword with
	 * no whitespace between them (e.g. "=element"), this method inserts " "
	 * at the boundary.
	 * </p>
	 *
	 * @param tokenIndex the index of the current token in the token list.
	 * @param types      the list of all token types.
	 * @param offsets     the list of all token offsets ([offset, length]).
	 * @param text       the document text.
	 * @param expected   the expected whitespace (e.g. " ", "\n  ", "").
	 * @param edits      the list to add any resulting edit to.
	 */
	private void replaceWhitespaceAfter(int tokenIndex, List<RNCTokenType> types, List<int[]> offsets,
			CharSequence text, String expected, List<TextEdit> edits) {
		if (tokenIndex + 1 >= types.size()) {
			return;
		}
		int nextIndex = tokenIndex + 1;
		if (types.get(nextIndex) == RNCTokenType.Whitespace) {
			// Next token is whitespace — compare its content with expected
			int wsOffset = offsets.get(nextIndex)[0];
			int wsLen = offsets.get(nextIndex)[1];
			String actual = text.subSequence(wsOffset, wsOffset + wsLen).toString();
			if (!actual.equals(expected)) {
				createTextEdit(wsOffset, wsOffset + wsLen, expected, edits);
			}
		} else if (!expected.isEmpty()) {
			// No whitespace token exists, but we need to insert whitespace
			int insertOffset = offsets.get(tokenIndex)[0] + offsets.get(tokenIndex)[1];
			createTextEdit(insertOffset, insertOffset, expected, edits);
		}
	}

	/**
	 * Creates a {@link TextEdit} that replaces the range [{@code from},
	 * {@code to}) with the given replacement string.
	 *
	 * @param from        the start offset in the document.
	 * @param to          the end offset in the document (exclusive).
	 * @param replacement the replacement text.
	 * @param edits       the list to add the edit to.
	 */
	private void createTextEdit(int from, int to, String replacement, List<TextEdit> edits) {
		try {
			Range range = new Range(textDocument.positionAt(from), textDocument.positionAt(to));
			edits.add(new TextEdit(range, replacement));
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error creating text edit", e);
		}
	}

	/**
	 * Checks whether the token at {@code tokenIndex} starts on its own line
	 * (i.e. the preceding whitespace contains a newline character).
	 *
	 * <p>
	 * Use case: determines whether a comment or identifier needs indentation
	 * adjustment. If a token is on the same line as the previous token, its
	 * indentation is not modified independently.
	 * </p>
	 *
	 * @param tokenIndex the index of the token to check.
	 * @param types      the list of all token types.
	 * @param offsets     the list of all token offsets.
	 * @param text       the document text.
	 * @return {@code true} if the token starts on its own line.
	 */
	private boolean isOnOwnLine(int tokenIndex, List<RNCTokenType> types, List<int[]> offsets, CharSequence text) {
		if (tokenIndex <= 0) {
			// First token is always on its own line
			return true;
		}
		int prevIndex = tokenIndex - 1;
		if (types.get(prevIndex) == RNCTokenType.Whitespace) {
			int wsOffset = offsets.get(prevIndex)[0];
			int wsLen = offsets.get(prevIndex)[1];
			for (int i = wsOffset; i < wsOffset + wsLen; i++) {
				if (text.charAt(i) == '\n') {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Checks whether the token at {@code tokenIndex} is the first non-whitespace
	 * token in the document.
	 *
	 * <p>
	 * Use case: the first definition in the file should not have a blank line
	 * before it, even though subsequent definitions do.
	 * </p>
	 *
	 * @param tokenIndex the index of the token to check.
	 * @param types      the list of all token types.
	 * @return {@code true} if all tokens before this one are whitespace.
	 */
	private boolean isFirstNonWhitespace(int tokenIndex, List<RNCTokenType> types) {
		for (int i = 0; i < tokenIndex; i++) {
			RNCTokenType t = types.get(i);
			if (t != RNCTokenType.Whitespace && t != RNCTokenType.Comment
					&& t != RNCTokenType.DocumentationComment) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the next non-whitespace token type after {@code tokenIndex},
	 * or {@code null} if there are no more meaningful tokens.
	 *
	 * <p>
	 * Use case: when processing '{', look ahead to check if the next
	 * meaningful token is '}' (empty block) to avoid inserting a newline
	 * inside an empty block.
	 * </p>
	 *
	 * @param tokenIndex the starting index (exclusive).
	 * @param types      the list of all token types.
	 * @return the next meaningful token type, or {@code null}.
	 */
	private RNCTokenType getNextMeaningful(int tokenIndex, List<RNCTokenType> types) {
		for (int i = tokenIndex + 1; i < types.size(); i++) {
			if (types.get(i) != RNCTokenType.Whitespace) {
				return types.get(i);
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} if the token type is an assignment operator
	 * ({@code =}, {@code |=}, or {@code &=}).
	 *
	 * @param t the token type to test.
	 * @return {@code true} for assignment operator types.
	 */
	private static boolean isAssign(RNCTokenType t) {
		return t == RNCTokenType.Assign || t == RNCTokenType.AssignChoice || t == RNCTokenType.AssignInterleave;
	}

	/**
	 * Returns {@code true} if the token type is a postfix cardinality
	 * operator ({@code ?}, {@code *}, or {@code +}).
	 *
	 * <p>
	 * Use case: postfix operators attach directly to their operand without
	 * a space: {@code name+}, {@code text*}, {@code item?}.
	 * </p>
	 *
	 * @param t the token type to test.
	 * @return {@code true} for postfix operator types.
	 */
	private static boolean isDeclarationKeyword(RNCTokenType t) {
		return t == RNCTokenType.KeywordNamespace || t == RNCTokenType.KeywordDefault
				|| t == RNCTokenType.KeywordDatatypes;
	}

	private static boolean isPostfixOperator(RNCTokenType t) {
		return t == RNCTokenType.QuestionMark || t == RNCTokenType.Star || t == RNCTokenType.Plus;
	}

	/**
	 * Returns {@code true} if the token type is a keyword that takes a name
	 * argument immediately after it.
	 *
	 * <p>
	 * Use case: ensures a single space between the keyword and its name:
	 * {@code element root}, {@code attribute id}, {@code namespace a},
	 * {@code datatypes xsd}.
	 * </p>
	 *
	 * @param t the token type to test.
	 * @return {@code true} for keywords that should be followed by a space + name.
	 */
	private static boolean isKeywordFollowedByName(RNCTokenType t) {
		return t == RNCTokenType.KeywordElement || t == RNCTokenType.KeywordAttribute
				|| t == RNCTokenType.KeywordNamespace || t == RNCTokenType.KeywordDatatypes
				|| t == RNCTokenType.KeywordDefault;
	}

	/**
	 * Returns {@code true} if the token at {@code index} starts a new
	 * definition or declaration (something that should be separated from
	 * the previous definition by a blank line at top level).
	 *
	 * <p>
	 * Matches:
	 * </p>
	 * <ul>
	 * <li>Keywords: {@code namespace}, {@code default}, {@code datatypes},
	 * {@code start}, {@code include}, {@code div}.</li>
	 * <li>Identifiers followed by {@code =}, {@code |=}, or {@code &=}
	 * (define statements like {@code root = element root { text }}).</li>
	 * </ul>
	 *
	 * <p>
	 * Excludes identifiers that are namespace/datatypes prefixes (e.g.
	 * {@code a} in {@code namespace a = "..."}) — these are NOT definitions
	 * and should stay on the same line as the keyword.
	 * </p>
	 *
	 * @param t     the token type.
	 * @param index the token index.
	 * @param types the list of all token types.
	 * @return {@code true} if this token starts a definition/declaration.
	 */
	private boolean isKeywordStartingDefinition(RNCTokenType t, int index, List<RNCTokenType> types) {
		if (t == RNCTokenType.KeywordDefault
				|| t == RNCTokenType.KeywordDatatypes || t == RNCTokenType.KeywordStart
				|| t == RNCTokenType.KeywordInclude || t == RNCTokenType.KeywordDiv) {
			return true;
		}
		if (t == RNCTokenType.KeywordNamespace) {
			RNCTokenType prev = getPrevMeaningful(index, types);
			return prev != RNCTokenType.KeywordDefault;
		}
		// An identifier followed by '=' is a definition, unless preceded by
		// namespace/datatypes/default (in which case it's a prefix, not a define name).
		// Use case: "root = text" → identifier 'root' starts a definition.
		// Counter-example: "namespace a = ..." → identifier 'a' is a prefix, NOT a definition.
		if (t == RNCTokenType.Identifier) {
			RNCTokenType next = getNextMeaningful(index, types);
			if (isAssign(next)) {
				RNCTokenType prev = getPrevMeaningful(index, types);
				return prev != RNCTokenType.KeywordNamespace && prev != RNCTokenType.KeywordDatatypes
						&& prev != RNCTokenType.KeywordDefault;
			}
		}
		return false;
	}

	/**
	 * Returns the previous non-whitespace token type before {@code tokenIndex},
	 * or {@code null} if there are no preceding meaningful tokens.
	 *
	 * <p>
	 * Use case: when checking whether an identifier followed by '=' is a
	 * definition or a namespace prefix, look back to see if the previous
	 * meaningful token was {@code namespace} or {@code datatypes}.
	 * </p>
	 *
	 * @param tokenIndex the starting index (exclusive).
	 * @param types      the list of all token types.
	 * @return the previous meaningful token type, or {@code null}.
	 */
	private boolean hasWhitespaceWithoutNewlineAfter(int tokenIndex, List<RNCTokenType> types, List<int[]> offsets,
			CharSequence text) {
		if (tokenIndex + 1 >= types.size()) {
			return false;
		}
		if (types.get(tokenIndex + 1) != RNCTokenType.Whitespace) {
			return false;
		}
		int wsOffset = offsets.get(tokenIndex + 1)[0];
		int wsLen = offsets.get(tokenIndex + 1)[1];
		for (int i = wsOffset; i < wsOffset + wsLen; i++) {
			if (text.charAt(i) == '\n') {
				return false;
			}
		}
		return true;
	}

	private boolean hasNewlineAfter(int tokenIndex, List<RNCTokenType> types, List<int[]> offsets,
			CharSequence text) {
		if (tokenIndex + 1 >= types.size()) {
			return false;
		}
		if (types.get(tokenIndex + 1) != RNCTokenType.Whitespace) {
			return false;
		}
		int wsOffset = offsets.get(tokenIndex + 1)[0];
		int wsLen = offsets.get(tokenIndex + 1)[1];
		for (int i = wsOffset; i < wsOffset + wsLen; i++) {
			if (text.charAt(i) == '\n') {
				return true;
			}
		}
		return false;
	}

	private RNCTokenType getPrevMeaningful(int tokenIndex, List<RNCTokenType> types) {
		for (int i = tokenIndex - 1; i >= 0; i--) {
			if (types.get(i) != RNCTokenType.Whitespace) {
				return types.get(i);
			}
		}
		return null;
	}

	private boolean isPrecededByChoiceOrInterleaveOnOwnLine(int braceOpenIndex, List<RNCTokenType> types,
			List<int[]> offsets, CharSequence text) {
		for (int i = braceOpenIndex - 1; i >= 0; i--) {
			RNCTokenType t = types.get(i);
			if (t == RNCTokenType.Pipe || t == RNCTokenType.Ampersand) {
				return isOnOwnLine(i, types, offsets, text);
			}
			if (t == RNCTokenType.Whitespace) {
				int wsOffset = offsets.get(i)[0];
				int wsLen = offsets.get(i)[1];
				for (int j = wsOffset; j < wsOffset + wsLen; j++) {
					if (text.charAt(j) == '\n') {
						return false;
					}
				}
			}
		}
		return false;
	}

	private static int sumStack(Deque<Integer> stack) {
		int sum = 0;
		for (int v : stack) {
			sum += v;
		}
		return sum;
	}

	/**
	 * Removes trailing whitespace (spaces and tabs) at the end of each line.
	 *
	 * <p>
	 * Use case: the user has {@code trimTrailingWhitespace} enabled and the
	 * document contains lines like {@code "start = root   "} — the trailing
	 * spaces are removed.
	 * </p>
	 *
	 * @param text  the document text.
	 * @param edits the list to add any resulting edits to.
	 */
	private void trimTrailingWhitespace(CharSequence text, List<TextEdit> edits) {
		int len = text.length();
		int i = 0;
		while (i < len) {
			// Find end of line (position of newline character)
			int lineEnd = i;
			while (lineEnd < len && text.charAt(lineEnd) != '\n' && text.charAt(lineEnd) != '\r') {
				lineEnd++;
			}
			// Scan backwards to find last non-space character on this line
			int lastNonSpace = lineEnd;
			while (lastNonSpace > i && Character.isWhitespace(text.charAt(lastNonSpace - 1))
					&& text.charAt(lastNonSpace - 1) != '\n' && text.charAt(lastNonSpace - 1) != '\r') {
				lastNonSpace--;
			}
			if (lastNonSpace < lineEnd) {
				// Trailing whitespace found — create edit to remove it
				createTextEdit(lastNonSpace, lineEnd, "", edits);
			}
			// Skip line ending (handle both \r\n and \n)
			if (lineEnd < len) {
				if (text.charAt(lineEnd) == '\r' && lineEnd + 1 < len && text.charAt(lineEnd + 1) == '\n') {
					i = lineEnd + 2;
				} else {
					i = lineEnd + 1;
				}
			} else {
				break;
			}
		}
	}

	/**
	 * Trims excess final newlines at the end of the document, keeping at most
	 * one final newline (if {@code insertFinalNewline} is enabled).
	 *
	 * <p>
	 * Use case: the document ends with three blank lines
	 * ({@code "start = root\n\n\n"}) — this method trims it to
	 * {@code "start = root\n"}.
	 * </p>
	 *
	 * @param text  the document text.
	 * @param edits the list to add any resulting edits to.
	 */
	private void trimFinalNewlines(CharSequence text, List<TextEdit> edits) {
		int len = text.length();
		if (len == 0) {
			return;
		}
		// Find the position of the last non-newline character
		int end = len;
		while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
			end--;
		}
		// Keep one final newline if insertFinalNewline is true
		if (sharedSettings.getFormattingSettings().isInsertFinalNewline() && end < len) {
			// Skip past the first preserved newline (could be \r\n or \n)
			int keepEnd = end;
			if (keepEnd < len && text.charAt(keepEnd) == '\r') {
				keepEnd++;
			}
			if (keepEnd < len && text.charAt(keepEnd) == '\n') {
				keepEnd++;
			}
			if (keepEnd < len) {
				createTextEdit(keepEnd, len, "", edits);
			}
		} else if (end < len - 1) {
			// Trim all but the last newline
			int keepEnd = end;
			if (keepEnd < len && text.charAt(keepEnd) == '\r') {
				keepEnd++;
			}
			if (keepEnd < len && text.charAt(keepEnd) == '\n') {
				keepEnd++;
			}
			if (keepEnd < len) {
				createTextEdit(keepEnd, len, "", edits);
			}
		}
	}
}
