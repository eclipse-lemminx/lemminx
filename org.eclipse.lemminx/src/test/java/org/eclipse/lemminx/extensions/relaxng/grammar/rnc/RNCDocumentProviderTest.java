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
package org.eclipse.lemminx.extensions.relaxng.grammar.rnc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.extensions.relaxng.grammar.rnc.parser.RNCParser;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link IRNCDocumentProvider} is correctly wired through all
 * language features (completion, hover, definition, code actions).
 *
 * <p>Uses an in-memory provider to serve included documents that do not exist
 * on disk, proving the provider is the actual resolution path.</p>
 */
public class RNCDocumentProviderTest {

	private static final String MAIN_URI = "file:///test/main.rnc";
	private static final String BASE_URI = "file:///test/base.rnc";

	private static final CancelChecker NULL_CHECKER = () -> {
	};

	private final Map<String, RNCDocument> inMemoryDocuments = new HashMap<>();
	private RNCLanguageService languageService;

	@BeforeEach
	void setup() {
		inMemoryDocuments.clear();
		languageService = new RNCLanguageService(uri -> inMemoryDocuments.get(uri));
	}

	private RNCDocument registerDocument(String uri, String content) {
		RNCDocument doc = RNCParser.getInstance().parse(content, uri);
		doc.setTextDocument(new TextDocument(content, uri));
		inMemoryDocuments.put(uri, doc);
		return doc;
	}

	private RNCDocument parseMainDocument(String content) {
		RNCDocument doc = RNCParser.getInstance().parse(content, MAIN_URI);
		doc.setTextDocument(new TextDocument(content, MAIN_URI));
		return doc;
	}

	// ------------------- Completion -------------------

