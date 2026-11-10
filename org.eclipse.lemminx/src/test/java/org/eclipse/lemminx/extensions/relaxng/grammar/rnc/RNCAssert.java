/*******************************************************************************
* Copyright (c) 2024 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.eclipse.lemminx.XMLAssert.r;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.settings.RNCFormattingSettings;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.utils.RNCIncludeUtils;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.validator.RNCValidator;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.utils.TextEditUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.SnippetTextEdit;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class RNCAssert {

	public static final String LS = System.lineSeparator();

	private static final String FILE_URI = "test.rnc";

	private static final String TEST_RESOURCES_DIR = "src/test/resources/relaxng/";

	public static final int NEW_RNC_FILE_SNIPPETS = 4;

	private static final RNCLanguageService LANGUAGE_SERVICE = new RNCLanguageService(
			uri -> RNCIncludeUtils.parseFile(uri));

	private static final CancelChecker NULL_CHECKER = () -> {
	};

	public static RNCDocument parse(String value, String fileUri) {
		TextDocument document = new TextDocument(value, fileUri);
		RNCDocument rncDocument = RNCParser.getInstance().parse(value, fileUri);
		rncDocument.setTextDocument(document);
		return rncDocument;
	}

	public static RNCDocument parse(String value) {
		return parse(value, FILE_URI);
	}

	public static String testFileUri(String filename) {
		return new File(TEST_RESOURCES_DIR + filename).toURI().toString();
	}

	public static String testFileContent(String filename) throws IOException {
		return Files.readString(Path.of(TEST_RESOURCES_DIR + filename));
	}

	// ------------------- Completion assert

	public static CompletionList testCompletionFor(String value, CompletionItem... expectedItems)
			throws BadLocationException {
		return testCompletionFor(value, false, null, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, boolean snippetsSupported,
			CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(value, snippetsSupported, null, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(value, false, expectedCount, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, boolean snippetsSupported, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(value, FILE_URI, snippetsSupported, expectedCount, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, String fileUri, boolean snippetsSupported,
			Integer expectedCount, CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(value, fileUri, snippetsSupported, false, expectedCount, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, boolean snippetsSupported,
			boolean triggerSuggestSupported, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(value, FILE_URI, snippetsSupported, triggerSuggestSupported, expectedCount,
				expectedItems);
	}

	public static CompletionList testCompletionFor(String value, String fileUri, boolean snippetsSupported,
			boolean triggerSuggestSupported, Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		SharedSettings settings = XMLAssert.createSharedSettings(snippetsSupported, triggerSuggestSupported);
		return testCompletionFor(value, fileUri, settings, expectedCount, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, String fileUri, SharedSettings sharedSettings,
			Integer expectedCount, CompletionItem... expectedItems) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value, fileUri);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		CompletionList list = LANGUAGE_SERVICE.doComplete(rncDocument, position, sharedSettings, NULL_CHECKER);
		assertNotNull(list);

		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), list.getItems().size());
		}
		if (expectedItems != null) {
			for (CompletionItem expected : expectedItems) {
				XMLAssert.assertCompletion(list, expected, expectedCount);
			}
		}
		return list;
	}

	// ------------------- Completion Resolve assert

	public static void assertResolveCompl(String value, String itemLabel, String expectedDoc)
			throws BadLocationException {
		assertResolveCompl(value, FILE_URI, itemLabel, expectedDoc);
	}

	public static void assertResolveCompl(String value, String fileUri, String itemLabel, String expectedDoc)
			throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value, fileUri);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		SharedSettings settings = XMLAssert.createSharedSettings(true, false);
		CompletionList list = LANGUAGE_SERVICE.doComplete(rncDocument, position, settings, NULL_CHECKER);
		assertNotNull(list);

		CompletionItem item = list.getItems().stream()
				.filter(i -> itemLabel.equals(i.getLabel()))
				.findFirst()
				.orElse(null);
		assertNotNull(item, "Expected completion item '" + itemLabel + "' not found in: "
				+ list.getItems().stream().map(CompletionItem::getLabel).collect(Collectors.joining(", ")));

		LANGUAGE_SERVICE.resolveCompletionItem(item, rncDocument, settings, NULL_CHECKER);

		assertNotNull(item.getDocumentation(), "Expected documentation for '" + itemLabel + "'");
		Either<String, MarkupContent> doc = item.getDocumentation();
		assertTrue(doc.isRight(), "Expected MarkupContent documentation for '" + itemLabel + "'");
		String actualDoc = doc.getRight().getValue();
		assertTrue(actualDoc.contains(expectedDoc),
				() -> "Expected documentation to contain '" + expectedDoc + "' but was: " + actualDoc);
	}

	// ------------------- Hover assert

	public static void assertHover(String value) throws BadLocationException {
		assertHover(value, null, null);
	}

	public static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		assertHover(value, FILE_URI, expectedHoverLabel, expectedHoverRange);
	}

	public static void assertHover(String value, String fileUri, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value, fileUri);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		SharedSettings sharedSettings = new SharedSettings();
		HoverCapabilities capabilities = new HoverCapabilities(Arrays.asList(MarkupKind.MARKDOWN), false);
		sharedSettings.getHoverSettings().setCapabilities(capabilities);

		Hover hover = LANGUAGE_SERVICE.doHover(rncDocument, position, sharedSettings, NULL_CHECKER);
		XMLAssert.assertHover(expectedHoverLabel, expectedHoverRange, hover);
	}

	// ------------------- Document Highlight assert

	public static void testHighlightsFor(String value, DocumentHighlight... expected) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		List<DocumentHighlight> actual = LANGUAGE_SERVICE.findDocumentHighlights(rncDocument, position,
				NULL_CHECKER);
		XMLAssert.assertDocumentHighlight(actual, expected);
	}

	public static DocumentHighlight hl(Range range, DocumentHighlightKind kind) {
		return new DocumentHighlight(range, kind);
	}

	// ------------------- Definition assert

	public static void testDefinitionFor(String value, LocationLink... expected) throws BadLocationException {
		testDefinitionFor(value, FILE_URI, expected);
	}

	public static void testDefinitionFor(String value, String fileUri, LocationLink... expected)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value, fileUri);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		List<? extends LocationLink> actual = LANGUAGE_SERVICE.findDefinition(rncDocument, position,
				NULL_CHECKER);
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			LocationLink expectedLink = expected[i];
			LocationLink actualLink = actual.get(i);
			if (expectedLink.getTargetUri() != null) {
				assertEquals(
						expectedLink.getTargetUri().replace("file:///", "file:/"),
						actualLink.getTargetUri().replace("file:///", "file:/"));
			}
			if (expectedLink.getTargetRange() != null) {
				assertEquals(expectedLink.getTargetRange(), actualLink.getTargetRange());
			}
			if (expectedLink.getOriginSelectionRange() != null) {
				assertEquals(expectedLink.getOriginSelectionRange(), actualLink.getOriginSelectionRange());
			}
		}
	}

	// ------------------- Diagnostics assert

	public static void testDiagnosticsFor(String value, String fileUri, Diagnostic... expected) {
		TextDocument document = new TextDocument(value, fileUri);
		List<Diagnostic> actual = RNCValidator.validate(document, NULL_CHECKER);
		XMLAssert.assertDiagnostics(actual, expected);
	}

	public static void testDiagnosticsFor(String value, Diagnostic... expected) {
		testDiagnosticsFor(value, FILE_URI, expected);
	}

	public static void testNoParserErrors(String value) {
		RNCDocument rncDocument = RNCParser.getInstance().parse(value, FILE_URI);
		assertTrue(rncDocument.getErrors().isEmpty(),
				() -> "Expected no parser errors but got: " + rncDocument.getErrors().stream()
						.map(e -> e.getErrorCode().getCode() + ": " + e.getMessage())
						.collect(Collectors.joining(", ")));
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, String code) {
		Diagnostic d = new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), "");
		d.setCode(code);
		return d;
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, String code,
			String message) {
		Diagnostic d = new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), message);
		d.setCode(code);
		return d;
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter,
			IXMLErrorCode code, DiagnosticTag... tags) {
		return XMLAssert.d(startLine, startCharacter, endLine, endCharacter, code, tags);
	}

	// ------------------- Code Action assert

	public static List<CodeAction> testCodeActionsFor(String value, Diagnostic diagnostic, CodeAction... expected)
			throws BadLocationException {
		return testCodeActionsFor(value, FILE_URI, false, diagnostic, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String value, String fileUri, Diagnostic diagnostic,
			CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(value, fileUri, false, diagnostic, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String value, boolean snippetSupported, Diagnostic diagnostic,
			CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(value, FILE_URI, snippetSupported, diagnostic, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String value, String fileUri, boolean snippetSupported,
			Diagnostic diagnostic, CodeAction... expected) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		if (offset != -1) {
			value = value.substring(0, offset) + value.substring(offset + 1);
		}

		RNCDocument rncDocument = parse(value, fileUri);
		TextDocument document = rncDocument.getTextDocument();
		Range range;
		if (offset != -1) {
			Position position = document.positionAt(offset);
			range = new Range(position, position);
		} else if (diagnostic != null) {
			range = diagnostic.getRange();
		} else {
			range = new Range(new Position(0, 0), new Position(0, 0));
		}

		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(diagnostic != null ? Arrays.asList(diagnostic) : Collections.emptyList());

		List<CodeAction> actual = LANGUAGE_SERVICE.doCodeActions(rncDocument, range, context,
				snippetSupported, false, NULL_CHECKER);
		assertEquals(expected.length, actual.size());

		for (int i = 0; i < expected.length; i++) {
			CodeAction exp = expected[i];
			CodeAction act = actual.get(i);
			if (exp.getTitle() != null && !exp.getTitle().isEmpty()) {
				assertEquals(exp.getTitle(), act.getTitle());
			}
			assertCodeActionEdits(exp, act);
		}
		return actual;
	}

	private static void assertCodeActionEdits(CodeAction expected, CodeAction actual) {
		if (expected.getEdit() == null) {
			return;
		}
		assertNotNull(actual.getEdit());

		List<TextEdit> expectedEdits = getTextEdits(expected.getEdit());
		List<TextEdit> actualEdits = getTextEdits(actual.getEdit());

		assertEquals(expectedEdits.size(), actualEdits.size());
		for (int j = 0; j < expectedEdits.size(); j++) {
			assertEquals(expectedEdits.get(j).getRange(), actualEdits.get(j).getRange());
			assertEquals(expectedEdits.get(j).getNewText(), actualEdits.get(j).getNewText());
		}
	}

	private static List<TextEdit> getTextEdits(WorkspaceEdit edit) {
		if (edit.getChanges() != null && !edit.getChanges().isEmpty()) {
			return edit.getChanges().values().iterator().next();
		}
		if (edit.getDocumentChanges() != null && !edit.getDocumentChanges().isEmpty()) {
			TextDocumentEdit docEdit = edit.getDocumentChanges().get(0).getLeft();
			List<TextEdit> result = new ArrayList<>();
			for (Either<TextEdit, SnippetTextEdit> e : docEdit.getEdits()) {
				if (e.isLeft()) {
					result.add(e.getLeft());
				} else {
					SnippetTextEdit se = e.getRight();
					result.add(new TextEdit(se.getRange(), se.getSnippet().getValue()));
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	public static CodeAction ca(Diagnostic d, TextEdit... te) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle("");
		codeAction.setDiagnostics(Arrays.asList(d));
		WorkspaceEdit workspaceEdit = new WorkspaceEdit();
		workspaceEdit.setChanges(Collections.singletonMap(FILE_URI, Arrays.asList(te)));
		codeAction.setEdit(workspaceEdit);
		return codeAction;
	}

	public static CodeAction ca(String title, TextEdit... te) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle(title);
		WorkspaceEdit workspaceEdit = new WorkspaceEdit();
		workspaceEdit.setChanges(Collections.singletonMap(FILE_URI, Arrays.asList(te)));
		codeAction.setEdit(workspaceEdit);
		return codeAction;
	}

	// ------------------- CodeLens assert

	public static void testCodeLensFor(String value, CodeLens... expected) {
		RNCDocument rncDocument = parse(value);
		List<CodeLens> actual = LANGUAGE_SERVICE.getCodeLens(rncDocument, false, NULL_CHECKER);
		XMLAssert.assertCodeLens(actual, expected);
	}

	public static CodeLens cl(Range range, String title) {
		return new CodeLens(range, new Command(title, ""), null);
	}

	// ------------------- Folding assert

	public static void testFoldingFor(String value, FoldingRange... expected) {
		RNCDocument rncDocument = parse(value);
		List<FoldingRange> actual = LANGUAGE_SERVICE.getFoldingRanges(rncDocument, NULL_CHECKER);

		assertEquals(expected.length, actual.size(),
				() -> "Expected " + expected.length + " folding ranges but got " + actual.size() + ": "
						+ actual.stream()
								.map(f -> "[" + f.getStartLine() + "-" + f.getEndLine() + ":" + f.getKind() + "]")
								.collect(Collectors.joining(",")));

		List<FoldingRange> sortedActual = actual.stream()
				.sorted((a, b) -> a.getStartLine() - b.getStartLine())
				.collect(Collectors.toList());
		List<FoldingRange> sortedExpected = Arrays.stream(expected)
				.sorted((a, b) -> a.getStartLine() - b.getStartLine())
				.collect(Collectors.toList());

		for (int i = 0; i < sortedExpected.size(); i++) {
			assertEquals(sortedExpected.get(i).getStartLine(), sortedActual.get(i).getStartLine());
			assertEquals(sortedExpected.get(i).getEndLine(), sortedActual.get(i).getEndLine());
			if (sortedExpected.get(i).getKind() != null) {
				assertEquals(sortedExpected.get(i).getKind(), sortedActual.get(i).getKind());
			}
		}
	}

	public static FoldingRange fr(int startLine, int endLine) {
		return new FoldingRange(startLine, endLine);
	}

	public static FoldingRange fr(int startLine, int endLine, String kind) {
		FoldingRange range = new FoldingRange(startLine, endLine);
		range.setKind(kind);
		return range;
	}

	// ------------------- Document Links assert

	public static void testDocumentLinksFor(String value, DocumentLink... expected) {
		testDocumentLinksFor(value, FILE_URI, expected);
	}

	public static void testDocumentLinksFor(String value, String fileUri, DocumentLink... expected) {
		TextDocument document = new TextDocument(value, fileUri);
		RNCDocument rncDocument = RNCParser.getInstance().parse(value, fileUri);
		rncDocument.setTextDocument(document);
		List<DocumentLink> actual = LANGUAGE_SERVICE.findDocumentLinks(rncDocument, NULL_CHECKER);

		assertEquals(expected.length, actual.size(),
				() -> "Expected " + expected.length + " document links but got " + actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange(),
					"Document link " + i + " range");
			if (expected[i].getTarget() != null) {
				assertEquals(expected[i].getTarget(), actual.get(i).getTarget(),
						"Document link " + i + " target");
			}
		}
	}

	public static DocumentLink dl(Range range, String target) {
		return new DocumentLink(range, target);
	}

	public static DocumentLink dl(Range range) {
		return new DocumentLink(range, null);
	}

	// ------------------- References assert

	public static void testReferencesFor(String value, Location... expected) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		ReferenceContext context = new ReferenceContext(false);
		List<? extends Location> actual = LANGUAGE_SERVICE.findReferences(rncDocument, position, context,
				NULL_CHECKER);
		XMLAssert.assertLocation(actual, expected);
	}

	public static Location l(Range range) {
		return new Location(FILE_URI, range);
	}

	// ------------------- Rename assert

	public static void testRenameFor(String value, String newName, TextEdit... expected) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		WorkspaceEdit workspaceEdit = LANGUAGE_SERVICE.doRename(rncDocument, position, newName, NULL_CHECKER);
		assertNotNull(workspaceEdit);

		List<TextEdit> actual = workspaceEdit.getChanges().get(FILE_URI);
		assertNotNull(actual);
		assertEquals(expected.length, actual.size(),
				() -> "Expected " + expected.length + " edits but got " + actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			assertEquals(expected[i].getNewText(), actual.get(i).getNewText());
		}
	}

	// ------------------- Prepare Rename assert

	public static void testPrepareRenameFor(String value, PrepareRenameResult expected) throws BadLocationException {
		int offset = value.lastIndexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		RNCDocument rncDocument = parse(value);
		Position position = rncDocument.getTextDocument().positionAt(offset);

		Either<Range, PrepareRenameResult> result = LANGUAGE_SERVICE.prepareRename(rncDocument, position);
		if (expected == null) {
			assertNull(result, "Expected no prepare rename result");
		} else {
			assertNotNull(result, "Expected a prepare rename result");
			PrepareRenameResult actual = result.getRight();
			assertNotNull(actual);
			assertEquals(expected.getRange(), actual.getRange());
			assertEquals(expected.getPlaceholder(), actual.getPlaceholder());
		}
	}

	public static PrepareRenameResult pr(Range range, String placeholder) {
		return new PrepareRenameResult(range, placeholder);
	}

	// ------------------- Formatting assert

	public static void assertFormat(String unformatted, String expected) throws BadLocationException {
		assertFormat(unformatted, expected, new SharedSettings(), null);
	}

	public static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings)
			throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, null);
	}

	public static void assertFormat(String unformatted, String expected,
			RNCFormattingSettings rncFormattingSettings) throws BadLocationException {
		assertFormat(unformatted, expected, new SharedSettings(), rncFormattingSettings);
	}

	public static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings,
			RNCFormattingSettings rncFormattingSettings) throws BadLocationException {
		TextDocument document = new TextDocument(unformatted, FILE_URI);
		List<? extends TextEdit> edits = LANGUAGE_SERVICE.format(document, sharedSettings, rncFormattingSettings);
		String formatted = TextEditUtils.applyEdits(document, edits);
		assertEquals(expected, formatted);
	}

	// ------------------- Document Symbols assert

	public static void testDocumentSymbolsFor(String value, DocumentSymbol... expected) {
		RNCDocument rncDocument = parse(value);
		List<DocumentSymbol> actual = LANGUAGE_SERVICE.findDocumentSymbols(rncDocument, NULL_CHECKER);

		assertEquals(expected.length, actual.size(),
				() -> "Expected " + expected.length + " symbols but got " + actual.size() + ": "
						+ actual.stream().map(DocumentSymbol::getName).collect(Collectors.joining(", ")));
		for (int i = 0; i < expected.length; i++) {
			assertDocumentSymbol(expected[i], actual.get(i), "Symbol " + i);
		}
	}

	private static void assertDocumentSymbol(DocumentSymbol expected, DocumentSymbol actual, String prefix) {
		assertEquals(expected.getName(), actual.getName(), prefix + " name");
		assertEquals(expected.getKind(), actual.getKind(), prefix + " kind");
		assertEquals(expected.getRange(), actual.getRange(), prefix + " range");
		assertEquals(expected.getSelectionRange(), actual.getSelectionRange(), prefix + " selectionRange");
		List<DocumentSymbol> expectedChildren = expected.getChildren();
		List<DocumentSymbol> actualChildren = actual.getChildren();
		if (expectedChildren != null && !expectedChildren.isEmpty()) {
			assertNotNull(actualChildren, prefix + " expected children but got none");
			assertEquals(expectedChildren.size(), actualChildren.size(),
					prefix + " children count");
			for (int j = 0; j < expectedChildren.size(); j++) {
				assertDocumentSymbol(expectedChildren.get(j), actualChildren.get(j),
						prefix + " > child " + j);
			}
		}
	}

	public static void testSymbolInformationsFor(String value, SymbolInformation... expected) {
		RNCDocument rncDocument = parse(value);
		List<SymbolInformation> actual = LANGUAGE_SERVICE.findSymbolInformations(rncDocument, NULL_CHECKER);
		XMLAssert.assertSymbolInformations(actual, expected);
	}

	public static SymbolInformation si(String name, SymbolKind kind, Range range) {
		return XMLAssert.si(name, kind, new Location(FILE_URI, range), null);
	}

	public static DocumentSymbol ds(String name, SymbolKind kind, Range range, Range selectionRange,
			DocumentSymbol... children) {
		DocumentSymbol symbol = new DocumentSymbol(name, kind, range, selectionRange);
		if (children.length > 0) {
			List<DocumentSymbol> childList = new ArrayList<>();
			for (DocumentSymbol child : children) {
				childList.add(child);
			}
			symbol.setChildren(childList);
		}
		return symbol;
	}

	// ------------------- Code Action Integrity assert

	public static void testCodeActionProducesValidRNC(String value, Diagnostic diagnostic,
			CodeAction... expected) throws BadLocationException {
		testCodeActionProducesValidRNC(value, false, diagnostic, expected);
	}

	public static void testCodeActionProducesValidRNC(String value, boolean snippetSupported,
			Diagnostic diagnostic, CodeAction... expected) throws BadLocationException {
		List<CodeAction> actions = testCodeActionsFor(value, snippetSupported, diagnostic, expected);
		for (CodeAction action : actions) {
			String result = applyCodeAction(value, action);
			RNCDocument resultDoc = RNCParser.getInstance().parse(result, FILE_URI);
			assertTrue(resultDoc.getErrors().isEmpty(),
					() -> "Code action '" + action.getTitle() + "' produced invalid RNC.\nResult:\n" + result
							+ "\nErrors: " + resultDoc.getErrors().stream()
									.map(e -> e.getErrorCode().getCode() + ": " + e.getMessage())
									.collect(Collectors.joining(", ")));
		}
	}

	public static String applyCodeAction(String value, CodeAction action) throws BadLocationException {
		TextDocument document = new TextDocument(value, FILE_URI);
		List<TextEdit> edits = getTextEdits(action.getEdit());
		List<TextEdit> cleanEdits = new ArrayList<>();
		for (TextEdit edit : edits) {
			String newText = stripSnippets(edit.getNewText());
			cleanEdits.add(new TextEdit(edit.getRange(), newText));
		}
		return TextEditUtils.applyEdits(document, cleanEdits);
	}

	private static String stripSnippets(String text) {
		return text.replaceAll("\\$\\{\\d+:([^}]*)}", "$1").replaceAll("\\$\\d+", "");
	}

}
