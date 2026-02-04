package org.eclipse.lemminx.dom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.commons.TextDocumentChange;
import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;
import org.eclipse.lsp4j.Position;

/**
 * Generates JUnit test code for incremental DOM parsing scenarios.
 * This is useful for capturing real user editing scenarios and converting them into tests.
 */
public class IncrementalDOMParserTestGenerator {

	private static final Logger LOGGER = Logger.getLogger(IncrementalDOMParserTestGenerator.class.getName());
	private static final IncrementalDOMParserTestGenerator INSTANCE = new IncrementalDOMParserTestGenerator();
	private static final String TEST_OUTPUT_DIR = "src/test/java/org/eclipse/lemminx/dom/generated";

	public static IncrementalDOMParserTestGenerator getInstance() {
		return INSTANCE;
	}

	/**
	 * Generate and save a JUnit test method from a document and its changes.
	 * The test will be saved to a file based on the document URI.
	 *
	 * @param document the document after changes
	 * @param changes the list of changes applied
	 * @param oldText the original text before changes
	 */
	public void generateTest(DOMDocument document, List<TextDocumentChange> changes, String oldText) {
		if (changes == null || changes.isEmpty()) {
			LOGGER.log(Level.FINE, "No changes to generate test for");
			return;
		}

		try {
			String testCode = generateTestCode(document, changes, oldText);
			saveTestToFile(document.getDocumentURI(), testCode);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error generating test", e);
		}
	}

	/**
	 * Generate a JUnit test method from a document and its changes.
	 *
	 * @param document the document after changes
	 * @param changes the list of changes applied
	 * @param oldText the original text before changes
	 * @return the generated test code as a string
	 */
	private String generateTestCode(DOMDocument document, List<TextDocumentChange> changes, String oldText) {
		if (changes == null || changes.isEmpty()) {
			return "// No changes to generate test for";
		}

		StringBuilder s = new StringBuilder();
		
		// Generate test method header with timestamp
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		s.append("\t@Test\n");
		s.append("\tpublic void testGenerated_").append(timestamp).append("() {\n");
		
		// Generate XML variable with original text
		s.append("\t\t// Original XML\n");
		s.append("\t\tString xml = ");
		s.append(formatXmlString(oldText));
		s.append(";\n\n");
		
		// Generate comment describing the change
		TextDocumentChange firstChange = changes.get(0);
		s.append("\t\t// ");
		s.append(describeChange(firstChange, oldText));
		s.append("\n");
		
		// Generate the change event
		try {
			TextDocument textDoc = new TextDocument(oldText, "test.xml");
			Position startPos = textDoc.positionAt(firstChange.getStartOffset());
			Position endPos = textDoc.positionAt(firstChange.getStartOffset() + firstChange.getOldLength());
			
			s.append("\t\tList<TextDocumentContentChangeEvent> changes = e(");
			s.append(startPos.getLine()).append(", ");
			s.append(startPos.getCharacter()).append(", ");
			if (startPos.getLine() == endPos.getLine()) {
				s.append(endPos.getCharacter());
			} else {
				s.append(endPos.getLine()).append(", ");
				s.append(endPos.getCharacter());
			}
			s.append(", ");
			s.append(formatString(firstChange.getText()));
			s.append(");\n");
			
		} catch (BadLocationException e) {
			s.append("\t\t// Error calculating positions: ").append(e.getMessage()).append("\n");
			s.append("\t\tList<TextDocumentContentChangeEvent> changes = null; // TODO: Fix positions\n");
		}
		
		// Determine expected strategy
		UpdateStrategy expectedStrategy = determineExpectedStrategy(firstChange);
		s.append("\t\tassertIncremental(xml, UpdateStrategy.").append(expectedStrategy).append(", changes);\n");
		
		s.append("\t}\n");
		
		return s.toString();
	}

