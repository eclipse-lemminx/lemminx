package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocumentChange;
import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Performance tests to measure incremental parsing efficiency
 */
public class IncrementalDOMParserPerformanceTest {

	@Test
	public void performanceComparison_smallDocument() {
		// Document with 100 items
		StringBuilder xml = new StringBuilder("<root>\r\n");
		for (int i = 0; i < 100; i++) {
			xml.append("    <item>Text").append(i).append("</item>\r\n");
		}
		xml.append("</root>");

		DOMDocument document = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser incrementalParser = IncrementalDOMParser.getInstance();

		// Measure: Incremental parse
		long start = System.nanoTime();
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 14, "Updated");
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		incrementalParser.parseIncremental(document, documentChanges);
		long incrementalTime = System.nanoTime() - start;

		// Measure: Full parse
		start = System.nanoTime();
		DOMDocument fullDoc = DOMParser.getInstance().parse(document.getText(), "test.xml", null);
		long fullTime = System.nanoTime() - start;

		System.out.println("=== Small Document (100 items) ===");
		System.out.println("Incremental parse: " + incrementalTime / 1_000_000 + "ms");
		System.out.println("Full parse: " + fullTime / 1_000_000 + "ms");
		System.out.println("Speedup: " + (fullTime / (double) incrementalTime) + "x");

		assertEquals(fullDoc.toString(), document.toString());

		// Incremental should be faster
		assertTrue(incrementalTime < fullTime, "Incremental parse should be faster than full parse");
	}

	@Test
	public void performanceComparison_largeDocument() {
		int nbItems = 1000000;
		// Document with a lot of items
		StringBuilder xml = new StringBuilder("<root>\r\n");
		for (int i = 0; i < nbItems; i++) {
			xml.append("    <item>Text").append(i).append("</item>\r\n");
		}
		xml.append("</root>");

		DOMDocument document = DOMParser.getInstance().parse(xml.toString(), "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser incrementalParser = IncrementalDOMParser.getInstance();

		List<TextDocumentContentChangeEvent> changes = e(1, 10, 14, "Updated");
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);

		// Measure: Full parse
		long start = System.nanoTime();
		DOMDocument fullDoc = DOMParser.getInstance().parse(document.getText(), "test.xml", null);
		long fullTime = System.nanoTime() - start;

		// Measure: Incremental parse
		start = System.nanoTime();
		UpdateStrategy strategy = incrementalParser.parseIncremental(document, documentChanges);
		long incrementalTime = System.nanoTime() - start;

		System.out.println("=== Large Document (" + nbItems + " items) ===");
		System.out.println("Incremental parse: " + incrementalTime / 1_000_000 + "ms");
		System.out.println("Full parse: " + fullTime / 1_000_000 + "ms");
		System.out.println("Speedup: " + (fullTime / (double) incrementalTime) + "x");

		assertEquals(strategy, UpdateStrategy.TEXT);
		assertEquals(fullDoc.toString(), document.toString());

		// Incremental should be significantly faster on large documents
		assertTrue(incrementalTime < fullTime, "Incremental parse should be faster than full parse");
	}

	@Test
	public void multipleSequentialChanges() {
		// Load XML
		DOMDocument document = DOMParser.getInstance().parse(
				"<section>\r\n" + "    <item>A</item>\r\n" + "    <item>B</item>\r\n" + "</section>", "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		long totalTime = 0;

		// Change 1: Insert C after A
		long start = System.nanoTime();
		List<TextDocumentContentChangeEvent> changes1 = e(1, 11, 11, "C");
		List<TextDocumentChange> documentChanges1 = document.getTextDocument().update(changes1);
		parser.parseIncremental(document, documentChanges1);
		totalTime += System.nanoTime() - start;

		// Change 2: Insert D after B
		start = System.nanoTime();
		List<TextDocumentContentChangeEvent> changes2 = e(2, 11, 11, "D");
		List<TextDocumentChange> documentChanges2 = document.getTextDocument().update(changes2);
		parser.parseIncremental(document, documentChanges2);
		totalTime += System.nanoTime() - start;

		// Change 3: Remove C
		start = System.nanoTime();
		List<TextDocumentContentChangeEvent> changes3 = e(1, 11, 12, "");
		List<TextDocumentChange> documentChanges3 = document.getTextDocument().update(changes3);
		
		parser.parseIncremental(document, documentChanges3);
		totalTime += System.nanoTime() - start;

		System.out.println("=== Multiple Sequential Changes ===");
		System.out.println("Total incremental time: " + totalTime / 1_000_000 + "ms");
		System.out.println("Average per change: " + (totalTime / 3) / 1_000_000 + "ms");

		// Verify final result
		DOMDocument fullDoc = DOMParser.getInstance().parse(document.getText(), "test.xml", null);
		assertEquals(fullDoc.toString(), document.toString());
	}

	@Test
	public void rapidTyping_simulation() {
		// Simulate user typing "Hello World" character by character
		DOMDocument document = DOMParser.getInstance().parse("<section>\r\n" + "    <item></item>\r\n" + "</section>",
				"test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();

		String textToType = "Hello World";
		long totalTime = 0;

		for (int i = 0; i < textToType.length(); i++) {
			char c = textToType.charAt(i);
			long start = System.nanoTime();

			List<TextDocumentContentChangeEvent> changes = e(1, 10 + i, 10 + i, String.valueOf(c));
			List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);
			parser.parseIncremental(document, documentChanges);

			totalTime += System.nanoTime() - start;
		}

		System.out.println("=== Rapid Typing Simulation ===");
		System.out.println("Total time for " + textToType.length() + " characters: " + totalTime / 1_000_000 + "ms");
		System.out.println("Average per character: " + (totalTime / textToType.length()) / 1_000_000 + "ms");

		// Verify final result
		DOMDocument fullDoc = DOMParser.getInstance().parse(document.getText(), "test.xml", null);
		assertEquals(fullDoc.toString(), document.toString());
	}

	private static List<TextDocumentContentChangeEvent> e(int line, int startCharacter, int endCharacter, String text) {
		return e(line, startCharacter, line, endCharacter, text);
	}

	private static List<TextDocumentContentChangeEvent> e(int startLine, int startCharacter, int endLine,
			int endCharacter, String text) {
		TextDocumentContentChangeEvent event = new TextDocumentContentChangeEvent(
				r(startLine, startCharacter, endLine, endCharacter), text);
		return Collections.singletonList(event);
	}
}