/*******************************************************************************
* Copyright (c) 2022, 2023 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Utilities for {@link TextEdit}.
 * 
 * @author Angelo ZERR
 *
 */
public class TextEditUtils {

	private static final Logger LOGGER = Logger.getLogger(TextEditUtils.class.getName());

	/**
	 * Returns the {@link TextEdit} to insert the given expected content from the
	 * given range (from, to) of the given text document and null otherwise.
	 * 
	 * @param from            the range from.
	 * @param to              the range to.
	 * @param expectedContent the expected content.
	 * @param textDocument    the text document.
	 * 
	 * @return the {@link TextEdit} to insert the given expected content from the
	 *         given range (from, to) of the given text document and null otherwise.
	 */
	public static TextEdit createTextEditIfNeeded(int from, int to, String expectedContent, TextDocument textDocument) {
		String text = textDocument.getText();

		// Check if content from the range [from, to] is the same than expected content
		if (isMatchExpectedContent(from, to, expectedContent, text)) {
			// The expected content exists, no need to create a TextEdit
			return null;
		}

		// Insert the expected content.
		try {
			Position endPos = textDocument.positionAt(to);
			Position startPos = to == from ? endPos : textDocument.positionAt(from);
			Range range = new Range(startPos, endPos);
			return new TextEdit(range, expectedContent);
		} catch (BadLocationException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Returns true if the given content from the range [from, to] of the given text
	 * is the same than expected content and false otherwise.
	 * 
	 * @param from            the from range.
	 * @param to              the to range.
	 * @param expectedContent the expected content.
	 * @param text            the text document.
	 * 
	 * @return true if the given content from the range [from, to] of the given text
	 *         is the same than expected content and false otherwise.
	 */
	private static boolean isMatchExpectedContent(int from, int to, String expectedContent, String text) {
		if (expectedContent.length() == to - from) {
			int j = 0;
			for (int i = from; i < to; i++) {
				char c = text.charAt(i);
				if (expectedContent.charAt(j) != c) {
					return false;
				}
				j++;
			}
		} else {
			return false;
		}
		return true;
	}

	public static String applyEdits(TextDocument document, List<? extends TextEdit> edits) throws BadLocationException {
		String text = document.getText();
		Collections.sort(edits /* .map(getWellformedEdit) */, (a, b) -> {
			int diff = a.getRange().getStart().getLine() - b.getRange().getStart().getLine();
			if (diff == 0) {
				return a.getRange().getStart().getCharacter() - b.getRange().getStart().getCharacter();
			}
			return diff;
		});
		
		// Use StringBuilder for better memory efficiency, especially for large files
		// Pre-allocate capacity based on original text size to minimize reallocations
		StringBuilder result = new StringBuilder(text.length());
		int lastModifiedOffset = 0;
		
		for (TextEdit e : edits) {
			int startOffset = document.offsetAt(e.getRange().getStart());
			if (startOffset < lastModifiedOffset) {
				throw new Error("Overlapping edit");
			} else if (startOffset > lastModifiedOffset) {
				// Append unchanged text between edits
				result.append(text, lastModifiedOffset, startOffset);
			}
			if (e.getNewText() != null) {
				result.append(e.getNewText());
			}
			lastModifiedOffset = document.offsetAt(e.getRange().getEnd());
		}
		// Append remaining text after last edit
		result.append(text, lastModifiedOffset, text.length());
		return result.toString();
	}

	/**
	 * Returns the offset of the first whitespace that's found in the given range
	 * [leftLimit,to] from the left of the to, and leftLimit otherwise.
	 * 
	 * @param leftLimit the left limit range.
	 * @param to        the to range.
	 * 
	 * @return the offset of the first whitespace that's found in the given range
	 *         [leftLimit,to] from the left of the to, and leftLimit otherwise.
	 */
	public static int adjustOffsetWithLeftWhitespaces(int leftLimit, int to, String text) {
		if (to == 0) {
			return -1;
		}
		for (int i = to - 1; i >= leftLimit; i--) {
			char c = text.charAt(i);
			if (!Character.isWhitespace(c)) {
				// The current character is not a whitespace, return the offset of the character
				return i + 1;
			}
		}
		return leftLimit;
	}

	/**
	 * Creates a TextDocumentEdit object for the specified document and list of text edits
	 * 
	 * @param document Document to be changed
	 * @param textEdits a list of text edit changes
	 * @return A Text Dpcument Edit object
	 */
	public static TextDocumentEdit creatTextDocumentEdit(DOMDocument document, List<TextEdit> textEdits) {
		VersionedTextDocumentIdentifier projectVersionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(
				document.getDocumentURI(), document.getTextDocument().getVersion());
		return new TextDocumentEdit(projectVersionedTextDocumentIdentifier, textEdits);
	}
	
	public static WorkspaceEdit createWorkspaceEdit(List<Either<TextDocumentEdit, ResourceOperation>> documentChanges) {
		return new WorkspaceEdit(documentChanges);
	}
}
