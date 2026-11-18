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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.services.extensions.IGrammarReferenceContributor;
import org.eclipse.lemminx.services.extensions.IIndexFileScannerContributor;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;

/**
 * Computes a {@link WorkspaceEdit} that updates all grammar file references
 * across the workspace when a grammar file is renamed.
 *
 * <p><b>Use case (UC14–UC18):</b> The user renames {@code schema.xsd} to
 * {@code types.xsd} in the IDE. All XML files that reference {@code schema.xsd}
 * via {@code <?xml-model href="schema.xsd"?>}, {@code xsi:schemaLocation},
 * {@code xsi:noNamespaceSchemaLocation}, {@code <!DOCTYPE ... SYSTEM "...">},
 * or {@code xs:include schemaLocation} must be updated automatically.
 *
 * <p><b>Integration point:</b> Called from the {@code workspace/willRenameFiles}
 * handler. The LSP client sends the old and new URIs before the actual rename;
 * this class computes the edits, the client applies them, then renames the file.
 *
 * <p><b>Flow:</b>
 * <pre>
 *   workspace/willRenameFiles("schema.xsd" → "types.xsd")
 *     │
 *     ├─ 1. WorkspaceIndex.getReferencesTo("schema.xsd")
 *     │     → Returns only confirmed referencing files from the in-memory index
 *     │     → Zero file I/O — {@link GrammarReference} line/column positions
 *     │        were computed once at indexing time and cached in {@link FileBloomEntry}
 *     │
 *     ├─ 2. For each confirmed file:
 *     │     → If the file is OPEN in the editor (live DOM may differ from disk):
 *     │        re-scan from the in-memory content to get accurate positions
 *     │     → If the file is CLOSED: use the cached line/column positions directly
 *     │
 *     └─ 3. Build WorkspaceEdit with TextEdits replacing old path with new path
 *           → preserves relative paths (e.g. "../schemas/schema.xsd" → "../schemas/types.xsd")
 * </pre>
 *
 * @see WorkspaceIndex
 * @see GrammarReferenceLocator
 * @see GrammarReference
 */
public class GrammarFileRenameSupport {

	/**
	 * Computes a {@link WorkspaceEdit} updating all references to a renamed grammar file.
	 *
	 * <p><b>Use case:</b> The user renames {@code schemas/order.xsd} to
	 * {@code schemas/order-v2.xsd}. The IDE calls {@code workspace/willRenameFiles}
	 * before the rename happens. This method:
	 * <ol>
	 *   <li>Retrieves only the confirmed referencing files from the in-memory index —
	 *       zero file I/O for closed files.</li>
	 *   <li>For files that are <em>open in the editor</em>, re-scans from the live
	 *       in-memory content (via {@code openDocumentContent}) because the user may
	 *       have made edits that shift line numbers since the last index update.</li>
	 *   <li>Skips references that resolve to a different file (e.g. another
	 *       {@code order.xsd} in a sibling directory).</li>
	 *   <li>Builds a TextEdit per reference, updating relative paths correctly
	 *       (e.g. {@code "../schemas/order.xsd"} → {@code "../schemas/order-v2.xsd"})</li>
	 * </ol>
	 *
	 * @param index               populated workspace Bloom index.
	 * @param oldPath             absolute path of the grammar file before the rename.
	 * @param newPath             absolute path of the grammar file after the rename.
	 * @param openDocumentContent function mapping a file URI to its current in-memory
	 *                            content (for open editor tabs), or returning {@code null}
	 *                            if the file is not open. Pass {@code null} to disable
	 *                            open-document re-scan (cached positions are used for all files).
	 * @return a {@link WorkspaceEdit} with edits for all referencing files,
	 *         or {@code null} if no references are found.
	 */
	/** Convenience overload — no open-document re-scan (uses cached positions for all files). */
	public static WorkspaceEdit computeRenameEdits(WorkspaceIndex index, Path oldPath, Path newPath) {
		return computeRenameEdits(index, oldPath, newPath, null);
	}

	public static WorkspaceEdit computeRenameEdits(WorkspaceIndex index,
			Path oldPath, Path newPath, Function<String, String> openDocumentContent) {
		// getReferencesTo returns only confirmed refs from the in-memory index — zero I/O.
		Map<String, List<GrammarReference>> confirmedRefs = index.getReferencesTo(oldPath);
		if (confirmedRefs.isEmpty()) {
			return null;
		}

		Map<String, List<TextEdit>> changes = new HashMap<>();

		for (Map.Entry<String, List<GrammarReference>> e : confirmedRefs.entrySet()) {
			String candidateUri = e.getKey();
			Path candidatePath = FilesUtils.getPathOrNull(candidateUri);
			if (candidatePath == null) {
				continue;
			}

			List<GrammarReference> refs = resolveCurrentRefs(
					candidateUri, candidatePath, oldPath, e.getValue(),
					openDocumentContent, index.getContributors(), index.getFileScannerContributors());

			if (refs.isEmpty()) {
				continue;
			}

			List<TextEdit> edits = new ArrayList<>();
			for (GrammarReference ref : refs) {
				String newValue = computeNewValue(ref.value(), newPath, candidatePath);
				Range range = new Range(
						new Position(ref.line(), ref.startChar()),
						new Position(ref.line(), ref.endChar()));
				edits.add(new TextEdit(range, newValue));
			}

			if (!edits.isEmpty()) {
				changes.put(candidateUri, edits);
			}
		}

		if (changes.isEmpty()) {
			return null;
		}

		WorkspaceEdit workspaceEdit = new WorkspaceEdit();
		workspaceEdit.setChanges(changes);
		return workspaceEdit;
	}

