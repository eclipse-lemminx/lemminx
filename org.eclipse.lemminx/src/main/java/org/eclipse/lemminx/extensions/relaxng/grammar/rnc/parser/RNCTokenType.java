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

/**
 * Enumerates the token types produced by {@link RNCScanner} when scanning
 * RELAX NG Compact Syntax (RNC) text.
 *
 * <p>
 * Use case: consumed by all RNC language features (formatting, semantic tokens,
 * folding, highlighting, completions, code actions) to analyze the structure of
 * an RNC document without building a full AST.
 * </p>
 *
 * <p>
 * Example — the RNC text {@code start = element root &#123; text &#125;}
 * produces the following token sequence:
 * </p>
 *
 * <pre>
 * KeywordStart  "start"
 * Whitespace    " "
 * Assign        "="
 * Whitespace    " "
 * KeywordElement "element"
 * Whitespace    " "
 * Identifier    "root"
 * Whitespace    " "
 * BraceOpen     "{"
 * Whitespace    " "
 * KeywordText   "text"
 * Whitespace    " "
 * BraceClose    "}"
 * EOS
 * </pre>
 *
 * @see RNCScanner
 */
public enum RNCTokenType {

	/** End of stream — returned when the scanner has consumed all input. */
	EOS,

	// -------------------- Identifiers and literals --------------------

	/**
	 * A user-defined identifier (define name, element/attribute name reference).
	 *
	 * <p>
	 * Use case: in {@code root = element root &#123; text &#125;}, the first
	 * {@code root} is an Identifier (define name), while the second {@code root}
	 * after {@code element} is also an Identifier (element name).
	 * </p>
	 *
	 * <p>
	 * Escaped identifiers ({@code \element}, {@code \text}) are also reported
	 * as Identifier, allowing reserved words to be used as names.
	 * </p>
	 */
	Identifier,

	/**
	 * A quoted string literal (single or double quotes, including triple-quoted).
	 *
	 * <p>
	 * Use case: namespace URIs, datatype values, include file paths.
	 * </p>
	 *
	 * <p>
	 * Examples: {@code "http://example.com"}, {@code 'value'},
	 * {@code """multi-line"""}.
	 * </p>
	 */
	Literal,

	// -------------------- Comments --------------------

	/**
	 * A single-line comment starting with {@code #} (but not {@code ##}).
	 *
	 * <p>
	 * Example: {@code # This is a regular comment}
	 * </p>
	 */
	Comment,

	/**
	 * A documentation comment starting with {@code ##}.
	 *
	 * <p>
	 * Documentation comments are attached to the following definition and
	 * can be used to generate human-readable schema documentation.
	 * </p>
	 *
	 * <p>
	 * Example: {@code ## The root element of the document}
	 * </p>
	 */
	DocumentationComment,

	// -------------------- Keywords --------------------

	/**
	 * The {@code namespace} keyword — declares a namespace prefix.
	 *
	 * <p>
	 * Example: {@code namespace a = "http://example.com"}
	 * </p>
	 */
	KeywordNamespace,

	/**
	 * The {@code default} keyword — used with {@code namespace} to set the
	 * default namespace.
	 *
	 * <p>
	 * Example: {@code default namespace = "http://example.com"}
	 * </p>
	 */
	KeywordDefault,

	/**
	 * The {@code datatypes} keyword — declares a datatype library prefix.
	 *
	 * <p>
	 * Example: {@code datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"}
	 * </p>
	 */
	KeywordDatatypes,

	/**
	 * The {@code element} keyword — defines an element pattern.
	 *
	 * <p>
	 * Example: {@code element root &#123; text &#125;}
	 * </p>
	 */
	KeywordElement,

	/**
	 * The {@code attribute} keyword — defines an attribute pattern.
	 *
	 * <p>
	 * Example: {@code attribute id &#123; text &#125;}
	 * </p>
	 */
	KeywordAttribute,

	/**
	 * The {@code list} keyword — matches whitespace-separated tokens.
	 *
	 * <p>
	 * Example: {@code list &#123; xsd:integer+ &#125;}
	 * </p>
	 */
	KeywordList,

	/**
	 * The {@code mixed} keyword — equivalent to {@code (text | pattern)*},
	 * allows mixed content.
	 *
	 * <p>
	 * Example: {@code mixed &#123; element b &#123; text &#125;* &#125;}
	 * </p>
	 */
	KeywordMixed,

	/**
	 * The {@code text} keyword — matches any text content.
	 *
	 * <p>
	 * Example: {@code element name &#123; text &#125;}
	 * </p>
	 */
	KeywordText,

	/**
	 * The {@code empty} keyword — matches empty content (no text, no elements).
	 *
	 * <p>
	 * Example: {@code element br &#123; empty &#125;}
	 * </p>
	 */
	KeywordEmpty,

	/**
	 * The {@code notAllowed} keyword — a pattern that matches nothing.
	 * Used for extensibility (a definition can be overridden via combine).
	 *
	 * <p>
	 * Example: {@code extra = notAllowed}
	 * </p>
	 */
	KeywordNotAllowed,

	/**
	 * The {@code external} keyword — references an external grammar as a pattern.
	 *
	 * <p>
	 * Example: {@code external "other.rnc"}
	 * </p>
	 */
	KeywordExternal,

	/**
	 * The {@code grammar} keyword — defines an explicit grammar block.
	 *
	 * <p>
	 * Example: {@code grammar &#123; start = element root &#123; text &#125; &#125;}
	 * </p>
	 */
	KeywordGrammar,

