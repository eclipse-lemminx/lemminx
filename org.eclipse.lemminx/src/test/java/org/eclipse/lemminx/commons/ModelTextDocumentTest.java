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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModelTextDocument} incremental edit support.
 */
public class ModelTextDocumentTest {

	@Test
	public void editInfoCapturedBeforeTextUpdate() {
		ModelTextDocument<String> doc = createDoc("<root>text</root>");
		doc.getModel();

		TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
				new Range(new Position(0, 10), new Position(0, 10)), "X");
		doc.update(Collections.singletonList(change));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		assertEquals(10, edit.getStartOffset());
		assertEquals(0, edit.getDeleteLength());
		assertEquals(1, edit.getInsertLength());
	}

	@Test
	public void previousIncrementalDataPreservedOnCancel() {
		ModelTextDocument<String> doc = createDoc("<root/>");
		String firstModel = doc.getModel();
		assertNotNull(firstModel);

		doc.setText("<root>changed</root>");

		Object prev = doc.getPreviousIncrementalData();
		assertNotNull(prev);
		assertEquals(firstModel, prev);
	}

	@Test
	public void pendingEditClearedAfterGetModel() {
		ModelTextDocument<String> doc = createDoc("<root>text</root>");
		doc.getModel();

		TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent(
				new Range(new Position(0, 10), new Position(0, 10)), "X");
		doc.update(Collections.singletonList(change));
		assertNotNull(doc.getPendingEdit());

		doc.getModel();

		assertNull(doc.getPendingEdit());
		assertNull(doc.getPreviousIncrementalData());
	}

	@Test
	public void multipleChangesNullifyPendingEdit() {
		ModelTextDocument<String> doc = createDoc("<root>text</root>");
		doc.getModel();

		TextDocumentContentChangeEvent change1 = createChange(0, 6, 0, 10, "newtext");
		TextDocumentContentChangeEvent change2 = createChange(0, 0, 0, 0, "X");
		doc.update(Arrays.asList(change1, change2));

		assertNull(doc.getPendingEdit());
	}

	@Test
	public void fullDocumentChangeNullifyPendingEdit() {
		ModelTextDocument<String> doc = createDoc("<root/>");
		doc.getModel();

		TextDocumentContentChangeEvent change = new TextDocumentContentChangeEvent("<new/>");
		doc.update(Collections.singletonList(change));

		assertNull(doc.getPendingEdit());
	}

	@Test
	public void doubleCancelModelPreservesFirstIncrementalData() {
		ModelTextDocument<String> doc = createDoc("<root/>");
		String firstModel = doc.getModel();
		assertNotNull(firstModel);

		doc.setText("<root>changed1</root>");
		Object prevAfterFirst = doc.getPreviousIncrementalData();
		assertEquals(firstModel, prevAfterFirst);

		// Second setText without getModel() in between — model is already null,
		// so cancelModel() does NOT overwrite previousIncrementalData
		doc.setText("<root>changed2</root>");
		Object prevAfterSecond = doc.getPreviousIncrementalData();
		assertEquals(firstModel, prevAfterSecond);
	}

	@Test
	public void noExtractorMeansNoPreviousIncrementalData() {
		ModelTextDocument<String> doc = new ModelTextDocument<>("<root/>", "test://test.xml",
				(document, cancelChecker) -> document.getText());
		doc.setIncremental(true);
		String firstModel = doc.getModel();
		assertNotNull(firstModel);

		doc.setText("<root>changed</root>");

		assertNull(doc.getPreviousIncrementalData());
	}

	@Test
	public void customExtractorStoresTransformedData() {
		ModelTextDocument<String> doc = new ModelTextDocument<>("<root/>", "test://test.xml",
				(document, cancelChecker) -> document.getText(),
				s -> s.length());
		doc.setIncremental(true);
		String firstModel = doc.getModel();
		assertNotNull(firstModel);

		doc.setText("<root>changed</root>");

		Object prev = doc.getPreviousIncrementalData();
		assertNotNull(prev);
		assertEquals(firstModel.length(), prev);
	}

	@Test
	public void doubleCancelWithoutExtractorRemainsNull() {
		ModelTextDocument<String> doc = new ModelTextDocument<>("<root/>", "test://test.xml",
				(document, cancelChecker) -> document.getText());
		doc.setIncremental(true);
		doc.getModel();

		doc.setText("<root>changed1</root>");
		assertNull(doc.getPreviousIncrementalData());

		doc.setText("<root>changed2</root>");
		assertNull(doc.getPreviousIncrementalData());
	}

	// --- Edit merge tests ---

	@Test
	public void mergeEditsAfterDirtyRegion() {
		// T0 = "AABBCCDD", edit1 replaces "BB" with "YYY", edit2 replaces second "C" with "Z"
		ModelTextDocument<String> doc = createDoc("AABBCCDD");
		doc.getModel();

		// Edit 1: replace "BB" (pos 2, len 2) with "YYY" → "AAYYYCCDD"
		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 2, 0, 4, "YYY")));

		// Edit 2: replace second "C" (pos 6 in T1, len 1) with "Z" → "AAYYYCZDD"
		doc.setVersion(2);
		doc.update(Collections.singletonList(createChange(0, 6, 0, 7, "Z")));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		// Merged against T0: dirty region is [2, 6), so delete "BBCC" (4 chars), insert "YYYCZ" (5 chars)
		assertEquals(2, edit.getStartOffset());
		assertEquals(4, edit.getDeleteLength());
		assertEquals(5, edit.getInsertLength());
	}

	@Test
	public void mergeEditsBeforeDirtyRegion() {
		// T0 = "AABBCCDD", edit1 at pos 4, edit2 at pos 1
		ModelTextDocument<String> doc = createDoc("AABBCCDD");
		doc.getModel();

		// Edit 1: replace "CC" (pos 4, len 2) with "YYY" → "AABBYYYТDD"
		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 4, 0, 6, "YYY")));

		// Edit 2: delete "A" at pos 1 (len 1) → "ABBYYYТDD"
		doc.setVersion(2);
		doc.update(Collections.singletonList(createChange(0, 1, 0, 2, "")));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		// Merged against T0: dirty region is [1, 6), delete "ABBCC" (5 chars), insert "BBYYY" (5 chars)
		assertEquals(1, edit.getStartOffset());
		assertEquals(5, edit.getDeleteLength());
		assertEquals(5, edit.getInsertLength());
	}

	@Test
	public void mergeEditsWithinDirtyRegion() {
		// T0 = "AABBCCDD", edit1 replaces "BB" with "YYY", edit2 within dirty region
		ModelTextDocument<String> doc = createDoc("AABBCCDD");
		doc.getModel();

		// Edit 1: replace "BB" (pos 2, len 2) with "YYY" → "AAYYYCCDD"
		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 2, 0, 4, "YYY")));

		// Edit 2: replace "Y" at pos 3 (within dirty) with "ZZ" → "AAYZZYYCCDD"... wait
		// T1 = "AAYYYCCDD", replace pos 3 len 1 with "ZZ" → "AAYZZYCCDD"
		doc.setVersion(2);
		doc.update(Collections.singletonList(createChange(0, 3, 0, 4, "ZZ")));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		// Merged against T0: dirty region is [2, 4), delete "BB" (2 chars), insert "YZZY" (4 chars)
		assertEquals(2, edit.getStartOffset());
		assertEquals(2, edit.getDeleteLength());
		assertEquals(4, edit.getInsertLength());
	}

	@Test
	public void mergeThreeConsecutiveEdits() {
		// Simulate rapid typing: insert 'a', 'b', 'c' at consecutive positions
		ModelTextDocument<String> doc = createDoc("<root></root>");
		doc.getModel();

		// Edit 1: insert "a" at pos 6 → "<root>a</root>"
		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 6, 0, 6, "a")));

		// Edit 2: insert "b" at pos 7 → "<root>ab</root>"
		doc.setVersion(2);
		doc.update(Collections.singletonList(createChange(0, 7, 0, 7, "b")));

		// Edit 3: insert "c" at pos 8 → "<root>abc</root>"
		doc.setVersion(3);
		doc.update(Collections.singletonList(createChange(0, 8, 0, 8, "c")));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		// Merged against T0: at pos 6, delete 0, insert 3
		assertEquals(6, edit.getStartOffset());
		assertEquals(0, edit.getDeleteLength());
		assertEquals(3, edit.getInsertLength());
	}

	@Test
	public void singleEditNoMerge() {
		ModelTextDocument<String> doc = createDoc("<root>text</root>");
		doc.getModel();

		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 6, 0, 10, "newtext")));

		ModelTextDocument.EditInfo edit = doc.getPendingEdit();
		assertNotNull(edit);
		assertEquals(6, edit.getStartOffset());
		assertEquals(4, edit.getDeleteLength());
		assertEquals(7, edit.getInsertLength());
	}

	@Test
	public void mergeEditsPreviousIncrementalDataPreserved() {
		ModelTextDocument<String> doc = createDoc("AABBCCDD");
		String firstModel = doc.getModel();

		doc.setVersion(1);
		doc.update(Collections.singletonList(createChange(0, 2, 0, 4, "YYY")));

		doc.setVersion(2);
		doc.update(Collections.singletonList(createChange(0, 6, 0, 7, "Z")));

		// previousIncrementalData should still be from the original model (T0)
		Object prev = doc.getPreviousIncrementalData();
		assertNotNull(prev);
		assertEquals(firstModel, prev);
	}

	private ModelTextDocument<String> createDoc(String text) {
		ModelTextDocument<String> doc = new ModelTextDocument<>(text, "test://test.xml",
				(document, cancelChecker) -> document.getText(),
				s -> s);
		doc.setIncremental(true);
		return doc;
	}

	private TextDocumentContentChangeEvent createChange(
			int startLine, int startChar, int endLine, int endChar, String text) {
		return new TextDocumentContentChangeEvent(
				new Range(new Position(startLine, startChar), new Position(endLine, endChar)),
				text);
	}
}
