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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.IXMLDocumentProvider;
import org.eclipse.lemminx.services.extensions.IReferenceParticipant;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides cross-file "Find References" using the workspace Bloom index.
 *
 * <p><b>Two entry points:</b>
 * <ul>
 *   <li>Cursor on a grammar reference attribute value
 *       ({@code href}, {@code noNamespaceSchemaLocation}, {@code schemaLocation}) in
 *       any XML file → returns all workspace files that reference that grammar.</li>
 *   <li>Cursor anywhere in a grammar file (XSD, DTD, RNG, RNC) → returns all
 *       workspace XML files that reference the current grammar file.</li>
 * </ul>
 *
 * <p><b>Cache-first with open-document accuracy:</b> Reference positions are served
 * from the in-memory {@link FileBloomEntry} cache populated during indexing.
 * When a referencing file is currently open in the editor with unsaved changes,
 * the cached line/column positions may be stale (e.g. the user added lines before
 * the grammar reference). In that case the file's live content is re-scanned to
 * return accurate {@link Location}s.
 *
 * @see WorkspaceIndex
 * @see GrammarReferenceLocator
 * @see WorkspaceIndexCodeLensParticipant
 */
public class WorkspaceIndexReferenceParticipant implements IReferenceParticipant {

	private final WorkspaceIndex workspaceIndex;
	private final Supplier<IXMLDocumentProvider> docProviderSupplier;

	/**
	 * Constructor used in production. The document-provider supplier is resolved
	 * lazily so that the participant can be created before the registry is fully
	 * initialized.
	 */
	public WorkspaceIndexReferenceParticipant(WorkspaceIndex workspaceIndex,
			Supplier<IXMLDocumentProvider> docProviderSupplier) {
		this.workspaceIndex = workspaceIndex;
		this.docProviderSupplier = docProviderSupplier;
	}

	/** Convenience constructor for tests that don't need open-document re-scan. */
	public WorkspaceIndexReferenceParticipant(WorkspaceIndex workspaceIndex) {
		this(workspaceIndex, () -> null);
	}

	/**
	 * Finds cross-file references using the workspace Bloom index.
	 *
	 * <p><b>Case 1 — cursor on a grammar reference value:</b>
	 * The user is in {@code order.xml} and positions the cursor inside the
	 * {@code "schema.xsd"} value of {@code xsi:noNamespaceSchemaLocation="schema.xsd"}.
	 * This method extracts {@code "schema.xsd"} as the target and returns all
	 * workspace files that also reference it.
	 *
	 * <p><b>Case 2 — cursor anywhere in a grammar file:</b>
	 * The user opens {@code schema.xsd} and invokes "Find All References".
	 * This method detects that the current document is a grammar file, uses
	 * its filename as the target, and returns all XML files in the workspace
	 * that reference {@code schema.xsd}.
	 */
	@Override
	public void findReference(DOMDocument document, Position position, ReferenceContext context,
			List<Location> locations, CancelChecker cancelChecker) {
		Path targetPath = resolveTargetPath(document, position);
		if (targetPath == null) {
			return;
		}
		locations.addAll(collectCrossFileReferences(workspaceIndex, targetPath, this::openDocumentContent, cancelChecker));
	}

