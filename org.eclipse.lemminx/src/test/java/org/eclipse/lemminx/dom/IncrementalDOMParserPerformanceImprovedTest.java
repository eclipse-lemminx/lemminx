package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertDOMEquals;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lemminx.commons.TextDocumentChange;
import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Performance tests for improved incremental parsing with SUBTREE strategy
 */
public class IncrementalDOMParserPerformanceImprovedTest {

	@Test
	public void testSubtreePerformance_mediumFile() {
		// Test SUBTREE performance on medium file (1000 items)
		int nbItems = 1000;
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 0; i < nbItems; i++) {
			xml.append("  <item>Text").append(i).append("</item>\n");
		}
		xml.append("</root>");

		DOMDocument document = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		// Modify an item in the middle
		List<TextDocumentContentChangeEvent> changes = e(500, 8, 15, "Modified");
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		long start = System.nanoTime();
		UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);
		long duration = (System.nanoTime() - start) / 1_000_000;

		System.out.println("=== Medium File (1000 items) ===");
		System.out.println("Strategy: " + strategy);
		System.out.println("Incremental parse: " + duration + "ms");

		// Should use TEXT and be fast
		assertEquals(UpdateStrategy.TEXT, strategy);
		assertTrue(duration < 100, "TEXT parsing should be < 100ms, was " + duration + "ms");

		assertDOMEquals(document);
	}

	@Test
	public void testSubtreePerformance_largeFile() {
		// Test SUBTREE performance on large file (10000 items)
		int nbItems = 10000;
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 0; i < nbItems; i++) {
			xml.append("  <item>Text").append(i).append("</item>\n");
		}
		xml.append("</root>");

		DOMDocument document = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		// Modify an item in the middle
		List<TextDocumentContentChangeEvent> changes = e(5000, 8, 16, "Modified");
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		long start = System.nanoTime();
		UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);
		long duration = (System.nanoTime() - start) / 1_000_000;

		System.out.println("=== Large File (10000 items) ===");
		System.out.println("Strategy: " + strategy);
		System.out.println("Incremental parse: " + duration + "ms");

		// Should use FULL because root is too large, but still reasonable time
		assertTrue(duration < 2000, "Parsing should be < 2s, was " + duration + "ms");

		assertDOMEquals(document);
	}

	@Test
	public void testSubtreeVsFullParse_comparison() {
		// Compare SUBTREE vs FULL parse performance
		int nbItems = 5000;
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 0; i < nbItems; i++) {
			xml.append("  <item>Text").append(i).append("</item>\n");
		}
		xml.append("</root>");

		// Incremental parse
		DOMDocument incrementalDoc = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		incrementalDoc.getTextDocument().setIncremental(true);

		List<TextDocumentContentChangeEvent> changes = e(2500, 8, 16, "Modified");
		List<TextDocumentChange> documentChanges = incrementalDoc.getTextDocument().update(changes);

		long startIncremental = System.nanoTime();
		UpdateStrategy strategy = IncrementalDOMParser.getInstance().parseIncremental(incrementalDoc, documentChanges);
		long incrementalTime = (System.nanoTime() - startIncremental) / 1_000_000;

		// Full parse
		long startFull = System.nanoTime();
		DOMDocument fullDoc = DOMParser.getInstance().parse(incrementalDoc.getText(), "test.xml", null);
		long fullTime = (System.nanoTime() - startFull) / 1_000_000;

		System.out.println("=== SUBTREE vs FULL Comparison (5000 items) ===");
		System.out.println("Strategy: " + strategy);
		System.out.println("Incremental parse: " + incrementalTime + "ms");
		System.out.println("Full parse: " + fullTime + "ms");
		System.out.println("Speedup: " + (fullTime / (double) incrementalTime) + "x");

		// Incremental should be faster (unless it falls back to FULL)
		if (strategy != UpdateStrategy.FULL) {
			assertTrue(incrementalTime < fullTime,
					"Incremental (" + incrementalTime + "ms) should be faster than full (" + fullTime + "ms)");
		}

		assertEquals(fullDoc.toString(), incrementalDoc.toString());
	}

	@Test
	public void testNestedSubtreePerformance() {
		// Test SUBTREE performance with nested structure
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 0; i < 100; i++) {
			xml.append("  <section>\n");
			for (int j = 0; j < 10; j++) {
				xml.append("    <item>Text").append(i).append("-").append(j).append("</item>\n");
			}
			xml.append("  </section>\n");
		}
		xml.append("</root>");

		DOMDocument document = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		// Modify an item in section 50, item 5 (line = 2 + 50*12 + 5 = 607)
		// Replace "Text50-5" (positions 10-18) with "Modified"
		List<TextDocumentContentChangeEvent> changes = e(607, 10, 18, "Modified");
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		long start = System.nanoTime();
		UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);
		long duration = (System.nanoTime() - start) / 1_000_000;

		System.out.println("=== Nested Structure (100 sections x 10 items) ===");
		System.out.println("Strategy: " + strategy);
		System.out.println("Incremental parse: " + duration + "ms");

		// Should use SUBTREE on the section
		assertEquals(UpdateStrategy.TEXT, strategy);
		assertTrue(duration < 50, "TEXT parsing should be < 50ms, was " + duration + "ms");

		assertDOMEquals(document);
	}

	@Test
	public void testRapidSequentialChanges() {
		// Simulate rapid typing with sequential changes
		String xml = "<root>\n" + "  <item></item>\n" + "</root>";

		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		String textToType = "Hello World";
		long totalTime = 0;
		int subtreeCount = 0;
		int textCount = 0;

		for (int i = 0; i < textToType.length(); i++) {
			char c = textToType.charAt(i);
			long start = System.nanoTime();

			List<TextDocumentContentChangeEvent> changes = e(1, 8 + i, 8 + i, String.valueOf(c));
			List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

			UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);

			totalTime += System.nanoTime() - start;

			if (strategy == UpdateStrategy.SUBTREE) {
				subtreeCount++;
			} else if (strategy == UpdateStrategy.TEXT) {
				textCount++;
			}
		}

		long avgTime = (totalTime / textToType.length()) / 1_000_000;

		System.out.println("=== Rapid Sequential Changes ===");
		System.out.println("Total time for " + textToType.length() + " characters: " + totalTime / 1_000_000 + "ms");
		System.out.println("Average per character: " + avgTime + "ms");
		System.out.println("TEXT strategy: " + textCount + " times");
		System.out.println("SUBTREE strategy: " + subtreeCount + " times");

		// Average should be very fast
		assertTrue(avgTime < 10, "Average per character should be < 10ms, was " + avgTime + "ms");

		assertDOMEquals(document);
	}

	@Test
	public void testAddMultipleElements_performance() {
		// Test performance when adding multiple elements
		String xml = "<root>\n" + "  <item>A</item>\n" + "</root>";

		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		// Add 100 new items at once
		StringBuilder newItems = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			newItems.append("\n  <item>").append(i).append("</item>");
		}

		// Insert after line 1 (after " <item>A</item>\n"), which is at line 2,
		// character 0
		List<TextDocumentContentChangeEvent> changes = e(2, 0, 2, 0, newItems.toString());
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		long start = System.nanoTime();
		UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);
		long duration = (System.nanoTime() - start) / 1_000_000;

		System.out.println("=== Add 100 Elements ===");
		System.out.println("Strategy: " + strategy);
		System.out.println("Incremental parse: " + duration + "ms");

		// Should use SUBTREE
		assertEquals(UpdateStrategy.SUBTREE, strategy);
		assertTrue(duration < 100, "SUBTREE parsing should be < 100ms, was " + duration + "ms");

		assertDOMEquals(document);
	}

}
