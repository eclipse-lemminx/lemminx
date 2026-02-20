package org.eclipse.lemminx.services.format.settings;

import static org.eclipse.lemminx.utils.IOUtils.convertStreamToString;

import java.io.InputStream;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.performance.DOMParserPerformance;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

public class XMLFormatterLargeFileTest {

	@Test
	public void largeFile() throws BadLocationException {
		SharedSettings settings = new SharedSettings();

		InputStream in = DOMParserPerformance.class.getResourceAsStream("/xml/large-dataset.xml");
		String content = convertStreamToString(in);

		String expected = content;
		assertFormat(content, expected, settings);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "test://test.html", expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			TextEdit... expectedEdits) throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, uri, true, expectedEdits);
	}

	private static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			Boolean considerRangeFormat, TextEdit... expectedEdits) throws BadLocationException {
		XMLAssert.assertFormat(null, unformatted, expected, sharedSettings, uri, considerRangeFormat, expectedEdits);
	}
}
