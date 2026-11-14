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
 * Represents a {@code datatypes} declaration in an RNC document.
 *
 * <p>A datatypes declaration binds a prefix to a datatypes library URI,
 * allowing the prefix to be used to reference specific data types from
 * that library.</p>
 *
 * <p>Use case: the parser creates an {@code RNCDatatypesDecl} node when it
 * encounters the {@code datatypes} keyword. Completion uses it to offer
 * well-known datatype URIs; semantic tokens classify the prefix as a
 * namespace token.</p>
 *
 * <p>Example RNC:</p>
 * <pre>
 * datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"
 *
 * root = element root {
 *   attribute id { xsd:ID }    ← uses "xsd" prefix declared above
 * }
 * </pre>
 */
public class RNCDatatypesDecl extends RNCNode {

	/** Inclusive start offset of the datatypes prefix in the source text. */
	private int prefixStart = -1;

	/** Exclusive end offset of the datatypes prefix in the source text. */
	private int prefixEnd = -1;

	/** Inclusive start offset of the URI literal (including quotes). */
	private int uriStart = -1;

	/** Exclusive end offset of the URI literal (including quotes). */
	private int uriEnd = -1;

	/**
	 * Creates a new datatypes declaration node spanning the given offsets.
	 *
	 * @param start the inclusive start offset.
	 * @param end   the exclusive end offset.
	 */
	public RNCDatatypesDecl(int start, int end) {
		super(start, end);
	}

	/**
	 * Sets the source offsets of the datatypes prefix.
	 *
	 * @param start the inclusive start offset of the prefix.
	 * @param end   the exclusive end offset of the prefix.
	 */
	public void setPrefix(int start, int end) {
		this.prefixStart = start;
		this.prefixEnd = end;
	}

	/**
	 * Returns the datatypes prefix extracted from the source text.
	 *
	 * <p>Example: for {@code datatypes xsd = "http://..."}, returns {@code "xsd"}.</p>
	 *
	 * @return the prefix string, or {@code null} if not set.
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
	 * Returns the datatypes library URI with surrounding quotes stripped.
	 *
	 * <p>Example: for {@code datatypes xsd = "http://www.w3.org/2001/XMLSchema-datatypes"},
	 * returns {@code "http://www.w3.org/2001/XMLSchema-datatypes"}.</p>
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
