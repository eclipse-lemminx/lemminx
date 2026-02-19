package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests that trigger full parse fallback
 */
public class IncrementalDOMParserFallbackTest {

	@Test
	public void insertTag_shouldTriggerFullParse() {
		String xml = "<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"</section>";

		// Insert a new tag <tag>C</tag>
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 11, "<tag>C</tag>");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void insertOpeningBracket_shouldTriggerFullParse() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Insert < in text
		List<TextDocumentContentChangeEvent> changes = e(1, 12, 12, "<");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void insertClosingBracket_shouldTriggerFullParse() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Insert > in text
		List<TextDocumentContentChangeEvent> changes = e(1, 12, 12, ">");
		assertIncremental(xml, UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void modifyTagName_shouldTriggerFullParse() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Modify tag name "item" -> "items"
		List<TextDocumentContentChangeEvent> changes = e(1, 5, 9, "items");
		assertIncremental(xml.toString(), UpdateStrategy.SUBTREE, changes);
	}

	@Test
	public void insertBothBrackets_shouldTriggerFullParse() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Insert <> in text
		List<TextDocumentContentChangeEvent> changes = e(1, 12, 12, "<>");
		assertIncremental(xml.toString(), UpdateStrategy.SUBTREE, changes);
	}
}