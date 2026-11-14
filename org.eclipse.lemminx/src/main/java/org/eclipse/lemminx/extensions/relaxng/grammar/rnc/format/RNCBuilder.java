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

import org.eclipse.lemminx.commons.SnippetsBuilder;

/**
 * Builder for generating formatted RNC (RELAX NG Compact Syntax) content.
 * Analogous to {@code XMLBuilder} for XML content generation.
 *
 * <p>
 * Wraps a {@link StringBuilder} and provides fluent methods for appending
 * RNC constructs (namespaces, defines, elements, attributes, operators,
 * keywords, comments) with correct formatting. All methods return
 * {@code this} to enable method chaining.
 * </p>
 *
 * <p>
 * Use case: used by code actions and completions to generate properly
 * formatted RNC fragments that respect the user's indentation preferences
 * (tab vs spaces, line delimiter). Also supports LSP snippet placeholders
 * ({@code $1}, {@code ${2:default}}) for interactive completions.
 * </p>
 *
 * <p>
 * Example — generating a complete RNC schema:
 * </p>
 *
 * <pre>
 * RNCBuilder builder = new RNCBuilder("  ", "\n", false);
 * builder.addDefaultNamespace("http://example.com")
 *        .blankLine()
 *        .addStart("root")
 *        .blankLine()
 *        .startDefine("root")
 *        .startElement("root", 0)
 *        .linefeed().indent(1)
 *        .appendText()
 *        .linefeed()
 *        .closeBrace();
 * // Produces:
 * // default namespace = "http://example.com"
 * //
 * // start = root
 * //
 * // root = element root {
 * //   text
 * // }
 * </pre>
 *
 * <p>
 * Example — generating a snippet with placeholders for a completion item:
 * </p>
 *
 * <pre>
 * RNCBuilder builder = new RNCBuilder("  ", "\n", true);
 * builder.append("element ")
 *        .addSnippetPlaceholder("name")  // ${1:name}
 *        .append(" {")
 *        .linefeed().indent(1)
 *        .addFinalTabstop()              // $0
 *        .linefeed()
 *        .closeBrace();
 * // Produces: element ${1:name} {\n  $0\n}
 * </pre>
 *
 * @see org.eclipse.lemminx.dom.builder.XMLBuilder
 */
public class RNCBuilder {

	/** The underlying buffer accumulating the RNC content. */
	private final StringBuilder rnc;

	/** The line delimiter to use (e.g. "\n" or "\r\n"). */
	private final String lineDelimiter;

	/** The whitespace string for a single indentation level (e.g. "  " or "\t"). */
	private final String whitespacesIndent;

	/** Whether LSP snippet syntax ($1, ${2:default}) is supported by the client. */
	private final boolean canSupportSnippets;

	/** Auto-incrementing index for snippet tabstops. */
	private int snippetIndex;

	/**
	 * Creates a new RNC builder.
	 *
	 * @param whitespacesIndent  the string for one indent level (e.g. "  " for
	 *                           2-space indent, "\t" for tab indent).
	 * @param lineDelimiter      the line delimiter to use (e.g. "\n", "\r\n").
	 * @param canSupportSnippets {@code true} if the client supports LSP snippet
	 *                           syntax in completions and code actions.
	 */
	public RNCBuilder(String whitespacesIndent, String lineDelimiter, boolean canSupportSnippets) {
		this.rnc = new StringBuilder();
		this.whitespacesIndent = whitespacesIndent;
		this.lineDelimiter = lineDelimiter;
		this.canSupportSnippets = canSupportSnippets;
		this.snippetIndex = 1;
	}

	// -------------------- Namespace / Datatypes --------------------

