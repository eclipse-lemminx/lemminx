/*******************************************************************************
* Copyright (c) 2022 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.services.format;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

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

		if (from == to) {
			// Insert the expected content.
			try {
				Position endPos = textDocument.positionAt(to);
				Position startPos = endPos;
				Range range = new Range(startPos, endPos);
				return new TextEdit(range, expectedContent);
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		int i = expectedContent.length() - 1;
		boolean matchExpectedContent = true;
		while (from >= 0) {
			char c = text.charAt(from);
			if (Character.isWhitespace(c)) {
				if (matchExpectedContent) {
					if (i < 0) {
						matchExpectedContent = false;
					} else {
						if (expectedContent.charAt(i) != c) {
							matchExpectedContent = false;
						}
						i--;
					}
				}
			} else {
				break;
			}
			from--;
		}
		from++;
		if (matchExpectedContent) {
			matchExpectedContent = to - from == expectedContent.length();
		}

		if (!matchExpectedContent) {
			try {
				Position endPos = textDocument.positionAt(to);
				Position startPos = to == from ? endPos : textDocument.positionAt(from);
				Range range = new Range(startPos, endPos);
				return new TextEdit(range, expectedContent);
			} catch (BadLocationException e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);
			}
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
		List<? extends TextEdit> sortedEdits = mergeSort(edits /* .map(getWellformedEdit) */, (a, b) -> {
			int diff = a.getRange().getStart().getLine() - b.getRange().getStart().getLine();
			if (diff == 0) {
				return a.getRange().getStart().getCharacter() - b.getRange().getStart().getCharacter();
			}
			return diff;
		});
		int lastModifiedOffset = 0;
		List<String> spans = new ArrayList<>();
		for (TextEdit e : sortedEdits) {
			int startOffset = document.offsetAt(e.getRange().getStart());
			if (startOffset < lastModifiedOffset) {
				throw new Error("Overlapping edit");
			} else if (startOffset > lastModifiedOffset) {
				spans.add(text.substring(lastModifiedOffset, startOffset));
			}
			if (e.getNewText() != null) {
				spans.add(e.getNewText());
			}
			lastModifiedOffset = document.offsetAt(e.getRange().getEnd());
		}
		spans.add(text.substring(lastModifiedOffset));
		return spans.stream() //
				.collect(Collectors.joining());
	}

	private static <T> List<T> mergeSort(List<T> data, Comparator<T> comparator) {
		if (data.size() <= 1) {
			// sorted
			return data;
		}
		int p = (data.size() / 2) | 0;
		List<T> left = data.subList(0, p);
		List<T> right = data.subList(p, data.size());

		mergeSort(left, comparator);
		mergeSort(right, comparator);

		int leftIdx = 0;
		int rightIdx = 0;
		int i = 0;
		while (leftIdx < left.size() && rightIdx < right.size()) {
			int ret = comparator.compare(left.get(leftIdx), right.get(rightIdx));
			if (ret <= 0) {
				// smaller_equal -> take left to preserve order
				data.set(i++, left.get(leftIdx++));
			} else {
				// greater -> take right
				data.set(i++, right.get(rightIdx++));
			}
		}
		while (leftIdx < left.size()) {
			data.set(i++, left.get(leftIdx++));
		}
		while (rightIdx < right.size()) {
			data.set(i++, right.get(rightIdx++));
		}
		return data;
	}

}
