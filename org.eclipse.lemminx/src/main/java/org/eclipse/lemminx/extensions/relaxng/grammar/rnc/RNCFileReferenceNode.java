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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

/**
 * Base class for RNC nodes that reference an external file via a URI literal.
 *
 * <p>Shared by {@link RNCInclude} ({@code include "file.rnc"}) and
 * {@link RNCExternal} ({@code external "file.rnc"}).</p>
 */
public abstract class RNCFileReferenceNode extends RNCNode {

	/** Inclusive start offset of the URI literal (including quotes). */
	private int uriStart = -1;

	/** Exclusive end offset of the URI literal (including quotes). */
	private int uriEnd = -1;

	/**
	 * Creates a new file reference node spanning the given offsets.
	 *
	 * @param start the inclusive start offset.
	 * @param end   the exclusive end offset.
	 */
	protected RNCFileReferenceNode(int start, int end) {
		super(start, end);
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
	 * Returns the file reference URI with surrounding quotes stripped.
	 *
	 * <p>Use case: used by document links to create clickable file references,
	 * and by the include resolver to locate the referenced grammar file.</p>
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
