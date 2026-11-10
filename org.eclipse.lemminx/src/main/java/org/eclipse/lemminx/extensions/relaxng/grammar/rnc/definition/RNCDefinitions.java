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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.definition;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.IRNCDocumentProvider;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCInclude;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCNode;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Provides go-to-definition support for RNC (RELAX NG Compact Syntax) define references.
 *
 * <p>Use case: the user invokes "Go to Definition" (Ctrl+Click or F12) on a define
 * reference, and the cursor navigates to the corresponding define declaration.</p>
 *
 * <p>Example: in the following RNC, invoking go-to-definition on {@code root} in
 * the {@code start} pattern navigates to the {@code root} define on line 2:</p>
 * <pre>
 * start = root                      &lt;-- Ctrl+Click on "root" here
 * root = element root { text }      &lt;-- navigates to "root" here
 * </pre>
 *
 * <p>The method returns a {@link LocationLink} with both the origin selection range
 * (the clicked reference) and the target range (the define name in the declaration),
 * enabling the IDE to highlight both ends of the link.</p>
 */
public class RNCDefinitions {

	private static final Logger LOGGER = Logger.getLogger(RNCDefinitions.class.getName());

	private final IRNCDocumentProvider documentProvider;

	public RNCDefinitions(IRNCDocumentProvider documentProvider) {
		this.documentProvider = documentProvider;
	}

	/**
	 * Finds the definition location for the word at the given cursor position.
	 *
	 * <p>The method extracts the word under the cursor, looks it up in the
	 * document's define declarations, and returns a location link from the
	 * reference to the definition.</p>
	 *
	 * @param textDocument  the RNC text document.
	 * @param position      the cursor position (typically the click position).
	 * @param cancelChecker used to check if the operation has been canceled.
	 * @return a singleton list with the {@link LocationLink} to the define
	 *         declaration, or an empty list if no matching define is found.
	 */
	public List<? extends LocationLink> findDefinition(RNCDocument rncDocument, Position position,
			CancelChecker cancelChecker) {
		try {
			TextDocument textDocument = rncDocument.getTextDocument();
			int offset = textDocument.offsetAt(position);
			CharSequence text = textDocument.getTextSequence();

			String word = RNCPositionUtils.getWordAt(text, offset);
			if (word == null || word.isEmpty()) {
				return Collections.emptyList();
			}

			// If cursor is on a define name inside an include body (override),
			// navigate to the original define in the included file
			RNCNode node = rncDocument.findNodeAt(offset);
			if (node instanceof RNCDefine && node.getParent() instanceof RNCInclude) {
				RNCDefine overrideDefine = (RNCDefine) node;
				if (word.equals(overrideDefine.getName())) {
					RNCInclude include = (RNCInclude) node.getParent();
					List<? extends LocationLink> result = findDefinitionInIncludedFile(
							textDocument, offset, word, include, rncDocument.getUri());
					if (!result.isEmpty()) {
						return result;
					}
				}
			}

			// Look up the define declaration matching this word in current document
			RNCDefine define = rncDocument.findDefine(word);
			if (define != null && define.getNameStart() >= 0) {
				Range originRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
				Range targetRange = RNCPositionUtils.createRange(textDocument, define.getNameStart(),
						define.getNameEnd());

				if (originRange != null && targetRange != null) {
					LocationLink link = new LocationLink();
					link.setTargetUri(textDocument.getUri());
					link.setTargetRange(targetRange);
					link.setTargetSelectionRange(targetRange);
					link.setOriginSelectionRange(originRange);
					return Collections.singletonList(link);
				}
			}

			// If not found locally, search in included files
			for (RNCInclude include : rncDocument.getIncludes()) {
				List<? extends LocationLink> result = findDefinitionInIncludedFile(
						textDocument, offset, word, include, rncDocument.getUri());
				if (!result.isEmpty()) {
					return result;
				}
			}
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Error computing RNC definition", e);
		}
		return Collections.emptyList();
	}

	private List<? extends LocationLink> findDefinitionInIncludedFile(
			TextDocument textDocument, int offset, String word,
			RNCInclude include, String documentUri) throws BadLocationException {
		String resolvedUri = RNCIncludeUtils.resolveUri(include, documentUri);
		RNCDocument includedDoc = documentProvider.getDocument(resolvedUri);
		if (includedDoc == null) {
			return Collections.emptyList();
		}
		RNCDefine targetDefine = includedDoc.findDefine(word);
		if (targetDefine != null && targetDefine.getNameStart() >= 0) {
			Range originRange = RNCPositionUtils.getWordRange(textDocument, offset, word);
			TextDocument includedTextDoc = new TextDocument(
					includedDoc.getSource().toString(), resolvedUri);
			Range targetRange = RNCPositionUtils.createRange(includedTextDoc,
					targetDefine.getNameStart(), targetDefine.getNameEnd());
			if (originRange != null && targetRange != null) {
				LocationLink link = new LocationLink();
				link.setTargetUri(resolvedUri);
				link.setTargetRange(targetRange);
				link.setTargetSelectionRange(targetRange);
				link.setOriginSelectionRange(originRange);
				return Collections.singletonList(link);
			}
		}
		return Collections.emptyList();
	}
}
