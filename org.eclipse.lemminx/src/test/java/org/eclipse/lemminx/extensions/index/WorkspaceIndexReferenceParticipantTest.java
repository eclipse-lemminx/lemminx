/*******************************************************************************
* Copyright (c) 2026 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lemminx.commons.BadLocationException;

import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensParticipant;
import org.eclipse.lemminx.services.extensions.codelens.ICodeLensRequest;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link WorkspaceIndexReferenceParticipant} and
 * {@link WorkspaceIndexCodeLensParticipant}.
 */
public class WorkspaceIndexReferenceParticipantTest {

	@TempDir
	Path tempDir;

	private WorkspaceIndex index;
	private WorkspaceIndexReferenceParticipant refParticipant;
	private WorkspaceIndexCodeLensParticipant lensParticipant;
	private static final CancelChecker NO_CANCEL = () -> {};
	private static final ReferenceContext CONTEXT = new ReferenceContext(false);

	@BeforeEach
	public void setUp() {
		index = new WorkspaceIndex();
		refParticipant = new WorkspaceIndexReferenceParticipant(index);
		lensParticipant = new WorkspaceIndexCodeLensParticipant(index);
	}

	// --- findReferenceByUri ---

	@Test
	public void testFindReferenceByUri_returnsReferences() throws Exception {
		Path xml1 = tempDir.resolve("order.xml");
		Path xml2 = tempDir.resolve("invoice.xml");
		Files.writeString(xml1, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		Files.writeString(xml2, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml1);
		index.indexFile(xml2);
		index.markScanCompleted();

		String xsdUri = tempDir.resolve("schema.xsd").toUri().toString();
		List<Location> locations = refParticipant
				.findReferenceByUri(xsdUri, new Position(0, 1), CONTEXT, NO_CANCEL).get();

		assertEquals(2, locations.size());
	}

	@Test
	public void testFindReferenceByUri_nonGrammarFileReturnsEmpty() throws Exception {
		index.markScanCompleted();
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml);

		// order.xml is not a grammar file — no results expected
		String xmlUri = xml.toUri().toString();
		List<Location> locations = refParticipant
				.findReferenceByUri(xmlUri, new Position(0, 1), CONTEXT, NO_CANCEL).get();

		assertTrue(locations.isEmpty(), "Non-grammar file must return no references");
	}

	@Test
	public void testFindReferenceByUri_noReferencesReturnsEmpty() throws Exception {
		// No XML files reference schema.xsd
		index.markScanCompleted();
		String xsdUri = tempDir.resolve("schema.xsd").toUri().toString();
		List<Location> locations = refParticipant
				.findReferenceByUri(xsdUri, new Position(0, 1), CONTEXT, NO_CANCEL).get();

		assertTrue(locations.isEmpty());
	}

	@Test
	public void testFindReferenceByUri_waitsForScan() throws Exception {
		// Index has files but scan not yet marked complete
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml);

		String xsdUri = tempDir.resolve("schema.xsd").toUri().toString();
		CompletableFuture<List<Location>> future =
				refParticipant.findReferenceByUri(xsdUri, new Position(0, 1), CONTEXT, NO_CANCEL);

		// Future must not be done yet — scan not complete
		assertFalse(future.isDone(), "Future must wait for scan completion");

		// Now complete the scan
		index.markScanCompleted();

