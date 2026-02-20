/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lemminx.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.lemminx.XMLLanguageServer;
import org.eclipse.lemminx.XMLTextDocumentService;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentLinkParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests to ensure that null is returned on requests for a document that happen before the document is open
 */
public class OutOfOrderRequestsTest {

	private static final TextDocumentIdentifier UNOPENED_DOC = new TextDocumentIdentifier("file:///file.xml");

	private TextDocumentService ls = null;

	@BeforeEach
	public void setupLS() {
		ls = new XMLTextDocumentService(new XMLLanguageServer(), null);
	}

	@Test
	public void completion() {
		CompletionParams params = new CompletionParams(UNOPENED_DOC, new Position());
		ls.completion(params) //
		.thenAccept((completion) -> {
			assertNull(completion);
		}) //
		.join();
	}

	@Test
	public void hover() {
		HoverParams params = new HoverParams(UNOPENED_DOC, new Position());
		ls.hover(params) //
		.thenAccept((hover) -> {
			assertNull(hover);
		}) //
		.join();
	}

	@Test
	public void documentHighlight() {
		DocumentHighlightParams params = new DocumentHighlightParams(UNOPENED_DOC, new Position());
		ls.documentHighlight(params) //
		.thenAccept((documentHighlight) -> {
			assertNull(documentHighlight);
		}) //
		.join();
	}

	@Test
	public void documentSymbol() {
		DocumentSymbolParams params = new DocumentSymbolParams(UNOPENED_DOC);
		ls.documentSymbol(params) //
		.thenAccept((documentSymbol) -> {
			assertNull(documentSymbol);
		}) //
		.join();
	}

	@Test
	public void rename() {
		RenameParams params = new RenameParams(UNOPENED_DOC, new Position(), "");
		ls.rename(params) //
		.thenAccept((rename) -> {
			assertNull(rename);
		}) //
		.join();
	}

	@Test
	public void foldingRange() {
		FoldingRangeRequestParams params = new FoldingRangeRequestParams(UNOPENED_DOC);
		ls.foldingRange(params) //
		.thenAccept((foldingRange) -> {
			assertNull(foldingRange);
		}) //
		.join();
	}

	@Test
	public void documentLink() {
		DocumentLinkParams params = new DocumentLinkParams(UNOPENED_DOC);
		ls.documentLink(params) //
		.thenAccept((documentLink) -> {
			assertNull(documentLink);
		}) //
		.join();
	}

	@Test
	public void definition() {
		DefinitionParams params = new DefinitionParams(UNOPENED_DOC, new Position());
		ls.definition(params) //
		.thenAccept((definition) -> {
			assertNull(definition);
		}) //
		.join();
	}

	@Test
	public void typeDefinition() {
		TypeDefinitionParams params = new TypeDefinitionParams(UNOPENED_DOC, new Position());
		ls.typeDefinition(params) //
		.thenAccept((typeDefinition) -> {
			assertNull(typeDefinition);
		}) //
		.join();
	}

	@Test
	public void references() {
		ReferenceParams params = new ReferenceParams(UNOPENED_DOC, new Position(), new ReferenceContext());
		ls.references(params) //
		.thenAccept((references) -> {
			assertNull(references);
		}) //
		.join();
	}

	@Test
	public void codeLens() {
		CodeLensParams params = new CodeLensParams(UNOPENED_DOC);
		ls.codeLens(params) //
		.thenAccept((codeLens) -> {
			// Code Lens is disabled by default, so it will return an empty list instead of null
			assertNotNull(codeLens);
			assertEquals(0, codeLens.size(), "Code lens is disabled, so it should return an empty list");
		}) //
		.join();
	}

	@Test
	public void formatting() {
		DocumentFormattingParams params = new DocumentFormattingParams(UNOPENED_DOC, new FormattingOptions());
		ls.formatting(params) //
		.thenAccept((formatting) -> {
			assertNull(formatting);
		}) //
		.join();
	}

	@Test
	public void rangeFormatting() {
		DocumentRangeFormattingParams params = new DocumentRangeFormattingParams(UNOPENED_DOC, new FormattingOptions(), new Range());
		ls.rangeFormatting(params) //
		.thenAccept((formatting) -> {
			assertNull(formatting);
		}) //
		.join();
	}

}