	/**
	 * Returns the current grammar references from {@code uri} that resolve to
	 * {@code targetPath}, preferring fresh content over the cached bloom-index positions.
	 *
	 * <p><b>Preference order:</b>
	 * <ol>
	 *   <li>Live in-memory content — when the file is open in the editor with unsaved
	 *       changes (e.g. the user added lines before {@code <?xml-model?>}, shifting
	 *       the line number). {@code openDocContent} provides this content.</li>
	 *   <li>On-disk content — when the file is closed. Avoids returning stale cached
	 *       positions that may lag behind a recent save / {@code didChangeWatchedFiles}
	 *       re-index that has not yet completed.</li>
	 *   <li>Cached bloom-index positions — last resort, when the file cannot be read.</li>
	 * </ol>
	 *
	 * @param uri            URI of the candidate referencing file.
	 * @param candidatePath  absolute path of the candidate file (derived from {@code uri}).
	 * @param targetPath     absolute path of the grammar file being queried.
	 * @param cachedRefs     refs from the in-memory bloom index (may be stale).
	 * @param openDocContent function mapping a URI to live in-memory content, or
	 *                       {@code null} if unavailable.
	 * @param contributors   extension-provided grammar-reference contributors.
	 * @return current refs resolved to {@code targetPath}, or {@code cachedRefs} as fallback.
	 */
	static List<GrammarReference> resolveCurrentRefs(
			String uri, Path candidatePath, Path targetPath, List<GrammarReference> cachedRefs,
			Function<String, String> openDocContent,
			List<IGrammarReferenceContributor> contributors) {
		return resolveCurrentRefs(uri, candidatePath, targetPath, cachedRefs,
				openDocContent, contributors, Collections.emptyList());
	}

	static List<GrammarReference> resolveCurrentRefs(
			String uri, Path candidatePath, Path targetPath, List<GrammarReference> cachedRefs,
			Function<String, String> openDocContent,
			List<IGrammarReferenceContributor> contributors,
			List<IIndexFileScannerContributor> fileScannerContributors) {
		// Check if a file scanner contributor handles this path (e.g. RNC).
		// For non-XML files the XML-based GrammarReferenceLocator would return an empty
		// list, so we must delegate to the appropriate contributor instead.
		IIndexFileScannerContributor fileContributor = findFileContributor(candidatePath, fileScannerContributors);

		if (fileContributor != null) {
			// 1. Live content — the XML document provider does not track non-XML files,
			// but the contributor itself may via getOpenDocumentContent(). The
			// openDocContent function supplied by the caller is also checked first so
			// that callers can inject any additional content sources.
			String liveContent = null;
			if (openDocContent != null) {
				liveContent = openDocContent.apply(uri);
			}
			if (liveContent == null) {
				liveContent = fileContributor.getOpenDocumentContent(uri);
			}
			if (liveContent != null) {
				try {
					List<GrammarReference> allRefs = new ArrayList<>();
					fileContributor.scan(liveContent, candidatePath, allRefs, new HashSet<>());
					return GrammarReferenceLocator.locateReferencesTo(allRefs, targetPath, candidatePath);
				} catch (IOException ignored) {
					// fall through to on-disk
				}
			}
			// 2. On-disk content
			if (candidatePath != null) {
				try {
					List<GrammarReference> allRefs = new ArrayList<>();
					fileContributor.scan(candidatePath, Integer.MAX_VALUE, allRefs, new HashSet<>());
					return GrammarReferenceLocator.locateReferencesTo(allRefs, targetPath, candidatePath);
				} catch (IOException ignored) {
					// fall through to cached
				}
			}
			return cachedRefs != null ? cachedRefs : Collections.emptyList();
		}

		// 1. Live content (open document, may have unsaved edits)
		if (openDocContent != null) {
			String content = openDocContent.apply(uri);
			if (content != null) {
				return GrammarReferenceLocator.locateReferencesTo(
						new TextDocument(content, uri), targetPath, candidatePath, contributors);
			}
		}
		// 2. On-disk content (closed file — avoids stale cache during async re-index)
		if (candidatePath != null) {
			try {
				String diskContent = Files.readString(candidatePath);
				return GrammarReferenceLocator.locateReferencesTo(
						new TextDocument(diskContent, uri), targetPath, candidatePath, contributors);
			} catch (IOException ignored) {
				// fall through
			}
		}
		// 3. Cached bloom-index positions (file unreadable — last resort)
		return cachedRefs != null ? cachedRefs : Collections.emptyList();
	}

	private static IIndexFileScannerContributor findFileContributor(Path path,
			List<IIndexFileScannerContributor> contributors) {
		if (path == null || contributors == null || contributors.isEmpty()) {
			return null;
		}
		for (IIndexFileScannerContributor sc : contributors) {
			if (sc.canScan(path)) {
				return sc;
			}
		}
		return null;
	}

	private static String computeNewValue(String oldValue, Path newPath, Path referencingFile) {
		int hashIdx = oldValue.lastIndexOf('#');
		String fragment = hashIdx >= 0 ? oldValue.substring(hashIdx) : "";
		Path dir = referencingFile.getParent();
		if (dir != null) {
			try {
				Path relative = dir.relativize(newPath);
				return relative.toString().replace('\\', '/') + fragment;
			} catch (IllegalArgumentException e) {
				// Different roots — fall back to filename
			}
		}
		return newPath.getFileName().toString() + fragment;
	}

}
