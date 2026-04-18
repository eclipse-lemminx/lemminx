/**
 *  Copyright (c) 2026 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx.services;

import static org.eclipse.lemminx.utils.TextEditUtils.applyEdits;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.extensions.minify.XMLMinifierDocument;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
 * XML minifier support.
 *
 */
public class XMLMinifier {
	private static final Logger LOGGER = Logger.getLogger(XMLMinifier.class.getName());

	public XMLMinifier() {
	}

	/**
	 * Returns a List containing multiple TextEdits to remove spaces.
	 *
	 * @param xmlDocument    document to perform minification on
	 * @param range          specified range in which minification will be done
	 * @param sharedSettings settings containing minification preferences
	 * @return List containing TextEdit with minification changes
	 */
	public List<? extends TextEdit> minify(DOMDocument xmlDocument, Range range, SharedSettings sharedSettings) {
		try {
			XMLMinifierDocument minifierDocument = new XMLMinifierDocument(xmlDocument, range, sharedSettings);
			List<? extends TextEdit> result = minifierDocument.minify();

			// For large files, merge all TextEdits into a single one to avoid OutOfMemory
			// This is more memory efficient as we don't keep thousands of TextEdit objects
			if (shouldMergeEdits(result, xmlDocument)) {
				String minified = applyEdits(xmlDocument.getTextDocument(), result);
				Range editRange = range != null ? range : getFullDocumentRange(xmlDocument);
				return List.of(new TextEdit(editRange, minified));
			}
			return result;
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Minification failed due to BadLocation", e);
		}
		return null;
	}

	/**
	 * Determines if TextEdits should be merged into a single edit.
	 * Merging is beneficial for large files to reduce memory consumption.
	 *
	 * @param edits       the list of text edits
	 * @param xmlDocument the XML document
	 * @return true if edits should be merged
	 */
	private boolean shouldMergeEdits(List<? extends TextEdit> edits, DOMDocument xmlDocument) {
		// Merge if there are many edits (> 1000) or if the document is large (> 100KB)
		int editCount = edits != null ? edits.size() : 0;
		int documentSize = xmlDocument.getTextDocument().getText().length();
		return editCount > 1000 || documentSize > 100_000;
	}

	/**
	 * Returns the full document range.
	 *
	 * @param xmlDocument the XML document
	 * @return the full document range
	 * @throws BadLocationException if position calculation fails
	 */
	private Range getFullDocumentRange(DOMDocument xmlDocument) throws BadLocationException {
		TextDocument textDocument = xmlDocument.getTextDocument();
		Position start = new Position(0, 0);
		Position end = textDocument.positionAt(textDocument.getText().length());
		return new Range(start, end);
	}
}
