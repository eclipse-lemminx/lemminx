package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for SUBTREE strategy - re-parsing subtrees instead of full document
 */
public class IncrementalDOMParserSubtreeTest {

	@Test
	public void testAddElement() {
		// Add a new element inside a parent
		String xml = "<root>\n" + //
				"  <item>A</item>\n" + //
				"</root>";

		// Add new element after first item (at start of line 2, before </root>)
		List<TextDocumentContentChangeEvent> changes = e(2, 0, 2, 0, "\n  <item>B</item>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testRemoveElement() {
		// Remove an element
		String xml = "<root>\n" + //
				"  <item>A</item>\n" + //
				"  <item>B</item>\n" + //
				"</root>";
		// Remove second item
		List<TextDocumentContentChangeEvent> changes = e(2, 0, 3, 0, "");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testModifyTagName() {
		// Modify a tag name
		String xml = "<root>\n" + //
				"  <item>Text</item>\n" + //
				"</root>";

		// Change "item" to "items" in opening tag
		List<TextDocumentContentChangeEvent> changes = e(1, 3, 7, "items");
		assertIncremental(xml, UpdateStrategy.FULL, changes);
	}

	@Test
	public void testAddAttribute() {
		// Add a new attribute to an element
		String xml = "<root>\n" + //
				"  <item>Text</item>\n" + //
				"</root>";

		// Add id attribute
		List<TextDocumentContentChangeEvent> changes = e(1, 7, 7, " id=\"1\"");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

	@Test
	public void testRemoveAttribute() {
		// Remove an attribute
		String xml = "<root>\n" + //
				"  <item id=\"1\" name=\"test\">Text</item>\n" + //
				"</root>";

		// Remove name attribute
		List<TextDocumentContentChangeEvent> changes = e(1, 15, 28, "");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testTransformToSelfClosing() {
		// Transform element to self-closing
		String xml = "<root>\n" + //
				"  <item></item>\n" + //
				"</root>";

		// Change to self-closing
		List<TextDocumentContentChangeEvent> changes = e(1, 7, 15, "/>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testTransformFromSelfClosing() {
		// Transform self-closing to regular element
		String xml = "<root>\n" + //
				"  <item/>\n" + //
				"</root>";

		// Change to regular element
		List<TextDocumentContentChangeEvent> changes = e(1, 7, 9, "></item>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testAddComment() {
		// Add a comment
		String xml = "<root>\n" + //
				"  <item>A</item>\n" + //
				"</root>";

		// Add comment before item
		List<TextDocumentContentChangeEvent> changes = e(1, 2, 2, "<!-- Comment -->\n  ");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testAddCDATA() {
		// Add CDATA section
		String xml = "<root>\n" + //
				"  <item>Text</item>\n" + //
				"</root>";

		// Replace text with CDATA
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 12, "<![CDATA[Text with <tags>]]>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testMultiLineModification() {
		// Add multiple elements at once
		String xml = "<root>\n" + //
				"  <item>A</item>\n" + //
				"</root>";

		// Add multiple items
		String newItems = "\n  <item>B</item>\n  <item>C</item>\n  <item>D</item>";
		List<TextDocumentContentChangeEvent> changes = e(1, 16, 16, newItems);
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testCopyPasteBlock() {
		// Copy-paste a whole section
		String xml = "<root>\n" + //
				"  <section1>\n" + //
				"    <item>A</item>\n" + //
				"  </section1>\n" + //
				"</root>";

		// Paste a new section
		String newSection = "\n  <section2>\n    <item>B</item>\n  </section2>";
		List<TextDocumentContentChangeEvent> changes = e(3, 13, 13, newSection);
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testNestedElementModification() {
		// Modify deeply nested element
		String xml = "<root>\n" + //
				"  <level1>\n" + //
				"    <level2>\n" + //
				"      <level3>Text</level3>\n" + //
				"    </level2>\n" + //
				"  </level1>\n" + //
				"</root>";

		// Add element in level3
		List<TextDocumentContentChangeEvent> changes = e(3, 22, 22, "<item>New</item>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void testIndentationChange() {
		// Reformat/indent XML
		String xml = "<root><item>A</item></root>";

		// Add newlines and indentation
		List<TextDocumentContentChangeEvent> changes = e(0, 6, 6, "\n  ");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

}