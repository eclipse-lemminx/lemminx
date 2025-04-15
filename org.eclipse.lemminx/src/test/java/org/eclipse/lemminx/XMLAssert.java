/**
 *  Copyright (c) 2018, 2023 Angelo ZERR
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  Angelo Zerr <angelo.zerr@gmail.com> - initial API and implementation
 */
package org.eclipse.lemminx;

import static org.eclipse.lemminx.utils.TextEditUtils.applyEdits;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lemminx.client.CodeLensKind;
import org.eclipse.lemminx.client.CodeLensKindCapabilities;
import org.eclipse.lemminx.client.ExtendedCodeLensCapabilities;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.commons.TextDocument;
import org.eclipse.lemminx.customservice.AutoCloseTagResponse;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMParser;
import org.eclipse.lemminx.extensions.colors.settings.XMLColorsSettings;
import org.eclipse.lemminx.extensions.contentmodel.commands.SurroundWithCommand;
import org.eclipse.lemminx.extensions.contentmodel.commands.SurroundWithCommand.SurroundWithKind;
import org.eclipse.lemminx.extensions.contentmodel.commands.SurroundWithCommand.SurroundWithResponse;
import org.eclipse.lemminx.extensions.contentmodel.settings.ContentModelSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.SchemaEnabled;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLSchemaSettings;
import org.eclipse.lemminx.extensions.contentmodel.settings.XMLValidationRootSettings;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorManager;
import org.eclipse.lemminx.extensions.generators.FileContentGeneratorSettings;
import org.eclipse.lemminx.services.XMLLanguageService;
import org.eclipse.lemminx.services.extensions.diagnostics.IXMLErrorCode;
import org.eclipse.lemminx.services.extensions.save.AbstractSaveContext;
import org.eclipse.lemminx.settings.SharedSettings;
import org.eclipse.lemminx.settings.XMLCodeLensSettings;
import org.eclipse.lemminx.settings.XMLSymbolSettings;
import org.eclipse.lemminx.settings.capabilities.CompletionResolveSupportProperty;
import org.eclipse.lemminx.utils.StringUtils;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Color;
import org.eclipse.lsp4j.ColorInformation;
import org.eclipse.lsp4j.ColorPresentation;
import org.eclipse.lsp4j.ColorPresentationParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemCapabilities;
import org.eclipse.lsp4j.CompletionItemResolveSupportCapabilities;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionListCapabilities;
import org.eclipse.lsp4j.CreateFile;
import org.eclipse.lsp4j.CreateFileOptions;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticRelatedInformation;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightKind;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverCapabilities;
import org.eclipse.lsp4j.LinkedEditingRanges;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ResourceOperation;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Assertions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * XML Assert
 *
 */
public class XMLAssert {

	// ------------------- Completion assert

	public static final int COMMENT_SNIPPETS = 1;

	public static final int CDATA_SNIPPETS = 1;

	public static final int DOCTYPE_SNIPPETS = 5;

	public static final int DTDNODE_SNIPPETS = 4;

	public static final int NEW_XML_SNIPPETS = 8;

	public static final int NEW_XSD_SNIPPETS = 1;

	public static final int XML_DECLARATION_SNIPPETS = 2;

	public static final int PROCESSING_INSTRUCTION_SNIPPETS = 4;

	public static final int REGION_SNIPPETS = 2;

	public static final int CATALOG_SNIPPETS = 3;

	private static final String FILE_URI = "test.xml";

	private static final CancelChecker NULL_CHECKER = () -> {
	};

	public static class SettingsSaveContext extends AbstractSaveContext {

		public SettingsSaveContext(Object settings) {
			super(settings);
		}

		@Override
		public DOMDocument getDocument(String uri) {
			return null;
		}

		@Override
		public void collectDocumentToValidate(Predicate<DOMDocument> validateDocumentPredicate) {

		}

	}

	public static CompletionList testCompletionFor(String value, CompletionItem... expectedItems)
			throws BadLocationException {
		return testCompletionFor(value, null, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, String catalogPath, CompletionItem... expectedItems)
			throws BadLocationException {
		return testCompletionFor(value, catalogPath, null, null, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, int expectedCount, CompletionItem... expectedItems)
			throws BadLocationException {
		return testCompletionFor(value, null, null, expectedCount, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, Integer expectedCount, boolean enableItemDefaults,
			CompletionItem... expectedItems)
			throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		return testCompletionFor(new XMLLanguageService(), value, null, null, null, expectedCount, settings,
				enableItemDefaults, expectedItems);
	}

	public static CompletionList testCompletionFor(String value, String catalogPath, String fileURI,
			Integer expectedCount,
			CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(new XMLLanguageService(), value, catalogPath, null, fileURI, expectedCount, true,
				expectedItems);
	}

	public static CompletionList testCompletionFor(String value, boolean autoCloseTags, CompletionItem... expectedItems)
			throws BadLocationException {
		return testCompletionFor(new XMLLanguageService(), value, null, null, null, null, autoCloseTags, expectedItems);
	}

	public static CompletionList testCompletionFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath, Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			boolean autoCloseTags, CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(xmlLanguageService, value,
				catalogPath, customConfiguration, fileURI, expectedCount,
				autoCloseTags, false, expectedItems);
	}

	public static CompletionList testCompletionFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath, Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			boolean autoCloseTags, boolean enableItemDefaults, CompletionItem... expectedItems)
			throws BadLocationException {

		SharedSettings settings = new SharedSettings();
		settings.getCompletionSettings().setAutoCloseTags(autoCloseTags);
		return testCompletionFor(xmlLanguageService, value, catalogPath, customConfiguration, fileURI, expectedCount,
				settings, enableItemDefaults, expectedItems);
	}

