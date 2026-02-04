package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for complex XML structures
 */
public class IncrementalDOMParserStructureTest {

	@Test
	public void nestedElements() {
		String xml = "<root>\r\n" + //
				"    <parent>\r\n" + //
				"        <child>Text</child>\r\n" + //
				"    </parent>\r\n" + //
				"</root>";

		// Modify text in nested <child>
		List<TextDocumentContentChangeEvent> changes = e(2, 15, 19, "Updated");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void deeplyNestedElements() {
		String xml = "<root>\r\n" + //
				"    <level1>\r\n" + //
				"        <level2>\r\n" + //
				"            <level3>DeepText</level3>\r\n" + //
				"        </level2>\r\n" + //
				"    </level1>\r\n" + //
				"</root>";

		// Modify deeply nested text
		List<TextDocumentContentChangeEvent> changes = e(3, 20, 28, "Modified");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void emptyElement() {
		String xml = "<section>\r\n" + //
				"    <item></item>\r\n" + //
				"</section>";

		// Insert text in empty element
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 10, "New");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void selfClosingTag() {
		String xml = "<section>\r\n" + //
				"    <item/>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>";

		// Modify B in second item
		List<TextDocumentContentChangeEvent> changes = e(2, 10, 11, "C");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void multipleSiblings() {
		String xml = "<root>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"    <item>C</item>\r\n" + //
				"    <item>D</item>\r\n" + //
				"</root>";

		// Modify B (second item)
		List<TextDocumentContentChangeEvent> changes = e(2, 10, 11, "Modified");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void firstSibling() {
		String xml = "<root>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"    <item>C</item>\r\n" + //
				"</root>";

		// Modify first item
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 11, "First");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void lastSibling() {
		String xml = "<root>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"    <item>C</item>\r\n" + //
				"</root>";

		// Modify last item
		List<TextDocumentContentChangeEvent> changes = e(3, 10, 11, "Last");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void closeStartTag() {
		String xml = "<foo>\r\n" + //
				"    <bar\r\n" + //
				"</foo>";

		// Insert text: >
		List<TextDocumentContentChangeEvent> changes = e(1, 8, 8, ">");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

}