	/**
	 * The {@code start} keyword — defines the start pattern of the grammar.
	 *
	 * <p>
	 * Example: {@code start = root}
	 * </p>
	 */
	KeywordStart,

	/**
	 * The {@code include} keyword — includes another RNC grammar file.
	 *
	 * <p>
	 * Example: {@code include "common.rnc"}
	 * </p>
	 */
	KeywordInclude,

	/**
	 * The {@code div} keyword — groups grammar definitions for organizational
	 * purposes (has no semantic effect).
	 *
	 * <p>
	 * Example: {@code div &#123; a = text &#125;}
	 * </p>
	 */
	KeywordDiv,

	/**
	 * The {@code parent} keyword — references a define in the parent grammar
	 * (used inside {@code grammar} blocks).
	 *
	 * <p>
	 * Example: {@code parent name}
	 * </p>
	 */
	KeywordParent,

	/**
	 * The {@code inherit} keyword — inherits the default namespace from the
	 * including grammar.
	 *
	 * <p>
	 * Example: {@code namespace local = inherit}
	 * </p>
	 */
	KeywordInherit,

	/**
	 * The {@code string} keyword — built-in datatype that matches a string value
	 * (with whitespace normalization).
	 *
	 * <p>
	 * Example: {@code attribute type &#123; string "fixed" &#125;}
	 * </p>
	 */
	KeywordString,

	/**
	 * The {@code token} keyword — built-in datatype that matches a tokenized
	 * string (whitespace collapsed).
	 *
	 * <p>
	 * Example: {@code attribute type &#123; token "value" &#125;}
	 * </p>
	 */
	KeywordToken,

	// -------------------- Operators --------------------

	/**
	 * The assignment operator: {@code =}.
	 *
	 * <p>
	 * Example: {@code start = root}
	 * </p>
	 */
	Assign,

	/**
	 * The choice-combine assignment operator: {@code |=}.
	 *
	 * <p>
	 * Use case: extending a definition with additional alternatives.
	 * Example: {@code content |= newElement}
	 * </p>
	 */
	AssignChoice,

	/**
	 * The interleave-combine assignment operator: {@code &=}.
	 *
	 * <p>
	 * Use case: extending a definition with interleaved content.
	 * Example: {@code content &= optionalElement}
	 * </p>
	 */
	AssignInterleave,

	/**
	 * The choice (alternation) operator: {@code |}.
	 *
	 * <p>
	 * Example: {@code a | b | c} — matches a, b, or c.
	 * </p>
	 */
	Pipe,

	/**
	 * The interleave operator: {@code &}.
	 *
	 * <p>
	 * Example: {@code a & b} — matches a and b in any order.
	 * </p>
	 */
	Ampersand,

	/**
	 * The group (sequence) operator: {@code ,}.
	 *
	 * <p>
	 * Example: {@code a, b, c} — matches a followed by b followed by c.
	 * </p>
	 */
	Comma,

	/**
	 * The optional postfix operator: {@code ?} — zero or one occurrence.
	 *
	 * <p>
	 * Example: {@code name?}
	 * </p>
	 */
	QuestionMark,

	/**
	 * The zero-or-more postfix operator: {@code *}.
	 *
	 * <p>
	 * Example: {@code item*}
	 * </p>
	 */
	Star,

	/**
	 * The one-or-more postfix operator: {@code +}.
	 *
	 * <p>
	 * Example: {@code name+}
	 * </p>
	 */
	Plus,

	/**
	 * The minus operator: {@code -}. Used in name classes for exception
	 * patterns.
	 *
	 * <p>
	 * Example: {@code anyName - (foo | bar)}
	 * </p>
	 */
	Minus,

	/**
	 * The tilde operator: {@code ~}. Used as a shorthand for the inherit
	 * namespace in name classes.
	 */
	Tilde,

	// -------------------- Delimiters --------------------

	/**
	 * Opening brace: {@code &#123;}. Starts a block (element body, attribute
	 * body, div body, grammar body, include overrides).
	 */
	BraceOpen,

	/**
	 * Closing brace: {@code &#125;}. Ends a block.
	 */
	BraceClose,

	/**
	 * Opening parenthesis: {@code (}. Groups patterns.
	 *
	 * <p>
	 * Example: {@code (a | b)*}
	 * </p>
	 */
	ParenOpen,

	/**
	 * Closing parenthesis: {@code )}.
	 */
	ParenClose,

	/**
	 * Opening bracket: {@code [}. Starts an annotation.
	 *
	 * <p>
	 * Example: {@code [a:documentation "..."]}
	 * </p>
	 */
	BracketOpen,

	/**
	 * Closing bracket: {@code ]}. Ends an annotation.
	 */
	BracketClose,

	// -------------------- Separators --------------------

	/**
	 * The colon separator: {@code :}. Used for prefixed names.
	 *
	 * <p>
	 * Example: {@code xsd:integer}, {@code a:documentation}
	 * </p>
	 */
	Colon,

	/**
	 * The backslash: {@code \}. Used to escape keywords as identifiers.
	 *
	 * <p>
	 * Example: {@code \element} — uses "element" as an identifier rather
	 * than a keyword.
	 * </p>
	 */
	Backslash,

	/**
	 * Whitespace: spaces, tabs, newlines, carriage returns.
	 * Whitespace tokens are significant for the formatter (it adjusts them)
	 * but are skipped by most other language features.
	 */
	Whitespace,

	/**
	 * An unknown/unrecognized character. Produced for characters that don't
	 * match any RNC syntax rule.
	 */
	Unknown
}
