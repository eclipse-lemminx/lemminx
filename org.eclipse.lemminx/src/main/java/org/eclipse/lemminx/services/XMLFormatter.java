/**
 *  Copyright (c) 2018 Angelo ZERR
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

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.services.extensions.XMLExtensionsRegistry;
import org.eclipse.lemminx.services.extensions.format.IFormatterParticipant;
import org.eclipse.lemminx.services.format.XMLFormatterDocumentOld;
import org.eclipse.lemminx.services.format.XMLFormatterDocument;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
 * XML formatter support.
 *
 */
class XMLFormatter {
	private static final Logger LOGGER = Logger.getLogger(XMLFormatter.class.getName());

	private final XMLExtensionsRegistry extensionsRegistry;

	public XMLFormatter(XMLExtensionsRegistry extensionsRegistry) {
		this.extensionsRegistry = extensionsRegistry;
	}

	/**
	 * Returns a List containing multiple TextEdits to remove, add,
	 * update spaces / indent.
	 *
	 * @param textDocument   document to perform formatting on
	 * @param range          specified range in which formatting will be done
	 * @param sharedSettings settings containing formatting preferences
	 * @return List containing a TextEdit with formatting changes
	 */
	public List<? extends TextEdit> format(DOMDocument xmlDocument, Range range, SharedSettings sharedSettings) {
		try {
			if (sharedSettings.getFormattingSettings().isLegacy()) {
				XMLFormatterDocumentOld formatterDocument = new XMLFormatterDocumentOld(xmlDocument.getTextDocument(),
						range, sharedSettings, getFormatterParticipants());
				return formatterDocument.format();
			}
			XMLFormatterDocument formatterDocument = new XMLFormatterDocument(xmlDocument, range,
					sharedSettings, getFormatterParticipants());
			List<? extends TextEdit> result =  formatterDocument.format();
			
			// For large files, merge all TextEdits into a single one to avoid OutOfMemory
			// This is more memory efficient as we don't keep thousands of TextEdit objects
			if (shouldMergeEdits(result, xmlDocument)) {
				String formatted = applyEdits(xmlDocument.getTextDocument(), result);
				Range editRange = range != null ? range : getFullDocumentRange(xmlDocument);
				return List.of(new TextEdit(editRange, formatted));
			}
			return result;
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, "Formatting failed due to BadLocation", e);
		}
		return null;
	}
	
	/**
	 * Determines if TextEdits should be merged into a single edit.
	 * Merging is beneficial for large files to reduce memory consumption.
	 *
	 * @param edits the list of text edits
	 * @param xmlDocument the XML document
	 * @return true if edits should be merged
	 */
	private boolean shouldMergeEdits(List<? extends TextEdit> edits, DOMDocument xmlDocument) {
		// Merge if there are many edits (> 1000) or if the document is large (> 100KB)
		int editCount = edits != null ? edits.size() : 0;
		int documentSize = xmlDocument.getTextDocument().getTextSequence().length();
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
		Position end = textDocument.positionAt(textDocument.getTextSequence().length());
		return new Range(start, end);
	}

	/**
	 * Returns list of {@link IFormatterParticipant}.
	 *
	 * @return list of {@link IFormatterParticipant}.
	 */
	private Collection<IFormatterParticipant> getFormatterParticipants() {
		return extensionsRegistry.getFormatterParticipants();
	}
}
