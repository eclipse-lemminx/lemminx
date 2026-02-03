package org.eclipse.lemminx.dom;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.junit.jupiter.api.Test;

public class IncrementalDOMParserTest {

	@Test
	public void replaceText() {
		// Load XML
		DOMDocument document = DOMParser.getInstance().parse("<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>", "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = new IncrementalDOMParser();

		// Replace A with B
		List<TextDocumentContentChangeEvent> changes = e(1, 10, 11, "C");
		document.getTextDocument().update(changes);
		document = parser.parseIncremental(document, changes);

		assertIncrementalParser(document);
	}

	@Test
	public void insertSimpleText() {
		// Load XML
		DOMDocument document = DOMParser.getInstance().parse("<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>", "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = new IncrementalDOMParser();

		// Insert C after A
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 11, "C");
		document.getTextDocument().update(changes);
		document = parser.parseIncremental(document, changes);

		assertIncrementalParser(document);
	}

	@Test
	public void insertNewLine() {
		// Load XML
		DOMDocument document = DOMParser.getInstance().parse("<section>\r\n" + //
				"    <item>A</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>", "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = new IncrementalDOMParser();

		// Insert 2 lines after B
		List<TextDocumentContentChangeEvent> changes = e(2, 11, 11, "\r\n\r\n        \r\n    ");
		document.getTextDocument().update(changes);
		document = parser.parseIncremental(document, changes);

		assertIncrementalParser(document);
	}

	@Test
	public void removeText() {
		// Load XML
		DOMDocument document = DOMParser.getInstance().parse("<section>\r\n" + //
				"    <item>AC</item>\r\n" + //
				"    <item>B</item>\r\n" + //
				"</section>", "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = new IncrementalDOMParser();

		// Insert C after A
		List<TextDocumentContentChangeEvent> changes = e(1, 11, 12, "");
		document.getTextDocument().update(changes);
		document = parser.parseIncremental(document, changes);

		assertIncrementalParser(document);
	}
	
	private static void assertIncrementalParser(DOMDocument incrementalDocument) {
		String xml = incrementalDocument.getText();
		System.err.println(xml);
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);

		// Compare the string representation of both documents
		assertEquals(document.toString(), incrementalDocument.toString());
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