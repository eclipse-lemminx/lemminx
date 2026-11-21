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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.IReferenceParticipant;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides "Find References" for OASIS catalog entries.
 *
 * <p><b>Use case:</b> The user clicks a "2 references" code lens on a catalog entry
 * such as {@code <system systemId="http://example.org/schema" uri="schema.xsd"/>} in
 * {@code catalog.xml}. The client sends {@code textDocument/references} on the catalog
 * file at the entry's position. This participant handles that request by locating all
 * workspace XML files that contain the catalog identifier
 * ({@code "http://example.org/schema"}) as a reference value — e.g. in a
 * {@code DOCTYPE} system ID or a namespace URI declaration.
 *
 * <p><b>Cache-first with open-document accuracy:</b> Reference positions are served
 * from the in-memory {@link FileBloomEntry} cache populated during indexing.
 * When a referencing file is currently open in the editor with unsaved changes,
 * the cached line/column positions may be stale (e.g. the user added lines before
 * the catalog reference). In that case the file's live content is re-scanned to
 * return accurate {@link Location}s.
 *
 * @see org.eclipse.lemminx.extensions.catalog.participants.XMLCatalogCodeLensParticipant
 * @see WorkspaceIndex
 * @see GrammarReference
 */
public class WorkspaceIndexCatalogReferenceParticipant implements IReferenceParticipant {

	private final WorkspaceIndex workspaceIndex;
	private final Supplier<IXMLDocumentProvider> docProviderSupplier;

	/**
	 * Constructor used in production. The document-provider supplier is resolved
	 * lazily so that the participant can be created before the registry is fully
	 * initialized.
	 */
	public WorkspaceIndexCatalogReferenceParticipant(WorkspaceIndex workspaceIndex,
			Supplier<IXMLDocumentProvider> docProviderSupplier) {
		this.workspaceIndex = workspaceIndex;
		this.docProviderSupplier = docProviderSupplier;
	}

	/** Convenience constructor for tests that don't need open-document re-scan. */
	public WorkspaceIndexCatalogReferenceParticipant(WorkspaceIndex workspaceIndex) {
		this(workspaceIndex, () -> null);
	}

	@Override
	public void findReference(DOMDocument document, Position position, ReferenceContext context,
			List<Location> locations, CancelChecker cancelChecker) {
		if (!DOMUtils.isCatalog(document)) {
			return;
		}
		int offset;
		try {
			offset = document.offsetAt(position);
		} catch (BadLocationException e) {
			return;
		}
		for (CatalogEntry entry : CatalogUtils.getCatalogEntries(document)) {
			DOMElement el = entry.getEntryElement();
			if (offset < el.getStart() || offset > el.getEnd()) {
				continue;
			}
			String identifier = CatalogUtils.getCatalogIdentifier(entry);
			if (identifier != null) {
				collectIdentifierReferences(identifier, locations, cancelChecker);
				return;
			}
		}
	}

	/**
	 * Finds all workspace XML instance files that reference the given catalog identifier
	 * (namespace URI, system ID, or public ID) and appends their {@link Location}s.
	 *
	 * <p>For open modified documents, the cached positions may be stale. In that case
	 * the file's live content is re-scanned using
	 * {@link GrammarReferenceLocator#locateReferencesWithValue} to return accurate
	 * line/column positions.
	 *
	 * @param identifier    the catalog key (namespace URI, system ID, or public ID).
	 * @param locations     list to append discovered {@link Location}s to.
	 * @param cancelChecker used to abort if the request is cancelled.
	 */
	private void collectIdentifierReferences(String identifier, List<Location> locations,
			CancelChecker cancelChecker) {
		for (Map.Entry<String, List<GrammarReference>> e :
				workspaceIndex.getReferencesWithValue(identifier, true).entrySet()) {
			cancelChecker.checkCanceled();
			String uri = e.getKey();
			List<GrammarReference> refs = e.getValue();
			// For open modified documents re-scan from live content to get accurate positions.
			String liveContent = openDocumentContent(uri);
			if (liveContent != null) {
				// Re-scan from live content to get accurate positions (line/char).
				// liveContent is a String which satisfies the CharSequence parameter — positions
				// are computed from line breaks in that content, independent of the URI.
				List<GrammarReference> liveRefs = GrammarReferenceLocator.locateReferencesWithValue(
						liveContent, identifier, workspaceIndex.getContributors());
				if (!liveRefs.isEmpty()) {
					refs = liveRefs;
				}
			}
			for (GrammarReference ref : refs) {
				locations.add(new Location(uri, new Range(
						new Position(ref.line(), ref.startChar()),
						new Position(ref.line(), ref.endChar()))));
			}
		}
	}

	/**
	 * Returns the in-memory content of {@code uri} if it is currently open in the
	 * editor, or {@code null} if closed. Delegates to
	 * {@link WorkspaceIndexPlugin#openDocumentContent} which handles URI format
	 * mismatches across LSP clients (VS Code vs LSP4E drive-letter case).
	 */
	private String openDocumentContent(String uri) {
		IXMLDocumentProvider docProvider = docProviderSupplier != null ? docProviderSupplier.get() : null;
		return docProvider != null ? WorkspaceIndexPlugin.openDocumentContent(docProvider, uri) : null;
	}
}
