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

import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensParticipant;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensRequest;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Shows a "N references" code lens at the root element of grammar files
 * opened via the XML language service (XSD, DTD, RNG).
 *
 * <p>RNC files go through a separate text document service and therefore
 * cannot reach this participant. Their "N references" lens is added by the
 * RNC code lens provider, which calls the shared
 * {@link WorkspaceIndexCodeLensSupport#buildWorkspaceReferenceLens} directly.
 *
 * <p><b>Use case (UC19 — Browse backlinks from a grammar file):</b>
 * The user opens {@code order-schema.xsd}. The root element shows:
 * <pre>
 *   3 references
 *   &lt;xs:schema ...&gt;
 * </pre>
 * Clicking the lens opens the references panel listing all three XML files
 * and the exact attribute values that point to this grammar.
 *
 * <p><b>Prerequisite:</b> Code lens must be enabled ({@code xml.codeLens.enabled: true}
 * in VS Code settings). The workspace scan must have populated {@link WorkspaceIndex}.
 * If the scan is still running when the lens is first requested, a
 * {@code workspace/codeLens/refresh} is sent once it completes so the count updates.
 *
 * <p><b>Two-pass approach (delegated to {@link WorkspaceIndexCodeLensSupport}):</b>
 * <ol>
 *   <li>{@link WorkspaceIndex#findReferencingFiles} — Bloom filter O(n) pre-filter</li>
 *   <li>{@link GrammarReferenceLocator#locateReferencesTo} — exact count per candidate</li>
 * </ol>
 *
 * @see WorkspaceIndexCodeLensSupport
 * @see WorkspaceIndexReferenceParticipant
 * @see WorkspaceIndex
 */
public class WorkspaceIndexCodeLensParticipant implements ICodeLensParticipant {

	private final WorkspaceIndex workspaceIndex;

	public WorkspaceIndexCodeLensParticipant(WorkspaceIndex workspaceIndex) {
		this.workspaceIndex = workspaceIndex;
	}

	/**
	 * Emits a "N references" lens on the root element of grammar files.
	 *
	 * <p>Use case: opening {@code types.xsd} when two XML files reference it via
	 * {@code xsi:noNamespaceSchemaLocation="types.xsd"} → shows "2 references".
	 * Clicking triggers {@code textDocument/references} at the root element position,
	 * which {@link WorkspaceIndexReferenceParticipant} resolves to all referencing
	 * locations across the workspace.
	 *
	 * <p>The lens range is the start tag of the DOM root element (or {@code (0,0)} as
	 * fallback). RNC uses {@code (0,0)} always since no DOM is available there.
	 */
	@Override
	public void doCodeLens(ICodeLensRequest request, List<CodeLens> lenses, CancelChecker cancelChecker) {
		DOMDocument document = request.getDocument();
		String uri = document.getDocumentURI();
		if (!WorkspaceIndexReferenceParticipant.isGrammarFile(uri)) {
			return;
		}
		Range range = rootRange(document);
		boolean supportedByClient = request.isSupportedByClient(CodeLensKind.References);
		CodeLens lens = WorkspaceIndexCodeLensSupport.buildWorkspaceReferenceLens(
				workspaceIndex, uri, range, supportedByClient, cancelChecker);
		if (lens != null) {
			lenses.add(lens);
		}
	}

	/**
	 * Returns a range covering the root element's start tag, used as the anchor
	 * for the workspace reference lens in XML-based grammar files.
	 *
	 * <p>Falls back to {@code (0,0)–(0,0)} when the document has no root element.
	 * RNC callers always use {@code (0,0)} since they have no DOM.
	 */
	private static Range rootRange(DOMDocument document) {
		DOMElement root = document.getDocumentElement();
		if (root != null) {
			String tagName = root.getTagName();
			int len = tagName != null ? tagName.length() + 1 : 1; // +1 for '<'
			// Start at root.getStart()+1 (first char of tag name, e.g. 'x' in '<xs:schema').
			// DOMContainerNode.findNodeAt uses "offset <= child.getStart()" to break, so
			// findNodeAt(root.getStart()) returns the DOCUMENT node instead of the element.
			// Using +1 ensures the reference command position lands inside the element,
			// preventing XSDReferenceParticipant from treating it as a document-level query
			// and returning all internal XSD cross-references.
			int tagStart = root.getStart() + 1;
			Range range = XMLPositionUtility.createRange(tagStart, root.getStart() + len, document);
			if (range != null) {
				return range;
			}
		}
		Position zero = new Position(0, 0);
		return new Range(zero, zero);
	}
}
