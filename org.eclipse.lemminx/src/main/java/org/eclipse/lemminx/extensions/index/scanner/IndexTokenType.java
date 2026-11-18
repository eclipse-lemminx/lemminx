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
package org.eclipse.lemminx.extensions.index.scanner;

/**
 * Token types produced by {@link IndexScanner}.
 *
 * <p>Only the tokens needed for workspace Bloom-index collection are included.
 * The scanner skips everything else (text content, end tags, comments, CDATA)
 * without allocating any objects.
 */
public enum IndexTokenType {

	/**
	 * Processing-instruction name, e.g. {@code xml-model} in
	 * {@code <?xml-model href="schema.xsd"?>}.
	 * Always followed immediately by {@link #PI_CONTENT}.
	 */
	PI_NAME,

	/**
	 * Processing-instruction content — everything between the PI name and the
	 * closing {@code ?>}, with leading whitespace stripped.
	 * Example: {@code href="schema.xsd" type="application/xml"}.
	 */
	PI_CONTENT,

	/**
	 * Local name of a start tag (namespace prefix stripped).
	 * Example: {@code element} for {@code <xs:element>}.
	 */
	START_TAG,

	/**
	 * Attribute local name (namespace prefix stripped).
	 * Always followed immediately by {@link #ATTRIBUTE_VALUE} when the attribute
	 * has a quoted value; otherwise the value is absent and the next token belongs
	 * to the same element.
	 */
	ATTRIBUTE_NAME,

	/**
	 * Attribute value with surrounding quotes stripped.
	 * Line/column position is of the value content (after the opening quote).
	 */
	ATTRIBUTE_VALUE,

	/**
	 * DOCTYPE system identifier — the filename after {@code SYSTEM} or the second
	 * quoted string after {@code PUBLIC}.
	 */
	DOCTYPE_SYSTEM_ID,

	/**
	 * DOCTYPE public identifier — the first quoted string after {@code PUBLIC}.
	 * Usually followed by a {@link #DOCTYPE_SYSTEM_ID}.
	 */
	DOCTYPE_PUBLIC_ID,

	/** End of stream — no more tokens. */
	EOS
}
