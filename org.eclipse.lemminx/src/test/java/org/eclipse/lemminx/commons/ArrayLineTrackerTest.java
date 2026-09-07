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
package org.eclipse.lemminx.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

public class ArrayLineTrackerTest {

	@Test
	public void emptyText() throws BadLocationException {
		ArrayLineTracker t = create("");
		assertEquals(1, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(0, t.getLineLength(0));
		assertNull(t.getLineDelimiter(0));
	}

	@Test
	public void singleLine() throws BadLocationException {
		ArrayLineTracker t = create("hello");
		assertEquals(1, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(5, t.getLineLength(0));
		assertNull(t.getLineDelimiter(0));

		assertEquals(0, t.getLineNumberOfOffset(0));
		assertEquals(0, t.getLineNumberOfOffset(4));
		assertEquals(0, t.getLineNumberOfOffset(5));
	}

	@Test
	public void twoLinesLF() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef");
		assertEquals(2, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(4, t.getLineLength(0));
		assertEquals("\n", t.getLineDelimiter(0));
		assertEquals(4, t.getLineOffset(1));
		assertEquals(3, t.getLineLength(1));
		assertNull(t.getLineDelimiter(1));

		assertEquals(0, t.getLineNumberOfOffset(0));
		assertEquals(0, t.getLineNumberOfOffset(3));
		assertEquals(1, t.getLineNumberOfOffset(4));
		assertEquals(1, t.getLineNumberOfOffset(7));
	}

	@Test
	public void twoLinesCRLF() throws BadLocationException {
		ArrayLineTracker t = create("abc\r\ndef");
		assertEquals(2, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(5, t.getLineLength(0));
		assertEquals("\r\n", t.getLineDelimiter(0));
		assertEquals(5, t.getLineOffset(1));
		assertEquals(3, t.getLineLength(1));
	}

	@Test
	public void trailingNewline() throws BadLocationException {
		ArrayLineTracker t = create("abc\n");
		assertEquals(2, t.getNumberOfLines());
		assertEquals(4, t.getLineOffset(1));
		assertEquals(0, t.getLineLength(1));
		assertNull(t.getLineDelimiter(1));
	}

	@Test
	public void positionAt() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		Position p = t.getPositionAt(0);
		assertEquals(0, p.getLine());
		assertEquals(0, p.getCharacter());

		p = t.getPositionAt(5);
		assertEquals(1, p.getLine());
		assertEquals(1, p.getCharacter());

		p = t.getPositionAt(11);
		assertEquals(2, p.getLine());
		assertEquals(3, p.getCharacter());
	}

	@Test
	public void offsetAt() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		assertEquals(0, t.getOffsetAt(new Position(0, 0)));
		assertEquals(3, t.getOffsetAt(new Position(0, 3)));
		assertEquals(4, t.getOffsetAt(new Position(1, 0)));
		assertEquals(8, t.getOffsetAt(new Position(2, 0)));
	}

	@Test
	public void replaceInsertCharNoDelimiter() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef");
		t.replace(1, 0, "X");
		assertEquals(2, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(5, t.getLineLength(0));
		assertEquals(5, t.getLineOffset(1));
		assertEquals(3, t.getLineLength(1));
	}

	@Test
	public void replaceInsertNewline() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef");
		t.replace(5, 0, "\n");
		assertEquals(3, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(4, t.getLineLength(0));
		assertEquals(4, t.getLineOffset(1));
		assertEquals(2, t.getLineLength(1));
		assertEquals("\n", t.getLineDelimiter(1));
		assertEquals(6, t.getLineOffset(2));
		assertEquals(2, t.getLineLength(2));
		assertNull(t.getLineDelimiter(2));
	}

	@Test
	public void replaceDeleteNewline() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef");
		t.replace(3, 1, "");
		assertEquals(1, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(6, t.getLineLength(0));
		assertNull(t.getLineDelimiter(0));
	}

	@Test
	public void replaceDeleteMultipleLines() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi\njkl");
		// Delete "ef\nghi" at offset 5, length 6 → "abc\nd\njkl"
		t.replace(5, 6, "");
		assertEquals(3, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(4, t.getLineLength(0));
		assertEquals(4, t.getLineOffset(1));
		assertEquals(2, t.getLineLength(1));
		assertEquals(6, t.getLineOffset(2));
		assertEquals(3, t.getLineLength(2));
	}

	@Test
	public void replaceWithMultipleNewlines() throws BadLocationException {
		ArrayLineTracker t = create("abcdef");
		t.replace(2, 2, "X\nY\nZ");
		assertEquals(3, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(4, t.getLineLength(0));
		assertEquals("\n", t.getLineDelimiter(0));
		assertEquals(4, t.getLineOffset(1));
		assertEquals(2, t.getLineLength(1));
		assertEquals("\n", t.getLineDelimiter(1));
		assertEquals(6, t.getLineOffset(2));
		assertEquals(3, t.getLineLength(2));
		assertNull(t.getLineDelimiter(2));
	}

	@Test
	public void replaceEntireContent() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef");
		t.replace(0, 7, "x\ny\nz");
		assertEquals(3, t.getNumberOfLines());
		assertEquals(0, t.getLineOffset(0));
		assertEquals(2, t.getLineLength(0));
		assertEquals(2, t.getLineOffset(1));
		assertEquals(2, t.getLineLength(1));
		assertEquals(4, t.getLineOffset(2));
		assertEquals(1, t.getLineLength(2));
	}

	@Test
	public void computeNumberOfLines() {
		ArrayLineTracker t = create("");
		assertEquals(0, t.computeNumberOfLines(""));
		assertEquals(0, t.computeNumberOfLines("abc"));
		assertEquals(1, t.computeNumberOfLines("abc\n"));
		assertEquals(2, t.computeNumberOfLines("abc\ndef\n"));
		assertEquals(1, t.computeNumberOfLines("\r\n"));
		assertEquals(2, t.computeNumberOfLines("\r\n\r\n"));
	}

	@Test
	public void getNumberOfLinesRange() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		assertEquals(1, t.getNumberOfLines(0, 0));
		assertEquals(1, t.getNumberOfLines(0, 3));
		assertEquals(2, t.getNumberOfLines(0, 4));
		assertEquals(3, t.getNumberOfLines(0, 11));
	}

