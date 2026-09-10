/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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

import java.util.ArrayList;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * IncrementalParsingTest
 */
public class IncrementalParsingTest {

	String textTemplate = "<a>\r\n" + //
			"\r\n" + //
			"\r\n" + //
			"\r\n" + //
			"\r\n" + //
			"\r\n" + //
			"\r\n" + //
			"\r\n";

	@Test
	public void testBasicChange() {
		String text = "<>\r\n" + // /// <-- inserting 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>\r\n";

		String expectedText = "<a>\r\n" + // /// <-- inserted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 1), new Position(0, 1));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "a");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());

	}

	@Test
	public void testBasicChangeWord() {
		String text = "<>\r\n" + // /// <-- inserting 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<aaa>\r\n" + // /// <-- inserted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 1), new Position(0, 1));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "aaa");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	@Test
	public void testChangeReplaceRange() {
		String text = "<zzz>\r\n" + // /// <-- inserting 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<aaa>\r\n" + // /// <-- inserted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 1), new Position(0, 4));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "aaa");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1509
	@Test
	public void testRangeLengthPreferredOverRangeEndPosition() {
		String text = "<zzz>\r\n" + //
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<aaa>\r\n" + //
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		// range.end is deliberately wrong (same as start, i.e. zero-length range);
		// rangeLength = 3 must take priority and replace "zzz".
		Range range1 = new Range(new Position(0, 1), new Position(0, 1));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "aaa");
		change1.setRangeLength(3);

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1509
	@Test
	public void testNullRangeLengthFallsBackToRangeEndPosition() {
		String text = "<zzz>\r\n" + //
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<aaa>\r\n" + //
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</aaa>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		// rangeLength is null (not set); length must be computed from range end - start.
		Range range1 = new Range(new Position(0, 1), new Position(0, 4));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "aaa");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1509
	@Test
	public void testNullRangeReplacesWholeDocument() {
		String text = "<aaa>\r\n" + //
				"  <b/>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<root/>";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		// range is null: the whole document is replaced.
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(expectedText);

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	// https://github.com/eclipse-lemminx/lemminx/issues/1674
	@Test
	public void testDeprecatedRangeLengthAllowsNull() {
		String text = "<aaa>\r\n" + //
				"  <b/>\r\n" + //
				"</aaa>\r\n";

		String expectedText = "<aaa/>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 4), new Position(2, 5));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "/");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());
	}

	@Test
	public void testBasicChangeMultipleChanges() {
		String text = "<>\r\n" + // // <-- inserting 'a' in tag name
				"  <b>\r\n" + //
				"  </>\r\n" + // // <-- inserting 'b' in tag name
				"</a>\r\n";

		String expectedText = "<a>\r\n" + // // <-- inserted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + // // <-- inserted 'b' in tag name
				"</a>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 1), new Position(0, 1));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "a");

		Range range2 = new Range(new Position(2, 4), new Position(2, 4));
		TextDocumentContentChangeEvent change2 = new TextDocumentContentChangeEvent(range2, "b");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		// The order they are added in is backwards with the largest offset being first
		changes.add(change2);
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());

	}

	@Test
	public void testBasicChangeMultipleChangesReplaceRange() {
		String text = "<zzz>\r\n" + // // <-- inserting 'a' in tag name
				"  <b>\r\n" + //
				"  </eee>\r\n" + // // <-- inserting 'b' in tag name
				"</a>\r\n";

		String expectedText = "<a>\r\n" + // // <-- inserted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + // // <-- inserted 'b' in tag name
				"</a>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 1), new Position(0, 4));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "a");

		Range range2 = new Range(new Position(2, 4), new Position(2, 7));
		TextDocumentContentChangeEvent change2 = new TextDocumentContentChangeEvent(range2, "b");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		// The order they are added in is backwards with the largest offset being first
		changes.add(change2);
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());

	}

	@Test
	public void testBasicDeletionChange() {
		String text = "<aa>\r\n" + // /// <-- deleting 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>\r\n";

		String expectedText = "<a>\r\n" + // /// <-- deleted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 2), new Position(0, 3));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());

	}

	@Test
	public void testMultipleDeletionChanges() {
		String text = "<aa>\r\n" + // /// <-- deleting 'a' in tag name
				"  <b>\r\n" + //
				"  </bb>\r\n" + //
				"</a>\r\n";

		String expectedText = "<a>\r\n" + // /// <-- deleted 'a' in tag name
				"  <b>\r\n" + //
				"  </b>\r\n" + //
				"</a>\r\n";

		TextDocument document = new TextDocument(text, "uri");
		document.setIncremental(true);

		Range range1 = new Range(new Position(0, 2), new Position(0, 3));
		TextDocumentContentChangeEvent change1 = new TextDocumentContentChangeEvent(range1, "");

		Range range2 = new Range(new Position(2, 5), new Position(2, 6));
		TextDocumentContentChangeEvent change2 = new TextDocumentContentChangeEvent(range2, "");

		ArrayList<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(change2);
		changes.add(change1);

		document.update(changes);

		assertEquals(expectedText, document.getText());

	}
}