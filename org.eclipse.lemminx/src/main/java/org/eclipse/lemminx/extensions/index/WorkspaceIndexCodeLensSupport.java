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

import org.eclipse.lemminx.services.extensions.codelens.ReferenceCommand;
import org.eclipse.lemminx.utils.FilesUtils;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Shared utility that builds the "N references" workspace code lens for grammar files
 * (XSD, DTD, RNG, RNC).
 *
 * <p>This class is called by both {@link WorkspaceIndexCodeLensParticipant} (for
 * grammar files opened via the XML language service) and directly by the RNC code
 * lens provider (which runs through a separate text document service and therefore
 * cannot go through the XML extension registry).
 *
 * <p><b>Use case:</b> The user opens {@code order.rnc} or {@code types.xsd}. The
 * code lens provider for that file type calls
 * {@link #buildWorkspaceReferenceLens} to create a "N references" lens at the
 * position of the first significant token. Clicking the lens triggers
 * {@code textDocument/references} → {@link WorkspaceIndexReferenceParticipant}.
 *
 * <p><b>Range:</b> Each caller supplies the lens {@link Range} because the
 * grammar AST differs per file type:
 * <ul>
 *   <li>XSD/RNG/DTD: range of the root DOM element's start tag</li>
 *   <li>RNC: position {@code (0,0)} (no DOM available)</li>
 * </ul>
 *
 * <p><b>No file I/O:</b> Reference counts come directly from the
 * {@link GrammarReference} list cached in each {@link FileBloomEntry} during
 * indexing. Files are never re-read during code lens computation.
 *
 * @see WorkspaceIndexCodeLensParticipant
 * @see GrammarReferenceLocator
 * @see WorkspaceIndex
 */
public class WorkspaceIndexCodeLensSupport {

	/**
	 * Builds a "N references" code lens for the grammar file identified by {@code uri}.
	 *
	 * <p><b>Use case:</b> {@code types.xsd} is referenced by three XML files in the
	 * workspace. The lens shows "3 references" at the position supplied by the caller.
	 * Clicking it opens the references panel.
	 *
	 * <p>Returns {@code null} when no workspace files reference this grammar, so the
	 * caller can omit it from the list without adding an empty lens.
	 *
	 * @param workspaceIndex    the populated Bloom index.
	 * @param uri               URI of the grammar file being viewed.
	 * @param range             position where the lens should appear (caller-supplied,
	 *                          differs per grammar file type).
	 * @param supportedByClient {@code true} if the client supports the
	 *                          {@code editor.action.showReferences} command.
	 * @param cancelChecker     unused — kept for API compatibility.
	 * @return a {@link CodeLens} with a {@link ReferenceCommand}, or {@code null}
	 *         if no references are found.
	 */
	public static CodeLens buildWorkspaceReferenceLens(WorkspaceIndex workspaceIndex,
			String uri, Range range, boolean supportedByClient, CancelChecker cancelChecker) {
		return buildWorkspaceReferenceLens(workspaceIndex, uri, range, supportedByClient, false, cancelChecker);
	}

	/**
	 * Builds a "N references" code lens for the grammar file identified by {@code uri},
	 * optionally excluding grammar-to-grammar references from the count.
	 *
	 * <p><b>Use case — {@code excludeGrammarFiles = true}:</b> A catalog entry
	 * {@code <uri name="http://foo" uri="schema.xsd"/>} should only count XML
	 * instance documents, not other XSD files that import {@code schema.xsd}.
	 * Passing {@code true} filters out XSD/DTD/RNG/RNC/XSL/XSLT candidates.
	 *
	 * @param excludeGrammarFiles when {@code true}, grammar files are excluded from
	 *        the reference count — used by catalog code lenses.
	 */
	public static CodeLens buildWorkspaceReferenceLens(WorkspaceIndex workspaceIndex,
			String uri, Range range, boolean supportedByClient, boolean excludeGrammarFiles,
			CancelChecker cancelChecker) {
		Path targetPath = FilesUtils.getPathOrNull(uri);
		int totalRefs = workspaceIndex.countReferencesTo(targetPath, excludeGrammarFiles);
		if (totalRefs == 0) {
			return null;
		}
		Position position = range != null ? range.getStart() : new Position(0, 0);
		ReferenceCommand command = new ReferenceCommand(uri, position, supportedByClient);
		for (int i = 1; i < totalRefs; i++) {
			command.increment();
		}
		Range lensRange = range != null ? range : new Range(new Position(0, 0), new Position(0, 0));
		CodeLens codeLens = new CodeLens(lensRange);
		codeLens.setCommand(command);
		return codeLens;
	}

	/**
	 * Builds a "N references" code lens for a catalog entry pointing to the grammar at {@code uri}.
	 *
	 * <p>Only XML instance files that reference the grammar <em>via the catalog identifier</em>
	 * are counted. Direct references to the grammar file by filename (e.g.
	 * {@code xsi:noNamespaceSchemaLocation="data2.xsd"}) are intentionally excluded because they
	 * bypass the catalog mapping and do not use the catalog entry.
	 *
	 * @param workspaceIndex    the populated Bloom index.
	 * @param catalogUri        URI of the catalog file containing the entry. Used as the target
	 *                          for the {@link ReferenceCommand} so that clicking the lens triggers
	 *                          {@code textDocument/references} on the catalog file.
	 * @param catalogIdentifier the catalog key attribute (systemId / publicId / URI name).
	 * @param range             position where the lens should appear.
	 * @param supportedByClient {@code true} if the client supports the references command.
	 * @param cancelChecker     unused — kept for API compatibility.
	 * @return a {@link CodeLens} with a {@link ReferenceCommand}, or {@code null} if no references
	 *         are found.
	 */
	public static CodeLens buildCatalogWorkspaceReferenceLens(WorkspaceIndex workspaceIndex,
			String catalogUri, String catalogIdentifier, Range range, boolean supportedByClient,
			CancelChecker cancelChecker) {
		if (catalogIdentifier == null) {
			return null;
		}
		int total = workspaceIndex.countFilesWithRefValue(catalogIdentifier, true);
		if (total == 0) {
			return null;
		}
		Position position = range != null ? range.getStart() : new Position(0, 0);
		ReferenceCommand command = new ReferenceCommand(catalogUri, position, supportedByClient);
		for (int i = 1; i < total; i++) {
			command.increment();
		}
		Range lensRange = range != null ? range : new Range(new Position(0, 0), new Position(0, 0));
		CodeLens codeLens = new CodeLens(lensRange);
		codeLens.setCommand(command);
		return codeLens;
	}

}