	/**
	 * Appends a namespace declaration: {@code namespace prefix = "uri"}.
	 *
	 * <p>
	 * Use case: generating a namespace declaration in a "New RNC" file template
	 * or when a code action needs to add a namespace.
	 * </p>
	 *
	 * <p>
	 * Example output: {@code namespace a = "http://example.com"}
	 * </p>
	 *
	 * @param prefix the namespace prefix (e.g. "a", "html").
	 * @param uri    the namespace URI.
	 * @return this builder for chaining.
	 */
	public RNCBuilder addNamespace(String prefix, String uri) {
		rnc.append("namespace ");
		rnc.append(prefix);
		rnc.append(" = \"");
		rnc.append(uri);
		rnc.append("\"");
		return this;
	}

	/**
	 * Appends a default namespace declaration: {@code default namespace = "uri"}.
	 *
	 * <p>
	 * Example output: {@code default namespace = "http://example.com"}
	 * </p>
	 *
	 * @param uri the default namespace URI.
	 * @return this builder for chaining.
	 */
	public RNCBuilder addDefaultNamespace(String uri) {
		rnc.append("default namespace = \"");
		rnc.append(uri);
		rnc.append("\"");
		return this;
	}

	/**
	 * Appends a datatypes declaration: {@code datatypes prefix = "uri"}.
	 *
	 * <p>
	 * Use case: adding an XSD datatypes declaration:
	 * {@code datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"}
	 * </p>
	 *
	 * @param prefix the datatypes prefix (e.g. "xsd").
	 * @param uri    the datatypes library URI.
	 * @return this builder for chaining.
	 */
	public RNCBuilder addDatatypes(String prefix, String uri) {
		rnc.append("datatypes ");
		rnc.append(prefix);
		rnc.append(" = \"");
		rnc.append(uri);
		rnc.append("\"");
		return this;
	}

	// -------------------- Start / Define --------------------

	/**
	 * Appends a start pattern: {@code start = pattern}.
	 *
	 * <p>
	 * Example output: {@code start = root}
	 * </p>
	 *
	 * @param pattern the start pattern name.
	 * @return this builder for chaining.
	 */
	public RNCBuilder addStart(String pattern) {
		rnc.append("start = ");
		rnc.append(pattern);
		return this;
	}

	/**
	 * Begins a define statement: {@code name = }.
	 *
	 * <p>
	 * Use case: start a define, then append the pattern body.
	 * Example: {@code builder.startDefine("root").startElement("root", 0)...}
	 * produces {@code root = element root {...}}.
	 * </p>
	 *
	 * @param name the define name.
	 * @return this builder for chaining.
	 */
	public RNCBuilder startDefine(String name) {
		rnc.append(name);
		rnc.append(" = ");
		return this;
	}

	/**
	 * Begins a choice-combine define: {@code name |= }.
	 *
	 * <p>
	 * Use case: extending a pattern with additional choices:
	 * {@code content |= newElement}.
	 * </p>
	 *
	 * @param name the define name.
	 * @return this builder for chaining.
	 */
	public RNCBuilder startDefineChoice(String name) {
		rnc.append(name);
		rnc.append(" |= ");
		return this;
	}

	/**
	 * Begins an interleave-combine define: {@code name &= }.
	 *
	 * <p>
	 * Use case: extending a pattern with interleaved content:
	 * {@code content &= optionalElement}.
	 * </p>
	 *
	 * @param name the define name.
	 * @return this builder for chaining.
	 */
	public RNCBuilder startDefineInterleave(String name) {
		rnc.append(name);
		rnc.append(" &= ");
		return this;
	}

	// -------------------- Element / Attribute --------------------

	/**
	 * Begins an element pattern: {@code element name &#123;}.
	 *
	 * <p>
	 * Use case: generating element patterns in completions or code actions.
	 * The caller is responsible for adding content and closing with
	 * {@link #closeBrace()}.
	 * </p>
	 *
	 * <p>
	 * Example: {@code builder.startElement("root", 0)} produces
	 * {@code element root &#123;}.
	 * </p>
	 *
	 * @param name        the element name.
	 * @param indentLevel the current indent level (reserved for future use).
	 * @return this builder for chaining.
	 */
	public RNCBuilder startElement(String name, int indentLevel) {
		rnc.append("element ");
		rnc.append(name);
		rnc.append(" {");
		return this;
	}

