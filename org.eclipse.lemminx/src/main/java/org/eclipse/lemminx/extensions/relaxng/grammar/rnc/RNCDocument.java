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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCErrorCode;

/**
 * Represents a parsed RNC (RELAX NG Compact Syntax) document.
 *
 * <p>This is the root node of the RNC abstract syntax tree, holding the full source
 * text, the document URI, and all top-level constructs (namespace declarations,
 * datatypes declarations, start pattern, defines, includes, divs, elements, and
 * attributes) as child nodes. It also collects parser errors.</p>
 *
 * <p>Use case: produced by {@link org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser#parse(CharSequence, String)} and used
 * by all language features (completion, hover, definition, etc.) to access the
 * document structure.</p>
 *
 * <p>Example: parsing the RNC source:</p>
 * <pre>
 * namespace a = "http://example.com"
 * start = root
 * root = element root {
 *   attribute id { text },
 *   name+
 * }
 * name = element name { text }
 * </pre>
 * <p>produces an {@code RNCDocument} with children:
 * {@code RNCNamespaceDecl}, {@code RNCStart}, {@code RNCDefine("root")},
 * {@code RNCDefine("name")}.</p>
 */
public class RNCDocument extends RNCNode {

	/** The full source text of the document. */
	private final CharSequence source;

	/** The document URI (e.g. {@code "file:///path/to/schema.rnc"}). */
	private final String uri;

	/** The LSP text document (for offset/position conversions). */
	private TextDocument textDocument;

	/** Parser errors collected during parsing. */
	private final List<RNCError> errors;

	/**
	 * Creates a new RNC document.
	 *
	 * @param source the full source text.
	 * @param uri    the document URI.
	 */
	public RNCDocument(CharSequence source, String uri) {
		super(0, source.length());
		this.source = source;
		this.uri = uri;
		this.errors = new ArrayList<>();
		setClosed(true);
	}

	public TextDocument getTextDocument() {
		return textDocument;
	}

	public void setTextDocument(TextDocument textDocument) {
		this.textDocument = textDocument;
	}

	/**
	 * Records a parser error in this document.
	 *
	 * @param errorCode the error code identifying the type of error.
	 * @param message   the human-readable error message.
	 * @param start     the inclusive start offset of the error range.
	 * @param end       the exclusive end offset of the error range.
	 */
	public void addError(RNCErrorCode errorCode, String message, int start, int end) {
		errors.add(new RNCError(errorCode, message, start, end));
	}

	/**
	 * Returns the list of parser errors collected during parsing.
	 *
	 * @return the errors (may be empty, never {@code null}).
	 */
	public List<RNCError> getErrors() {
		return errors;
	}

	/**
	 * Represents a single parser error with a code, message, and source range.
	 *
	 * <p>Use case: converted into LSP {@code Diagnostic} by the validator, and
	 * matched by {@link RNCErrorCode} in the code actions delegate for quick fixes.</p>
	 */
	public static class RNCError {
		private final RNCErrorCode errorCode;
		private final String message;
		private final int start;
		private final int end;

		/**
		 * Creates a new parser error.
		 *
		 * @param errorCode the error code.
		 * @param message   the error message.
		 * @param start     the inclusive start offset.
		 * @param end       the exclusive end offset.
		 */
		public RNCError(RNCErrorCode errorCode, String message, int start, int end) {
			this.errorCode = errorCode;
			this.message = message;
			this.start = start;
			this.end = end;
		}

		public RNCErrorCode getErrorCode() {
			return errorCode;
		}

		public String getMessage() {
			return message;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	/**
	 * Returns the document URI.
	 *
	 * @return the URI string (e.g. {@code "file:///path/to/schema.rnc"}).
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Returns the full source text of the document.
	 *
	 * @return the source text.
	 */
	public CharSequence getSource() {
		return source;
	}

	/**
	 * Extracts a substring from the source text.
	 *
	 * @param start the inclusive start offset.
	 * @param end   the exclusive end offset.
	 * @return the extracted text.
	 */
	public String getText(int start, int end) {
		return source.subSequence(start, end).toString();
	}

	/**
	 * Returns all top-level namespace declarations in this document.
	 *
	 * <p>Example RNC: {@code namespace a = "http://example.com"}</p>
	 *
	 * @return the namespace declarations.
	 */
	public List<RNCNamespaceDecl> getNamespaceDecls() {
		List<RNCNamespaceDecl> result = new ArrayList<>();
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCNamespaceDecl) {
				result.add((RNCNamespaceDecl) child);
			}
		}
		return result;
	}