	@Test
	public void completionIncludesDefineRefsFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }\n"
				+ "heading = element h1 { text }");

		String mainContent = "include \"base.rnc\"\nfoo = |";
		int offset = mainContent.indexOf('|');
		mainContent = mainContent.replace("|", "");
		RNCDocument mainDoc = parseMainDocument(mainContent);

		SharedSettings settings = XMLAssert.createSharedSettings(false, false);
		Position position = mainDoc.getTextDocument().positionAt(offset);
		CompletionList list = languageService.doComplete(mainDoc, position, settings, NULL_CHECKER);

		assertNotNull(list);
		List<String> labels = list.getItems().stream()
				.map(CompletionItem::getLabel)
				.collect(Collectors.toList());
		assertTrue(labels.contains("paragraph"), "Expected 'paragraph' from included file via provider");
		assertTrue(labels.contains("heading"), "Expected 'heading' from included file via provider");
	}

	@Test
	public void completionIncludeOverridesFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }\n"
				+ "heading = element h1 { text }");

		String mainContent = "include \"base.rnc\" {\n  |\n}";
		int offset = mainContent.indexOf('|');
		mainContent = mainContent.replace("|", "");
		RNCDocument mainDoc = parseMainDocument(mainContent);

		SharedSettings settings = XMLAssert.createSharedSettings(false, false);
		Position position = mainDoc.getTextDocument().positionAt(offset);
		CompletionList list = languageService.doComplete(mainDoc, position, settings, NULL_CHECKER);

		assertNotNull(list);
		List<String> labels = list.getItems().stream()
				.map(CompletionItem::getLabel)
				.collect(Collectors.toList());
		assertTrue(labels.contains("start"), "Expected 'start' override from included file via provider");
		assertTrue(labels.contains("paragraph"), "Expected 'paragraph' override from included file via provider");
		assertTrue(labels.contains("heading"), "Expected 'heading' override from included file via provider");
	}

	@Test
	public void completionWithNullProviderResponseDoesNotCrash() throws BadLocationException {
		// No document registered for base.rnc — provider returns null
		String mainContent = "include \"base.rnc\" {\n  |\n}";
		int offset = mainContent.indexOf('|');
		mainContent = mainContent.replace("|", "");
		RNCDocument mainDoc = parseMainDocument(mainContent);

		SharedSettings settings = XMLAssert.createSharedSettings(false, false);
		Position position = mainDoc.getTextDocument().positionAt(offset);
		CompletionList list = languageService.doComplete(mainDoc, position, settings, NULL_CHECKER);

		assertNotNull(list);
	}

	// ------------------- Hover -------------------

	@Test
	public void hoverOnOverrideShowsOriginalFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\" {\n  paragraph = element p { mixed { text } }\n}";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		// Hover on "paragraph" at line 1, col ~4
		int offset = mainContent.indexOf("paragraph");
		Position position = mainDoc.getTextDocument().positionAt(offset + 2);

		Hover hover = languageService.doHover(mainDoc, position, new SharedSettings(), NULL_CHECKER);
		assertNotNull(hover, "Expected hover on override");
		String hoverText = hover.getContents().getRight().getValue();
		assertTrue(hoverText.contains("Original"), "Expected 'Original' label in hover: " + hoverText);
		assertTrue(hoverText.contains("paragraph = element p { text }"),
				"Expected original define text from provider: " + hoverText);
	}

	@Test
	public void hoverOnIncludeShowsDefinitionsFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\"";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		Position position = new Position(0, 2);
		Hover hover = languageService.doHover(mainDoc, position, new SharedSettings(), NULL_CHECKER);
		assertNotNull(hover, "Expected hover on include");
		String hoverText = hover.getContents().getRight().getValue();
		assertTrue(hoverText.contains("start"), "Expected 'start' from included file: " + hoverText);
		assertTrue(hoverText.contains("paragraph"), "Expected 'paragraph' from included file: " + hoverText);
	}

	@Test
	public void hoverOnIncludeWithNullProviderFallsBackToKeyword() throws BadLocationException {
		// No document registered — provider returns null
		String mainContent = "include \"nonexistent.rnc\"";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		Position position = new Position(0, 2);
		Hover hover = languageService.doHover(mainDoc, position, new SharedSettings(), NULL_CHECKER);
		assertNotNull(hover, "Expected fallback to keyword hover");
		String hoverText = hover.getContents().getRight().getValue();
		assertTrue(hoverText.contains("include"), "Expected keyword documentation: " + hoverText);
	}

	// ------------------- Definition -------------------

	@Test
	public void definitionNavigatesToIncludedFileFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\"\nfoo = paragraph";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		// Click on "paragraph" reference
		int offset = mainContent.lastIndexOf("paragraph");
		Position position = mainDoc.getTextDocument().positionAt(offset + 2);

		List<? extends LocationLink> links = languageService.findDefinition(mainDoc, position, NULL_CHECKER);
		assertFalse(links.isEmpty(), "Expected definition link to included file");
		assertEquals(BASE_URI, links.get(0).getTargetUri());
	}

	@Test
	public void definitionOverrideGoesToOriginalFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\" {\n  paragraph = element p { mixed { text } }\n}";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		// Click on "paragraph" in the override
		int offset = mainContent.indexOf("paragraph");
		Position position = mainDoc.getTextDocument().positionAt(offset + 2);

		List<? extends LocationLink> links = languageService.findDefinition(mainDoc, position, NULL_CHECKER);
		assertFalse(links.isEmpty(), "Expected definition link to original in included file");
		assertEquals(BASE_URI, links.get(0).getTargetUri());
	}

	@Test
	public void definitionWithNullProviderReturnsEmpty() throws BadLocationException {
		// No document registered
		String mainContent = "include \"base.rnc\"\nfoo = paragraph";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		int offset = mainContent.lastIndexOf("paragraph");
		Position position = mainDoc.getTextDocument().positionAt(offset + 2);

		List<? extends LocationLink> links = languageService.findDefinition(mainDoc, position, NULL_CHECKER);
		assertTrue(links.isEmpty(), "Expected no definition when provider returns null");
	}

	// ------------------- Code Actions -------------------

	@Test
	public void codeActionIncludeNotOverridingUsesProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\" {\n  xparagraph = element p { text }\n}";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		Range diagRange = new Range(new Position(1, 2), new Position(1, 12));
		Diagnostic diag = new Diagnostic(diagRange,
				"definition of \"xparagraph\" in \"include\" does not override anything");
		diag.setCode("include-not-overriding");

		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(java.util.Arrays.asList(diag));

		List<CodeAction> actions = languageService.doCodeActions(mainDoc, diagRange, context,
				false, false, NULL_CHECKER);

		assertFalse(actions.isEmpty(), "Expected code actions from provider-resolved included file");
		List<String> titles = actions.stream()
				.map(CodeAction::getTitle)
				.collect(Collectors.toList());
		assertTrue(titles.stream().anyMatch(t -> t.contains("paragraph")),
				"Expected 'paragraph' suggestion from included file: " + titles);
	}

	@Test
	public void codeActionUndefinedPatternIncludesDefinesFromProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\"\nstart = paragrph";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		Range diagRange = new Range(new Position(1, 8), new Position(1, 16));
		Diagnostic diag = new Diagnostic(diagRange,
				"reference to undefined pattern \"paragrph\"");
		diag.setCode("undefined-pattern");

		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(java.util.Arrays.asList(diag));

		List<CodeAction> actions = languageService.doCodeActions(mainDoc, diagRange, context,
				false, false, NULL_CHECKER);

		List<String> titles = actions.stream()
				.map(CodeAction::getTitle)
				.collect(Collectors.toList());
		assertTrue(titles.stream().anyMatch(t -> t.contains("paragraph")),
				"Expected 'Did you mean paragraph?' from included file via provider: " + titles);
	}

	// ------------------- Completion Resolve -------------------

	@Test
	public void resolveCompletionUsesProvider() throws BadLocationException {
		registerDocument(BASE_URI, "start = element doc { text }\n"
				+ "paragraph = element p { text }");

		String mainContent = "include \"base.rnc\" {\n  \n}";
		RNCDocument mainDoc = parseMainDocument(mainContent);

		SharedSettings settings = XMLAssert.createSharedSettings(true, false);
		Position position = mainDoc.getTextDocument().positionAt(mainContent.indexOf('\n') + 3);
		CompletionList list = languageService.doComplete(mainDoc, position, settings, NULL_CHECKER);

		CompletionItem paragraphItem = list.getItems().stream()
				.filter(i -> "paragraph".equals(i.getLabel()))
				.findFirst()
				.orElse(null);
		assertNotNull(paragraphItem, "Expected 'paragraph' completion item");

		CompletionItem resolved = languageService.resolveCompletionItem(paragraphItem, mainDoc, settings,
				NULL_CHECKER);
		assertNotNull(resolved.getDocumentation(),
				"Expected resolved documentation from provider-served document");
	}
}
