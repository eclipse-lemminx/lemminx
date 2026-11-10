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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.completion;

import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCScanner;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCTokenType;

/**
 * Determines the completion context at a given cursor position in an RNC
 * document by scanning all tokens before the cursor.
 *
 * <p>Use case: {@link RNCCompletions} calls {@link #getCompletionContext(CharSequence, int)}
 * to decide which set of completion items to offer — top-level declarations,
 * pattern keywords, namespace URIs, file paths, etc.</p>
 */
public class RNCCompletionContext {

	/**
	 * Describes the context in which completion was triggered.
	 *
	 * <p>The context determines which set of completion items to offer.</p>
	 */
	public enum CompletionContextType {
		/** Cursor is at the top level, outside any braces. */
		TOP_LEVEL,
		/** Cursor follows an assignment operator ({@code =}, {@code |=}, {@code &=}). */
		AFTER_ASSIGN,
		/** Cursor follows {@code start =}. Excludes attribute from completions. */
		AFTER_START_ASSIGN,
		/** Cursor is inside a pattern (within braces, or after a combining operator). */
		IN_PATTERN,
		/** Cursor follows {@code =} inside a {@code namespace} declaration. */
		AFTER_NAMESPACE_ASSIGN,
		/** Cursor follows {@code =} inside a {@code datatypes} declaration. */
		AFTER_DATATYPES_ASSIGN,
		/** Cursor follows the {@code include} keyword. */
		AFTER_INCLUDE,
		/** Cursor is inside the string literal after {@code include}. */
		IN_INCLUDE_LITERAL,
		/** Cursor follows the {@code external} keyword. */
		AFTER_EXTERNAL,
		/** Cursor is inside the string literal after {@code external}. */
		IN_EXTERNAL_LITERAL,
		/** Cursor follows the {@code element} keyword (expecting a name). */
		AFTER_ELEMENT_KEYWORD,
		/** Cursor follows the {@code attribute} keyword (expecting a name). */
		AFTER_ATTRIBUTE_KEYWORD,
		/** Cursor is inside a comment. */
		IN_COMMENT,
		/** Fallback when the context cannot be determined. */
		UNKNOWN
	}