	public static CompletionList testCompletionFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath, Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			SharedSettings sharedSettings, CompletionItem... expectedItems) throws BadLocationException {
		return testCompletionFor(xmlLanguageService, value, catalogPath, customConfiguration, fileURI, expectedCount,
				sharedSettings, false, expectedItems);
	}

	public static CompletionList testCompletionFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath,
			Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			SharedSettings sharedSettings, boolean enableItemDefaults, CompletionItem... expectedItems)
			throws BadLocationException {
		if (enableItemDefaults) {
			if (sharedSettings.getCompletionSettings().getCompletionCapabilities() == null) {
				CompletionCapabilities completionCapabilities = new CompletionCapabilities();
				sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);
			}
			if (sharedSettings.getCompletionSettings().getCompletionCapabilities().getCompletionList() == null) {
				CompletionListCapabilities completionListCapabilities = new CompletionListCapabilities();
				sharedSettings.getCompletionSettings().getCompletionCapabilities()
						.setCompletionList(completionListCapabilities);
			}
			sharedSettings.getCompletionSettings().getCompletionCapabilities().getCompletionList()
					.setItemDefaults(Arrays.asList("insertTextFormat", "editRange"));
		}
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);
		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> htmlDoc);

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		CompletionList list = xmlLanguageService.doComplete(htmlDoc, position, sharedSettings);

		// no duplicate labels
		List<String> labels = list.getItems().stream().map(i -> i.getLabel()).sorted().collect(Collectors.toList());
		String previous = null;
		for (String label : labels) {
			if (expectedCount != null) {
				continue;
			}
			assertNotEquals(previous, label, () -> {
				return "Duplicate label " + label + " in " + labels.stream().collect(Collectors.joining(",")) + "}";
			});
			previous = label;
		}
		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), list.getItems().size());
		}
		if (expectedItems != null) {
			for (CompletionItem item : expectedItems) {
				assertCompletion(list, item, enableItemDefaults, expectedCount);
			}
		}
		return list;
	}

	public static void assertCompletion(CompletionList completions, CompletionItem expected, Integer expectedCount) {
		assertCompletion(completions, expected, false, expectedCount);
	}

	public static void assertCompletion(CompletionList completions, CompletionItem expected, boolean enableItemDefaults,
			Integer expectedCount) {
		List<CompletionItem> matches = completions.getItems().stream().filter(completion -> {
			return expected.getLabel().equals(completion.getLabel());
		}).collect(Collectors.toList());

		if (expectedCount != null) {
			assertTrue(matches.size() >= 1, () -> {
				return expected.getLabel() + " should only exist once: Actual: "
						+ completions.getItems().stream().map(c -> c.getLabel()).collect(Collectors.joining(","));
			});
		} else {
			assertEquals(1, matches.size(), () -> {
				return expected.getLabel() + " should only exist once: Actual: "
						+ completions.getItems().stream().map(c -> c.getLabel()).collect(Collectors.joining(","));
			});
		}

		CompletionItem match = getCompletionMatch(matches, enableItemDefaults, expected);
		if (!enableItemDefaults && expected.getTextEdit() != null && match.getTextEdit() != null) {
			if (expected.getTextEdit().getLeft().getNewText() != null) {
				assertEquals(expected.getTextEdit().getLeft().getNewText(), match.getTextEdit().getLeft().getNewText());
			}
			Range r = expected.getTextEdit().getLeft().getRange();
			if (r != null && r.getStart() != null && r.getEnd() != null) {
				assertEquals(expected.getTextEdit().getLeft().getRange(), match.getTextEdit().getLeft().getRange());
			}

			if (expected.getAdditionalTextEdits() != null) {
				List<TextEdit> matchedAdditionalTextEdits = match.getAdditionalTextEdits() != null
						? match.getAdditionalTextEdits()
						: Collections.emptyList();
				assertEquals(expected.getAdditionalTextEdits().size(),
						matchedAdditionalTextEdits.size());
				assertArrayEquals(expected.getAdditionalTextEdits().toArray(),
						matchedAdditionalTextEdits.toArray());
			}
		} else {
			assertNull(match.getTextEdit());
			assertNull(match.getInsertTextFormat());
			if (match.getTextEditText() != null) {
				assertEquals(expected.getTextEdit().getLeft().getNewText(), match.getTextEditText());
			}
			Range r = expected.getTextEdit().getLeft().getRange();
			if (r != null && r.getStart() != null && r.getEnd() != null) {
				assertEquals(expected.getTextEdit().getLeft().getRange(),
						completions.getItemDefaults().getEditRange().getLeft());
			}
		}
		if (expected.getFilterText() != null && match.getFilterText() != null) {
			assertEquals(expected.getFilterText(), match.getFilterText());
		}

		if (expected.getDocumentation() != null) {
			assertEquals(expected.getDocumentation(), match.getDocumentation());
		}
	}

	public static void testCompletionApply(String value, CompletionItem completionItem, String expected)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);
		TextDocument document = new TextDocument(value, "test.xml");
		List<TextEdit> edits = new ArrayList<>();
		edits.add(completionItem.getTextEdit().getLeft());
		if (completionItem.getAdditionalTextEdits() != null) {
			edits.addAll(completionItem.getAdditionalTextEdits());
		}
		String actual = applyEdits(document, edits);
		assertEquals(expected, actual);
	}

	public static void assertAdditionalTextEdit(List<TextEdit> matches, TextEdit expected) {
		for (TextEdit match : matches) {
			if (expected.getNewText() != null && expected.getNewText().equals(match.getNewText())) {
				org.eclipse.lsp4j.Range r = expected.getRange();
				if (r != null && r.getStart() != null && r.getEnd() != null) {
					assertEquals(expected.getRange(), match.getRange());
				}

				// New Text and Range of this match are equals to the expected ones
				return;
			}
		}
		fail("No AdditionalTextEdit match found for " + expected.toString());
	}

	private static CompletionItem getCompletionMatch(List<CompletionItem> matches, CompletionItem expected) {
		return getCompletionMatch(matches, false, expected);
	}

	private static CompletionItem getCompletionMatch(List<CompletionItem> matches, boolean enableItemDefaults,
			CompletionItem expected) {
		for (CompletionItem item : matches) {
			if (!enableItemDefaults && expected.getTextEdit().getLeft().getNewText()
					.equals(item.getTextEdit().getLeft().getNewText())) {
				return item;
			} else if (expected.getTextEdit().getLeft().getNewText().equals(item.getTextEditText())) {
				return item;
			}
		}
		return matches.get(0);
	}

	public static CompletionItem c(String label, TextEdit textEdit, String filterText, String documentation) {
		return c(label, textEdit, filterText, documentation, null);
	}

	public static CompletionItem c(String label, TextEdit textEdit, String filterText, String documentation,
			String kind) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setFilterText(filterText);
		item.setTextEdit(Either.forLeft(textEdit));
		if (kind == null) {
			item.setDocumentation(documentation);
		} else {
			item.setDocumentation(new MarkupContent(kind, documentation));
		}
		return item;
	}

	public static CompletionItem c(String label, TextEdit textEdit, String filterText) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setFilterText(filterText);
		item.setTextEdit(Either.forLeft(textEdit));
		return item;
	}

	/**
	 * Creates a completion item with a list of additional text edits
	 *
	 * @since 0.20.0
	 * @param label
	 * @param textEdit
	 * @param additionalTextEdits
	 * @param filterText
	 * @return
	 */
	public static CompletionItem c(String label, TextEdit textEdit, List<TextEdit> additionalTextEdits,
			String filterText) {
		CompletionItem item = new CompletionItem();
		item.setLabel(label);
		item.setFilterText(filterText);
		item.setTextEdit(Either.forLeft(textEdit));
		item.setAdditionalTextEdits(additionalTextEdits);
		return item;
	}

	public static CompletionItem c(String label, String newText) {
		return c(label, newText, null);
	}

	public static CompletionItem c(String label, String newText, String filterText) {
		return c(label, newText, new Range(), filterText);
	}

	public static CompletionItem c(String label, String newText, Range range, String filterText) {
		return c(label, new TextEdit(range, newText), filterText);
	}

	public static void testTagCompletion(String value, String expected) throws BadLocationException {
		testTagCompletion(value, expected, new SharedSettings());
	}

	public static void testTagCompletion(String value, String expected, SharedSettings settings)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		XMLLanguageService ls = new XMLLanguageService();

		TextDocument document = new TextDocument(value, "test://test/test.html");
		Position position = document.positionAt(offset);
		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, ls.getResolverExtensionManager());

		AutoCloseTagResponse response = ls.doTagComplete(htmlDoc, settings.getCompletionSettings(), position);
		if (expected == null) {
			assertNull(response);
			return;
		}
		String actual = response.snippet;
		assertEquals(expected, actual);
	}

	public static void testTagCompletion(String value, AutoCloseTagResponse expected, SharedSettings settings)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		XMLLanguageService ls = new XMLLanguageService();

		TextDocument document = new TextDocument(value, "test://test/test.html");
		Position position = document.positionAt(offset);
		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, ls.getResolverExtensionManager());

		AutoCloseTagResponse actual = ls.doTagComplete(htmlDoc, settings.getCompletionSettings(), position);
		if (expected == null) {
			assertNull(actual);
			return;
		}
		assertNotNull(actual);
		assertEquals(expected.snippet, actual.snippet);
		assertEquals(expected.range, actual.range);
	}

	// ------------------- Completion Item resolve assert

	public static void testCompletionItemUnresolvedFor(String value, String catalogPath, String fileURI,
			Integer expectedCount, CompletionItem... expectedItems) throws BadLocationException {
		CompletionItemResolveSupportCapabilities completionItemResolveSupportCapabilities = new CompletionItemResolveSupportCapabilities();
		completionItemResolveSupportCapabilities
				.setProperties(Arrays.asList(CompletionResolveSupportProperty.documentation.name(),
						CompletionResolveSupportProperty.additionalTextEdits.name()));
		CompletionItemCapabilities completionItemCapabilities = new CompletionItemCapabilities();
		completionItemCapabilities.setResolveSupport(completionItemResolveSupportCapabilities);
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		completionCapabilities.setCompletionItem(completionItemCapabilities);
		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);

		testCompletionItemUnresolvedFor(new XMLLanguageService(), value, catalogPath, (_a) -> {
		}, fileURI, expectedCount, sharedSettings, expectedItems);
	}

	public static void testCompletionItemUnresolvedFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath,
			Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			SharedSettings sharedSettings, CompletionItem... expectedItems) throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);
		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> htmlDoc);

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		CompletionList list = xmlLanguageService.doComplete(htmlDoc, position, sharedSettings);

		// no duplicate labels
		List<String> labels = list.getItems().stream().map(i -> i.getLabel()).sorted().collect(Collectors.toList());
		String previous = null;
		for (String label : labels) {
			if (expectedCount != null) {
				continue;
			}
			assertNotEquals(previous, label, () -> {
				return "Duplicate label " + label + " in " + labels.stream().collect(Collectors.joining(",")) + "}";
			});
			previous = label;
		}
		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), list.getItems().size());
		}

		if (expectedItems != null) {
			for (CompletionItem item : expectedItems) {
				assertUnresolvedCompletion(list, item, expectedCount);
			}
		}
	}

	public static void testCompletionItemResolveFor(String value, String catalogPath, String fileURI,
			Integer expectedCount, CompletionItem... expectedItems) throws BadLocationException {
		CompletionItemResolveSupportCapabilities completionItemResolveSupportCapabilities = new CompletionItemResolveSupportCapabilities();
		completionItemResolveSupportCapabilities
				.setProperties(Arrays.asList("documentation"));
		CompletionItemCapabilities completionItemCapabilities = new CompletionItemCapabilities();
		completionItemCapabilities.setResolveSupport(completionItemResolveSupportCapabilities);
		CompletionCapabilities completionCapabilities = new CompletionCapabilities();
		completionCapabilities.setCompletionItem(completionItemCapabilities);
		SharedSettings sharedSettings = new SharedSettings();
		sharedSettings.getCompletionSettings().setCapabilities(completionCapabilities);

		testCompletionItemResolveFor(new XMLLanguageService(), value, catalogPath, (_a) -> {
		}, fileURI, expectedCount, sharedSettings, expectedItems);
	}

	public static void testCompletionItemResolveFor(XMLLanguageService xmlLanguageService, String value,
			String catalogPath,
			Consumer<XMLLanguageService> customConfiguration, String fileURI, Integer expectedCount,
			SharedSettings sharedSettings, CompletionItem... expectedItems) throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);
		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> htmlDoc);

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		CompletionList list = xmlLanguageService.doComplete(htmlDoc, position, sharedSettings);

		// no duplicate labels
		List<String> labels = list.getItems().stream().map(i -> i.getLabel()).sorted().collect(Collectors.toList());
		String previous = null;
		for (String label : labels) {
			if (expectedCount != null) {
				continue;
			}
			assertNotEquals(previous, label, () -> {
				return "Duplicate label " + label + " in " + labels.stream().collect(Collectors.joining(",")) + "}";
			});
			previous = label;
		}
		if (expectedCount != null) {
			assertEquals(expectedCount.intValue(), list.getItems().size());
		}

		CompletionList resolved = new CompletionList(
				list.getItems().stream() //
						.map((item) -> {
							return xmlLanguageService.resolveCompletionItem(item, htmlDoc,
									sharedSettings,
									() -> {
									});
						}) //
						.collect(Collectors.toList()));

		if (expectedItems != null) {
			for (CompletionItem item : expectedItems) {
				assertCompletion(resolved, item, expectedCount);
			}
		}
	}

	public static void assertUnresolvedCompletion(CompletionList completions, CompletionItem expected,
			Integer expectedCount) {
		List<CompletionItem> matches = completions.getItems().stream().filter(completion -> {
			return expected.getLabel().equals(completion.getLabel());
		}).collect(Collectors.toList());

		if (expectedCount != null) {
			assertTrue(matches.size() >= 1, () -> {
				return expected.getLabel() + " should only exist once: Actual: "
						+ completions.getItems().stream().map(c -> c.getLabel()).collect(Collectors.joining(","));
			});
		} else {
			assertEquals(1, matches.size(), () -> {
				return expected.getLabel() + " should only exist once: Actual: "
						+ completions.getItems().stream().map(c -> c.getLabel()).collect(Collectors.joining(","));
			});
		}

		CompletionItem match = getCompletionMatch(matches, expected);
		if (expected.getTextEdit() != null && match.getTextEdit() != null) {
			if (expected.getTextEdit().getLeft().getNewText() != null) {
				assertEquals(expected.getTextEdit().getLeft().getNewText(), match.getTextEdit().getLeft().getNewText());
			}
			Range r = expected.getTextEdit().getLeft().getRange();
			if (r != null && r.getStart() != null && r.getEnd() != null) {
				assertEquals(expected.getTextEdit().getLeft().getRange(), match.getTextEdit().getLeft().getRange());
			}

			if (expected.getAdditionalTextEdits() != null && match.getAdditionalTextEdits() != null) {
				assertEquals(expected.getAdditionalTextEdits().size(), match.getAdditionalTextEdits().size());
				assertArrayEquals(expected.getAdditionalTextEdits().toArray(),
						match.getAdditionalTextEdits().toArray());
			}
		}
		if (expected.getFilterText() != null && match.getFilterText() != null) {
			assertEquals(expected.getFilterText(), match.getFilterText());
		}

		if (expected.getDocumentation() != null) {
			assertNull(match.getDocumentation());
		}
	}

	// ------------------- Diagnostics assert

	public static void testDiagnosticsFor(String xml, Diagnostic... expected) {
		testDiagnosticsFor(xml, null, expected);
	}

	public static void testDiagnosticsFor(String xml, String catalogPath, Diagnostic... expected) {
		testDiagnosticsFor(xml, catalogPath, null, null, expected);
	}

	public static void testDiagnosticsFor(String xml, String catalogPath, Consumer<XMLLanguageService> configuration,
			String fileURI, Diagnostic... expected) {
		testDiagnosticsFor(xml, catalogPath, configuration, fileURI, true, expected);
	}

	public static void testDiagnosticsFor(String xml, String catalogPath, Consumer<XMLLanguageService> configuration,
			String fileURI, boolean filter, Diagnostic... expected) {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		XMLValidationRootSettings problems = new XMLValidationRootSettings();
		problems.setNoGrammar("ignore");
		settings.setValidation(problems);
		if (catalogPath != null) {
			// Configure XML catalog for XML schema
			settings.setCatalogs(new String[] { catalogPath });
		}
		testDiagnosticsFor(xml, catalogPath, configuration, fileURI, filter, settings, expected);
	}

	public static void testDiagnosticsFor(String xml, String catalogPath, Consumer<XMLLanguageService> configuration,
			String fileURI, boolean filter, ContentModelSettings settings, Diagnostic... expected) {
		testDiagnosticsFor(new XMLLanguageService(), xml, catalogPath, configuration, fileURI, filter, settings,
				expected);
	}

	public static void testDiagnosticsFor(XMLLanguageService xmlLanguageService, String xml, String catalogPath,
			Consumer<XMLLanguageService> configuration, String fileURI, boolean filter, ContentModelSettings settings,
			Diagnostic... expected) {
		TextDocument document = new TextDocument(xml, fileURI != null ? fileURI : "test.xml");

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		xmlLanguageService.doSave(new SettingsSaveContext(settings));
		if (configuration != null) {
			xmlLanguageService.initializeIfNeeded();
			configuration.accept(xmlLanguageService);
		}

		List<Diagnostic> actual = xmlLanguageService.doDiagnostics(xmlDocument, settings.getValidation(),
				Collections.emptyMap(), () -> {
				});
		if (expected == null) {
			assertTrue(actual.isEmpty());
			return;
		}
		assertDiagnostics(actual, Arrays.asList(expected), filter);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, Diagnostic... expected) {
		assertDiagnostics(actual, Arrays.asList(expected), true);
	}

	public static void assertDiagnostics(List<Diagnostic> actual, List<Diagnostic> expected, boolean filter) {
		List<Diagnostic> received = actual;
		final boolean filterMessage;
		if (expected != null && !expected.isEmpty() && !StringUtils.isEmpty(expected.get(0).getMessage())) {
			filterMessage = true;
		} else {
			filterMessage = false;
		}
		if (filter) {
			received = actual.stream().map(d -> {
				Diagnostic simpler = new Diagnostic(d.getRange(), "");
				if (d.getCode() != null && !StringUtils.isEmpty(d.getCode().getLeft())) {
					simpler.setCode(d.getCode());
				}
				if (filterMessage) {
					simpler.setMessage(d.getMessage());
				}
				return simpler;
			}).collect(Collectors.toList());
		}
		// Don't compare message of diagnosticRelatedInformation
		for (Diagnostic diagnostic : received) {
			List<DiagnosticRelatedInformation> diagnosticRelatedInformations = diagnostic.getRelatedInformation();
			if (diagnosticRelatedInformations != null) {
				for (DiagnosticRelatedInformation diagnosticRelatedInformation : diagnosticRelatedInformations) {
					diagnosticRelatedInformation.setMessage("");
				}
			}
		}
		assertIterableEquals(expected, received, "Unexpected diagnostics:\n" + actual);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, IXMLErrorCode code) {
		return d(startLine, startCharacter, endLine, endCharacter, code, "");
	}

	public static Diagnostic d(int startLine, int startCharacter, int endCharacter, IXMLErrorCode code) {
		// Diagnostic on 1 line
		return d(startLine, startCharacter, startLine, endCharacter, code);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, IXMLErrorCode code,
			String message) {
		return d(startLine, startCharacter, endLine, endCharacter, code, message, null, null);
	}

	public static Diagnostic d(int startLine, int startCharacter, int endLine, int endCharacter, IXMLErrorCode code,
			String message, String source, DiagnosticSeverity severity) {
		// Diagnostic on 1 line
		return new Diagnostic(r(startLine, startCharacter, endLine, endCharacter), message, severity, source,
				code != null ? code.getCode() : null);
	}

	public static Range r(int line, int startCharacter, int endCharacter) {
		return r(line, startCharacter, line, endCharacter);
	}

	public static Range r(int startLine, int startCharacter, int endLine, int endCharacter) {
		return new Range(new Position(startLine, startCharacter), new Position(endLine, endCharacter));
	}

	public static ContentModelSettings getContentModelSettings(boolean isEnabled, SchemaEnabled schemaEnabled) {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		XMLValidationRootSettings problems = new XMLValidationRootSettings();
		problems.setNoGrammar("ignore");
		settings.setValidation(problems);
		XMLValidationRootSettings diagnostics = new XMLValidationRootSettings();
		diagnostics.setEnabled(isEnabled);
		XMLSchemaSettings schemaSettings = new XMLSchemaSettings();
		schemaSettings.setEnabled(schemaEnabled);
		diagnostics.setSchema(schemaSettings);
		settings.setValidation(diagnostics);
		return settings;
	}

	// ------------------- Publish Diagnostics assert

	public static void testPublishDiagnosticsFor(String xml, String fileURI, Consumer<XMLLanguageService> configuration,
			PublishDiagnosticsParams... expected) {
		testPublishDiagnosticsFor(xml, fileURI, null, configuration, expected);
	}

	public static void testPublishDiagnosticsFor(String xml, String fileURI,
			XMLValidationRootSettings validationSettings,
			PublishDiagnosticsParams... expected) {
		testPublishDiagnosticsFor(xml, fileURI, validationSettings, (Consumer<XMLLanguageService>) null, expected);
	}

	public static void testPublishDiagnosticsFor(String xml, String fileURI,
			XMLValidationRootSettings validationSettings,
			Consumer<XMLLanguageService> configuration, PublishDiagnosticsParams... expected) {
		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		if (configuration != null) {
			xmlLanguageService.initializeIfNeeded();
			configuration.accept(xmlLanguageService);
		}
		testPublishDiagnosticsFor(xml, fileURI, validationSettings, xmlLanguageService, expected);
	}

	public static void testPublishDiagnosticsFor(String xml, String fileURI, XMLLanguageService xmlLanguageService,
			PublishDiagnosticsParams... expected) {
		testPublishDiagnosticsFor(xml, fileURI, null, xmlLanguageService, expected);
	}

	public static void testPublishDiagnosticsFor(long timeout, String xml, String fileURI,
			XMLValidationRootSettings validationSettings, XMLLanguageService xmlLanguageService,
			PublishDiagnosticsParams... expected) {
		long deadline = System.currentTimeMillis() + timeout;
		while (true) {
			try {
				testPublishDiagnosticsFor(xml, fileURI, validationSettings, xmlLanguageService, expected);
				return;
			} catch (AssertionError e) {
				if (System.currentTimeMillis() < deadline) {
					Thread.yield();
					continue;
				}
				throw e;
			}
		}
	}

	public static void testPublishDiagnosticsFor(String xml, String fileURI,
			XMLValidationRootSettings validationSettings,
			XMLLanguageService xmlLanguageService, PublishDiagnosticsParams... expected) {
		List<PublishDiagnosticsParams> actual = new ArrayList<>();

		DOMDocument xmlDocument = DOMParser.getInstance().parse(xml, fileURI,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		publishDiagnostics(xmlDocument, validationSettings, actual, xmlLanguageService);

		assertPublishDiagnostics(actual, expected);
	}

	public static void assertPublishDiagnostics(List<PublishDiagnosticsParams> actual,
			PublishDiagnosticsParams... expected) {
		assertEquals(expected.length, actual.size(), () -> {
			return "Unexpected diagnostics. Expected:"+ getMessages(Arrays.stream(expected)) + ",\nReceived: " +getMessages(actual.stream());
		});
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getUri(), actual.get(i).getUri());
			actual.get(i).getDiagnostics().forEach(d -> {
				d.setMessage(cleanExceptionMessage.apply(d.getMessage()));
			});
			expected[i].getDiagnostics().forEach(d -> {
				d.setMessage(cleanExceptionMessage.apply(d.getMessage()));
			});
			assertDiagnostics(actual.get(i).getDiagnostics(), expected[i].getDiagnostics(), false);
		}
	}

	private static List<String> getMessages(Stream<PublishDiagnosticsParams> diagParams) {
		return diagParams.flatMap(d -> d.getDiagnostics().stream())
						.map(d -> cleanExceptionMessage.apply(d.getMessage()))
						.collect(Collectors.toList());
	}

	private static final Function<String, String> cleanExceptionMessage = (message) -> {
		if (message.contains("[")) {
			String exceptionClassName = message.substring(message.indexOf("[") + 1, message.indexOf("]"));
			message = message.split("\\s:\\s")[0] + " : " + exceptionClassName;
		}
		return message;
	};

	public static void publishDiagnostics(DOMDocument xmlDocument, List<PublishDiagnosticsParams> actual,
			XMLLanguageService languageService) {
		publishDiagnostics(xmlDocument, null, actual, languageService);
	}

	public static void publishDiagnostics(DOMDocument xmlDocument, XMLValidationRootSettings validationSettings,
			List<PublishDiagnosticsParams> actual, XMLLanguageService languageService) {
		CompletableFuture<Path> error = languageService.publishDiagnostics(xmlDocument, params -> {
			actual.add(params);
		}, (doc) -> {
			// Retrigger validation
			publishDiagnostics(xmlDocument, actual, languageService);
		}, validationSettings, Collections.emptyMap(), () -> {
		});

		if (error != null) {
			try {
				error.join();
				// Wait for 500 ms to collect the last params
				Thread.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static PublishDiagnosticsParams pd(String uri, Diagnostic... diagnostics) {
		return new PublishDiagnosticsParams(uri, Arrays.asList(diagnostics));
	}

	// ------------------- CodeAction assert

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, CodeAction... expected)
			throws BadLocationException {
		return testCodeActionsFor(xml, diagnostic, (String) null, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, int index,
			CodeAction... expected)
			throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setInsertSpaces(false);
		return testCodeActionsFor(xml, diagnostic, null, null, null, settings, null, index, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, String fileURI, Diagnostic diagnostic,
			CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(xml, fileURI, diagnostic, (String) null, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, SharedSettings settings,
			CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(xml, diagnostic, null, settings, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, String catalogPath,
			CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(xml, null, diagnostic, catalogPath, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, String fileURI, Diagnostic diagnostic,
			String catalogPath, CodeAction... expected) throws BadLocationException {
		SharedSettings settings = new SharedSettings();
		settings.getFormattingSettings().setTabSize(4);
		settings.getFormattingSettings().setInsertSpaces(false);
		return testCodeActionsFor(xml, diagnostic, null, catalogPath, fileURI, settings, null, -1, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, String catalogPath,
			SharedSettings sharedSettings, CodeAction... expected) throws BadLocationException {
		return testCodeActionsFor(xml, diagnostic, catalogPath, sharedSettings, null, expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, String catalogPath,
			SharedSettings sharedSettings, XMLLanguageService xmlLanguageService, CodeAction... expected)
			throws BadLocationException {
		return testCodeActionsFor(xml, diagnostic, null, catalogPath, null, sharedSettings, xmlLanguageService, -1,
				expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Range range, String catalogPath,
			SharedSettings sharedSettings, XMLLanguageService xmlLanguageService, CodeAction... expected)
			throws BadLocationException {
		return testCodeActionsFor(xml, null, range, catalogPath, null, sharedSettings, xmlLanguageService, -1,
				expected);
	}

	public static List<CodeAction> testCodeActionsFor(String xml, Diagnostic diagnostic, Range range,
			String catalogPath,
			String fileURI, SharedSettings sharedSettings, XMLLanguageService xmlLanguageService, int index,
			CodeAction... expected) throws BadLocationException {
		int offset = xml.indexOf('|');
		if (offset != -1) {
			xml = xml.substring(0, offset) + xml.substring(offset + 1);
		}
		TextDocument document = new TextDocument(xml.toString(), fileURI != null ? fileURI : FILE_URI);

		// Use range from the text (if marked by "|"-char or from diagnostics
		if (offset != -1) {
			Position position = document.positionAt(offset);
			range = new Range(position, position);
		} else if (range == null && diagnostic != null) {
			range = diagnostic.getRange();
		}

		// Otherwise, range is to be specified in parameters
		assertNotNull(range, "Range cannot be null");

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		if (catalogPath != null) {
			// Configure XML catalog for XML schema
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));

		CodeActionContext context = new CodeActionContext();
		context.setDiagnostics(Arrays.asList(diagnostic));
		DOMDocument xmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDoc);

		List<CodeAction> actual = xmlLanguageService.doCodeActions(context, range, xmlDoc, sharedSettings, () -> {
		});

		// Clone
		// Creating a gson object
		Gson gson = new Gson();
		// Converting the list into a json string
		String jsonstring = gson.toJson(actual);

		// Converting the json string
		// back into a list
		CodeAction[] cloned_list = gson.fromJson(jsonstring, CodeAction[].class);

		// Only test the code action at index if a proper index is given
		if (index >= 0) {
			assertCodeActions(Arrays.asList(actual.get(index)), Arrays.asList(expected).get(index));
			return Arrays.asList(cloned_list);
		}
		assertCodeActions(actual, expected);
		return Arrays.asList(cloned_list);
	}

	public static void assertCodeActions(List<CodeAction> actual, CodeAction... expected) {
		actual.stream().forEach(ca -> {
			// we don't want to compare title, etc
			ca.setKind(null);
			ca.setTitle("");
			if (ca.getDiagnostics() != null) {
				ca.getDiagnostics().forEach(d -> {
					if (d != null) {
						d.setSeverity(null);
						d.setMessage("");
						d.setSource(null);
					}
				});
			}
		});

		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toArray());
	}

	public static CodeAction ca(Diagnostic d, TextEdit... te) {
		return ca(d, FILE_URI, te);
	}

	public static CodeAction ca(Diagnostic d, String fileUri, TextEdit... te) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle("");
		codeAction.setDiagnostics(Arrays.asList(d));

		TextDocumentEdit textDocumentEdit = tde(fileUri, 0, te);
		WorkspaceEdit workspaceEdit = new WorkspaceEdit(Collections.singletonList(Either.forLeft(textDocumentEdit)));
		codeAction.setEdit(workspaceEdit);
		return codeAction;
	}

	/**
	 * Mock code action for creating a command code action
	 */
	public static CodeAction ca(Diagnostic d) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle("");
		codeAction.setDiagnostics(Arrays.asList(d));
		return codeAction;
	}

	public static CodeAction ca(Diagnostic d, Command c) {
		CodeAction codeAction = new CodeAction();
		codeAction.setTitle("");
		codeAction.setDiagnostics(Arrays.asList(d));

		codeAction.setCommand(c);

		return codeAction;
	}

	public static CodeAction ca(Diagnostic d, JsonObject data) {
		return internalCa(d, data);
	}

	@SafeVarargs
	public static CodeAction ca(Diagnostic d, Either<TextDocumentEdit, ResourceOperation>... ops) {
		return internalCa(d, null, ops);
	}

	@SafeVarargs
	public static CodeAction ca(Diagnostic d, JsonObject data, Either<TextDocumentEdit, ResourceOperation>... ops) {
		return internalCa(d, data, ops);
	}

	@SafeVarargs
	private static CodeAction internalCa(Diagnostic d, JsonObject data,
			Either<TextDocumentEdit, ResourceOperation>... ops) {
		CodeAction codeAction = new CodeAction();
		codeAction.setDiagnostics(Collections.singletonList(d));
		if (ops != null && ops.length > 0) {
			codeAction.setEdit(new WorkspaceEdit(Arrays.asList(ops)));
		}
		codeAction.setTitle("");
		codeAction.setData(data);
		return codeAction;
	}

	// ------------------- Resolve CodeAction assert

	public static void testResolveCodeActionsFor(String xml, CodeAction unresolved, SharedSettings sharedSettings,
			CodeAction expected) throws BadLocationException {
		testResolveCodeActionsFor(xml, unresolved, null, null, sharedSettings, null, expected);
	}

	public static void testResolveCodeActionsFor(String xml, CodeAction unresolved, SharedSettings sharedSettings,
			XMLLanguageService xmlLanguageService, CodeAction expected) throws BadLocationException {
		testResolveCodeActionsFor(xml, unresolved, null, null, sharedSettings, xmlLanguageService, expected);
	}

	public static void testResolveCodeActionsFor(String xml, CodeAction unresolved, String catalogPath, String fileURI,
			SharedSettings sharedSettings, XMLLanguageService xmlLanguageService, CodeAction expected)
			throws BadLocationException {
		TextDocument document = new TextDocument(xml.toString(), fileURI != null ? fileURI : FILE_URI);

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		if (catalogPath != null) {
			// Configure XML catalog for XML schema
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));

		DOMDocument xmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());

		CodeAction actual = xmlLanguageService.resolveCodeAction(unresolved, xmlDoc, sharedSettings, () -> {
		});
		if (expected == null) {
			assertNull(actual);
		} else {
			assertCodeActions(Arrays.asList(actual), expected);
		}
	}

	public static TextDocumentEdit tde(String uri, int version, TextEdit... te) {
		VersionedTextDocumentIdentifier versionedTextDocumentIdentifier = new VersionedTextDocumentIdentifier(uri,
				version);
		return new TextDocumentEdit(versionedTextDocumentIdentifier, Arrays.asList(te));
	}

	public static TextEdit te(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
		TextEdit textEdit = new TextEdit();
		textEdit.setNewText(newText);
		textEdit.setRange(r(startLine, startCharacter, endLine, endCharacter));
		return textEdit;
	}

	public static Either<TextDocumentEdit, ResourceOperation> createFile(String uri, boolean overwrite) {
		CreateFileOptions options = new CreateFileOptions();
		options.setIgnoreIfExists(!overwrite);
		options.setOverwrite(overwrite);
		return Either.forRight(new CreateFile(uri, options));
	}

	public static Either<TextDocumentEdit, ResourceOperation> teOp(String uri, int startLine, int startChar,
			int endLine, int endChar, String newText) {
		return Either.forLeft(new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri, 0),
				Collections.singletonList(te(startLine, startChar, endLine, endChar, newText))));
	}

	public static Either<TextDocumentEdit, ResourceOperation> teOp(String uri, TextEdit... te) {
		return Either.forLeft(new TextDocumentEdit(new VersionedTextDocumentIdentifier(uri, 0), Arrays.asList(te)));
	}

	// ------------------- Hover assert

	public static void assertHover(String value) throws BadLocationException {
		assertHover(value, null, null);
	}

	public static void assertHover(String value, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		assertHover(value, null, expectedHoverLabel, expectedHoverRange);
	}

	public static void assertHover(String value, String fileURI, String expectedHoverLabel, Range expectedHoverRange)
			throws BadLocationException {
		assertHover(new XMLLanguageService(), value, null, fileURI, expectedHoverLabel, expectedHoverRange);
	}

	public static void assertHover(XMLLanguageService xmlLanguageService, String value, String catalogPath,
			String fileURI, String expectedHoverLabel, Range expectedHoverRange) throws BadLocationException {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		assertHover(xmlLanguageService, value, catalogPath, fileURI, expectedHoverLabel, expectedHoverRange, settings);
	}

	public static void assertHover(XMLLanguageService xmlLanguageService, String value, String catalogPath,
			String fileURI, String expectedHoverLabel, Range expectedHoverRange, SharedSettings sharedSettings)
			throws BadLocationException {
		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		assertHover(xmlLanguageService, value, catalogPath, fileURI, expectedHoverLabel, expectedHoverRange, settings,
				sharedSettings);
	}

	public static void assertHover(XMLLanguageService xmlLanguageService, String value, String catalogPath,
			String fileURI, String expectedHoverLabel, Range expectedHoverRange, ContentModelSettings settings)
			throws BadLocationException {
		SharedSettings sharedSettings = new SharedSettings();
		HoverCapabilities capabilities = new HoverCapabilities(Arrays.asList(MarkupKind.MARKDOWN), false);
		sharedSettings.getHoverSettings().setCapabilities(capabilities);
		assertHover(xmlLanguageService, value, catalogPath, fileURI, expectedHoverLabel, expectedHoverRange, settings,
				sharedSettings);
	}

	public static void assertHover(XMLLanguageService xmlLanguageService, String value, String catalogPath,
			String fileURI, String expectedHoverLabel, Range expectedHoverRange, ContentModelSettings settings,
			SharedSettings sharedSettings) throws BadLocationException {
		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.html");

		Position position = document.positionAt(offset);

		DOMDocument htmlDoc = DOMParser.getInstance().parse(document, xmlLanguageService.getResolverExtensionManager());
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			settings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(settings));

		Hover hover = xmlLanguageService.doHover(htmlDoc, position, sharedSettings);
		if (expectedHoverLabel == null) {
			assertNull(hover);
		} else {
			String actualHoverLabel = getHoverLabel(hover);
			assertEquals(expectedHoverLabel, actualHoverLabel);
			if (expectedHoverRange != null) {
				assertEquals(expectedHoverRange, hover.getRange());
			}
		}
	}

	private static String getHoverLabel(Hover hover) {
		Either<List<Either<String, MarkedString>>, MarkupContent> contents = hover != null ? hover.getContents() : null;
		if (contents == null) {
			return null;
		}
		return contents.getRight().getValue();
	}

	// ------------------- Links assert

	public static void testDocumentLinkFor(String xml, String fileURI, DocumentLink... expected) {
		testDocumentLinkFor(xml, fileURI, null, expected);
	}

	public static void testDocumentLinkFor(String xml, String fileURI, String catalogPath, DocumentLink... expected) {
		testDocumentLinkFor(null, xml, fileURI, catalogPath, expected);
	}

	public static void testDocumentLinkFor(XMLLanguageService xmlLanguageService, String xml, String fileURI,
			String catalogPath, DocumentLink... expected) {
		TextDocument document = new TextDocument(xml, fileURI != null ? fileURI : "test.xml");

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			settings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(settings));

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<DocumentLink> actual = xmlLanguageService.findDocumentLinks(xmlDocument);
		assertDocumentLinks(actual, expected);

	}

	public static DocumentLink dl(Range range, String target) {
		return new DocumentLink(range, target);
	}

	public static void assertDocumentLinks(List<DocumentLink> actual, DocumentLink... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange(), " Range test '" + i + "' link");
			assertEquals(Paths.get(expected[i].getTarget()).toUri().toString().replace("file:///", "file:/"),
					actual.get(i).getTarget().replace("file:///", "file:/"), " Target test '" + i + "' link");
		}
	}

	// ------------------- DocumentSymbol assert

	public static void testDocumentSymbolsFor(String xml, DocumentSymbol... expected) {
		testDocumentSymbolsFor(xml, null, new XMLSymbolSettings(), expected);
	}

	public static void testDocumentSymbolsFor(String xml, XMLSymbolSettings symbolSettings,
			DocumentSymbol... expected) {
		testDocumentSymbolsFor(xml, null, symbolSettings, expected);
	}

	public static void testDocumentSymbolsFor(String xml, String fileURI, DocumentSymbol... expected) {
		testDocumentSymbolsFor(xml, fileURI, new XMLSymbolSettings(), expected);
	}

	public static void testDocumentSymbolsFor(String xml, String fileURI, XMLSymbolSettings symbolSettings,
			DocumentSymbol... expected) {
		testDocumentSymbolsFor(xml, fileURI, symbolSettings, null, expected);
	}

	public static void testDocumentSymbolsFor(String xml, String fileURI, XMLSymbolSettings symbolSettings,
			Consumer<XMLLanguageService> customConfiguration, DocumentSymbol... expected) {
		testDocumentSymbolsFor(new XMLLanguageService(), xml, fileURI, symbolSettings, customConfiguration, expected);
	}

	public static void testDocumentSymbolsFor(XMLLanguageService xmlLanguageService, String xml, String fileURI,
			XMLSymbolSettings symbolSettings, Consumer<XMLLanguageService> customConfiguration,
			DocumentSymbol... expected) {
		TextDocument document = new TextDocument(xml, fileURI != null ? fileURI : "test.xml");

		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		xmlLanguageService.doSave(new SettingsSaveContext(settings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<DocumentSymbol> actual = xmlLanguageService.findDocumentSymbols(xmlDocument, symbolSettings);
		assertDocumentSymbols(actual, expected);

	}

	public static DocumentSymbol ds(final String name, final SymbolKind kind, final Range range,
			final Range selectionRange, final String detail, final List<DocumentSymbol> children) {
		return new DocumentSymbol(name, kind, range, selectionRange, detail, children);
	}

	public static void assertDocumentSymbols(List<DocumentSymbol> actual, DocumentSymbol... expected) {
		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- SymbolInformation assert

	public static void testSymbolInformationsFor(String xml, SymbolInformation... expected) {
		testSymbolInformationsFor(xml, null, new XMLSymbolSettings(), expected);
	}

	public static void testSymbolInformationsFor(String xml, XMLSymbolSettings symbolSettings,
			SymbolInformation... expected) {
		testSymbolInformationsFor(xml, null, symbolSettings, expected);
	}

	public static void testSymbolInformationsFor(String xml, String fileURI, SymbolInformation... expected) {
		testSymbolInformationsFor(xml, fileURI, new XMLSymbolSettings(), expected);
	}

	public static void testSymbolInformationsFor(String xml, String fileURI, XMLSymbolSettings symbolSettings,
			SymbolInformation... expected) {
		testSymbolInformationsFor(xml, fileURI, symbolSettings, null, expected);
	}

	public static void testSymbolInformationsFor(String xml, String fileURI, XMLSymbolSettings symbolSettings,
			Consumer<XMLLanguageService> customConfiguration, SymbolInformation... expected) {
		testSymbolInformationsFor(new XMLLanguageService(), xml, fileURI, symbolSettings, customConfiguration,
				expected);
	}

	public static void testSymbolInformationsFor(XMLLanguageService xmlLanguageService, String xml, String fileURI,
			XMLSymbolSettings symbolSettings, Consumer<XMLLanguageService> customConfiguration,
			SymbolInformation... expected) {
		TextDocument document = new TextDocument(xml, fileURI != null ? fileURI : "test.xml");

		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		xmlLanguageService.doSave(new SettingsSaveContext(settings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<SymbolInformation> actual = xmlLanguageService.findSymbolInformations(xmlDocument, symbolSettings);
		assertSymbolInformations(actual, expected);

	}

	public static SymbolInformation si(String name, SymbolKind kind, Location location, String containerName) {
		return new SymbolInformation(name, kind, location, containerName);
	}

	public static void assertSymbolInformations(List<SymbolInformation> actual, SymbolInformation... expected) {
		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- Definition assert

	public static void testDefinitionFor(String xml, LocationLink... expected) throws BadLocationException {
		testDefinitionFor(xml, null, expected);
	}

	public static void testDefinitionFor(String value, String fileURI, LocationLink... expected)
			throws BadLocationException {
		testDefinitionFor(null, value, fileURI, expected);
	}

	public static void testDefinitionFor(XMLLanguageService xmlLanguageService, String value, String fileURI,
			LocationLink... expected) throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<? extends LocationLink> actual = xmlLanguageService.findDefinition(xmlDocument, position, () -> {
		});
		assertLocationLink(actual, expected);

	}

	public static LocationLink ll(final String uri, final Range originRange, Range targetRange) {
		return new LocationLink(uri, targetRange, targetRange, originRange);
	}

	public static void assertLocationLink(List<? extends LocationLink> actual, LocationLink... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			actual.get(i).setTargetUri(actual.get(i).getTargetUri().replace("file:///", "file:/"));
			expected[i].setTargetUri(expected[i].getTargetUri().replace("file:///", "file:/"));
		}
		assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- Type Definition assert

	public static void testTypeDefinitionFor(XMLLanguageService xmlLanguageService, String value, String fileURI,
			LocationLink... expected) throws BadLocationException {
		testTypeDefinitionFor(xmlLanguageService, null, value, fileURI, expected);
	}

	public static void testTypeDefinitionFor(XMLLanguageService xmlLanguageService, String catalogPath, String value,
			String fileURI, LocationLink... expected) throws BadLocationException {
		testTypeDefinitionFor(xmlLanguageService, catalogPath, null, value, fileURI, expected);
	}

	public static void testTypeDefinitionFor(XMLLanguageService xmlLanguageService, String catalogPath,
			Consumer<XMLLanguageService> customConfiguration, String value, String fileURI, LocationLink... expected)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);

		ContentModelSettings cmSettings = new ContentModelSettings();
		cmSettings.setUseCache(false);
		// Configure XML catalog for XML schema
		if (catalogPath != null) {
			cmSettings.setCatalogs(new String[] { catalogPath });
		}
		xmlLanguageService.doSave(new SettingsSaveContext(cmSettings));
		xmlLanguageService.initializeIfNeeded();

		if (customConfiguration != null) {
			customConfiguration.accept(xmlLanguageService);
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<? extends LocationLink> actual = xmlLanguageService.findTypeDefinition(xmlDocument, position, () -> {
		});
		assertLocationLink(actual, expected);

	}

	// ------------------- Reference assert

	public static void testReferencesFor(String xml, Location... expected) throws BadLocationException {
		testReferencesFor(xml, null, expected);
	}

	public static void testReferencesFor(String value, String fileURI, Location... expected)
			throws BadLocationException {
		testReferencesFor(null, value, fileURI, expected);
	}

	public static void testReferencesFor(XMLLanguageService xmlLanguageService, String value, String fileURI,
			Location... expected) throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<? extends Location> actual = xmlLanguageService.findReferences(xmlDocument, position,
				new ReferenceContext(), () -> {
				});
		assertLocation(actual, expected);

	}

	public static Location l(final String uri, final Range range) {
		return new Location(uri, range);
	}

	public static void assertLocation(List<? extends Location> actual, Location... expected) {
		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toArray());
	}

	// ------------------- CodeLens assert

	public static void testCodeLensFor(String xml, CodeLens... expected) throws BadLocationException {
		testCodeLensFor(xml, null, expected);
	}

	public static void testCodeLensFor(String value, String fileURI, CodeLens... expected) throws BadLocationException {
		testCodeLensFor(value, fileURI, new XMLLanguageService(), expected);
	}

	public static void testCodeLensFor(String value, String fileURI, XMLLanguageService xmlLanguageService,
			CodeLens... expected) {
		testCodeLensFor(value, fileURI, xmlLanguageService,
				Arrays.asList(CodeLensKind.References, CodeLensKind.Association, CodeLensKind.OpenUri), expected);
	}

	public static void testCodeLensFor(String value, String fileURI, List<String> codelensKinds, CodeLens... expected) {
		testCodeLensFor(value, fileURI, new XMLLanguageService(), codelensKinds, expected);
	}

	public static void testCodeLensFor(String value, String fileURI, XMLLanguageService xmlLanguageService,
			List<String> codelensKinds, CodeLens... expected) {
		testCodeLensFor(value, fileURI, xmlLanguageService, codelensKinds, null, expected);
	}

	public static void testCodeLensFor(String value, String fileURI, XMLLanguageService xmlLanguageService,
			List<String> codelensKinds, Consumer<XMLLanguageService> customConfiguration, CodeLens... expected) {
		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		ContentModelSettings settings = new ContentModelSettings();
		settings.setUseCache(false);
		xmlLanguageService.doSave(new SettingsSaveContext(settings));

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		if (customConfiguration != null) {
			xmlLanguageService.initializeIfNeeded();
			customConfiguration.accept(xmlLanguageService);
		}

		XMLCodeLensSettings codeLensSettings = new XMLCodeLensSettings();
		ExtendedCodeLensCapabilities codeLensCapabilities = new ExtendedCodeLensCapabilities(
				new CodeLensKindCapabilities(codelensKinds));
		codeLensSettings.setCodeLens(codeLensCapabilities);
		List<? extends CodeLens> actual = xmlLanguageService.getCodeLens(xmlDocument, codeLensSettings, () -> {
		});
		assertCodeLens(actual, expected);
	}

	public static CodeLens cl(Range range, String title, String command) {
		return new CodeLens(range, new Command(title, command), null);
	}

	public static void assertCodeLens(List<? extends CodeLens> actual, CodeLens... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			Command expectedCommand = expected[i].getCommand();
			Command actualCommand = actual.get(i).getCommand();
			if (expectedCommand != null && actualCommand != null) {
				assertEquals(expectedCommand.getTitle(), actualCommand.getTitle());
				assertEquals(expectedCommand.getCommand(), actualCommand.getCommand());
			}
			assertEquals(expected[i].getData(), actual.get(i).getData());
		}
	}

	// ------------------- Highlights assert

	public static void testHighlightsFor(String xml, DocumentHighlight... expected) throws BadLocationException {
		testHighlightsFor(xml, null, expected);
	}

	public static void testHighlightsFor(String value, String fileURI,
			DocumentHighlight... expected)
			throws BadLocationException {
		testHighlightsFor(new XMLLanguageService(), value, fileURI, expected);
	}

	public static void testHighlightsFor(XMLLanguageService xmlLanguageService, String value, String fileURI,
			DocumentHighlight... expected)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);

		if (xmlLanguageService == null) {
			xmlLanguageService = new XMLLanguageService();
		}

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<? extends DocumentHighlight> actual = xmlLanguageService.findDocumentHighlights(xmlDocument, position,
				() -> {
				});
		assertDocumentHighlight(actual, expected);
	}

	public static void assertDocumentHighlight(List<? extends DocumentHighlight> actual,
			DocumentHighlight... expected) {
		assertEquals(expected.length, actual.size());
		assertArrayEquals(expected, actual.toArray());
	}

	public static DocumentHighlight hl(Range range) {
		return hl(range, DocumentHighlightKind.Read);
	}

	public static DocumentHighlight hl(Range range, DocumentHighlightKind kind) {
		return new DocumentHighlight(range, kind);
	}

	public static void assertHighlights(String value, int[] expectedMatches, String elementName)
			throws BadLocationException {
		assertHighlights(null, value, expectedMatches, elementName);
	}

	public static void assertHighlights(XMLLanguageService languageService, String value, int[] expectedMatches,
			String elementName) throws BadLocationException {
		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);

		DOMDocument document = DOMParser.getInstance().parse(value, "test://test/test.html", null);

		Position position = document.positionAt(offset);

		if (languageService == null) {
			languageService = new XMLLanguageService();
		}
		List<DocumentHighlight> highlights = languageService.findDocumentHighlights(document, position);
		assertEquals(expectedMatches.length, highlights.size());
		for (int i = 0; i < highlights.size(); i++) {
			DocumentHighlight highlight = highlights.get(i);
			int actualStartOffset = document.offsetAt(highlight.getRange().getStart());
			assertEquals(expectedMatches[i], actualStartOffset);
			int actualEndOffset = document.offsetAt(highlight.getRange().getEnd());
			assertEquals(expectedMatches[i] + (elementName != null ? elementName.length() : 0), actualEndOffset);
			assertEquals(elementName, document.getText().substring(actualStartOffset, actualEndOffset).toLowerCase());
		}
	}

	// ------------------- Format assert

	public static void assertFormat(String unformatted, String actual) throws BadLocationException {
		assertFormat(unformatted, actual, new SharedSettings());
	}

	public static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings)
			throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, "test://test.html");
	}

	public static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri)
			throws BadLocationException {
		assertFormat(unformatted, expected, sharedSettings, uri, true);
	}

	public static void assertFormat(String unformatted, String expected, SharedSettings sharedSettings, String uri,
			Boolean considerRangeFormat) throws BadLocationException {
		assertFormat(null, unformatted, expected, sharedSettings, uri, considerRangeFormat);
	}

	public static void assertFormat(XMLLanguageService languageService, String unformatted, String expected,
			SharedSettings sharedSettings, String uri, Boolean considerRangeFormat) throws BadLocationException {
		assertFormat(languageService, unformatted, expected, sharedSettings, uri, considerRangeFormat,
				(TextEdit[]) null);
	}

	public static void assertFormat(XMLLanguageService languageService, String unformatted, String expected,
			SharedSettings sharedSettings, String uri, Boolean considerRangeFormat, TextEdit... expectedEdits)
			throws BadLocationException {
		Range range = null;
		int rangeStart = considerRangeFormat ? unformatted.indexOf('|') : -1;
		int rangeEnd = considerRangeFormat ? unformatted.lastIndexOf('|') : -1;
		if (rangeStart != -1 && rangeEnd != -1) {
			// remove '|'
			unformatted = unformatted.substring(0, rangeStart) + unformatted.substring(rangeStart + 1, rangeEnd)
					+ unformatted.substring(rangeEnd + 1);
			DOMDocument unformattedDoc = DOMParser.getInstance().parse(unformatted, uri, null);
			Position startPos = unformattedDoc.positionAt(rangeStart);
			Position endPos = unformattedDoc.positionAt(rangeEnd - 1);
			range = new Range(startPos, endPos);
		}

		TextDocument document = new TextDocument(unformatted, uri);
		document.setIncremental(true);
		DOMDocument xmlDocument = DOMParser.getInstance().parse(document, null);

		if (languageService == null) {
			languageService = new XMLLanguageService();
		}
		List<? extends TextEdit> edits = languageService.format(xmlDocument, range, sharedSettings);

		String formatted = applyEdits(document, edits);
		assertEquals(expected, formatted);

		if (expectedEdits != null) {
			Assertions.assertArrayEquals(expectedEdits, edits.toArray(new TextEdit[0]));
		}
	}

	// ------------------- Prepare rename assert

	public static PrepareRenameResult pr(Range range, String placeholder) {
		return new PrepareRenameResult(range, placeholder);
	}

	public static void assertPrepareRename(String value) throws BadLocationException {
		assertRename(value, null);
	}

	public static void assertPrepareRename(String value, PrepareRenameResult expected)
			throws BadLocationException {
		assertPrepareRename(null, value, expected);
	}

	public static void assertPrepareRename(XMLLanguageService languageService, String value,
			PrepareRenameResult expected) throws BadLocationException {
		assertPrepareRename(languageService, value, null, expected);
	}

	public static void assertPrepareRename(XMLLanguageService languageService, String value, String fileURI,
			PrepareRenameResult expected) throws BadLocationException {
		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);

		fileURI = fileURI != null ? fileURI : "test://test/test.html";
		DOMDocument document = DOMParser.getInstance().parse(value, fileURI,
				null);

		Position position = document.positionAt(offset);

		if (languageService == null) {
			languageService = new XMLLanguageService();
		}
		Either<Range, PrepareRenameResult> result = languageService.prepareRename(document, position, () -> {
		});
		PrepareRenameResult actual = result != null ? result.getRight() : null;
		assertEquals(expected, actual);
	}

	// ------------------- Rename assert

	public static void assertRename(String value, String newText) throws BadLocationException {
		assertRename(value, newText, Collections.emptyList());
	}

	public static void assertRename(String value, String newText, List<TextEdit> expectedEdits)
			throws BadLocationException {
		assertRename(null, value, newText, expectedEdits);
	}

	public static void assertRename(XMLLanguageService languageService, String value, String newText,
			List<TextEdit> expectedEdits) throws BadLocationException {
		assertRename(languageService, value, null, newText, expectedEdits);
	}

	public static void assertRename(XMLLanguageService languageService, String value, String fileURI, String newText,
			List<TextEdit> expectedEdits) throws BadLocationException {
		int offset = value.indexOf("|");
		value = value.substring(0, offset) + value.substring(offset + 1);

		fileURI = fileURI != null ? fileURI : "test://test/test.html";
		DOMDocument document = DOMParser.getInstance().parse(value, fileURI,
				null);

		Position position = document.positionAt(offset);

		if (languageService == null) {
			languageService = new XMLLanguageService();
		}
		WorkspaceEdit workspaceEdit = languageService.doRename(document, position, newText, () -> {
		});
		final String uri = fileURI;
		Optional<TextDocumentEdit> documentChange = workspaceEdit.getDocumentChanges()
				.stream().filter(Either::isLeft)
				.filter(e -> uri.equals(e.getLeft().getTextDocument().getUri()))
				.map(Either::getLeft).findFirst();
		List<TextEdit> actualEdits = documentChange.isPresent() ? documentChange.get().getEdits()
				: Collections.emptyList();
		assertArrayEquals(expectedEdits.toArray(), actualEdits.toArray());
	}

	// ------------------- Linked Editing assert

	public static void testLinkedEditingFor(String xml, LinkedEditingRanges expected) throws BadLocationException {
		testLinkedEditingFor(xml, null, expected);
	}

	public static void testLinkedEditingFor(String value, String fileURI, LinkedEditingRanges expected)
			throws BadLocationException {
		testLinkedEditingFor(new XMLLanguageService(), value, fileURI, expected);
	}

	public static void testLinkedEditingFor(XMLLanguageService xmlLanguageService, String value, String fileURI,
			LinkedEditingRanges expected)
			throws BadLocationException {
		int offset = value.indexOf('|');
		value = value.substring(0, offset) + value.substring(offset + 1);

		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");
		Position position = document.positionAt(offset);

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		LinkedEditingRanges actual = xmlLanguageService.findLinkedEditingRanges(xmlDocument, position, () -> {
		});
		assertLinkedEditing(actual, expected);
	}

	public static void assertLinkedEditing(LinkedEditingRanges actual, LinkedEditingRanges expected) {
		if (expected == null) {
			assertNull(actual);
		} else {
			assertNotNull(actual);
			assertEquals(expected.getWordPattern(), actual.getWordPattern());
			assertEquals(expected.getRanges(), actual.getRanges());
		}
	}

	public static LinkedEditingRanges le(Range... ranges) {
		return new LinkedEditingRanges(Arrays.asList(ranges), "[^\\s>]+");
	}

	public static LinkedEditingRanges le(String wordPattern, Range... ranges) {
		return new LinkedEditingRanges(Arrays.asList(ranges), wordPattern);
	}

	// ------------------- Generator assert

	public static void assertGrammarGenerator(String xml, FileContentGeneratorSettings grammarSettings,
			String expected) {
		assertGrammarGenerator(xml, grammarSettings, new SharedSettings(), expected);
	}

	public static void assertGrammarGenerator(String xml, FileContentGeneratorSettings grammarSettings,
			SharedSettings sharedSettings, String expected) {
		DOMDocument document = DOMParser.getInstance().parse(xml, "test.xml", null);
		XMLLanguageService languageService = new XMLLanguageService();
		FileContentGeneratorManager manager = new FileContentGeneratorManager(languageService);
		String actual = manager.generate(document, sharedSettings, grammarSettings, () -> {
		});
		Assertions.assertEquals(expected, actual);
	}

	// ------------------- Selection Range assert

	public static void testSelectionRange(String xml, SelectionRange... selectionRanges) {
		StringBuilder stringBuilder = new StringBuilder(xml);
		List<Integer> cursorOffsets = new ArrayList<>();
		int nextPipe = stringBuilder.indexOf("|");
		while (nextPipe > 0) {
			cursorOffsets.add(nextPipe);
			stringBuilder.deleteCharAt(nextPipe);
			nextPipe = stringBuilder.indexOf("|");
		}
		assertEquals(selectionRanges.length, cursorOffsets.size(),
				"Number of cursors and SelectionRanges should be equal");
		testSelectionRange(stringBuilder.toString(), cursorOffsets, selectionRanges);
	}

	public static void testSelectionRange(String xml, List<Integer> cursorOffsets, SelectionRange... selectionRanges) {
		DOMDocument document = DOMParser.getInstance().parse(xml, "test://test/test.html", null);
		List<Position> positions = new ArrayList<>();
		for (Integer offset : cursorOffsets) {
			positions.add(XMLPositionUtility.createRange(offset, offset, document).getStart());
		}
		XMLLanguageService ls = new XMLLanguageService();
		List<SelectionRange> actual = ls.getSelectionRanges(document, positions, NULL_CHECKER);
		assertSelectionRangeEquals(Arrays.asList(selectionRanges), actual);
	}

	public static void assertSelectionRangeEquals(List<SelectionRange> expected, List<SelectionRange> actual) {
		assertEquals(expected.size(), actual.size(), "Different number of expected and actual selection ranges");
		for (int i = 0; i < expected.size(); i++) {
			assertEquals(expected.get(i), actual.get(i));
		}
	}

	public static SelectionRange sr(Range... ranges) {
		return sr(Arrays.asList(ranges));
	}

	public static SelectionRange sr(List<Range> ranges) {
		if (ranges.size() == 0) {
			return null;
		}
		SelectionRange selectionRange = new SelectionRange();
		selectionRange.setRange(ranges.get(0));
		selectionRange.setParent(sr(ranges.subList(1, ranges.size())));
		return selectionRange;
	}

	public static void assertSurroundWith(String xml, SurroundWithKind kind, boolean snippetsSupported,
			String expected) throws BadLocationException, InterruptedException, ExecutionException {
		assertSurroundWith(xml, kind, snippetsSupported, (service) -> {
		}, "src/test/resources/test.xml", expected);
	}

	public static void assertSurroundWith(String xml, SurroundWithKind kind, boolean snippetsSupported,
			Consumer<XMLLanguageService> configuration, String uri,
			String expected) throws BadLocationException, InterruptedException, ExecutionException {
		MockXMLLanguageServer languageServer = new MockXMLLanguageServer();

		configuration.accept(languageServer.getXMLLanguageService());

		int rangeStart = xml.indexOf('|');
		int rangeEnd = xml.lastIndexOf('|');
		// remove '|'
		StringBuilder x = new StringBuilder(xml.substring(0, rangeStart));
		if (rangeEnd > rangeStart) {
			x.append(xml.substring(rangeStart + 1, rangeEnd));
		}
		x.append(xml.substring(Math.min(rangeEnd + 1, xml.length())));

		TextDocument document = new TextDocument(x.toString(), "");
		Position startPos = document.positionAt(rangeStart);
		Position endPos = rangeStart == rangeEnd ? startPos : document.positionAt(rangeEnd - 1);
		Range selection = new Range(startPos, endPos);

		TextDocumentIdentifier xmlIdentifier = languageServer.didOpen(uri, x.toString());

		// Execute surround with tags command
		SurroundWithResponse response = (SurroundWithResponse) languageServer
				.executeCommand(SurroundWithCommand.COMMAND_ID, xmlIdentifier, selection, kind.name(),
						snippetsSupported)
				.get();

		String actual = applyEdits(document, Arrays.asList(response.getStart(), response.getEnd()));
		assertEquals(expected, actual);
	}

	// ------------------- ColorInformation assert

	public static void testColorInformationFor(String value, String fileURI, XMLColorsSettings colorSettings,
			ColorInformation... expected) {
		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		xmlLanguageService.doSave(new SettingsSaveContext(colorSettings));

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		List<ColorInformation> actual = xmlLanguageService.findDocumentColors(xmlDocument, () -> {
		});
		assertColorInformation(actual, expected);
	}

	public static ColorInformation colorInfo(double red, double green, double blue, double alpha, Range range) {
		return new ColorInformation(range, new Color(red, green, blue, alpha));
	}

	public static void assertColorInformation(List<? extends ColorInformation> actual, ColorInformation... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getRange(), actual.get(i).getRange());
			assertEquals(expected[i].getColor(), actual.get(i).getColor());
		}
	}

	// ------------------- ColorInformation assert

	public static void testColorPresentationFor(String value, String fileURI, Color color, Range range,
			XMLColorsSettings colorSettings,
			ColorPresentation... expected) {
		TextDocument document = new TextDocument(value, fileURI != null ? fileURI : "test://test/test.xml");

		XMLLanguageService xmlLanguageService = new XMLLanguageService();
		xmlLanguageService.doSave(new SettingsSaveContext(colorSettings));

		DOMDocument xmlDocument = DOMParser.getInstance().parse(document,
				xmlLanguageService.getResolverExtensionManager());
		xmlLanguageService.setDocumentProvider((uri) -> xmlDocument);

		ColorPresentationParams params = new ColorPresentationParams(new TextDocumentIdentifier(document.getUri()),
				color, range);
		List<ColorPresentation> actual = xmlLanguageService.getColorPresentations(xmlDocument, params, () -> {
		});
		assertColorPresentation(actual, expected);
	}

	public static void assertColorPresentation(List<? extends ColorPresentation> actual,
			ColorPresentation... expected) {
		assertEquals(expected.length, actual.size());
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i].getLabel(), actual.get(i).getLabel());
			assertEquals(expected[i].getTextEdit(), actual.get(i).getTextEdit());
		}
	}

	public static ColorPresentation colorPres(String label, TextEdit textEdit) {
		return new ColorPresentation(label, textEdit);
	}
}