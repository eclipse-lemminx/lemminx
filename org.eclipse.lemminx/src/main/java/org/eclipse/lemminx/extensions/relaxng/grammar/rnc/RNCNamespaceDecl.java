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

/**
 * Represents a {@code namespace} declaration in an RNC document.
 *
 * <p>A namespace declaration binds a prefix to a namespace URI, or sets the
 * default namespace. The prefix can then be used to qualify element and
 * attribute names in the grammar.</p>
 *
 * <p>Use case: the parser creates an {@code RNCNamespaceDecl} node for each
 * {@code namespace} or {@code default namespace} declaration. Semantic tokens
 * use it to classify the prefix as a namespace token.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * default namespace = "http://www.w3.org/1999/xhtml"   ← isDefault=true, prefix=null
 * namespace a = "http://relaxng.org/ns/compatibility/annotations/1.0"  ← isDefault=false, prefix="a"
 * </pre>
 */
public class RNCNamespaceDecl extends RNCNode {

	/** Whether this is a {@code default namespace} declaration. */
	private boolean isDefault;

	/** Inclusive start offset of the namespace prefix in the source text. */
	private int prefixStart = -1;

	/** Exclusive end offset of the namespace prefix in the source text. */
	private int prefixEnd = -1;

	/** Inclusive start offset of the URI literal (including quotes). */
	private int uriStart = -1;

	/** Exclusive end offset of the URI literal (including quotes). */
	private int uriEnd = -1;

	/**
	 * Creates a new namespace declaration node spanning the given offsets.
	 *
	 * @param start the inclusive start offset.
	 * @param end   the exclusive end offset.
	 */
	public RNCNamespaceDecl(int start, int end) {
		super(start, end);
	}

	/**
	 * Returns whether this is a default namespace declaration.
	 *
	 * @return {@code true} for {@code default namespace = "uri"}, {@code false}
	 *         for {@code namespace prefix = "uri"}.
	 */
	public boolean isDefault() {
		return isDefault;
	}

	/**
	 * Sets whether this is a default namespace declaration.
	 *
	 * @param isDefault {@code true} if the declaration is preceded by {@code default}.
	 */
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	/**
	 * Sets the source offsets of the namespace prefix.
	 *
	 * @param start the inclusive start offset of the prefix.
	 * @param end   the exclusive end offset of the prefix.
	 */
	public void setPrefix(int start, int end) {
		this.prefixStart = start;
		this.prefixEnd = end;
	}

	/**
	 * Returns the namespace prefix extracted from the source text.
	 *
	 * <p>Example: for {@code namespace a = "http://..."}, returns {@code "a"}.</p>
	 *
	 * @return the prefix string, or {@code null} for default namespace declarations.
	 */
	public String getPrefix() {
		if (prefixStart == -1) {
			return null;
		}
		RNCDocument doc = getOwnerDocument();
		return doc != null ? doc.getText(prefixStart, prefixEnd) : null;
	}

	/**
	 * Returns the inclusive start offset of the prefix.
	 *
	 * @return the prefix start offset, or {@code -1} if not set.
	 */
	public int getPrefixStart() {
		return prefixStart;
	}

	/**
	 * Returns the exclusive end offset of the prefix.
	 *
	 * @return the prefix end offset, or {@code -1} if not set.
	 */
	public int getPrefixEnd() {
		return prefixEnd;
	}

	/**
	 * Sets the source offsets of the URI literal.
	 *
	 * @param start the inclusive start offset (including the opening quote).
	 * @param end   the exclusive end offset (after the closing quote).
	 */
	public void setUri(int start, int end) {
		this.uriStart = start;
		this.uriEnd = end;
	}

	/**
	 * Returns the namespace URI with surrounding quotes stripped.
	 *
	 * <p>Example: for {@code namespace a = "http://example.com"}, returns
	 * {@code "http://example.com"} (without the surrounding double quotes).</p>
	 *
	 * @return the URI string without quotes, or {@code null} if not set.
	 */
	public String getUri() {
		if (uriStart == -1) {
			return null;
		}
		RNCDocument doc = getOwnerDocument();
		if (doc == null) {
			return null;
		}
		String raw = doc.getText(uriStart, uriEnd);
		if (raw.length() >= 2 && (raw.charAt(0) == '"' || raw.charAt(0) == '\'')) {
			return raw.substring(1, raw.length() - 1);
		}
		return raw;
	}

	/**
	 * Returns the inclusive start offset of the URI literal.
	 *
	 * @return the URI start offset, or {@code -1} if not set.
	 */
	public int getUriStart() {
		return uriStart;
	}

	/**
	 * Returns the exclusive end offset of the URI literal.
	 *
	 * @return the URI end offset, or {@code -1} if not set.
	 */
	public int getUriEnd() {
		return uriEnd;
	}
}
