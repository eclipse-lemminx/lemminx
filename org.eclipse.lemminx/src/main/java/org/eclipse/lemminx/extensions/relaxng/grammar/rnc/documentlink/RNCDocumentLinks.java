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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.documentlink;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xerces.impl.XMLEntityManager;
import org.apache.xerces.util.URI.MalformedURIException;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCFileReferenceNode;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides document links for RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Scans the AST for file reference nodes ({@code include} and
 * {@code external} declarations) and produces clickable
 * {@link DocumentLink}s that allow the user to navigate to the
 * referenced file.</p>
 *
 * <p>Use case: the user Ctrl-clicks an include URI like
 * {@code include "common.rnc"} and the editor opens {@code common.rnc}.</p>
 */
public class RNCDocumentLinks {

	private static final Logger LOGGER = Logger.getLogger(RNCDocumentLinks.class.getName());

	/**
	 * Finds all document links in the given RNC document by resolving
	 * file reference URIs from {@code include} and {@code external}
	 * declarations.
	 *
	 * @param rncDocument   the parsed RNC document containing file references.
	 * @param cancelChecker used to abort computation if the document changes.
	 * @return a list of {@link DocumentLink} objects, one per file reference.
	 */
	public List<DocumentLink> findDocumentLinks(RNCDocument rncDocument, CancelChecker cancelChecker) {
		List<DocumentLink> links = new ArrayList<>();
		TextDocument textDocument = rncDocument.getTextDocument();
		String documentURI = rncDocument.getUri();

		for (RNCFileReferenceNode ref : rncDocument.getFileReferences()) {
			cancelChecker.checkCanceled();
			String uri = ref.getUri();
			if (uri != null && !uri.isEmpty()) {
				int uriStart = ref.getUriStart();
				int uriEnd = ref.getUriEnd();
				String resolvedLocation = getResolvedLocation(documentURI, uri);
				try {
					Position start = textDocument.positionAt(uriStart + 1);
					Position end = textDocument.positionAt(uriEnd - 1);
					links.add(new DocumentLink(new Range(start, end), resolvedLocation));
				} catch (BadLocationException e) {
					LOGGER.log(Level.SEVERE, "Creation of document link failed", e);
				}
			}
		}
		return links;
	}

	private static String getResolvedLocation(String documentURI, String location) {
		if (location == null) {
			return null;
		}
		try {
			return XMLEntityManager.expandSystemId(location, documentURI, false);
		} catch (MalformedURIException e) {
			return location;
		}
	}
}