	@Test
	public void lineInformationOfOffset() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		Line line = t.getLineInformationOfOffset(0);
		assertEquals(0, line.offset);
		assertEquals(3, line.length);

		line = t.getLineInformationOfOffset(5);
		assertEquals(4, line.offset);
		assertEquals(3, line.length);
	}

	@Test
	public void lineInformation() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		Line line = t.getLineInformation(0);
		assertEquals(0, line.offset);
		assertEquals(3, line.length);

		line = t.getLineInformation(1);
		assertEquals(4, line.offset);
		assertEquals(3, line.length);

		line = t.getLineInformation(2);
		assertEquals(8, line.offset);
		assertEquals(3, line.length);
	}

	@Test
	public void badOffsetThrows() {
		ArrayLineTracker t = create("abc");
		assertThrows(BadLocationException.class, () -> t.getLineNumberOfOffset(-1));
		assertThrows(BadLocationException.class, () -> t.getLineNumberOfOffset(4));
		assertThrows(BadLocationException.class, () -> t.getLineOffset(-1));
		assertThrows(BadLocationException.class, () -> t.getLineOffset(2));
	}

	@Test
	public void multipleSequentialReplacements() throws BadLocationException {
		ArrayLineTracker t = create("abc\ndef\nghi");
		// Insert "X" at offset 4 → "abc\nXdef\nghi"
		t.replace(4, 0, "X");
		assertEquals(3, t.getNumberOfLines());
		assertEquals(4, t.getLineOffset(1));
		// Insert "\n" at offset 5 → "abc\nX\ndef\nghi"
		t.replace(5, 0, "\n");
		assertEquals(4, t.getNumberOfLines());
		assertEquals(4, t.getLineOffset(1));
		assertEquals(6, t.getLineOffset(2));
		assertEquals(10, t.getLineOffset(3));
	}

	private static ArrayLineTracker create(String text) {
		ArrayLineTracker t = new ArrayLineTracker();
		t.set(text);
		return t;
	}
}
