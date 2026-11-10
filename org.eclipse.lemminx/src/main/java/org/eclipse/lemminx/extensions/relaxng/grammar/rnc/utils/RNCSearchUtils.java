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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.RNCDefine;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCPositionUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCScanner;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCTokenType;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

/**
 * Utility methods for searching RNC (RELAX NG Compact Syntax) documents.
 *
 * <p>Use case: finding all references to a define name across the document
 * for "Go to References" and "Find All References" LSP operations.</p>
 *
 * <p>Example: given this RNC document:</p>
 * <pre>
 * start = root
 * root = element root {
 *   inline*
 * }
 * inline = element span { text }
 * </pre>
 * <p>Finding references for {@code root} returns: the reference at {@code start = root}
 * (and optionally the declaration at {@code root = element root}).</p>
 */
public class RNCSearchUtils {

	private RNCSearchUtils() {
	}

	/**
	 * Finds all pattern-usage references to a given define in the RNC document.
	 *
	 * <p>Scans the document tokens and identifies identifiers that match the
	 * define name, excluding:</p>
	 * <ul>
	 *   <li>Element/attribute names (identifiers preceded by {@code element} or
	 *       {@code attribute} keywords)</li>
	 *   <li>All assignment forms ({@code =}, {@code |=}, {@code &=}) — these are
	 *       declarations, not pattern usages. This keeps unused-define detection in
	 *       the validator correct: a define with only extends but no pattern usage
	 *       is still considered unused.</li>
	 *   <li>The define's own declaration position (unless {@code includeDeclaration}
	 *       is {@code true})</li>
	 * </ul>
	 *
	 * <p>Callers that need to include extend declarations (e.g. code lens, rename)
	 * must collect same-name defines from {@code rncDocument.getDefines()} separately.</p>
	 *
	 * @param textDocument       the RNC text document.
	 * @param define             the define whose references to find.
	 * @param includeDeclaration whether to include the define's own declaration
	 *                           in the results.
	 * @return a list of {@link Location} objects for each pattern-usage reference found.
	 */
	public static List<Location> findReferences(TextDocument textDocument,
			RNCDefine define, boolean includeDeclaration) {
		List<Location> locations = new ArrayList<>();
		String defineName = define.getName();
		if (defineName == null || defineName.equals("start")) {
			return locations;
		}
		String uri = textDocument.getUri();

		if (includeDeclaration) {
			Range defRange = RNCPositionUtils.createRange(textDocument, define.getNameStart(),
					define.getNameEnd());
			if (defRange != null) {
				locations.add(new Location(uri, defRange));
			}
		}

		CharSequence text = textDocument.getTextSequence();
		RNCScanner scanner = RNCScanner.createScanner(text);
		List<RNCTokenType> types = new ArrayList<>();
		List<int[]> positions = new ArrayList<>();

		RNCTokenType token = scanner.scan();
		while (token != RNCTokenType.EOS) {
			if (token != RNCTokenType.Whitespace && token != RNCTokenType.Comment
					&& token != RNCTokenType.DocumentationComment) {
				types.add(token);
				if (token == RNCTokenType.Identifier) {
					positions.add(new int[] { scanner.getTokenOffset(), scanner.getTokenLength() });
				} else {
					positions.add(null);
				}
			}
			token = scanner.scan();
		}

		for (int i = 0; i < types.size(); i++) {
			if (types.get(i) != RNCTokenType.Identifier || positions.get(i) == null) {
				continue;
			}
			int tokOffset = positions.get(i)[0];
			int tokLen = positions.get(i)[1];
			if (tokLen != defineName.length()) {
				continue;
			}
			boolean match = true;
			for (int j = 0; j < tokLen; j++) {
				if (text.charAt(tokOffset + j) != defineName.charAt(j)) {
					match = false;
					break;
				}
			}
			if (!match) {
				continue;
			}
			if (tokOffset == define.getNameStart()) {
				continue;
			}
			RNCTokenType prev = (i > 0) ? types.get(i - 1) : null;
			if (prev == RNCTokenType.KeywordElement || prev == RNCTokenType.KeywordAttribute) {
				continue;
			}
			RNCTokenType next = (i + 1 < types.size()) ? types.get(i + 1) : null;
			if (next == RNCTokenType.Assign || next == RNCTokenType.AssignChoice
					|| next == RNCTokenType.AssignInterleave) {
				continue;
			}
			Range refRange = RNCPositionUtils.createRange(textDocument, tokOffset, tokOffset + tokLen);
			if (refRange != null) {
				locations.add(new Location(uri, refRange));
			}
		}
		return locations;
	}
}