	/**
	 * Begins an attribute pattern: {@code attribute name &#123; }.
	 *
	 * <p>
	 * Attributes use inline formatting with spaces inside braces:
	 * {@code attribute id &#123; text &#125;}.
	 * </p>
	 *
	 * @param name the attribute name.
	 * @return this builder for chaining.
	 */
	public RNCBuilder startAttribute(String name) {
		rnc.append("attribute ");
		rnc.append(name);
		rnc.append(" { ");
		return this;
	}

	/**
	 * Closes an attribute pattern: {@code  &#125;} (with leading space).
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder endAttribute() {
		rnc.append(" }");
		return this;
	}

	// -------------------- Block --------------------

	/**
	 * Appends an opening brace: {@code &#123;}.
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder openBrace() {
		rnc.append("{");
		return this;
	}

	/**
	 * Appends a closing brace: {@code &#125;}.
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder closeBrace() {
		rnc.append("}");
		return this;
	}

	// -------------------- Operators --------------------

	/**
	 * Appends the choice operator with surrounding spaces: {@code  |  }.
	 *
	 * <p>
	 * Use case: combining patterns with choice: {@code a | b | c}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendChoice() {
		rnc.append(" | ");
		return this;
	}

	/**
	 * Appends the interleave operator with surrounding spaces: {@code  &amp;  }.
	 *
	 * <p>
	 * Use case: combining patterns with interleave: {@code a &amp; b}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendInterleave() {
		rnc.append(" & ");
		return this;
	}

	/**
	 * Appends the group (sequence) operator: {@code , } (comma + space).
	 *
	 * <p>
	 * Use case: sequencing patterns: {@code a, b, c}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendGroup() {
		rnc.append(", ");
		return this;
	}

	/**
	 * Appends the optional postfix operator: {@code ?}.
	 *
	 * <p>
	 * Use case: marking a pattern as optional: {@code name?}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendOptional() {
		rnc.append("?");
		return this;
	}

	/**
	 * Appends the zero-or-more postfix operator: {@code *}.
	 *
	 * <p>
	 * Use case: marking a pattern as repeatable: {@code item*}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendZeroOrMore() {
		rnc.append("*");
		return this;
	}

	/**
	 * Appends the one-or-more postfix operator: {@code +}.
	 *
	 * <p>
	 * Use case: marking a pattern as requiring at least one: {@code name+}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendOneOrMore() {
		rnc.append("+");
		return this;
	}

	// -------------------- Keywords --------------------

	/**
	 * Appends the {@code text} keyword (matches any text content).
	 *
	 * <p>
	 * Example: inside an element — {@code element name &#123; text &#125;}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendText() {
		rnc.append("text");
		return this;
	}

	/**
	 * Appends the {@code empty} keyword (matches empty content).
	 *
	 * <p>
	 * Example: {@code element br &#123; empty &#125;}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendEmpty() {
		rnc.append("empty");
		return this;
	}

	/**
	 * Appends an include declaration: {@code include "file"}.
	 *
	 * <p>
	 * Use case: including another RNC grammar:
	 * {@code include "common.rnc"}.
	 * </p>
	 *
	 * @param file the file path to include.
	 * @return this builder for chaining.
	 */
	public RNCBuilder appendInclude(String file) {
		rnc.append("include \"");
		rnc.append(file);
		rnc.append("\"");
		return this;
	}

	// -------------------- Snippets --------------------

