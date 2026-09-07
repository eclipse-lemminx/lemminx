/**
 *  Copyright (c) 2018 Angelo ZERR.
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
package org.eclipse.lemminx.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * TextDocument tests
 *
 */
public class TextDocumentTest {

	// Test with non incremental (with ListLineTracker)

	@Test
	public void testEmptyDocument() throws BadLocationException {
		TextDocument document = new TextDocument("", "");

		Position position = document.positionAt(0);
		assertEquals(0, position.getLine());
		assertEquals(0, position.getCharacter());

		position = new Position(0, 0);
		int offset = document.offsetAt(position);
		assertEquals(0, offset);

	}

	@Test
	public void testPositionAt() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\nefgh", "");

		Position position = document.positionAt(0);
		assertEquals(0, position.getLine());
		assertEquals(0, position.getCharacter());

		position = document.positionAt(4);
		assertEquals(0, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(5);
		assertEquals(1, position.getLine());
		assertEquals(0, position.getCharacter());

		position = document.positionAt(9);
		assertEquals(1, position.getLine());
		assertEquals(4, position.getCharacter());

		BadLocationException ex = null;
		try {
			position = document.positionAt(10);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	@Test
	public void testPositionAtEndLine() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\n", "");

		Position position = document.positionAt(4);
		assertEquals(0, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(5);
		assertEquals(1, position.getLine());
		assertEquals(0, position.getCharacter());

		BadLocationException ex = null;
		try {
			position = document.positionAt(6);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);

		document = new TextDocument("abcd\nefgh\n", "");

		position = document.positionAt(9);
		assertEquals(1, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(10);
		assertEquals(2, position.getLine());
		assertEquals(0, position.getCharacter());

		ex = null;
		try {
			position = document.positionAt(11);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	@Test
	public void testOffsetAt() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\nefgh", "");

		Position position = new Position(0, 0);
		int offset = document.offsetAt(position);
		assertEquals(0, offset);

		position = new Position(0, 4);
		offset = document.offsetAt(position);
		assertEquals(4, offset);

		position = new Position(1, 0);
		offset = document.offsetAt(position);
		assertEquals(5, offset);

		position = new Position(1, 4);
		offset = document.offsetAt(position);
		assertEquals(9, offset);

		BadLocationException ex = null;
		try {
			position = new Position(1, 5);
			document.offsetAt(position);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	// Test with incremental (with TreeLineTracker)

	@Test
	public void testEmptyDocumentInc() throws BadLocationException {
		TextDocument document = new TextDocument("", "");
		document.setIncremental(true);

		Position position = document.positionAt(0);
		assertEquals(0, position.getLine());
		assertEquals(0, position.getCharacter());

		position = new Position(0, 0);
		int offset = document.offsetAt(position);
		assertEquals(0, offset);

	}

	@Test
	public void testGetLineInformation() throws BadLocationException {
		assertThrows(BadLocationException.class, () -> {
			TextDocument document = new TextDocument("", "");
			document.setIncremental(true);
			Position position = new Position(-1, 0);
			document.offsetAt(position);
		});
	}

	@Test
	public void testPositionAtInc() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\nefgh", "");
		document.setIncremental(true);

		Position position = document.positionAt(0);
		assertEquals(0, position.getLine());
		assertEquals(0, position.getCharacter());

		position = document.positionAt(4);
		assertEquals(0, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(5);
		assertEquals(1, position.getLine());
		assertEquals(0, position.getCharacter());

		position = document.positionAt(9);
		assertEquals(1, position.getLine());
		assertEquals(4, position.getCharacter());

		BadLocationException ex = null;
		try {
			position = document.positionAt(10);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	@Test
	public void testPositionAtEndLineInc() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\n", "");
		document.setIncremental(true);

		Position position = document.positionAt(4);
		assertEquals(0, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(5);
		assertEquals(1, position.getLine());
		assertEquals(0, position.getCharacter());

		BadLocationException ex = null;
		try {
			position = document.positionAt(6);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);

		document = new TextDocument("abcd\nefgh\n", "");

		position = document.positionAt(9);
		assertEquals(1, position.getLine());
		assertEquals(4, position.getCharacter());

		position = document.positionAt(10);
		assertEquals(2, position.getLine());
		assertEquals(0, position.getCharacter());

		ex = null;
		try {
			position = document.positionAt(11);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	@Test
	public void testOffsetAtInc() throws BadLocationException {
		TextDocument document = new TextDocument("abcd\nefgh", "");
		document.setIncremental(true);

		Position position = new Position(0, 0);
		int offset = document.offsetAt(position);
		assertEquals(0, offset);

		position = new Position(0, 4);
		offset = document.offsetAt(position);
		assertEquals(4, offset);

		position = new Position(1, 0);
		offset = document.offsetAt(position);
		assertEquals(5, offset);

		position = new Position(1, 4);
		offset = document.offsetAt(position);
		assertEquals(9, offset);

		BadLocationException ex = null;
		try {
			position = new Position(1, 5);
			document.offsetAt(position);
		} catch (BadLocationException e) {
			ex = e;
		}
		assertNotNull(ex);
	}

	// CharSequence / StringBuilder tests

	@Test
	public void testGetTextSequenceReturnsContent() {
		TextDocument document = new TextDocument("hello world", "test.xml");
		CharSequence seq = document.getTextSequence();
		assertEquals("hello world", seq.toString());
		assertEquals(11, seq.length());
		assertEquals('h', seq.charAt(0));
		assertEquals('d', seq.charAt(10));
	}

	@Test
	public void testGetTextSequenceReturnsSameInstance() {
		TextDocument document = new TextDocument("abc", "test.xml");
		CharSequence seq1 = document.getTextSequence();
		CharSequence seq2 = document.getTextSequence();
		assertSame(seq1, seq2);
	}

	@Test
	public void testGetTextSequenceReflectsIncrementalUpdate() throws BadLocationException {
		TextDocument document = new TextDocument("<root></root>", "test.xml");
		document.setIncremental(true);

		TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
				new Range(new Position(0, 6), new Position(0, 6)), " attr=\"v\"");
		document.update(Collections.singletonList(change));

		assertEquals("<root> attr=\"v\"</root>", document.getTextSequence().toString());
	}

	@Test
	public void testGetTextLazySyncAfterIncrementalUpdate() throws BadLocationException {
		TextDocument document = new TextDocument("<a/>", "test.xml");
		document.setIncremental(true);

		TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
				new Range(new Position(0, 2), new Position(0, 2)), " x=\"1\"");
		document.update(Collections.singletonList(change));

		assertEquals("<a x=\"1\"/>", document.getTextSequence().toString());
		@SuppressWarnings("deprecation")
		String text = document.getText();
		assertEquals("<a x=\"1\"/>", text);
	}

	@Test
	public void testCharSequenceReaderReadsContent() throws IOException {
		CharSequence source = new StringBuilder("hello");
		CharSequenceReader reader = new CharSequenceReader(source);
		char[] buf = new char[10];
		int read = reader.read(buf, 0, 10);
		assertEquals(5, read);
		assertEquals("hello", new String(buf, 0, read));
		assertEquals(-1, reader.read(buf, 0, 10));
		reader.close();
	}

	@Test
	public void testCharSequenceReaderChunkedRead() throws IOException {
		CharSequence source = new StringBuilder("abcdef");
		CharSequenceReader reader = new CharSequenceReader(source);
		char[] buf = new char[3];
		assertEquals(3, reader.read(buf, 0, 3));
		assertEquals("abc", new String(buf));
		assertEquals(3, reader.read(buf, 0, 3));
		assertEquals("def", new String(buf));
		assertEquals(-1, reader.read(buf, 0, 3));
		reader.close();
	}

	@Test
	public void testCharSequenceReaderEmpty() throws IOException {
		CharSequenceReader reader = new CharSequenceReader("");
		char[] buf = new char[5];
		assertEquals(-1, reader.read(buf, 0, 5));
		reader.close();
	}

}
