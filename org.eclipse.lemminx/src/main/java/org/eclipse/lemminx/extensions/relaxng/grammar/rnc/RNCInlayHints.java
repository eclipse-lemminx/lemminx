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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides inlay hints for RNC documents.
 *
 * <p><b>Inherited start hint:</b> When an RNC file has an {@code include} without
 * its own {@code start} declaration — and the included file defines {@code start} —
 * an inlay hint is shown at the end of the include statement to make the inherited
 * start pattern visible inline.</p>
 *
 * <p>Example: given {@code include "base.rnc"} where {@code base.rnc} declares
 * {@code start = element order { ... }}, the editor shows:</p>
 * <pre>
 * include "base.rnc"  ← start = element order { … }
 * </pre>
 *
 * <p>This hint is suppressed when the include already overrides {@code start}
 * (i.e. {@code include "base.rnc" \{ start = ... \}}).</p>
 */
public class RNCInlayHints {

	/** Maximum label length before truncation with "…". */
	private static final int MAX_LABEL_LENGTH = 60;

	public List<InlayHint> getInlayHints(RNCDocument rncDocument, Range range) {
		// If the document already has an explicit start, no inherited-start hint needed.
		if (rncDocument.getStartDecl() != null) {
			return Collections.emptyList();
		}
		List<RNCInclude> includes = rncDocument.getIncludes();
		if (includes.isEmpty()) {
			return Collections.emptyList();
		}
		TextDocument textDocument = rncDocument.getTextDocument();
		if (textDocument == null) {
			return Collections.emptyList();
		}

		List<InlayHint> hints = new ArrayList<>();
		String docUri = rncDocument.getUri();

		for (RNCInclude include : includes) {
			// Skip includes that already override start in their body.
			if (include.hasBody() && include.getOverriddenNames().contains("start")) {
				continue;
			}
			String inheritedStart = resolveInheritedStart(docUri, include);
			if (inheritedStart == null) {
				continue;
			}
			// Position the hint at the end of the URI literal (closing quote).
			int uriEnd = include.getUriEnd();
			if (uriEnd < 0) {
				continue;
			}
			try {
				Position pos = textDocument.positionAt(uriEnd);
				String label = truncate(inheritedStart);
				InlayHint hint = new InlayHint(pos, Either.forLeft(label));
				hint.setKind(InlayHintKind.Type);
				hint.setPaddingLeft(true);
				hints.add(hint);
			} catch (BadLocationException e) {
				// offset out of range — skip
			}
		}
		return hints;
	}

	/**
	 * Resolves the {@code start} declaration text inherited from the included file.
	 * Returns {@code null} if the included file cannot be parsed or has no start.
	 */
	private static String resolveInheritedStart(String documentUri, RNCInclude include) {
		RNCDocument includedDoc = RNCIncludeUtils.resolveAndParse(include, documentUri);
		if (includedDoc == null) {
			return null;
		}
		RNCStart startDecl = includedDoc.getStartDecl();
		if (startDecl == null) {
			return null;
		}
		return startDecl.getText();
	}

	/** Truncates {@code text} to the first line and to at most {@code MAX_LABEL_LENGTH} chars. */
	private static String truncate(String text) {
		if (text == null) {
			return null;
		}
		// Collapse to first line
		int nl = text.indexOf('\n');
		if (nl >= 0) {
			text = text.substring(0, nl).stripTrailing() + " …"; // "…"
		}
		if (text.length() > MAX_LABEL_LENGTH) {
			text = text.substring(0, MAX_LABEL_LENGTH - 1) + "…";
		}
		return text;
	}
}
