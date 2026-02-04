package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;
import static org.eclipse.lemminx.XMLIncrementalParserAssert.e;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests for simple text modifications (replace, insert, remove)
 */
public class IncrementalDOMParserTextChangeTest {

	@Test
	public void replaceText() {
		String xml = "<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>";

		// Replace A with C
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 11, "C");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void insertSimpleText() {
		String xml = "<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>";

		// Insert C after A
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 11, "C");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void insertNewLine() {
		String xml = "<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>";

		// Insert 2 lines after B
		List<TextDocumentContentChangeEvent> changes = e(2, 11, 11, "\r\n\r\n        \r\n    ");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void removeText() {
		String xml = "<section>\r\n" + //
				"    <item>AC</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>";

		// Remove C
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 12, "");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void removeMultipleCharacters() {
		String xml = "<section>\r\n" + //
				"    <item>ABCDEF</item>\r\n" + //
				"</section>";

		// Remove BCDE (keep A and F)
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 15, "");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void insertAtBeginning() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Insert "Start" at beginning
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 10, "Start");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void insertAtEnd() {
		String xml = "<section>\r\n" + //
				"    <item>Text</item>\r\n" + //
				"</section>";

		// Insert "End" at end
		List<TextDocumentContentChangeEvent> changes = e(1, 14, 14, "End");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void replaceAllText() {
		String xml = "<section>\r\n" + //
				"    <item>OldText</item>\r\n" + //
				"</section>";

		// Replace all text "OldText" -> "NewText"
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 17, "NewText");
		assertIncremental(xml, UpdateStrategy.TEXT, changes);
	}

	@Test
	public void insertNewLineInEmptyElement() {
		String xml = "<foo>\n" + //
				"    <bar></bar>\n" + //
				"    <bar></bar>\n" + //
				"</foo>";

		// Insert newlines and spaces inside the second <bar> element
		// Change <bar></bar> to <bar>\n \n </bar>
		// Position is after <bar> (line 2, character 9)
		List<TextDocumentContentChangeEvent> changes = e(2, 9, 9, "\n        \n    ");
		DOMDocument document = assertIncremental(xml, UpdateStrategy.TEXT, changes);

		// Verify the final structure
		String expectedXml = "<foo>\n" + //
				"    <bar></bar>\n" + //
				"    <bar>\n" + //
				"        \n" + //
				"    </bar>\n" + //
				"</foo>";
		assertEquals(expectedXml, document.getText());
	}

	@Test
	public void insertStructuralCharInEmptyElement() {
		String xml = "<foo>\n" + //
				"    <bar></bar>\n" + //
				"    <bar></bar>\n" + //
				"</foo>";

		// Insert '<' inside the second <bar> element (structural character)
		// Change <bar></bar> to <bar>\n <\n </bar>
		// Position is after <bar> (line 2, character 9)
		List<TextDocumentContentChangeEvent> changes = e(2, 9, 9, "\n        <\n    ");
		DOMDocument document = assertIncremental(xml, UpdateStrategy.SUBTREE, changes);

		// Verify the final structure
		String expectedXml = "<foo>\n" + //
				"    <bar></bar>\n" + //
				"    <bar>\n" + //
				"        <\n" + //
				"    </bar>\n" + //
				"</foo>";
		assertEquals(expectedXml, document.getText());
	}

}