	/**
	 * Determines the completion context by scanning all tokens before the cursor.
	 *
	 * <p>Implements a state machine that tracks the last meaningful token,
	 * whether we are inside a {@code namespace} or {@code datatypes} declaration,
	 * and the current brace nesting depth.</p>
	 *
	 * @param text   the full document text.
	 * @param offset the cursor offset.
	 * @return the detected {@link CompletionContextType}.
	 */
	public static CompletionContextType getCompletionContext(CharSequence text, int offset) {
		RNCScanner scanner = RNCScanner.createScanner(text);

		// Last non-whitespace, non-comment token before the cursor.
		RNCTokenType lastMeaningful = null;
		// The token before lastMeaningful (used when the cursor is mid-identifier).
		RNCTokenType prevMeaningful = null;
		// End offset of lastMeaningful (to detect if cursor is inside it).
		int lastMeaningfulEnd = -1;

		// true while scanning tokens inside "namespace prefix = ..."
		// Use case: "namespace ns = |" → AFTER_NAMESPACE_ASSIGN
		boolean inNamespaceDecl = false;

		// true while scanning tokens inside "datatypes prefix = ..."
		// Use case: "datatypes xsd = |" → AFTER_DATATYPES_ASSIGN
		boolean inDatatypesDecl = false;

		// Set to the keyword token when scanning "include" or "external",
		// to distinguish include vs external file path contexts.
		// Use case: "include |" → AFTER_INCLUDE, "external |" → AFTER_EXTERNAL
		RNCTokenType afterFileRefKeyword = null;

		// Current brace nesting depth ({...}).
		// Use case: depth > 0 means we are inside a block body.
		int braceDepth = 0;

		// Number of brace levels that are declaration blocks (div/grammar bodies),
		// as opposed to pattern blocks (element/attribute bodies).
		// Use case: "div { | }" → TOP_LEVEL (not IN_PATTERN) because declaration
		// blocks contain top-level declarations, not patterns.
		int declarationBraceDepth = 0;

		// The token that preceded the last assignment operator (=, |=, &=).
		// Use case: "start = |" → beforeAssign is KeywordStart → AFTER_START_ASSIGN
		// vs "content = |" → beforeAssign is Identifier → AFTER_ASSIGN
		RNCTokenType beforeAssign = null;

		// ---- Phase 1: scan all tokens before the cursor ----

		RNCTokenType token = scanner.scan();
		while (token != RNCTokenType.EOS && scanner.getTokenOffset() < offset) {

			// Use case: "# comment|" → cursor is inside a comment, no completions
			if (token == RNCTokenType.Comment || token == RNCTokenType.DocumentationComment) {
				int tokenEnd = scanner.getTokenOffset() + scanner.getTokenLength();
				if (offset <= tokenEnd) {
					return CompletionContextType.IN_COMMENT;
				}
			}

			// Only track meaningful tokens (skip whitespace and comments)
			if (token != RNCTokenType.Whitespace && token != RNCTokenType.Comment
					&& token != RNCTokenType.DocumentationComment) {
				prevMeaningful = lastMeaningful;
				lastMeaningful = token;
				lastMeaningfulEnd = scanner.getTokenOffset() + scanner.getTokenLength();

				switch (token) {
					// Use case: "namespace |" → track that we're in a namespace declaration
					case KeywordNamespace:
						inNamespaceDecl = true;
						inDatatypesDecl = false;
						afterFileRefKeyword = null;
						break;

					// Use case: "datatypes |" → track that we're in a datatypes declaration
					case KeywordDatatypes:
						inDatatypesDecl = true;
						inNamespaceDecl = false;
						afterFileRefKeyword = null;
						break;

					// Use case: "include |" or "external |" → remember which keyword for
					// file path completion
					case KeywordInclude:
					case KeywordExternal:
						afterFileRefKeyword = token;
						inNamespaceDecl = false;
						inDatatypesDecl = false;
						break;

					// Use case: "namespace ns = 'http://...' |" → literal ends the
					// namespace/datatypes declaration
					case Literal:
						inNamespaceDecl = false;
						inDatatypesDecl = false;
						break;

					// Use case: "element root {|" → entering a pattern block (braceDepth++)
					// Use case: "div {|" → entering a declaration block (declarationBraceDepth++)
					case BraceOpen:
						braceDepth++;
						if (prevMeaningful == RNCTokenType.KeywordDiv
								|| prevMeaningful == RNCTokenType.KeywordGrammar) {
							declarationBraceDepth++;
						}
						inNamespaceDecl = false;
						inDatatypesDecl = false;
						afterFileRefKeyword = null;
						break;

					// Use case: "element root { text }|" → leaving a block (braceDepth--)
					case BraceClose:
						if (declarationBraceDepth >= braceDepth) {
							declarationBraceDepth--;
						}
						braceDepth--;
						afterFileRefKeyword = null;
						break;

					// Use case: "start = |" → remember what preceded the assignment
					// to distinguish start-assign from define-assign
					case Assign:
					case AssignChoice:
					case AssignInterleave:
						beforeAssign = prevMeaningful;
						afterFileRefKeyword = null;
						break;
					default:
						afterFileRefKeyword = null;
						break;
				}
			}
			token = scanner.scan();
		}

		// ---- Phase 2: determine context from accumulated state ----

		// Use case: empty file or cursor before any token → top-level completions
		if (lastMeaningful == null) {
			return CompletionContextType.TOP_LEVEL;
		}

		// Use case: "start = fo|" → cursor is mid-identifier; use the token
		// before the identifier (Assign) to determine context, not the partial
		// identifier itself
		if (lastMeaningful == RNCTokenType.Identifier && lastMeaningfulEnd >= offset && prevMeaningful != null) {
			lastMeaningful = prevMeaningful;
		}

		// Use case: "include 'sub/|'" → cursor is inside the string literal
		// after include/external → offer file path completions
		if (afterFileRefKeyword != null && lastMeaningful == RNCTokenType.Literal && lastMeaningfulEnd >= offset) {
			return afterFileRefKeyword == RNCTokenType.KeywordExternal
					? CompletionContextType.IN_EXTERNAL_LITERAL
					: CompletionContextType.IN_INCLUDE_LITERAL;
		}

		// Use case: "namespace foo = |" → offer well-known namespace URIs
		if (inNamespaceDecl && lastMeaningful == RNCTokenType.Assign) {
			return CompletionContextType.AFTER_NAMESPACE_ASSIGN;
		}

		// Use case: "datatypes xsd = |" → offer datatype library URIs
		if (inDatatypesDecl && lastMeaningful == RNCTokenType.Assign) {
			return CompletionContextType.AFTER_DATATYPES_ASSIGN;
		}

		// Use case: "include |" → offer file path completions
		if (lastMeaningful == RNCTokenType.KeywordInclude) {
			return CompletionContextType.AFTER_INCLUDE;
		}

		// Use case: "external |" → offer file path completions
		if (lastMeaningful == RNCTokenType.KeywordExternal) {
			return CompletionContextType.AFTER_EXTERNAL;
		}

		// Use case: "element |" → name is expected next, suppress keyword completions
		if (lastMeaningful == RNCTokenType.KeywordElement) {
			return CompletionContextType.AFTER_ELEMENT_KEYWORD;
		}

		// Use case: "attribute |" → name is expected next, suppress keyword completions
		if (lastMeaningful == RNCTokenType.KeywordAttribute) {
			return CompletionContextType.AFTER_ATTRIBUTE_KEYWORD;
		}

		// Use case: "start = |" → offer element/external/grammar + define refs
		// Use case: "content = |" → offer all pattern keywords + define refs
		// Use case: "content |= |" → same (choice extension)
		if (lastMeaningful == RNCTokenType.Assign || lastMeaningful == RNCTokenType.AssignChoice
				|| lastMeaningful == RNCTokenType.AssignInterleave) {
			if (beforeAssign == RNCTokenType.KeywordStart) {
				return CompletionContextType.AFTER_START_ASSIGN;
			}
			return CompletionContextType.AFTER_ASSIGN;
		}

		// Use case: "div { | }" or "grammar { | }" → declaration block body,
		// offer top-level completions (namespace, start, defines, etc.)
		if (braceDepth > 0 && declarationBraceDepth >= braceDepth) {
			return CompletionContextType.TOP_LEVEL;
		}

		// Use case: "element root { | }" → inside a pattern block,
		// offer pattern keywords (attribute, text, empty, ...) + define refs
		// Use case: "element root { text, | }" → after comma, same
		// Use case: "element root { text | | }" → after pipe, same
		if (braceDepth > 0 || lastMeaningful == RNCTokenType.Comma || lastMeaningful == RNCTokenType.Pipe
				|| lastMeaningful == RNCTokenType.Ampersand || lastMeaningful == RNCTokenType.BraceOpen
				|| lastMeaningful == RNCTokenType.ParenOpen) {
			return CompletionContextType.IN_PATTERN;
		}

		// Use case: after "}" at top level → back to top-level completions
		if (braceDepth == 0) {
			return CompletionContextType.TOP_LEVEL;
		}

		return CompletionContextType.UNKNOWN;
	}

	private RNCCompletionContext() {
	}
}