		List<Location> locations = future.get();
		assertEquals(1, locations.size(), "Must find the reference after scan completes");
	}

	// --- fileNameOf() ---

	@Test
	public void testFileNameOfSimple() {
		assertEquals("schema.xsd", WorkspaceIndexReferenceParticipant.fileNameOf("schema.xsd"));
	}

	@Test
	public void testFileNameOfWithPath() {
		assertEquals("schema.xsd", WorkspaceIndexReferenceParticipant.fileNameOf("schemas/schema.xsd"));
	}

	@Test
	public void testFileNameOfWithUri() {
		assertEquals("schema.xsd",
				WorkspaceIndexReferenceParticipant.fileNameOf("file:///workspace/schemas/schema.xsd"));
	}

	// --- isGrammarFile() ---

	@Test
	public void testIsGrammarFileXsd() {
		assertTrue(WorkspaceIndexReferenceParticipant.isGrammarFile("file:///schemas/types.xsd"));
	}

	@Test
	public void testIsGrammarFileDtd() {
		assertTrue(WorkspaceIndexReferenceParticipant.isGrammarFile("config.dtd"));
	}

	@Test
	public void testIsGrammarFileRng() {
		assertTrue(WorkspaceIndexReferenceParticipant.isGrammarFile("book.rng"));
	}

	@Test
	public void testIsGrammarFileRnc() {
		assertTrue(WorkspaceIndexReferenceParticipant.isGrammarFile("book.rnc"));
	}

	@Test
	public void testIsGrammarFileXml() {
		assertFalse(WorkspaceIndexReferenceParticipant.isGrammarFile("order.xml"));
	}

	// --- findReference: cursor on grammar attribute value (Case 1) ---

	@Test
	public void testFindReferenceCursorOnAttributeValue() throws IOException, BadLocationException {
		// Two XML files referencing schema.xsd
		String schema = "file:///" + tempDir.toString().replace('\\', '/') + "/schema.xsd";
		Path xml1 = tempDir.resolve("order.xml");
		Path xml2 = tempDir.resolve("invoice.xml");
		Files.writeString(xml1, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		Files.writeString(xml2, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");

		index.indexFile(xml1);
		index.indexFile(xml2);

		// Cursor inside "schema.xsd" attribute value in order.xml
		// Content: <root xsi:noNamespaceSchemaLocation="schema.xsd" ...
		// Position of 's' in "schema.xsd" (after the quote)
		String content = Files.readString(xml1);
		int attrValueOffset = content.indexOf("\"schema.xsd\"") + 1; // skip opening quote
		DOMDocument doc = DOMParser.getInstance().parse(content, xml1.toUri().toString(), null);
		Position position = doc.getTextDocument().positionAt(attrValueOffset);

		List<Location> locations = new ArrayList<>();
		refParticipant.findReference(doc, position, CONTEXT, locations, NO_CANCEL);

		assertEquals(2, locations.size());
	}

	// --- findReference: cursor in grammar file (Case 2) ---

	@Test
	public void testFindReferenceCursorInGrammarFile() throws IOException {
		// Two XML files referencing schema.xsd
		Path xml1 = tempDir.resolve("order.xml");
		Path xml2 = tempDir.resolve("invoice.xml");
		Files.writeString(xml1, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		Files.writeString(xml2, "<root xsi:noNamespaceSchemaLocation=\"schema.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");

		index.indexFile(xml1);
		index.indexFile(xml2);

		// Cursor at position (0,0) in schema.xsd → Case 2
		String xsdContent = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";
		String xsdUri = "file:///" + tempDir.toString().replace('\\', '/') + "/schema.xsd";
		DOMDocument xsdDoc = DOMParser.getInstance().parse(xsdContent, xsdUri, null);
		Position position = new Position(0, 0);

		List<Location> locations = new ArrayList<>();
		refParticipant.findReference(xsdDoc, position, CONTEXT, locations, NO_CANCEL);

		assertEquals(2, locations.size());
	}

	@Test
	public void testFindReferenceNoMatch() throws IOException {
		// XML file referencing other.xsd, not schema.xsd
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, "<root xsi:noNamespaceSchemaLocation=\"other.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml);

		String xsdContent = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";
		String xsdUri = "file:///" + tempDir.toString().replace('\\', '/') + "/schema.xsd";
		DOMDocument xsdDoc = DOMParser.getInstance().parse(xsdContent, xsdUri, null);

		List<Location> locations = new ArrayList<>();
		refParticipant.findReference(xsdDoc, new Position(0, 0), CONTEXT, locations, NO_CANCEL);

		assertTrue(locations.isEmpty());
	}

	// --- findReference: code lens position does not leak XSD-internal refs ---

	/**
	 * Regression: WorkspaceIndexCodeLensParticipant previously used root.getStart() as
	 * the reference-command position. DOMContainerNode.findNodeAt() breaks when
	 * offset == child.getStart(), so findNodeAt(root.getStart()) returned the DOCUMENT
	 * node instead of the root element. XSDReferenceParticipant then treated the query
	 * as a document-level search and returned all internal XSD element cross-references.
	 *
	 * Fix: use root.getStart()+1 so findNodeAt() correctly returns the element.
	 * This test verifies that the code lens position (taken from the actual CodeLens
	 * command arguments) is INSIDE the root tag name and that WorkspaceIndexReferenceParticipant
	 * returns only cross-file references at that position.
	 */
	@Test
	public void testCodeLensPositionIsInsideTagName() throws IOException, BadLocationException {
		// XSD with an internal ref (simulating internal cross-references like ref="tns:product")
		String xsdContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
				+ "  <xs:element name=\"catalog\">\n"
				+ "    <xs:complexType><xs:sequence>\n"
				+ "      <xs:element ref=\"catalog\"/>\n"
				+ "    </xs:sequence></xs:complexType>\n"
				+ "  </xs:element>\n"
				+ "</xs:schema>";

		// XML file on disk that references the XSD
		Path xml = tempDir.resolve("test.xml");
		Files.writeString(xml,
				"<catalog xsi:noNamespaceSchemaLocation=\"schema.xsd\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml);

		String xsdUri = "file:///" + tempDir.toString().replace('\\', '/') + "/schema.xsd";
		DOMDocument xsdDoc = DOMParser.getInstance().parse(xsdContent, xsdUri, null);

		// Get the code lens — it must be present since test.xml references schema.xsd
		List<CodeLens> lenses = new ArrayList<>();
		lensParticipant.doCodeLens(mockRequest(xsdDoc), lenses, NO_CANCEL);
		assertEquals(1, lenses.size(), "Expected 1 code lens on schema.xsd");

		// Extract the position stored in the ReferenceCommand arguments
		List<Object> args = lenses.get(0).getCommand().getArguments();
		Position codeLensPos = (Position) args.get(1);

		// The position must be INSIDE the root element tag name (char > 0 on the root line),
		// not at the '<' character, so that DOMContainerNode.findNodeAt returns the element
		// and not the DOCUMENT node.
		assertTrue(codeLensPos.getCharacter() > 0,
				"Code lens position character must be > 0 to avoid findNodeAt returning DOCUMENT node; got: "
				+ codeLensPos);

		// findReference at the code lens position must return exactly 1 cross-file reference
		// (the test.xml file), not XSD-internal references.
		List<Location> locations = new ArrayList<>();
		refParticipant.findReference(xsdDoc, codeLensPos, CONTEXT, locations, NO_CANCEL);
		assertEquals(1, locations.size(), "Expected exactly 1 cross-file reference");
		assertTrue(locations.get(0).getUri().contains("test.xml"));
	}

	// --- CodeLens participant ---

	@Test
	public void testCodeLensGrammarFile() throws IOException {
		Path xml1 = tempDir.resolve("a.xml");
		Path xml2 = tempDir.resolve("b.xml");
		Files.writeString(xml1, "<root xsi:noNamespaceSchemaLocation=\"types.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		Files.writeString(xml2, "<root xsi:noNamespaceSchemaLocation=\"types.xsd\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
		index.indexFile(xml1);
		index.indexFile(xml2);

		String xsdContent = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";
		String xsdUri = "file:///" + tempDir.toString().replace('\\', '/') + "/types.xsd";
		DOMDocument xsdDoc = DOMParser.getInstance().parse(xsdContent, xsdUri, null);

		List<CodeLens> lenses = new ArrayList<>();
		lensParticipant.doCodeLens(mockRequest(xsdDoc), lenses, NO_CANCEL);

		assertEquals(1, lenses.size());
		assertEquals("2 references", lenses.get(0).getCommand().getTitle());
	}

	@Test
	public void testCodeLensNonGrammarFile() throws IOException {
		String xmlContent = "<root/>";
		String xmlUri = "file:///order.xml";
		DOMDocument xmlDoc = DOMParser.getInstance().parse(xmlContent, xmlUri, null);

		List<CodeLens> lenses = new ArrayList<>();
		lensParticipant.doCodeLens(mockRequest(xmlDoc), lenses, NO_CANCEL);

		assertTrue(lenses.isEmpty());
	}

	@Test
	public void testCodeLensNoReferences() throws IOException {
		String xsdContent = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";
		String xsdUri = "file:///" + tempDir.toString().replace('\\', '/') + "/orphan.xsd";
		DOMDocument xsdDoc = DOMParser.getInstance().parse(xsdContent, xsdUri, null);

		List<CodeLens> lenses = new ArrayList<>();
		lensParticipant.doCodeLens(mockRequest(xsdDoc), lenses, NO_CANCEL);

		assertTrue(lenses.isEmpty());
	}

	// --- collectCrossFileReferences: open modified document uses live positions ---

	@Test
	public void testCollectCrossFileReferences_usesLiveContentWhenDocumentIsOpen() throws IOException {
		// On disk: xml-model PI is on line 1 (after xml declaration)
		String savedContent = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, savedContent);
		index.indexFile(xml);
		index.markScanCompleted();

		// Simulate user adding two comment lines before the xml-model PI (without saving):
		// The ref is now at line 3 instead of line 1.
		String liveContent = "<?xml version=\"1.0\"?>\n" +
				"<!-- added line 1 -->\n" +
				"<!-- added line 2 -->\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		String xmlUri = xml.toUri().toString();

		Path targetPath = tempDir.resolve("schema.xsd");

		List<Location> locations = WorkspaceIndexReferenceParticipant.collectCrossFileReferences(
				index, targetPath, uri -> uri.equals(xmlUri) ? liveContent : null, NO_CANCEL);

		assertEquals(1, locations.size());
		// Must use live position (line 3), not cached position (line 1)
		assertEquals(3, locations.get(0).getRange().getStart().getLine(),
				"References must use live content positions, not cached positions");
	}

	@Test
	public void testCollectCrossFileReferences_usesCachedPositionsWhenDocumentIsClosed() throws IOException {
		// xml-model PI on line 1
		String content = "<?xml version=\"1.0\"?>\n" +
				"<?xml-model href=\"schema.xsd\"?>\n" +
				"<order/>";
		Path xml = tempDir.resolve("order.xml");
		Files.writeString(xml, content);
		index.indexFile(xml);
		index.markScanCompleted();

		Path targetPath = tempDir.resolve("schema.xsd");

		// openDocContent returns null → file closed → use cached positions
		List<Location> locations = WorkspaceIndexReferenceParticipant.collectCrossFileReferences(
				index, targetPath, uri -> null, NO_CANCEL);

		assertEquals(1, locations.size());
		assertEquals(1, locations.get(0).getRange().getStart().getLine(),
				"Closed file must use cached line from index");
	}

	private static ICodeLensRequest mockRequest(DOMDocument document) {
		return new ICodeLensRequest() {
			@Override
			public DOMDocument getDocument() {
				return document;
			}
			@Override
			public boolean isSupportedByClient(String kind) {
				return false;
			}
		};
	}
}
