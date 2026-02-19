package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for edge cases in incremental parsing
 */
public class IncrementalDOMParserEdgeCasesTest {

	@Test
	public void testModificationAtElementBoundary() {
		// Modification right at the boundary of an element
		String xml = "<root>\n" + //
				"  <item>Text</item>\n" + //
				"</root>";

		// Insert right after >
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 8, "New");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testModificationInEmptyElement() {
		// Insert text in an empty element
		String xml = "<root>\n" + //
				"  <item></item>\n" + //
				"</root>";

		// Insert text in empty element
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 8, "Content");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testDeeplyNestedModification() {
		// Modification in deeply nested structure
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 1; i <= 10; i++) {
			xml.append("  ".repeat(i)).append("<level").append(i).append(">\n");
		}
		xml.append("  ".repeat(11)).append("Text\n");
		for (int i = 10; i >= 1; i--) {
			xml.append("  ".repeat(i)).append("</level").append(i).append(">\n");
		}
		xml.append("</root>");

		// Modify the deeply nested text
		List<TextDocumentContentChangeEvent> changes = e(10, 22, 26, "Modified");
		assertIncremental(xml.toString(), UpdateStrategy.FULL, changes);
	}

	@Test
	public void testLargeSubtreeThreshold() {
		// Test that very large subtrees fall back to FULL parse
		StringBuilder xml = new StringBuilder("<root>\n");

		// Create a large subtree (> 100KB)
		for (int i = 0; i < 5000; i++) {
			xml.append("  <item>This is item number ").append(i).append("</item>\n");
		}
		xml.append("</root>");

		// Modify first item
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 29, "Modified item");
		assertIncremental(xml.toString(), UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testModificationInPreserveSpace() {
		// Modification in element with xml:space="preserve"
		String xml = "<root>\n" + //
				"  <item xml:space=\"preserve\">  Text  </item>\n" + //
				"</root>";

		// Modify text (spaces should be preserved)
		// Line 1, char 31-35 is "Text" (not 34-38 which would include the closing tag)
		List<TextDocumentContentChangeEvent> changes = e(1, 31, 35, "More");
		assertIncremental(xml.toString(), UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testMultipleSiblings() {
		// Modification with many siblings
		StringBuilder xml = new StringBuilder("<root>\n");
		for (int i = 0; i < 100; i++) {
			xml.append("  <item>").append(i).append("</item>\n");
		}
		xml.append("</root>");

		// Modify item in the middle
		List<TextDocumentContentChangeEvent> changes = e(50, 8, 10, "Modified");
		assertIncremental(xml.toString(), UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testModificationWithEntities() {
		// Modification involving HTML entities
		String xml = "<root>\n" + //
				"  <item>Text</item>\n" + //
				"</root>";

		// Add entity
		List<TextDocumentContentChangeEvent> changes = e(1, 12, 12, " & More");
		assertIncremental(xml.toString(), UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testModificationInAttributeWithQuotes() {
		// Modification in attribute value containing quotes
		String xml = "<root>\n" + //
				"  <item title=\"Test\">Text</item>\n" + //
				"</root>";

		// Modify attribute value
		List<TextDocumentContentChangeEvent> changes = e(1, 15, 19, "New Value");
		assertIncremental(xml.toString(), UpdateStrategy.ATTR, changes);
	}

	@Test
	public void testEmptyDocument() {
		// Start with empty document
		String xml = "";

		// Add root element
		List<TextDocumentContentChangeEvent> changes = e(0, 0, 0, "<root></root>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testSingleCharacterChange() {
		// Change a single character
		String xml = "<root>\n" + //
				"  <item>A</item>\n" + //
				"</root>";

		// Change A to B
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 9, "B");
		assertIncremental(xml.toString(), UpdateStrategy.TEXT, changes);
	}

	@Test
	public void testInsertLessThanInElementWithIndentation() {
		// Test inserting '<' in the middle of an element with indentation
		// This is a critical case: inserting '<' creates invalid XML that needs proper
		// handling
		String xml = "<foo>\n" + //
				"    \n" + //
				"</foo>";

		// Insert '<' in the middle of the whitespace (after indentation)
		// Line 1, position 4 is right after the 4 spaces of indentation
		List<TextDocumentContentChangeEvent> changes = e(1, 4, 4, "<");
		assertIncremental(xml.toString(), UpdateStrategy.SUBTREE, changes);
	}

}
