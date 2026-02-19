package org.eclipse.lemminx;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import org.eclipse.lemminx.commons.TextDocumentChange;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.dom.IncrementalDOMParser;
import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;

public class XMLIncrementalParserAssert {

	public static DOMDocument assertIncremental(String xml, UpdateStrategy expectedStrategy,
			List<TextDocumentContentChangeEvent> changes) {
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		document.getTextDocument().setIncremental(true);

		IncrementalDOMParser parser = IncrementalDOMParser.getInstance();
		List<TextDocumentChange> documentChanges = document.getTextDocument().update(changes);
		UpdateStrategy strategy = parser.parseIncremental(document, documentChanges);

		assertEquals(expectedStrategy, strategy);
		assertDOMEquals(document);
		return document;
	}

	/**
	 * Verify that incremental parsing produces the same DOM as full parsing
	 */
	public static void assertDOMEquals(DOMDocument incrementalDocument) {
		DOMDocument fullDocument = DOMParser.getInstance().parse(incrementalDocument.getTextDocument(), null, true,
				null);
		assertEquals(fullDocument.toString(), incrementalDocument.toString(),
				"Incremental DOM should match full parse DOM");
	}

	public static List<TextDocumentContentChangeEvent> e(int line, int startCharacter, int endCharacter, String text) {
		return e(line, startCharacter, line, endCharacter, text);
	}

	public static List<TextDocumentContentChangeEvent> e(int startLine, int startCharacter, int endLine,
			int endCharacter, String text) {
		TextDocumentContentChangeEvent event = new TextDocumentContentChangeEvent(
				r(startLine, startCharacter, endLine, endCharacter), text);
		return Collections.singletonList(event);
	}
}