	/**
	 * Returns all top-level datatypes declarations in this document.
	 *
	 * <p>Example RNC: {@code datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"}</p>
	 *
	 * @return the datatypes declarations.
	 */
	public List<RNCDatatypesDecl> getDatatypesDecls() {
		List<RNCDatatypesDecl> result = new ArrayList<>();
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCDatatypesDecl) {
				result.add((RNCDatatypesDecl) child);
			}
		}
		return result;
	}

	/**
	 * Returns all define declarations in this document, including those nested
	 * inside {@code div} or {@code include} blocks.
	 *
	 * <p>Use case: used by completion (to suggest define references), code lens
	 * (to count references), highlighting, and definition (to find declarations).</p>
	 *
	 * <p>Example RNC: {@code root = element root \{ text \}} declares a define named "root".</p>
	 *
	 * @return all defines in the document (searches recursively through children).
	 */
	public List<RNCDefine> getDefines() {
		List<RNCDefine> result = new ArrayList<>();
		collectNodes(this, RNCDefine.class, result);
		return result;
	}

	/**
	 * Returns the start declaration, or {@code null} if none exists.
	 *
	 * <p>Example RNC: {@code start = root}</p>
	 *
	 * @return the {@link RNCStart} node, or {@code null}.
	 */
	public RNCStart getStartDecl() {
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCStart) {
				return (RNCStart) child;
			}
		}
		return null;
	}

	/**
	 * Returns whether this document has a top-level pattern (e.g., a standalone
	 * {@code element root { text }} without a {@code start =} declaration).
	 *
	 * <p>In RNC, a schema can be either a grammar form ({@code start = ...}) or
	 * a pattern form (a bare pattern at the top level).</p>
	 *
	 * @return {@code true} if a top-level element or attribute pattern exists.
	 */
	public boolean hasTopLevelPattern() {
		// Pattern form never has defines — if defines exist, we're in grammar form
		// and any top-level element/attribute is a parser artifact, not a standalone pattern.
		if (!getDefines().isEmpty()) {
			return false;
		}
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCElement || child instanceof RNCAttribute) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether a {@code default namespace} declaration exists.
	 *
	 * @return {@code true} if a default namespace is declared.
	 */
	public boolean hasDefaultNamespace() {
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCNamespaceDecl && ((RNCNamespaceDecl) child).isDefault()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns all top-level include declarations in this document.
	 *
	 * <p>Example RNC: {@code include "other.rnc"}</p>
	 *
	 * @return the include declarations.
	 */
	public List<RNCInclude> getIncludes() {
		List<RNCInclude> result = new ArrayList<>();
		for (RNCNode child : getChildren()) {
			if (child instanceof RNCInclude) {
				result.add((RNCInclude) child);
			}
		}
		return result;
	}

	/**
	 * Returns all external references in this document, including nested ones.
	 *
	 * <p>Example RNC: {@code start = external "other.rnc"}</p>
	 */
	public List<RNCExternal> getExternals() {
		List<RNCExternal> result = new ArrayList<>();
		collectNodes(this, RNCExternal.class, result);
		return result;
	}

	/**
	 * Returns all file reference nodes (includes and externals) in this document.
	 */
	public List<RNCFileReferenceNode> getFileReferences() {
		List<RNCFileReferenceNode> result = new ArrayList<>();
		collectNodes(this, RNCFileReferenceNode.class, result);
		return result;
	}

	private static <T> void collectNodes(RNCNode node, Class<T> type, List<T> result) {
		for (RNCNode child : node.getChildren()) {
			if (type.isInstance(child)) {
				result.add(type.cast(child));
			}
			collectNodes(child, type, result);
		}
	}

	/**
	 * Returns all defines visible from this document: local defines plus defines
	 * from all included files. Duplicates (by name) are excluded — local defines
	 * take precedence.
	 *
	 * @param excludeInclude   optional include to skip (e.g. the enclosing include
	 *                         when completing inside an include body), or {@code null}.
	 * @param documentProvider provides access to included documents (checks open
	 *                         documents first, falls back to disk).
	 * @return the list of all visible defines.
	 */
	public List<RNCDefine> getAllVisibleDefines(RNCInclude excludeInclude, IRNCDocumentProvider documentProvider) {
		List<RNCDefine> result = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (RNCDefine define : getDefines()) {
			String name = define.getName();
			if (name != null && seen.add(name)) {
				result.add(define);
			}
		}
		for (RNCInclude include : getIncludes()) {
			if (include == excludeInclude) {
				continue;
			}
			String resolvedUri = RNCIncludeUtils.resolveUri(include, getUri());
			RNCDocument includedDoc = documentProvider.getDocument(resolvedUri);
			if (includedDoc != null) {
				for (RNCDefine define : includedDoc.getDefines()) {
					String name = define.getName();
					if (name != null && seen.add(name)) {
						result.add(define);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Finds a define by name.
	 *
	 * <p>Use case: used by hover (to show the define text), definition (to navigate
	 * to the declaration), and highlighting (to locate the Write highlight).</p>
	 *
	 * @param name the define name to search for (e.g. "root").
	 * @return the matching {@link RNCDefine}, or {@code null} if not found.
	 */
	public RNCDefine findDefine(String name) {
		for (RNCDefine define : getDefines()) {
			if (name.equals(define.getName())) {
				return define;
			}
		}
		return null;
	}

	/**
	 * Returns {@code true} if this document has no meaningful content — only
	 * whitespace and comments, no declarations or patterns.
	 *
	 * <p>Use case: when the document has no content, RNC completion offers
	 * new-file snippets (full schema templates) instead of incremental
	 * declarations.</p>
	 *
	 * @return {@code true} if the document has no content (no declarations,
	 *         no non-whitespace/non-comment text).
	 */
	public boolean hasNoContent() {
		if (getStartDecl() != null || !getDefines().isEmpty()
				|| !getNamespaceDecls().isEmpty() || !getDatatypesDecls().isEmpty()
				|| !getIncludes().isEmpty()) {
			return false;
		}
		boolean inComment = false;
		for (int i = 0; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (inComment) {
				if (ch == '\n' || ch == '\r') {
					inComment = false;
				}
				continue;
			}
			if (ch == '#') {
				inComment = true;
				continue;
			}
			if (!Character.isWhitespace(ch)) {
				return false;
			}
		}
		return true;
	}
}
