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
package org.eclipse.lemminx.extensions.index;

/**
 * A located grammar reference within an XML file, stored as a line/column
 * range so that callers can build LSP {@link org.eclipse.lsp4j.Location}s and
 * {@link org.eclipse.lsp4j.TextEdit}s without re-reading the file.
 *
 * <p>Grammar references are always single-line (attribute values, DOCTYPE
 * system/public IDs, PI content), so only one line number is stored.
 * {@code endLine} is omitted — it is always equal to {@code line}.
 *
 * <p><b>Example:</b> In {@code <?xml-model href="schema.xsd"?>} on line 0,
 * the GrammarReference would be:
 * <pre>
 *   line      =  0  (zero-based)
 *   startChar = 22  (start of {@code schema.xsd}, after the opening {@code "})
 *   endChar   = 32  (exclusive, before the closing {@code "})
 *   value     = "schema.xsd"
 *   kind      = XML_MODEL_HREF
 * </pre>
 *
 * <p>All fields are populated once during {@link GrammarReferenceLocator#locate}
 * and cached in {@link FileBloomEntry}. After indexing, files are never
 * re-read to build Locations or TextEdits — the cached line/column positions
 * are used directly.
 *
 * @see GrammarReferenceLocator
 * @see GrammarFileRenameSupport
 */
public final class GrammarReference {

	/**
	 * The kind of grammar reference.
	 */
	public enum Kind {
		/** {@code <?xml-model href="..."?>} (UC14) */
		XML_MODEL_HREF,
		/** {@code xsi:schemaLocation="ns path"} — the path part (UC16) */
		SCHEMA_LOCATION,
		/** {@code xsi:noNamespaceSchemaLocation="..."} (UC15) */
		NO_NS_SCHEMA_LOCATION,
		/** {@code <!DOCTYPE ... SYSTEM "...">} (UC17/UC18) */
		DOCTYPE_SYSTEM,
		/** {@code <!DOCTYPE ... PUBLIC "..." ...>} — the public ID */
		DOCTYPE_PUBLIC,
		/** {@code schemaLocation="..."} on xs:include/xs:import */
		INCLUDE_SCHEMA_LOCATION,
		/** {@code href="..."} on XInclude, XSLT import/include (UC21) */
		HREF,
		/** {@code uri="..."} on OASIS catalog entries ({@code uri}, {@code public}, {@code system}, etc.) */
		CATALOG_URI,
		/**
		 * Namespace URI from {@code xmlns="..."} or {@code xmlns:prefix="..."}, or the
		 * namespace part of {@code xsi:schemaLocation="ns path"}. Used by catalog code
		 * lenses to count documents bound to a grammar via namespace resolution
		 * ({@code resolveURI(namespaceURI)} in {@code LSPXMLCatalogResolver}).
		 */
		NAMESPACE_DECL
	}

	/** Absolute character offset from the start of the document head. */
	private final int offset;
	/** Zero-based line number. Grammar references are always single-line, so no {@code endLine} is needed. */
	private final int line;
	/** Zero-based start column (inclusive). */
	private final int startChar;
	/** Zero-based end column (exclusive). */
	private final int endChar;
	private final String value;
	private final Kind kind;

	/**
	 * Full constructor used by {@link GrammarReferenceLocator#locate(org.eclipse.lemminx.commons.TextDocument)}.
	 * Both the absolute offset (for text extraction in tests) and the line/column positions
	 * (for building LSP {@link org.eclipse.lsp4j.Location}s and {@link org.eclipse.lsp4j.TextEdit}s)
	 * are stored once at indexing time.
	 */
	public GrammarReference(int offset, int line, int startChar, int endChar, String value, Kind kind) {
		this.offset = offset;
		this.line = line;
		this.startChar = startChar;
		this.endChar = endChar;
		this.value = value;
		this.kind = kind;
	}

	/** Absolute character offset from the start of the document. */
	public int offset() {
		return offset;
	}

	/** Zero-based line number. Equal to {@code endLine} — grammar refs are always single-line. */
	public int line() {
		return line;
	}

	/** Zero-based start column (inclusive). */
	public int startChar() {
		return startChar;
	}

	/** Zero-based end column (exclusive). */
	public int endChar() {
		return endChar;
	}

	/** Character length of the value: {@code endChar - startChar}. */
	public int length() {
		return endChar - startChar;
	}

	public String value() {
		return value;
	}

	public Kind kind() {
		return kind;
	}
}
