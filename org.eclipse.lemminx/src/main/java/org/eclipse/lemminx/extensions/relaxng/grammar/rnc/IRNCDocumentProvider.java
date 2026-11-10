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
 * Provides access to parsed {@link RNCDocument} instances by URI.
 *
 * <p>Use case: when a language feature (completion, hover, definition, etc.)
 * needs to access an included RNC file, this provider first checks if the file
 * is already open in the editor (returning the in-memory version with unsaved
 * edits) and falls back to reading from disk otherwise.</p>
 *
 * <p>This avoids re-parsing files from disk when they are already open and
 * ensures correct ranges when the user has unsaved modifications.</p>
 *
 * @see org.eclipse.lemminx.services.IXMLDocumentProvider
 */
public interface IRNCDocumentProvider {

	/**
	 * Returns the parsed {@link RNCDocument} for the given URI, or {@code null}
	 * if the document cannot be found or parsed.
	 *
	 * <p>Implementations should check open documents first, then fall back to
	 * reading from disk.</p>
	 *
	 * @param uri the document URI (e.g. {@code "file:///path/to/schema.rnc"}).
	 * @return the parsed document, or {@code null}.
	 */
	RNCDocument getDocument(String uri);
}