	/**
	 * Save the generated test to a file based on the document URI.
	 *
	 * @param documentURI the URI of the document being edited
	 * @param testCode the generated test code
	 */
	private void saveTestToFile(String documentURI, String testCode) {
		try {
			// Create output directory if it doesn't exist
			Path outputDir = Paths.get(TEST_OUTPUT_DIR);
			if (!Files.exists(outputDir)) {
				Files.createDirectories(outputDir);
			}

			// Generate filename from document URI
			String filename = generateFilename(documentURI);
			Path outputFile = outputDir.resolve(filename);

			// Check if file exists and append or create
			if (Files.exists(outputFile)) {
				// Append to existing file
				Files.writeString(outputFile, "\n" + testCode, StandardOpenOption.APPEND);
				LOGGER.log(Level.INFO, "Appended test to: " + outputFile);
			} else {
				// Create new file with class structure
				String fullContent = generateTestClass(filename, testCode);
				Files.writeString(outputFile, fullContent, StandardOpenOption.CREATE);
				LOGGER.log(Level.INFO, "Created test file: " + outputFile);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error saving test to file", e);
		}
	}

	/**
	 * Generate a filename from the document URI.
	 */
	private String generateFilename(String documentURI) {
		if (documentURI == null || documentURI.isEmpty()) {
			return "GeneratedIncrementalTest.java";
		}

		// Extract filename from URI
		String filename = documentURI;
		int lastSlash = filename.lastIndexOf('/');
		if (lastSlash >= 0) {
			filename = filename.substring(lastSlash + 1);
		}
		
		// Remove extension and sanitize
		int lastDot = filename.lastIndexOf('.');
		if (lastDot > 0) {
			filename = filename.substring(0, lastDot);
		}
		
		// Convert to valid Java class name
		filename = filename.replaceAll("[^a-zA-Z0-9]", "_");
		if (!filename.isEmpty() && Character.isDigit(filename.charAt(0))) {
			filename = "Test_" + filename;
		}
		
		return "Generated_" + filename + "_Test.java";
	}

	/**
	 * Generate a complete test class with the given test method.
	 */
	private String generateTestClass(String filename, String testCode) {
		String className = filename.replace(".java", "");
		StringBuilder s = new StringBuilder();
		
		s.append("package org.eclipse.lemminx.dom.generated;\n\n");
		s.append("import static org.eclipse.lemminx.dom.IncrementalParsingTestUtils.e;\n");
		s.append("import static org.eclipse.lemminx.XMLIncrementalParserAssert.assertIncremental;\n\n");
		s.append("import java.util.List;\n\n");
		s.append("import org.eclipse.lemminx.dom.IncrementalDOMParser.UpdateStrategy;\n");
		s.append("import org.eclipse.lsp4j.TextDocumentContentChangeEvent;\n");
		s.append("import org.junit.jupiter.api.Test;\n\n");
		s.append("/**\n");
		s.append(" * Auto-generated test class for incremental DOM parsing.\n");
		s.append(" * Generated from user editing scenarios.\n");
		s.append(" */\n");
		s.append("public class ").append(className).append(" {\n\n");
		s.append(testCode);
		s.append("\n}\n");
		
		return s.toString();
	}

	/**
	 * Format XML string for Java code with proper escaping and line breaks.
	 */
	private String formatXmlString(String xml) {
		if (xml == null || xml.isEmpty()) {
			return "\"\"";
		}
		
		String[] lines = xml.split("\n");
		if (lines.length == 1) {
			return "\"" + escapeJavaString(xml) + "\"";
		}
		
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				result.append(" + //\n\t\t\t\t");
			}
			result.append("\"").append(escapeJavaString(lines[i]));
			if (i < lines.length - 1) {
				result.append("\\n");
			}
			result.append("\"");
		}
		return result.toString();
	}

	/**
	 * Format a string for Java code with proper escaping.
	 */
	private String formatString(String str) {
		if (str == null) {
			return "\"\"";
		}
		return "\"" + escapeJavaString(str) + "\"";
	}

	/**
	 * Escape special characters in a string for Java code.
	 */
	private String escapeJavaString(String str) {
		if (str == null) {
			return "";
		}
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/**
	 * Generate a human-readable description of the change.
	 */
	private String describeChange(TextDocumentChange change, String oldText) {
		String text = change.getText();
		int oldLength = change.getOldLength();
		
		if (oldLength == 0) {
			// Insertion
			if (text.contains("<") && text.contains(">")) {
				return "Insert element/tag";
			}
			return "Insert text: " + truncate(text, 30);
		} else if (text.isEmpty()) {
			// Deletion
			String deleted = oldText.substring(change.getStartOffset(),
					Math.min(change.getStartOffset() + oldLength, oldText.length()));
			return "Delete: " + truncate(deleted, 30);
		} else {
			// Replacement
			String oldContent = oldText.substring(change.getStartOffset(),
					Math.min(change.getStartOffset() + oldLength, oldText.length()));
			return "Replace '" + truncate(oldContent, 20) + "' with '" + truncate(text, 20) + "'";
		}
	}

	/**
	 * Truncate a string to a maximum length with ellipsis.
	 */
	private String truncate(String str, int maxLength) {
		if (str == null || str.length() <= maxLength) {
			return str;
		}
		return str.substring(0, maxLength) + "...";
	}

	/**
	 * Determine the expected update strategy based on the change.
	 */
	private UpdateStrategy determineExpectedStrategy(TextDocumentChange change) {
		String text = change.getText();
		
		// Check for structural changes
		if (text.contains("<") || text.contains(">")) {
			if (text.contains("</") || text.contains("/>")) {
				return UpdateStrategy.SUBTREE;
			}
			return UpdateStrategy.FULL;
		}
		
		// Simple text change
		if (change.getOldLength() == text.length()) {
			return UpdateStrategy.TEXT;
		}
		
		// Default to FULL for safety
		return UpdateStrategy.FULL;
	}
}
