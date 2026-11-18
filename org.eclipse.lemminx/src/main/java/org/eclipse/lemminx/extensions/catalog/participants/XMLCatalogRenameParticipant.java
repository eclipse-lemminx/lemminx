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
package org.eclipse.lemminx.extensions.catalog.participants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.lemminx.dom.DOMAttr;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.extensions.catalog.CatalogEntry;
import org.eclipse.lemminx.extensions.catalog.CatalogUtils;
import org.eclipse.lemminx.extensions.index.GrammarReference;
import org.eclipse.lemminx.extensions.index.GrammarReferenceLocator;
import org.eclipse.lemminx.extensions.index.WorkspaceIndex;
import org.eclipse.lemminx.extensions.index.WorkspaceIndexPlugin;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.rename.IPrepareRenameRequest;
import org.eclipse.lemminx.services.extensions.rename.IRenameParticipant;
import org.eclipse.lemminx.services.extensions.rename.IRenameRequest;
import org.eclipse.lemminx.services.extensions.rename.IRenameResponse;
import org.eclipse.lemminx.utils.DOMTextEditUtils;
import org.eclipse.lemminx.utils.DOMUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides rename support for OASIS XML catalog identifier attributes.
 *
 * <p><b>Supported targets:</b>
 * <ul>
 *   <li>{@code <uri name="http://example.org/ns" .../>} — the {@code name} attribute</li>
 *   <li>{@code <system systemId="http://example.org/dtd" .../>} — the {@code systemId} attribute</li>
 *   <li>{@code <public publicId="-//Ex//DTD//EN" .../>} — the {@code publicId} attribute</li>
 * </ul>
 *
 * <p><b>Effect:</b> Renaming the catalog identifier propagates to all workspace XML
 * files that use that identifier — {@code xmlns=}, {@code DOCTYPE SYSTEM}, namespace
 * declarations, etc. — without touching grammar-to-grammar references.
 *
 * <p><b>Example:</b>
 * <pre>
 * catalog.xml:  &lt;uri name="|http://example.org/order|" uri="schemas/order.xsd"/&gt;
 * order.xml:    &lt;order xmlns="http://example.org/order"&gt; ← also updated
 * orders.xml:   &lt;orders xmlns="http://example.org/order"&gt; ← also updated
 * </pre>
 *
 * @see WorkspaceIndex
 * @see CatalogUtils
 */
public class XMLCatalogRenameParticipant implements IRenameParticipant {

	private final WorkspaceIndex workspaceIndex;
	private final Supplier<IXMLDocumentProvider> docProviderSupplier;

	public XMLCatalogRenameParticipant(WorkspaceIndex workspaceIndex,
			Supplier<IXMLDocumentProvider> docProviderSupplier) {
		this.workspaceIndex = workspaceIndex;
		this.docProviderSupplier = docProviderSupplier;
	}

	// -------------------- Prepare rename --------------------

	@Override
	public Either<Range, PrepareRenameResult> prepareRename(IPrepareRenameRequest request,
			CancelChecker cancelChecker) {
		DOMAttr attr = findIdentifierAttr(request.getXMLDocument(), request.getOffset());
		if (attr == null) {
			return null;
		}
		Range range = XMLPositionUtility.selectAttributeValue(attr, true);
		String placeholder = attr.getValue();
		return Either.forRight(new PrepareRenameResult(range, placeholder));
	}

	// -------------------- Rename --------------------

	@Override
	public void doRename(IRenameRequest request, IRenameResponse renameResponse, CancelChecker cancelChecker) {
		DOMDocument catalogDoc = request.getXMLDocument();
		DOMAttr attr = findIdentifierAttr(catalogDoc, request.getOffset());
		if (attr == null) {
			return;
		}
		String oldIdentifier = attr.getValue();
		String newIdentifier = request.getNewText();
		if (oldIdentifier == null || oldIdentifier.equals(newIdentifier)) {
			return;
		}

		// 1. Update the catalog attribute itself
		List<TextEdit> catalogEdits = new ArrayList<>();
		Range attrRange = XMLPositionUtility.selectAttributeValue(attr, true);
		catalogEdits.add(new TextEdit(attrRange, newIdentifier));
		renameResponse.addTextDocumentEdit(DOMTextEditUtils.creatTextDocumentEdit(catalogDoc, catalogEdits));

		// 2. Update all workspace XML files that reference the old identifier
		if (workspaceIndex == null) {
			return;
		}
		Map<String, List<GrammarReference>> referencingFiles =
				workspaceIndex.getReferencesWithValue(oldIdentifier, true);
		// Group edits by URI (a single file may have multiple references to the same identifier)
		Map<String, List<TextEdit>> editsByUri = new LinkedHashMap<>();
		for (Map.Entry<String, List<GrammarReference>> e : referencingFiles.entrySet()) {
			cancelChecker.checkCanceled();
			String uri = e.getKey();
			List<GrammarReference> refs = e.getValue();
			// Re-scan open modified documents for accurate positions
			String liveContent = openDocumentContent(uri);
			if (liveContent != null) {
				List<GrammarReference> liveRefs = GrammarReferenceLocator.locateReferencesWithValue(
						liveContent, oldIdentifier, workspaceIndex.getContributors());
				if (!liveRefs.isEmpty()) {
					refs = liveRefs;
				}
			}
			List<TextEdit> edits = editsByUri.computeIfAbsent(uri, k -> new ArrayList<>());
			for (GrammarReference ref : refs) {
				Range range = new Range(
						new Position(ref.line(), ref.startChar()),
						new Position(ref.line(), ref.endChar()));
				edits.add(new TextEdit(range, newIdentifier));
			}
		}
		for (Map.Entry<String, List<TextEdit>> e : editsByUri.entrySet()) {
			String uri = e.getKey();
			List<TextEdit> edits = e.getValue();
			VersionedTextDocumentIdentifier identifier = new VersionedTextDocumentIdentifier(uri, null);
			List<Either<TextEdit, SnippetTextEdit>> eitherEdits = new ArrayList<>();
			for (TextEdit edit : edits) {
				eitherEdits.add(Either.forLeft(edit));
			}
			renameResponse.addTextDocumentEdit(new TextDocumentEdit(identifier, eitherEdits));
		}
	}

	// -------------------- Helpers --------------------

	/**
	 * Returns the catalog identifier attribute node under {@code offset}, or
	 * {@code null} if the cursor is not on a renameable catalog identifier.
	 *
	 * <p>Only the attribute VALUE is renameable (not the attribute name). If the
	 * cursor is on the attribute name part (e.g. on "name" in {@code name="http://..."}),
	 * returns {@code null}.
	 */
	private static DOMAttr findIdentifierAttr(DOMDocument document, int offset) {
		if (!DOMUtils.isCatalog(document)) {
			return null;
		}
		for (CatalogEntry entry : CatalogUtils.getCatalogEntries(document)) {
			DOMElement el = entry.getEntryElement();
			if (el == null || offset < el.getStart() || offset > el.getEnd()) {
				continue;
			}
			DOMAttr attr = CatalogUtils.getCatalogIdentifierAttr(el);
			if (attr == null) {
				continue;
			}
			// Accept cursor only inside the attribute value (not on the attribute name)
			if (attr.valueContainsOffset(offset)) {
				return attr;
			}
		}
		return null;
	}

	/** Returns in-memory content for open files, or {@code null} if closed. */
	private String openDocumentContent(String uri) {
		IXMLDocumentProvider docProvider = docProviderSupplier != null ? docProviderSupplier.get() : null;
		return docProvider != null ? WorkspaceIndexPlugin.openDocumentContent(docProvider, uri) : null;
	}
}
