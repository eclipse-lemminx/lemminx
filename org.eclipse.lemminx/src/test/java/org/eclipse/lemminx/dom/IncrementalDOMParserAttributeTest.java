package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for attribute modifications
 */
public class IncrementalDOMParserAttributeTest {

	@Test
	public void replaceAttributeValue() {
		String xml = "<section>\r\n" + //
				"    <item id=\"A\">Text</item>\r\n" + //
				"</section>";

		// Replace id="A" with id="B"
		List<TextDocumentContentChangeEvent> changes = e(1, 14, 15, "B");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

	@Test
	public void insertInAttributeValue() {
		String xml = "<section>\r\n" + //
				"    <item id=\"item1\">Text</item>\r\n" + //
				"</section>";

		// Insert "0" after "item1" -> "item10"
		List<TextDocumentContentChangeEvent> changes = e(1, 19, 19, "0");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

	@Test
	public void removeFromAttributeValue() {
		String xml = "<section>\r\n" + //
				"    <item name=\"TestValue\">Text</item>\r\n" + //
				"</section>";

		// Remove "Value" from "TestValue" -> "Test"
		List<TextDocumentContentChangeEvent> changes = e(1, 20, 25, "");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

	@Test
	public void multipleAttributes() {
		String xml = "<section>\r\n" + //
				"    <item id=\"1\" name=\"Test\" value=\"ABC\">Text</item>\r\n" + //
				"</section>";

		// Modify middle attribute "Test" -> "Updated"
		List<TextDocumentContentChangeEvent> changes = e(1, 23, 27, "Updated");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

	@Test
	public void replaceEntireAttributeValue() {
		String xml = "<section>\r\n" + //
				"    <item class=\"old-class\">Text</item>\r\n" + //
				"</section>";

		// Replace entire value "old-class" -> "new-class"
		List<TextDocumentContentChangeEvent> changes = e(1, 17, 26, "new-class");
		assertIncremental(xml, UpdateStrategy.ATTR, changes);
	}

}