	/**
	 * Appends a snippet tabstop ({@code $N}) at the current position,
	 * auto-incrementing the index. If snippets are not supported, appends
	 * nothing.
	 *
	 * <p>
	 * Use case: placing cursor positions in a completion snippet:
	 * {@code element $1 &#123; $2 &#125;}.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder addSnippetTabstop() {
		if (canSupportSnippets) {
			SnippetsBuilder.tabstops(snippetIndex++, rnc);
		}
		return this;
	}

	/**
	 * Appends a snippet placeholder ({@code ${N:defaultValue}}) at the
	 * current position. If snippets are not supported, appends the default
	 * value as plain text.
	 *
	 * <p>
	 * Use case: a completion for {@code element} with a placeholder for the
	 * name: {@code element ${1:name} &#123; $0 &#125;}.
	 * </p>
	 *
	 * @param defaultValue the default text shown in the placeholder.
	 * @return this builder for chaining.
	 */
	public RNCBuilder addSnippetPlaceholder(String defaultValue) {
		if (canSupportSnippets) {
			SnippetsBuilder.placeholders(snippetIndex++, defaultValue, rnc);
		} else {
			rnc.append(defaultValue);
		}
		return this;
	}

	/**
	 * Appends the final tabstop ({@code $0}), which marks where the cursor
	 * rests after all other tabstops are visited. If snippets are not
	 * supported, appends nothing.
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder addFinalTabstop() {
		if (canSupportSnippets) {
			SnippetsBuilder.tabstops(0, rnc);
		}
		return this;
	}

	// -------------------- Whitespace --------------------

	/**
	 * Appends a line feed (line delimiter).
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder linefeed() {
		rnc.append(lineDelimiter);
		return this;
	}

	/**
	 * Appends indentation for the given nesting level.
	 *
	 * <p>
	 * Use case: indenting content inside a block:
	 * {@code builder.linefeed().indent(1).appendText()} produces
	 * {@code \n  text} (with 2-space indent).
	 * </p>
	 *
	 * @param level the number of indent levels to append.
	 * @return this builder for chaining.
	 */
	public RNCBuilder indent(int level) {
		for (int i = 0; i < level; i++) {
			rnc.append(whitespacesIndent);
		}
		return this;
	}

	/**
	 * Appends a single space character.
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder space() {
		rnc.append(" ");
		return this;
	}

	/**
	 * Appends a blank line (two consecutive line delimiters).
	 *
	 * <p>
	 * Use case: separating top-level definitions with a visual gap.
	 * </p>
	 *
	 * @return this builder for chaining.
	 */
	public RNCBuilder blankLine() {
		rnc.append(lineDelimiter);
		rnc.append(lineDelimiter);
		return this;
	}

	// -------------------- Raw append --------------------

	/**
	 * Appends arbitrary text to the buffer.
	 *
	 * @param text the text to append.
	 * @return this builder for chaining.
	 */
	public RNCBuilder append(String text) {
		rnc.append(text);
		return this;
	}

	/**
	 * Appends a single character to the buffer.
	 *
	 * @param c the character to append.
	 * @return this builder for chaining.
	 */
	public RNCBuilder append(char c) {
		rnc.append(c);
		return this;
	}

	// -------------------- Comment --------------------

	/**
	 * Appends a single-line comment: {@code # text}.
	 *
	 * <p>
	 * Example output: {@code # This is a comment}
	 * </p>
	 *
	 * @param text the comment text (without the {@code #} prefix).
	 * @return this builder for chaining.
	 */
	public RNCBuilder addComment(String text) {
		rnc.append("# ");
		rnc.append(text);
		return this;
	}

	/**
	 * Appends a documentation comment: {@code ## text}.
	 *
	 * <p>
	 * Documentation comments in RNC are attached to the following definition
	 * and can be used to generate documentation.
	 * </p>
	 *
	 * <p>
	 * Example output: {@code ## The root element of the document}
	 * </p>
	 *
	 * @param text the documentation comment text (without the {@code ##} prefix).
	 * @return this builder for chaining.
	 */
	public RNCBuilder addDocComment(String text) {
		rnc.append("## ");
		rnc.append(text);
		return this;
	}

	// -------------------- Build --------------------

	/**
	 * Returns the accumulated RNC content as a string.
	 *
	 * @return the built RNC content.
	 */
	@Override
	public String toString() {
		return rnc.toString();
	}

	/**
	 * Returns the current length of the accumulated content.
	 *
	 * @return the number of characters in the buffer.
	 */
	public int length() {
		return rnc.length();
	}
}