	@Override
	public CompletableFuture<List<Location>> findReferenceByUri(String uri, Position position,
			ReferenceContext context, CancelChecker cancelChecker) {
		if (!isGrammarFile(uri)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		Path targetPath = FilesUtils.getPathOrNull(uri);
		if (targetPath == null) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		return workspaceIndex.getScanCompletedFuture()
				.thenApply(v -> collectCrossFileReferences(workspaceIndex, targetPath, this::openDocumentContent, cancelChecker));
	}

	/**
	 * Finds all workspace files that reference {@code targetPath} and returns their
	 * exact {@link Location}s, re-scanning open documents from live content when
	 * the caller provides an {@code openDocContent} function.
	 *
	 * <p><b>Use case — open modified file:</b> {@code order.xml} is open in the editor
	 * and the user added 2 comment lines before {@code <?xml-model href="schema.xsd"?>}.
	 * The cached ref says line 1, but the live position is line 3. With
	 * {@code openDocContent} supplied, this method re-scans {@code order.xml}'s live
	 * content and returns the accurate line-3 {@link Location}.
	 *
	 * @param index          the workspace Bloom index.
	 * @param targetPath     absolute path of the grammar file being queried.
	 * @param openDocContent function mapping a URI to its in-memory content if the
	 *                       file is currently open, or returning {@code null} if closed.
	 *                       Pass {@code null} to always use cached positions.
	 * @param cancelChecker  used to abort if the request is cancelled.
	 * @return all locations across the workspace that reference {@code targetPath}.
	 */
	public static List<Location> collectCrossFileReferences(WorkspaceIndex index, Path targetPath,
			Function<String, String> openDocContent, CancelChecker cancelChecker) {
		List<Location> locations = new ArrayList<>();
		for (Map.Entry<String, List<GrammarReference>> e : index.getReferencesTo(targetPath).entrySet()) {
			cancelChecker.checkCanceled();
			String uri = e.getKey();
			List<GrammarReference> refs = e.getValue();
			// For open modified documents re-scan from live content to get accurate positions.
			// If the user added lines before the grammar reference since the last save, the
			// cached line numbers are stale and would point the user to the wrong location.
			if (openDocContent != null) {
				String liveContent = openDocContent.apply(uri);
				if (liveContent != null) {
					Path candidatePath = FilesUtils.getPathOrNull(uri);
					List<GrammarReference> liveRefs = GrammarReferenceLocator.locateReferencesTo(
							new TextDocument(liveContent, uri), targetPath, candidatePath,
							index.getContributors());
					if (!liveRefs.isEmpty()) {
						refs = liveRefs;
					}
				}
			}
			for (GrammarReference ref : refs) {
				locations.add(new Location(uri, new Range(
						new Position(ref.line(), ref.startChar()),
						new Position(ref.line(), ref.endChar()))));
			}
		}
		return locations;
	}

	/**
	 * Convenience overload without open-document re-scan. Used by RNC references
	 * and other callers that don't have access to a document provider.
	 */
	public static List<Location> collectCrossFileReferences(WorkspaceIndex index, Path targetPath,
			CancelChecker cancelChecker) {
		return collectCrossFileReferences(index, targetPath, null, cancelChecker);
	}

	/**
	 * Resolves the grammar file path that the cursor is pointing at.
	 *
	 * <p><b>Case 1:</b> Cursor inside a grammar reference attribute value in any XML file
	 * (e.g. inside {@code "schema.xsd"} in {@code href="schema.xsd"}) → resolves the
	 * relative reference to an absolute path using the document's own location.
	 *
	 * <p><b>Case 2:</b> Cursor anywhere in a grammar file (XSD, DTD, RNG, RNC) → returns
	 * the grammar file's own path so callers can find all XML files referencing it.
	 *
	 * @return absolute path of the target grammar, or {@code null} if the cursor is
	 *         not on a grammar reference or in a grammar file.
	 */
	private Path resolveTargetPath(DOMDocument document, Position position) {
		// Case 1: cursor on a grammar reference value — scan refs from the live DOM
		// and compare the cursor's line/character against each ref's cached range.
		TextDocument textDoc = document.getTextDocument();
		List<GrammarReference> refs = GrammarReferenceLocator.locate(textDoc);
		int line = position.getLine();
		int character = position.getCharacter();
		for (GrammarReference ref : refs) {
			if (line == ref.line() && character >= ref.startChar() && character <= ref.endChar()) {
				// Resolve the relative href to an absolute path
				Path docPath = FilesUtils.getPathOrNull(document.getDocumentURI());
				if (docPath != null && docPath.getParent() != null) {
					try {
						return docPath.getParent().resolve(ref.value()).normalize();
					} catch (Exception e) {
						// fall through: use filename-only lookup
					}
				}
				return null;
			}
		}
		// Case 2: cursor in a grammar file
		String uri = document.getDocumentURI();
		if (isGrammarFile(uri)) {
			return FilesUtils.getPathOrNull(uri);
		}
		return null;
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

	/**
	 * Extracts the filename portion from a URI, path, or bare filename.
	 *
	 * <p><b>Use case:</b> {@code fileNameOf("file:///workspace/schemas/order.xsd")}
	 * returns {@code "order.xsd"} — used to query the Bloom index by filename so
	 * the lookup works regardless of where in the workspace the grammar lives.
	 *
	 * @param path a URI, absolute path, or bare filename.
	 * @return the last path segment (after the last {@code /} or {@code \}).
	 */
	static String fileNameOf(String path) {
		int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
	}

	/**
	 * Returns {@code true} if {@code uri} points to a grammar file that can be
	 * the target of cross-file references (XSD, DTD, RNG, RNC).
	 *
	 * <p><b>Use case:</b> Both {@link WorkspaceIndexCodeLensParticipant} and the
	 * Case-2 path of {@link #findReference} call this to decide whether to activate
	 * on the current document. Opening {@code order.xml} → {@code false} (no lens,
	 * no Case-2 search). Opening {@code types.xsd} → {@code true}.
	 */
	public static boolean isGrammarFile(String uri) {
		if (uri == null) {
			return false;
		}
		String lower = uri.toLowerCase();
		return lower.endsWith(".xsd") || lower.endsWith(".dtd")
				|| lower.endsWith(".rng") || lower.endsWith(".rnc");
	}

}
